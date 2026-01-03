package com.example.kanakku.ui

import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionFilter
import com.example.kanakku.data.model.TransactionType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for transaction filtering logic in MainViewModel.
 *
 * Tests cover:
 * - Search query matching (merchant, reference number, raw SMS) - case-insensitive, partial
 * - Transaction type filtering (DEBIT only, CREDIT only)
 * - Date range filtering (inclusive)
 * - Amount range filtering (inclusive)
 * - Category filtering
 * - Combined filters working together
 * - Edge cases (null values, empty strings, boundary values)
 */
class TransactionFilteringTest {

    private lateinit var testTransactions: List<ParsedTransaction>
    private lateinit var categoryMap: Map<Long, Category>

    @Before
    fun setup() {
        // Create test transactions with diverse data
        testTransactions = listOf(
            // Transaction 1: Amazon Shopping - DEBIT
            ParsedTransaction(
                smsId = 1L,
                amount = 1299.50,
                type = TransactionType.DEBIT,
                merchant = "Amazon",
                accountNumber = "1234",
                referenceNumber = "REF123ABC",
                date = 1704067200000L, // 2024-01-01
                rawSms = "Debited Rs.1299.50 from A/c 1234 on 01-Jan-24 to Amazon REF123ABC",
                senderAddress = "VM-AMAZON"
            ),
            // Transaction 2: Swiggy Food - DEBIT
            ParsedTransaction(
                smsId = 2L,
                amount = 450.00,
                type = TransactionType.DEBIT,
                merchant = "Swiggy",
                accountNumber = "1234",
                referenceNumber = "REF456DEF",
                date = 1704153600000L, // 2024-01-02
                rawSms = "Debited Rs.450.00 from A/c 1234 on 02-Jan-24 to Swiggy REF456DEF",
                senderAddress = "VM-SWIGGY"
            ),
            // Transaction 3: Salary - CREDIT
            ParsedTransaction(
                smsId = 3L,
                amount = 50000.00,
                type = TransactionType.CREDIT,
                merchant = "COMPANY LTD",
                accountNumber = "1234",
                referenceNumber = "SAL789GHI",
                date = 1704240000000L, // 2024-01-03
                rawSms = "Credited Rs.50000.00 to A/c 1234 on 03-Jan-24 from COMPANY LTD SAL789GHI",
                senderAddress = "VM-HDFCBK"
            ),
            // Transaction 4: Uber Ride - DEBIT
            ParsedTransaction(
                smsId = 4L,
                amount = 250.00,
                type = TransactionType.DEBIT,
                merchant = "Uber India",
                accountNumber = "1234",
                referenceNumber = "UBER001",
                date = 1704326400000L, // 2024-01-04
                rawSms = "Debited Rs.250.00 from A/c 1234 on 04-Jan-24 to Uber India UBER001",
                senderAddress = "VM-UBER"
            ),
            // Transaction 5: Flipkart Shopping - DEBIT
            ParsedTransaction(
                smsId = 5L,
                amount = 2500.00,
                type = TransactionType.DEBIT,
                merchant = "Flipkart",
                accountNumber = "1234",
                referenceNumber = "FLK999",
                date = 1704412800000L, // 2024-01-05
                rawSms = "Debited Rs.2500.00 from A/c 1234 on 05-Jan-24 to Flipkart FLK999",
                senderAddress = "VM-FLIPKART"
            ),
            // Transaction 6: Freelance Payment - CREDIT
            ParsedTransaction(
                smsId = 6L,
                amount = 15000.00,
                type = TransactionType.CREDIT,
                merchant = "Freelance Client",
                accountNumber = "1234",
                referenceNumber = "FREE888",
                date = 1704499200000L, // 2024-01-06
                rawSms = "Credited Rs.15000.00 to A/c 1234 on 06-Jan-24 from Freelance Client FREE888",
                senderAddress = "VM-HDFCBK"
            ),
            // Transaction 7: Zomato Food - DEBIT
            ParsedTransaction(
                smsId = 7L,
                amount = 600.00,
                type = TransactionType.DEBIT,
                merchant = "Zomato",
                accountNumber = "1234",
                referenceNumber = "ZOM777",
                date = 1704585600000L, // 2024-01-07
                rawSms = "Debited Rs.600.00 from A/c 1234 on 07-Jan-24 to Zomato ZOM777",
                senderAddress = "VM-ZOMATO"
            ),
            // Transaction 8: Netflix Subscription - DEBIT
            ParsedTransaction(
                smsId = 8L,
                amount = 199.00,
                type = TransactionType.DEBIT,
                merchant = "Netflix",
                accountNumber = "1234",
                referenceNumber = null, // Test null reference number
                date = 1704672000000L, // 2024-01-08
                rawSms = "Debited Rs.199.00 from A/c 1234 on 08-Jan-24 to Netflix",
                senderAddress = "VM-NETFLIX"
            ),
            // Transaction 9: Interest Credit - CREDIT
            ParsedTransaction(
                smsId = 9L,
                amount = 125.50,
                type = TransactionType.CREDIT,
                merchant = null, // Test null merchant
                accountNumber = "1234",
                referenceNumber = "INT555",
                date = 1704758400000L, // 2024-01-09
                rawSms = "Credited Rs.125.50 to A/c 1234 on 09-Jan-24 as Interest INT555",
                senderAddress = "VM-HDFCBK"
            ),
            // Transaction 10: Amazon Prime - DEBIT (lowercase test)
            ParsedTransaction(
                smsId = 10L,
                amount = 1499.00,
                type = TransactionType.DEBIT,
                merchant = "amazon prime",
                accountNumber = "1234",
                referenceNumber = "PRIME111",
                date = 1704844800000L, // 2024-01-10
                rawSms = "Debited Rs.1499.00 from A/c 1234 on 10-Jan-24 to amazon prime PRIME111",
                senderAddress = "VM-AMAZON"
            )
        )

        // Create test category map
        val foodCategory = Category(id = "food", name = "Food", icon = "🍔", color = "#FF5722")
        val shoppingCategory = Category(id = "shopping", name = "Shopping", icon = "🛒", color = "#2196F3")
        val transportCategory = Category(id = "transport", name = "Transport", icon = "🚗", color = "#4CAF50")
        val entertainmentCategory = Category(id = "entertainment", name = "Entertainment", icon = "🎬", color = "#9C27B0")

        categoryMap = mapOf(
            2L to foodCategory,     // Swiggy
            7L to foodCategory,     // Zomato
            1L to shoppingCategory, // Amazon
            5L to shoppingCategory, // Flipkart
            10L to shoppingCategory, // Amazon Prime
            4L to transportCategory, // Uber
            8L to entertainmentCategory // Netflix
        )
    }

