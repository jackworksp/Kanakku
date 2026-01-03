package com.example.kanakku.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.core.error.toErrorInfo
import com.example.kanakku.data.category.CategoryManager
import com.example.kanakku.data.category.CategorySuggestion
import com.example.kanakku.data.category.CategorySuggestionEngine
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

/**
 * UI State for the main screen.
 *
 * @property isLoading Whether the screen is currently loading data
 * @property hasPermission Whether SMS permission is granted
 * @property totalSmsCount Total number of SMS messages scanned
 * @property bankSmsCount Number of bank SMS messages found
 * @property duplicatesRemoved Number of duplicate transactions removed
 * @property transactions List of all parsed transactions
 * @property rawBankSms List of raw bank SMS messages
 * @property errorMessage User-friendly error message to display (null if no error)
 * @property isLoadedFromDatabase Whether data was loaded from database
 * @property newTransactionsSynced Number of new transactions synced in last operation
 * @property lastSyncTimestamp Timestamp of last successful sync
 * @property categorySuggestions Map of transaction SMS ID to list of category suggestions
 * @property isSuggestionsLoading Whether category suggestions are being generated
 * @property applyingCategoryToMultiple Whether a bulk category application is in progress
 */
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
    val categorySuggestions: Map<Long, List<CategorySuggestion>> = emptyMap(),
    val isSuggestionsLoading: Boolean = false,
    val applyingCategoryToMultiple: Boolean = false
)

/**
 * ViewModel for the main screen.
 *
 * This ViewModel manages the state and business logic for:
 * - Loading and displaying transactions from SMS
 * - Categorizing transactions automatically and manually
 * - Generating category suggestions for uncategorized transactions
 * - Applying categories to single or multiple transactions
 * - Syncing transactions with incremental updates
 *
 * Error Handling Strategy:
 * - All repository operations return Result<T> for explicit error handling
 * - Errors are caught and converted to user-friendly messages via ErrorHandler
 * - Loading states are managed to prevent UI inconsistencies
 * - Graceful degradation when subsystems fail (e.g., suggestions unavailable)
 *
 * Architecture:
 * - Follows offline-first principles with database-backed storage
 * - Uses CategoryManager for auto-categorization based on keywords
 * - Uses CategorySuggestionEngine for smart suggestions based on patterns
 * - Manages in-memory state with StateFlow for reactive UI updates
 */
