package com.example.kanakku.ui.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.RecurringTransaction
import java.text.SimpleDateFormat
import java.util.*

/**
 * List component displaying upcoming recurring payments sorted by next expected date.
 * Shows recurring transactions that are expected to occur in the near future.
 *
 * @param recurringTransactions List of recurring transactions to display
 * @param onConfirm Callback when user confirms a recurring pattern
 * @param onRemove Callback when user removes a recurring pattern
 * @param modifier Optional modifier for custom styling
 * @param daysAhead Number of days ahead to filter (default 30 days)
 */
@Composable
fun UpcomingPaymentsList(
    recurringTransactions: List<RecurringTransaction>,
    onConfirm: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    daysAhead: Int = 30
) {
    val now = System.currentTimeMillis()
    val cutoffDate = now + (daysAhead * 24 * 60 * 60 * 1000L)

    // Filter and sort upcoming payments
    val upcomingPayments = recurringTransactions
        .filter { it.nextExpected in now..cutoffDate }
        .sortedBy { it.nextExpected }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Payments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${upcomingPayments.size}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (upcomingPayments.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No upcoming payments",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Payments expected in the next $daysAhead days will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // List of upcoming payments with timeline indicators
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(upcomingPayments) { transaction ->
                    Column {
                        // Timeline indicator showing days until payment
                        val daysUntil = calculateDaysUntil(transaction.nextExpected)
                        TimelineIndicator(
                            daysUntil = daysUntil,
                            nextExpectedDate = transaction.nextExpected
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Transaction card
                        RecurringTransactionCard(
                            recurringTransaction = transaction,
                            onConfirm = { onConfirm(transaction.id) },
                            onRemove = { onRemove(transaction.id) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

/**
 * Timeline indicator showing how many days until the payment.
 * Provides visual context for payment urgency.
 *
 * @param daysUntil Number of days until payment is expected
 * @param nextExpectedDate Timestamp of next expected payment
 */
@Composable
private fun TimelineIndicator(
    daysUntil: Int,
    nextExpectedDate: Long
) {
    val (text, color) = when {
        daysUntil < 0 -> "Overdue" to Color(0xFFC62828) // Red for overdue
        daysUntil == 0 -> "Due today" to Color(0xFFEF6C00) // Orange for today
        daysUntil == 1 -> "Due tomorrow" to Color(0xFFF57C00) // Orange for tomorrow
        daysUntil <= 7 -> "Due in $daysUntil days" to Color(0xFF1976D2) // Blue for this week
        else -> "Due ${formatDate(nextExpectedDate)}" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Timeline dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = color,
                modifier = Modifier.size(8.dp)
            ) {}
        }

        // Timeline text
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/**
 * Calculates the number of days until a future date.
 *
 * @param timestamp Future timestamp in milliseconds
 * @return Number of days until the timestamp (negative if in the past)
 */
private fun calculateDaysUntil(timestamp: Long): Int {
    val now = System.currentTimeMillis()
    val diff = timestamp - now
    return (diff / (24 * 60 * 60 * 1000)).toInt()
}

/**
 * Formats a timestamp as a readable date string.
 *
 * @param timestamp The timestamp in milliseconds
 * @return Formatted date string (e.g., "15 Jan")
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
