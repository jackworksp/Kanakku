package com.example.kanakku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.category.CategorySuggestion
import com.example.kanakku.data.category.ConfidenceLevel
import com.example.kanakku.data.model.ParsedTransaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bottom sheet displaying smart category suggestions for an uncategorized transaction.
 *
 * Shows the transaction details followed by a list of suggested categories ranked by
 * confidence level. Users can:
 * - Apply the suggested category to this single transaction
 * - Apply the suggested category to all similar transactions (same merchant)
 * - Dismiss the suggestions
 *
 * Each suggestion displays:
 * - Category icon and color
 * - Category name
 * - Confidence level badge (HIGH, MEDIUM, LOW)
 * - Reason for the suggestion
 *
 * @param transaction The uncategorized transaction to show suggestions for
 * @param suggestions List of category suggestions sorted by confidence (highest first)
 * @param onApplyToSingle Callback invoked when applying a suggestion to this transaction only
 * @param onApplyToAllSimilar Callback invoked when applying a suggestion to all similar transactions
 * @param onDismiss Callback invoked when the sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySuggestionsSheet(
    transaction: ParsedTransaction,
    suggestions: List<CategorySuggestion>,
    onApplyToSingle: (CategorySuggestion) -> Unit,
    onApplyToAllSimilar: (CategorySuggestion) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSuggestion by remember { mutableStateOf<CategorySuggestion?>(null) }
    var showApplyOptionsDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Suggestions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction details card
            TransactionDetailsCard(transaction = transaction)

            Spacer(modifier = Modifier.height(16.dp))

            // Suggestions section
            if (suggestions.isNotEmpty()) {
                Text(
                    text = "Suggested Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(suggestions) { suggestion ->
                        CategorySuggestionItem(
                            suggestion = suggestion,
                            onClick = {
                                selectedSuggestion = suggestion
                                showApplyOptionsDialog = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                // Empty state
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🤔",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No suggestions available",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Categorize this transaction manually to help improve suggestions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Dismiss button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("Dismiss")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Apply options dialog
    if (showApplyOptionsDialog && selectedSuggestion != null) {
        ApplyOptionsDialog(
            suggestion = selectedSuggestion!!,
            transaction = transaction,
            onApplyToSingle = {
                onApplyToSingle(selectedSuggestion!!)
                showApplyOptionsDialog = false
                onDismiss()
            },
            onApplyToAllSimilar = {
                onApplyToAllSimilar(selectedSuggestion!!)
                showApplyOptionsDialog = false
                onDismiss()
            },
            onDismiss = {
                showApplyOptionsDialog = false
                selectedSuggestion = null
            }
        )
    }
}

/**
 * Card displaying transaction details for context.
 *
 * Shows merchant, amount, and date to help the user understand which transaction
 * is being categorized.
 *
 * @param transaction The transaction to display
 */
@Composable
private fun TransactionDetailsCard(transaction: ParsedTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Transaction type and amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.merchant ?: "Unknown Merchant",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                val formattedAmount = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                    .format(transaction.amount)
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type.name == "DEBIT") {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            Text(
                text = dateFormat.format(Date(transaction.date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )

            // Location (if available)
            transaction.location?.let { location ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 $location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Composable for displaying a single category suggestion item.
 *
 * Shows the category icon, name, confidence level badge, and reason for the suggestion.
 * Clickable to trigger the apply options dialog.
 *
 * @param suggestion The category suggestion to display
 * @param onClick Callback invoked when the item is clicked
 */
@Composable
private fun CategorySuggestionItem(
    suggestion: CategorySuggestion,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = suggestion.category.color.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = suggestion.category.icon,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Category details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = suggestion.category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Confidence badge
                    ConfidenceBadge(confidenceLevel = suggestion.confidenceLevel)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Suggestion reason
                Text(
                    text = suggestion.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Confidence percentage
                Text(
                    text = "${(suggestion.confidence * 100).toInt()}% match",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Badge displaying the confidence level of a suggestion.
 *
 * Uses different colors to indicate HIGH, MEDIUM, or LOW confidence.
 *
 * @param confidenceLevel The confidence level to display
 */
@Composable
private fun ConfidenceBadge(confidenceLevel: ConfidenceLevel) {
    val (backgroundColor, textColor, label) = when (confidenceLevel) {
        ConfidenceLevel.HIGH -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.primary,
            "HIGH"
        )
        ConfidenceLevel.MEDIUM -> Triple(
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.tertiary,
            "MEDIUM"
        )
        ConfidenceLevel.LOW -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "LOW"
        )
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Dialog for choosing how to apply the selected category suggestion.
 *
 * Offers two options:
 * 1. Apply to this transaction only
 * 2. Apply to all similar transactions (same merchant)
 *
 * @param suggestion The selected category suggestion
 * @param transaction The transaction being categorized
 * @param onApplyToSingle Callback for applying to single transaction
 * @param onApplyToAllSimilar Callback for applying to all similar transactions
 * @param onDismiss Callback to dismiss the dialog
 */
@Composable
private fun ApplyOptionsDialog(
    suggestion: CategorySuggestion,
    transaction: ParsedTransaction,
    onApplyToSingle: () -> Unit,
    onApplyToAllSimilar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = suggestion.category.color.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = suggestion.category.icon,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        },
        title = {
            Text(
                text = "Apply \"${suggestion.category.name}\"?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "How would you like to apply this category?",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Option 1: Apply to single
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onApplyToSingle),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Apply to this transaction only",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Categorize only this transaction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Option 2: Apply to all similar
                transaction.merchant?.let { merchant ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onApplyToAllSimilar),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Apply to all similar transactions",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Categorize all transactions from \"$merchant\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
