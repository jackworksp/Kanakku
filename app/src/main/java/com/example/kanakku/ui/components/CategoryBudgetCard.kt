package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.BudgetStatus
import com.example.kanakku.data.model.CategoryBudgetProgress
import java.util.*

/**
 * Card component showing individual category budget progress.
 *
 * Displays budget tracking information for a specific spending category with:
 * - Category icon and name
 * - Visual progress bar with color coding (green/yellow/red based on status)
 * - Spent and budget limit amounts
 * - Edit button for quick access to modify budget
 *
 * @param categoryBudgetProgress The category budget progress data to display
 * @param modifier Modifier for customizing the component's appearance
 * @param onEdit Callback invoked when the edit button is tapped
 */
@Composable
fun CategoryBudgetCard(
    categoryBudgetProgress: CategoryBudgetProgress,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with category info and edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon and name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = categoryBudgetProgress.category.icon,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = categoryBudgetProgress.category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Edit button
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit budget",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Spent and Budget amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${formatAmount(categoryBudgetProgress.progress.spent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = getBudgetStatusColor(categoryBudgetProgress.progress.status)
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
                        text = "₹${formatAmount(categoryBudgetProgress.progress.limit)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar with color coding
            BudgetProgressBar(
                budgetProgress = categoryBudgetProgress.progress,
                showPercentageText = true,
                showRemainingAmount = true
            )
        }
    }
}

/**
 * Compact version of CategoryBudgetCard for use in constrained spaces.
 *
 * Shows essential information only: category, percentage, and mini progress bar.
 *
 * @param categoryBudgetProgress The category budget progress data to display
 * @param modifier Modifier for customizing the component's appearance
 * @param onEdit Callback invoked when the card is tapped
 */
@Composable
fun CategoryBudgetCardCompact(
    categoryBudgetProgress: CategoryBudgetProgress,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onEdit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Category and percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = categoryBudgetProgress.category.icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = categoryBudgetProgress.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "${String.format(Locale.getDefault(), "%.0f", categoryBudgetProgress.progress.percentage)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = getBudgetStatusColor(categoryBudgetProgress.progress.status)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compact progress bar
            BudgetProgressBarCompact(
                budgetProgress = categoryBudgetProgress.progress,
                height = 4.dp
            )
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
private fun getBudgetStatusColor(status: BudgetStatus): Color {
    return when (status) {
        BudgetStatus.UNDER_BUDGET -> Color(0xFF2E7D32)  // Green
        BudgetStatus.APPROACHING -> Color(0xFFF57F17)   // Amber/Yellow
        BudgetStatus.EXCEEDED -> Color(0xFFC62828)      // Red
    }
}
