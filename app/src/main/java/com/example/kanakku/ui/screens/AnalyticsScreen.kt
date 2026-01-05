package com.example.kanakku.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.*
import com.example.kanakku.domain.analytics.AnalyticsCalculator
import com.example.kanakku.ui.charts.CategoryPieChart
import com.example.kanakku.ui.charts.SpendingBarChart
import com.example.kanakku.ui.charts.SpendingLineChart
import com.example.kanakku.ui.components.BentoCard
import com.example.kanakku.ui.components.BentoFeatureCard
import com.example.kanakku.ui.components.BentoGlassCard
import com.example.kanakku.ui.components.BentoMiniStat
import com.example.kanakku.ui.components.BentoSectionHeader
import com.example.kanakku.ui.components.BentoStatCard
import com.example.kanakku.ui.theme.BentoGradientEnd
import com.example.kanakku.ui.theme.BentoGradientStart
import com.example.kanakku.ui.theme.CreditColor
import com.example.kanakku.ui.theme.DebitColor
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    transactions: List<ParsedTransaction>,
    categoryMap: Map<Long, Category>
) {
    var selectedPeriod by remember { mutableStateOf(TimePeriod.MONTH) }
    val calculator = remember { AnalyticsCalculator() }

    val summary = remember(transactions, categoryMap, selectedPeriod) {
        calculator.calculatePeriodSummary(transactions, categoryMap, selectedPeriod)
    }

    val categoryBreakdown = remember(transactions, categoryMap, selectedPeriod) {
        val now = System.currentTimeMillis()
        val startTime = now - (selectedPeriod.days * 24 * 60 * 60 * 1000L)
        val filtered = transactions.filter { it.date >= startTime }
        calculator.getCategoryBreakdown(filtered, categoryMap)
    }

    val dailySpending = remember(transactions, selectedPeriod) {
        calculator.getDailySpending(transactions, selectedPeriod)
    }

    val trendPoints = remember(transactions, selectedPeriod) {
        calculator.getSpendingTrend(transactions, selectedPeriod)
    }

    val topMerchants = remember(transactions, selectedPeriod) {
        val now = System.currentTimeMillis()
        val startTime = now - (selectedPeriod.days * 24 * 60 * 60 * 1000L)
        val filtered = transactions.filter { it.date >= startTime }
        calculator.getTopMerchants(filtered, 5)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            AnalyticsBentoHeader(
                summary = summary,
                selectedPeriod = selectedPeriod,
                onPeriodChange = { selectedPeriod = it }
            )
        }

        // Stats Grid
        item {
            AnalyticsStatsGrid(summary = summary)
        }

        // Category Breakdown Chart
        item {
            BentoSectionHeader(
                title = "Spending by Category",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                CategoryPieChart(
                    categoryTotals = categoryBreakdown,
                    modifier = Modifier
                )
            }
        }

        // Daily Spending Chart
        item {
            BentoSectionHeader(
                title = "Daily Spending",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                SpendingBarChart(
                    dailySpending = dailySpending,
                    modifier = Modifier
                )
            }
        }

        // Trend Chart
        item {
            BentoSectionHeader(
                title = "Spending Trend",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                SpendingLineChart(
                    trendPoints = trendPoints,
                    modifier = Modifier
                )
            }
        }

        // Top Merchants
        item {
            BentoSectionHeader(
                title = "Top Merchants",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier.animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                ) {
                    if (topMerchants.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No merchant data available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        topMerchants.forEachIndexed { index, merchant ->
                            ExpressiveMerchantRow(rank = index + 1, merchant = merchant)
                            if (index < topMerchants.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsBentoHeader(
    summary: PeriodSummary,
    selectedPeriod: TimePeriod,
    onPeriodChange: (TimePeriod) -> Unit
) {
    // Animation state for staggered entrance
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationTriggered = true }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Title with enhanced styling
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Analytics",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Insights & Trends",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Period Selector - Expressive pills style
        AnimatedVisibility(
            visible = animationTriggered,
            enter = fadeIn(tween(400))
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                TimePeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodChange(period) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TimePeriod.entries.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(
                            text = when (period) {
                                TimePeriod.DAY -> "Day"
                                TimePeriod.WEEK -> "Week"
                                TimePeriod.MONTH -> "Month"
                                TimePeriod.YEAR -> "Year"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Hero Card - Total Spent with enhanced design
        AnimatedVisibility(
            visible = animationTriggered,
            enter = fadeIn(tween(500)) + slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1565C0),
                                Color(0xFF1976D2),
                                Color(0xFF2196F3)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.extraLarge
                    )
            ) {
                // Decorative elements
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .offset(x = (-40).dp, y = (-40).dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 30.dp, y = 30.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Total Spent",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = getPeriodLabel(selectedPeriod),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.AttachMoney,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "₹${formatAmount(summary.totalSpent)}",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${summary.transactionCount} transactions",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "₹${formatAmount(summary.averageDaily)}/day",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsStatsGrid(summary: PeriodSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Stats cards in a 2x2 grid style
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Daily Average Card
            BentoCard(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                backgroundColor = Color(0xFF1565C0).copy(alpha = 0.08f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Average",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF1565C0).copy(alpha = 0.8f)
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1565C0).copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "₹${formatAmount(summary.averageDaily)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                }
            }

            // Transactions Count Card
            BentoCard(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                backgroundColor = Color(0xFF6A1B9A).copy(alpha = 0.08f)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF6A1B9A).copy(alpha = 0.8f)
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6A1B9A).copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color(0xFF6A1B9A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${summary.transactionCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6A1B9A)
                    )
                }
            }
        }

        // Top Category Card - Enhanced with decorative elements
        summary.topCategory?.let { category ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                category.color.copy(alpha = 0.1f),
                                category.color.copy(alpha = 0.05f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = category.color.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Top Category",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = category.color.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = category.icon,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = category.color
                                )
                                Text(
                                    text = "Most frequent spending",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = category.color.copy(alpha = 0.2f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = category.color,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveMerchantRow(rank: Int, merchant: MerchantTotal) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge - Expressive style
        Surface(
            shape = CircleShape,
            color = when (rank) {
                1 -> Color(0xFFFFD700).copy(alpha = 0.25f)
                2 -> Color(0xFFC0C0C0).copy(alpha = 0.25f)
                3 -> Color(0xFFCD7F32).copy(alpha = 0.25f)
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (rank) {
                        1 -> Color(0xFFB8860B)
                        2 -> Color(0xFF808080)
                        3 -> Color(0xFF8B4513)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = merchant.merchantName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${merchant.transactionCount} transaction${if (merchant.transactionCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DebitColor.copy(alpha = 0.1f)
        ) {
            Text(
                text = "₹${formatAmount(merchant.totalAmount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = DebitColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

private fun formatAmount(amount: Double): String {
    return when {
        amount >= 10000000 -> String.format(Locale.getDefault(), "%.2fCr", amount / 10000000)
        amount >= 100000 -> String.format(Locale.getDefault(), "%.2fL", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1000)
        else -> String.format(Locale.getDefault(), "%.0f", amount)
    }
}

private fun getPeriodLabel(period: TimePeriod): String {
    return when (period) {
        TimePeriod.DAY -> "Today"
        TimePeriod.WEEK -> "This Week"
        TimePeriod.MONTH -> "This Month"
        TimePeriod.YEAR -> "This Year"
    }
}

// Keep for backward compatibility
@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    BentoStatCard(
        title = title,
        value = value,
        valueColor = color,
        iconBackgroundColor = color.copy(alpha = 0.15f),
        iconColor = color,
        modifier = modifier
    )
}
