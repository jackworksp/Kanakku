package com.example.kanakku.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.kanakku.widget.data.WidgetDataRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small widget (2x1) displaying today's total spending.
 *
 * This widget provides a quick glance at the user's spending for the current day.
 * It shows:
 * - "Today's Spending" label
 * - Formatted currency amount (e.g., ₹1,234.00)
 * - Last updated timestamp
 *
 * The widget supports both light and dark themes and updates periodically
 * to show the latest spending data.
 *
 * Size: 2x1 cells (approximately 110dp x 40dp)
 * Update frequency: Managed by WorkManager (hourly)
 *
 * @see com.example.kanakku.widget.TodaySpendingWidgetReceiver
 * @see com.example.kanakku.widget.data.WidgetDataRepository
 */
class TodaySpendingWidget : GlanceAppWidget() {

    /**
     * Provides the widget content using Jetpack Glance composables.
     *
     * This method is called when the widget needs to be rendered.
     * It fetches the latest data from the repository and composes the UI.
     *
     * @param context The context for the widget
     * @param glanceId Unique identifier for this widget instance
     */
    override suspend fun provideGlance(context: Context, glanceId: GlanceId) {
        // Fetch today's spending data from repository
        val repository = WidgetDataRepository(context)
        val spendingData = repository.getTodaySpending()

        provideContent {
            GlanceTheme {
                TodaySpendingContent(
                    amount = spendingData.todayTotal,
                    currency = spendingData.currency,
                    lastUpdated = spendingData.lastUpdated
                )
            }
        }
    }

    /**
     * Composable function that defines the widget's UI layout.
     *
     * Displays today's spending in a compact, readable format suitable for
     * a small 2x1 widget size. The layout adapts to both light and dark themes.
     *
     * @param amount The total spending amount for today
     * @param currency The currency symbol (default: ₹)
     * @param lastUpdated Timestamp of the last data update
     */
    @Composable
    private fun TodaySpendingContent(
        amount: Double,
        currency: String,
        lastUpdated: Long
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Widget title
            Text(
                text = "Today's Spending",
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.size(4.dp))

            // Amount display
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatAmount(amount, currency),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.size(2.dp))

            // Last updated timestamp
            Text(
                text = "Updated: ${formatTime(lastUpdated)}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 9.sp
                )
            )
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Formats the amount with currency symbol and proper number formatting.
     *
     * Examples:
     * - formatAmount(1234.50, "₹") -> "₹1,234.50"
     * - formatAmount(0.0, "₹") -> "₹0.00"
     *
     * @param amount The amount to format
     * @param currency The currency symbol
     * @return Formatted amount string
     */
    private fun formatAmount(amount: Double, currency: String): String {
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$currency${formatter.format(amount)}"
    }

    /**
     * Formats the timestamp to a short time string.
     *
     * Examples:
     * - "10:30 AM"
     * - "03:45 PM"
     *
     * @param timestamp The timestamp in milliseconds
     * @return Formatted time string
     */
    private fun formatTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}
