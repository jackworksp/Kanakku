package com.example.kanakku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import com.example.kanakku.ui.components.ActiveFilterChips
import com.example.kanakku.ui.components.CategoryPickerSheet
import com.example.kanakku.ui.components.EmptySearchState
import com.example.kanakku.ui.components.FilterSheet
import com.example.kanakku.ui.components.HighlightedText
import com.example.kanakku.ui.components.SearchBar
import java.text.SimpleDateFormat
import java.util.*

/**
 * Date range filter options for viewing transaction history.
 * Allows users to filter transactions by predefined time periods.
 */
enum class DateRangeFilter(val displayName: String) {
    ALL_TIME("All Time"),
    THIS_MONTH("This Month"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    LAST_YEAR("Last Year"),
    CUSTOM("Custom")
}

/**
 * Calculates the start timestamp for a given date range filter.
 * Returns null for ALL_TIME (no filtering).
 *
 * @param filter The date range filter to calculate timestamp for
 * @return Start timestamp in milliseconds, or null for ALL_TIME
 */
fun getDateRangeStartTimestamp(filter: DateRangeFilter): Long? {
    if (filter == DateRangeFilter.ALL_TIME) return null

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = System.currentTimeMillis()

    return when (filter) {
        DateRangeFilter.THIS_MONTH -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        DateRangeFilter.LAST_3_MONTHS -> {
            calendar.add(Calendar.MONTH, -3)
            calendar.timeInMillis
        }
        DateRangeFilter.LAST_6_MONTHS -> {
            calendar.add(Calendar.MONTH, -6)
            calendar.timeInMillis
        }
        DateRangeFilter.LAST_YEAR -> {
            calendar.add(Calendar.YEAR, -1)
            calendar.timeInMillis
        }
        DateRangeFilter.CUSTOM -> {
            // For now, CUSTOM behaves like ALL_TIME
            // Can be extended with date picker dialog in the future
            null
        }
        DateRangeFilter.ALL_TIME -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: MainUiState,
    searchFilterState: SearchFilterState,
    categoryMap: Map<Long, Category>,
    selectedDateRange: DateRange,
    onDateRangeChange: (DateRange) -> Unit,
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
        TransactionsHeader(
            uiState = uiState,
            searchFilterState = searchFilterState,
            onRefresh = onRefresh
        )

        // Search bar
        SearchBar(
            query = searchFilterState.currentFilter.searchQuery ?: "",
            onQueryChange = onSearchQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Active filter chips
        ActiveFilterChips(
            currentFilter = searchFilterState.currentFilter,
            onFilterChange = onFilterChange,
            onOpenFilterSheet = { showFilterSheet = true },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            // No transactions at all - show original empty state
            uiState.transactions.isEmpty() -> {
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
            }
            // Filters active but no results - show EmptySearchState
            searchFilterState.filteredTransactions.isEmpty() && uiState.transactions.isNotEmpty() -> {
                EmptySearchState(
                    currentFilter = searchFilterState.currentFilter,
                    onClearFilters = onClearFilters
                )
            }
            // Show filtered transactions
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchFilterState.filteredTransactions) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            category = categoryMap[transaction.smsId],
                            onClick = {
                                selectedTransaction = transaction
                                showCategoryPicker = true
                            },
                            searchQuery = searchFilterState.currentFilter.searchQuery
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

    if (showFilterSheet) {
        FilterSheet(
            currentFilter = searchFilterState.currentFilter,
            onFilterChange = { newFilter ->
                onFilterChange(newFilter)
                showFilterSheet = false
            },
            onDismiss = {
                showFilterSheet = false
            }
        )
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
private fun TransactionsHeader(
    transactions: List<ParsedTransaction>,
    uiState: MainUiState,
    searchFilterState: SearchFilterState,
    onRefresh: () -> Unit
) {
    // Calculate totals based on filtered transactions
    val totalDebit = filteredTransactions
        .filter { it.type == TransactionType.DEBIT }
        .sumOf { it.amount }

    val totalCredit = filteredTransactions
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

        // Date Range Filter Chips
        DateRangeFilterChips(
            selectedFilter = selectedDateRange,
            onFilterSelected = onDateRangeChanged
        )

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

        // Show filtered count when filters are active, otherwise show total count
        Text(
            text = if (searchFilterState.isSearchActive) {
                "Showing ${searchFilterState.filteredTransactions.size} of ${uiState.transactions.size} transactions"
            } else {
                "${uiState.transactions.size} transactions from ${uiState.bankSmsCount} bank SMS" +
                        if (uiState.duplicatesRemoved > 0) " (${uiState.duplicatesRemoved} duplicates removed)" else ""
            },
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

/**
 * Date range filter chips component for selecting transaction time periods.
 * Uses horizontally scrollable row of filter chips for easy selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeFilterChips(
    selectedFilter: DateRangeFilter,
    onFilterSelected: (DateRangeFilter) -> Unit
) {
    // Filter out CUSTOM for now (can be added later with date picker)
    val availableFilters = remember {
        DateRangeFilter.entries.filter { it != DateRangeFilter.CUSTOM }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        availableFilters.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.displayName,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun TransactionCard(
    transaction: ParsedTransaction,
    category: Category?,
    onClick: () -> Unit,
    searchQuery: String? = null
) {
    val isDebit = transaction.type == TransactionType.DEBIT
    val amountColor = if (isDebit) Color(0xFFC62828) else Color(0xFF2E7D32)
    val amountPrefix = if (isDebit) "-" else "+"
    val isManual = transaction.source == TransactionSource.MANUAL

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
                    HighlightedText(
                        text = transaction.merchant ?: transaction.senderAddress,
                        highlight = searchQuery,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
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
                        HighlightedText(
                            text = "Ref: $ref",
                            highlight = searchQuery,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
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

            // Manual transaction badge and category badges
            if (isManual || category != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show "Manual" badge for manually entered transactions
                    if (isManual) {
                        Row(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Manual",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Show category badge if assigned
                    category?.let { cat ->
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

            // Show notes if present (primarily for manual transactions)
            transaction.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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
