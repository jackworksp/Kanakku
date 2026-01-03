package com.example.kanakku.data.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for TransactionFilter data class.
 *
 * Tests cover:
 * - Default values initialization
 * - hasActiveFilters() property for various filter states
 * - activeFilterCount property accuracy
 * - copy() function for data class modifications
 * - Edge cases (blank search query, whitespace)
 */
class TransactionFilterTest {

    // ==================== Default Values Tests ====================

    @Test
    fun defaultConstructor_allValuesAreNull() {
        // When
        val filter = TransactionFilter()

        // Then
        assertNull(filter.searchQuery)
        assertNull(filter.transactionType)
        assertNull(filter.categoryId)
        assertNull(filter.dateRange)
        assertNull(filter.amountRange)
    }

    @Test
    fun defaultConstructor_hasActiveFiltersIsFalse() {
        // When
        val filter = TransactionFilter()

        // Then
        assertFalse(filter.hasActiveFilters)
    }

    @Test
    fun defaultConstructor_activeFilterCountIsZero() {
        // When
        val filter = TransactionFilter()

        // Then
        assertEquals(0, filter.activeFilterCount)
    }

    // ==================== hasActiveFilters Tests ====================

    @Test
    fun hasActiveFilters_searchQueryNotBlank_returnsTrue() {
        // When
        val filter = TransactionFilter(searchQuery = "Amazon")

        // Then
        assertTrue(filter.hasActiveFilters)
    }

    @Test
    fun hasActiveFilters_searchQueryBlank_returnsFalse() {
        // When
        val filter = TransactionFilter(searchQuery = "")

        // Then
        assertFalse(filter.hasActiveFilters)
    }

    @Test
    fun hasActiveFilters_searchQueryWhitespaceOnly_returnsFalse() {
        // When
        val filter = TransactionFilter(searchQuery = "   ")

        // Then
        assertFalse(filter.hasActiveFilters)
    }

    @Test
    fun hasActiveFilters_transactionTypeSet_returnsTrue() {
        // When
        val filter = TransactionFilter(transactionType = TransactionType.DEBIT)

        // Then
        assertTrue(filter.hasActiveFilters)
    }

    @Test
    fun hasActiveFilters_categoryIdSet_returnsTrue() {
        // When
        val filter = TransactionFilter(categoryId = "food")

        // Then
        assertTrue(filter.hasActiveFilters)
    }

    @Test
    fun hasActiveFilters_dateRangeSet_returnsTrue() {
        // When
        val filter = TransactionFilter(dateRange = Pair(1000L, 2000L))

        // Then
        assertTrue(filter.hasActiveFilters)
    }

    @Test
    fun hasActiveFilters_amountRangeSet_returnsTrue() {
        // When
        val filter = TransactionFilter(amountRange = Pair(100.0, 500.0))

        // Then
        assertTrue(filter.hasActiveFilters)
    }

    @Test
    fun hasActiveFilters_multipleFiltersSet_returnsTrue() {
        // When
        val filter = TransactionFilter(
            searchQuery = "Swiggy",
            transactionType = TransactionType.DEBIT,
            categoryId = "food"
        )

        // Then
        assertTrue(filter.hasActiveFilters)
    }

    @Test
    fun hasActiveFilters_allFiltersSet_returnsTrue() {
        // When
        val filter = TransactionFilter(
            searchQuery = "Zomato",
            transactionType = TransactionType.CREDIT,
            categoryId = "food",
            dateRange = Pair(1000L, 2000L),
            amountRange = Pair(50.0, 1000.0)
        )

        // Then
        assertTrue(filter.hasActiveFilters)
    }

    // ==================== activeFilterCount Tests ====================

    @Test
    fun activeFilterCount_noFilters_returnsZero() {
        // When
        val filter = TransactionFilter()

        // Then
        assertEquals(0, filter.activeFilterCount)
    }

    @Test
    fun activeFilterCount_searchQueryOnly_returnsOne() {
        // When
        val filter = TransactionFilter(searchQuery = "Netflix")

        // Then
        assertEquals(1, filter.activeFilterCount)
    }

    @Test
    fun activeFilterCount_searchQueryBlank_returnsZero() {
        // When
        val filter = TransactionFilter(searchQuery = "")

        // Then
        assertEquals(0, filter.activeFilterCount)
    }

    @Test
    fun activeFilterCount_searchQueryWhitespace_returnsZero() {
        // When
        val filter = TransactionFilter(searchQuery = "  \t  ")

        // Then
        assertEquals(0, filter.activeFilterCount)
    }

    @Test
    fun activeFilterCount_transactionTypeOnly_returnsOne() {
        // When
        val filter = TransactionFilter(transactionType = TransactionType.DEBIT)

        // Then
        assertEquals(1, filter.activeFilterCount)
    }

