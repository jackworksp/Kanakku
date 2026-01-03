package com.example.kanakku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.*
import com.example.kanakku.ui.MainUiState
import com.example.kanakku.ui.SearchFilterState
import com.example.kanakku.ui.components.CategoryPickerSheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: MainUiState,
    searchFilterState: SearchFilterState,
    categoryMap: Map<Long, Category>,
    onRefresh: () -> Unit,
    onCategoryChange: (Long, Category) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (TransactionFilter) -> Unit,
    onClearFilters: () -> Unit
) {
    var selectedTransaction by remember { mutableStateOf<ParsedTransaction?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TransactionsHeader(uiState = uiState, onRefresh = onRefresh)

        if (uiState.transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No bank transactions found in the last 30 days",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.transactions) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        category = categoryMap[transaction.smsId],
                        onClick = {
                            selectedTransaction = transaction
                            showCategoryPicker = true
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showCategoryPicker && selectedTransaction != null) {
        CategoryPickerSheet(
            currentCategory = categoryMap[selectedTransaction!!.smsId],
            onCategorySelected = { category ->
                onCategoryChange(selectedTransaction!!.smsId, category)
                showCategoryPicker = false
                selectedTransaction = null
            },
            onDismiss = {
                showCategoryPicker = false
                selectedTransaction = null
            }
        )
    }
}

@Composable
private fun TransactionsHeader(
    uiState: MainUiState,
    onRefresh: () -> Unit
) {
    val totalDebit = uiState.transactions
        .filter { it.type == TransactionType.DEBIT }
        .sumOf { it.amount }

    val totalCredit = uiState.transactions
        .filter { it.type == TransactionType.CREDIT }
        .sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onRefresh) {
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFC62828)
                    )
                    Text(
                        text = "₹${formatAmount(totalDebit)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Received",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "₹${formatAmount(totalCredit)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${uiState.transactions.size} transactions from ${uiState.bankSmsCount} bank SMS" +
                    if (uiState.duplicatesRemoved > 0) " (${uiState.duplicatesRemoved} duplicates removed)" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Display last sync timestamp if available
        uiState.lastSyncTimestamp?.let { timestamp ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last synced: ${formatRelativeTime(timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun TransactionCard(
    transaction: ParsedTransaction,
    category: Category?,
    onClick: () -> Unit
) {
    val isDebit = transaction.type == TransactionType.DEBIT
    val amountColor = if (isDebit) Color(0xFFC62828) else Color(0xFF2E7D32)
    val amountPrefix = if (isDebit) "-" else "+"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant ?: transaction.senderAddress,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatDate(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    transaction.referenceNumber?.let { ref ->
                        Text(
                            text = "Ref: $ref",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$amountPrefix₹${formatAmount(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isDebit) Color(0xFFFFCDD2) else Color(0xFFC8E6C9),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isDebit) "DEBIT" else "CREDIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = amountColor
                        )
                    }
                }
            }

            category?.let { cat ->
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = cat.color.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = cat.color
                    )
                }
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.2f", amount)
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Formats a timestamp as a relative time string (e.g., "just now", "5 minutes ago", "2 hours ago").
 * For times older than 24 hours, displays absolute date and time.
 *
 * @param timestamp The timestamp in milliseconds
 * @return A user-friendly relative time string
 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "just now" // Less than 1 minute
        diff < 3600_000 -> { // Less than 1 hour
            val minutes = (diff / 60_000).toInt()
            if (minutes == 1) "1 minute ago" else "$minutes minutes ago"
        }
        diff < 86400_000 -> { // Less than 24 hours
            val hours = (diff / 3600_000).toInt()
            if (hours == 1) "1 hour ago" else "$hours hours ago"
        }
        else -> {
            // More than 24 hours - show absolute date and time
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
