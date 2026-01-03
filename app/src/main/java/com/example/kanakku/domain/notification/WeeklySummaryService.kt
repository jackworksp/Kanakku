package com.example.kanakku.domain.notification

import android.content.Context
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories
import com.example.kanakku.data.model.NotificationData
import com.example.kanakku.data.model.NotificationType
import com.example.kanakku.data.model.TimePeriod
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.data.repository.TransactionRepository
import com.example.kanakku.domain.analytics.AnalyticsCalculator
import com.example.kanakku.notification.SpendingNotificationManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service for generating and sending weekly spending summary notifications.
 *
 * This service:
 * - Generates weekly spending summary data using AnalyticsCalculator
 * - Calculates total spent, total received, transaction count, and average daily spending
 * - Identifies the top spending category for the week
 * - Formats a comprehensive summary message with key insights
 * - Sends weekly summary notifications on user-configured schedule
 * - Tracks when summaries have been sent to prevent duplicate notifications
 * - Respects user notification preferences (enabled/disabled, day/time)
 *
 * Weekly Summary Logic:
 * - Summary covers the last 7 days (rolling weekly period)
 * - Includes total spending, income, transaction count, daily average
 * - Highlights top spending category
 * - Formatted as a non-intrusive, informational notification
 * - Only sent once per week based on user's configured day/time
 *
 * Alert Tracking:
 * - Summary state stored in AppPreferences to persist across app restarts
 * - Key format: "weekly_summary_sent_{weekKey}"
 * - Week key identifies the specific week (e.g., "2026-W02" for week 2)
 * - Prevents duplicate summaries for the same week
 * - Automatically resets when a new week begins
 *
 * Thread-safe: All methods use suspend functions and are safe for concurrent access.
 *
 * Usage:
 * ```
 * // Generate and send weekly summary
 * WeeklySummaryService.generateAndSendWeeklySummary(
 *     context = context,
 *     transactionRepository = transactionRepository,
 *     appPreferences = appPreferences
 * )
 * ```
 */
object WeeklySummaryService {

    private const val TAG = "WeeklySummaryService"

    /**
     * Prefix for summary tracking keys in AppPreferences.
     * Full key format: "{PREFIX}{weekKey}"
     */
    private const val SUMMARY_KEY_PREFIX = "weekly_summary_sent_"

    /**
     * Generates weekly spending summary and sends a notification.
     *
     * This method:
     * 1. Retrieves user's weekly summary notification settings
     * 2. Checks if weekly summary notifications are enabled
     * 3. Checks if summary has already been sent this week
     * 4. Retrieves all transactions from the repository
     * 5. Uses AnalyticsCalculator to generate weekly summary data
     * 6. Formats summary data into a comprehensive notification message
     * 7. Sends notification via SpendingNotificationManager
     * 8. Tracks summary state to prevent duplicate notifications
     *
     * The method respects user notification preferences:
     * - Only sends summaries if weekly summary notifications are enabled
     * - Uses user-configured day/time for scheduling (handled by coordinator)
     *
     * @param context Application context for notifications and preferences
     * @param transactionRepository Repository for accessing transaction data
     * @param appPreferences User preferences for notification settings
     * @return Result<Boolean> containing true if summary was sent, false if not needed, or error information
     */
    suspend fun generateAndSendWeeklySummary(
        context: Context,
        transactionRepository: TransactionRepository,
        appPreferences: AppPreferences
    ): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Generate and send weekly summary") {
            // Check if weekly summary notifications are enabled
            val settings = appPreferences.getWeeklySummarySettings()
            if (!settings.enabled) {
                ErrorHandler.logDebug("Weekly summary notifications disabled, skipping", TAG)
                return@runSuspendCatching false
            }

            // Get current week key
            val weekKey = getCurrentWeekKey()

            // Check if summary already sent for this week
            val summaryKey = getSummaryKey(weekKey)
            if (appPreferences.getBoolean(summaryKey, false)) {
                ErrorHandler.logDebug(
                    "Weekly summary already sent for week $weekKey, skipping",
                    TAG
                )
                return@runSuspendCatching false
            }

            // Get all transactions
            val transactionsResult = transactionRepository.getAllTransactionsSnapshot()
            if (transactionsResult.isFailure) {
                ErrorHandler.logWarning(
                    "Failed to retrieve transactions: ${transactionsResult.exceptionOrNull()?.message}",
                    TAG
                )
                return@runSuspendCatching false
            }

            val transactions = transactionsResult.getOrNull() ?: emptyList()
            if (transactions.isEmpty()) {
                ErrorHandler.logDebug("No transactions available, skipping weekly summary", TAG)
                return@runSuspendCatching false
            }

