package com.example.kanakku.widget.data

import android.content.Context
import com.example.kanakku.data.database.DatabaseProvider
import com.example.kanakku.data.database.dao.TransactionDao
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.widget.model.BudgetProgressData
import com.example.kanakku.widget.model.RecentTransactionsData
import com.example.kanakku.widget.model.TodaySpendingData
import com.example.kanakku.widget.model.WidgetTransaction
import java.util.Calendar

/**
 * Repository for fetching widget-specific data from the database.
 *
 * This repository provides optimized data access methods for home screen widgets,
 * with synchronous suspend functions suitable for widget update contexts.
 * All methods perform direct database queries without reactive Flow streams
 * to minimize overhead in widget updates.
 *
 * Key responsibilities:
 * - Fetch today's spending total for small widget
 * - Calculate weekly budget progress for medium widget
 * - Retrieve recent transactions for large widget
 * - Provide snapshot data suitable for widget rendering
 *
 * @param context Application context for database access
 */
class WidgetDataRepository(context: Context) {

    private val transactionDao: TransactionDao =
        DatabaseProvider.getDatabase(context.applicationContext).transactionDao()

    /**
     * Gets today's total debit amount for the spending widget.
     * Calculates sum of all DEBIT transactions that occurred today.
     *
     * @return TodaySpendingData containing today's total, timestamp, and currency
     */
    suspend fun getTodaySpending(): TodaySpendingData {
        val (startOfDay, endOfDay) = getTodayDateRange()

        // Get all transactions for today
        val todayTransactions = transactionDao.getAllTransactionsSnapshot()
            .filter { it.date in startOfDay..endOfDay && it.type == TransactionType.DEBIT }

        // Sum up all debit amounts
        val todayTotal = todayTransactions.sumOf { it.amount }

        return TodaySpendingData(
            todayTotal = todayTotal,
            lastUpdated = System.currentTimeMillis(),
            currency = "₹"
        )
    }

    /**
     * Gets weekly budget progress data for the budget widget.
     * Calculates spending for the current week (Monday to Sunday) and compares against budget.
     *
     * @param budget The user's weekly budget amount
     * @return BudgetProgressData containing spent amount, budget, percentage, and period label
     */
    suspend fun getWeeklyBudgetProgress(budget: Double): BudgetProgressData {
        val (startOfWeek, endOfWeek) = getWeekDateRange()

        // Get all debit transactions for this week
        val weekTransactions = transactionDao.getAllTransactionsSnapshot()
            .filter { it.date in startOfWeek..endOfWeek && it.type == TransactionType.DEBIT }

        // Sum up all debit amounts for the week
        val spent = weekTransactions.sumOf { it.amount }

        // Calculate percentage (handle division by zero)
        val percentage = if (budget > 0) {
            (spent / budget * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        return BudgetProgressData(
            spent = spent,
            budget = budget,
            percentage = percentage,
            periodLabel = "This Week"
        )
    }

    /**
     * Gets recent transactions for the transactions widget.
     * Retrieves the latest N transactions sorted by date (newest first).
     *
     * @param limit Maximum number of transactions to retrieve (default: 5)
     * @return RecentTransactionsData containing list of recent transactions and timestamp
     */
    suspend fun getRecentTransactions(limit: Int = 5): RecentTransactionsData {
        // Get all transactions and take the most recent ones
        val recentTransactions = transactionDao.getAllTransactionsSnapshot()
            .take(limit)
            .map { entity ->
                WidgetTransaction(
                    id = entity.smsId,
                    merchant = entity.merchant ?: "Unknown",
                    amount = entity.amount,
                    type = entity.type,
                    date = entity.date
                )
            }

        return RecentTransactionsData(
            transactions = recentTransactions,
            lastUpdated = System.currentTimeMillis()
        )
    }

    // ==================== Helper Methods ====================

    /**
     * Calculates the start and end timestamps for today.
     * Start is at 00:00:00.000 and end is at 23:59:59.999.
     *
     * @return Pair of (startOfDay, endOfDay) timestamps in milliseconds
     */
    private fun getTodayDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Start of day (00:00:00.000)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        // End of day (23:59:59.999)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }

    /**
     * Calculates the start and end timestamps for the current week.
     * Week starts on Monday at 00:00:00.000 and ends on Sunday at 23:59:59.999.
     *
     * @return Pair of (startOfWeek, endOfWeek) timestamps in milliseconds
     */
    private fun getWeekDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Calculate start of week (Monday)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis

        // Calculate end of week (Sunday)
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfWeek = calendar.timeInMillis

        return Pair(startOfWeek, endOfWeek)
    }
}
