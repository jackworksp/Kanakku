package com.example.kanakku.domain.recurring

import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.RecurringFrequency
import com.example.kanakku.data.model.RecurringTransaction
import com.example.kanakku.data.model.RecurringType
import com.example.kanakku.data.model.TransactionType
import java.util.UUID
import kotlin.math.abs

/**
 * Detects recurring transaction patterns from a list of parsed transactions.
 *
 * The detector analyzes transactions to identify patterns based on:
 * - Merchant similarity (normalized name matching)
 * - Amount tolerance (±5% variance allowed)
 * - Time intervals (weekly, bi-weekly, monthly, quarterly, annual)
 *
 * A pattern is considered recurring if:
 * 1. At least 3 transactions match the same merchant
 * 2. Amounts are within 5% of each other
 * 3. Time intervals between transactions are consistent (±20% tolerance)
 */
class RecurringTransactionDetector {

    companion object {
        /** Minimum number of transactions required to consider a pattern as recurring */
        private const val MIN_RECURRING_COUNT = 3

        /** Amount tolerance as a fraction (5% = 0.05) */
        private const val AMOUNT_TOLERANCE = 0.05

        /** Interval tolerance as a fraction (20% = 0.20) */
        private const val INTERVAL_TOLERANCE = 0.20

        /** Days in a week */
        private const val DAYS_IN_WEEK = 7

        /** Days in a bi-weekly period */
        private const val DAYS_IN_BI_WEEKLY = 14

        /** Minimum days in a month (accounting for February) */
        private const val DAYS_IN_MONTH_MIN = 28

        /** Maximum days in a month */
        private const val DAYS_IN_MONTH_MAX = 31

        /** Days in a quarter (approximately) */
        private const val DAYS_IN_QUARTER = 90

        /** Days in a year (approximately) */
        private const val DAYS_IN_YEAR = 365

        /** Milliseconds in a day */
        private const val MILLIS_IN_DAY = 24 * 60 * 60 * 1000L
    }

    /**
     * Detects all recurring transaction patterns from the provided list.
     *
     * @param transactions List of all parsed transactions to analyze
     * @return List of detected recurring transaction patterns
     */
    fun detectRecurringPatterns(transactions: List<ParsedTransaction>): List<RecurringTransaction> {
        if (transactions.size < MIN_RECURRING_COUNT) {
            return emptyList()
        }

        // Group transactions by normalized merchant name
        val merchantGroups = groupByMerchant(transactions)

        val recurringPatterns = mutableListOf<RecurringTransaction>()

        // Analyze each merchant group for recurring patterns
        merchantGroups.forEach { (merchantPattern, merchantTransactions) ->
            if (merchantTransactions.size >= MIN_RECURRING_COUNT) {
                // Sort by date for interval analysis
                val sorted = merchantTransactions.sortedBy { it.date }

                // Try to detect recurring patterns by amount similarity
                val amountGroups = groupByAmountSimilarity(sorted)

                amountGroups.forEach { amountGroup ->
                    if (amountGroup.size >= MIN_RECURRING_COUNT) {
                        // Analyze time intervals to detect frequency
                        val pattern = analyzeIntervals(merchantPattern, amountGroup)
                        if (pattern != null) {
                            recurringPatterns.add(pattern)
                        }
                    }
                }
            }
        }

        return recurringPatterns
    }

    /**
     * Groups transactions by normalized merchant name.
     * Uses case-insensitive matching and removes common suffixes/prefixes.
     *
     * @param transactions List of transactions to group
     * @return Map of normalized merchant pattern to list of matching transactions
     */
    private fun groupByMerchant(transactions: List<ParsedTransaction>): Map<String, List<ParsedTransaction>> {
        return transactions
            .filter { it.merchant != null }
            .groupBy { normalizeMerchantName(it.merchant!!) }
    }

    /**
     * Normalizes a merchant name for consistent matching.
     * - Converts to uppercase
     * - Trims whitespace
     * - Removes common suffixes (INC, LTD, PVT, etc.)
     * - Removes special characters
     *
     * @param merchant Raw merchant name from transaction
     * @return Normalized merchant pattern
     */
    private fun normalizeMerchantName(merchant: String): String {
        return merchant
            .uppercase()
            .trim()
            .replace(Regex("[^A-Z0-9\\s]"), "") // Remove special characters
            .replace(Regex("\\s+(INC|LTD|PVT|PRIVATE|LIMITED|COMPANY|CO|CORP)\\s*$"), "")
            .trim()
            .replace(Regex("\\s+"), " ") // Normalize multiple spaces
    }

