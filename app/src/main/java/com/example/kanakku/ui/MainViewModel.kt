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
import com.example.kanakku.data.model.SmsMessage
import com.example.kanakku.data.repository.TransactionRepository
import com.example.kanakku.data.sms.BankSmsParser
import com.example.kanakku.data.sms.SmsReader
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
    val rawBankSms: List<SmsMessage> = emptyList(),
    val errorMessage: String? = null,
    val isLoadedFromDatabase: Boolean = false,
    val newTransactionsSynced: Int = 0,
    val lastSyncTimestamp: Long? = null,
    val merchantMappingCount: Int = 0
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _categoryMap = MutableStateFlow<Map<Long, Category>>(emptyMap())
    val categoryMap: StateFlow<Map<Long, Category>> = _categoryMap.asStateFlow()

    private val parser = BankSmsParser()
    private val categoryManager = CategoryManager()

    private var repository: TransactionRepository? = null

    fun updatePermissionStatus(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = hasPermission)
    }

    /**
     * Loads transaction data with database-first approach for fast startup.
     *
     * Flow:
     * 1. Load existing transactions from database immediately (fast)
     * 2. Check last sync timestamp
     * 3. Only parse SMS newer than last sync
     * 4. Save new transactions to database
     * 5. Update sync timestamp
     *
     * Error Handling:
     * - Database initialization failures
     * - Permission denied when reading SMS
     * - Database read/write errors
     * - All errors provide user-friendly messages via ErrorHandler
     */
    fun loadSmsData(context: Context, daysAgo: Int = 30) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Step 0: Initialize repository if not already done
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

                // Get merchant mapping count for UI display
                val mappingCount = repo.getMerchantMappingCount()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to get merchant mapping count: ${errorInfo.technicalMessage}",
                            "loadSmsData"
                        )
                    }
                    .getOrElse { 0 }

                // Step 1: Load existing transactions from database (FAST)
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

                // Step 2: Check last sync timestamp (before showing existing data)
                val lastSyncTimestamp = repo.getLastSyncTimestamp()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to get last sync timestamp: ${errorInfo.technicalMessage}",
                            "loadSmsData"
                        )
                        // Continue with null - will do full sync
                    }
                    .getOrNull()

                if (existingTransactions.isNotEmpty()) {
                    // Show existing data immediately for fast startup
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
                        lastSyncTimestamp = lastSyncTimestamp,
                        merchantMappingCount = mappingCount
                    )
                }

                // Step 3: Read only new SMS since last sync
                val smsReader = SmsReader(context)
                val newSms = if (lastSyncTimestamp != null) {
                    // Incremental sync: only read SMS newer than last sync
                    smsReader.readSmsSince(lastSyncTimestamp)
                } else {
                    // First sync: read all SMS from last N days
                    smsReader.readInboxSms(sinceDaysAgo = daysAgo)
                }

                // Check if SMS reading returned empty due to permission issues
                if (newSms.isEmpty() && existingTransactions.isEmpty()) {
                    // No existing data and no new SMS - likely permission issue
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No SMS messages found. Please ensure SMS permission is granted and you have bank SMS messages."
                    )
                    return@launch
                }

                // Step 4: Parse and filter only new bank SMS
                val newBankSms = try {
                    parser.filterBankSms(newSms)
                } catch (e: Exception) {
                    val errorInfo = ErrorHandler.handleError(e, "Filter bank SMS")
                    ErrorHandler.logWarning(
                        "Failed to filter bank SMS: ${errorInfo.technicalMessage}",
                        "loadSmsData"
                    )
                    emptyList() // Continue with empty list
                }

                val newParsed = try {
                    parser.parseAllBankSms(newBankSms)
                } catch (e: Exception) {
                    val errorInfo = ErrorHandler.handleError(e, "Parse bank SMS")
                    ErrorHandler.logWarning(
                        "Failed to parse bank SMS: ${errorInfo.technicalMessage}",
                        "loadSmsData"
                    )
                    emptyList() // Continue with empty list
                }

                // Filter out transactions that already exist in database
                val newTransactions = mutableListOf<ParsedTransaction>()
                for (transaction in newParsed) {
                    val exists = repo.transactionExists(transaction.smsId)
                        .onFailure { throwable ->
                            val errorInfo = throwable.toErrorInfo()
                            ErrorHandler.logWarning(
                                "Failed to check transaction existence: ${errorInfo.technicalMessage}",
                                "loadSmsData"
                            )
                            // Assume doesn't exist to avoid skipping
                        }
                        .getOrElse { false }

                    if (!exists) {
                        newTransactions.add(transaction)
                    }
                }

                val deduplicated = try {
                    parser.removeDuplicates(newTransactions)
                } catch (e: Exception) {
                    val errorInfo = ErrorHandler.handleError(e, "Remove duplicate transactions")
                    ErrorHandler.logWarning(
                        "Failed to remove duplicates: ${errorInfo.technicalMessage}, using all transactions",
                        "loadSmsData"
                    )
                    newTransactions // Continue with potentially duplicate transactions
                }

                // Step 5: Save new transactions to database
                if (deduplicated.isNotEmpty()) {
                    repo.saveTransactions(deduplicated)
                        .onFailure { throwable ->
                            val errorInfo = throwable.toErrorInfo()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Failed to save transactions: ${errorInfo.userMessage}"
                            )
                            return@launch
                        }
                }

                // Step 6: Update sync timestamp
                val currentTimestamp = System.currentTimeMillis()
                repo.setLastSyncTimestamp(currentTimestamp)
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to update sync timestamp: ${errorInfo.technicalMessage}",
                            "loadSmsData"
                        )
                        // Continue - not critical
                    }

                // Step 7: Load final state from database (includes both old and new)
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

                // Calculate stats
                val totalSmsCount = if (lastSyncTimestamp != null) {
                    existingTransactions.size + newSms.size
                } else {
                    newSms.size
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalSmsCount = totalSmsCount,
                    bankSmsCount = newBankSms.size,
                    duplicatesRemoved = newParsed.size - deduplicated.size,
                    transactions = allTransactions,
                    rawBankSms = newBankSms,
                    isLoadedFromDatabase = true,
                    newTransactionsSynced = deduplicated.size,
                    lastSyncTimestamp = currentTimestamp,
                    merchantMappingCount = mappingCount
                )

                ErrorHandler.logInfo(
                    "Successfully loaded ${allTransactions.size} transactions (${deduplicated.size} new)",
                    "loadSmsData"
                )
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
                // Look up the transaction to get merchant name for learning
                val transaction = _uiState.value.transactions.find { it.smsId == smsId }
                val merchant = transaction?.merchant

                // Update in-memory state and persist to database
                // Pass merchant to enable learning merchant-to-category mappings
                categoryManager.setManualOverride(smsId, category, merchant)
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
     * Resets all learned merchant-to-category mappings.
     * Clears the learned preferences and re-categorizes all transactions using default rules.
     *
     * Error Handling:
     * - Database errors when deleting mappings
     * - Errors are logged and shown to user with clear messages
     */
    fun resetLearnedMappings() {
        viewModelScope.launch {
            try {
                categoryManager.resetAllMerchantMappings()
                    .onSuccess { count ->
                        // Re-categorize all transactions with default rules
                        val transactions = _uiState.value.transactions
                        if (transactions.isNotEmpty()) {
                            val categories = try {
                                categoryManager.categorizeAll(transactions)
                            } catch (e: Exception) {
                                val errorInfo = ErrorHandler.handleError(e, "Re-categorize after reset")
                                ErrorHandler.logWarning(
                                    "Failed to re-categorize after reset: ${errorInfo.technicalMessage}",
                                    "resetLearnedMappings"
                                )
                                emptyMap() // Continue with uncategorized transactions
                            }
                            _categoryMap.value = categories
                        }

                        // Update UI state to reflect zero merchant mappings
                        _uiState.value = _uiState.value.copy(
                            merchantMappingCount = 0
                        )

                        ErrorHandler.logInfo(
                            "Reset $count learned merchant mappings",
                            "resetLearnedMappings"
                        )
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to reset learned preferences: ${errorInfo.userMessage}"
                        )
                    }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "resetLearnedMappings")
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
