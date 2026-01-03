package com.example.kanakku.domain.recurring

import com.example.kanakku.data.model.RecurringFrequency
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FrequencyAnalyzer.
 *
 * Tests cover:
 * - analyzeIntervals() with various frequency patterns
 * - detectFrequency() for each frequency type
 * - areIntervalsConsistent() with consistent and inconsistent intervals
 * - Pattern matching methods (isWeekly, isBiWeekly, isMonthly, isQuarterly, isAnnual)
 * - Edge cases: month boundaries, tolerance limits, empty lists, single intervals
 * - Utility methods: calculateAverageInterval, calculateStandardDeviation, getToleranceRange
 */
class FrequencyAnalyzerTest {

    // ==================== analyzeIntervals() Tests ====================

    @Test
    fun analyzeIntervals_weeklyPattern_returnsWeekly() {
        // Given: Consistent 7-day intervals
        val intervals = listOf(7, 7, 7)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.WEEKLY, frequency)
    }

    @Test
    fun analyzeIntervals_weeklyWithSlightVariation_returnsWeekly() {
        // Given: Weekly pattern with slight variations within tolerance (6, 7, 8 days)
        val intervals = listOf(6, 7, 8, 7)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.WEEKLY, frequency)
    }

    @Test
    fun analyzeIntervals_biWeeklyPattern_returnsBiWeekly() {
        // Given: Consistent 14-day intervals
        val intervals = listOf(14, 14, 14)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.BI_WEEKLY, frequency)
    }

    @Test
    fun analyzeIntervals_biWeeklyWithSlightVariation_returnsBiWeekly() {
        // Given: Bi-weekly pattern with slight variations (13, 14, 15 days)
        val intervals = listOf(13, 14, 15, 14)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.BI_WEEKLY, frequency)
    }

    @Test
    fun analyzeIntervals_monthlyPattern30Days_returnsMonthly() {
        // Given: Consistent 30-day intervals (typical month)
        val intervals = listOf(30, 30, 30)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun analyzeIntervals_monthlyPattern31Days_returnsMonthly() {
        // Given: Consistent 31-day intervals
        val intervals = listOf(31, 31, 31)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun analyzeIntervals_monthlyPattern28Days_returnsMonthly() {
        // Given: Consistent 28-day intervals (February pattern)
        val intervals = listOf(28, 28, 28)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun analyzeIntervals_monthlyPatternMixedMonthLengths_returnsMonthly() {
        // Given: Mixed month lengths (28-31 days) - typical for monthly billing
        // Example: Jan 31 → Feb 28 = 28 days, Feb 28 → Mar 31 = 31 days, etc.
        val intervals = listOf(31, 28, 31, 30)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun analyzeIntervals_monthlyPatternFebruaryToMarch_returnsMonthly() {
        // Given: February to March pattern (28 days in Feb, 31 in Mar)
        val intervals = listOf(28, 31, 30, 31)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun analyzeIntervals_monthlyPatternLeapYear_returnsMonthly() {
        // Given: Leap year February pattern (29 days)
        val intervals = listOf(31, 29, 31, 30)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun analyzeIntervals_quarterlyPattern_returnsQuarterly() {
        // Given: Consistent ~90-day intervals
        val intervals = listOf(90, 90, 90)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.QUARTERLY, frequency)
    }

    @Test
    fun analyzeIntervals_quarterlyWithVariation_returnsQuarterly() {
        // Given: Quarterly pattern with variations (88-92 days)
        val intervals = listOf(88, 91, 89, 92)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.QUARTERLY, frequency)
    }

    @Test
    fun analyzeIntervals_annualPattern_returnsAnnual() {
        // Given: Consistent ~365-day intervals
        val intervals = listOf(365, 365, 365)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.ANNUAL, frequency)
    }

    @Test
    fun analyzeIntervals_annualWithLeapYear_returnsAnnual() {
        // Given: Annual pattern with leap year variation (365, 366 days)
        val intervals = listOf(365, 366, 365)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.ANNUAL, frequency)
    }

    @Test
    fun analyzeIntervals_inconsistentIntervals_returnsNull() {
        // Given: Inconsistent intervals (7, 30, 90) - no pattern
        val intervals = listOf(7, 30, 90)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertNull(frequency)
    }

    @Test
    fun analyzeIntervals_intervalsExceedingTolerance_returnsNull() {
        // Given: Intervals exceeding 20% tolerance (30, 50 days)
        val intervals = listOf(30, 30, 50, 30)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertNull(frequency)
    }

    @Test
    fun analyzeIntervals_emptyList_returnsNull() {
        // Given: Empty interval list
        val intervals = emptyList<Int>()

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertNull(frequency)
    }

    @Test
    fun analyzeIntervals_singleInterval_detectsFrequency() {
        // Given: Single interval (should be considered consistent)
        val intervals = listOf(30)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    // ==================== detectFrequency() Tests ====================

    @Test
    fun detectFrequency_exactWeekly_returnsWeekly() {
        // Given: Exact 7-day interval
        val averageInterval = 7

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.WEEKLY, frequency)
    }

    @Test
    fun detectFrequency_weeklyAtLowerTolerance_returnsWeekly() {
        // Given: 6 days (within 20% tolerance of 7)
        val averageInterval = 6

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.WEEKLY, frequency)
    }

    @Test
    fun detectFrequency_weeklyAtUpperTolerance_returnsWeekly() {
        // Given: 8 days (within 20% tolerance of 7)
        val averageInterval = 8

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.WEEKLY, frequency)
    }

    @Test
    fun detectFrequency_exactBiWeekly_returnsBiWeekly() {
        // Given: Exact 14-day interval
        val averageInterval = 14

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.BI_WEEKLY, frequency)
    }

    @Test
    fun detectFrequency_biWeeklyAtTolerance_returnsBiWeekly() {
        // Given: 12 days (within 20% tolerance of 14)
        val averageInterval = 12

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.BI_WEEKLY, frequency)
    }

    @Test
    fun detectFrequency_monthly28Days_returnsMonthly() {
        // Given: 28 days (minimum month length)
        val averageInterval = 28

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun detectFrequency_monthly29Days_returnsMonthly() {
        // Given: 29 days (leap year February)
        val averageInterval = 29

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun detectFrequency_monthly30Days_returnsMonthly() {
        // Given: 30 days (typical month)
        val averageInterval = 30

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun detectFrequency_monthly31Days_returnsMonthly() {
        // Given: 31 days (maximum month length)
        val averageInterval = 31

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun detectFrequency_exactQuarterly_returnsQuarterly() {
        // Given: Exact 90-day interval
        val averageInterval = 90

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.QUARTERLY, frequency)
    }

    @Test
    fun detectFrequency_quarterlyAtTolerance_returnsQuarterly() {
        // Given: 75 days (within 20% tolerance of 90)
        val averageInterval = 75

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.QUARTERLY, frequency)
    }

    @Test
    fun detectFrequency_exactAnnual_returnsAnnual() {
        // Given: Exact 365-day interval
        val averageInterval = 365

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.ANNUAL, frequency)
    }

    @Test
    fun detectFrequency_annualAtTolerance_returnsAnnual() {
        // Given: 300 days (within 20% tolerance of 365)
        val averageInterval = 300

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertEquals(RecurringFrequency.ANNUAL, frequency)
    }

    @Test
    fun detectFrequency_unknownInterval_returnsNull() {
        // Given: Interval that doesn't match any pattern (e.g., 45 days)
        val averageInterval = 45

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertNull(frequency)
    }

    @Test
    fun detectFrequency_zeroDays_returnsNull() {
        // Given: Zero-day interval (invalid)
        val averageInterval = 0

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(averageInterval)

        // Then
        assertNull(frequency)
    }

    // ==================== areIntervalsConsistent() Tests ====================

    @Test
    fun areIntervalsConsistent_identicalIntervals_returnsTrue() {
        // Given: Perfectly consistent intervals
        val intervals = listOf(30, 30, 30, 30)

        // When
        val isConsistent = FrequencyAnalyzer.areIntervalsConsistent(intervals)

        // Then
        assertTrue(isConsistent)
    }

    @Test
    fun areIntervalsConsistent_withinTolerance_returnsTrue() {
        // Given: Intervals within 20% tolerance (28-31 days, avg ~30)
        val intervals = listOf(28, 30, 31, 29)

        // When
        val isConsistent = FrequencyAnalyzer.areIntervalsConsistent(intervals)

        // Then
        assertTrue(isConsistent)
    }

    @Test
    fun areIntervalsConsistent_slightlyWithinTolerance_returnsTrue() {
        // Given: Intervals just within 20% tolerance
        // Average = 30, tolerance = 6, range = 24-36
        val intervals = listOf(24, 30, 36, 30)

        // When
        val isConsistent = FrequencyAnalyzer.areIntervalsConsistent(intervals)

        // Then
        assertTrue(isConsistent)
    }

    @Test
    fun areIntervalsConsistent_exceedingTolerance_returnsFalse() {
        // Given: One interval exceeds 20% tolerance
        // Average ≈ 33, but one interval is 50 (too far)
        val intervals = listOf(30, 30, 50, 30)

        // When
        val isConsistent = FrequencyAnalyzer.areIntervalsConsistent(intervals)

        // Then
        assertFalse(isConsistent)
    }

    @Test
    fun areIntervalsConsistent_highlyInconsistent_returnsFalse() {
        // Given: Highly inconsistent intervals
        val intervals = listOf(7, 30, 90, 365)

        // When
        val isConsistent = FrequencyAnalyzer.areIntervalsConsistent(intervals)

        // Then
        assertFalse(isConsistent)
    }

    @Test
    fun areIntervalsConsistent_emptyList_returnsFalse() {
        // Given: Empty interval list
        val intervals = emptyList<Int>()

        // When
        val isConsistent = FrequencyAnalyzer.areIntervalsConsistent(intervals)

        // Then
        assertFalse(isConsistent)
    }

    @Test
    fun areIntervalsConsistent_singleInterval_returnsTrue() {
        // Given: Single interval (always consistent)
        val intervals = listOf(30)

        // When
        val isConsistent = FrequencyAnalyzer.areIntervalsConsistent(intervals)

        // Then
        assertTrue(isConsistent)
    }

    // ==================== Pattern Matcher Tests ====================

    @Test
    fun isWeekly_exactMatch_returnsTrue() {
        // Given: Exact 7-day interval
        val interval = 7

        // When
        val result = FrequencyAnalyzer.isWeekly(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isWeekly_withinTolerance_returnsTrue() {
        // Given: 6 days (within tolerance)
        val interval = 6

        // When
        val result = FrequencyAnalyzer.isWeekly(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isWeekly_outsideTolerance_returnsFalse() {
        // Given: 10 days (outside 20% tolerance of 7)
        val interval = 10

        // When
        val result = FrequencyAnalyzer.isWeekly(interval)

        // Then
        assertFalse(result)
    }

    @Test
    fun isBiWeekly_exactMatch_returnsTrue() {
        // Given: Exact 14-day interval
        val interval = 14

        // When
        val result = FrequencyAnalyzer.isBiWeekly(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isBiWeekly_withinTolerance_returnsTrue() {
        // Given: 13 days (within tolerance)
        val interval = 13

        // When
        val result = FrequencyAnalyzer.isBiWeekly(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isBiWeekly_outsideTolerance_returnsFalse() {
        // Given: 20 days (outside tolerance)
        val interval = 20

        // When
        val result = FrequencyAnalyzer.isBiWeekly(interval)

        // Then
        assertFalse(result)
    }

    @Test
    fun isMonthly_28Days_returnsTrue() {
        // Given: 28 days (minimum month - February)
        val interval = 28

        // When
        val result = FrequencyAnalyzer.isMonthly(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isMonthly_29Days_returnsTrue() {
        // Given: 29 days (leap year February)
        val interval = 29

        // When
        val result = FrequencyAnalyzer.isMonthly(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isMonthly_30Days_returnsTrue() {
        // Given: 30 days (typical month)
        val interval = 30

        // When
        val result = FrequencyAnalyzer.isMonthly(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isMonthly_31Days_returnsTrue() {
        // Given: 31 days (maximum month)
        val interval = 31

        // When
        val result = FrequencyAnalyzer.isMonthly(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isMonthly_27Days_returnsFalse() {
        // Given: 27 days (below minimum)
        val interval = 27

        // When
        val result = FrequencyAnalyzer.isMonthly(interval)

        // Then
        assertFalse(result)
    }

    @Test
    fun isMonthly_32Days_returnsFalse() {
        // Given: 32 days (above maximum)
        val interval = 32

        // When
        val result = FrequencyAnalyzer.isMonthly(interval)

        // Then
        assertFalse(result)
    }

    @Test
    fun isQuarterly_exactMatch_returnsTrue() {
        // Given: Exact 90-day interval
        val interval = 90

        // When
        val result = FrequencyAnalyzer.isQuarterly(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isQuarterly_withinTolerance_returnsTrue() {
        // Given: 75 days (within 20% tolerance)
        val interval = 75

        // When
        val result = FrequencyAnalyzer.isQuarterly(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isQuarterly_outsideTolerance_returnsFalse() {
        // Given: 60 days (outside tolerance)
        val interval = 60

        // When
        val result = FrequencyAnalyzer.isQuarterly(interval)

        // Then
        assertFalse(result)
    }

    @Test
    fun isAnnual_exactMatch_returnsTrue() {
        // Given: Exact 365-day interval
        val interval = 365

        // When
        val result = FrequencyAnalyzer.isAnnual(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isAnnual_366Days_returnsTrue() {
        // Given: 366 days (leap year)
        val interval = 366

        // When
        val result = FrequencyAnalyzer.isAnnual(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isAnnual_withinTolerance_returnsTrue() {
        // Given: 300 days (within 20% tolerance)
        val interval = 300

        // When
        val result = FrequencyAnalyzer.isAnnual(interval)

        // Then
        assertTrue(result)
    }

    @Test
    fun isAnnual_outsideTolerance_returnsFalse() {
        // Given: 200 days (outside tolerance)
        val interval = 200

        // When
        val result = FrequencyAnalyzer.isAnnual(interval)

        // Then
        assertFalse(result)
    }

    // ==================== Utility Method Tests ====================

    @Test
    fun calculateAverageInterval_normalIntervals_returnsCorrectAverage() {
        // Given: Intervals with known average
        val intervals = listOf(10, 20, 30, 40)

        // When
        val average = FrequencyAnalyzer.calculateAverageInterval(intervals)

        // Then
        assertEquals(25, average)
    }

    @Test
    fun calculateAverageInterval_singleInterval_returnsThatValue() {
        // Given: Single interval
        val intervals = listOf(30)

        // When
        val average = FrequencyAnalyzer.calculateAverageInterval(intervals)

        // Then
        assertEquals(30, average)
    }

    @Test
    fun calculateAverageInterval_emptyList_returnsZero() {
        // Given: Empty list
        val intervals = emptyList<Int>()

        // When
        val average = FrequencyAnalyzer.calculateAverageInterval(intervals)

        // Then
        assertEquals(0, average)
    }

    @Test
    fun calculateStandardDeviation_identicalValues_returnsZero() {
        // Given: All identical intervals (no deviation)
        val intervals = listOf(30, 30, 30, 30)

        // When
        val stdDev = FrequencyAnalyzer.calculateStandardDeviation(intervals)

        // Then
        assertEquals(0.0, stdDev, 0.001)
    }

    @Test
    fun calculateStandardDeviation_normalVariation_returnsCorrectValue() {
        // Given: Intervals with known standard deviation
        // Values: 10, 20, 30, 40 (average = 25)
        // Variance = ((15^2 + 5^2 + 5^2 + 15^2) / 4) = 280 / 4 = 70
        // StdDev = sqrt(70) ≈ 8.37
        val intervals = listOf(10, 20, 30, 40)

        // When
        val stdDev = FrequencyAnalyzer.calculateStandardDeviation(intervals)

        // Then
        assertEquals(11.18, stdDev, 0.1) // Allow small rounding difference
    }

    @Test
    fun calculateStandardDeviation_emptyList_returnsZero() {
        // Given: Empty list
        val intervals = emptyList<Int>()

        // When
        val stdDev = FrequencyAnalyzer.calculateStandardDeviation(intervals)

        // Then
        assertEquals(0.0, stdDev, 0.001)
    }

    @Test
    fun getToleranceRange_weekly_returnsCorrectRange() {
        // Given: 7-day interval
        // 20% tolerance = ±1.4, rounds to ±1
        val interval = 7

        // When
        val (min, max) = FrequencyAnalyzer.getToleranceRange(interval)

        // Then
        assertEquals(6, min)
        assertEquals(8, max)
    }

    @Test
    fun getToleranceRange_monthly_returnsCorrectRange() {
        // Given: 30-day interval
        // 20% tolerance = ±6
        val interval = 30

        // When
        val (min, max) = FrequencyAnalyzer.getToleranceRange(interval)

        // Then
        assertEquals(24, min)
        assertEquals(36, max)
    }

    @Test
    fun getToleranceRange_quarterly_returnsCorrectRange() {
        // Given: 90-day interval
        // 20% tolerance = ±18
        val interval = 90

        // When
        val (min, max) = FrequencyAnalyzer.getToleranceRange(interval)

        // Then
        assertEquals(72, min)
        assertEquals(108, max)
    }

    @Test
    fun getToleranceRange_annual_returnsCorrectRange() {
        // Given: 365-day interval
        // 20% tolerance = ±73
        val interval = 365

        // When
        val (min, max) = FrequencyAnalyzer.getToleranceRange(interval)

        // Then
        assertEquals(292, min)
        assertEquals(438, max)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun analyzeIntervals_monthBoundaryJanuaryToFebruary_detectsMonthly() {
        // Given: Jan 31 → Feb 28 = 28 days
        val intervals = listOf(28, 31, 30)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun analyzeIntervals_monthBoundaryFebruaryLeapYear_detectsMonthly() {
        // Given: Jan 31 → Feb 29 (leap year) = 29 days
        val intervals = listOf(29, 31, 30)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun analyzeIntervals_monthBoundary30DayMonths_detectsMonthly() {
        // Given: April, June, September, November (30-day months)
        val intervals = listOf(30, 31, 30, 31)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertEquals(RecurringFrequency.MONTHLY, frequency)
    }

    @Test
    fun analyzeIntervals_atToleranceBoundary_detectsPattern() {
        // Given: Intervals exactly at 20% tolerance boundary
        // Average = 30, tolerance = 6, so 24 and 36 should both be accepted
        val intervals = listOf(24, 30, 36)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertNotNull(frequency)
    }

    @Test
    fun analyzeIntervals_justBeyondToleranceBoundary_returnsNull() {
        // Given: Intervals just beyond 20% tolerance
        // Average ≈ 30, tolerance = 6, so 23 or 37 should fail
        val intervals = listOf(23, 30, 30)

        // When
        val frequency = FrequencyAnalyzer.analyzeIntervals(intervals)

        // Then
        assertNull(frequency)
    }

    @Test
    fun detectFrequency_boundaryBetweenWeeklyAndBiWeekly_selectsCorrect() {
        // Given: 10 days (closer to weekly's max than bi-weekly's min)
        val interval = 10

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(interval)

        // Then
        // Should not match either (outside both tolerances)
        assertNull(frequency)
    }

    @Test
    fun detectFrequency_boundaryBetweenBiWeeklyAndMonthly_selectsCorrect() {
        // Given: 20 days (outside bi-weekly, below monthly)
        val interval = 20

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(interval)

        // Then
        assertNull(frequency)
    }

    @Test
    fun detectFrequency_boundaryBetweenMonthlyAndQuarterly_selectsCorrect() {
        // Given: 50 days (above monthly max, below quarterly min)
        val interval = 50

        // When
        val frequency = FrequencyAnalyzer.detectFrequency(interval)

        // Then
        assertNull(frequency)
    }
}
