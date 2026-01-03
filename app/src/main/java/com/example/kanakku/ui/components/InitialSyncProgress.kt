package com.example.kanakku.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.max

/**
 * A dedicated component that displays progress during the initial full history sync.
 *
 * This component provides visual feedback during the first-time SMS parsing operation,
 * which may take several seconds or minutes depending on the size of the SMS history.
 *
 * Features:
 * - Linear progress indicator with animated percentage
 * - Count display: "Processing X of Y messages"
 * - Status message showing current operation
 * - Estimated time remaining based on current progress rate
 * - Cancel button to abort long-running syncs
 * - Clean Material 3 design with proper spacing and colors
 *
 * @param progress The current progress count (number of items processed)
 * @param total The total number of items to process
 * @param statusMessage Optional status message describing the current operation
 * @param onCancel Optional callback invoked when the user requests to cancel the sync
 * @param modifier Optional modifier for customizing the component appearance and position
 */
@Composable
fun InitialSyncProgress(
    progress: Int,
    total: Int,
    statusMessage: String? = null,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Track start time for estimated time remaining calculation
    val startTime = remember { System.currentTimeMillis() }
    var estimatedTimeRemaining by remember { mutableStateOf<String?>(null) }

    // Calculate progress percentage
    val progressPercentage = if (total > 0) {
        (progress.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Calculate estimated time remaining
    LaunchedEffect(progress, total) {
        if (progress > 0 && total > 0 && progress < total) {
            val elapsedMillis = System.currentTimeMillis() - startTime
            val avgTimePerItem = elapsedMillis.toDouble() / progress
            val remainingItems = total - progress
            val estimatedRemainingMillis = (avgTimePerItem * remainingItems).toLong()

            estimatedTimeRemaining = when {
                estimatedRemainingMillis < 1000 -> "Less than a second"
                estimatedRemainingMillis < 60_000 -> {
                    val seconds = max(1, estimatedRemainingMillis / 1000)
                    "${seconds}s remaining"
                }
                estimatedRemainingMillis < 3_600_000 -> {
                    val minutes = max(1, estimatedRemainingMillis / 60_000)
                    val seconds = (estimatedRemainingMillis % 60_000) / 1000
                    if (seconds > 0) "${minutes}m ${seconds}s remaining" else "${minutes}m remaining"
                }
                else -> {
                    val hours = estimatedRemainingMillis / 3_600_000
                    val minutes = (estimatedRemainingMillis % 3_600_000) / 60_000
                    if (minutes > 0) "${hours}h ${minutes}m remaining" else "${hours}h remaining"
                }
            }
        }
    }

    // Animated rotation for sync icon
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_rotation"
    )

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Sync icon with rotation animation
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Syncing",
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = MaterialTheme.colorScheme.primary
            )

            // Title
            Text(
                text = "Syncing Transaction History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Status message
            if (statusMessage != null) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Progress indicator and percentage
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Linear progress bar
                LinearProgressIndicator(
                    progress = { progressPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                // Progress text row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Count: "Processing X of Y messages"
                    Text(
                        text = if (total > 0) {
                            "Processing $progress of $total messages"
                        } else {
                            "Preparing..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    // Percentage
                    Text(
                        text = "${(progressPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Estimated time remaining
            if (estimatedTimeRemaining != null && progress < total) {
                Text(
                    text = estimatedTimeRemaining!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Informational message
            Text(
                text = "This is a one-time sync of your complete SMS history. Future syncs will be much faster.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Cancel button (if callback provided)
            if (onCancel != null) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Sync")
                }
            }
        }
    }
}

/**
 * A compact variant of the initial sync progress indicator for use in constrained spaces
 * such as app bars or small cards.
 *
 * @param progress The current progress count
 * @param total The total number of items to process
 * @param modifier Optional modifier for customizing the component appearance and position
 */
@Composable
fun CompactSyncProgress(
    progress: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val progressPercentage = if (total > 0) {
        (progress.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            progress = { progressPercentage },
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Syncing...",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "$progress of $total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