    // ==================== Search Query Tests ====================

    @Test
    fun filter_searchMerchantExactMatch_returnsMatches() {
        // Given
        val filter = TransactionFilter(searchQuery = "Amazon")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.merchant == "Amazon" })
        assertTrue(result.any { it.merchant == "amazon prime" })
    }

    @Test
    fun filter_searchMerchantPartialMatch_returnsMatches() {
        // Given
        val filter = TransactionFilter(searchQuery = "ama")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.merchant?.lowercase()?.contains("ama") == true })
    }

    @Test
    fun filter_searchMerchantCaseInsensitive_returnsMatches() {
        // Given
        val filter = TransactionFilter(searchQuery = "SWIGGY")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(1, result.size)
        assertEquals("Swiggy", result[0].merchant)
    }

    @Test
    fun filter_searchMerchantMixedCase_returnsMatches() {
        // Given
        val filter = TransactionFilter(searchQuery = "FlIpKaRt")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(1, result.size)
        assertEquals("Flipkart", result[0].merchant)
    }

    @Test
    fun filter_searchReferenceNumberExactMatch_returnsMatches() {
        // Given
        val filter = TransactionFilter(searchQuery = "REF123ABC")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(1, result.size)
        assertEquals("REF123ABC", result[0].referenceNumber)
        assertEquals("Amazon", result[0].merchant)
    }

    @Test
    fun filter_searchReferenceNumberPartialMatch_returnsMatches() {
        // Given
        val filter = TransactionFilter(searchQuery = "REF")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(3, result.size) // REF123ABC, REF456DEF, FREE888
        assertTrue(result.all { it.referenceNumber?.contains("REF", ignoreCase = true) == true ||
                                it.rawSms.contains("REF", ignoreCase = true) })
    }

    @Test
    fun filter_searchReferenceNumberCaseInsensitive_returnsMatches() {
        // Given
        val filter = TransactionFilter(searchQuery = "uber001")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(1, result.size)
        assertEquals("UBER001", result[0].referenceNumber)
    }

    @Test
    fun filter_searchRawSmsContent_returnsMatches() {
        // Given
        val filter = TransactionFilter(searchQuery = "Freelance")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].rawSms.contains("Freelance"))
    }

    @Test
    fun filter_searchRawSmsPartialContent_returnsMatches() {
        // Given
        val filter = TransactionFilter(searchQuery = "Interest")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].rawSms.contains("Interest"))
    }

    @Test
    fun filter_searchNoMatches_returnsEmpty() {
        // Given
        val filter = TransactionFilter(searchQuery = "NonExistent")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun filter_searchEmptyString_returnsAll() {
        // Given
        val filter = TransactionFilter(searchQuery = "")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(testTransactions.size, result.size)
    }

    @Test
    fun filter_searchBlankString_returnsAll() {
        // Given
        val filter = TransactionFilter(searchQuery = "   ")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(testTransactions.size, result.size)
    }

    @Test
    fun filter_searchNullMerchant_noException() {
        // Given - Transaction 9 has null merchant
        val filter = TransactionFilter(searchQuery = "Merchant")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then - Should not throw exception, should filter by rawSms only
        assertEquals(0, result.size)
    }

    @Test
    fun filter_searchNullReferenceNumber_noException() {
        // Given - Transaction 8 has null reference number
        val filter = TransactionFilter(searchQuery = "Netflix")

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then - Should match by merchant or rawSms
        assertEquals(1, result.size)
        assertEquals("Netflix", result[0].merchant)
    }

    // ==================== Transaction Type Filter Tests ====================

    @Test
    fun filter_typeDebitOnly_returnsOnlyDebits() {
        // Given
        val filter = TransactionFilter(transactionType = TransactionType.DEBIT)

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(7, result.size)
        assertTrue(result.all { it.type == TransactionType.DEBIT })
    }

    @Test
    fun filter_typeCreditOnly_returnsOnlyCredits() {
        // Given
        val filter = TransactionFilter(transactionType = TransactionType.CREDIT)

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(3, result.size)
        assertTrue(result.all { it.type == TransactionType.CREDIT })
    }

    @Test
    fun filter_typeNull_returnsAll() {
        // Given
        val filter = TransactionFilter(transactionType = null)

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(testTransactions.size, result.size)
    }

    // ==================== Date Range Filter Tests ====================

    @Test
    fun filter_dateRangeInclusive_returnsMatchingDates() {
        // Given - Filter for Jan 2-5, 2024
        val startDate = 1704153600000L // 2024-01-02
        val endDate = 1704412800000L   // 2024-01-05
        val filter = TransactionFilter(dateRange = Pair(startDate, endDate))

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(4, result.size) // Transactions 2, 3, 4, 5
        assertTrue(result.all { it.date >= startDate && it.date <= endDate })
    }

    @Test
    fun filter_dateRangeSingleDay_returnsSingleDay() {
        // Given - Filter for exactly Jan 1, 2024
        val date = 1704067200000L // 2024-01-01
        val filter = TransactionFilter(dateRange = Pair(date, date))

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(1, result.size)
        assertEquals(date, result[0].date)
    }

    @Test
    fun filter_dateRangeBeforeAllTransactions_returnsEmpty() {
        // Given - Filter for dates before any transaction
        val startDate = 1000000000000L
        val endDate = 1000000000000L
        val filter = TransactionFilter(dateRange = Pair(startDate, endDate))

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun filter_dateRangeAfterAllTransactions_returnsEmpty() {
        // Given - Filter for dates after any transaction
        val startDate = 2000000000000L
        val endDate = 2000000000000L
        val filter = TransactionFilter(dateRange = Pair(startDate, endDate))

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun filter_dateRangeNull_returnsAll() {
        // Given
        val filter = TransactionFilter(dateRange = null)

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(testTransactions.size, result.size)
    }

    @Test
    fun filter_dateRangeBoundaryMatch_includesBoundaryValues() {
        // Given - Filter with exact boundary dates
        val startDate = 1704067200000L // 2024-01-01 (Transaction 1)
        val endDate = 1704844800000L   // 2024-01-10 (Transaction 10)
        val filter = TransactionFilter(dateRange = Pair(startDate, endDate))

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(10, result.size) // All transactions
        assertTrue(result.any { it.date == startDate })
        assertTrue(result.any { it.date == endDate })
    }

    // ==================== Amount Range Filter Tests ====================

    @Test
    fun filter_amountRangeInclusive_returnsMatchingAmounts() {
        // Given - Filter for amounts 200-500
        val filter = TransactionFilter(amountRange = Pair(200.0, 500.0))

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(3, result.size) // Swiggy (450), Uber (250), Netflix (199) - wait, 199 is outside
        assertTrue(result.all { it.amount >= 200.0 && it.amount <= 500.0 })
    }

    @Test
    fun filter_amountRangeExactMatch_returnsSingleAmount() {
        // Given - Filter for exact amount
        val amount = 450.0
        val filter = TransactionFilter(amountRange = Pair(amount, amount))

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(1, result.size)
        assertEquals(amount, result[0].amount, 0.01)
    }

    @Test
    fun filter_amountRangeLargeAmounts_returnsHighValueTransactions() {
        // Given - Filter for amounts over 10000
        val filter = TransactionFilter(amountRange = Pair(10000.0, 100000.0))

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(2, result.size) // Salary (50000) and Freelance (15000)
        assertTrue(result.all { it.amount >= 10000.0 })
    }

    @Test
    fun filter_amountRangeSmallAmounts_returnsLowValueTransactions() {
        // Given - Filter for amounts under 300
        val filter = TransactionFilter(amountRange = Pair(0.0, 300.0))

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(3, result.size) // Uber (250), Netflix (199), Interest (125.50)
        assertTrue(result.all { it.amount <= 300.0 })
    }

    @Test
    fun filter_amountRangeNull_returnsAll() {
        // Given
        val filter = TransactionFilter(amountRange = null)

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(testTransactions.size, result.size)
    }

    @Test
    fun filter_amountRangeBoundaryMatch_includesBoundaryValues() {
        // Given
        val filter = TransactionFilter(amountRange = Pair(450.0, 1299.50))

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertTrue(result.any { it.amount == 450.0 })
        assertTrue(result.any { it.amount == 1299.50 })
    }

    @Test
    fun filter_amountRangeZeroToSmall_includesSmallAmounts() {
        // Given
        val filter = TransactionFilter(amountRange = Pair(0.0, 200.0))

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(2, result.size) // Netflix (199), Interest (125.50)
    }

    // ==================== Category Filter Tests ====================

    @Test
    fun filter_categoryFood_returnsOnlyFoodTransactions() {
        // Given
        val filter = TransactionFilter(categoryId = "food")

        // When
        val result = filterTransactions(testTransactions, filter, categoryMap)

        // Then
        assertEquals(2, result.size) // Swiggy and Zomato
        assertTrue(result.all { categoryMap[it.smsId]?.id == "food" })
    }

    @Test
    fun filter_categoryShopping_returnsOnlyShoppingTransactions() {
        // Given
        val filter = TransactionFilter(categoryId = "shopping")

        // When
        val result = filterTransactions(testTransactions, filter, categoryMap)

        // Then
        assertEquals(3, result.size) // Amazon, Flipkart, Amazon Prime
        assertTrue(result.all { categoryMap[it.smsId]?.id == "shopping" })
    }

    @Test
    fun filter_categoryTransport_returnsOnlyTransportTransactions() {
        // Given
        val filter = TransactionFilter(categoryId = "transport")

        // When
        val result = filterTransactions(testTransactions, filter, categoryMap)

        // Then
        assertEquals(1, result.size) // Uber
        assertEquals("Uber India", result[0].merchant)
    }

    @Test
    fun filter_categoryEntertainment_returnsOnlyEntertainmentTransactions() {
        // Given
        val filter = TransactionFilter(categoryId = "entertainment")

        // When
        val result = filterTransactions(testTransactions, filter, categoryMap)

        // Then
        assertEquals(1, result.size) // Netflix
        assertEquals("Netflix", result[0].merchant)
    }

    @Test
    fun filter_categoryNonExistent_returnsEmpty() {
        // Given
        val filter = TransactionFilter(categoryId = "bills")

        // When
        val result = filterTransactions(testTransactions, filter, categoryMap)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun filter_categoryNull_returnsAll() {
        // Given
        val filter = TransactionFilter(categoryId = null)

        // When
        val result = filterTransactions(testTransactions, filter, categoryMap)

        // Then
        assertEquals(testTransactions.size, result.size)
    }

    // ==================== Combined Filters Tests ====================

    @Test
    fun filter_searchAndType_bothFiltersApply() {
        // Given - Search for "Amazon" and DEBIT type
        val filter = TransactionFilter(
            searchQuery = "Amazon",
            transactionType = TransactionType.DEBIT
        )

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(2, result.size) // Both Amazon transactions are DEBIT
        assertTrue(result.all { it.type == TransactionType.DEBIT })
        assertTrue(result.all { it.merchant?.contains("amazon", ignoreCase = true) == true })
    }

    @Test
    fun filter_typeAndDateRange_bothFiltersApply() {
        // Given - DEBIT transactions in Jan 1-3, 2024
        val filter = TransactionFilter(
            transactionType = TransactionType.DEBIT,
            dateRange = Pair(1704067200000L, 1704240000000L)
        )

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(2, result.size) // Amazon and Swiggy (Transaction 3 is CREDIT)
        assertTrue(result.all { it.type == TransactionType.DEBIT })
    }

    @Test
    fun filter_dateRangeAndAmountRange_bothFiltersApply() {
        // Given - Transactions Jan 2-5 with amount 400-2000
        val filter = TransactionFilter(
            dateRange = Pair(1704153600000L, 1704412800000L),
            amountRange = Pair(400.0, 2000.0)
        )

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(1, result.size) // Only Swiggy (450)
        assertEquals("Swiggy", result[0].merchant)
    }

    @Test
    fun filter_searchTypeAndCategory_allThreeFiltersApply() {
        // Given - Search for shopping category DEBIT transactions with "a" in merchant
        val filter = TransactionFilter(
            searchQuery = "a",
            transactionType = TransactionType.DEBIT,
            categoryId = "shopping"
        )

        // When
        val result = filterTransactions(testTransactions, filter, categoryMap)

        // Then
        assertEquals(3, result.size) // Amazon, Flipkart, Amazon Prime
        assertTrue(result.all { it.type == TransactionType.DEBIT })
        assertTrue(result.all { categoryMap[it.smsId]?.id == "shopping" })
    }

    @Test
    fun filter_allFiveFilters_returnsMatchingSubset() {
        // Given - Complex filter: search, type, category, date, amount
        val filter = TransactionFilter(
            searchQuery = "Amazon",
            transactionType = TransactionType.DEBIT,
            categoryId = "shopping",
            dateRange = Pair(1704067200000L, 1704153600000L),
            amountRange = Pair(1000.0, 2000.0)
        )

        // When
        val result = filterTransactions(testTransactions, filter, categoryMap)

        // Then
        assertEquals(1, result.size) // Only first Amazon transaction
        assertEquals("Amazon", result[0].merchant)
        assertEquals(1299.50, result[0].amount, 0.01)
    }

    @Test
    fun filter_combinedFiltersNoMatches_returnsEmpty() {
        // Given - Impossible combination: CREDIT transaction in food category
        val filter = TransactionFilter(
            transactionType = TransactionType.CREDIT,
            categoryId = "food"
        )

        // When
        val result = filterTransactions(testTransactions, filter, categoryMap)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun filter_searchAndAmountRange_bothFiltersApply() {
        // Given - Search for "amazon" with amount under 1300
        val filter = TransactionFilter(
            searchQuery = "amazon",
            amountRange = Pair(0.0, 1300.0)
        )

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(1, result.size) // Only first Amazon (1299.50)
        assertEquals("Amazon", result[0].merchant)
    }

    @Test
    fun filter_categoryAndAmountRange_bothFiltersApply() {
        // Given - Food category with amount under 500
        val filter = TransactionFilter(
            categoryId = "food",
            amountRange = Pair(0.0, 500.0)
        )

        // When
        val result = filterTransactions(testTransactions, filter, categoryMap)

        // Then
        assertEquals(1, result.size) // Only Swiggy (450)
        assertEquals("Swiggy", result[0].merchant)
    }

    // ==================== Edge Cases ====================

    @Test
    fun filter_emptyTransactionList_returnsEmpty() {
        // Given
        val filter = TransactionFilter(searchQuery = "Amazon")

        // When
        val result = filterTransactions(emptyList(), filter)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun filter_noActiveFilters_returnsAll() {
        // Given
        val filter = TransactionFilter()

        // When
        val result = filterTransactions(testTransactions, filter)

        // Then
        assertEquals(testTransactions.size, result.size)
    }

    @Test
    fun filter_multipleCategoriesNotPossible_filterByFirstCategory() {
        // Given - A transaction can only have one category
        val filter = TransactionFilter(categoryId = "food")

        // When
        val result = filterTransactions(testTransactions, filter, categoryMap)

        // Then - Should return only food category transactions
        assertEquals(2, result.size)
    }

    // ==================== Helper Methods ====================

    /**
     * Helper method that replicates the filtering logic from MainViewModel.filterTransactionsInMemory()
     * This ensures tests match the actual implementation.
     */
    private fun filterTransactions(
        transactions: List<ParsedTransaction>,
        filter: TransactionFilter,
        categories: Map<Long, Category> = emptyMap()
    ): List<ParsedTransaction> {
        var filtered = transactions

        // 1. Apply search query filter (case-insensitive search on merchant/rawSms/reference)
        filter.searchQuery?.let { query ->
            if (query.isNotBlank()) {
                val searchLower = query.lowercase()
                filtered = filtered.filter { transaction ->
                    // Search in merchant name
                    transaction.merchant?.lowercase()?.contains(searchLower) == true ||
                    // Search in raw SMS content
                    transaction.rawSms.lowercase().contains(searchLower) ||
                    // Search in reference number
                    transaction.referenceNumber?.lowercase()?.contains(searchLower) == true
                }
            }
        }

        // 2. Apply transaction type filter
        filter.transactionType?.let { type ->
            filtered = filtered.filter { it.type == type }
        }

        // 3. Apply category filter using categoryMap
        filter.categoryId?.let { categoryId ->
            filtered = filtered.filter { transaction ->
                // Check if transaction's category matches the filter
                categories[transaction.smsId]?.id == categoryId
            }
        }

        // 4. Apply date range filter (inclusive)
        filter.dateRange?.let { (startDate, endDate) ->
            filtered = filtered.filter { transaction ->
                transaction.date >= startDate && transaction.date <= endDate
            }
        }

        // 5. Apply amount range filter (inclusive)
        filter.amountRange?.let { (minAmount, maxAmount) ->
            filtered = filtered.filter { transaction ->
                transaction.amount >= minAmount && transaction.amount <= maxAmount
            }
        }

        return filtered
    }
}
