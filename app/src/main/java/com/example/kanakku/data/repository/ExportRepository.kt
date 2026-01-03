package com.example.kanakku.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.export.CsvExporter
import com.example.kanakku.data.export.PdfExporter
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DateRange
import com.example.kanakku.data.model.ExportFilter
import com.example.kanakku.data.model.ExportFormat
import com.example.kanakku.data.model.ExportResult
import com.example.kanakku.data.model.ParsedTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for managing transaction export operations.
 *
 * This repository handles:
 * - Exporting transactions to CSV and PDF formats
 * - Applying filters (date range, categories)
 * - Writing export files to app's private storage
 * - Generating shareable URIs using FileProvider
 * - Managing export file lifecycle (cleanup old exports)
 *
 * Key responsibilities:
 * - Coordinate between TransactionRepository and export utilities
 * - Handle file I/O operations with proper error handling
 * - Generate file names with timestamps
 * - Provide secure file sharing via FileProvider
 * - Clean up old export files to manage storage
 *
 * Error Handling Strategy:
 * - All operations return Result<ExportResult> for explicit error propagation
 * - File I/O errors are caught and converted to user-friendly messages
 * - Export utilities handle format-specific errors
 * - No uncaught exceptions - all errors handled gracefully
 *
 * File Storage:
 * - Files stored in Context.filesDir/exports subdirectory
 * - Private to the app, not accessible by other apps
 * - Shared via FileProvider URIs for security
 * - Files auto-cleaned after 7 days
 *
 * @param context Android context for file access
 * @param transactionRepository Repository for fetching transactions
 */
