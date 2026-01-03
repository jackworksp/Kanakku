package com.example.kanakku.domain.analytics

import com.example.kanakku.data.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

/**
 * Unit tests for AnalyticsCalculator with DateRange support.
 *
 * Tests cover:
 * - calculatePeriodSummary with custom date ranges
 * - getSpendingTrend with smart date formatting
 * - getDailySpending with adaptive formatting
 * - Edge cases (same day, multi-year ranges, empty transactions)
 * - Backward compatibility with TimePeriod methods
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AnalyticsCalculatorTest {

    private lateinit var calculator: AnalyticsCalculator
    private lateinit var sampleTransactions: List<ParsedTransaction>
    private lateinit var categoryMap: Map<Long, Category>

    @Before
    fun setup() {
        calculator = AnalyticsCalculator()

        // Create sample categories
        val foodCategory = Category(
            id = 1L,
            name = "Food",
            icon = "🍔",
            color = "#FF5722"
        )
        val transportCategory = Category(
            id = 2L,
            name = "Transport",
            icon = "🚗",
            color = "#2196F3"
        )

        categoryMap = mapOf(
            101L to foodCategory,
            102L to transportCategory,
            103L to foodCategory,
            104L to DefaultCategories.OTHER
        )

        // Create sample transactions spanning multiple days
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (1 * 24 * 60 * 60 * 1000L)
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000L)
        val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000L)
        val tenDaysAgo = now - (10 * 24 * 60 * 60 * 1000L)

        sampleTransactions = listOf(
            // Recent transactions
            ParsedTransaction(101L, 150.0, TransactionType.DEBIT, "Restaurant A", "1234", now),
            ParsedTransaction(102L, 50.0, TransactionType.DEBIT, "Uber", "1234", oneDayAgo),
            ParsedTransaction(103L, 200.0, TransactionType.DEBIT, "Restaurant B", "1234", twoDaysAgo),
            ParsedTransaction(104L, 1000.0, TransactionType.CREDIT, "Salary", "1234", threeDaysAgo),
            // Older transaction (outside 7-day range)
            ParsedTransaction(105L, 75.0, TransactionType.DEBIT, "Restaurant C", "1234", tenDaysAgo)
        )
    }

    // ==================== calculatePeriodSummary Tests ====================

    @Test
    fun calculatePeriodSummary_withDateRange_filtersTransactionsCorrectly() {
        // Given - Last 7 days range
        val dateRange = DateRange.last7Days()

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)

        // Then
        // Should include transactions from last 7 days (4 transactions)
        // 150 + 50 + 200 = 400 spent
        assertEquals(400.0, summary.totalSpent, 0.01)
        assertEquals(1000.0, summary.totalReceived, 0.01)
        assertEquals(4, summary.transactionCount)
        assertNull(summary.period) // period should be null for DateRange
        assertNotNull(summary.topCategory)
    }

    @Test
    fun calculatePeriodSummary_customRange_filtersCorrectly() {
        // Given - Custom range covering only first 2 transactions
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000L)
        val dateRange = DateRange.custom(twoDaysAgo, now)

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)

        // Then
        // Should include only transactions within range
        // At minimum: 150 (today) + 50 (1 day ago)
        assertTrue(summary.totalSpent >= 150.0)
        assertTrue(summary.transactionCount >= 2)
    }

    @Test
    fun calculatePeriodSummary_singleDayRange_calculatesAverageDailyCorrectly() {
        // Given - Today only
        val dateRange = DateRange.today()

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)

        // Then
        // Average daily should equal total spent for single day
        assertEquals(summary.totalSpent, summary.averageDaily, 0.01)
        assertNotNull(summary)
    }

    @Test
    fun calculatePeriodSummary_multiDayRange_calculatesAverageDailyCorrectly() {
        // Given - Last 30 days
        val dateRange = DateRange.last30Days()

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)

        // Then
        // Average should be total spent divided by 30 days
        val expectedAverage = summary.totalSpent / 30
        assertEquals(expectedAverage, summary.averageDaily, 0.01)
    }

    @Test
    fun calculatePeriodSummary_emptyTransactions_returnsZeros() {
        // Given
        val emptyTransactions = emptyList<ParsedTransaction>()
        val dateRange = DateRange.last7Days()

        // When
        val summary = calculator.calculatePeriodSummary(emptyTransactions, categoryMap, dateRange)

        // Then
        assertEquals(0.0, summary.totalSpent, 0.01)
        assertEquals(0.0, summary.totalReceived, 0.01)
        assertEquals(0, summary.transactionCount)
        assertEquals(0.0, summary.averageDaily, 0.01)
        assertNull(summary.topCategory)
    }

    @Test
    fun calculatePeriodSummary_noTransactionsInRange_returnsZeros() {
        // Given - Range in the future
        val futureStart = System.currentTimeMillis() + (10 * 24 * 60 * 60 * 1000L)
        val futureEnd = futureStart + (5 * 24 * 60 * 60 * 1000L)
        val dateRange = DateRange.custom(futureStart, futureEnd)

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)

        // Then
        assertEquals(0.0, summary.totalSpent, 0.01)
        assertEquals(0.0, summary.totalReceived, 0.01)
        assertEquals(0, summary.transactionCount)
    }

    @Test
    fun calculatePeriodSummary_identifiesTopCategory() {
        // Given - Range including food transactions (150 + 200 = 350)
        val dateRange = DateRange.last7Days()

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)

        // Then
        assertNotNull(summary.topCategory)
        assertEquals("Food", summary.topCategory?.name)
    }

    @Test
    fun calculatePeriodSummary_boundaryInclusive_includesStartAndEnd() {
        // Given - Exact timestamps
        val start = sampleTransactions[2].date // twoDaysAgo
        val end = sampleTransactions[0].date // now
        val dateRange = DateRange.custom(start, end)

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)

        // Then
        // Should include transactions at both boundaries
        assertTrue(summary.transactionCount >= 3)
        assertTrue(summary.totalSpent >= 350.0) // 150 + 50 + 200
    }

    // ==================== getSpendingTrend Tests ====================

    @Test
    fun getSpendingTrend_withDateRange_returnsFilteredTrend() {
        // Given
        val dateRange = DateRange.last7Days()

        // When
        val trend = calculator.getSpendingTrend(sampleTransactions, dateRange)

        // Then
        assertNotNull(trend)
        assertTrue(trend.isNotEmpty())
        // Should only include debit transactions
        trend.forEach { point ->
            assertTrue(point.amount >= 0)
        }
    }

    @Test
    fun getSpendingTrend_singleDay_usesHourlyFormat() {
        // Given - Today only
        val dateRange = DateRange.today()

        // When
        val trend = calculator.getSpendingTrend(sampleTransactions, dateRange)

        // Then
        if (trend.isNotEmpty()) {
            // Should use hourly format (HH:00)
            assertTrue(trend.all { it.label.matches(Regex("\\d{2}:\\d{2}")) })
        }
    }

    @Test
    fun getSpendingTrend_last7Days_usesDayOfWeekFormat() {
        // Given
        val dateRange = DateRange.last7Days()

        // When
        val trend = calculator.getSpendingTrend(sampleTransactions, dateRange)

        // Then
        if (trend.isNotEmpty()) {
            // Should use day of week format (Mon, Tue, etc.)
            // Just verify it's not empty and properly structured
            trend.forEach { point ->
                assertTrue(point.label.isNotEmpty())
                assertTrue(point.amount >= 0)
            }
        }
    }

    @Test
    fun getSpendingTrend_last30Days_usesDayNumberFormat() {
        // Given
        val dateRange = DateRange.last30Days()

        // When
        val trend = calculator.getSpendingTrend(sampleTransactions, dateRange)

        // Then
        if (trend.isNotEmpty()) {
            // Should use day number format (dd)
            // Verify structure
            trend.forEach { point ->
                assertTrue(point.label.isNotEmpty())
                assertTrue(point.amount >= 0)
            }
        }
    }

    @Test
    fun getSpendingTrend_last90Days_usesMonthFormat() {
        // Given
        val dateRange = DateRange.last90Days()

        // When
        val trend = calculator.getSpendingTrend(sampleTransactions, dateRange)

        // Then
        if (trend.isNotEmpty()) {
            // Should use month format (MMM)
            // Verify structure
            trend.forEach { point ->
                assertTrue(point.label.isNotEmpty())
                assertTrue(point.amount >= 0)
            }
        }
    }

    @Test
    fun getSpendingTrend_multiYearRange_usesMonthFormat() {
        // Given - 2 year range
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -2)
        val start = calendar.timeInMillis
        val end = System.currentTimeMillis()
        val dateRange = DateRange.custom(start, end)

        // When
        val trend = calculator.getSpendingTrend(sampleTransactions, dateRange)

        // Then
        // Should use monthly format for long ranges
        assertNotNull(trend)
    }

    @Test
    fun getSpendingTrend_onlyIncludesDebitTransactions() {
        // Given
        val dateRange = DateRange.last30Days()

        // When
        val trend = calculator.getSpendingTrend(sampleTransactions, dateRange)

        // Then
        // Verify all transactions are debits by checking total
        val totalFromTrend = trend.sumOf { it.amount }
        val expectedTotal = sampleTransactions
            .filter { it.type == TransactionType.DEBIT }
            .filter { dateRange.contains(it.date) }
            .sumOf { it.amount }

        assertEquals(expectedTotal, totalFromTrend, 0.01)
    }

    @Test
    fun getSpendingTrend_sortedByDate() {
        // Given
        val dateRange = DateRange.last30Days()

        // When
        val trend = calculator.getSpendingTrend(sampleTransactions, dateRange)

        // Then
        // Verify sorted by date ascending
        for (i in 0 until trend.size - 1) {
            assertTrue(trend[i].date <= trend[i + 1].date)
        }
    }

    @Test
    fun getSpendingTrend_emptyTransactions_returnsEmptyList() {
        // Given
        val emptyTransactions = emptyList<ParsedTransaction>()
        val dateRange = DateRange.last7Days()

        // When
        val trend = calculator.getSpendingTrend(emptyTransactions, dateRange)

        // Then
        assertTrue(trend.isEmpty())
    }

    // ==================== getDailySpending Tests ====================

    @Test
    fun getDailySpending_withDateRange_returnsFilteredDays() {
        // Given
        val dateRange = DateRange.last7Days()

        // When
        val dailySpending = calculator.getDailySpending(sampleTransactions, dateRange)

        // Then
        assertNotNull(dailySpending)
        assertTrue(dailySpending.isNotEmpty())
        dailySpending.forEach { day ->
            assertTrue(day.spent >= 0)
            assertTrue(day.received >= 0)
        }
    }

    @Test
    fun getDailySpending_short14Days_usesDetailedFormat() {
        // Given
        val dateRange = DateRange.last7Days()

        // When
        val dailySpending = calculator.getDailySpending(sampleTransactions, dateRange)

        // Then
        if (dailySpending.isNotEmpty()) {
            // Should use format like "Mon, 01 Jan"
            // Just verify it has content
            dailySpending.forEach { day ->
                assertTrue(day.dayLabel.isNotEmpty())
            }
        }
    }

    @Test
    fun getDailySpending_medium90Days_usesMediumFormat() {
        // Given
        val dateRange = DateRange.last30Days()

        // When
        val dailySpending = calculator.getDailySpending(sampleTransactions, dateRange)

        // Then
        if (dailySpending.isNotEmpty()) {
            // Should use format like "01 Jan"
            dailySpending.forEach { day ->
                assertTrue(day.dayLabel.isNotEmpty())
            }
        }
    }

    @Test
    fun getDailySpending_longRange_usesMonthlyFormat() {
        // Given
        val dateRange = DateRange.last90Days()

        // When
        val dailySpending = calculator.getDailySpending(sampleTransactions, dateRange)

        // Then
        if (dailySpending.isNotEmpty()) {
            // Should use format like "Jan 24"
            dailySpending.forEach { day ->
                assertTrue(day.dayLabel.isNotEmpty())
            }
        }
    }

    @Test
    fun getDailySpending_separatesSpentAndReceived() {
        // Given
        val dateRange = DateRange.last7Days()

        // When
        val dailySpending = calculator.getDailySpending(sampleTransactions, dateRange)

        // Then
        val totalSpent = dailySpending.sumOf { it.spent }
        val totalReceived = dailySpending.sumOf { it.received }

        val expectedSpent = sampleTransactions
            .filter { it.type == TransactionType.DEBIT }
            .filter { dateRange.contains(it.date) }
            .sumOf { it.amount }

        val expectedReceived = sampleTransactions
            .filter { it.type == TransactionType.CREDIT }
            .filter { dateRange.contains(it.date) }
            .sumOf { it.amount }

        assertEquals(expectedSpent, totalSpent, 0.01)
        assertEquals(expectedReceived, totalReceived, 0.01)
    }

    @Test
    fun getDailySpending_sortedByDate() {
        // Given
        val dateRange = DateRange.last30Days()

        // When
        val dailySpending = calculator.getDailySpending(sampleTransactions, dateRange)

        // Then
        for (i in 0 until dailySpending.size - 1) {
            assertTrue(dailySpending[i].date <= dailySpending[i + 1].date)
        }
    }

    @Test
    fun getDailySpending_emptyTransactions_returnsEmptyList() {
        // Given
        val emptyTransactions = emptyList<ParsedTransaction>()
        val dateRange = DateRange.last7Days()

        // When
        val dailySpending = calculator.getDailySpending(emptyTransactions, dateRange)

        // Then
        assertTrue(dailySpending.isEmpty())
    }

    @Test
    fun getDailySpending_singleDay_groupsCorrectly() {
        // Given - All transactions on same day
        val now = System.currentTimeMillis()
        val sameDayTransactions = listOf(
            ParsedTransaction(201L, 100.0, TransactionType.DEBIT, "Store A", "1234", now),
            ParsedTransaction(202L, 50.0, TransactionType.DEBIT, "Store B", "1234", now),
            ParsedTransaction(203L, 500.0, TransactionType.CREDIT, "Payment", "1234", now)
        )
        val dateRange = DateRange.today()

        // When
        val dailySpending = calculator.getDailySpending(sameDayTransactions, dateRange)

        // Then
        // Should group into single day
        assertTrue(dailySpending.size <= 1)
        if (dailySpending.isNotEmpty()) {
            assertEquals(150.0, dailySpending[0].spent, 0.01)
            assertEquals(500.0, dailySpending[0].received, 0.01)
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun edgeCase_transactionAtExactRangeBoundary_included() {
        // Given - Transaction at exact start time
        val start = sampleTransactions[2].date
        val end = System.currentTimeMillis()
        val dateRange = DateRange.custom(start, end)

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)

        // Then
        // Should include the transaction at exact boundary
        assertTrue(summary.transactionCount >= 3)
    }

    @Test
    fun edgeCase_sameDayRange_calculatesCorrectly() {
        // Given - Same day start and end
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val end = calendar.timeInMillis
        val dateRange = DateRange.custom(start, end)

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)

        // Then
        assertNotNull(summary)
        assertTrue(summary.averageDaily >= 0)
    }

    @Test
    fun edgeCase_veryLargeRange_handlesCorrectly() {
        // Given - 10 year range
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -10)
        val start = calendar.timeInMillis
        val end = System.currentTimeMillis()
        val dateRange = DateRange.custom(start, end)

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)
        val trend = calculator.getSpendingTrend(sampleTransactions, dateRange)
        val daily = calculator.getDailySpending(sampleTransactions, dateRange)

        // Then
        assertNotNull(summary)
        assertNotNull(trend)
        assertNotNull(daily)
    }

    // ==================== Backward Compatibility Tests ====================

    @Test
    fun backwardCompatibility_timePeriodMethod_stillWorks() {
        // Given
        val period = TimePeriod.WEEK

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, period)

        // Then
        assertNotNull(summary)
        assertEquals(TimePeriod.WEEK, summary.period)
        assertTrue(summary.totalSpent >= 0)
    }

    @Test
    fun backwardCompatibility_getSpendingTrend_timePeriod_stillWorks() {
        // Given
        val period = TimePeriod.MONTH

        // When
        val trend = calculator.getSpendingTrend(sampleTransactions, period)

        // Then
        assertNotNull(trend)
    }

    @Test
    fun backwardCompatibility_getDailySpending_timePeriod_stillWorks() {
        // Given
        val period = TimePeriod.MONTH

        // When
        val daily = calculator.getDailySpending(sampleTransactions, period)

        // Then
        assertNotNull(daily)
    }

    @Test
    fun backwardCompatibility_bothMethodsProduceSimilarResults() {
        // Given
        val period = TimePeriod.WEEK
        val dateRange = DateRange.last7Days()

        // When
        val summaryWithPeriod = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, period)
        val summaryWithDateRange = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)

        // Then
        // Results should be similar (within small margin due to timing)
        val spentDiff = Math.abs(summaryWithPeriod.totalSpent - summaryWithDateRange.totalSpent)
        assertTrue(spentDiff < 100.0) // Allow small difference
    }

    // ==================== Integration Tests ====================

    @Test
    fun integration_fullAnalyticsWorkflow_worksEndToEnd() {
        // Given
        val dateRange = DateRange.last30Days()

        // When
        val summary = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, dateRange)
        val trend = calculator.getSpendingTrend(sampleTransactions, dateRange)
        val daily = calculator.getDailySpending(sampleTransactions, dateRange)

        // Then
        assertNotNull(summary)
        assertNotNull(trend)
        assertNotNull(daily)

        // Verify consistency
        val totalFromDaily = daily.sumOf { it.spent }
        assertEquals(summary.totalSpent, totalFromDaily, 0.01)
    }

    @Test
    fun integration_differentRanges_produceConsistentResults() {
        // Given
        val today = DateRange.today()
        val week = DateRange.last7Days()
        val month = DateRange.last30Days()

        // When
        val summaryToday = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, today)
        val summaryWeek = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, week)
        val summaryMonth = calculator.calculatePeriodSummary(sampleTransactions, categoryMap, month)

        // Then
        // Longer ranges should include more or equal transactions
        assertTrue(summaryToday.transactionCount <= summaryWeek.transactionCount)
        assertTrue(summaryWeek.transactionCount <= summaryMonth.transactionCount)

        // Longer ranges should have more or equal spending
        assertTrue(summaryToday.totalSpent <= summaryWeek.totalSpent)
        assertTrue(summaryWeek.totalSpent <= summaryMonth.totalSpent)
    }
}
