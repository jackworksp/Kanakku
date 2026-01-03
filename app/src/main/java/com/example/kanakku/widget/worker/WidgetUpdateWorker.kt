package com.example.kanakku.widget.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kanakku.widget.BudgetProgressWidget
import com.example.kanakku.widget.RecentTransactionsWidget
import com.example.kanakku.widget.TodaySpendingWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager Worker for periodic widget data updates.
 *
 * This worker runs periodically in the background to refresh all home screen widgets
 * with the latest financial data from the database. It ensures widgets stay up-to-date
 * without requiring user interaction.
 *
 * The worker performs the following operations:
 * 1. Fetches latest data from the database via widget repositories
 * 2. Updates all three widget types:
 *    - TodaySpendingWidget (small, 2x1)
 *    - BudgetProgressWidget (medium, 3x2)
 *    - RecentTransactionsWidget (large, 4x3)
 * 3. Handles errors gracefully with automatic retry mechanism
 *
 * Update frequency: Configured by WidgetUpdateScheduler (typically 1 hour)
 * Constraints: Requires battery not low (configured in scheduler)
 *
 * Error handling:
 * - Transient errors (network, database): Returns Result.retry()
 * - Permanent errors: Returns Result.failure()
 * - Success: Returns Result.success()
 *
 * @param context Application context
 * @param params Worker parameters from WorkManager
 *
 * @see com.example.kanakku.widget.worker.WidgetUpdateScheduler
 * @see com.example.kanakku.widget.TodaySpendingWidget
 * @see com.example.kanakku.widget.BudgetProgressWidget
 * @see com.example.kanakku.widget.RecentTransactionsWidget
 */
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    /**
     * Executes the widget update work on a background thread.
     *
     * This method is called by WorkManager when it's time to update the widgets.
     * It uses GlanceAppWidgetManager to refresh all widget instances with the
     * latest data from the database.
     *
     * The update process is performed on the IO dispatcher to avoid blocking
     * the main thread or the WorkManager thread pool.
     *
     * @return Result indicating success, failure, or need to retry
     *         - Result.success(): All widgets updated successfully
     *         - Result.retry(): Transient error occurred, retry later
     *         - Result.failure(): Permanent error, don't retry
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Update all widget types using GlanceAppWidgetManager
            // Each widget will fetch its own data during the update process

            // Update Today's Spending widgets
            TodaySpendingWidget().updateAll(applicationContext)

            // Update Budget Progress widgets
            BudgetProgressWidget().updateAll(applicationContext)

            // Update Recent Transactions widgets
            RecentTransactionsWidget().updateAll(applicationContext)

            // All widgets updated successfully
            Result.success()
        } catch (e: Exception) {
            // Log the error for debugging purposes
            e.printStackTrace()

            // Determine if error is transient (should retry) or permanent
            when {
                // Transient errors - should retry
                isTransientError(e) -> {
                    Result.retry()
                }
                // Permanent errors - don't retry
                else -> {
                    Result.failure()
                }
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Determines if an exception represents a transient error that should be retried.
     *
     * Transient errors include:
     * - Database locks or temporary unavailability
     * - Memory pressure
     * - Thread interruption
     *
     * Permanent errors include:
     * - Invalid data or corruption
     * - Programming errors (NPE, illegal state, etc.)
     *
     * @param exception The exception to check
     * @return true if the error is transient and should be retried, false otherwise
     */
    private fun isTransientError(exception: Exception): Boolean {
        return when (exception) {
            // Database-related transient errors
            is android.database.sqlite.SQLiteException -> true
            // Memory or resource pressure
            is OutOfMemoryError -> true
            // Thread interruption (can happen during app updates)
            is InterruptedException -> true
            // Default: treat as permanent error
            else -> false
        }
    }
}
