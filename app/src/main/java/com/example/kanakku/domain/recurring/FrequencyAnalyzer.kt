package com.example.kanakku.domain.recurring

import com.example.kanakku.data.model.RecurringFrequency
import kotlin.math.abs

/**
 * Utility for analyzing transaction intervals to detect recurring frequency patterns.
 *
 * This analyzer examines time intervals between transactions to identify patterns such as:
 * - Weekly: 7 days (±20% tolerance)
 * - Bi-weekly: 14 days (±20% tolerance)
 * - Monthly: 28-31 days (flexible range for different month lengths)
 * - Quarterly: 90 days (±20% tolerance)
 * - Annual: 365 days (±20% tolerance)
 *
 * The analyzer uses statistical consistency checks to ensure intervals are regular enough
 * to be considered recurring. Patterns must be consistent within the tolerance threshold.
 */
object FrequencyAnalyzer {

    /**
     * Interval tolerance as a fraction (20% = 0.20).
     * This allows for some variation in intervals while still detecting patterns.
     */
    private const val INTERVAL_TOLERANCE = 0.20

    /**
     * Days in a week pattern.
     */
    private const val DAYS_IN_WEEK = 7

    /**
     * Days in a bi-weekly pattern.
     */
    private const val DAYS_IN_BI_WEEKLY = 14

    /**
     * Minimum days in a month (accounting for February).
     */
    private const val DAYS_IN_MONTH_MIN = 28

    /**
     * Maximum days in a month.
     */
    private const val DAYS_IN_MONTH_MAX = 31

    /**
     * Days in a quarter (approximately).
     */
    private const val DAYS_IN_QUARTER = 90

    /**
     * Days in a year (approximately).
     */
    private const val DAYS_IN_YEAR = 365

    /**
     * Analyzes a list of intervals and detects the recurring frequency pattern.
     *
     * The method first checks if the intervals are consistent enough to be considered
     * recurring. If they are consistent, it attempts to match them to known frequency
     * patterns (weekly, bi-weekly, monthly, quarterly, annual).
     *
     * Examples:
     * ```
     * analyzeIntervals(listOf(7, 7, 7))           → WEEKLY
     * analyzeIntervals(listOf(14, 14, 13))        → BI_WEEKLY
     * analyzeIntervals(listOf(30, 31, 30, 29))    → MONTHLY (handles month variations)
     * analyzeIntervals(listOf(90, 92, 89))        → QUARTERLY
     * analyzeIntervals(listOf(365, 366, 365))     → ANNUAL
     * analyzeIntervals(listOf(7, 30, 90))         → null (inconsistent)
     * ```
     *
     * @param intervals List of intervals in days between consecutive transactions
     * @return Detected RecurringFrequency if pattern is consistent, null otherwise
     */
    fun analyzeIntervals(intervals: List<Int>): RecurringFrequency? {
        if (intervals.isEmpty()) {
            return null
        }

        // Check if intervals are consistent enough to be considered recurring
        if (!areIntervalsConsistent(intervals)) {
            return null
        }

        // Calculate average interval for pattern matching
        val averageInterval = intervals.average().toInt()

        // Match to known frequency patterns
        return detectFrequency(averageInterval)
    }

    /**
     * Detects the recurring frequency based on average interval.
     *
     * Matches the average interval to known frequency patterns using tolerance-based
     * comparison. Monthly patterns have special handling to account for varying month
     * lengths (28-31 days).
     *
     * @param averageInterval Average days between transactions
     * @return RecurringFrequency if pattern matches a known frequency, null otherwise
     */
    fun detectFrequency(averageInterval: Int): RecurringFrequency? {
        return when {
            isWeekly(averageInterval) -> RecurringFrequency.WEEKLY
            isBiWeekly(averageInterval) -> RecurringFrequency.BI_WEEKLY
            isMonthly(averageInterval) -> RecurringFrequency.MONTHLY
            isQuarterly(averageInterval) -> RecurringFrequency.QUARTERLY
            isAnnual(averageInterval) -> RecurringFrequency.ANNUAL
            else -> null // Interval doesn't match known patterns
        }
    }

    /**
     * Checks if the intervals are consistent enough to be considered recurring.
     *
     * Uses the coefficient of variation approach to measure consistency. Each interval
     * must be within the tolerance threshold of the average interval.
     *
     * Example:
     * ```
     * areIntervalsConsistent(listOf(30, 31, 29, 30))  → true (all within 20% of avg)
     * areIntervalsConsistent(listOf(7, 30, 90))       → false (too inconsistent)
     * areIntervalsConsistent(listOf(7, 8, 6))         → true (all within 20% of avg)
     * ```
     *
     * @param intervals List of intervals in days
     * @return True if intervals are consistent within tolerance, false otherwise
     */
    fun areIntervalsConsistent(intervals: List<Int>): Boolean {
        if (intervals.isEmpty()) {
            return false
        }

        // Single interval is always consistent
        if (intervals.size == 1) {
            return true
        }

        val average = intervals.average()

        // Check if each interval is within tolerance of average
        return intervals.all { interval ->
            val deviation = abs(interval - average)
            deviation <= (average * INTERVAL_TOLERANCE)
        }
    }

