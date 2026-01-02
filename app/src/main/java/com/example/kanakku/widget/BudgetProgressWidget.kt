package com.example.kanakku.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.kanakku.widget.data.BudgetPreferences
import com.example.kanakku.widget.data.WidgetDataRepository
import java.text.NumberFormat
import java.util.Locale

/**
 * Medium widget (3x2) displaying weekly budget progress.
 *
 * This widget provides a visual representation of the user's weekly spending
 * compared to their budget. It shows:
 * - "Weekly Budget" label
 * - Spent/Budget amounts (e.g., ₹5,000 / ₹10,000)
 * - Progress bar with color coding based on spending percentage
 * - Handles case when no budget is set
 *
 * Color coding:
 * - Green: < 80% of budget spent (on track)
 * - Yellow: 80-100% of budget spent (approaching limit)
 * - Red: > 100% of budget spent (over budget)
 *
 * The widget supports both light and dark themes and updates periodically
 * to show the latest budget progress.
 *
 * Size: 3x2 cells (approximately 180dp x 110dp)
 * Update frequency: Managed by WorkManager (hourly)
 *
 * @see com.example.kanakku.widget.BudgetProgressWidgetReceiver
 * @see com.example.kanakku.widget.data.WidgetDataRepository
 * @see com.example.kanakku.widget.data.BudgetPreferences
 */
class BudgetProgressWidget : GlanceAppWidget() {

    /**
     * Provides the widget content using Jetpack Glance composables.
     *
     * This method is called when the widget needs to be rendered.
     * It fetches the user's budget from preferences and the latest spending
     * data from the repository, then composes the UI.
     *
     * @param context The context for the widget
     * @param glanceId Unique identifier for this widget instance
     */
    override suspend fun provideGlance(context: Context, glanceId: GlanceId) {
        // Fetch budget from preferences
        val budget = BudgetPreferences.getWeeklyBudget(context)

        // Fetch weekly spending data from repository
        val repository = WidgetDataRepository(context)
        val budgetData = repository.getWeeklyBudgetProgress(budget)

        provideContent {
            GlanceTheme {
                BudgetProgressContent(
                    spent = budgetData.spent,
                    budget = budgetData.budget,
                    percentage = budgetData.percentage,
                    hasBudgetSet = budget > 0.0
                )
            }
        }
    }

    /**
     * Composable function that defines the widget's UI layout.
     *
     * Displays budget progress with a progress bar and amounts in a format
     * suitable for a medium 3x2 widget size. The layout adapts to both light
     * and dark themes, with color-coded progress indication.
     *
     * @param spent The amount spent this week
     * @param budget The total weekly budget
     * @param percentage The percentage of budget spent (0-100, may exceed 100)
     * @param hasBudgetSet Whether the user has configured a budget
     */
    @Composable
    private fun BudgetProgressContent(
        spent: Double,
        budget: Double,
        percentage: Double,
        hasBudgetSet: Boolean
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            // Widget title
            Text(
                text = "Weekly Budget",
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.size(8.dp))

            if (hasBudgetSet) {
                // Show budget progress when budget is set
                BudgetProgressView(
                    spent = spent,
                    budget = budget,
                    percentage = percentage
                )
            } else {
                // Show message when no budget is set
                NoBudgetView()
            }
        }
    }

    /**
     * Composable displaying the budget progress bar and amounts.
     *
     * Shows spent vs budget amounts and a visual progress bar with
     * color coding based on the spending percentage.
     *
     * @param spent The amount spent this week
     * @param budget The total weekly budget
     * @param percentage The percentage of budget spent
     */
    @Composable
    private fun BudgetProgressView(
        spent: Double,
        budget: Double,
        percentage: Double
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            // Amount display (Spent / Budget)
            Text(
                text = "${formatAmount(spent)} / ${formatAmount(budget)}",
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.size(8.dp))

            // Progress bar with side-by-side fill and empty portions
            val progressColor = getProgressColor(percentage)
            val fillFraction = (percentage / 100.0).coerceIn(0.0, 1.0)
            val barWidth = 140.dp
            val fillWidth = barWidth * fillFraction.toFloat()
            val emptyWidth = barWidth * (1.0f - fillFraction.toFloat())

            Row(
                modifier = GlanceModifier
                    .width(barWidth)
                    .height(12.dp)
            ) {
                // Filled portion
                if (fillFraction > 0.0) {
                    Box(
                        modifier = GlanceModifier
                            .width(fillWidth)
                            .height(12.dp)
                            .background(progressColor),
                        content = {}
                    )
                }

                // Empty portion
                if (fillFraction < 1.0) {
                    Box(
                        modifier = GlanceModifier
                            .width(emptyWidth)
                            .height(12.dp)
                            .background(ColorProvider(Color(0xFFE0E0E0))),
                        content = {}
                    )
                }
            }

            Spacer(modifier = GlanceModifier.size(6.dp))

            // Percentage text with color coding
            val percentageColor = getProgressColor(percentage)
            val statusText = when {
                percentage >= 100.0 -> "Over budget (${formatPercentage(percentage)})"
                percentage >= 80.0 -> "Approaching limit (${formatPercentage(percentage)})"
                else -> "${formatPercentage(percentage)} spent"
            }

            Text(
                text = statusText,
                style = TextStyle(
                    color = percentageColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    /**
     * Composable displaying a message when no budget is configured.
     *
     * Prompts the user to configure their budget to start tracking
     * weekly spending progress.
     */
    @Composable
    private fun NoBudgetView() {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No budget set",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.size(4.dp))

            Text(
                text = "Configure widget to start tracking",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Determines the color for the progress bar based on spending percentage.
     *
     * Color coding logic:
     * - Green: < 80% (on track)
     * - Yellow: 80-100% (approaching limit)
     * - Red: > 100% (over budget)
     *
     * @param percentage The spending percentage
     * @return ColorProvider for the progress bar
     */
    private fun getProgressColor(percentage: Double): ColorProvider {
        return when {
            percentage >= 100.0 -> ColorProvider(Color(0xFFE53935)) // Red - over budget
            percentage >= 80.0 -> ColorProvider(Color(0xFFFFA726)) // Yellow/Orange - approaching limit
            else -> ColorProvider(Color(0xFF43A047)) // Green - on track
        }
    }

    /**
     * Formats the amount with currency symbol and proper number formatting.
     *
     * Examples:
     * - formatAmount(5000.0) -> "₹5,000"
     * - formatAmount(12345.50) -> "₹12,345.50"
     * - formatAmount(0.0) -> "₹0"
     *
     * @param amount The amount to format
     * @return Formatted amount string with currency symbol
     */
    private fun formatAmount(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
        return "₹${formatter.format(amount)}"
    }

    /**
     * Formats the percentage value for display.
     *
     * Examples:
     * - formatPercentage(45.5) -> "46%"
     * - formatPercentage(99.9) -> "100%"
     * - formatPercentage(120.0) -> "120%"
     *
     * @param percentage The percentage value
     * @return Formatted percentage string
     */
    private fun formatPercentage(percentage: Double): String {
        return "${percentage.toInt()}%"
    }
}
