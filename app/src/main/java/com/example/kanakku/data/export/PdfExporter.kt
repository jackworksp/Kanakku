package com.example.kanakku.data.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.Typeface
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for exporting transactions to PDF format using Android's PdfDocument API.
 *
 * This class handles:
 * - PDF document creation with proper page layout
 * - Title header with date range
 * - Summary section (total spent, total received, transaction count)
 * - Category breakdown table
 * - Transaction list table with automatic pagination
 * - Proper formatting and styling
 *
 * Thread-safe: All methods are stateless and safe to call from any thread.
 */
object PdfExporter {

    // Page dimensions (A4 in points: 1 inch = 72 points)
    private const val PAGE_WIDTH = 595  // 8.27 inches
    private const val PAGE_HEIGHT = 842 // 11.69 inches

    // Margins
    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 40f
    private const val MARGIN_TOP = 40f
    private const val MARGIN_BOTTOM = 40f

    // Content area
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT
    private const val CONTENT_START_X = MARGIN_LEFT
    private const val CONTENT_END_X = PAGE_WIDTH - MARGIN_RIGHT

    // Font sizes
    private const val FONT_SIZE_TITLE = 24f
    private const val FONT_SIZE_SUBTITLE = 14f
    private const val FONT_SIZE_HEADER = 12f
    private const val FONT_SIZE_NORMAL = 10f
    private const val FONT_SIZE_SMALL = 8f

    // Spacing
    private const val LINE_SPACING = 16f
    private const val SECTION_SPACING = 24f
    private const val TABLE_ROW_HEIGHT = 20f

