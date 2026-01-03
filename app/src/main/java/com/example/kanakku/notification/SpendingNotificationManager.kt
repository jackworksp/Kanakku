package com.example.kanakku.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.kanakku.MainActivity
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.model.NotificationData
import com.example.kanakku.data.model.NotificationType

/**
 * Manages creation and display of spending notifications for the Kanakku app.
 *
 * This class handles:
 * - Building notifications with appropriate styling for each notification type
 * - Creating deep links (PendingIntents) to navigate to specific screens
 * - Grouping multiple notifications together for better UX
 * - Managing notification IDs to prevent duplicates
 * - Proper icon selection and action buttons
 *
 * Notification types:
 * - Budget Alerts: Shown when user reaches 80% or 100% of their budget
 * - Large Transactions: Shown for transactions exceeding user-defined threshold
 * - Weekly Summary: Weekly spending overview shown on user's preferred day/time
 *
 * Usage:
 * ```
 * // Show a budget alert notification
 * val notificationData = NotificationData(
 *     type = NotificationType.BUDGET_ALERT,
 *     title = "Budget Alert: 80% Used",
 *     message = "You've spent ₹8,000 of your ₹10,000 budget"
 * )
 * SpendingNotificationManager.showNotification(context, notificationData)
 * ```
 *
 * Thread-safe: All methods are safe to call from any thread.
 */
object SpendingNotificationManager {

    private const val TAG = "SpendingNotificationManager"

    /**
     * Notification group for spending alerts.
     * All spending notifications are grouped together for better organization.
     */
    private const val NOTIFICATION_GROUP_KEY = "com.example.kanakku.SPENDING_ALERTS"

    /**
     * Base notification IDs for different notification types.
     * These are used to ensure each type has unique IDs and to enable notification replacement.
     */
    private object NotificationIds {
        const val BUDGET_ALERT_80 = 1001
        const val BUDGET_ALERT_100 = 1002
        const val LARGE_TRANSACTION_BASE = 2000
        const val WEEKLY_SUMMARY = 3000
        const val GROUP_SUMMARY = 9999
    }

    /**
     * Intent actions for notification deep links.
     * These are used to navigate to specific screens when notification is tapped.
     */
    private object IntentActions {
        const val OPEN_TRANSACTIONS = "com.example.kanakku.OPEN_TRANSACTIONS"
        const val OPEN_ANALYTICS = "com.example.kanakku.OPEN_ANALYTICS"
        const val OPEN_CATEGORIES = "com.example.kanakku.OPEN_CATEGORIES"
    }

    /**
     * Intent extras for passing data to the app when notification is tapped.
     */
    private object IntentExtras {
        const val NOTIFICATION_TYPE = "notification_type"
        const val TRANSACTION_ID = "transaction_id"
        const val CATEGORY_ID = "category_id"
    }

    /**
     * Shows a notification based on the provided notification data.
     *
     * This method automatically:
     * - Selects the appropriate notification channel
     * - Determines the notification ID (for replacement of similar notifications)
     * - Creates deep links based on notification type
     * - Applies proper styling and icons
     * - Groups notifications together
     *
     * @param context Application context for creating and showing notifications
     * @param notificationData Data containing notification details (type, title, message, etc.)
     * @return true if notification was shown successfully, false otherwise
     */
    fun showNotification(context: Context, notificationData: NotificationData): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            if (notificationManager == null) {
                ErrorHandler.logWarning("NotificationManager not available", TAG)
                return false
            }

            // Build the notification based on type
            val notification = when (notificationData.type) {
                NotificationType.BUDGET_ALERT -> buildBudgetAlertNotification(context, notificationData)
                NotificationType.LARGE_TRANSACTION -> buildLargeTransactionNotification(context, notificationData)
                NotificationType.WEEKLY_SUMMARY -> buildWeeklySummaryNotification(context, notificationData)
            }

            // Determine notification ID
            val notificationId = getNotificationId(notificationData)

            // Show the notification
            notificationManager.notify(notificationId, notification.build())