    @Test
    fun activeFilterCount_categoryIdOnly_returnsOne() {
        // When
        val filter = TransactionFilter(categoryId = "entertainment")

        // Then
        assertEquals(1, filter.activeFilterCount)
    }

    @Test
    fun activeFilterCount_dateRangeOnly_returnsOne() {
        // When
        val filter = TransactionFilter(dateRange = Pair(5000L, 10000L))

        // Then
        assertEquals(1, filter.activeFilterCount)
    }

    @Test
    fun activeFilterCount_amountRangeOnly_returnsOne() {
        // When
        val filter = TransactionFilter(amountRange = Pair(200.0, 800.0))

        // Then
        assertEquals(1, filter.activeFilterCount)
    }

    @Test
    fun activeFilterCount_twoFilters_returnsTwo() {
        // When
        val filter = TransactionFilter(
            searchQuery = "Uber",
            transactionType = TransactionType.DEBIT
        )

        // Then
        assertEquals(2, filter.activeFilterCount)
    }

    @Test
    fun activeFilterCount_threeFilters_returnsThree() {
        // When
        val filter = TransactionFilter(
            transactionType = TransactionType.CREDIT,
            categoryId = "shopping",
            dateRange = Pair(1000L, 5000L)
        )

        // Then
        assertEquals(3, filter.activeFilterCount)
    }

    @Test
    fun activeFilterCount_fourFilters_returnsFour() {
        // When
        val filter = TransactionFilter(
            searchQuery = "Amazon",
            categoryId = "shopping",
            dateRange = Pair(1000L, 5000L),
            amountRange = Pair(100.0, 1000.0)
        )

        // Then
        assertEquals(4, filter.activeFilterCount)
    }

    @Test
    fun activeFilterCount_allFiveFilters_returnsFive() {
        // When
        val filter = TransactionFilter(
            searchQuery = "Flipkart",
            transactionType = TransactionType.DEBIT,
            categoryId = "shopping",
            dateRange = Pair(1000L, 5000L),
            amountRange = Pair(50.0, 2000.0)
        )

        // Then
        assertEquals(5, filter.activeFilterCount)
    }

    // ==================== copy() Tests ====================

    @Test
    fun copy_noChanges_returnsEqualInstance() {
        // Given
        val original = TransactionFilter(
            searchQuery = "Test",
            transactionType = TransactionType.DEBIT
        )

        // When
        val copied = original.copy()

        // Then
        assertEquals(original, copied)
        assertEquals(original.searchQuery, copied.searchQuery)
        assertEquals(original.transactionType, copied.transactionType)
        assertEquals(original.categoryId, copied.categoryId)
        assertEquals(original.dateRange, copied.dateRange)
        assertEquals(original.amountRange, copied.amountRange)
    }

    @Test
    fun copy_changeSearchQuery_onlySearchQueryChanged() {
        // Given
        val original = TransactionFilter(
            searchQuery = "Original",
            transactionType = TransactionType.DEBIT,
            categoryId = "food"
        )

        // When
        val modified = original.copy(searchQuery = "Modified")

        // Then
        assertEquals("Modified", modified.searchQuery)
        assertEquals(original.transactionType, modified.transactionType)
        assertEquals(original.categoryId, modified.categoryId)
        assertEquals(original.dateRange, modified.dateRange)
        assertEquals(original.amountRange, modified.amountRange)
    }

    @Test
    fun copy_changeTransactionType_onlyTransactionTypeChanged() {
        // Given
        val original = TransactionFilter(
            searchQuery = "Test",
            transactionType = TransactionType.DEBIT
        )

        // When
        val modified = original.copy(transactionType = TransactionType.CREDIT)

        // Then
        assertEquals(original.searchQuery, modified.searchQuery)
        assertEquals(TransactionType.CREDIT, modified.transactionType)
        assertEquals(original.categoryId, modified.categoryId)
        assertEquals(original.dateRange, modified.dateRange)
        assertEquals(original.amountRange, modified.amountRange)
    }

    @Test
    fun copy_changeCategoryId_onlyCategoryIdChanged() {
        // Given
        val original = TransactionFilter(categoryId = "food")

        // When
        val modified = original.copy(categoryId = "shopping")

        // Then
        assertEquals(original.searchQuery, modified.searchQuery)
        assertEquals(original.transactionType, modified.transactionType)
        assertEquals("shopping", modified.categoryId)
        assertEquals(original.dateRange, modified.dateRange)
        assertEquals(original.amountRange, modified.amountRange)
    }

