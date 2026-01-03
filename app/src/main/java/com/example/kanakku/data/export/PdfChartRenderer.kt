package com.example.kanakku.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.CategoryTotal
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.domain.analytics.AnalyticsCalculator
import kotlin.math.min

/**
 * Utility class for generating chart bitmaps for PDF embedding using Canvas API.
 *
 * This class creates simplified charts that can be embedded in PDF documents:
 * - Category pie chart showing spending breakdown by category
 * - Spending bar chart showing spending trends over time
 *
 * Uses AnalyticsCalculator for data processing and Canvas API for drawing.
 *
 * Thread-safe: All methods are stateless and safe to call from any thread.
 */
object PdfChartRenderer {

    // Chart dimensions
    private const val PIE_CHART_SIZE = 400
    private const val BAR_CHART_WIDTH = 500
    private const val BAR_CHART_HEIGHT = 300

    // Chart styling
    private const val CHART_BACKGROUND_COLOR = 0xFFFFFFFF.toInt()
    private const val PIE_STROKE_WIDTH = 2f
    private const val BAR_SPACING = 10f
    private const val LABEL_SIZE = 12f
    private const val TITLE_SIZE = 16f

    private val analyticsCalculator = AnalyticsCalculator()

    /**
     * Generates a pie chart bitmap showing category breakdown.
     *
     * @param transactions List of transactions to analyze
     * @param categoryMap Map of category IDs to Category objects
     * @return Bitmap containing the rendered pie chart
     */
    fun generateCategoryPieChart(
        transactions: List<ParsedTransaction>,
        categoryMap: Map<String, Category>
    ): Bitmap {
        // Create bitmap with white background
        val bitmap = Bitmap.createBitmap(PIE_CHART_SIZE, PIE_CHART_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(CHART_BACKGROUND_COLOR)

        // Get category breakdown data
        // Convert categoryMap to use Long keys for AnalyticsCalculator
        val categoryMapLong = categoryMap.mapKeys { 0L }
        val categoryTotals = analyticsCalculator.getCategoryBreakdown(transactions, categoryMapLong)

        if (categoryTotals.isEmpty()) {
            drawEmptyChartMessage(canvas, PIE_CHART_SIZE, PIE_CHART_SIZE, "No data available")
            return bitmap
        }

        // Draw title
        drawChartTitle(canvas, "Spending by Category", PIE_CHART_SIZE.toFloat())

        // Calculate pie chart dimensions
        val centerX = PIE_CHART_SIZE / 2f
        val centerY = PIE_CHART_SIZE / 2f + 30f // Offset for title
        val radius = min(PIE_CHART_SIZE / 2f - 80f, 120f)

        // Draw pie slices
        val rectF = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        var startAngle = 0f
        val totalAmount = categoryTotals.sumOf { it.totalAmount }

        categoryTotals.forEach { categoryTotal ->
            val sweepAngle = ((categoryTotal.totalAmount / totalAmount) * 360f).toFloat()

            // Draw pie slice
            val slicePaint = Paint().apply {
                style = Paint.Style.FILL
                color = composeColorToAndroidColor(categoryTotal.category.color)
                isAntiAlias = true
            }
            canvas.drawArc(rectF, startAngle, sweepAngle, true, slicePaint)

            // Draw slice border
            val borderPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = PIE_STROKE_WIDTH
                color = android.graphics.Color.WHITE
                isAntiAlias = true
            }
            canvas.drawArc(rectF, startAngle, sweepAngle, true, borderPaint)

            startAngle += sweepAngle
        }

        // Draw legend
        drawPieChartLegend(canvas, categoryTotals, PIE_CHART_SIZE)

        return bitmap
    }

