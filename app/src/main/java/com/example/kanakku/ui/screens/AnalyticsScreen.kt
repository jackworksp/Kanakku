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
import com.example.kanakku.ui.theme.bronzeRanking
import com.example.kanakku.ui.theme.chartBlue
import com.example.kanakku.ui.theme.chartPurple
import com.example.kanakku.ui.theme.expenseColor
import com.example.kanakku.ui.theme.goldRanking
import com.example.kanakku.ui.theme.silverRanking
import com.example.kanakku.ui.charts.CategoryPieChart
import com.example.kanakku.ui.charts.SpendingBarChart
import com.example.kanakku.ui.charts.SpendingLineChart
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

        // Period Selector
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            TimePeriod.entries.forEachIndexed { index, period ->
                SegmentedButton(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = TimePeriod.entries.size
                    )
                ) {
                    Text(
                        text = when(period) {
                            TimePeriod.DAY -> "Day"
                            TimePeriod.WEEK -> "Week"
                            TimePeriod.MONTH -> "Month"
                            TimePeriod.YEAR -> "Year"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Total Spent",
                value = "₹${formatAmount(summary.totalSpent)}",
                color = MaterialTheme.colorScheme.expenseColor,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Avg/Day",
                value = "₹${formatAmount(summary.averageDaily)}",
                color = MaterialTheme.colorScheme.chartBlue,
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
                color = MaterialTheme.colorScheme.chartPurple,
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
                1 -> MaterialTheme.colorScheme.goldRanking.copy(alpha = 0.2f)
                2 -> MaterialTheme.colorScheme.silverRanking.copy(alpha = 0.2f)
                3 -> MaterialTheme.colorScheme.bronzeRanking.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = when(rank) {
                    1 -> MaterialTheme.colorScheme.goldRanking
                    2 -> MaterialTheme.colorScheme.silverRanking
                    3 -> MaterialTheme.colorScheme.bronzeRanking
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
            color = MaterialTheme.colorScheme.expenseColor
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
