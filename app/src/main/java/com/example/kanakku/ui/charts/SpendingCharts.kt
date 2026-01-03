package com.example.kanakku.ui.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kanakku.data.model.CategoryTotal
import com.example.kanakku.data.model.DaySpending
import com.example.kanakku.data.model.TrendPoint
import java.util.Locale

@Composable
fun CategoryPieChart(
    categoryTotals: List<CategoryTotal>,
    modifier: Modifier = Modifier
) {
    if (categoryTotals.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No spending data", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "pie_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Donut Chart
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val total = categoryTotals.sumOf { it.totalAmount }
                var startAngle = -90f
                val strokeWidth = 32.dp.toPx()

                categoryTotals.forEach { categoryTotal ->
                    val sweepAngle = ((categoryTotal.totalAmount / total) * 360f * animatedProgress).toFloat()
                    drawArc(
                        color = categoryTotal.category.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    )
                    startAngle += sweepAngle
                }
            }

            // Center text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "₹${formatCompact(categoryTotals.sumOf { it.totalAmount })}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Legend
        Column(
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categoryTotals.take(5).forEach { categoryTotal ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(categoryTotal.category.color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = categoryTotal.category.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.0f", categoryTotal.percentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SpendingBarChart(
    dailySpending: List<DaySpending>,
    modifier: Modifier = Modifier
) {
    if (dailySpending.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No spending data", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "bar_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val maxSpending = dailySpending.maxOfOrNull { it.spent } ?: 1.0
    val barColor = MaterialTheme.colorScheme.error
    val displayData = dailySpending.takeLast(7) // Show last 7 days

    Column(modifier = modifier.fillMaxWidth()) {
        // Y-axis label
        Text(
            text = "₹${formatCompact(maxSpending)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Chart area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            displayData.forEach { dayData ->
                val barHeight = if (maxSpending > 0) {
                    ((dayData.spent / maxSpending) * 130 * animatedProgress).dp
                } else 0.dp

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // Amount label on top
                    if (dayData.spent > 0) {
                        Text(
                            text = formatCompact(dayData.spent),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Bar
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(barHeight.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (dayData.spent > 0) barColor else barColor.copy(alpha = 0.2f)
                            )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Day label
                    Text(
                        text = dayData.dayLabel.take(6),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SpendingLineChart(
    trendPoints: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    if (trendPoints.isEmpty() || trendPoints.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Not enough data for trend", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "line_animation"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val maxAmount = trendPoints.maxOfOrNull { it.amount } ?: 1.0
    val minAmount = trendPoints.minOfOrNull { it.amount } ?: 0.0
    val range = (maxAmount - minAmount).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        // Y-axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "₹${formatCompact(maxAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "₹${formatCompact(minAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val width = size.width
            val height = size.height
            val pointCount = trendPoints.size
            val stepX = width / (pointCount - 1).coerceAtLeast(1)

            // Create path for line
            val linePath = Path()
            val fillPath = Path()

            trendPoints.forEachIndexed { index, point ->
                val x = index * stepX
                val normalizedY = ((point.amount - minAmount) / range).toFloat()
                val y = height - (normalizedY * height * animatedProgress)

                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            // Complete fill path
            fillPath.lineTo(width, height)
            fillPath.close()

            // Draw fill
            drawPath(
                path = fillPath,
                color = fillColor
            )

            // Draw line
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw points
            trendPoints.forEachIndexed { index, point ->
                val x = index * stepX
                val normalizedY = ((point.amount - minAmount) / range).toFloat()
                val y = height - (normalizedY * height * animatedProgress)

                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = MaterialTheme.colorScheme.surface,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labelsToShow = listOf(
                trendPoints.first().label,
                trendPoints[trendPoints.size / 2].label,
                trendPoints.last().label
            )
            labelsToShow.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatCompact(amount: Double): String {
    return when {
        amount >= 100000 -> String.format(Locale.getDefault(), "%.1fL", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1000)
        else -> String.format(Locale.getDefault(), "%.0f", amount)
    }
}
