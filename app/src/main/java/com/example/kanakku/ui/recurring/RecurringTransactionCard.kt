package com.example.kanakku.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.RecurringTransaction
import com.example.kanakku.data.model.RecurringType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card component displaying a recurring transaction pattern.
 * Shows merchant, amount, frequency, next expected date, and action buttons.
 *
 * @param recurringTransaction The recurring transaction data to display
 * @param onConfirm Callback when user confirms this recurring pattern
 * @param onRemove Callback when user removes this recurring pattern
 * @param modifier Optional modifier for custom styling
 */
@Composable
fun RecurringTransactionCard(
    recurringTransaction: RecurringTransaction,
    onConfirm: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (recurringTransaction.isUserConfirmed) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Merchant name and type icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Merchant Pattern
                    Text(
                        text = recurringTransaction.merchantPattern,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Frequency badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF1565C0).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = recurringTransaction.frequency.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1565C0)
                        )
                    }
                }

                // Type badge with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = recurringTransaction.type.icon,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = recurringTransaction.type.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount and dates row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${formatAmount(recurringTransaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Next Expected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(recurringTransaction.nextExpected),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction count and last occurrence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${recurringTransaction.transactionIds.size} transaction${if (recurringTransaction.transactionIds.size > 1) "s" else ""} found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Last: ${formatDate(recurringTransaction.lastOccurrence)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action buttons (only show if not confirmed)
            if (!recurringTransaction.isUserConfirmed) {
                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Remove button
                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFC62828)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove")
                    }

                    // Confirm button
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirm")
                    }
                }
            } else {
                // Confirmed badge
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirmed",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "Confirmed Recurring",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats a monetary amount for display.
 * Adds thousand separators and shows 2 decimal places.
 *
 * @param amount The amount to format
 * @return Formatted amount string (e.g., "1,234.50")
 */
private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.2f", amount)
}

/**
 * Formats a timestamp as a readable date string.
 *
 * @param timestamp The timestamp in milliseconds
 * @return Formatted date string (e.g., "15 Jan 2024")
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
