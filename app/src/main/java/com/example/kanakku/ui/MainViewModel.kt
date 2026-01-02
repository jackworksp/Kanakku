package com.example.kanakku.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val newTransactionsSynced: Int = 0
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
     */
    fun loadSmsData(context: Context, daysAgo: Int = 30) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Initialize repository if not already done
                if (repository == null) {
                    repository = DatabaseProvider.getRepository(context)
                }
                val repo = repository!!

                // Step 1: Load existing transactions from database (FAST)
                val existingTransactions = repo.getAllTransactionsSnapshot()

                if (existingTransactions.isNotEmpty()) {
                    // Show existing data immediately for fast startup
                    val categories = categoryManager.categorizeAll(existingTransactions)
                    _categoryMap.value = categories

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transactions = existingTransactions,
                        isLoadedFromDatabase = true
                    )
                }

                // Step 2: Check last sync timestamp
                val lastSyncTimestamp = repo.getLastSyncTimestamp()

                // Step 3: Read only new SMS since last sync
                val smsReader = SmsReader(context)
                val newSms = if (lastSyncTimestamp != null) {
                    // Incremental sync: only read SMS newer than last sync
                    smsReader.readSmsSince(lastSyncTimestamp)
                } else {
                    // First sync: read all SMS from last N days
                    smsReader.readInboxSms(sinceDaysAgo = daysAgo)
                }

                // Step 4: Parse and filter only new bank SMS
                val newBankSms = parser.filterBankSms(newSms)
                val newParsed = parser.parseAllBankSms(newBankSms)

                // Filter out transactions that already exist in database
                val newTransactions = mutableListOf<ParsedTransaction>()
                for (transaction in newParsed) {
                    if (!repo.transactionExists(transaction.smsId)) {
                        newTransactions.add(transaction)
                    }
                }
                val deduplicated = parser.removeDuplicates(newTransactions)

                // Step 5: Save new transactions to database
                if (deduplicated.isNotEmpty()) {
                    repo.saveTransactions(deduplicated)
                }

                // Step 6: Update sync timestamp
                val currentTimestamp = System.currentTimeMillis()
                repo.setLastSyncTimestamp(currentTimestamp)

                // Step 7: Load final state from database (includes both old and new)
                val allTransactions = repo.getAllTransactionsSnapshot()
                val categories = categoryManager.categorizeAll(allTransactions)
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
                    newTransactionsSynced = deduplicated.size
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error reading SMS: ${e.message}"
                )
            }
        }
    }

    /**
     * Updates a transaction's category and persists the override to database.
     */
    fun updateTransactionCategory(smsId: Long, category: Category) {
        viewModelScope.launch {
            // Update in-memory state
            categoryManager.setManualOverride(smsId, category)
            _categoryMap.value = _categoryMap.value.toMutableMap().apply {
                put(smsId, category)
            }

            // Persist to database
            repository?.setCategoryOverride(smsId, category.id)
        }
    }
}
