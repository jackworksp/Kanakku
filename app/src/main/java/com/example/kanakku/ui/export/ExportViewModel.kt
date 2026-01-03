package com.example.kanakku.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.core.error.toErrorInfo
import com.example.kanakku.data.database.DatabaseProvider
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DateRange
import com.example.kanakku.data.model.ExportFilter
import com.example.kanakku.data.model.ExportFormat
import com.example.kanakku.data.model.ExportResult
import com.example.kanakku.data.repository.ExportRepository
import com.example.kanakku.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Export screen.
 *
 * @property selectedDateRange Optional date range filter for export
 * @property selectedCategories List of category IDs to include in export (empty = all categories)
 * @property exportFormat Currently selected export format (CSV or PDF)
 * @property isExporting True while export operation is in progress
 * @property exportResult Result of the last export operation (null if no export yet)
 * @property errorMessage User-friendly error message to display (null if no error)
 * @property availableCategories List of all available categories for filtering
 */
data class ExportUiState(
    val selectedDateRange: DateRange? = null,
    val selectedCategories: List<String> = emptyList(),
    val exportFormat: ExportFormat = ExportFormat.CSV,
    val isExporting: Boolean = false,
    val exportResult: ExportResult? = null,
    val errorMessage: String? = null,
    val availableCategories: List<Category> = emptyList()
)

/**
 * ViewModel for managing export operations and UI state.
 *
 * This ViewModel handles:
 * - Managing export filter selections (date range, categories, format)
 * - Coordinating export operations via ExportRepository
 * - Managing export state and results
 * - Sharing exported files via Android share sheet
 *
 * Key responsibilities:
 * - Maintain reactive UI state for export screen
 * - Apply user-selected filters to export operations
 * - Handle export success/failure with clear user feedback
 * - Generate shareable intents for exported files
 * - Provide methods for modifying export parameters
 *
 * Error Handling:
 * - All export operations wrapped in try-catch via ErrorHandler
 * - User-friendly error messages displayed in UI state
 * - Errors logged for debugging with appropriate context
 * - Loading state managed during async operations
 */
class ExportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _categoryMap = MutableStateFlow<Map<String, Category>>(emptyMap())
    val categoryMap: StateFlow<Map<String, Category>> = _categoryMap.asStateFlow()

    private var exportRepository: ExportRepository? = null
    private var transactionRepository: TransactionRepository? = null

    /**
     * Initializes the ViewModel with required repositories.
     * Should be called after the ViewModel is created.
     *
     * @param context Android context for repository initialization
     */
    fun initialize(context: Context) {
        viewModelScope.launch {
            try {
                // Initialize repositories if not already done
                if (transactionRepository == null) {
                    transactionRepository = DatabaseProvider.getRepository(context)
                }
                if (exportRepository == null) {
                    exportRepository = ExportRepository(
                        context = context,
                        transactionRepository = transactionRepository!!
                    )
                }

                // Load available categories
                loadCategories()
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Initialize ExportViewModel")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Sets the date range filter for export.
     *
     * @param dateRange The date range to filter by (null to clear filter)
     */
    fun setDateRange(dateRange: DateRange?) {
        _uiState.value = _uiState.value.copy(
            selectedDateRange = dateRange,
            errorMessage = null
        )
        ErrorHandler.logDebug(
            "Date range updated: ${dateRange?.let { "${it.startDate} - ${it.endDate}" } ?: "cleared"}",
            "ExportViewModel"
        )
    }

    /**
     * Toggles a category in the selected categories list.
     * If the category is already selected, it will be removed.
     * If not selected, it will be added.
     *
     * @param categoryId The category ID to toggle
     */
    fun toggleCategory(categoryId: String) {
        val currentCategories = _uiState.value.selectedCategories.toMutableList()
        if (currentCategories.contains(categoryId)) {
            currentCategories.remove(categoryId)
        } else {
            currentCategories.add(categoryId)
        }
        _uiState.value = _uiState.value.copy(
            selectedCategories = currentCategories,
            errorMessage = null
        )
        ErrorHandler.logDebug(
            "Category toggled: $categoryId (${currentCategories.size} selected)",
            "ExportViewModel"
        )
    }

    /**
     * Selects all available categories for export.
     */
    fun selectAllCategories() {
        val allCategoryIds = _uiState.value.availableCategories.map { it.id }
        _uiState.value = _uiState.value.copy(
            selectedCategories = allCategoryIds,
            errorMessage = null
        )
        ErrorHandler.logDebug(
            "All categories selected: ${allCategoryIds.size}",
            "ExportViewModel"
        )
    }

    /**
     * Clears all category selections (export all categories).
     */
    fun clearCategorySelection() {
        _uiState.value = _uiState.value.copy(
            selectedCategories = emptyList(),
            errorMessage = null
        )
        ErrorHandler.logDebug(
            "Category selection cleared",
            "ExportViewModel"
        )
    }

    /**
     * Sets the export format (CSV or PDF).
     *
     * @param format The export format to use
     */
    fun setFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(
            exportFormat = format,
            errorMessage = null
        )
        ErrorHandler.logDebug(
            "Export format updated: ${format.displayName}",
            "ExportViewModel"
        )
    }

    /**
     * Exports transaction data based on current filter settings.
     * Uses the selected date range, categories, and format from UI state.
     *
     * Error Handling:
     * - Repository initialization failures
     * - Export operation failures (file I/O, data formatting)
     * - Empty transaction set after filtering
     * - All errors provide user-friendly messages via ErrorHandler
     */
    fun exportData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExporting = true,
                errorMessage = null,
                exportResult = null
            )

            try {
                // Ensure repository is initialized
                val repo = exportRepository
                if (repo == null) {
                    val errorInfo = ErrorHandler.handleError(
                        Exception("Export repository not initialized"),
                        "Export data"
                    )
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        errorMessage = errorInfo.userMessage
                    )
                    return@launch
                }

                // Build export filter from UI state
                val filter = ExportFilter(
                    dateRange = _uiState.value.selectedDateRange,
                    categories = _uiState.value.selectedCategories,
                    exportFormat = _uiState.value.exportFormat
                )

                // Perform export based on format
                val result = when (filter.exportFormat) {
                    ExportFormat.CSV -> repo.exportToCsv(filter, _categoryMap.value)
                    ExportFormat.PDF -> repo.exportToPdf(filter, _categoryMap.value)
                }

                // Handle export result
                result
                    .onSuccess { exportResult ->
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            exportResult = exportResult
                        )

                        // Log result
                        when (exportResult) {
                            is ExportResult.Success -> {
                                ErrorHandler.logInfo(
                                    "Export successful: ${exportResult.fileName} (${exportResult.fileSizeBytes} bytes)",
                                    "ExportViewModel"
                                )
                            }
                            is ExportResult.Failure -> {
                                ErrorHandler.logWarning(
                                    "Export failed: ${exportResult.errorMessage}",
                                    "ExportViewModel"
                                )
                                _uiState.value = _uiState.value.copy(
                                    errorMessage = exportResult.errorMessage
                                )
                            }
                        }
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            isExporting = false,
                            errorMessage = errorInfo.userMessage
                        )
                    }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "Export data")
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Creates a share intent for the exported file.
     * Returns null if no successful export result is available.
     *
     * @param context Android context for creating the intent
     * @return Share intent or null if no file to share
     */
    fun shareFile(context: Context): Intent? {
        val result = _uiState.value.exportResult
        if (result !is ExportResult.Success) {
            ErrorHandler.logWarning(
                "Cannot share file: no successful export result",
                "ExportViewModel"
            )
            return null
        }

        return try {
            Intent(Intent.ACTION_SEND).apply {
                type = when (_uiState.value.exportFormat) {
                    ExportFormat.CSV -> "text/csv"
                    ExportFormat.PDF -> "application/pdf"
                }
                putExtra(Intent.EXTRA_STREAM, result.fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Kanakku Transaction Export")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Exported ${result.fileName} (${formatFileSize(result.fileSizeBytes)})"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.let { intent ->
                Intent.createChooser(intent, "Share Export")
            }.also {
                ErrorHandler.logInfo(
                    "Share intent created for ${result.fileName}",
                    "ExportViewModel"
                )
            }
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Create share intent")
            _uiState.value = _uiState.value.copy(
                errorMessage = errorInfo.userMessage
            )
            null
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
     * Clears the export result from the UI state.
     * Useful for resetting the UI after sharing or starting a new export.
     */
    fun clearExportResult() {
        _uiState.value = _uiState.value.copy(
            exportResult = null,
            errorMessage = null
        )
    }

    // ==================== Private Helper Methods ====================

    /**
     * Loads available categories and builds the category map.
     * Categories are used for filtering and display in the export UI.
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                // Load default categories
                // In a real implementation, this would load from CategoryManager
                // For now, use DefaultCategories from the data model
                val categories = com.example.kanakku.data.model.DefaultCategories.ALL

                // Build category map
                val categoryMap = categories.associateBy { it.id }
                _categoryMap.value = categoryMap

                // Update UI state
                _uiState.value = _uiState.value.copy(
                    availableCategories = categories
                )

                ErrorHandler.logInfo(
                    "Loaded ${categories.size} categories",
                    "ExportViewModel"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Load categories")
                ErrorHandler.logWarning(
                    "Failed to load categories: ${errorInfo.technicalMessage}",
                    "ExportViewModel"
                )
                // Continue with empty categories - not critical
            }
        }
    }

    /**
     * Formats file size in bytes to a human-readable string.
     *
     * @param bytes File size in bytes
     * @return Formatted string (e.g., "1.5 MB", "256 KB")
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