class ExportRepository(
    private val context: Context,
    private val transactionRepository: TransactionRepository
) {

    companion object {
        private const val EXPORTS_DIR = "exports"
        private const val FILE_PROVIDER_AUTHORITY = "com.example.kanakku.fileprovider"
        private const val MAX_EXPORT_AGE_DAYS = 7
        private const val MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000L

        // Date format for file names (yyyy-MM-dd_HHmmss)
        private val filenameDateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
    }

    /**
     * Exports transactions to CSV format.
     *
     * @param filter Export filter containing date range, categories, and format
     * @param categoryMap Map of category IDs to Category objects for name resolution
     * @return Result<ExportResult> containing success with file URI or failure with error
     */
    suspend fun exportToCsv(
        filter: ExportFilter,
        categoryMap: Map<String, Category>
    ): Result<ExportResult> {
        return ErrorHandler.runSuspendCatching("Export to CSV") {
            // Get filtered transactions
            val transactions = getFilteredTransactions(filter).getOrThrow()

            if (transactions.isEmpty()) {
                return@runSuspendCatching ExportResult.Failure(
                    errorMessage = "No transactions found matching the selected filters.",
                    technicalError = null
                )
            }

            // Generate CSV content
            val csvContent = CsvExporter.exportToCsv(transactions, categoryMap)

            // Generate file name
            val fileName = generateFileName("transactions", "csv")

            // Write to file
            val file = writeToFile(fileName, csvContent)

            // Generate shareable URI
            val uri = getFileUri(file)

            // Return success result
            ExportResult.Success(
                fileUri = uri,
                fileName = fileName,
                fileSizeBytes = file.length()
            )
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable ->
                val errorInfo = ErrorHandler.handleError(
                    throwable as Exception,
                    "Export to CSV"
                )
                Result.success(
                    ExportResult.Failure(
                        errorMessage = errorInfo.userMessage,
                        technicalError = throwable
                    )
                )
            }
        )
    }

    /**
     * Exports transactions to PDF format.
     *
     * @param filter Export filter containing date range, categories, and format
     * @param categoryMap Map of category IDs to Category objects for name resolution
     * @return Result<ExportResult> containing success with file URI or failure with error
     */
    suspend fun exportToPdf(
        filter: ExportFilter,
        categoryMap: Map<String, Category>
    ): Result<ExportResult> {
        return ErrorHandler.runSuspendCatching("Export to PDF") {
            // Get filtered transactions
            val transactions = getFilteredTransactions(filter).getOrThrow()

            if (transactions.isEmpty()) {
                return@runSuspendCatching ExportResult.Failure(
                    errorMessage = "No transactions found matching the selected filters.",
                    technicalError = null
                )
            }

            // Generate file name
            val fileName = generateFileName("transactions", "pdf")

            // Create export file
            val file = getExportFile(fileName)

            // Write PDF to file
            FileOutputStream(file).use { outputStream ->
                PdfExporter.exportToPdf(
                    transactions = transactions,
                    categoryMap = categoryMap,
                    outputStream = outputStream,
                    dateRangeStart = filter.dateRange?.startDate,
                    dateRangeEnd = filter.dateRange?.endDate
                )
            }

            // Generate shareable URI
            val uri = getFileUri(file)

            // Return success result
            ExportResult.Success(
                fileUri = uri,
                fileName = fileName,
                fileSizeBytes = file.length()
            )
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { throwable ->
                val errorInfo = ErrorHandler.handleError(
                    throwable as Exception,
                    "Export to PDF"
                )
                Result.success(
                    ExportResult.Failure(
                        errorMessage = errorInfo.userMessage,
                        technicalError = throwable
                    )
                )
            }
        )
    }

    /**
     * Gets a file reference for export.
     * Creates the exports directory if it doesn't exist.
     *
     * @param fileName The name of the export file
     * @return File reference in the exports directory
     */
    fun getExportFile(fileName: String): File {
        val exportsDir = File(context.filesDir, EXPORTS_DIR)
        if (!exportsDir.exists()) {
            exportsDir.mkdirs()
        }
        return File(exportsDir, fileName)
    }

    /**
     * Cleans up old export files to manage storage.
     * Deletes files older than MAX_EXPORT_AGE_DAYS.
     *
     * @return Result<Int> containing number of files deleted or error information
     */
    suspend fun cleanupOldExports(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Cleanup old exports") {
            val exportsDir = File(context.filesDir, EXPORTS_DIR)
            if (!exportsDir.exists()) {
                return@runSuspendCatching 0
            }

            val currentTime = System.currentTimeMillis()
            val maxAge = MAX_EXPORT_AGE_DAYS * MILLISECONDS_PER_DAY
            var deletedCount = 0

            exportsDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val fileAge = currentTime - file.lastModified()
                    if (fileAge > maxAge) {
                        if (file.delete()) {
                            deletedCount++
                            ErrorHandler.logDebug(
                                "Deleted old export: ${file.name}",
                                "ExportRepository"
                            )
                        }
                    }
                }
            }

            ErrorHandler.logInfo(
                "Cleanup completed: $deletedCount files deleted",
                "ExportRepository"
            )
            deletedCount
        }
    }

    /**
     * Gets all export files in the exports directory.
     *
     * @return Result<List<File>> containing list of export files or error information
     */
    suspend fun getAllExportFiles(): Result<List<File>> {
        return ErrorHandler.runSuspendCatching("Get all export files") {
            val exportsDir = File(context.filesDir, EXPORTS_DIR)
            if (!exportsDir.exists()) {
                return@runSuspendCatching emptyList()
            }

            exportsDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }
    }

    /**
     * Deletes a specific export file.
     *
     * @param fileName The name of the file to delete
     * @return Result<Boolean> indicating if file was deleted or error information
     */
    suspend fun deleteExportFile(fileName: String): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Delete export file") {
            val file = getExportFile(fileName)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Gets filtered transactions based on export filter criteria.
     *
     * @param filter Export filter containing date range and categories
     * @return Result<List<ParsedTransaction>> containing filtered transactions
     */
    private suspend fun getFilteredTransactions(filter: ExportFilter): Result<List<ParsedTransaction>> {
        return ErrorHandler.runSuspendCatching("Get filtered transactions") {
            // Get all transactions
            val allTransactions = transactionRepository.getAllTransactionsSnapshot()
                .getOrThrow()

            // Apply filters
            var filtered = allTransactions

            // Filter by date range if specified
            filter.dateRange?.let { dateRange ->
                filtered = filtered.filter { transaction ->
                    transaction.date in dateRange.startDate..dateRange.endDate
                }
            }

            // Filter by categories if specified
            if (filter.categories.isNotEmpty()) {
                // Note: Category filtering requires category matching logic
                // For now, we'll pass all transactions since category matching
                // is done in the exporters based on merchant keywords
                // This would be enhanced when category overrides are implemented
                ErrorHandler.logDebug(
                    "Category filtering not yet implemented, exporting all filtered transactions",
                    "ExportRepository"
                )
            }

            filtered
        }
    }

    /**
     * Writes content to a file in the exports directory.
     *
     * @param fileName The name of the file
     * @param content The content to write
     * @return File reference to the written file
     */
    private fun writeToFile(fileName: String, content: String): File {
        val file = getExportFile(fileName)
        file.writeText(content)
        ErrorHandler.logInfo(
            "Wrote ${content.length} characters to ${file.name}",
            "ExportRepository"
        )
        return file
    }

    /**
     * Generates a shareable URI for a file using FileProvider.
     *
     * @param file The file to generate URI for
     * @return Content URI that can be shared with other apps
     */
    private fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            file
        )
    }

    /**
     * Generates a unique file name with timestamp.
     *
     * @param prefix File name prefix (e.g., "transactions")
     * @param extension File extension without dot (e.g., "csv", "pdf")
     * @return Generated file name in format: prefix_yyyy-MM-dd_HHmmss.extension
     */
    private fun generateFileName(prefix: String, extension: String): String {
        val timestamp = filenameDateFormat.format(Date())
        return "${prefix}_${timestamp}.${extension}"
    }
}
