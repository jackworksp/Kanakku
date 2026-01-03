package com.example.kanakku.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.*
import com.example.kanakku.domain.analytics.AnalyticsCalculator
import com.example.kanakku.ui.charts.CategoryPieChart
import com.example.kanakku.ui.charts.SpendingBarChart
import com.example.kanakku.ui.charts.SpendingLineChart
import com.example.kanakku.ui.components.DateRangeSelectorChip
import com.example.kanakku.ui.components.DateRangePickerSheet
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    transactions: List<ParsedTransaction>,
    categoryMap: Map<Long, Category>,
    selectedDateRange: DateRange,
    onDateRangeChange: (DateRange) -> Unit
) {
    val calculator = remember { AnalyticsCalculator() }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val summary = remember(transactions, categoryMap, selectedDateRange) {
        calculator.calculatePeriodSummary(transactions, categoryMap, selectedDateRange)
    }

    val categoryBreakdown = remember(transactions, categoryMap, selectedDateRange) {
        val filtered = transactions.filter { selectedDateRange.contains(it.date) }
        calculator.getCategoryBreakdown(filtered, categoryMap)
    }

    val dailySpending = remember(transactions, selectedDateRange) {
        calculator.getDailySpending(transactions, selectedDateRange)
    }

    val trendPoints = remember(transactions, selectedDateRange) {
        calculator.getSpendingTrend(transactions, selectedDateRange)
    }

    val topMerchants = remember(transactions, selectedDateRange) {
        val filtered = transactions.filter { selectedDateRange.contains(it.date) }
        calculator.getTopMerchants(filtered, 5)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date Range Selector
        DateRangeSelectorChip(
            dateRange = selectedDateRange,
            onClick = { showDateRangePicker = true },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Total Spent",
                value = "₹${formatAmount(summary.totalSpent)}",
                color = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Avg/Day",
                value = "₹${formatAmount(summary.averageDaily)}",
                color = Color(0xFF1565C0),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Transactions",
                value = "${summary.transactionCount}",
                color = Color(0xFF6A1B9A),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Top Category",
                value = summary.topCategory?.name?.take(12) ?: "N/A",
                color = summary.topCategory?.color ?: Color.Gray,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Category Breakdown - Donut Chart
        Text(
            text = "Spending by Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            CategoryPieChart(
                categoryTotals = categoryBreakdown,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Spending Bar Chart
        Text(
            text = "Daily Spending",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            SpendingBarChart(
                dailySpending = dailySpending,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Spending Trend Line Chart
        Text(
            text = "Spending Trend",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            SpendingLineChart(
                trendPoints = trendPoints,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Top Merchants
        Text(
            text = "Top Merchants",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (topMerchants.isEmpty()) {
                    Text(
                        text = "No merchant data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    topMerchants.forEachIndexed { index, merchant ->
                        MerchantRow(rank = index + 1, merchant = merchant)
                        if (index < topMerchants.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Date Range Picker Sheet
    if (showDateRangePicker) {
        DateRangePickerSheet(
            currentDateRange = selectedDateRange,
            onDateRangeSelected = { newRange ->
                onDateRangeChange(newRange)
                showDateRangePicker = false
            },
            onDismiss = {
                showDateRangePicker = false
            }
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun MerchantRow(rank: Int, merchant: MerchantTotal) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Surface(
            shape = MaterialTheme.shapes.small,
            color = when(rank) {
                1 -> Color(0xFFFFD700).copy(alpha = 0.2f)
                2 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                3 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = when(rank) {
                    1 -> Color(0xFFB8860B)
                    2 -> Color(0xFF808080)
                    3 -> Color(0xFF8B4513)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = merchant.merchantName.take(25),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "${merchant.transactionCount} transaction${if (merchant.transactionCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "₹${formatAmount(merchant.totalAmount)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC62828)
        )
    }
}

private fun formatAmount(amount: Double): String {
    return when {
        amount >= 100000 -> String.format(Locale.getDefault(), "%.1fL", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1000)
        else -> String.format(Locale.getDefault(), "%.0f", amount)
    }
}