    @Test
    fun copy_changeDateRange_onlyDateRangeChanged() {
        // Given
        val original = TransactionFilter(dateRange = Pair(1000L, 2000L))

        // When
        val modified = original.copy(dateRange = Pair(3000L, 4000L))

        // Then
        assertEquals(original.searchQuery, modified.searchQuery)
        assertEquals(original.transactionType, modified.transactionType)
        assertEquals(original.categoryId, modified.categoryId)
        assertEquals(Pair(3000L, 4000L), modified.dateRange)
        assertEquals(original.amountRange, modified.amountRange)
    }

    @Test
    fun copy_changeAmountRange_onlyAmountRangeChanged() {
        // Given
        val original = TransactionFilter(amountRange = Pair(100.0, 500.0))

        // When
        val modified = original.copy(amountRange = Pair(200.0, 800.0))

        // Then
        assertEquals(original.searchQuery, modified.searchQuery)
        assertEquals(original.transactionType, modified.transactionType)
        assertEquals(original.categoryId, modified.categoryId)
        assertEquals(original.dateRange, modified.dateRange)
        assertEquals(Pair(200.0, 800.0), modified.amountRange)
    }

    @Test
    fun copy_changeMultipleProperties_onlySpecifiedPropertiesChanged() {
        // Given
        val original = TransactionFilter(
            searchQuery = "Original",
            transactionType = TransactionType.DEBIT,
            categoryId = "food",
            dateRange = Pair(1000L, 2000L),
            amountRange = Pair(100.0, 500.0)
        )

        // When
        val modified = original.copy(
            searchQuery = "Modified",
            categoryId = "shopping",
            amountRange = Pair(50.0, 1000.0)
        )

        // Then
        assertEquals("Modified", modified.searchQuery)
        assertEquals(TransactionType.DEBIT, modified.transactionType) // Unchanged
        assertEquals("shopping", modified.categoryId)
        assertEquals(Pair(1000L, 2000L), modified.dateRange) // Unchanged
        assertEquals(Pair(50.0, 1000.0), modified.amountRange)
    }

    @Test
    fun copy_setToNull_propertyBecomesNull() {
        // Given
        val original = TransactionFilter(
            searchQuery = "Test",
            transactionType = TransactionType.DEBIT
        )

        // When
        val modified = original.copy(
            searchQuery = null,
            transactionType = null
        )

        // Then
        assertNull(modified.searchQuery)
        assertNull(modified.transactionType)
        assertFalse(modified.hasActiveFilters)
        assertEquals(0, modified.activeFilterCount)
    }

    // ==================== Edge Cases ====================

    @Test
    fun hasActiveFilters_blankSearchQueryWithOtherFilters_returnsTrue() {
        // When
        val filter = TransactionFilter(
            searchQuery = "  ",
            transactionType = TransactionType.DEBIT
        )

        // Then
        assertTrue(filter.hasActiveFilters)
    }

    @Test
    fun activeFilterCount_blankSearchQueryWithOtherFilters_countsOnlyNonBlank() {
        // When
        val filter = TransactionFilter(
            searchQuery = "",
            transactionType = TransactionType.CREDIT,
            categoryId = "food"
        )

        // Then
        assertEquals(2, filter.activeFilterCount) // Only transactionType and categoryId
    }

    @Test
    fun dataClass_equals_worksCorrectly() {
        // Given
        val filter1 = TransactionFilter(
            searchQuery = "Test",
            transactionType = TransactionType.DEBIT,
            categoryId = "food"
        )
        val filter2 = TransactionFilter(
            searchQuery = "Test",
            transactionType = TransactionType.DEBIT,
            categoryId = "food"
        )

        // Then
        assertEquals(filter1, filter2)
    }

    @Test
    fun dataClass_hashCode_worksCorrectly() {
        // Given
        val filter1 = TransactionFilter(
            searchQuery = "Test",
            transactionType = TransactionType.DEBIT
        )
        val filter2 = TransactionFilter(
            searchQuery = "Test",
            transactionType = TransactionType.DEBIT
        )

        // Then
        assertEquals(filter1.hashCode(), filter2.hashCode())
    }

    @Test
    fun dataClass_toString_containsAllProperties() {
        // Given
        val filter = TransactionFilter(
            searchQuery = "Test",
            transactionType = TransactionType.DEBIT,
            categoryId = "food",
            dateRange = Pair(1000L, 2000L),
            amountRange = Pair(100.0, 500.0)
        )

        // When
        val toString = filter.toString()

        // Then
        assertTrue(toString.contains("searchQuery"))
        assertTrue(toString.contains("Test"))
        assertTrue(toString.contains("transactionType"))
        assertTrue(toString.contains("DEBIT"))
        assertTrue(toString.contains("categoryId"))
        assertTrue(toString.contains("food"))
        assertTrue(toString.contains("dateRange"))
        assertTrue(toString.contains("amountRange"))
    }
}
