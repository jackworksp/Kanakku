package com.example.kanakku.data.model

import android.net.Uri

/**
 * Date range for filtering export data
 */
data class DateRange(
    val startDate: Long,  // Start timestamp in milliseconds
    val endDate: Long     // End timestamp in milliseconds
)

/**
 * Export format options
 */
enum class ExportFormat(val displayName: String, val extension: String) {
    CSV("CSV", "csv"),
    PDF("PDF", "pdf")
}

/**
 * Filter criteria for export operations
 */
data class ExportFilter(
    val dateRange: DateRange?,           // Optional date range filter
    val categories: List<String>,        // List of category IDs to include (empty = all)
    val exportFormat: ExportFormat       // Target export format
)

/**
 * Result of an export operation
 */
sealed class ExportResult {
    /**
     * Export completed successfully
     * @param fileUri URI of the generated export file
     * @param fileName Name of the exported file
     * @param fileSizeBytes Size of the file in bytes
     */
    data class Success(
        val fileUri: Uri,
        val fileName: String,
        val fileSizeBytes: Long
    ) : ExportResult()

    /**
     * Export failed
     * @param errorMessage User-friendly error message
     * @param technicalError Technical error details for logging
     */
    data class Failure(
        val errorMessage: String,
        val technicalError: Throwable? = null
    ) : ExportResult()
}
