package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.DefaultCategories
import com.example.kanakku.data.model.TransactionFilter
import com.example.kanakku.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

/**
 * A Material3 component that displays active filters as removable chips.
 *
 * This component provides:
 * - Horizontally scrollable LazyRow of AssistChip for each active filter
 * - Each chip shows the filter label and an X button to remove it
 * - Filter icon button to open the FilterSheet
 *
 * @param currentFilter The current filter state containing all active filters
 * @param onFilterChange Callback invoked when a filter is removed or modified
 * @param onOpenFilterSheet Callback invoked when the filter button is clicked
 * @param modifier Optional modifier for customizing the component's appearance and layout
 */
@Composable
fun ActiveFilterChips(
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onOpenFilterSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Filter icon button
        IconButton(
            onClick = onOpenFilterSheet,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (currentFilter.hasActiveFilters) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Open filters",
                tint = if (currentFilter.hasActiveFilters) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Active filter chips
        if (currentFilter.hasActiveFilters) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Transaction Type Chip
                currentFilter.transactionType?.let { type ->
                    item(key = "type_$type") {
                        FilterChipItem(
                            label = when (type) {
                                TransactionType.DEBIT -> "Debit"
                                TransactionType.CREDIT -> "Credit"
                            },
                            onRemove = {
                                onFilterChange(currentFilter.copy(transactionType = null))
                            }
                        )
                    }
                }

                // Category Chip
                currentFilter.categoryId?.let { categoryId ->
                    item(key = "category_$categoryId") {
                        val category = DefaultCategories.ALL.find { it.id == categoryId }
                        category?.let {
                            FilterChipItem(
                                label = "${it.icon} ${it.name}",
                                onRemove = {
                                    onFilterChange(currentFilter.copy(categoryId = null))
                                }
                            )
                        }
                    }
                }

                // Date Range Chip
                currentFilter.dateRange?.let { range ->
                    item(key = "date_${range.first}_${range.second}") {
                        FilterChipItem(
                            label = formatDateRangeForChip(range),
                            onRemove = {
                                onFilterChange(currentFilter.copy(dateRange = null))
                            }
                        )
                    }
                }

                // Amount Range Chip
                currentFilter.amountRange?.let { range ->
                    item(key = "amount_${range.first}_${range.second}") {
                        FilterChipItem(
                            label = formatAmountRange(range),
                            onRemove = {
                                onFilterChange(currentFilter.copy(amountRange = null))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual filter chip with remove button
 */
@Composable
private fun FilterChipItem(
    label: String,
    onRemove: () -> Unit
) {
    AssistChip(
        onClick = { /* Chip click is handled by trailing icon */ },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove filter",
                    modifier = Modifier.size(14.dp)
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = AssistChipDefaults.assistChipBorder(
            borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
        )
    )
}

/**
 * Format date range for display in chip
 */
private fun formatDateRangeForChip(range: Pair<Long, Long>): String {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val startDate = dateFormat.format(Date(range.first))
    val endDate = dateFormat.format(Date(range.second))

    // Check if it's the same day
    val calendar1 = Calendar.getInstance().apply { timeInMillis = range.first }
    val calendar2 = Calendar.getInstance().apply { timeInMillis = range.second }

    return if (calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
        calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    ) {
        // Same day - show "Today" or just the date
        val today = Calendar.getInstance()
        if (calendar1.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            calendar1.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        ) {
            "Today"
        } else {
            startDate
        }
    } else {
        "$startDate - $endDate"
    }
}

/**
 * Format amount range for display in chip
 */
private fun formatAmountRange(range: Pair<Double, Double>): String {
    val min = range.first
    val max = range.second

    return when {
        min > 0.0 && max < Double.MAX_VALUE -> "₹$min - ₹$max"
        min > 0.0 -> "Min ₹$min"
        max < Double.MAX_VALUE -> "Max ₹$max"
        else -> "Any amount"
    }
}
