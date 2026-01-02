package com.example.kanakku.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.kanakku.data.category.CategoryManager
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.SmsMessage
import com.example.kanakku.data.sms.BankSmsParser
import com.example.kanakku.data.sms.SmsReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MainUiState(
    val isLoading: Boolean = false,
    val hasPermission: Boolean = false,
    val totalSmsCount: Int = 0,
    val bankSmsCount: Int = 0,
    val duplicatesRemoved: Int = 0,
    val transactions: List<ParsedTransaction> = emptyList(),
    val rawBankSms: List<SmsMessage> = emptyList(),
    val errorMessage: String? = null
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _categoryMap = MutableStateFlow<Map<Long, Category>>(emptyMap())
    val categoryMap: StateFlow<Map<Long, Category>> = _categoryMap.asStateFlow()

    private val parser = BankSmsParser()
    private val categoryManager = CategoryManager()

    fun updatePermissionStatus(hasPermission: Boolean) {
        _uiState.value = _uiState.value.copy(hasPermission = hasPermission)
    }

    fun loadSmsData(context: Context, daysAgo: Int = 30) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        try {
            val smsReader = SmsReader(context)
            val allSms = smsReader.readInboxSms(sinceDaysAgo = daysAgo)
            val bankSms = parser.filterBankSms(allSms)

            val allParsed = parser.parseAllBankSms(bankSms)
            val deduplicated = parser.removeDuplicates(allParsed)
            val duplicatesRemoved = allParsed.size - deduplicated.size

            val categories = categoryManager.categorizeAll(deduplicated)
            _categoryMap.value = categories

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                totalSmsCount = allSms.size,
                bankSmsCount = bankSms.size,
                duplicatesRemoved = duplicatesRemoved,
                transactions = deduplicated,
                rawBankSms = bankSms
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Error reading SMS: ${e.message}"
            )
        }
    }

    fun updateTransactionCategory(smsId: Long, category: Category) {
        categoryManager.setManualOverride(smsId, category)
        _categoryMap.value = _categoryMap.value.toMutableMap().apply {
            put(smsId, category)
        }
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