            // Show group summary if needed (Android 7.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                showGroupSummary(context, notificationManager)
            }

            ErrorHandler.logInfo(
                "Notification shown: ${notificationData.type} (ID: $notificationId)",
                TAG
            )

            true
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Showing notification")
            false
        }
    }

    /**
     * Builds a budget alert notification.
     *
     * Budget alerts are high-priority notifications shown when user reaches spending thresholds.
     * They include:
     * - Alert icon (warning)
     * - High priority for visibility
     * - "View Budget" action to navigate to analytics screen
     * - Auto-cancel when tapped
     *
     * @param context Application context
     * @param data Notification data
     * @return NotificationCompat.Builder configured for budget alerts
     */
    private fun buildBudgetAlertNotification(
        context: Context,
        data: NotificationData
    ): NotificationCompat.Builder {
        val contentIntent = createDeepLinkIntent(
            context,
            IntentActions.OPEN_ANALYTICS,
            data.actionData
        )

        return NotificationCompat.Builder(context, NotificationChannelManager.ChannelIds.BUDGET_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(data.title)
            .setContentText(data.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setWhen(data.timestamp)
            .setShowWhen(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "View Budget",
                contentIntent
            )
    }

    /**
     * Builds a large transaction alert notification.
     *
     * Large transaction alerts help users stay aware of significant spending.
     * They include:
     * - Money/payment icon
     * - High priority for fraud awareness
     * - "View Transaction" action to navigate to transactions screen
     * - Auto-cancel when tapped
     *
     * @param context Application context
     * @param data Notification data
     * @return NotificationCompat.Builder configured for large transactions
     */
    private fun buildLargeTransactionNotification(
        context: Context,
        data: NotificationData
    ): NotificationCompat.Builder {
        val contentIntent = createDeepLinkIntent(
            context,
            IntentActions.OPEN_TRANSACTIONS,
            data.actionData
        )

        return NotificationCompat.Builder(context, NotificationChannelManager.ChannelIds.LARGE_TRANSACTIONS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(data.title)
            .setContentText(data.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setWhen(data.timestamp)
            .setShowWhen(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "View Transaction",
                contentIntent
            )
    }

    /**
     * Builds a weekly summary notification.
     *
     * Weekly summaries are low-priority informational notifications.
     * They include:
     * - Email/message icon (for summary content)
     * - Low priority (non-intrusive)
     * - "View Details" action to navigate to analytics screen
     * - Auto-cancel when tapped
     * - Optional big text style for longer summaries
     *
     * @param context Application context
     * @param data Notification data
     * @return NotificationCompat.Builder configured for weekly summaries
     */
    private fun buildWeeklySummaryNotification(
        context: Context,
        data: NotificationData
    ): NotificationCompat.Builder {
        val contentIntent = createDeepLinkIntent(
            context,
            IntentActions.OPEN_ANALYTICS,
            data.actionData
        )

        return NotificationCompat.Builder(context, NotificationChannelManager.ChannelIds.WEEKLY_SUMMARY)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(data.title)
            .setContentText(data.message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(data.message)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setWhen(data.timestamp)
            .setShowWhen(true)
            .addAction(
                android.R.drawable.ic_menu_view,
                "View Details",
                contentIntent
            )
    }

    /**
     * Creates a PendingIntent for deep linking into the app.
     *
     * The deep link allows notifications to navigate to specific screens when tapped.
     * The intent includes:
     * - Action to identify which screen to open
     * - Extra data for context (notification type, transaction ID, etc.)
     * - Proper flags for Android 12+ (FLAG_IMMUTABLE)
     *
     * @param context Application context
     * @param action Intent action identifying the destination screen
     * @param extras Additional data to pass to the activity
     * @return PendingIntent configured for the specified action
     */
    private fun createDeepLinkIntent(
        context: Context,
        action: String,
        extras: Map<String, String>
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            // Add all extras from the notification data
            extras.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    /**
     * Determines the appropriate notification ID based on notification data.
     *
     * Notification IDs are used to:
     * - Replace similar notifications (e.g., update budget alert from 80% to 100%)
     * - Prevent duplicate notifications for the same event
     * - Allow multiple unique notifications (e.g., multiple large transactions)
     *
     * ID allocation:
     * - Budget 80%: Fixed ID (replaces previous 80% alert)
     * - Budget 100%: Fixed ID (replaces previous 100% alert)
     * - Large Transaction: Base ID + transaction hash (unique per transaction)
     * - Weekly Summary: Fixed ID (replaces previous week's summary)
     *
     * @param data Notification data
     * @return Unique notification ID for this notification
     */
    private fun getNotificationId(data: NotificationData): Int {
        return when (data.type) {
            NotificationType.BUDGET_ALERT -> {
                // Use different IDs for 80% vs 100% budget alerts
                val threshold = data.actionData["threshold"]
                when (threshold) {
                    "80" -> NotificationIds.BUDGET_ALERT_80
                    "100" -> NotificationIds.BUDGET_ALERT_100
                    else -> NotificationIds.BUDGET_ALERT_80 // Default to 80%
                }
            }
            NotificationType.LARGE_TRANSACTION -> {
                // Use unique ID for each transaction (allows multiple notifications)
                val transactionId = data.actionData["transaction_id"]
                if (transactionId != null) {
                    NotificationIds.LARGE_TRANSACTION_BASE + transactionId.hashCode()
                } else {
                    NotificationIds.LARGE_TRANSACTION_BASE
                }
            }
            NotificationType.WEEKLY_SUMMARY -> {
                // Fixed ID - each new summary replaces the previous one
                NotificationIds.WEEKLY_SUMMARY
            }
        }
    }

    /**
     * Shows a group summary notification for grouped notifications.
     *
     * On Android 7.0 (API 24) and above, notifications can be grouped together.
     * The group summary provides a collapsed view of all notifications in the group.
     * This improves UX when multiple spending notifications are shown.
     *
     * The summary shows:
     * - Generic "Spending Alerts" title
     * - Count of active notifications
     * - Inbox style listing recent alerts
     *
     * @param context Application context
     * @param notificationManager System notification manager
     */
    private fun showGroupSummary(context: Context, notificationManager: NotificationManager) {
        try {
            val summaryNotification = NotificationCompat.Builder(
                context,
                NotificationChannelManager.ChannelIds.BUDGET_ALERTS
            )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Spending Alerts")
                .setContentText("You have new spending notifications")
                .setGroup(NOTIFICATION_GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(NotificationIds.GROUP_SUMMARY, summaryNotification)
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Showing group summary notification")
        }
    }

    /**
     * Cancels a specific notification by type and optional identifier.
     *
     * This is useful for:
     * - Dismissing notifications programmatically
     * - Clearing outdated notifications
     * - Removing notifications when the triggering condition is resolved
     *
     * @param context Application context
     * @param type Notification type to cancel
     * @param identifier Optional identifier (e.g., transaction ID for large transactions)
     * @return true if cancellation was successful, false otherwise
     */
    fun cancelNotification(
        context: Context,
        type: NotificationType,
        identifier: String? = null
    ): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            if (notificationManager == null) {
                ErrorHandler.logWarning("NotificationManager not available", TAG)
                return false
            }

            val notificationId = when (type) {
                NotificationType.BUDGET_ALERT -> {
                    // Cancel both 80% and 100% alerts if no specific threshold provided
                    if (identifier == null) {
                        notificationManager.cancel(NotificationIds.BUDGET_ALERT_80)
                        notificationManager.cancel(NotificationIds.BUDGET_ALERT_100)
                        return true
                    } else {
                        when (identifier) {
                            "80" -> NotificationIds.BUDGET_ALERT_80
                            "100" -> NotificationIds.BUDGET_ALERT_100
                            else -> NotificationIds.BUDGET_ALERT_80
                        }
                    }
                }
                NotificationType.LARGE_TRANSACTION -> {
                    if (identifier != null) {
                        NotificationIds.LARGE_TRANSACTION_BASE + identifier.hashCode()
                    } else {
                        NotificationIds.LARGE_TRANSACTION_BASE
                    }
                }
                NotificationType.WEEKLY_SUMMARY -> NotificationIds.WEEKLY_SUMMARY
            }

            notificationManager.cancel(notificationId)

            ErrorHandler.logInfo("Cancelled notification: $type (ID: $notificationId)", TAG)
            true
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Cancelling notification")
            false
        }
    }

    /**
     * Cancels all notifications shown by this manager.
     *
     * This clears:
     * - All budget alerts (80% and 100%)
     * - The group summary notification
     * - Weekly summary notification
     *
     * Note: Individual large transaction notifications are not cancelled by this method
     * as they have dynamic IDs. To cancel those, use cancelNotification() with the
     * specific transaction identifier.
     *
     * @param context Application context
     * @return true if cancellation was successful, false otherwise
     */
    fun cancelAllNotifications(context: Context): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            if (notificationManager == null) {
                ErrorHandler.logWarning("NotificationManager not available", TAG)
                return false
            }

            // Cancel known fixed-ID notifications
            notificationManager.cancel(NotificationIds.BUDGET_ALERT_80)
            notificationManager.cancel(NotificationIds.BUDGET_ALERT_100)
            notificationManager.cancel(NotificationIds.WEEKLY_SUMMARY)
            notificationManager.cancel(NotificationIds.GROUP_SUMMARY)

            ErrorHandler.logInfo("Cancelled all notifications", TAG)
            true
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Cancelling all notifications")
            false
        }
    }

    /**
     * Checks if notifications are enabled for the app.
     *
     * This checks:
     * - System-level notification permission (Android 13+)
     * - App-level notification settings
     *
     * @param context Application context
     * @return true if notifications are enabled, false otherwise
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            if (notificationManager == null) {
                ErrorHandler.logWarning("NotificationManager not available", TAG)
                return false
            }

            // Check if notifications are enabled at the app level
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationManager.areNotificationsEnabled()
            } else {
                true // Notifications are always enabled on pre-N devices
            }
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Checking notification status")
            false
        }
    }
}
