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
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.data.repository.TransactionRepository
import com.example.kanakku.data.sms.BankSmsParser
import com.example.kanakku.data.sms.SmsReader
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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
    // Sync progress tracking
    val isInitialSync: Boolean = false,
    val syncProgress: Int = 0,
    val syncTotal: Int = 0,
    val syncStatusMessage: String? = null
)

class MainViewModel : ViewModel() {

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

    private val parser = BankSmsParser()
    private val categoryManager = CategoryManager()

    private var repository: TransactionRepository? = null

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
     * 1. Detect first launch vs subsequent launches
     * 2. First launch: Parse ALL SMS history with progress tracking
     * 3. Subsequent launches: Only parse SMS newer than last sync (incremental)
     * 4. Load existing transactions from database immediately (fast)
     * 5. Save new transactions to database
     * 6. Update sync timestamp and initial sync status
     *
     * Error Handling:
     * - Database initialization failures
     * - Permission denied when reading SMS
     * - Database read/write errors
     * - All errors provide user-friendly messages via ErrorHandler
     */
    fun loadSmsData(context: Context, daysAgo: Int = 30) {
        // Cancel any existing sync job
        syncJob?.cancel()

        syncJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Step 0: Initialize repository and preferences if not already done
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
                val appPreferences = AppPreferences.getInstance(context)

                // Step 1: Detect if this is the first launch (initial full history sync needed)
                val isInitialSync = !appPreferences.isInitialSyncComplete()

                if (isInitialSync) {
                    ErrorHandler.logInfo("Starting initial full history sync", "loadSmsData")
                    _uiState.value = _uiState.value.copy(
                        isInitialSync = true,
                        syncStatusMessage = "Preparing to sync transaction history..."
                    )
                }

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

                // Step 3: Check last sync timestamp (before showing existing data)
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

                if (existingTransactions.isNotEmpty() && !isInitialSync) {
                    // Show existing data immediately for fast startup (only for incremental sync)
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

                // Step 4: Read SMS based on sync mode
                val smsReader = SmsReader(context)
                val newSms = if (isInitialSync) {
                    // Initial sync: Read ALL SMS history
                    ErrorHandler.logInfo("Reading all SMS history for initial sync", "loadSmsData")
                    _uiState.value = _uiState.value.copy(
                        syncStatusMessage = "Reading SMS history..."
                    )
                    smsReader.readAllSms()
                } else if (lastSyncTimestamp != null) {
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

                if (isInitialSync && newSms.size > SMS_BATCH_SIZE) {
                    // Batch processing for large initial sync
                    ErrorHandler.logInfo(
                        "Starting batch processing of ${newSms.size} SMS messages in batches of $SMS_BATCH_SIZE",
                        "loadSmsData"
                    )

                    val allBankSms = mutableListOf<SmsMessage>()
                    val allTransactions = mutableListOf<ParsedTransaction>()
                    var totalProcessed = 0

                    // Process SMS in batches
                    newSms.chunked(SMS_BATCH_SIZE).forEachIndexed { batchIndex, smsBatch ->
                        // Check for cancellation before processing each batch
                        if (!isActive) {
                            ErrorHandler.logInfo("Sync cancelled by user", "loadSmsData")
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
                repo.setLastSyncTimestamp(currentTimestamp)
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to update sync timestamp: ${errorInfo.technicalMessage}",
                            "loadSmsData"
                        )
                        // Continue - not critical
                    }

                // Mark initial sync as complete if this was the first launch
                if (isInitialSync) {
                    appPreferences.setInitialSyncComplete()
                    appPreferences.clearSyncProgress()
                    ErrorHandler.logInfo(
                        "Initial sync complete: Processed ${newSms.size} SMS, found ${newBankSms.size} bank SMS, saved ${deduplicated.size} transactions",
                        "loadSmsData"
                    )
                }

                // Step 9: Load final state from database (includes both old and new)
                val allTransactions = repo.getAllTransactionsSnapshot()
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
                val totalSmsCount = if (isInitialSync) {
                    newSms.size
                } else if (lastSyncTimestamp != null) {
                    existingTransactions.size + newSms.size
                } else {
                    newSms.size
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInitialSync = false,
                    syncProgress = 0,
                    syncTotal = 0,
                    syncStatusMessage = null,
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
                    "Successfully loaded ${allTransactions.size} transactions (${deduplicated.size} new)${if (isInitialSync) " - Initial sync complete" else ""}",
                    "loadSmsData"
                )
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
     * Cancels the current sync operation if one is running.
     * This allows users to abort long-running initial sync operations.
     */
    fun cancelSync() {
        syncJob?.cancel()
        syncJob = null
        ErrorHandler.logInfo("Sync cancellation requested", "cancelSync")
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isInitialSync = false,
            syncProgress = 0,
            syncTotal = 0,
            syncStatusMessage = null
        )
    }

    /**
     * Clears any error message from the UI state.
     * Should be called after the user has acknowledged the error.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
