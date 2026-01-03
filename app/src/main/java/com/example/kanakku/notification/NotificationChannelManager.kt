package com.example.kanakku.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.kanakku.core.error.ErrorHandler

/**
 * Manages notification channels for the Kanakku app.
 *
 * This class handles creation and registration of notification channels
 * for different types of spending alerts. Channels are only required on
 * Android O (API 26) and above.
 *
 * Notification channels:
 * - Budget Alerts: High importance notifications for budget thresholds (80%, 100%)
 * - Large Transactions: High importance notifications for large transaction alerts
 * - Weekly Summary: Low importance notifications for weekly spending summaries
 *
 * Usage:
 * ```
 * // Initialize channels once during app startup
 * NotificationChannelManager.createNotificationChannels(context)
 * ```
 *
 * Thread-safe: All methods are safe to call from any thread.
 */
object NotificationChannelManager {

    private const val TAG = "NotificationChannelManager"

    /**
     * Notification channel IDs for different alert types.
     * These IDs are used when creating notifications to assign them to the correct channel.
     */
    object ChannelIds {
        /** Channel for budget threshold alerts (80%, 100%) */
        const val BUDGET_ALERTS = "budget_alerts"

        /** Channel for large transaction alerts */
        const val LARGE_TRANSACTIONS = "large_transactions"

        /** Channel for weekly spending summaries */
        const val WEEKLY_SUMMARY = "weekly_summary"
    }

    /**
     * Creates and registers all notification channels with the system.
     *
     * This method should be called once during app initialization (e.g., in Application.onCreate()
     * or MainActivity.onCreate()). It's safe to call multiple times - the system will update
     * existing channels if needed.
     *
     * Note: Notification channels are only supported on Android O (API 26) and above.
     * On lower API levels, this method does nothing.
     *
     * @param context Application context for accessing NotificationManager
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

                if (notificationManager == null) {
                    ErrorHandler.logWarning(
                        "NotificationManager not available - channels not created",
                        TAG
                    )
                    return
                }

                // Create all channels
                val channels = listOf(
                    createBudgetAlertsChannel(),
                    createLargeTransactionsChannel(),
                    createWeeklySummaryChannel()
                )

                // Register all channels at once
                notificationManager.createNotificationChannels(channels)

                ErrorHandler.logInfo(
                    "Successfully created ${channels.size} notification channels",
                    TAG
                )
            } catch (e: Exception) {
                ErrorHandler.handleError(e, "Creating notification channels")
            }
        } else {
            ErrorHandler.logDebug(
                "Notification channels not required for API ${Build.VERSION.SDK_INT}",
                TAG
            )
        }
    }

    /**
     * Creates the Budget Alerts notification channel.
     *
     * High importance channel for critical budget threshold notifications (80%, 100%).
     * These notifications should make sound and appear as heads-up notifications.
     *
     * @return Configured NotificationChannel for budget alerts
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createBudgetAlertsChannel(): NotificationChannel {
        return NotificationChannel(
            ChannelIds.BUDGET_ALERTS,
            "Budget Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications when you reach 80% or 100% of your budget"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }
    }

    /**
     * Creates the Large Transactions notification channel.
     *
     * High importance channel for large transaction alerts.
     * These notifications help users stay aware of significant spending and potential fraud.
     *
     * @return Configured NotificationChannel for large transactions
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createLargeTransactionsChannel(): NotificationChannel {
        return NotificationChannel(
            ChannelIds.LARGE_TRANSACTIONS,
            "Large Transactions",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for unusually large transactions"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }
    }

    /**
     * Creates the Weekly Summary notification channel.
     *
     * Low importance channel for weekly spending summaries.
     * These are informational notifications that don't require immediate attention.
     *
     * @return Configured NotificationChannel for weekly summaries
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createWeeklySummaryChannel(): NotificationChannel {
        return NotificationChannel(
            ChannelIds.WEEKLY_SUMMARY,
            "Weekly Summary",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Weekly spending summary notifications"
            enableLights(false)
            enableVibration(false)
            setShowBadge(true)
        }
    }

    /**
     * Checks if a specific notification channel is enabled.
     *
     * Note: Users can disable channels in system settings. This method allows
     * checking if a channel is currently enabled.
     *
     * @param context Application context
     * @param channelId The channel ID to check (use ChannelIds constants)
     * @return true if the channel is enabled, false otherwise or if API < 26
     */
    fun isChannelEnabled(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val channel = notificationManager?.getNotificationChannel(channelId)

                return channel?.importance != NotificationManager.IMPORTANCE_NONE
            } catch (e: Exception) {
                ErrorHandler.handleError(e, "Checking notification channel status")
                return false
            }
        }
        return true // On pre-O devices, there are no channels to disable
    }

    /**
     * Deletes a notification channel from the system.
     *
     * WARNING: Once a channel is deleted, it cannot be recreated with the same ID
     * unless the user reinstalls the app. Use with extreme caution.
     *
     * This method is primarily for testing purposes or major app restructuring.
     *
     * @param context Application context
     * @param channelId The channel ID to delete
     */
    fun deleteChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.deleteNotificationChannel(channelId)

                ErrorHandler.logInfo("Deleted notification channel: $channelId", TAG)
            } catch (e: Exception) {
                ErrorHandler.handleError(e, "Deleting notification channel")
            }
        }
    }

    /**
     * Gets the importance level of a notification channel.
     *
     * This can be useful for adapting notification behavior based on user preferences.
     *
     * @param context Application context
     * @param channelId The channel ID to query
     * @return The importance level, or IMPORTANCE_NONE if channel doesn't exist or API < 26
     */
    fun getChannelImportance(context: Context, channelId: String): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val channel = notificationManager?.getNotificationChannel(channelId)

                return channel?.importance ?: NotificationManager.IMPORTANCE_NONE
            } catch (e: Exception) {
                ErrorHandler.handleError(e, "Getting notification channel importance")
                return NotificationManager.IMPORTANCE_NONE
            }
        }
        return NotificationManager.IMPORTANCE_DEFAULT
    }
}
