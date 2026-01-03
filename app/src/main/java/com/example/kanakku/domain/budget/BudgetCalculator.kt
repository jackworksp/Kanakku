package com.example.kanakku.domain.budget

import com.example.kanakku.data.model.*
import java.util.*

/**
 * Utility class for calculating budget progress, status, and spending totals.
 *
 * This calculator provides business logic for budget tracking including:
 * - Budget progress calculation (spent, remaining, percentage)
 * - Budget status determination (green/yellow/red)
 * - Category spending aggregation
 * - Date range utilities for monthly budgets
 */
class BudgetCalculator {

    companion object {
        /**
         * Threshold for APPROACHING status (yellow warning)
         * Spending between 80-100% of budget
         */
        const val THRESHOLD_WARNING = 80.0

        /**
         * Threshold for EXCEEDED status (red alert)
         * Spending at or above 100% of budget
         */
        const val THRESHOLD_EXCEEDED = 100.0
    }

    /**
     * Calculate comprehensive budget progress including spent, remaining, percentage, and status.
     *
     * @param budget The budget to calculate progress for
     * @param spent The amount already spent against this budget
     * @return BudgetProgress with all calculated metrics
     */
    fun calculateBudgetProgress(budget: Budget, spent: Double): BudgetProgress {
        val limit = budget.amount
        val remaining = limit - spent
        val percentage = if (limit > 0) (spent / limit) * 100.0 else 0.0
        val status = getBudgetStatus(percentage)

        return BudgetProgress(
            spent = spent,
            limit = limit,
            remaining = remaining,
            percentage = percentage,
            status = status
        )
    }

    /**
     * Determine budget status based on percentage spent.
     *
     * Color coding:
     * - UNDER_BUDGET (green): < 80% spent
     * - APPROACHING (yellow/amber): 80-100% spent
     * - EXCEEDED (red): >= 100% spent
     *
     * @param percentage The percentage of budget spent (0-100+)
     * @return BudgetStatus enum value
     */
    fun getBudgetStatus(percentage: Double): BudgetStatus {
        return when {
            percentage >= THRESHOLD_EXCEEDED -> BudgetStatus.EXCEEDED
            percentage >= THRESHOLD_WARNING -> BudgetStatus.APPROACHING
            else -> BudgetStatus.UNDER_BUDGET
        }
    }

    /**
     * Calculate total spending by category from a list of transactions.
     *
     * This aggregates debit transactions by their assigned category.
     * Only DEBIT transactions are counted (not credits/income).
     *
     * @param transactions List of parsed transactions
     * @param categoryMap Map of SMS ID to Category for categorization
     * @return Map of Category ID to total amount spent in that category
     */
    fun getSpentByCategory(
        transactions: List<ParsedTransaction>,
        categoryMap: Map<Long, Category>
    ): Map<String, Double> {
        return transactions
            .filter { it.type == TransactionType.DEBIT }
            .groupBy { transaction ->
                val category = categoryMap[transaction.smsId] ?: DefaultCategories.OTHER
                category.id
            }
            .mapValues { (_, txns) ->
                txns.sumOf { it.amount }
            }
    }

    /**
     * Get the date range for the current month.
     *
     * Returns a Pair of (startOfMonth, endOfMonth) timestamps in milliseconds.
     * This is useful for filtering transactions for monthly budget tracking.
     *
     * @return Pair of (startTimestamp, endTimestamp) for current month
     */
    fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Start of month (first day at 00:00:00)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        // End of month (last day at 23:59:59)
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis

        return Pair(startOfMonth, endOfMonth)
    }

    /**
     * Get the date range for a specific month and year.
     *
     * Returns a Pair of (startOfMonth, endOfMonth) timestamps in milliseconds.
     *
     * @param month Month (1-12, where 1 = January)
     * @param year Year (e.g., 2026)
     * @return Pair of (startTimestamp, endTimestamp) for specified month
     */
    fun getMonthRange(month: Int, year: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Calendar.MONTH is 0-indexed (0 = January), so subtract 1
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        // End of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis

        return Pair(startOfMonth, endOfMonth)
    }

    /**
     * Filter transactions to only those within a specific month and year.
     *
     * @param transactions List of transactions to filter
     * @param month Month (1-12, where 1 = January)
     * @param year Year (e.g., 2026)
     * @return Filtered list of transactions in the specified month
     */
    fun filterTransactionsByMonth(
        transactions: List<ParsedTransaction>,
        month: Int,
        year: Int
    ): List<ParsedTransaction> {
        val (startOfMonth, endOfMonth) = getMonthRange(month, year)
        return transactions.filter { it.date in startOfMonth..endOfMonth }
    }

    /**
     * Calculate total spent amount from a list of transactions.
     *
     * Only counts DEBIT transactions (expenses), not credits (income).
     *
     * @param transactions List of transactions to sum
     * @return Total debit amount
     */
    fun getTotalSpent(transactions: List<ParsedTransaction>): Double {
        return transactions
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }
    }
}
