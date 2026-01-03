package com.example.kanakku.domain.budget

import com.example.kanakku.data.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Unit tests for BudgetCalculator.
 *
 * Tests cover:
 * - Budget progress calculation (spent, remaining, percentage)
 * - Budget status determination (green/yellow/red thresholds)
 * - Category spending aggregation
 * - Date range utilities
 * - Transaction filtering by month
 * - Total spending calculation
 * - Edge cases (zero budget, negative values, threshold boundaries)
 */
class BudgetCalculatorTest {

    private lateinit var calculator: BudgetCalculator

    @Before
    fun setup() {
        calculator = BudgetCalculator()
    }

    // ==================== Helper Functions ====================

    private fun createTestBudget(
        amount: Double = 1000.0,
        categoryId: String? = null,
        month: Int = 1,
        year: Int = 2026
    ): Budget {
        return Budget(
            id = 1L,
            categoryId = categoryId,
            amount = amount,
            month = month,
            year = year,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun createTestTransaction(
        smsId: Long = 1L,
        amount: Double = 100.0,
        type: TransactionType = TransactionType.DEBIT,
        date: Long = System.currentTimeMillis()
    ): ParsedTransaction {
        return ParsedTransaction(
            smsId = smsId,
            amount = amount,
            type = type,
            merchant = "Test Merchant",
            accountNumber = "1234",
            referenceNumber = "REF123",
            date = date,
            rawSms = "Test SMS",
            senderAddress = "VM-TESTBK",
            balanceAfter = 500.0,
            location = "Test Location"
        )
    }

    // ==================== calculateBudgetProgress Tests ====================

    @Test
    fun calculateBudgetProgress_normalSpending_calculatesCorrectly() {
        // Given
        val budget = createTestBudget(amount = 1000.0)
        val spent = 500.0

        // When
        val progress = calculator.calculateBudgetProgress(budget, spent)

        // Then
        assertEquals(500.0, progress.spent, 0.01)
        assertEquals(1000.0, progress.limit, 0.01)
        assertEquals(500.0, progress.remaining, 0.01)
        assertEquals(50.0, progress.percentage, 0.01)
        assertEquals(BudgetStatus.UNDER_BUDGET, progress.status)
    }

    @Test
    fun calculateBudgetProgress_zeroSpent_calculatesCorrectly() {
        // Given
        val budget = createTestBudget(amount = 1000.0)
        val spent = 0.0

        // When
        val progress = calculator.calculateBudgetProgress(budget, spent)

        // Then
        assertEquals(0.0, progress.spent, 0.01)
        assertEquals(1000.0, progress.limit, 0.01)
        assertEquals(1000.0, progress.remaining, 0.01)
        assertEquals(0.0, progress.percentage, 0.01)
        assertEquals(BudgetStatus.UNDER_BUDGET, progress.status)
    }

    @Test
    fun calculateBudgetProgress_exactlyAtBudget_calculatesCorrectly() {
        // Given
        val budget = createTestBudget(amount = 1000.0)
        val spent = 1000.0

        // When
        val progress = calculator.calculateBudgetProgress(budget, spent)

        // Then
        assertEquals(1000.0, progress.spent, 0.01)
        assertEquals(1000.0, progress.limit, 0.01)
        assertEquals(0.0, progress.remaining, 0.01)
        assertEquals(100.0, progress.percentage, 0.01)
        assertEquals(BudgetStatus.EXCEEDED, progress.status)
    }

    @Test
    fun calculateBudgetProgress_overBudget_calculatesCorrectly() {
        // Given
        val budget = createTestBudget(amount = 1000.0)
        val spent = 1500.0

        // When
        val progress = calculator.calculateBudgetProgress(budget, spent)

        // Then
        assertEquals(1500.0, progress.spent, 0.01)
        assertEquals(1000.0, progress.limit, 0.01)
        assertEquals(-500.0, progress.remaining, 0.01)
        assertEquals(150.0, progress.percentage, 0.01)
        assertEquals(BudgetStatus.EXCEEDED, progress.status)
    }

    @Test
    fun calculateBudgetProgress_zeroBudget_handlesGracefully() {
        // Given
        val budget = createTestBudget(amount = 0.0)
        val spent = 100.0

        // When
        val progress = calculator.calculateBudgetProgress(budget, spent)

        // Then
        assertEquals(100.0, progress.spent, 0.01)
        assertEquals(0.0, progress.limit, 0.01)
        assertEquals(-100.0, progress.remaining, 0.01)
        assertEquals(0.0, progress.percentage, 0.01) // Avoids division by zero
        assertEquals(BudgetStatus.UNDER_BUDGET, progress.status)
    }

    @Test
    fun calculateBudgetProgress_negativeSpent_calculatesCorrectly() {
        // Given - Edge case: negative spent (shouldn't happen but test robustness)
        val budget = createTestBudget(amount = 1000.0)
        val spent = -100.0

        // When
        val progress = calculator.calculateBudgetProgress(budget, spent)

        // Then
        assertEquals(-100.0, progress.spent, 0.01)
        assertEquals(1000.0, progress.limit, 0.01)
        assertEquals(1100.0, progress.remaining, 0.01)
        assertEquals(-10.0, progress.percentage, 0.01)
        assertEquals(BudgetStatus.UNDER_BUDGET, progress.status)
    }

    @Test
    fun calculateBudgetProgress_veryLargeAmounts_handlesCorrectly() {
        // Given
        val budget = createTestBudget(amount = 999999999.99)
        val spent = 499999999.99

        // When
        val progress = calculator.calculateBudgetProgress(budget, spent)

        // Then
        assertEquals(499999999.99, progress.spent, 0.01)
        assertEquals(999999999.99, progress.limit, 0.01)
        assertEquals(500000000.0, progress.remaining, 0.01)
        assertEquals(50.0, progress.percentage, 0.01)
        assertEquals(BudgetStatus.UNDER_BUDGET, progress.status)
    }

    @Test
    fun calculateBudgetProgress_verySmallAmounts_handlesCorrectly() {
        // Given
        val budget = createTestBudget(amount = 10.0)
        val spent = 5.0

        // When
        val progress = calculator.calculateBudgetProgress(budget, spent)

        // Then
        assertEquals(5.0, progress.spent, 0.01)
        assertEquals(10.0, progress.limit, 0.01)
        assertEquals(5.0, progress.remaining, 0.01)
        assertEquals(50.0, progress.percentage, 0.01)
        assertEquals(BudgetStatus.UNDER_BUDGET, progress.status)
    }

    // ==================== getBudgetStatus Tests ====================

    @Test
    fun getBudgetStatus_underBudget_returnsGreenStatus() {
        // Given - Less than 80%
        val percentage = 50.0

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.UNDER_BUDGET, status)
    }

    @Test
    fun getBudgetStatus_approaching_returnsYellowStatus() {
        // Given - Between 80% and 100%
        val percentage = 90.0

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.APPROACHING, status)
    }

