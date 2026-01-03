package com.example.kanakku.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.BudgetProgress
import java.util.*

/**
 * Card component showing overall budget status for dashboard display.
 *
 * Features:
 * - Shows spent vs budget amount
 * - Visual progress bar with color coding (green/yellow/red)
 * - Remaining amount display
 * - Percentage spent text
 * - Tap to navigate to budget screen
 *
 * @param budgetProgress The budget progress data to display
 * @param modifier Modifier for customizing the component's appearance
 * @param onNavigateToBudget Callback invoked when the card is tapped
 */
@Composable
fun BudgetOverviewCard(
    budgetProgress: BudgetProgress?,
    modifier: Modifier = Modifier,
    onNavigateToBudget: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToBudget),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Monthly Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (budgetProgress != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View budget details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (budgetProgress != null) {
                // Spent and Budget amounts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${formatAmount(budgetProgress.spent)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Budget",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${formatAmount(budgetProgress.limit)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress bar with color coding
                BudgetProgressBar(
                    budgetProgress = budgetProgress,
                    showPercentageText = true,
                    showRemainingAmount = true
                )
            } else {
                // No budget set state
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No budget set for this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap to set your monthly budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Compact version of BudgetOverviewCard for use in constrained spaces.
 *
 * Shows essential information only: percentage and status with minimal padding.
 *
 * @param budgetProgress The budget progress data to display
 * @param modifier Modifier for customizing the component's appearance
 * @param onNavigateToBudget Callback invoked when the card is tapped
 */
@Composable
fun BudgetOverviewCardCompact(
    budgetProgress: BudgetProgress?,
    modifier: Modifier = Modifier,
    onNavigateToBudget: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToBudget),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        if (budgetProgress != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "${String.format(Locale.getDefault(), "%.0f", budgetProgress.percentage)}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = getBudgetStatusColor(budgetProgress.status)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                BudgetProgressBarCompact(
                    budgetProgress = budgetProgress,
                    height = 4.dp
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Set monthly budget",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Set budget",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Helper functions

/**
 * Formats an amount for display with appropriate suffixes (K for thousands, L for lakhs).
 *
 * @param amount The amount to format
 * @return Formatted string representation of the amount
 */
private fun formatAmount(amount: Double): String {
    return when {
        amount >= 100000 -> String.format(Locale.getDefault(), "%.1fL", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1000)
        else -> String.format(Locale.getDefault(), "%.0f", amount)
    }
}

/**
 * Returns the appropriate color based on budget status.
 *
 * @param status The budget status (UNDER_BUDGET, APPROACHING, or EXCEEDED)
 * @return The color to use for status indicators
 */
private fun getBudgetStatusColor(status: com.example.kanakku.data.model.BudgetStatus): Color {
    return when (status) {
        com.example.kanakku.data.model.BudgetStatus.UNDER_BUDGET -> Color(0xFF2E7D32)  // Green
        com.example.kanakku.data.model.BudgetStatus.APPROACHING -> Color(0xFFF57F17)   // Amber/Yellow
        com.example.kanakku.data.model.BudgetStatus.EXCEEDED -> Color(0xFFC62828)      // Red
    }
}
