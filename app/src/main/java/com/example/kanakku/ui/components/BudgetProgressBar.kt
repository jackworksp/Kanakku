package com.example.kanakku.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.BudgetProgress
import com.example.kanakku.data.model.BudgetStatus
import java.util.*

/**
 * A reusable progress bar component that displays budget spending progress
 * with dynamic color coding based on budget status.
 *
 * Color coding:
 * - Green: Under 80% spent (UNDER_BUDGET)
 * - Yellow/Amber: 80-100% spent (APPROACHING)
 * - Red: Over 100% spent (EXCEEDED)
 *
 * Features:
 * - Smooth animated progress changes
 * - Percentage display
 * - Remaining amount display
 * - Customizable appearance
 *
 * @param budgetProgress The budget progress data containing spent, limit, and status
 * @param modifier Modifier for customizing the component's appearance
 * @param showPercentageText Whether to display the percentage text below the bar (default: true)
 * @param showRemainingAmount Whether to display the remaining amount text (default: true)
 * @param height The height of the progress bar (default: 8.dp)
 * @param animationDuration Duration of the progress animation in milliseconds (default: 600ms)
 */
@Composable
fun BudgetProgressBar(
    budgetProgress: BudgetProgress,
    modifier: Modifier = Modifier,
    showPercentageText: Boolean = true,
    showRemainingAmount: Boolean = true,
    height: Dp = 8.dp,
    animationDuration: Int = 600
) {
    val progressValue = (budgetProgress.percentage / 100.0).toFloat().coerceIn(0f, 1f)

    // Animate progress changes smoothly
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        animationSpec = tween(durationMillis = animationDuration),
        label = "BudgetProgressAnimation"
    )

    val statusColor = getBudgetStatusColor(budgetProgress.status)

    Column(modifier = modifier) {
        // Progress bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            color = statusColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Text information below the bar
        if (showPercentageText || showRemainingAmount) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showPercentageText) {
                    Text(
                        text = "${String.format("%.1f", budgetProgress.percentage)}% used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showRemainingAmount) {
                    val remainingColor = if (budgetProgress.remaining >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }

                    Text(
                        text = if (budgetProgress.remaining >= 0) {
                            "₹${formatAmount(budgetProgress.remaining)} left"
                        } else {
                            "₹${formatAmount(-budgetProgress.remaining)} over"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = remainingColor
                    )
                }
            }
        }
    }
}

/**
 * A compact version of BudgetProgressBar that only shows the progress bar
 * without text labels, ideal for use in constrained spaces like list items.
 *
 * @param budgetProgress The budget progress data containing spent, limit, and status
 * @param modifier Modifier for customizing the component's appearance
 * @param height The height of the progress bar (default: 6.dp for compact size)
 * @param animationDuration Duration of the progress animation in milliseconds (default: 600ms)
 */
@Composable
fun BudgetProgressBarCompact(
    budgetProgress: BudgetProgress,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    animationDuration: Int = 600
) {
    BudgetProgressBar(
        budgetProgress = budgetProgress,
        modifier = modifier,
        showPercentageText = false,
        showRemainingAmount = false,
        height = height,
        animationDuration = animationDuration
    )
}

/**
 * A minimal progress bar variant that only shows percentage text,
 * useful for dashboard widgets or summary views.
 *
 * @param budgetProgress The budget progress data containing spent, limit, and status
 * @param modifier Modifier for customizing the component's appearance
 * @param height The height of the progress bar (default: 8.dp)
 * @param animationDuration Duration of the progress animation in milliseconds (default: 600ms)
 */
@Composable
fun BudgetProgressBarWithPercentage(
    budgetProgress: BudgetProgress,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    animationDuration: Int = 600
) {
    BudgetProgressBar(
        budgetProgress = budgetProgress,
        modifier = modifier,
        showPercentageText = true,
        showRemainingAmount = false,
        height = height,
        animationDuration = animationDuration
    )
}

// Helper functions

/**
 * Returns the appropriate color based on budget status.
 *
 * @param status The budget status (UNDER_BUDGET, APPROACHING, or EXCEEDED)
 * @return The color to use for the progress bar
 */
private fun getBudgetStatusColor(status: BudgetStatus): Color {
    return when (status) {
        BudgetStatus.UNDER_BUDGET -> Color(0xFF2E7D32)  // Green
        BudgetStatus.APPROACHING -> Color(0xFFF57F17)   // Amber/Yellow
        BudgetStatus.EXCEEDED -> Color(0xFFC62828)      // Red
    }
}

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
