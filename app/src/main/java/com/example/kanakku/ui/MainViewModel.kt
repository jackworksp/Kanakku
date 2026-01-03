package com.example.kanakku.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.core.error.toErrorInfo
import com.example.kanakku.data.category.CategoryManager
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.CategoryBudgetProgress
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.SmsMessage
import com.example.kanakku.data.model.TransactionFilter
import com.example.kanakku.data.repository.TransactionRepository
import com.example.kanakku.data.sms.BankSmsParser
import com.example.kanakku.data.sms.SmsReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val lastSyncTimestamp: Long? = null,
    val merchantMappingCount: Int = 0
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val parser: BankSmsParser,
    private val categoryManager: CategoryManager,
    private val smsReader: SmsReader
) : ViewModel() {

    companion object {
        /**
         * Batch size for processing large SMS histories during initial sync.
         * Prevents overwhelming the database and causing ANRs by processing
         * SMS in manageable chunks and updating UI progress between batches.
         */
        private const val SMS_BATCH_SIZE = 100
    }

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _categoryMap = MutableStateFlow<Map<Long, Category>>(emptyMap())
    val categoryMap: StateFlow<Map<Long, Category>> = _categoryMap.asStateFlow()

    init {
        // Initialize CategoryManager to load category overrides from database
        viewModelScope.launch {
            try {
                categoryManager.initialize()
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Initialize CategoryManager")
                ErrorHandler.logWarning(
                    "Failed to initialize category manager: ${errorInfo.technicalMessage}",
                    "MainViewModel.init"
                )
                // Continue without overrides - not critical
            }
        }
    }

    /**
     * Active sync job for cancellation support.
     * Allows users to cancel long-running initial sync operations.
     */
    private var syncJob: Job? = null

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
     * - Permission denied when reading SMS
     * - SMS sync errors
     * - All errors provide user-friendly messages via ErrorHandler
     *
     * @param daysAgo Number of days to look back for initial SMS sync (default 30)
     */
    fun loadSmsData(daysAgo: Int = 30) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Step 1: Initialize repository if not already done
                if (repository == null) {
                    try {
                        repository = DatabaseProvider.getRepository(context)
                        budgetRepository = DatabaseProvider.getBudgetRepository(context)
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
                val appPreferences = AppPreferences.getInstance(context)

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
                val existingTransactions = repository.getAllTransactionsSnapshot()
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
                val lastSyncTimestamp = repository.getLastSyncTimestamp()
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
                        lastSyncTimestamp = lastSyncTimestamp,
                        merchantMappingCount = mappingCount
                    )

                    // Update filtered transactions with existing data
                    _searchFilterState.value = _searchFilterState.value.copy(
                        filteredTransactions = existingTransactions
                    )
                }

                // Step 3: Read only new SMS since last sync
                val newSms = if (lastSyncTimestamp != null) {
                    // Incremental sync: only read SMS newer than last sync
                    ErrorHandler.logInfo("Reading SMS since timestamp $lastSyncTimestamp", "loadSmsData")
                    smsReader.readSmsSince(lastSyncTimestamp)
                } else {
                    // Fallback: read SMS from last N days (shouldn't happen if initial sync is tracked)
                    ErrorHandler.logWarning(
                        "No initial sync flag and no timestamp - falling back to $daysAgo days",
                        "loadSmsData"
                    )
                    smsReader.readInboxSms(sinceDaysAgo = daysAgo)
                }

                // Update sync progress with total count
                if (isInitialSync && newSms.isNotEmpty()) {
                    appPreferences.setSyncTotalCount(newSms.size)
                    _uiState.value = _uiState.value.copy(
                        syncTotal = newSms.size,
                        syncProgress = 0,
                        syncStatusMessage = "Processing ${newSms.size} SMS messages..."
                    )
                    ErrorHandler.logInfo("Initial sync: Found ${newSms.size} total SMS messages", "loadSmsData")
                }

                // Check if SMS reading returned empty due to permission issues
                if (newSms.isEmpty() && existingTransactions.isEmpty()) {
                    // No existing data and no new SMS - likely permission issue
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isInitialSync = false,
                        syncStatusMessage = null,
                        errorMessage = "No SMS messages found. Please ensure SMS permission is granted and you have bank SMS messages."
                    )
                    return@launch
                }

                // Step 5: Process SMS based on sync mode
                val newBankSms: List<SmsMessage>
                val deduplicated: List<ParsedTransaction>
                var totalParsedCount = 0 // Track total parsed transactions before deduplication

                if (isInitialSync && newSms.size > SMS_BATCH_SIZE) {
                    // Batch processing for large initial sync
                    ErrorHandler.logInfo(
                        "Starting batch processing of ${newSms.size} SMS messages in batches of $SMS_BATCH_SIZE",
                        "loadSmsData"
                    )

                    val allBankSms = mutableListOf<SmsMessage>()
                    val allTransactions = mutableListOf<ParsedTransaction>()
                    var totalProcessed = 0

                // Filter out transactions that already exist in database
                val newTransactions = mutableListOf<ParsedTransaction>()
                for (transaction in newParsed) {
                    val exists = repository.transactionExists(transaction.smsId)
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
                    repository.saveTransactions(deduplicated)
                        .onFailure { throwable ->
                            val errorInfo = throwable.toErrorInfo()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isInitialSync = false,
                                syncProgress = 0,
                                syncTotal = 0,
                                syncStatusMessage = null,
                                errorMessage = "Sync cancelled"
                            )
                            return@launch
                        }

                        val batchNum = batchIndex + 1
                        val totalBatches = (newSms.size + SMS_BATCH_SIZE - 1) / SMS_BATCH_SIZE

                        ErrorHandler.logInfo(
                            "Processing batch $batchNum/$totalBatches (${smsBatch.size} SMS)",
                            "loadSmsData"
                        )

                        _uiState.value = _uiState.value.copy(
                            syncStatusMessage = "Processing batch $batchNum of $totalBatches..."
                        )

                        // Filter bank SMS in this batch
                        val batchBankSms = try {
                            parser.filterBankSms(smsBatch)
                        } catch (e: Exception) {
                            val errorInfo = ErrorHandler.handleError(e, "Filter bank SMS batch $batchNum")
                            ErrorHandler.logWarning(
                                "Failed to filter bank SMS in batch $batchNum: ${errorInfo.technicalMessage}",
                                "loadSmsData"
                            )
                            emptyList()
                        }
                        allBankSms.addAll(batchBankSms)

                        // Parse bank SMS in this batch
                        val batchParsed = try {
                            parser.parseAllBankSms(batchBankSms)
                        } catch (e: Exception) {
                            val errorInfo = ErrorHandler.handleError(e, "Parse bank SMS batch $batchNum")
                            ErrorHandler.logWarning(
                                "Failed to parse bank SMS in batch $batchNum: ${errorInfo.technicalMessage}",
                                "loadSmsData"
                            )
                            emptyList()
                        }

                        totalParsedCount += batchParsed.size

                        // Filter out transactions that already exist in database
                        val batchNewTransactions = mutableListOf<ParsedTransaction>()
                        for (transaction in batchParsed) {
                            val exists = repo.transactionExists(transaction.smsId)
                                .onFailure { throwable ->
                                    val errorInfo = throwable.toErrorInfo()
                                    ErrorHandler.logWarning(
                                        "Failed to check transaction existence: ${errorInfo.technicalMessage}",
                                        "loadSmsData"
                                    )
                                }
                                .getOrElse { false }

                            if (!exists) {
                                batchNewTransactions.add(transaction)
                            }
                        }

                        // Remove duplicates within this batch
                        val batchDeduplicated = try {
                            parser.removeDuplicates(batchNewTransactions)
                        } catch (e: Exception) {
                            val errorInfo = ErrorHandler.handleError(e, "Remove duplicates batch $batchNum")
                            ErrorHandler.logWarning(
                                "Failed to remove duplicates in batch $batchNum: ${errorInfo.technicalMessage}",
                                "loadSmsData"
                            )
                            batchNewTransactions
                        }

                        // Save batch to database
                        if (batchDeduplicated.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(
                                syncStatusMessage = "Saving batch $batchNum (${batchDeduplicated.size} transactions)..."
                            )

                            repo.saveTransactions(batchDeduplicated)
                                .onFailure { throwable ->
                                    val errorInfo = throwable.toErrorInfo()
                                    ErrorHandler.logWarning(
                                        "Failed to save batch $batchNum: ${errorInfo.technicalMessage}",
                                        "loadSmsData"
                                    )
                                    // Continue with next batch instead of failing completely
                                }
                                .onSuccess {
                                    allTransactions.addAll(batchDeduplicated)
                                    ErrorHandler.logInfo(
                                        "Batch $batchNum saved: ${batchDeduplicated.size} transactions",
                                        "loadSmsData"
                                    )
                                }
                        }

                        // Update progress after each batch
                        totalProcessed += smsBatch.size
                        _uiState.value = _uiState.value.copy(
                            syncProgress = totalProcessed
                        )

                        ErrorHandler.logInfo(
                            "Batch $batchNum complete: ${batchBankSms.size} bank SMS, ${batchDeduplicated.size} new transactions. Total progress: $totalProcessed/${newSms.size}",
                            "loadSmsData"
                        )
                    }

                    newBankSms = allBankSms
                    deduplicated = allTransactions

                    ErrorHandler.logInfo(
                        "Batch processing complete: ${newBankSms.size} total bank SMS, ${deduplicated.size} total new transactions",
                        "loadSmsData"
                    )
                } else {
                    // Single-pass processing for small initial sync or incremental sync
                    ErrorHandler.logInfo(
                        "Processing ${newSms.size} SMS messages in single pass",
                        "loadSmsData"
                    )

                    // Step 5a: Filter bank SMS
                    newBankSms = try {
                        if (isInitialSync) {
                            _uiState.value = _uiState.value.copy(
                                syncStatusMessage = "Filtering bank SMS..."
                            )
                        }
                        parser.filterBankSms(newSms)
                    } catch (e: Exception) {
                        val errorInfo = ErrorHandler.handleError(e, "Filter bank SMS")
                        ErrorHandler.logWarning(
                            "Failed to filter bank SMS: ${errorInfo.technicalMessage}",
                            "loadSmsData"
                        )
                        emptyList()
                    }

                    // Step 5b: Parse bank SMS
                    val newParsed = try {
                        if (isInitialSync) {
                            _uiState.value = _uiState.value.copy(
                                syncStatusMessage = "Parsing ${newBankSms.size} bank transactions..."
                            )
                        }
                        parser.parseAllBankSms(newBankSms)
                    } catch (e: Exception) {
                        val errorInfo = ErrorHandler.handleError(e, "Parse bank SMS")
                        ErrorHandler.logWarning(
                            "Failed to parse bank SMS: ${errorInfo.technicalMessage}",
                            "loadSmsData"
                        )
                        emptyList()
                    }

                    totalParsedCount = newParsed.size

                    // Step 6: Filter out transactions that already exist in database
                    if (isInitialSync) {
                        _uiState.value = _uiState.value.copy(
                            syncStatusMessage = "Checking for duplicates..."
                        )
                    }

                    val newTransactions = mutableListOf<ParsedTransaction>()
                    for (transaction in newParsed) {
                        val exists = repo.transactionExists(transaction.smsId)
                            .onFailure { throwable ->
                                val errorInfo = throwable.toErrorInfo()
                                ErrorHandler.logWarning(
                                    "Failed to check transaction existence: ${errorInfo.technicalMessage}",
                                    "loadSmsData"
                                )
                            }
                            .getOrElse { false }

                        if (!exists) {
                            newTransactions.add(transaction)
                        }
                    }

                    deduplicated = try {
                        parser.removeDuplicates(newTransactions)
                    } catch (e: Exception) {
                        val errorInfo = ErrorHandler.handleError(e, "Remove duplicate transactions")
                        ErrorHandler.logWarning(
                            "Failed to remove duplicates: ${errorInfo.technicalMessage}, using all transactions",
                            "loadSmsData"
                        )
                        newTransactions
                    }

                    // Step 7: Save new transactions to database
                    if (deduplicated.isNotEmpty()) {
                        if (isInitialSync) {
                            _uiState.value = _uiState.value.copy(
                                syncStatusMessage = "Saving ${deduplicated.size} transactions to database..."
                            )
                        }
                        repo.saveTransactions(deduplicated)
                            .onFailure { throwable ->
                                val errorInfo = throwable.toErrorInfo()
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isInitialSync = false,
                                    syncStatusMessage = null,
                                    errorMessage = "Failed to save transactions: ${errorInfo.userMessage}"
                                )
                                return@launch
                            }
                    }
                }

                // Step 8: Update sync timestamp and mark initial sync complete
                val currentTimestamp = System.currentTimeMillis()
                repository.setLastSyncTimestamp(currentTimestamp)
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

                // Step 7: Load final state from database (includes both old and new)
                val allTransactions = repository.getAllTransactionsSnapshot()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isInitialSync = false,
                            syncStatusMessage = null,
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

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitialSync = false,
                    syncProgress = 0,
                    syncTotal = 0,
                    syncStatusMessage = null,
                    totalSmsCount = totalSmsCount,
                    bankSmsCount = newBankSms.size,
                    duplicatesRemoved = totalParsedCount - deduplicated.size,
                    transactions = allTransactions,
                    rawBankSms = newBankSms,
                    isLoadedFromDatabase = true,
                    newTransactionsSynced = deduplicated.size,
                    lastSyncTimestamp = currentTimestamp,
                    merchantMappingCount = mappingCount
                )

                // Update filtered transactions with the loaded data
                _searchFilterState.value = _searchFilterState.value.copy(
                    filteredTransactions = allTransactions
                )

                ErrorHandler.logInfo(
                    "Successfully loaded ${allTransactions.size} transactions (${deduplicated.size} new)${if (isInitialSync) " - Initial sync complete" else ""}",
                    "loadSmsData"
                )

                // Step 8: Load budget summary for current month
                loadBudgetSummary(context)

            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "loadSmsData")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitialSync = false,
                    syncProgress = 0,
                    syncTotal = 0,
                    syncStatusMessage = null,
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
<<<<<<< HEAD
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

    /**
     * Get all transactions for backup purposes.
     *
     * @return Current list of parsed transactions
     */
    fun getTransactionsForBackup(): List<ParsedTransaction> {
        return _uiState.value.transactions
    }

    /**
     * Get category overrides for backup purposes.
     *
     * @return Map of smsId to categoryId for all manual category overrides
     */
    fun getCategoryOverridesForBackup(): Map<Long, String> {
        return categoryManager.exportCategoryOverrides()
    }

    /**
     * Get the CategoryManager instance for backup operations.
     *
     * @return CategoryManager instance used by this ViewModel
     */
    fun getCategoryManager(): CategoryManager {
        return categoryManager
    }
}