            // Create category map for analytics
            val categoryMap = createCategoryMap()

            // Generate weekly summary using AnalyticsCalculator
            val analyticsCalculator = AnalyticsCalculator()
            val summary = analyticsCalculator.calculatePeriodSummary(
                transactions = transactions,
                categoryMap = categoryMap,
                period = TimePeriod.WEEK
            )

            // Create notification data
            val notificationData = createWeeklySummaryNotificationData(summary, weekKey)

            // Send the notification
            val success = SpendingNotificationManager.showNotification(context, notificationData)

            if (success) {
                // Mark summary as sent for this week
                appPreferences.setBoolean(summaryKey, true)
                ErrorHandler.logInfo(
                    "Weekly summary sent for week $weekKey: ₹${formatAmount(summary.totalSpent)} spent",
                    TAG
                )
            } else {
                ErrorHandler.logWarning(
                    "Failed to send weekly summary for week $weekKey",
                    TAG
                )
            }

            success
        }
    }

    /**
     * Creates notification data for a weekly spending summary.
     *
     * The notification includes:
     * - Title: "Weekly Spending Summary"
     * - Message: Comprehensive summary with spending, income, transaction count, daily average, top category
     * - Action data: Summary statistics for deep linking
     *
     * @param summary The period summary data from AnalyticsCalculator
     * @param weekKey The week identifier for this summary
     * @return NotificationData configured for the weekly summary
     */
    private fun createWeeklySummaryNotificationData(
        summary: com.example.kanakku.data.model.PeriodSummary,
        weekKey: String
    ): NotificationData {
        val title = "Weekly Spending Summary"

        val message = buildString {
            // Total spending
            append("You spent ₹${formatAmount(summary.totalSpent)} this week")

            // Transaction count
            if (summary.transactionCount > 0) {
                append(" across ${summary.transactionCount} transaction${if (summary.transactionCount != 1) "s" else ""}. ")
            } else {
                append(". ")
            }

            // Daily average
            if (summary.averageDaily > 0) {
                append("Daily average: ₹${formatAmount(summary.averageDaily)}. ")
            }

            // Top category
            summary.topCategory?.let { category ->
                append("Top category: ${category.name}. ")
            }

            // Income (if any)
            if (summary.totalReceived > 0) {
                append("Income: ₹${formatAmount(summary.totalReceived)}.")
            }
        }

        return NotificationData(
            type = NotificationType.WEEKLY_SUMMARY,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            actionData = mapOf(
                "week_key" to weekKey,
                "total_spent" to summary.totalSpent.toString(),
                "total_received" to summary.totalReceived.toString(),
                "transaction_count" to summary.transactionCount.toString(),
                "average_daily" to summary.averageDaily.toString(),
                "top_category" to (summary.topCategory?.name ?: "None"),
                "period" to summary.period.name
            )
        )
    }

    /**
     * Creates a map of SMS IDs to Category objects for use with AnalyticsCalculator.
     *
     * Since the app uses category IDs stored in transactions rather than SMS IDs,
     * this creates a mapping using transaction indices as keys and default categories.
     * The AnalyticsCalculator will use this to categorize transactions.
     *
     * @return Map of transaction ID to Category
     */
    private fun createCategoryMap(): Map<Long, Category> {
        // Create a map with default categories indexed by their position
        // This is used by AnalyticsCalculator's getCategoryBreakdown method
        return DefaultCategories.ALL.mapIndexed { index, category ->
            index.toLong() to category
        }.toMap()
    }

    /**
     * Gets the current week key in ISO 8601 week date format.
     *
     * Format: "YYYY-Www" (e.g., "2026-W02" for week 2 of 2026)
     *
     * This key is used to:
     * - Track which week a summary was sent for
     * - Prevent duplicate summaries for the same week
     * - Identify week boundaries for reset logic
     *
     * @return String key identifying the current week
     */
    private fun getCurrentWeekKey(): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()

        val year = calendar.get(Calendar.YEAR)
        val week = calendar.get(Calendar.WEEK_OF_YEAR)

        return String.format("%04d-W%02d", year, week)
    }

    /**
     * Gets the formatted date range for the current week.
     *
     * Format: "MMM dd - MMM dd" (e.g., "Jan 01 - Jan 07")
     *
     * @return String representing the current week's date range
     */
    private fun getWeekDateRange(): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()

        // Start of week (Monday)
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = calendar.timeInMillis

        // End of week (Sunday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val weekEnd = calendar.timeInMillis

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        return "${dateFormat.format(Date(weekStart))} - ${dateFormat.format(Date(weekEnd))}"
    }

    /**
     * Generates the AppPreferences key for tracking summary state.
     *
     * @param weekKey Week identifier
     * @return Preference key for summary tracking
     */
    private fun getSummaryKey(weekKey: String): String {
        return "$SUMMARY_KEY_PREFIX$weekKey"
    }

    /**
     * Clears the summary tracking flag for a specific week.
     *
     * This allows the summary to be sent again if needed (e.g., for testing).
     *
     * @param weekKey Week identifier to clear summary for
     * @param appPreferences User preferences
     */
    fun clearSummary(weekKey: String, appPreferences: AppPreferences) {
        val summaryKey = getSummaryKey(weekKey)

        if (appPreferences.contains(summaryKey)) {
            appPreferences.remove(summaryKey)
            ErrorHandler.logDebug("Cleared weekly summary: $summaryKey", TAG)
        }
    }

    /**
     * Clears the summary tracking flag for the current week.
     *
     * Convenience method for clearing the current week's summary.
     *
     * @param appPreferences User preferences
     */
    fun clearCurrentWeekSummary(appPreferences: AppPreferences) {
        val weekKey = getCurrentWeekKey()
        clearSummary(weekKey, appPreferences)
    }

    /**
     * Clears all weekly summary tracking flags.
     *
     * This can be used to:
     * - Reset all summaries for testing purposes
     * - Clear summaries after user changes settings
     * - Clean up old summary data
     *
     * @param appPreferences User preferences
     * @return Number of summary flags cleared
     */
    fun clearAllSummaries(appPreferences: AppPreferences): Int {
        var clearedCount = 0
        try {
            val allKeys = appPreferences.getAllKeys()
            for (key in allKeys) {
                if (key.startsWith(SUMMARY_KEY_PREFIX)) {
                    appPreferences.remove(key)
                    clearedCount++
                }
            }
            ErrorHandler.logInfo("Cleared $clearedCount weekly summary flags", TAG)
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Clear all weekly summaries")
        }
        return clearedCount
    }

    /**
     * Clears old summary flags from previous weeks.
     *
     * This utility method helps clean up AppPreferences by removing summary flags
     * for weeks that are no longer active (older than specified weeks).
     *
     * @param appPreferences User preferences
     * @param weeksToKeep Number of recent weeks to keep (default: 4 for ~1 month)
     * @return Number of old summary flags cleared
     */
    fun clearOldSummaries(appPreferences: AppPreferences, weeksToKeep: Int = 4): Int {
        var clearedCount = 0
        try {
            val currentWeekKey = getCurrentWeekKey()
            val currentYear = currentWeekKey.substringBefore("-W").toInt()
            val currentWeek = currentWeekKey.substringAfter("-W").toInt()

            val allKeys = appPreferences.getAllKeys()
            for (key in allKeys) {
                if (key.startsWith(SUMMARY_KEY_PREFIX)) {
                    // Extract week key from the summary key
                    val weekKey = key.removePrefix(SUMMARY_KEY_PREFIX)

                    try {
                        val year = weekKey.substringBefore("-W").toInt()
                        val week = weekKey.substringAfter("-W").toInt()

                        // Calculate age in weeks (approximate)
                        val weeksDiff = (currentYear - year) * 52 + (currentWeek - week)

                        if (weeksDiff > weeksToKeep) {
                            appPreferences.remove(key)
                            clearedCount++
                        }
                    } catch (e: Exception) {
                        // Invalid week key format, remove it
                        appPreferences.remove(key)
                        clearedCount++
                    }
                }
            }
            ErrorHandler.logInfo("Cleared $clearedCount old weekly summary flags", TAG)
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Clear old weekly summaries")
        }
        return clearedCount
    }

    /**
     * Checks if a summary has already been sent for the current week.
     *
     * @param appPreferences User preferences
     * @return true if summary was already sent this week, false otherwise
     */
    fun hasSummaryBeenSentThisWeek(appPreferences: AppPreferences): Boolean {
        val weekKey = getCurrentWeekKey()
        val summaryKey = getSummaryKey(weekKey)
        return appPreferences.getBoolean(summaryKey, false)
    }

    /**
     * Checks if a summary has already been sent for a specific week.
     *
     * @param weekKey Week identifier to check
     * @param appPreferences User preferences
     * @return true if summary was already sent for that week, false otherwise
     */
    fun hasSummaryBeenSent(weekKey: String, appPreferences: AppPreferences): Boolean {
        val summaryKey = getSummaryKey(weekKey)
        return appPreferences.getBoolean(summaryKey, false)
    }

    /**
     * Formats an amount for display in notifications.
     *
     * @param amount Amount to format
     * @return Formatted string (e.g., "1,234.56")
     */
    private fun formatAmount(amount: Double): String {
        return String.format("%,.2f", amount)
    }
}
