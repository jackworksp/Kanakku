package com.example.kanakku.data.export

import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for exporting transactions to CSV format.
 *
 * This class handles:
 * - Conversion of transactions to CSV format
 * - Proper CSV escaping for special characters (quotes, commas, newlines)
 * - Readable date formatting (yyyy-MM-dd HH:mm)
 * - Graceful handling of null/missing values
 * - Category name resolution from category map
 *
 * Thread-safe: All methods are stateless and safe to call from any thread.
 */
object CsvExporter {

    private const val CSV_DELIMITER = ","
    private const val CSV_QUOTE = "\""
    private const val CSV_NEWLINE = "\n"

    // Date format for CSV export (readable format)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * CSV column headers
     */
    private val HEADERS = listOf(
        "Date",
        "Type",
        "Amount",
        "Category",
        "Merchant",
        "Account",
        "Reference",
        "Balance After",
        "Location"
    )

    /**
     * Exports a list of transactions to CSV format.
     *
     * @param transactions List of parsed transactions to export
     * @param categoryMap Map of category IDs to Category objects for name resolution
     * @return CSV formatted string with headers and transaction data
     */
    fun exportToCsv(
        transactions: List<ParsedTransaction>,
        categoryMap: Map<String, Category>
    ): String {
        val csvBuilder = StringBuilder()

        // Add header row
        csvBuilder.append(HEADERS.joinToString(CSV_DELIMITER))
        csvBuilder.append(CSV_NEWLINE)

        // Add transaction rows
        transactions.forEach { transaction ->
            val row = buildTransactionRow(transaction, categoryMap)
            csvBuilder.append(row)
            csvBuilder.append(CSV_NEWLINE)
        }

        return csvBuilder.toString()
    }

    /**
     * Builds a CSV row for a single transaction.
     *
     * @param transaction The transaction to convert to CSV row
     * @param categoryMap Map of category IDs to Category objects
     * @return CSV formatted row string
     */
    private fun buildTransactionRow(
        transaction: ParsedTransaction,
        categoryMap: Map<String, Category>
    ): String {
        val fields = listOf(
            formatDate(transaction.date),
            transaction.type.name,
            formatAmount(transaction.amount),
            getCategoryName(transaction, categoryMap),
            transaction.merchant ?: "",
            transaction.accountNumber ?: "",
            transaction.referenceNumber ?: "",
            formatBalance(transaction.balanceAfter),
            transaction.location ?: ""
        )

        return fields.joinToString(CSV_DELIMITER) { escapeCsvField(it) }
    }

    /**
     * Formats a timestamp to readable date string.
     *
     * @param timestamp Timestamp in milliseconds
     * @return Formatted date string (yyyy-MM-dd HH:mm)
     */
    private fun formatDate(timestamp: Long): String {
        return try {
            dateFormat.format(Date(timestamp))
        } catch (e: Exception) {
            // Fallback to timestamp if formatting fails
            timestamp.toString()
        }
    }

    /**
     * Formats an amount with 2 decimal places.
     *
     * @param amount The amount to format
     * @return Formatted amount string
     */
    private fun formatAmount(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount)
    }

    /**
     * Formats a balance value, handling null gracefully.
     *
     * @param balance The balance value (nullable)
     * @return Formatted balance string or empty string if null
     */
    private fun formatBalance(balance: Double?): String {
        return balance?.let { String.format(Locale.US, "%.2f", it) } ?: ""
    }

    /**
     * Gets the category name for a transaction.
     * Uses a simple heuristic based on merchant keywords for categorization.
     *
     * @param transaction The transaction to categorize
     * @param categoryMap Map of category IDs to Category objects
     * @return Category name or "Uncategorized" if no match found
     */
    private fun getCategoryName(
        transaction: ParsedTransaction,
        categoryMap: Map<String, Category>
    ): String {
        val merchant = transaction.merchant?.lowercase() ?: ""

        // Try to find matching category based on keywords
        val matchedCategory = categoryMap.values.firstOrNull { category ->
            category.keywords.any { keyword ->
                merchant.contains(keyword.lowercase())
            }
        }

        return matchedCategory?.name ?: "Uncategorized"
    }

    /**
     * Escapes a CSV field according to RFC 4180 standard.
     *
     * Rules:
     * - Fields containing quotes, commas, or newlines must be quoted
     * - Quotes within fields must be escaped by doubling them
     * - Empty strings are allowed
     *
     * @param field The field value to escape
     * @return Properly escaped CSV field
     */
    private fun escapeCsvField(field: String): String {
        // Check if field needs quoting
        val needsQuoting = field.contains(CSV_QUOTE) ||
                          field.contains(CSV_DELIMITER) ||
                          field.contains("\n") ||
                          field.contains("\r")

        return if (needsQuoting) {
            // Escape quotes by doubling them
            val escaped = field.replace(CSV_QUOTE, CSV_QUOTE + CSV_QUOTE)
            // Wrap in quotes
            "$CSV_QUOTE$escaped$CSV_QUOTE"
        } else {
            field
        }
    }

    /**
     * Exports transactions to CSV with custom headers.
     * Useful for specialized export formats.
     *
     * @param transactions List of transactions to export
     * @param categoryMap Map of category IDs to Category objects
     * @param customHeaders Custom header names (must match field count)
     * @return CSV formatted string with custom headers
     * @throws IllegalArgumentException if custom headers count doesn't match expected count
     */
    fun exportToCsvWithHeaders(
        transactions: List<ParsedTransaction>,
        categoryMap: Map<String, Category>,
        customHeaders: List<String>
    ): String {
        require(customHeaders.size == HEADERS.size) {
            "Custom headers must have ${HEADERS.size} elements, got ${customHeaders.size}"
        }

        val csvBuilder = StringBuilder()

        // Add custom header row
        csvBuilder.append(customHeaders.joinToString(CSV_DELIMITER) { escapeCsvField(it) })
        csvBuilder.append(CSV_NEWLINE)

        // Add transaction rows
        transactions.forEach { transaction ->
            val row = buildTransactionRow(transaction, categoryMap)
            csvBuilder.append(row)
            csvBuilder.append(CSV_NEWLINE)
        }

        return csvBuilder.toString()
    }
}