    /**
     * Checks if the average interval matches weekly pattern (7 days ± tolerance).
     *
     * Weekly patterns are expected to be around 7 days, with ±20% tolerance
     * allowing for intervals between 5.6 and 8.4 days.
     *
     * @param averageInterval Average days between transactions
     * @return True if interval matches weekly pattern
     */
    fun isWeekly(averageInterval: Int): Boolean {
        val tolerance = (DAYS_IN_WEEK * INTERVAL_TOLERANCE).toInt()
        return abs(averageInterval - DAYS_IN_WEEK) <= tolerance
    }

    /**
     * Checks if the average interval matches bi-weekly pattern (14 days ± tolerance).
     *
     * Bi-weekly patterns are expected to be around 14 days, with ±20% tolerance
     * allowing for intervals between 11.2 and 16.8 days.
     *
     * @param averageInterval Average days between transactions
     * @return True if interval matches bi-weekly pattern
     */
    fun isBiWeekly(averageInterval: Int): Boolean {
        val tolerance = (DAYS_IN_BI_WEEKLY * INTERVAL_TOLERANCE).toInt()
        return abs(averageInterval - DAYS_IN_BI_WEEKLY) <= tolerance
    }

    /**
     * Checks if the average interval matches monthly pattern (28-31 days).
     *
     * Monthly patterns have flexible tolerance to account for different month lengths:
     * - February: 28 days (29 in leap years)
     * - April, June, September, November: 30 days
     * - January, March, May, July, August, October, December: 31 days
     *
     * This method accepts any interval within the 28-31 day range without requiring
     * strict tolerance checks, as month-to-month variations are expected and normal.
     *
     * @param averageInterval Average days between transactions
     * @return True if interval matches monthly pattern (28-31 days inclusive)
     */
    fun isMonthly(averageInterval: Int): Boolean {
        return averageInterval in DAYS_IN_MONTH_MIN..DAYS_IN_MONTH_MAX
    }

    /**
     * Checks if the average interval matches quarterly pattern (90 days ± tolerance).
     *
     * Quarterly patterns are expected to be around 90 days (approximately 3 months),
     * with ±20% tolerance allowing for intervals between 72 and 108 days.
     *
     * @param averageInterval Average days between transactions
     * @return True if interval matches quarterly pattern
     */
    fun isQuarterly(averageInterval: Int): Boolean {
        val tolerance = (DAYS_IN_QUARTER * INTERVAL_TOLERANCE).toInt()
        return abs(averageInterval - DAYS_IN_QUARTER) <= tolerance
    }

    /**
     * Checks if the average interval matches annual pattern (365 days ± tolerance).
     *
     * Annual patterns are expected to be around 365 days (1 year), with ±20%
     * tolerance allowing for intervals between 292 and 438 days. This accounts
     * for leap years and variations in yearly billing cycles.
     *
     * @param averageInterval Average days between transactions
     * @return True if interval matches annual pattern
     */
    fun isAnnual(averageInterval: Int): Boolean {
        val tolerance = (DAYS_IN_YEAR * INTERVAL_TOLERANCE).toInt()
        return abs(averageInterval - DAYS_IN_YEAR) <= tolerance
    }

    /**
     * Calculates the average interval from a list of intervals.
     *
     * This is a convenience method for getting the average interval in days.
     *
     * @param intervals List of intervals in days
     * @return Average interval, or 0 if list is empty
     */
    fun calculateAverageInterval(intervals: List<Int>): Int {
        return if (intervals.isEmpty()) 0 else intervals.average().toInt()
    }

    /**
     * Calculates the standard deviation of intervals.
     *
     * This can be used to measure the consistency of intervals. Lower standard
     * deviation indicates more regular patterns.
     *
     * @param intervals List of intervals in days
     * @return Standard deviation of intervals, or 0.0 if list is empty
     */
    fun calculateStandardDeviation(intervals: List<Int>): Double {
        if (intervals.isEmpty()) {
            return 0.0
        }

        val average = intervals.average()
        val variance = intervals.map { (it - average) * (it - average) }.average()
        return kotlin.math.sqrt(variance)
    }

    /**
     * Gets the tolerance range for a given average interval.
     *
     * Returns a Pair of (min, max) acceptable values based on the tolerance threshold.
     *
     * Example:
     * ```
     * getToleranceRange(30)  → Pair(24, 36)  // 30 ± 20%
     * getToleranceRange(7)   → Pair(5, 9)    // 7 ± 20%
     * ```
     *
     * @param averageInterval Average interval in days
     * @return Pair of (minimum, maximum) acceptable interval values
     */
    fun getToleranceRange(averageInterval: Int): Pair<Int, Int> {
        val tolerance = (averageInterval * INTERVAL_TOLERANCE).toInt()
        return Pair(averageInterval - tolerance, averageInterval + tolerance)
    }
}
