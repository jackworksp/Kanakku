package com.example.kanakku.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.core.error.toErrorInfo
import com.example.kanakku.data.category.CategoryManager
import com.example.kanakku.data.database.DatabaseProvider
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val totalSmsCount: Int = 0,
    val bankSmsCount: Int = 0,
    val duplicatesRemoved: Int = 0,
    val transactions: List<ParsedTransaction> = emptyList(),
    val errorMessage: String? = null,
    val isLoadedFromDatabase: Boolean = false,
    val newTransactionsSynced: Int = 0,
    val lastSyncTimestamp: Long? = null
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _categoryMap = MutableStateFlow<Map<Long, Category>>(emptyMap())
    val categoryMap: StateFlow<Map<Long, Category>> = _categoryMap.asStateFlow()

    private val categoryManager = CategoryManager()

    private var repository: TransactionRepository? = null

    fun updatePermissionStatus(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = hasPermission)
    }

    /**
     * Loads transaction data with database-first approach for fast startup.
     *
     * Flow:
     * 1. Initialize repository and CategoryManager
     * 2. Load existing transactions from database immediately (fast startup)
     * 3. Sync new transactions from SMS using repository
     * 4. Load final state and categorize transactions
     *
     * Error Handling:
     * - Database initialization failures
     * - Permission denied when reading SMS
     * - SMS sync errors
     * - All errors provide user-friendly messages via ErrorHandler
     */
    fun loadSmsData(context: Context, daysAgo: Int = 30) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Step 1: Initialize repository if not already done
                if (repository == null) {
                    try {
                        repository = DatabaseProvider.getRepository(context)
                        // Initialize CategoryManager with repository and load overrides
                        categoryManager.initialize(repository!!)
                    } catch (e: Exception) {
                        val errorInfo = ErrorHandler.handleError(e, "Database initialization")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = errorInfo.userMessage
                        )
                        return@launch
                    }
                }
                val repo = repository!!

                // Step 2: Load existing transactions from database (FAST)
                val existingTransactions = repo.getAllTransactionsSnapshot()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to load existing transactions: ${errorInfo.technicalMessage}",
                            "loadSmsData"
                        )
                        // Continue with empty list - not critical
                    }
                    .getOrElse { emptyList() }

                // Get last sync timestamp for display
                val lastSyncTimestamp = repo.getLastSyncTimestamp()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to get last sync timestamp: ${errorInfo.technicalMessage}",
                            "loadSmsData"
                        )
                        // Continue with null
                    }
                    .getOrNull()

                // Step 3: Show existing data immediately for fast startup
                if (existingTransactions.isNotEmpty()) {
                    val categories = try {
                        categoryManager.categorizeAll(existingTransactions)
                    } catch (e: Exception) {
                        val errorInfo = ErrorHandler.handleError(e, "Categorize existing transactions")
                        ErrorHandler.logWarning(
                            "Failed to categorize existing transactions: ${errorInfo.technicalMessage}",
                            "loadSmsData"
                        )
                        emptyMap() // Continue with uncategorized transactions
                    }
                    _categoryMap.value = categories

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transactions = existingTransactions,
                        isLoadedFromDatabase = true,
                        lastSyncTimestamp = lastSyncTimestamp
                    )
                }

                // Step 4: Sync new transactions from SMS using repository
                val syncResult = repo.syncFromSms(daysAgo)
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to sync transactions from SMS: ${errorInfo.technicalMessage}",
                            "loadSmsData"
                        )
                        // If sync fails but we have existing data, show that
                        if (existingTransactions.isEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to sync transactions: ${errorInfo.userMessage}"
                            )
                            return@launch
                        }
                    }
                    .getOrNull()

                // Step 5: Load final state from database (includes both old and new)
                val allTransactions = repo.getAllTransactionsSnapshot()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load transactions: ${errorInfo.userMessage}"
                        )
                        return@launch
                    }
                    .getOrElse { emptyList() }

                // Step 6: Categorize all transactions
                val categories = try {
                    categoryManager.categorizeAll(allTransactions)
                } catch (e: Exception) {
                    val errorInfo = ErrorHandler.handleError(e, "Categorize all transactions")
                    ErrorHandler.logWarning(
                        "Failed to categorize all transactions: ${errorInfo.technicalMessage}",
                        "loadSmsData"
                    )
                    emptyMap() // Continue with uncategorized transactions
                }
                _categoryMap.value = categories

                // Step 7: Update UI state with sync results
                if (syncResult != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        totalSmsCount = syncResult.totalSmsRead,
                        bankSmsCount = syncResult.bankSmsFound,
                        duplicatesRemoved = syncResult.duplicatesRemoved,
                        transactions = allTransactions,
                        isLoadedFromDatabase = true,
                        newTransactionsSynced = syncResult.newTransactionsSaved,
                        lastSyncTimestamp = syncResult.syncTimestamp
                    )

                    ErrorHandler.logInfo(
                        "Successfully loaded ${allTransactions.size} transactions (${syncResult.newTransactionsSaved} new)",
                        "loadSmsData"
                    )
                } else {
                    // Sync failed, but we have existing data - update UI with what we have
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transactions = allTransactions,
                        isLoadedFromDatabase = true,
                        lastSyncTimestamp = lastSyncTimestamp
                    )

                    ErrorHandler.logInfo(
                        "Loaded ${allTransactions.size} existing transactions (sync skipped due to error)",
                        "loadSmsData"
                    )
                }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "loadSmsData")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Updates a transaction's category and persists the override to database.
     *
     * Error Handling:
     * - Database write failures when saving category override
     * - Errors are logged and shown to user with clear messages
     */
    fun updateTransactionCategory(smsId: Long, category: Category) {
        viewModelScope.launch {
            try {
                // Update in-memory state and persist to database
                categoryManager.setManualOverride(smsId, category)
                    .onSuccess {
                        // Update UI state only on successful save
                        _categoryMap.value = _categoryMap.value.toMutableMap().apply {
                            put(smsId, category)
                        }
                        ErrorHandler.logInfo(
                            "Category updated for transaction $smsId to ${category.name}",
                            "updateTransactionCategory"
                        )
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to update category: ${errorInfo.userMessage}"
                        )
                    }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "updateTransactionCategory")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Clears any error message from the UI state.
     * Should be called after the user has acknowledged the error.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
