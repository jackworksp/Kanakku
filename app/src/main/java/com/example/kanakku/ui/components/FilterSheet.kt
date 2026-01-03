package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.DefaultCategories
import com.example.kanakku.data.model.TransactionFilter
import com.example.kanakku.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

/**
 * A Material3 ModalBottomSheet for filtering transactions.
 *
 * This component provides comprehensive filtering options including:
 * - Transaction type (All/Debit/Credit) using segmented buttons
 * - Category selection using filter chips
 * - Date range selection with preset options and custom range picker
 * - Amount range with min/max input fields
 *
 * @param currentFilter The current filter state
 * @param onFilterChange Callback invoked when filters are applied
 * @param onDismiss Callback invoked when the sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit
) {
    // Local state for filter editing
    var transactionType by remember { mutableStateOf(currentFilter.transactionType) }
    var selectedCategoryId by remember { mutableStateOf(currentFilter.categoryId) }
    var dateRange by remember { mutableStateOf(currentFilter.dateRange) }
    var minAmount by remember { mutableStateOf(currentFilter.amountRange?.first?.toString() ?: "") }
    var maxAmount by remember { mutableStateOf(currentFilter.amountRange?.second?.toString() ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDatePreset by remember { mutableStateOf<DatePreset?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "Filter Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Transaction Type Section
            FilterSectionTitle(title = "Transaction Type")
            Spacer(modifier = Modifier.height(8.dp))
            TransactionTypeSelector(
                selectedType = transactionType,
                onTypeSelected = { transactionType = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category Section
            FilterSectionTitle(title = "Category")
            Spacer(modifier = Modifier.height(8.dp))
            CategoryFilterChips(
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { categoryId ->
                    selectedCategoryId = if (selectedCategoryId == categoryId) null else categoryId
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Date Range Section
            FilterSectionTitle(title = "Date Range")
            Spacer(modifier = Modifier.height(8.dp))
            DateRangeSelector(
                selectedPreset = selectedDatePreset,
                customDateRange = dateRange,
                onPresetSelected = { preset ->
                    selectedDatePreset = preset
                    dateRange = preset?.getDateRange()
                },
                onCustomRangeClick = { showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Amount Range Section
            FilterSectionTitle(title = "Amount Range")
            Spacer(modifier = Modifier.height(8.dp))
            AmountRangeInputs(
                minAmount = minAmount,
                maxAmount = maxAmount,
                onMinAmountChange = { minAmount = it },
                onMaxAmountChange = { maxAmount = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Reset all filters
                        transactionType = null
                        selectedCategoryId = null
                        dateRange = null
                        minAmount = ""
                        maxAmount = ""
                        selectedDatePreset = null
                        onFilterChange(TransactionFilter())
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }

                Button(
                    onClick = {
                        // Apply filters
                        val amountRangeValue = if (minAmount.isNotBlank() || maxAmount.isNotBlank()) {
                            val min = minAmount.toDoubleOrNull() ?: 0.0
                            val max = maxAmount.toDoubleOrNull() ?: Double.MAX_VALUE
                            Pair(min, max)
                        } else null

                        val newFilter = TransactionFilter(
                            searchQuery = currentFilter.searchQuery, // Keep existing search
                            transactionType = transactionType,
                            categoryId = selectedCategoryId,
                            dateRange = dateRange,
                            amountRange = amountRangeValue
                        )
                        onFilterChange(newFilter)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Date Picker Dialog (if needed)
    if (showDatePicker) {
        // Note: Material3 DateRangePicker would go here
        // For now, we'll use preset options only
        showDatePicker = false
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType?,
    onTypeSelected: (TransactionType?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All option
        FilterChip(
            selected = selectedType == null,
            onClick = { onTypeSelected(null) },
            label = { Text("All") },
            modifier = Modifier.weight(1f)
        )

        // Debit option
        FilterChip(
            selected = selectedType == TransactionType.DEBIT,
            onClick = { onTypeSelected(TransactionType.DEBIT) },
            label = { Text("Debit") },
            modifier = Modifier.weight(1f)
        )

        // Credit option
        FilterChip(
            selected = selectedType == TransactionType.CREDIT,
            onClick = { onTypeSelected(TransactionType.CREDIT) },
            label = { Text("Credit") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CategoryFilterChips(
    selectedCategoryId: String?,
    onCategorySelected: (String) -> Unit
) {
    // Display categories in a wrapping grid-like layout
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Split categories into rows of 2
        DefaultCategories.ALL.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(category.icon)
                                Text(category.name)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = category.color.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                // Add spacer if odd number of items in row
                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DateRangeSelector(
    selectedPreset: DatePreset?,
    customDateRange: Pair<Long, Long>?,
    onPresetSelected: (DatePreset?) -> Unit,
    onCustomRangeClick: () -> Unit
) {
    Column {
        // Display date presets in rows
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First row: Today and This Week
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPreset == DatePreset.TODAY,
                    onClick = {
                        onPresetSelected(if (selectedPreset == DatePreset.TODAY) null else DatePreset.TODAY)
                    },
                    label = { Text(DatePreset.TODAY.label) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPreset == DatePreset.THIS_WEEK,
                    onClick = {
                        onPresetSelected(if (selectedPreset == DatePreset.THIS_WEEK) null else DatePreset.THIS_WEEK)
                    },
                    label = { Text(DatePreset.THIS_WEEK.label) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Second row: This Month and Last 30 Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPreset == DatePreset.THIS_MONTH,
                    onClick = {
                        onPresetSelected(if (selectedPreset == DatePreset.THIS_MONTH) null else DatePreset.THIS_MONTH)
                    },
                    label = { Text(DatePreset.THIS_MONTH.label) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPreset == DatePreset.LAST_30_DAYS,
                    onClick = {
                        onPresetSelected(if (selectedPreset == DatePreset.LAST_30_DAYS) null else DatePreset.LAST_30_DAYS)
                    },
                    label = { Text(DatePreset.LAST_30_DAYS.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Display selected custom range if any
        if (customDateRange != null && selectedPreset == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatDateRange(customDateRange),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AmountRangeInputs(
    minAmount: String,
    maxAmount: String,
    onMinAmountChange: (String) -> Unit,
    onMaxAmountChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = minAmount,
            onValueChange = onMinAmountChange,
            modifier = Modifier.weight(1f),
            label = { Text("Min Amount") },
            placeholder = { Text("0") },
            prefix = { Text("₹") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        OutlinedTextField(
            value = maxAmount,
            onValueChange = onMaxAmountChange,
            modifier = Modifier.weight(1f),
            label = { Text("Max Amount") },
            placeholder = { Text("Any") },
            prefix = { Text("₹") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
    }
}

/**
 * Date preset options for quick filtering
 */
private enum class DatePreset(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days");

    fun getDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val startOfPeriod = when (this) {
            TODAY -> {
                calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            THIS_WEEK -> {
                calendar.apply {
                    set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            THIS_MONTH -> {
                calendar.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            LAST_30_DAYS -> {
                calendar.apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }

        return Pair(startOfPeriod, endOfDay)
    }
}

/**
 * Format date range for display
 */
private fun formatDateRange(range: Pair<Long, Long>): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val startDate = dateFormat.format(Date(range.first))
    val endDate = dateFormat.format(Date(range.second))
    return "$startDate - $endDate"
}
