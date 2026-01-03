package com.example.kanakku.ui.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Header component showing summary statistics for recurring transactions.
 * Displays total monthly recurring expenses, subscription count, and upcoming payment count.
 *
 * @param totalMonthlyRecurring Estimated total monthly recurring expenses
 * @param subscriptionCount Number of subscription-type recurring transactions
 * @param upcomingCount Number of recurring transactions expected in next 7 days
 * @param modifier Optional modifier for custom styling
 */
@Composable
fun RecurringSummaryHeader(
    totalMonthlyRecurring: Double,
    subscriptionCount: Int,
    upcomingCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Title
        Text(
            text = "Monthly Recurring Expenses",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Summary cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total monthly recurring card
            SummaryCard(
                title = "Total Monthly",
                value = "₹${formatAmount(totalMonthlyRecurring)}",
                color = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )

            // Subscription count card
            SummaryCard(
                title = "Subscriptions",
                value = "$subscriptionCount",
                color = Color(0xFF1565C0),
                modifier = Modifier.weight(1f)
            )

            // Upcoming count card
            SummaryCard(
                title = "Upcoming",
                value = "$upcomingCount",
                color = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual summary card displaying a metric.
 * Shows a title and value with colored styling.
 *
 * @param title The label for this metric
 * @param value The value to display
 * @param color The color theme for this card
 * @param modifier Optional modifier for custom styling
 */
@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Formats a monetary amount for display.
 * Adds thousand separators and shows appropriate precision.
 *
 * @param amount The amount to format
 * @return Formatted amount string (e.g., "1,234.50" or "12.5K" or "1.2L")
 */
private fun formatAmount(amount: Double): String {
    return when {
        amount >= 100000 -> String.format(Locale.getDefault(), "%.1fL", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1000)
        else -> String.format(Locale.getDefault(), "%.0f", amount)
    }
}
