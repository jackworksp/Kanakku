package com.example.kanakku.widget.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Helper class for scheduling and managing periodic widget updates using WorkManager.
 *
 * This scheduler manages the periodic background job that refreshes all home screen widgets
 * with the latest financial data. It uses WorkManager to ensure reliable execution even
 * across app restarts and device reboots.
 *
 * Key features:
 * - Schedules periodic updates every 1 hour
 * - Uses unique work name for replacement policy (avoiding duplicates)
 * - Battery-aware: only runs when battery is not critically low
 * - Survives app restarts and device reboots
 *
 * Update frequency: 1 hour (60 minutes)
 * Worker: WidgetUpdateWorker
 * Constraints: Battery not low
 *
 * Usage:
 * ```
 * // Schedule widget updates
 * WidgetUpdateScheduler.schedulePeriodicUpdates(context)
 *
 * // Cancel scheduled updates
 * WidgetUpdateScheduler.cancelUpdates(context)
 * ```
 *
 * @see WidgetUpdateWorker
 * @see com.example.kanakku.widget.TodaySpendingWidget
 * @see com.example.kanakku.widget.BudgetProgressWidget
 * @see com.example.kanakku.widget.RecentTransactionsWidget
 */
object WidgetUpdateScheduler {

    /**
     * Unique work name used for scheduling periodic widget updates.
     * Using a unique name allows WorkManager to replace existing work instead of creating duplicates.
     */
    private const val WORK_NAME = "widget_periodic_update"

    /**
     * Update interval in hours.
     * Widgets are refreshed every hour to keep data reasonably up-to-date without
     * excessive battery drain.
     */
    private const val UPDATE_INTERVAL_HOURS = 1L

    /**
     * Schedules periodic widget updates using WorkManager.
     *
     * This method enqueues a periodic work request that runs every hour to update all
     * widgets with the latest financial data. If work with the same name is already
     * scheduled, it will be replaced with this new request (ExistingPeriodicWorkPolicy.REPLACE).
     *
     * The work is executed with the following constraints:
     * - Battery not low: Ensures updates don't drain battery when critically low
     *
     * The worker will:
     * 1. Fetch latest data from the database
     * 2. Update all widget types (Today's Spending, Budget Progress, Recent Transactions)
     * 3. Handle errors with automatic retry for transient failures
     *
     * Note: The first update may have an initial delay of up to 1 hour. For immediate
     * updates, widgets should be refreshed directly when transactions are added.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     */
    fun schedulePeriodicUpdates(context: Context) {
        // Build constraints for the work request
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true) // Don't run when battery is critically low
            .build()

        // Create the periodic work request
        val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            UPDATE_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        // Enqueue the work, replacing any existing work with the same name
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE, // Replace existing work to avoid duplicates
                workRequest
            )
    }

    /**
     * Cancels all scheduled widget update work.
     *
     * This method cancels the periodic widget update work. It's useful for:
     * - User preference to disable automatic updates
     * - Debugging and testing
     * - Cleanup when widgets are removed
     *
     * Note: This doesn't prevent widgets from updating when the user opens the app
     * or adds transactions. It only stops the automatic periodic background updates.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     */
    fun cancelUpdates(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(WORK_NAME)
    }

    /**
     * Checks if periodic widget updates are currently scheduled.
     *
     * This is useful for debugging or showing the update status in a settings screen.
     *
     * Note: This method returns immediately with the last known state. For the most
     * up-to-date status, observe the WorkInfo LiveData from WorkManager.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     * @return true if work is scheduled (enqueued or running), false otherwise
     */
    fun isUpdateScheduled(context: Context): Boolean {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val workInfos = workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
        return workInfos.any { !it.state.isFinished }
    }
}
