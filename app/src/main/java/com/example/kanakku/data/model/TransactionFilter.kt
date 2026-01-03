package com.example.kanakku.data.model

/**
 * Data class to hold all filter parameters for transaction filtering.
 *
 * @property searchQuery Optional search query to filter by merchant name, reference number, or raw SMS content
 * @property transactionType Optional transaction type filter (DEBIT/CREDIT)
 * @property categoryId Optional category ID to filter by specific category
 * @property dateRange Optional date range as Pair of start and end timestamps (inclusive)
 * @property amountRange Optional amount range as Pair of min and max amounts (inclusive)
 */
data class TransactionFilter(
    val searchQuery: String? = null,
    val transactionType: TransactionType? = null,
    val categoryId: String? = null,
    val dateRange: Pair<Long, Long>? = null,
    val amountRange: Pair<Double, Double>? = null
) {
    /**
     * Checks if any filters are currently active.
     * @return true if at least one filter is set, false otherwise
     */
    val hasActiveFilters: Boolean
        get() = searchQuery?.isNotBlank() == true ||
                transactionType != null ||
                categoryId != null ||
                dateRange != null ||
                amountRange != null

    /**
     * Counts the number of active filters.
     * @return count of non-null filters (0-5)
     */
    val activeFilterCount: Int
        get() {
            var count = 0
            if (searchQuery?.isNotBlank() == true) count++
            if (transactionType != null) count++
            if (categoryId != null) count++
            if (dateRange != null) count++
            if (amountRange != null) count++
            return count
        }
}