    @Test
    fun getBudgetStatus_exceeded_returnsRedStatus() {
        // Given - 100% or more
        val percentage = 150.0

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.EXCEEDED, status)
    }

    @Test
    fun getBudgetStatus_exactlyAtWarningThreshold_returnsYellowStatus() {
        // Given - Exactly 80%
        val percentage = 80.0

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.APPROACHING, status)
    }

    @Test
    fun getBudgetStatus_exactlyAtExceededThreshold_returnsRedStatus() {
        // Given - Exactly 100%
        val percentage = 100.0

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.EXCEEDED, status)
    }

    @Test
    fun getBudgetStatus_justBelowWarningThreshold_returnsGreenStatus() {
        // Given - Just below 80%
        val percentage = 79.99

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.UNDER_BUDGET, status)
    }

    @Test
    fun getBudgetStatus_justAboveWarningThreshold_returnsYellowStatus() {
        // Given - Just above 80%
        val percentage = 80.01

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.APPROACHING, status)
    }

    @Test
    fun getBudgetStatus_justBelowExceededThreshold_returnsYellowStatus() {
        // Given - Just below 100%
        val percentage = 99.99

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.APPROACHING, status)
    }

    @Test
    fun getBudgetStatus_justAboveExceededThreshold_returnsRedStatus() {
        // Given - Just above 100%
        val percentage = 100.01

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.EXCEEDED, status)
    }

    @Test
    fun getBudgetStatus_zeroPercentage_returnsGreenStatus() {
        // Given
        val percentage = 0.0

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.UNDER_BUDGET, status)
    }

    @Test
    fun getBudgetStatus_negativePercentage_returnsGreenStatus() {
        // Given - Edge case: negative percentage
        val percentage = -10.0

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.UNDER_BUDGET, status)
    }

    @Test
    fun getBudgetStatus_veryHighPercentage_returnsRedStatus() {
        // Given
        val percentage = 999.0

        // When
        val status = calculator.getBudgetStatus(percentage)

        // Then
        assertEquals(BudgetStatus.EXCEEDED, status)
    }

    // ==================== getSpentByCategory Tests ====================

    @Test
    fun getSpentByCategory_singleCategory_calculatesCorrectly() {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT)
        )
        val categoryMap = mapOf(
            1L to DefaultCategories.FOOD,
            2L to DefaultCategories.FOOD
        )

        // When
        val spending = calculator.getSpentByCategory(transactions, categoryMap)

        // Then
        assertEquals(1, spending.size.toLong())
        assertEquals(300.0, spending[DefaultCategories.FOOD.id] ?: 0.0, 0.01)
    }

    @Test
    fun getSpentByCategory_multipleCategories_calculatesCorrectly() {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 3L, amount = 300.0, type = TransactionType.DEBIT)
        )
        val categoryMap = mapOf(
            1L to DefaultCategories.FOOD,
            2L to DefaultCategories.SHOPPING,
            3L to DefaultCategories.TRANSPORT
        )

        // When
        val spending = calculator.getSpentByCategory(transactions, categoryMap)

        // Then
        assertEquals(3, spending.size.toLong())
        assertEquals(100.0, spending[DefaultCategories.FOOD.id] ?: 0.0, 0.01)
        assertEquals(200.0, spending[DefaultCategories.SHOPPING.id] ?: 0.0, 0.01)
        assertEquals(300.0, spending[DefaultCategories.TRANSPORT.id] ?: 0.0, 0.01)
    }

    @Test
    fun getSpentByCategory_onlyDebitsAreCounted() {
        // Given - Mix of DEBIT and CREDIT transactions
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.CREDIT),
            createTestTransaction(smsId = 3L, amount = 300.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 4L, amount = 400.0, type = TransactionType.UNKNOWN)
        )
        val categoryMap = mapOf(
            1L to DefaultCategories.FOOD,
            2L to DefaultCategories.FOOD,
            3L to DefaultCategories.FOOD,
            4L to DefaultCategories.FOOD
        )

        // When
        val spending = calculator.getSpentByCategory(transactions, categoryMap)

        // Then - Only DEBIT transactions (100 + 300 = 400)
        assertEquals(1, spending.size.toLong())
        assertEquals(400.0, spending[DefaultCategories.FOOD.id] ?: 0.0, 0.01)
    }

    @Test
    fun getSpentByCategory_emptyTransactions_returnsEmptyMap() {
        // Given
        val transactions = emptyList<ParsedTransaction>()
        val categoryMap = mapOf<Long, Category>()

        // When
        val spending = calculator.getSpentByCategory(transactions, categoryMap)

        // Then
        assertTrue(spending.isEmpty())
    }

    @Test
    fun getSpentByCategory_uncategorizedTransactions_usesOtherCategory() {
        // Given - Transaction without category mapping
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT)
        )
        val categoryMap = mapOf(
            1L to DefaultCategories.FOOD
            // smsId 2 is not in the map
        )

        // When
        val spending = calculator.getSpentByCategory(transactions, categoryMap)

        // Then
        assertEquals(2, spending.size.toLong())
        assertEquals(100.0, spending[DefaultCategories.FOOD.id] ?: 0.0, 0.01)
        assertEquals(200.0, spending[DefaultCategories.OTHER.id] ?: 0.0, 0.01)
    }

    @Test
    fun getSpentByCategory_emptyCategoryMap_categorizeAsOther() {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT)
        )
        val categoryMap = emptyMap<Long, Category>()

        // When
        val spending = calculator.getSpentByCategory(transactions, categoryMap)

        // Then - All should go to OTHER category
        assertEquals(1, spending.size.toLong())
        assertEquals(300.0, spending[DefaultCategories.OTHER.id] ?: 0.0, 0.01)
    }

    @Test
    fun getSpentByCategory_zeroAmountTransactions_includedInSum() {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 0.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 2L, amount = 100.0, type = TransactionType.DEBIT)
        )
        val categoryMap = mapOf(
            1L to DefaultCategories.FOOD,
            2L to DefaultCategories.FOOD
        )

        // When
        val spending = calculator.getSpentByCategory(transactions, categoryMap)

        // Then
        assertEquals(1, spending.size.toLong())
        assertEquals(100.0, spending[DefaultCategories.FOOD.id] ?: 0.0, 0.01)
    }

    @Test
    fun getSpentByCategory_veryLargeAmounts_handlesCorrectly() {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 999999.99, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 2L, amount = 888888.88, type = TransactionType.DEBIT)
        )
        val categoryMap = mapOf(
            1L to DefaultCategories.FOOD,
            2L to DefaultCategories.FOOD
        )

        // When
        val spending = calculator.getSpentByCategory(transactions, categoryMap)

        // Then
        assertEquals(1, spending.size.toLong())
        assertEquals(1888888.87, spending[DefaultCategories.FOOD.id] ?: 0.0, 0.01)
    }

    // ==================== getCurrentMonthRange Tests ====================

    @Test
    fun getCurrentMonthRange_returnsValidRange() {
        // When
        val (start, end) = calculator.getCurrentMonthRange()

        // Then
        assertTrue(start > 0)
        assertTrue(end > start)
        assertTrue(end > System.currentTimeMillis() - 32 * 24 * 3600000L) // Within last month
    }

    @Test
    fun getCurrentMonthRange_startIsFirstDayOfMonth() {
        // When
        val (start, _) = calculator.getCurrentMonthRange()

        // Then
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = start
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH).toLong())
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY).toLong())
        assertEquals(0, calendar.get(Calendar.MINUTE).toLong())
        assertEquals(0, calendar.get(Calendar.SECOND).toLong())
    }

    @Test
    fun getCurrentMonthRange_endIsLastDayOfMonth() {
        // When
        val (_, end) = calculator.getCurrentMonthRange()

        // Then
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = end
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        assertEquals(maxDay.toLong(), calendar.get(Calendar.DAY_OF_MONTH).toLong())
        assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY).toLong())
        assertEquals(59, calendar.get(Calendar.MINUTE).toLong())
        assertEquals(59, calendar.get(Calendar.SECOND).toLong())
    }

    // ==================== getMonthRange Tests ====================

    @Test
    fun getMonthRange_january2026_returnsCorrectRange() {
        // Given
        val month = 1
        val year = 2026

        // When
        val (start, end) = calculator.getMonthRange(month, year)

        // Then
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = start
        assertEquals(2026, startCal.get(Calendar.YEAR).toLong())
        assertEquals(0, startCal.get(Calendar.MONTH).toLong()) // 0 = January
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH).toLong())

        val endCal = Calendar.getInstance()
        endCal.timeInMillis = end
        assertEquals(2026, endCal.get(Calendar.YEAR).toLong())
        assertEquals(0, endCal.get(Calendar.MONTH).toLong())
        assertEquals(31, endCal.get(Calendar.DAY_OF_MONTH).toLong()) // January has 31 days
    }

    @Test
    fun getMonthRange_february2024_handlesLeapYear() {
        // Given - 2024 is a leap year
        val month = 2
        val year = 2024

        // When
        val (start, end) = calculator.getMonthRange(month, year)

        // Then
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = end
        assertEquals(29, endCal.get(Calendar.DAY_OF_MONTH).toLong()) // Leap year: 29 days
    }

    @Test
    fun getMonthRange_february2023_handlesNonLeapYear() {
        // Given - 2023 is not a leap year
        val month = 2
        val year = 2023

        // When
        val (start, end) = calculator.getMonthRange(month, year)

        // Then
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = end
        assertEquals(28, endCal.get(Calendar.DAY_OF_MONTH).toLong()) // Non-leap year: 28 days
    }

    @Test
    fun getMonthRange_december2026_returnsCorrectRange() {
        // Given
        val month = 12
        val year = 2026

        // When
        val (start, end) = calculator.getMonthRange(month, year)

        // Then
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = start
        assertEquals(2026, startCal.get(Calendar.YEAR).toLong())
        assertEquals(11, startCal.get(Calendar.MONTH).toLong()) // 11 = December

        val endCal = Calendar.getInstance()
        endCal.timeInMillis = end
        assertEquals(31, endCal.get(Calendar.DAY_OF_MONTH).toLong()) // December has 31 days
    }

    @Test
    fun getMonthRange_april2026_handles30DayMonth() {
        // Given - April has 30 days
        val month = 4
        val year = 2026

        // When
        val (start, end) = calculator.getMonthRange(month, year)

        // Then
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = end
        assertEquals(30, endCal.get(Calendar.DAY_OF_MONTH).toLong()) // April has 30 days
    }

    @Test
    fun getMonthRange_startAndEndDifferent() {
        // Given
        val month = 6
        val year = 2026

        // When
        val (start, end) = calculator.getMonthRange(month, year)

        // Then
        assertTrue(end > start)
        // Approximately 30 days difference
        val diffDays = (end - start) / (24 * 3600000L)
        assertTrue(diffDays >= 29 && diffDays <= 31)
    }

    // ==================== filterTransactionsByMonth Tests ====================

    @Test
    fun filterTransactionsByMonth_filtersCorrectly() {
        // Given
        val jan1 = createCalendar(2026, 0, 1).timeInMillis
        val jan15 = createCalendar(2026, 0, 15).timeInMillis
        val feb1 = createCalendar(2026, 1, 1).timeInMillis

        val transactions = listOf(
            createTestTransaction(smsId = 1L, date = jan1),
            createTestTransaction(smsId = 2L, date = jan15),
            createTestTransaction(smsId = 3L, date = feb1)
        )

        // When
        val januaryTxns = calculator.filterTransactionsByMonth(transactions, 1, 2026)

        // Then
        assertEquals(2, januaryTxns.size.toLong())
        assertTrue(januaryTxns.any { it.smsId == 1L })
        assertTrue(januaryTxns.any { it.smsId == 2L })
    }

    @Test
    fun filterTransactionsByMonth_emptyList_returnsEmpty() {
        // Given
        val transactions = emptyList<ParsedTransaction>()

        // When
        val filtered = calculator.filterTransactionsByMonth(transactions, 1, 2026)

        // Then
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun filterTransactionsByMonth_noMatchingTransactions_returnsEmpty() {
        // Given - Transactions in January, filter for February
        val jan15 = createCalendar(2026, 0, 15).timeInMillis
        val transactions = listOf(
            createTestTransaction(smsId = 1L, date = jan15)
        )

        // When
        val februaryTxns = calculator.filterTransactionsByMonth(transactions, 2, 2026)

        // Then
        assertTrue(februaryTxns.isEmpty())
    }

    @Test
    fun filterTransactionsByMonth_boundaryDates_includesAll() {
        // Given - Transactions at first and last day of month
        val jan1 = createCalendar(2026, 0, 1, 0, 0, 0).timeInMillis
        val jan31 = createCalendar(2026, 0, 31, 23, 59, 59).timeInMillis

        val transactions = listOf(
            createTestTransaction(smsId = 1L, date = jan1),
            createTestTransaction(smsId = 2L, date = jan31)
        )

        // When
        val januaryTxns = calculator.filterTransactionsByMonth(transactions, 1, 2026)

        // Then
        assertEquals(2, januaryTxns.size.toLong())
    }

    // ==================== getTotalSpent Tests ====================

    @Test
    fun getTotalSpent_onlyDebits_calculatesCorrectly() {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 3L, amount = 300.0, type = TransactionType.DEBIT)
        )

        // When
        val total = calculator.getTotalSpent(transactions)

        // Then
        assertEquals(600.0, total, 0.01)
    }

    @Test
    fun getTotalSpent_mixedTypes_onlyCountsDebits() {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.CREDIT),
            createTestTransaction(smsId = 3L, amount = 300.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 4L, amount = 400.0, type = TransactionType.UNKNOWN)
        )

        // When
        val total = calculator.getTotalSpent(transactions)

        // Then - Only DEBIT transactions (100 + 300 = 400)
        assertEquals(400.0, total, 0.01)
    }

    @Test
    fun getTotalSpent_emptyList_returnsZero() {
        // Given
        val transactions = emptyList<ParsedTransaction>()

        // When
        val total = calculator.getTotalSpent(transactions)

        // Then
        assertEquals(0.0, total, 0.01)
    }

    @Test
    fun getTotalSpent_onlyCredits_returnsZero() {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.CREDIT),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.CREDIT)
        )

        // When
        val total = calculator.getTotalSpent(transactions)

        // Then
        assertEquals(0.0, total, 0.01)
    }

    @Test
    fun getTotalSpent_zeroAmounts_handlesCorrectly() {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 0.0, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 2L, amount = 100.0, type = TransactionType.DEBIT)
        )

        // When
        val total = calculator.getTotalSpent(transactions)

        // Then
        assertEquals(100.0, total, 0.01)
    }

    @Test
    fun getTotalSpent_veryLargeAmounts_handlesCorrectly() {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 999999.99, type = TransactionType.DEBIT),
            createTestTransaction(smsId = 2L, amount = 888888.88, type = TransactionType.DEBIT)
        )

        // When
        val total = calculator.getTotalSpent(transactions)

        // Then
        assertEquals(1888888.87, total, 0.01)
    }

    // ==================== Integration Tests ====================

    @Test
    fun fullWorkflow_calculateMonthlyBudgetProgress() {
        // Given - Budget for January 2026
        val budget = createTestBudget(amount = 5000.0, month = 1, year = 2026)

        // Create transactions for January
        val jan5 = createCalendar(2026, 0, 5).timeInMillis
        val jan15 = createCalendar(2026, 0, 15).timeInMillis
        val jan25 = createCalendar(2026, 0, 25).timeInMillis
        val feb5 = createCalendar(2026, 1, 5).timeInMillis

        val allTransactions = listOf(
            createTestTransaction(smsId = 1L, amount = 1000.0, type = TransactionType.DEBIT, date = jan5),
            createTestTransaction(smsId = 2L, amount = 1500.0, type = TransactionType.DEBIT, date = jan15),
            createTestTransaction(smsId = 3L, amount = 500.0, type = TransactionType.CREDIT, date = jan15), // Should be ignored
            createTestTransaction(smsId = 4L, amount = 2000.0, type = TransactionType.DEBIT, date = jan25),
            createTestTransaction(smsId = 5L, amount = 1000.0, type = TransactionType.DEBIT, date = feb5) // Should be filtered out
        )

        // When
        val januaryTxns = calculator.filterTransactionsByMonth(allTransactions, 1, 2026)
        val totalSpent = calculator.getTotalSpent(januaryTxns)
        val progress = calculator.calculateBudgetProgress(budget, totalSpent)

        // Then
        assertEquals(4, januaryTxns.size.toLong()) // All January transactions (including CREDIT)
        assertEquals(4500.0, totalSpent, 0.01) // 1000 + 1500 + 2000 (only DEBIT)
        assertEquals(4500.0, progress.spent, 0.01)
        assertEquals(5000.0, progress.limit, 0.01)
        assertEquals(500.0, progress.remaining, 0.01)
        assertEquals(90.0, progress.percentage, 0.01)
        assertEquals(BudgetStatus.APPROACHING, progress.status) // 90% is in yellow zone
    }

    @Test
    fun fullWorkflow_calculateCategoryBudgetProgress() {
        // Given - Category budget for Food
        val budget = createTestBudget(amount = 2000.0, categoryId = DefaultCategories.FOOD.id, month = 1, year = 2026)

        // Create transactions
        val jan10 = createCalendar(2026, 0, 10).timeInMillis
        val jan20 = createCalendar(2026, 0, 20).timeInMillis

        val allTransactions = listOf(
            createTestTransaction(smsId = 1L, amount = 500.0, type = TransactionType.DEBIT, date = jan10),
            createTestTransaction(smsId = 2L, amount = 700.0, type = TransactionType.DEBIT, date = jan20),
            createTestTransaction(smsId = 3L, amount = 300.0, type = TransactionType.DEBIT, date = jan10),
            createTestTransaction(smsId = 4L, amount = 200.0, type = TransactionType.CREDIT, date = jan10) // Should be ignored
        )

        val categoryMap = mapOf(
            1L to DefaultCategories.FOOD,
            2L to DefaultCategories.FOOD,
            3L to DefaultCategories.SHOPPING, // Different category
            4L to DefaultCategories.FOOD
        )

        // When
        val januaryTxns = calculator.filterTransactionsByMonth(allTransactions, 1, 2026)
        val spendingByCategory = calculator.getSpentByCategory(januaryTxns, categoryMap)
        val foodSpent = spendingByCategory[DefaultCategories.FOOD.id] ?: 0.0
        val progress = calculator.calculateBudgetProgress(budget, foodSpent)

        // Then
        assertEquals(1200.0, foodSpent, 0.01) // 500 + 700 (only Food debits)
        assertEquals(1200.0, progress.spent, 0.01)
        assertEquals(2000.0, progress.limit, 0.01)
        assertEquals(800.0, progress.remaining, 0.01)
        assertEquals(60.0, progress.percentage, 0.01)
        assertEquals(BudgetStatus.UNDER_BUDGET, progress.status) // 60% is green
    }

    @Test
    fun edgeCase_budgetExceededByExactlyOne() {
        // Given
        val budget = createTestBudget(amount = 1000.0)
        val spent = 1001.0

        // When
        val progress = calculator.calculateBudgetProgress(budget, spent)

        // Then
        assertEquals(1001.0, progress.spent, 0.01)
        assertEquals(-1.0, progress.remaining, 0.01)
        assertEquals(100.1, progress.percentage, 0.01)
        assertEquals(BudgetStatus.EXCEEDED, progress.status)
    }

    @Test
    fun edgeCase_budgetUnderByExactlyOne() {
        // Given
        val budget = createTestBudget(amount = 1000.0)
        val spent = 999.0

        // When
        val progress = calculator.calculateBudgetProgress(budget, spent)

        // Then
        assertEquals(999.0, progress.spent, 0.01)
        assertEquals(1.0, progress.remaining, 0.01)
        assertEquals(99.9, progress.percentage, 0.01)
        assertEquals(BudgetStatus.APPROACHING, progress.status)
    }

    @Test
    fun thresholds_verifyConstants() {
        // Verify the threshold constants are as expected
        assertEquals(80.0, BudgetCalculator.THRESHOLD_WARNING, 0.01)
        assertEquals(100.0, BudgetCalculator.THRESHOLD_EXCEEDED, 0.01)
    }

    // ==================== Helper Methods ====================

    private fun createCalendar(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
        minute: Int = 0,
        second: Int = 0
    ): Calendar {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, minute, second)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
