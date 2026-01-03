package com.example.kanakku.ui.savings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.GoalStatus
import com.example.kanakku.data.model.SavingsGoal
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card component displaying goal information with progress and actions
 *
 * @param goal The savings goal to display
 * @param modifier Modifier for this composable
 * @param onClick Callback when the card is clicked
 * @param onAddContribution Callback when add contribution button is clicked
 */
@Composable
fun GoalCard(
    goal: SavingsGoal,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onAddContribution: () -> Unit = {}
) {
    val statusColor = when (goal.status) {
        GoalStatus.COMPLETED -> Color(0xFF2E7D32)
        GoalStatus.OVERDUE -> Color(0xFFC62828)
        GoalStatus.ACTIVE -> if (goal.isOnTrack) {
            MaterialTheme.colorScheme.primary
        } else {
            Color(0xFFFF8F00)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Icon, Name, and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Goal Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = statusColor.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = goal.icon ?: "🎯",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Goal Name and Target
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Target: ₹${formatAmount(goal.targetAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                GoalStatusBadge(status = goal.status, isOnTrack = goal.isOnTrack)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Section
            LinearGoalProgressIndicator(
                progress = goal.progressPercentage.toFloat(),
                color = statusColor,
                showPercentage = false,
                height = 8.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₹${formatAmount(goal.currentAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
                Text(
                    text = "${(goal.progressPercentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Deadline and Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Deadline Information
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (goal.status) {
                            GoalStatus.COMPLETED -> Icons.Default.CheckCircle
                            GoalStatus.OVERDUE -> Icons.Default.Warning
                            GoalStatus.ACTIVE -> if (goal.isOnTrack) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Warning
                            }
                        },
                        contentDescription = null,
                        tint = when (goal.status) {
                            GoalStatus.COMPLETED -> Color(0xFF2E7D32)
                            GoalStatus.OVERDUE -> Color(0xFFC62828)
                            GoalStatus.ACTIVE -> if (goal.isOnTrack) {
                                Color(0xFF2E7D32)
                            } else {
                                Color(0xFFFF8F00)
                            }
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (goal.status) {
                            GoalStatus.COMPLETED -> "Completed"
                            GoalStatus.OVERDUE -> "Overdue"
                            GoalStatus.ACTIVE -> if (goal.daysRemaining == 0L) {
                                "Due today"
                            } else if (goal.daysRemaining == 1L) {
                                "1 day left"
                            } else {
                                "${goal.daysRemaining} days left"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Add Contribution Button (only for active goals)
                if (goal.status == GoalStatus.ACTIVE) {
                    FilledTonalButton(
                        onClick = onAddContribution,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add contribution",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Deadline Date
            if (goal.status != GoalStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Deadline: ${formatDate(goal.deadline)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                goal.completedAt?.let { completedDate ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Completed on ${formatDate(completedDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

/**
 * Status badge showing the current state of the goal
 *
 * @param status The current goal status
 * @param isOnTrack Whether the goal is on track
 */
@Composable
private fun GoalStatusBadge(
    status: GoalStatus,
    isOnTrack: Boolean
) {
    val (backgroundColor, textColor, text) = when (status) {
        GoalStatus.COMPLETED -> Triple(
            Color(0xFFC8E6C9),
            Color(0xFF2E7D32),
            "Done"
        )
        GoalStatus.OVERDUE -> Triple(
            Color(0xFFFFCDD2),
            Color(0xFFC62828),
            "Overdue"
        )
        GoalStatus.ACTIVE -> if (isOnTrack) {
            Triple(
                Color(0xFFC8E6C9),
                Color(0xFF2E7D32),
                "On Track"
            )
        } else {
            Triple(
                Color(0xFFFFE0B2),
                Color(0xFFFF8F00),
                "Behind"
            )
        }
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Format amount with compact notation for large numbers
 */
private fun formatAmount(amount: Double): String {
    return when {
        amount >= 100000 -> String.format(Locale.getDefault(), "%.1fL", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1000)
        else -> String.format(Locale.getDefault(), "%.0f", amount)
    }
}

/**
 * Format date to readable string
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