    /**
     * Groups transactions by amount similarity.
     * Transactions with amounts within AMOUNT_TOLERANCE of each other are grouped together.
     *
     * @param transactions List of transactions to group (should be same merchant)
     * @return List of transaction groups with similar amounts
     */
    private fun groupByAmountSimilarity(transactions: List<ParsedTransaction>): List<List<ParsedTransaction>> {
        val groups = mutableListOf<MutableList<ParsedTransaction>>()

        for (transaction in transactions) {
            var addedToGroup = false

            // Try to add to existing group
            for (group in groups) {
                if (isAmountSimilar(transaction.amount, group.first().amount)) {
                    group.add(transaction)
                    addedToGroup = true
                    break
                }
            }

            // Create new group if not added
            if (!addedToGroup) {
                groups.add(mutableListOf(transaction))
            }
        }

        return groups
    }

    /**
     * Checks if two amounts are similar within the tolerance threshold.
     *
     * @param amount1 First amount to compare
     * @param amount2 Second amount to compare
     * @return True if amounts are within AMOUNT_TOLERANCE of each other
     */
    private fun isAmountSimilar(amount1: Double, amount2: Double): Boolean {
        val tolerance = amount1 * AMOUNT_TOLERANCE
        return abs(amount1 - amount2) <= tolerance
    }

    /**
     * Analyzes time intervals between transactions to detect recurring frequency.
     *
     * @param merchantPattern Normalized merchant name
     * @param transactions List of transactions with similar amounts (sorted by date)
     * @return RecurringTransaction if a pattern is detected, null otherwise
     */
    private fun analyzeIntervals(
        merchantPattern: String,
        transactions: List<ParsedTransaction>
    ): RecurringTransaction? {
        if (transactions.size < MIN_RECURRING_COUNT) {
            return null
        }

        // Calculate intervals between consecutive transactions in days
        val intervals = mutableListOf<Int>()
        for (i in 1 until transactions.size) {
            val daysBetween = ((transactions[i].date - transactions[i - 1].date) / MILLIS_IN_DAY).toInt()
            intervals.add(daysBetween)
        }

        // Calculate average interval
        val averageInterval = intervals.average().toInt()

        // Detect frequency based on average interval
        val frequency = detectFrequency(averageInterval, intervals)

        if (frequency == null) {
            // Intervals are too inconsistent to be considered recurring
            return null
        }

        // Calculate average amount
        val averageAmount = transactions.map { it.amount }.average()

        // Get the most recent transaction
        val lastTransaction = transactions.maxByOrNull { it.date }!!

        // Predict next expected date
        val nextExpected = calculateNextExpectedDate(lastTransaction.date, averageInterval)

        // Determine recurring type based on amount and merchant pattern
        val type = inferRecurringType(merchantPattern, averageAmount, transactions.first().type)

        return RecurringTransaction(
            id = UUID.randomUUID().toString(),
            merchantPattern = merchantPattern,
            amount = averageAmount,
            frequency = frequency,
            averageInterval = averageInterval,
            lastOccurrence = lastTransaction.date,
            nextExpected = nextExpected,
            transactionIds = transactions.map { it.smsId },
            isUserConfirmed = false, // New detection, not confirmed by user
            type = type
        )
    }

