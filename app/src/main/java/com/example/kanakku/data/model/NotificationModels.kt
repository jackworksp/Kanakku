package com.example.kanakku.data.model

/**
 * Represents different types of notifications supported by the app.
 */
enum class NotificationType {
    BUDGET_ALERT,
    LARGE_TRANSACTION,
    WEEKLY_SUMMARY
}

/**
 * Represents budget alert threshold levels.
 */
enum class BudgetThreshold(val percentage: Int) {
    WARNING(80),
    LIMIT(100)
}

/**
 * Represents days of the week for scheduling weekly summaries.
 */
enum class DayOfWeek(val displayName: String, val calendarDay: Int) {
    SUNDAY("Sunday", 1),
    MONDAY("Monday", 2),
    TUESDAY("Tuesday", 3),
    WEDNESDAY("Wednesday", 4),
    THURSDAY("Thursday", 5),
    FRIDAY("Friday", 6),
    SATURDAY("Saturday", 7)
}

/**
 * Settings for budget alert notifications.
 *
 * @property enabled Whether budget alerts are enabled
 * @property notifyAt80Percent Enable notification when 80% of budget is reached
 * @property notifyAt100Percent Enable notification when 100% of budget is reached
 */
data class BudgetAlertSettings(
    val enabled: Boolean = true,
    val notifyAt80Percent: Boolean = true,
    val notifyAt100Percent: Boolean = true
)

/**
 * Settings for large transaction alert notifications.
 *
 * @property enabled Whether large transaction alerts are enabled
 * @property threshold Amount threshold for triggering alert (default: 5000.0)
 */
data class LargeTransactionSettings(
    val enabled: Boolean = true,
    val threshold: Double = 5000.0
)

/**
 * Settings for weekly spending summary notifications.
 *
 * @property enabled Whether weekly summary is enabled
 * @property dayOfWeek Day of week to send summary (default: MONDAY)
 * @property hourOfDay Hour of day to send summary in 24-hour format (default: 9 for 9 AM)
 */
data class WeeklySummarySettings(
    val enabled: Boolean = false,
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val hourOfDay: Int = 9
)

/**
 * Comprehensive notification settings for all notification types.
 *
 * This is the main data class that encapsulates all notification preferences.
 * It provides default values for all settings to ensure a good out-of-box experience.
 *
 * @property budgetAlerts Settings for budget threshold notifications
 * @property largeTransactions Settings for large transaction notifications
 * @property weeklySummary Settings for weekly spending summary notifications
 */
data class NotificationSettings(
    val budgetAlerts: BudgetAlertSettings = BudgetAlertSettings(),
    val largeTransactions: LargeTransactionSettings = LargeTransactionSettings(),
    val weeklySummary: WeeklySummarySettings = WeeklySummarySettings()
) {
    /**
     * Checks if any notification type is enabled.
     *
     * @return true if at least one notification type is enabled
     */
    fun hasAnyEnabled(): Boolean {
        return budgetAlerts.enabled || largeTransactions.enabled || weeklySummary.enabled
    }

    /**
     * Gets a list of enabled notification types.
     *
     * @return List of enabled NotificationType values
     */
    fun getEnabledTypes(): List<NotificationType> {
        return buildList {
            if (budgetAlerts.enabled) add(NotificationType.BUDGET_ALERT)
            if (largeTransactions.enabled) add(NotificationType.LARGE_TRANSACTION)
            if (weeklySummary.enabled) add(NotificationType.WEEKLY_SUMMARY)
        }
    }
}

/**
 * Represents the data for a notification that will be displayed.
 *
 * @property type The type of notification
 * @property title The notification title
 * @property message The notification message/body
 * @property timestamp When the notification was triggered
 * @property actionData Optional data for notification actions (e.g., deep link data)
 */
data class NotificationData(
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionData: Map<String, String> = emptyMap()
)