    /**
     * Generates a bar chart bitmap showing spending trends.
     *
     * @param transactions List of transactions to analyze
     * @param period Time period for trend analysis
     * @return Bitmap containing the rendered bar chart
     */
    fun generateSpendingBarChart(
        transactions: List<ParsedTransaction>,
        period: com.example.kanakku.data.model.TimePeriod = com.example.kanakku.data.model.TimePeriod.WEEK
    ): Bitmap {
        // Create bitmap with white background
        val bitmap = Bitmap.createBitmap(BAR_CHART_WIDTH, BAR_CHART_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(CHART_BACKGROUND_COLOR)

        // Get trend data
        val trendPoints = analyticsCalculator.getSpendingTrend(transactions, period)

        if (trendPoints.isEmpty()) {
            drawEmptyChartMessage(canvas, BAR_CHART_WIDTH, BAR_CHART_HEIGHT, "No data available")
            return bitmap
        }

        // Draw title
        drawChartTitle(canvas, "Spending Trend - ${period.displayName}", BAR_CHART_WIDTH.toFloat())

        // Calculate chart area
        val chartLeft = 60f
        val chartRight = BAR_CHART_WIDTH - 20f
        val chartTop = 60f
        val chartBottom = BAR_CHART_HEIGHT - 40f
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        // Find max value for scaling
        val maxValue = trendPoints.maxOfOrNull { it.amount } ?: 1.0
        val barWidth = (chartWidth / trendPoints.size) - BAR_SPACING

        // Draw bars
        trendPoints.forEachIndexed { index, point ->
            val barHeight = ((point.amount / maxValue) * chartHeight).toFloat()
            val barLeft = chartLeft + (index * (barWidth + BAR_SPACING))
            val barTop = chartBottom - barHeight
            val barRight = barLeft + barWidth

            // Draw bar
            val barPaint = Paint().apply {
                style = Paint.Style.FILL
                color = 0xFF2196F3.toInt() // Blue color
                isAntiAlias = true
            }
            canvas.drawRect(barLeft, barTop, barRight, chartBottom, barPaint)

            // Draw bar border
            val borderPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = 0xFF1976D2.toInt() // Darker blue
                isAntiAlias = true
            }
            canvas.drawRect(barLeft, barTop, barRight, chartBottom, borderPaint)

            // Draw label
            val labelPaint = Paint().apply {
                textSize = LABEL_SIZE
                textAlign = Paint.Align.CENTER
                color = android.graphics.Color.BLACK
                isAntiAlias = true
            }
            canvas.drawText(
                point.label,
                barLeft + (barWidth / 2),
                chartBottom + 20f,
                labelPaint
            )

            // Draw value on top of bar
            if (barHeight > 20f) {
                val valuePaint = Paint().apply {
                    textSize = LABEL_SIZE - 2f
                    textAlign = Paint.Align.CENTER
                    color = android.graphics.Color.WHITE
                    isAntiAlias = true
                }
                canvas.drawText(
                    "₹${String.format("%.0f", point.amount)}",
                    barLeft + (barWidth / 2),
                    barTop + 15f,
                    valuePaint
                )
            }
        }

        // Draw axes
        drawAxes(canvas, chartLeft, chartRight, chartTop, chartBottom, maxValue)

        return bitmap
    }

    /**
     * Draws the chart title.
     */
    private fun drawChartTitle(canvas: Canvas, title: String, width: Float) {
        val titlePaint = Paint().apply {
            textSize = TITLE_SIZE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }
        canvas.drawText(title, width / 2f, 25f, titlePaint)
    }

    /**
     * Draws a message for empty charts.
     */
    private fun drawEmptyChartMessage(canvas: Canvas, width: Int, height: Int, message: String) {
        val messagePaint = Paint().apply {
            textSize = 14f
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.GRAY
            isAntiAlias = true
        }
        canvas.drawText(message, width / 2f, height / 2f, messagePaint)
    }

    /**
     * Draws the legend for pie chart.
     */
    private fun drawPieChartLegend(
        canvas: Canvas,
        categoryTotals: List<CategoryTotal>,
        chartSize: Int
    ) {
        val legendX = 20f
        var legendY = chartSize - 100f
        val boxSize = 12f
        val spacing = 18f

        val labelPaint = Paint().apply {
            textSize = LABEL_SIZE
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }

        val boxPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Show top 5 categories
        categoryTotals.take(5).forEach { categoryTotal ->
            // Draw color box
            boxPaint.color = composeColorToAndroidColor(categoryTotal.category.color)
            canvas.drawRect(
                legendX,
                legendY - boxSize,
                legendX + boxSize,
                legendY,
                boxPaint
            )

            // Draw label
            val label = "${categoryTotal.category.icon} ${categoryTotal.category.name} " +
                    "(${String.format("%.1f", categoryTotal.percentage)}%)"
            canvas.drawText(label, legendX + boxSize + 8f, legendY, labelPaint)

            legendY += spacing
        }
    }

    /**
     * Converts Compose Color to Android graphics color integer.
     */
    private fun composeColorToAndroidColor(color: androidx.compose.ui.graphics.Color): Int {
        return android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
    }

    /**
     * Draws X and Y axes for bar chart.
     */
    private fun drawAxes(
        canvas: Canvas,
        chartLeft: Float,
        chartRight: Float,
        chartTop: Float,
        chartBottom: Float,
        maxValue: Double
    ) {
        val axisPaint = Paint().apply {
            strokeWidth = 2f
            color = android.graphics.Color.GRAY
            isAntiAlias = true
        }

        // Draw X axis
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint)

        // Draw Y axis
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint)

        // Draw Y axis labels
        val labelPaint = Paint().apply {
            textSize = LABEL_SIZE - 2f
            textAlign = Paint.Align.RIGHT
            color = android.graphics.Color.GRAY
            isAntiAlias = true
        }

        // Draw max value at top
        canvas.drawText(
            "₹${String.format("%.0f", maxValue)}",
            chartLeft - 5f,
            chartTop + 5f,
            labelPaint
        )

        // Draw mid value
        canvas.drawText(
            "₹${String.format("%.0f", maxValue / 2)}",
            chartLeft - 5f,
            (chartTop + chartBottom) / 2,
            labelPaint
        )

        // Draw zero at bottom
        canvas.drawText(
            "₹0",
            chartLeft - 5f,
            chartBottom,
            labelPaint
        )
    }
}