    /**
     * Detects the recurring frequency based on average interval and consistency.
     *
     * @param averageInterval Average days between transactions
     * @param intervals List of all intervals for consistency checking
     * @return RecurringFrequency if pattern is consistent, null otherwise
     */
    private fun detectFrequency(averageInterval: Int, intervals: List<Int>): RecurringFrequency? {
        // Check if intervals are consistent enough
        if (!areIntervalsConsistent(intervals)) {
            return null
        }

        // Match to known frequency patterns
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
     * Uses coefficient of variation to measure consistency.
     *
     * @param intervals List of intervals in days
     * @return True if intervals are consistent within tolerance
     */
    private fun areIntervalsConsistent(intervals: List<Int>): Boolean {
        if (intervals.isEmpty()) return false

        val average = intervals.average()

        // Check if each interval is within tolerance of average
        return intervals.all { interval ->
            val deviation = abs(interval - average)
            deviation <= (average * INTERVAL_TOLERANCE)
        }
    }

    /**
     * Checks if the average interval matches weekly pattern (7 days ± tolerance).
     */
    private fun isWeekly(averageInterval: Int): Boolean {
        val tolerance = (DAYS_IN_WEEK * INTERVAL_TOLERANCE).toInt()
        return abs(averageInterval - DAYS_IN_WEEK) <= tolerance
    }

    /**
     * Checks if the average interval matches bi-weekly pattern (14 days ± tolerance).
     */
    private fun isBiWeekly(averageInterval: Int): Boolean {
        val tolerance = (DAYS_IN_BI_WEEKLY * INTERVAL_TOLERANCE).toInt()
        return abs(averageInterval - DAYS_IN_BI_WEEKLY) <= tolerance
    }

    /**
     * Checks if the average interval matches monthly pattern (28-31 days).
     * More flexible tolerance to account for different month lengths.
     */
    private fun isMonthly(averageInterval: Int): Boolean {
        return averageInterval in DAYS_IN_MONTH_MIN..DAYS_IN_MONTH_MAX
    }

    /**
     * Checks if the average interval matches quarterly pattern (90 days ± tolerance).
     */
    private fun isQuarterly(averageInterval: Int): Boolean {
        val tolerance = (DAYS_IN_QUARTER * INTERVAL_TOLERANCE).toInt()
        return abs(averageInterval - DAYS_IN_QUARTER) <= tolerance
    }

    /**
     * Checks if the average interval matches annual pattern (365 days ± tolerance).
     */
    private fun isAnnual(averageInterval: Int): Boolean {
        val tolerance = (DAYS_IN_YEAR * INTERVAL_TOLERANCE).toInt()
        return abs(averageInterval - DAYS_IN_YEAR) <= tolerance
    }

    /**
     * Calculates the next expected transaction date based on the last occurrence and interval.
     *
     * @param lastOccurrence Timestamp of last transaction
     * @param averageInterval Average days between transactions
     * @return Predicted timestamp of next transaction
     */
    private fun calculateNextExpectedDate(lastOccurrence: Long, averageInterval: Int): Long {
        return lastOccurrence + (averageInterval * MILLIS_IN_DAY)
    }

    /**
     * Infers the type of recurring transaction based on patterns and heuristics.
     *
     * @param merchantPattern Normalized merchant name
     * @param amount Average transaction amount
     * @param transactionType Type of transaction (DEBIT/CREDIT)
     * @return Inferred recurring type
     */
    private fun inferRecurringType(
        merchantPattern: String,
        amount: Double,
        transactionType: TransactionType
    ): RecurringType {
        return when {
            // Salary detection: CREDIT transactions with large amounts
            transactionType == TransactionType.CREDIT && amount > 10000 -> RecurringType.SALARY

            // Rent detection: keywords in merchant pattern
            merchantPattern.contains(Regex("RENT|LANDLORD|HOUSING")) -> RecurringType.RENT

            // EMI detection: keywords in merchant pattern
            merchantPattern.contains(Regex("EMI|LOAN|FINANCE|BAJAJ|HDFC|ICICI")) &&
                transactionType == TransactionType.DEBIT -> RecurringType.EMI

            // Utility detection: keywords in merchant pattern
            merchantPattern.contains(Regex("ELECTRIC|ELECTRICITY|WATER|GAS|BILL|BSES|TATA POWER")) ->
                RecurringType.UTILITY

            // Subscription detection: common subscription services or small regular amounts
            merchantPattern.contains(Regex("NETFLIX|PRIME|SPOTIFY|YOUTUBE|SUBSCRIPTION|ADOBE|MICROSOFT")) ||
                (transactionType == TransactionType.DEBIT && amount < 1000) -> RecurringType.SUBSCRIPTION

            // Default to OTHER if no specific pattern matches
            else -> RecurringType.OTHER
        }
    }
}
