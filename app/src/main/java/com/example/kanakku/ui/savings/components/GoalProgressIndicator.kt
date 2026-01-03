package com.example.kanakku.ui.savings.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Circular progress indicator showing goal completion percentage with animated fill
 *
 * @param progress Completion percentage from 0.0 to 1.0
 * @param modifier Modifier for this composable
 * @param size Size of the circular indicator
 * @param strokeWidth Width of the progress arc
 * @param color Color of the progress arc
 * @param backgroundColor Color of the background arc
 * @param showPercentage Whether to show the percentage text in the center
 * @param animationDuration Duration of the fill animation in milliseconds
 */
@Composable
fun CircularGoalProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    showPercentage: Boolean = true,
    animationDuration: Int = 1000
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "circular_progress_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size
            val strokeWidthPx = strokeWidth.toPx()

            // Background arc
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                size = Size(canvasSize.width - strokeWidthPx, canvasSize.height - strokeWidthPx),
                topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            )

            // Progress arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                size = Size(canvasSize.width - strokeWidthPx, canvasSize.height - strokeWidthPx),
                topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            )
        }

        // Center percentage text
        if (showPercentage) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value / 6).sp.coerceAtLeast(16.sp)
                )
            }
        }
    }
}

/**
 * Linear progress indicator showing goal completion percentage with animated fill
 *
 * @param progress Completion percentage from 0.0 to 1.0
 * @param modifier Modifier for this composable
 * @param height Height of the progress bar
 * @param color Color of the progress fill
 * @param backgroundColor Color of the background
 * @param showPercentage Whether to show the percentage text
 * @param animationDuration Duration of the fill animation in milliseconds
 */
@Composable
fun LinearGoalProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    showPercentage: Boolean = true,
    animationDuration: Int = 1000
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "linear_progress_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(backgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(height / 2))
                    .background(color)
            )
        }

        if (showPercentage) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Goal progress indicator with percentage display and amount information
 *
 * @param currentAmount Current saved amount
 * @param targetAmount Target amount to reach
 * @param modifier Modifier for this composable
 * @param isCircular Whether to use circular (true) or linear (false) indicator
 * @param color Color of the progress indicator
 * @param showAmounts Whether to show current and target amounts
 */
@Composable
fun GoalProgressIndicator(
    currentAmount: Double,
    targetAmount: Double,
    modifier: Modifier = Modifier,
    isCircular: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    showAmounts: Boolean = true
) {
    val progress = if (targetAmount > 0) {
        (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = modifier,
        horizontalAlignment = if (isCircular) Alignment.CenterHorizontally else Alignment.Start
    ) {
        if (isCircular) {
            CircularGoalProgressIndicator(
                progress = progress,
                color = color,
                showPercentage = true
            )
        } else {
            LinearGoalProgressIndicator(
                progress = progress,
                color = color,
                showPercentage = true
            )
        }

        if (showAmounts) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₹${formatAmount(currentAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹${formatAmount(targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
