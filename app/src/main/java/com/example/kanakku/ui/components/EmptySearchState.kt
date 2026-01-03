package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.DefaultCategories
import com.example.kanakku.data.model.TransactionFilter

/**
 * A Material3 component that displays an empty state when no search results are found.
 *
 * This component provides:
 * - Centered icon indicating no results
 * - "No results found" title
 * - Contextual message based on the active filter type
 * - "Clear filters" button to reset all filters
 *
 * @param currentFilter The current filter state to generate contextual messages
 * @param onClearFilters Callback invoked when the user clicks the "Clear filters" button
 * @param modifier Optional modifier for customizing the component's appearance and layout
 */
@Composable
fun EmptySearchState(
    currentFilter: TransactionFilter,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = "No results found",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "No results found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Contextual message
            Text(
                text = getContextualMessage(currentFilter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Clear filters button
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "Clear filters")
            }
        }
    }
}

/**
 * Generates a contextual message based on the active filter type.
 *
 * @param filter The current transaction filter
 * @return A user-friendly message explaining why no results were found
 */
private fun getContextualMessage(filter: TransactionFilter): String {
    val messages = mutableListOf<String>()

    // Search query message
    filter.searchQuery?.takeIf { it.isNotBlank() }?.let { query ->
        messages.add("searching for \"$query\"")
    }

    // Transaction type message
    filter.transactionType?.let { type ->
        messages.add("${type.name.lowercase()} transactions")
    }

    // Category message
    filter.categoryId?.let { categoryId ->
        val category = DefaultCategories.ALL.find { it.id == categoryId }
        category?.let {
            messages.add("${it.icon} ${it.name} category")
        }
    }

    // Date range message
    filter.dateRange?.let {
        messages.add("the selected date range")
    }

    // Amount range message
    filter.amountRange?.let { range ->
        val min = range.first
        val max = range.second
        when {
            min > 0.0 && max < Double.MAX_VALUE -> messages.add("₹$min - ₹$max range")
            min > 0.0 -> messages.add("amounts above ₹$min")
            max < Double.MAX_VALUE -> messages.add("amounts below ₹$max")
        }
    }

    return when {
        messages.isEmpty() -> "Try adjusting your filters"
        messages.size == 1 -> "No transactions match ${messages[0]}"
        else -> "No transactions match ${messages.joinToString(", ", limit = 2, truncated = "and other filters")}"
    }
}