    // Date format for PDF display
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    /**
     * Exports transactions to a PDF document and writes to the provided output stream.
     *
     * @param transactions List of parsed transactions to export
     * @param categoryMap Map of category IDs to Category objects for name resolution
     * @param outputStream The output stream to write the PDF to
     * @param dateRangeStart Optional start date for the report (null = no filter)
     * @param dateRangeEnd Optional end date for the report (null = no filter)
     * @throws Exception if PDF generation fails
     */
    fun exportToPdf(
        transactions: List<ParsedTransaction>,
        categoryMap: Map<String, Category>,
        outputStream: OutputStream,
        dateRangeStart: Long? = null,
        dateRangeEnd: Long? = null
    ) {
        val pdfDocument = PdfDocument()

        try {
            var currentY = MARGIN_TOP
            var pageNumber = 1
            var currentPage = startNewPage(pdfDocument, pageNumber)
            var canvas = currentPage.canvas

            // Title
            currentY = drawTitle(canvas, currentY, dateRangeStart, dateRangeEnd)
            currentY += SECTION_SPACING

            // Summary section
            currentY = drawSummary(canvas, currentY, transactions)
            currentY += SECTION_SPACING

            // Category breakdown
            val categoryBreakdown = calculateCategoryBreakdown(transactions, categoryMap)
            currentY = drawCategoryBreakdown(canvas, currentY, categoryBreakdown)
            currentY += SECTION_SPACING

            // Check if we need a new page for transaction table
            if (currentY > PAGE_HEIGHT - MARGIN_BOTTOM - 100) {
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPage = startNewPage(pdfDocument, pageNumber)
                canvas = currentPage.canvas
                currentY = MARGIN_TOP
            }

            // Transaction table header
            currentY = drawTransactionTableHeader(canvas, currentY)

            // Transaction rows with pagination
            for (transaction in transactions) {
                // Check if we need a new page
                if (currentY > PAGE_HEIGHT - MARGIN_BOTTOM - TABLE_ROW_HEIGHT) {
                    pdfDocument.finishPage(currentPage)
                    pageNumber++
                    currentPage = startNewPage(pdfDocument, pageNumber)
                    canvas = currentPage.canvas
                    currentY = MARGIN_TOP
                    // Redraw table header on new page
                    currentY = drawTransactionTableHeader(canvas, currentY)
                }

                currentY = drawTransactionRow(canvas, currentY, transaction, categoryMap)
            }

            // Finish the last page
            pdfDocument.finishPage(currentPage)

            // Write to output stream
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Starts a new page in the PDF document.
     *
     * @param pdfDocument The PDF document
     * @param pageNumber The page number
     * @return The new page info
     */
    private fun startNewPage(pdfDocument: PdfDocument, pageNumber: Int): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        return pdfDocument.startPage(pageInfo)
    }

    /**
     * Draws the title header with date range.
     *
     * @param canvas The canvas to draw on
     * @param startY The starting Y position
     * @param dateRangeStart Optional start date
     * @param dateRangeEnd Optional end date
     * @return The new Y position after drawing
     */
    private fun drawTitle(
        canvas: android.graphics.Canvas,
        startY: Float,
        dateRangeStart: Long?,
        dateRangeEnd: Long?
    ): Float {
        var y = startY

        // Main title
        val titlePaint = Paint().apply {
            textSize = FONT_SIZE_TITLE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Transaction Report", CONTENT_START_X, y, titlePaint)
        y += FONT_SIZE_TITLE + LINE_SPACING

        // Date range subtitle
        if (dateRangeStart != null && dateRangeEnd != null) {
            val subtitlePaint = Paint().apply {
                textSize = FONT_SIZE_SUBTITLE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
                color = android.graphics.Color.GRAY
            }
            val startDate = dateOnlyFormat.format(Date(dateRangeStart))
            val endDate = dateOnlyFormat.format(Date(dateRangeEnd))
            canvas.drawText("$startDate - $endDate", CONTENT_START_X, y, subtitlePaint)
            y += FONT_SIZE_SUBTITLE + LINE_SPACING
        } else if (dateRangeStart != null || dateRangeEnd != null) {
            val subtitlePaint = Paint().apply {
                textSize = FONT_SIZE_SUBTITLE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
                color = android.graphics.Color.GRAY
            }
            val dateStr = if (dateRangeStart != null) {
                "From ${dateOnlyFormat.format(Date(dateRangeStart))}"
            } else {
                "Until ${dateOnlyFormat.format(Date(dateRangeEnd!!))}"
            }
            canvas.drawText(dateStr, CONTENT_START_X, y, subtitlePaint)
            y += FONT_SIZE_SUBTITLE + LINE_SPACING
        }

        // Draw line separator
        val linePaint = Paint().apply {
            strokeWidth = 2f
            color = android.graphics.Color.BLACK
        }
        canvas.drawLine(CONTENT_START_X, y, CONTENT_END_X, y, linePaint)
        y += LINE_SPACING

        return y
    }

    /**
     * Draws the summary section with totals.
     *
     * @param canvas The canvas to draw on
     * @param startY The starting Y position
     * @param transactions List of transactions
     * @return The new Y position after drawing
     */
    private fun drawSummary(
        canvas: android.graphics.Canvas,
        startY: Float,
        transactions: List<ParsedTransaction>
    ): Float {
        var y = startY

        // Calculate totals
        val totalSpent = transactions
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }
        val totalReceived = transactions
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }
        val transactionCount = transactions.size

        // Section header
        val headerPaint = Paint().apply {
            textSize = FONT_SIZE_HEADER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Summary", CONTENT_START_X, y, headerPaint)
        y += FONT_SIZE_HEADER + LINE_SPACING

        // Summary details
        val normalPaint = Paint().apply {
            textSize = FONT_SIZE_NORMAL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        canvas.drawText(
            "Total Transactions: $transactionCount",
            CONTENT_START_X + 20f,
            y,
            normalPaint
        )
        y += FONT_SIZE_NORMAL + LINE_SPACING

        canvas.drawText(
            "Total Spent: ₹${String.format(Locale.US, "%.2f", totalSpent)}",
            CONTENT_START_X + 20f,
            y,
            normalPaint
        )
        y += FONT_SIZE_NORMAL + LINE_SPACING

        canvas.drawText(
            "Total Received: ₹${String.format(Locale.US, "%.2f", totalReceived)}",
            CONTENT_START_X + 20f,
            y,
            normalPaint
        )
        y += FONT_SIZE_NORMAL + LINE_SPACING

        val netAmount = totalReceived - totalSpent
        val netLabel = if (netAmount >= 0) "Net Gain" else "Net Loss"
        canvas.drawText(
            "$netLabel: ₹${String.format(Locale.US, "%.2f", Math.abs(netAmount))}",
            CONTENT_START_X + 20f,
            y,
            normalPaint
        )
        y += FONT_SIZE_NORMAL + LINE_SPACING

        return y
    }

    /**
     * Calculates category breakdown from transactions.
     *
     * @param transactions List of transactions
     * @param categoryMap Map of category IDs to Category objects
     * @return Map of category name to total amount
     */
    private fun calculateCategoryBreakdown(
        transactions: List<ParsedTransaction>,
        categoryMap: Map<String, Category>
    ): Map<String, Double> {
        val breakdown = mutableMapOf<String, Double>()

        transactions.forEach { transaction ->
            val categoryName = getCategoryName(transaction, categoryMap)
            val currentAmount = breakdown.getOrDefault(categoryName, 0.0)
            breakdown[categoryName] = currentAmount + transaction.amount
        }

        return breakdown.toList().sortedByDescending { it.second }.toMap()
    }

    /**
     * Draws the category breakdown table.
     *
     * @param canvas The canvas to draw on
     * @param startY The starting Y position
     * @param categoryBreakdown Map of category name to total amount
     * @return The new Y position after drawing
     */
    private fun drawCategoryBreakdown(
        canvas: android.graphics.Canvas,
        startY: Float,
        categoryBreakdown: Map<String, Double>
    ): Float {
        var y = startY

        // Section header
        val headerPaint = Paint().apply {
            textSize = FONT_SIZE_HEADER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Category Breakdown", CONTENT_START_X, y, headerPaint)
        y += FONT_SIZE_HEADER + LINE_SPACING

        // Table header
        val tablePaint = Paint().apply {
            textSize = FONT_SIZE_NORMAL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Category", CONTENT_START_X + 20f, y, tablePaint)
        canvas.drawText("Amount", CONTENT_END_X - 100f, y, tablePaint)
        y += FONT_SIZE_NORMAL + 8f

        // Draw line
        val linePaint = Paint().apply {
            strokeWidth = 1f
            color = android.graphics.Color.GRAY
        }
        canvas.drawLine(CONTENT_START_X + 20f, y, CONTENT_END_X - 20f, y, linePaint)
        y += 8f

        // Category rows
        val rowPaint = Paint().apply {
            textSize = FONT_SIZE_NORMAL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        categoryBreakdown.forEach { (category, amount) ->
            canvas.drawText(category, CONTENT_START_X + 20f, y, rowPaint)
            canvas.drawText(
                "₹${String.format(Locale.US, "%.2f", amount)}",
                CONTENT_END_X - 100f,
                y,
                rowPaint
            )
            y += FONT_SIZE_NORMAL + 8f
        }

        return y
    }

    /**
     * Draws the transaction table header.
     *
     * @param canvas The canvas to draw on
     * @param startY The starting Y position
     * @return The new Y position after drawing
     */
    private fun drawTransactionTableHeader(
        canvas: android.graphics.Canvas,
        startY: Float
    ): Float {
        var y = startY

        // Section header
        val headerPaint = Paint().apply {
            textSize = FONT_SIZE_HEADER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Transactions", CONTENT_START_X, y, headerPaint)
        y += FONT_SIZE_HEADER + LINE_SPACING

        // Table header
        val tablePaint = Paint().apply {
            textSize = FONT_SIZE_SMALL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val col1X = CONTENT_START_X + 10f
        val col2X = CONTENT_START_X + 90f
        val col3X = CONTENT_START_X + 140f
        val col4X = CONTENT_START_X + 250f
        val col5X = CONTENT_START_X + 380f

        canvas.drawText("Date", col1X, y, tablePaint)
        canvas.drawText("Type", col2X, y, tablePaint)
        canvas.drawText("Amount", col3X, y, tablePaint)
        canvas.drawText("Merchant", col4X, y, tablePaint)
        canvas.drawText("Category", col5X, y, tablePaint)
        y += FONT_SIZE_SMALL + 6f

        // Draw line
        val linePaint = Paint().apply {
            strokeWidth = 1f
            color = android.graphics.Color.GRAY
        }
        canvas.drawLine(CONTENT_START_X + 10f, y, CONTENT_END_X - 10f, y, linePaint)
        y += 10f

        return y
    }

    /**
     * Draws a transaction row in the table.
     *
     * @param canvas The canvas to draw on
     * @param startY The starting Y position
     * @param transaction The transaction to draw
     * @param categoryMap Map of category IDs to Category objects
     * @return The new Y position after drawing
     */
    private fun drawTransactionRow(
        canvas: android.graphics.Canvas,
        startY: Float,
        transaction: ParsedTransaction,
        categoryMap: Map<String, Category>
    ): Float {
        var y = startY

        val rowPaint = Paint().apply {
            textSize = FONT_SIZE_SMALL
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val col1X = CONTENT_START_X + 10f
        val col2X = CONTENT_START_X + 90f
        val col3X = CONTENT_START_X + 140f
        val col4X = CONTENT_START_X + 250f
        val col5X = CONTENT_START_X + 380f

        // Date
        val dateStr = dateOnlyFormat.format(Date(transaction.date))
        canvas.drawText(truncateText(dateStr, 12), col1X, y, rowPaint)

        // Type
        val typeColor = when (transaction.type) {
            TransactionType.DEBIT -> android.graphics.Color.RED
            TransactionType.CREDIT -> android.graphics.Color.rgb(0, 150, 0)
            TransactionType.UNKNOWN -> android.graphics.Color.GRAY
        }
        rowPaint.color = typeColor
        canvas.drawText(transaction.type.name, col2X, y, rowPaint)
        rowPaint.color = android.graphics.Color.BLACK

        // Amount
        canvas.drawText(
            "₹${String.format(Locale.US, "%.2f", transaction.amount)}",
            col3X,
            y,
            rowPaint
        )

        // Merchant
        val merchant = transaction.merchant ?: "Unknown"
        canvas.drawText(truncateText(merchant, 20), col4X, y, rowPaint)

        // Category
        val category = getCategoryName(transaction, categoryMap)
        canvas.drawText(truncateText(category, 15), col5X, y, rowPaint)

        y += TABLE_ROW_HEIGHT

        return y
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
     * Truncates text to a maximum length with ellipsis.
     *
     * @param text The text to truncate
     * @param maxLength Maximum length
     * @return Truncated text
     */
    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.substring(0, maxLength - 3) + "..."
        } else {
            text
        }
    }
}