class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _categoryMap = MutableStateFlow<Map<Long, Category>>(emptyMap())
    val categoryMap: StateFlow<Map<Long, Category>> = _categoryMap.asStateFlow()

    private val parser = BankSmsParser()
    private var categoryManager: CategoryManager? = null
    private var suggestionEngine: CategorySuggestionEngine? = null

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
                // Step 0: Initialize repositories, CategoryManager, and CategorySuggestionEngine if not already done
                if (repository == null || categoryManager == null || suggestionEngine == null) {
                    try {
                        repository = DatabaseProvider.getRepository(context)
                        val categoryRepo = DatabaseProvider.getCategoryRepository(context)

                        // Create and initialize CategoryManager with both repositories
                        categoryManager = CategoryManager(
                            categoryRepository = categoryRepo,
                            transactionRepository = repository
                        )
                        categoryManager!!.initialize(repository!!)

                        // Create and initialize CategorySuggestionEngine for smart suggestions
                        suggestionEngine = CategorySuggestionEngine(
                            categoryRepository = categoryRepo,
                            transactionRepository = repository!!
                        )
                        suggestionEngine!!.initialize()
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
                val catManager = categoryManager!!

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
                        catManager.categorizeAll(existingTransactions)
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
                    catManager.categorizeAll(allTransactions)
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
                    lastSyncTimestamp = currentTimestamp
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
     * Also records the categorization in the suggestion engine for pattern learning.
     *
     * Error Handling:
     * - Database write failures when saving category override
     * - Errors are logged and shown to user with clear messages
     * - Suggestion engine unavailable (continues without recording patterns)
     */
    fun updateTransactionCategory(smsId: Long, category: Category) {
        viewModelScope.launch {
            try {
                val catManager = categoryManager
                if (catManager == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Category manager not initialized. Please try again."
                    )
                    return@launch
                }

                // Update in-memory state and persist to database
                catManager.setManualOverride(smsId, category)
                    .onSuccess {
                        // Update UI state only on successful save
                        _categoryMap.value = _categoryMap.value.toMutableMap().apply {
                            put(smsId, category)
                        }

                        // Record categorization for pattern learning
                        val engine = suggestionEngine
                        if (engine != null) {
                            val transaction = _uiState.value.transactions.find { it.smsId == smsId }
                            if (transaction != null) {
                                engine.recordCategorization(transaction, category.id)
                            }
                        }

                        // Clear suggestions for this transaction since it's now categorized
                        val updatedSuggestions = _uiState.value.categorySuggestions.toMutableMap()
                        updatedSuggestions.remove(smsId)
                        _uiState.value = _uiState.value.copy(categorySuggestions = updatedSuggestions)

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
     * Generates category suggestions for uncategorized transactions.
     *
     * This method analyzes transactions that don't have a manual category override
     * and generates smart suggestions based on:
     * - Historical merchant patterns
     * - Keyword matching
     * - Pattern learning from past categorizations
     *
     * Suggestions are stored in the UI state and can be displayed to the user
     * for quick categorization.
     *
     * @param transactionIds Optional list of specific transaction IDs to get suggestions for.
     *                       If null, generates suggestions for all uncategorized transactions.
     * @param maxSuggestionsPerTransaction Maximum suggestions to generate per transaction (default: 3)
     *
     * Error Handling:
     * - Suggestion engine not initialized (logs warning, doesn't fail)
     * - Suggestion generation failures (logs error, continues for remaining transactions)
     * - Missing category overrides (treats as uncategorized)
     */
    fun generateCategorySuggestions(
        transactionIds: List<Long>? = null,
        maxSuggestionsPerTransaction: Int = 3
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSuggestionsLoading = true)

            try {
                val engine = suggestionEngine
                if (engine == null) {
                    ErrorHandler.logWarning(
                        "Suggestion engine not initialized. Cannot generate suggestions.",
                        "generateCategorySuggestions"
                    )
                    _uiState.value = _uiState.value.copy(isSuggestionsLoading = false)
                    return@launch
                }

                val repo = repository
                if (repo == null) {
                    ErrorHandler.logWarning(
                        "Repository not initialized. Cannot generate suggestions.",
                        "generateCategorySuggestions"
                    )
                    _uiState.value = _uiState.value.copy(isSuggestionsLoading = false)
                    return@launch
                }

                // Get all category overrides to identify uncategorized transactions
                val overrides = repo.getAllCategoryOverrides()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to load category overrides: ${errorInfo.technicalMessage}",
                            "generateCategorySuggestions"
                        )
                    }
                    .getOrElse { emptyMap() }

                // Determine which transactions need suggestions
                val currentTransactions = _uiState.value.transactions
                val transactionsToAnalyze = if (transactionIds != null) {
                    currentTransactions.filter { it.smsId in transactionIds }
                } else {
                    // Only suggest for uncategorized transactions (no manual override)
                    currentTransactions.filter { it.smsId !in overrides.keys }
                }

                // Generate suggestions for each transaction
                val suggestionsMap = mutableMapOf<Long, List<CategorySuggestion>>()
                for (transaction in transactionsToAnalyze) {
                    engine.suggestCategories(transaction, maxSuggestionsPerTransaction)
                        .onSuccess { suggestions ->
                            if (suggestions.isNotEmpty()) {
                                suggestionsMap[transaction.smsId] = suggestions
                            }
                        }
                        .onFailure { throwable ->
                            val errorInfo = throwable.toErrorInfo()
                            ErrorHandler.logWarning(
                                "Failed to generate suggestions for transaction ${transaction.smsId}: ${errorInfo.technicalMessage}",
                                "generateCategorySuggestions"
                            )
                            // Continue processing other transactions
                        }
                }

                _uiState.value = _uiState.value.copy(
                    categorySuggestions = suggestionsMap,
                    isSuggestionsLoading = false
                )

                ErrorHandler.logInfo(
                    "Generated suggestions for ${suggestionsMap.size} transactions",
                    "generateCategorySuggestions"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "generateCategorySuggestions")
                _uiState.value = _uiState.value.copy(
                    isSuggestionsLoading = false,
                    errorMessage = "Failed to generate suggestions: ${errorInfo.userMessage}"
                )
            }
        }
    }

    /**
     * Applies a category to multiple transactions at once.
     *
     * This is useful for:
     * - Bulk categorization from suggestions (e.g., "Apply to all similar")
     * - Batch updates from category management screen
     * - Correcting multiple miscategorized transactions
     *
     * The method updates the category override for each transaction and refreshes
     * the UI state to reflect the changes. It also records the categorization in
     * the suggestion engine for pattern learning.
     *
     * @param transactionIds List of transaction SMS IDs to update
     * @param category The category to apply to all transactions
     *
     * Error Handling:
     * - CategoryManager not initialized (shows error to user)
     * - Individual transaction update failures (logs warning, continues)
     * - Suggestion engine unavailable (continues without recording patterns)
     */
    fun applyCategoryToMultiple(transactionIds: List<Long>, category: Category) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(applyingCategoryToMultiple = true, errorMessage = null)

            try {
                val catManager = categoryManager
                if (catManager == null) {
                    _uiState.value = _uiState.value.copy(
                        applyingCategoryToMultiple = false,
                        errorMessage = "Category manager not initialized. Please try again."
                    )
                    return@launch
                }

                val engine = suggestionEngine
                var successCount = 0
                var failureCount = 0

                // Get current transactions for pattern learning
                val currentTransactions = _uiState.value.transactions

                // Apply category to each transaction
                val updatedCategoryMap = _categoryMap.value.toMutableMap()
                for (smsId in transactionIds) {
                    catManager.setManualOverride(smsId, category)
                        .onSuccess {
                            // Update in-memory state
                            updatedCategoryMap[smsId] = category
                            successCount++

                            // Record categorization for pattern learning
                            if (engine != null) {
                                val transaction = currentTransactions.find { it.smsId == smsId }
                                if (transaction != null) {
                                    engine.recordCategorization(transaction, category.id)
                                }
                            }
                        }
                        .onFailure { throwable ->
                            val errorInfo = throwable.toErrorInfo()
                            ErrorHandler.logWarning(
                                "Failed to update category for transaction $smsId: ${errorInfo.technicalMessage}",
                                "applyCategoryToMultiple"
                            )
                            failureCount++
                        }
                }

                // Update category map if any succeeded
                if (successCount > 0) {
                    _categoryMap.value = updatedCategoryMap

                    // Clear suggestions for successfully categorized transactions
                    val updatedSuggestions = _uiState.value.categorySuggestions.toMutableMap()
                    transactionIds.forEach { updatedSuggestions.remove(it) }

                    _uiState.value = _uiState.value.copy(
                        categorySuggestions = updatedSuggestions
                    )
                }

                _uiState.value = _uiState.value.copy(
                    applyingCategoryToMultiple = false,
                    errorMessage = if (failureCount > 0) {
                        "Updated $successCount transactions. $failureCount failed."
                    } else null
                )

                ErrorHandler.logInfo(
                    "Applied category '${category.name}' to $successCount of ${transactionIds.size} transactions",
                    "applyCategoryToMultiple"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "applyCategoryToMultiple")
                _uiState.value = _uiState.value.copy(
                    applyingCategoryToMultiple = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Clears category suggestions for specific transactions.
     * Useful when user dismisses suggestions or manually categorizes a transaction.
     *
     * @param transactionIds List of transaction SMS IDs to clear suggestions for.
     *                       If null, clears all suggestions.
     */
    fun clearCategorySuggestions(transactionIds: List<Long>? = null) {
        val updatedSuggestions = if (transactionIds == null) {
            emptyMap()
        } else {
            _uiState.value.categorySuggestions.toMutableMap().apply {
                transactionIds.forEach { remove(it) }
            }
        }
        _uiState.value = _uiState.value.copy(categorySuggestions = updatedSuggestions)
    }

    /**
     * Clears any error message from the UI state.
     * Should be called after the user has acknowledged the error.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
