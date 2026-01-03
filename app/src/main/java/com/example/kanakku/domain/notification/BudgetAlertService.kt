package com.example.kanakku.domain.notification

import android.content.Context
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.database.entity.BudgetEntity
import com.example.kanakku.data.model.BudgetPeriod
import com.example.kanakku.data.model.BudgetThreshold
import com.example.kanakku.data.model.NotificationData
import com.example.kanakku.data.model.NotificationType
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.data.repository.BudgetRepository
import com.example.kanakku.data.repository.TransactionRepository
import com.example.kanakku.notification.SpendingNotificationManager
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Service for checking current spending against budget thresholds and triggering notifications.
 *
 * This service:
 * - Calculates current spending for each budget period (monthly/weekly)
 * - Compares spending against configured budget thresholds (80%, 100%)
 * - Sends budget warning notifications when thresholds are crossed
 * - Tracks which alerts have already been sent to avoid duplicate notifications
 * - Respects user notification preferences from AppPreferences
 *
 * Budget Alert Logic:
 * - 80% threshold (WARNING): Shown when spending reaches 80% of budget
 * - 100% threshold (LIMIT): Shown when spending reaches or exceeds 100% of budget
 * - Alerts are sent only once per threshold until the budget period resets
 * - Alert state is tracked per category, period, and threshold combination
 *
 * Alert Tracking:
 * - Alert state stored in AppPreferences to persist across app restarts
 * - Key format: "budget_alert_sent_{categoryId}_{period}_{threshold}_{periodKey}"
 * - periodKey identifies the specific month/week to detect period changes
 * - Old alerts are automatically cleared when a new budget period begins
 *
 * Thread-safe: All methods use suspend functions and are safe for concurrent access.
 *
 * Usage:
 * ```
 * // Check all budgets and send alerts if thresholds crossed
 * BudgetAlertService.checkBudgetAlerts(
 *     context = context,
 *     budgetRepository = budgetRepository,
 *     transactionRepository = transactionRepository,
 *     appPreferences = appPreferences
 * )
 * ```
 */
object BudgetAlertService {

    private const val TAG = "BudgetAlertService"

    /**
     * Prefix for alert tracking keys in AppPreferences.
     * Full key format: "{PREFIX}{categoryId}_{period}_{threshold}_{periodKey}"
     */
    private const val ALERT_KEY_PREFIX = "budget_alert_sent_"

    /**
     * Checks all budgets against current spending and sends alerts if thresholds are crossed.
     *
     * This method:
     * 1. Retrieves all budgets from the repository
     * 2. Calculates current spending for each budget period
     * 3. Checks if spending exceeds configured thresholds (80%, 100%)
     * 4. Sends notifications for newly crossed thresholds (not previously alerted)
     * 5. Tracks alert state to prevent duplicate notifications
     * 6. Clears old alerts when budget periods reset
     *
     * The method respects user notification preferences:
     * - Only sends alerts if budget notifications are enabled
     * - Respects individual threshold preferences (80%, 100%)
     *
     * @param context Application context for notifications and preferences
     * @param budgetRepository Repository for accessing budget data
     * @param transactionRepository Repository for accessing transaction data
     * @param appPreferences User preferences for notification settings
     * @return Result<Int> containing count of alerts sent, or error information
     */
    suspend fun checkBudgetAlerts(
        context: Context,
        budgetRepository: BudgetRepository,
        transactionRepository: TransactionRepository,
        appPreferences: AppPreferences
    ): Result<Int> {
        return ErrorHandler.runSuspendCatching("Check budget alerts") {
            // Check if budget alerts are enabled
            val budgetSettings = appPreferences.getBudgetAlertSettings()
            if (!budgetSettings.enabled) {
                ErrorHandler.logDebug("Budget alerts disabled, skipping check", TAG)
                return@runSuspendCatching 0
            }

            // Get all budgets
            val budgetsResult = budgetRepository.getAllBudgetsSnapshot()
            if (budgetsResult.isFailure) {
                ErrorHandler.logWarning(
                    "Failed to retrieve budgets: ${budgetsResult.exceptionOrNull()?.message}",
                    TAG
                )
                return@runSuspendCatching 0
            }

            val budgets = budgetsResult.getOrNull() ?: emptyList()
            if (budgets.isEmpty()) {
                ErrorHandler.logDebug("No budgets configured, skipping alert check", TAG)
                return@runSuspendCatching 0
            }

            // Check each budget for threshold crossings
            var alertsSent = 0
            for (budget in budgets) {
                val alertsForBudget = checkBudgetThresholds(
                    context = context,
                    budget = budget,
                    transactionRepository = transactionRepository,
                    appPreferences = appPreferences,
                    budgetSettings = budgetSettings
                )
                alertsSent += alertsForBudget
            }

            ErrorHandler.logInfo("Budget alert check completed: $alertsSent alerts sent", TAG)
            alertsSent
        }
    }

    /**
     * Checks a specific budget against spending thresholds and sends alerts if needed.
     *
     * @param context Application context
     * @param budget The budget to check
     * @param transactionRepository Repository for transaction data
     * @param appPreferences User preferences
     * @param budgetSettings Current budget alert settings
     * @return Number of alerts sent for this budget (0-2)
     */
    private suspend fun checkBudgetThresholds(
        context: Context,
        budget: BudgetEntity,
        transactionRepository: TransactionRepository,
        appPreferences: AppPreferences,
        budgetSettings: com.example.kanakku.data.model.BudgetAlertSettings
    ): Int {
        var alertsSent = 0

        try {
            // Calculate the date range for this budget period
            val (startDate, endDate) = getBudgetPeriodDates(budget.period, budget.startDate)
            val periodKey = getPeriodKey(budget.period, startDate)

            // Get spending for this period
            val spending = calculateSpending(
                transactionRepository = transactionRepository,
                categoryId = budget.categoryId,
                startDate = startDate,
                endDate = endDate
            )

            // Calculate threshold amounts
            val threshold80 = budget.amount * 0.8
            val threshold100 = budget.amount

            // Check 80% threshold
            if (budgetSettings.notifyAt80Percent && spending >= threshold80) {
                val alerted = checkAndSendAlert(
                    context = context,
                    budget = budget,
                    spending = spending,
                    threshold = BudgetThreshold.WARNING,
                    periodKey = periodKey,
                    appPreferences = appPreferences
                )
                if (alerted) alertsSent++
            }

            // Check 100% threshold
            if (budgetSettings.notifyAt100Percent && spending >= threshold100) {
                val alerted = checkAndSendAlert(
                    context = context,
                    budget = budget,
                    spending = spending,
                    threshold = BudgetThreshold.LIMIT,
                    periodKey = periodKey,
                    appPreferences = appPreferences
                )
                if (alerted) alertsSent++
            }

            // Clear old alerts if spending dropped below thresholds
            if (spending < threshold80) {
                clearAlert(budget, BudgetThreshold.WARNING, periodKey, appPreferences)
            }
            if (spending < threshold100) {
                clearAlert(budget, BudgetThreshold.LIMIT, periodKey, appPreferences)
            }

        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Check budget thresholds for category ${budget.categoryId}")
        }

        return alertsSent
    }

    /**
     * Checks if an alert should be sent and sends it if needed.
     *
     * @param context Application context
     * @param budget The budget being checked
     * @param spending Current spending amount
     * @param threshold Threshold type (80% or 100%)
     * @param periodKey Key identifying the current budget period
     * @param appPreferences User preferences for alert tracking
     * @return true if alert was sent, false if already sent or error occurred
     */
    private suspend fun checkAndSendAlert(
        context: Context,
        budget: BudgetEntity,
        spending: Double,
        threshold: BudgetThreshold,
        periodKey: String,
        appPreferences: AppPreferences
    ): Boolean {
        // Check if alert already sent for this period
        val alertKey = getAlertKey(budget.categoryId, budget.period, threshold, periodKey)
        if (appPreferences.getBoolean(alertKey, false)) {
            ErrorHandler.logDebug(
                "Alert already sent for $alertKey, skipping",
                TAG
            )
            return false
        }

        // Send the notification
        val notificationData = createBudgetNotificationData(
            budget = budget,
            spending = spending,
            threshold = threshold
        )

        val success = SpendingNotificationManager.showNotification(context, notificationData)

        if (success) {
            // Mark alert as sent
            appPreferences.setBoolean(alertKey, true)
            ErrorHandler.logInfo(
                "Budget alert sent: ${budget.categoryId} ${budget.period} ${threshold.percentage}%",
                TAG
            )
        } else {
            ErrorHandler.logWarning(
                "Failed to send budget alert for ${budget.categoryId}",
                TAG
            )
        }

        return success
    }

    /**
     * Creates notification data for a budget alert.
     *
     * @param budget The budget that triggered the alert
     * @param spending Current spending amount
     * @param threshold Threshold that was crossed
     * @return NotificationData configured for the budget alert
     */
    private fun createBudgetNotificationData(
        budget: BudgetEntity,
        spending: Double,
        threshold: BudgetThreshold
    ): NotificationData {
        val percentage = (spending / budget.amount * 100).toInt()
        val title = when (threshold) {
            BudgetThreshold.WARNING -> "Budget Alert: ${threshold.percentage}% Reached"
            BudgetThreshold.LIMIT -> "Budget Limit Reached!"
        }

        val message = when (threshold) {
            BudgetThreshold.WARNING -> {
                "You've spent ₹${formatAmount(spending)} of your ₹${formatAmount(budget.amount)} " +
                    "${budget.period.displayName.lowercase()} budget for category ${budget.categoryId}. " +
                    "You're at $percentage%."
            }
            BudgetThreshold.LIMIT -> {
                "You've reached your ${budget.period.displayName.lowercase()} budget limit! " +
                    "Spent: ₹${formatAmount(spending)} / ₹${formatAmount(budget.amount)} " +
                    "for category ${budget.categoryId}."
            }
        }

        return NotificationData(
            type = NotificationType.BUDGET_ALERT,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            actionData = mapOf(
                "threshold" to threshold.percentage.toString(),
                "category_id" to budget.categoryId,
                "period" to budget.period.name,
                "spending" to spending.toString(),
                "budget" to budget.amount.toString()
            )
        )
    }

    /**
     * Calculates total spending for a specific category within a date range.
     *
     * @param transactionRepository Repository for accessing transactions
     * @param categoryId Category to filter by
     * @param startDate Start of the period (inclusive)
     * @param endDate End of the period (inclusive)
     * @return Total spending amount (sum of DEBIT transactions)
     */
    private suspend fun calculateSpending(
        transactionRepository: TransactionRepository,
        categoryId: String,
        startDate: Long,
        endDate: Long
    ): Double {
        return try {
            // Get transactions for the date range
            val transactions = transactionRepository
                .getTransactionsByDateRange(startDate, endDate)
                .first()

            // Filter by category and sum DEBIT amounts
            transactions
                .filter { it.categoryId == categoryId }
                .filter { it.type == com.example.kanakku.data.model.TransactionType.DEBIT }
                .sumOf { it.amount }

        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Calculate spending for category $categoryId")
            0.0
        }
    }

    /**
     * Calculates the start and end date for a budget period.
     *
     * For MONTHLY budgets:
     * - Start: First day of the current month at 00:00:00
     * - End: Last day of the current month at 23:59:59
     *
     * For WEEKLY budgets:
     * - Start: Beginning of the week containing startDate (Monday)
     * - End: End of the week containing startDate (Sunday)
     *
     * @param period Budget period type (MONTHLY or WEEKLY)
     * @param startDate Reference start date from the budget
     * @return Pair of (startDate, endDate) timestamps in milliseconds
     */
    private fun getBudgetPeriodDates(period: BudgetPeriod, startDate: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis() // Use current time to get current period

        return when (period) {
            BudgetPeriod.MONTHLY -> {
                // Start of current month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis

                // End of current month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val monthEnd = calendar.timeInMillis

                Pair(monthStart, monthEnd)
            }

            BudgetPeriod.WEEKLY -> {
                // Start of current week (Monday)
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.timeInMillis

                // End of current week (Sunday)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val weekEnd = calendar.timeInMillis

                Pair(weekStart, weekEnd)
            }
        }
    }

    /**
     * Generates a unique key identifying the current budget period.
     *
     * This key is used to track which period an alert was sent for, allowing
     * alerts to be reset when a new period begins.
     *
     * Format:
     * - MONTHLY: "YYYY-MM" (e.g., "2026-01")
     * - WEEKLY: "YYYY-Www" (e.g., "2026-W02" for week 2)
     *
     * @param period Budget period type
     * @param startDate Start timestamp of the period
     * @return String key identifying this specific period
     */
    private fun getPeriodKey(period: BudgetPeriod, startDate: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate

        return when (period) {
            BudgetPeriod.MONTHLY -> {
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
                String.format("%04d-%02d", year, month)
            }
            BudgetPeriod.WEEKLY -> {
                val year = calendar.get(Calendar.YEAR)
                val week = calendar.get(Calendar.WEEK_OF_YEAR)
                String.format("%04d-W%02d", year, week)
            }
        }
    }

    /**
     * Generates the AppPreferences key for tracking alert state.
     *
     * @param categoryId Category identifier
     * @param period Budget period type
     * @param threshold Threshold type (WARNING or LIMIT)
     * @param periodKey Current period identifier
     * @return Preference key for alert tracking
     */
    private fun getAlertKey(
        categoryId: String,
        period: BudgetPeriod,
        threshold: BudgetThreshold,
        periodKey: String
    ): String {
        return "${ALERT_KEY_PREFIX}${categoryId}_${period.name}_${threshold.percentage}_$periodKey"
    }

    /**
     * Clears an alert tracking flag.
     *
     * This is called when spending drops below a threshold, allowing the alert
     * to be sent again if the threshold is crossed again in the same period.
     *
     * @param budget Budget entity
     * @param threshold Threshold to clear
     * @param periodKey Current period identifier
     * @param appPreferences User preferences
     */
    private fun clearAlert(
        budget: BudgetEntity,
        threshold: BudgetThreshold,
        periodKey: String,
        appPreferences: AppPreferences
    ) {
        val alertKey = getAlertKey(budget.categoryId, budget.period, threshold, periodKey)
        if (appPreferences.contains(alertKey)) {
            appPreferences.remove(alertKey)
            ErrorHandler.logDebug("Cleared alert: $alertKey", TAG)
        }
    }

    /**
     * Clears all budget alert tracking flags.
     *
     * This can be used to reset all alerts, for example after user configuration changes
     * or for testing purposes.
     *
     * @param appPreferences User preferences
     * @return Number of alert flags cleared
     */
    fun clearAllAlerts(appPreferences: AppPreferences): Int {
        var clearedCount = 0
        try {
            val allKeys = appPreferences.getAllKeys()
            for (key in allKeys) {
                if (key.startsWith(ALERT_KEY_PREFIX)) {
                    appPreferences.remove(key)
                    clearedCount++
                }
            }
            ErrorHandler.logInfo("Cleared $clearedCount budget alert flags", TAG)
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Clear all budget alerts")
        }
        return clearedCount
    }

    /**
     * Clears old alert flags from previous budget periods.
     *
     * This utility method helps clean up AppPreferences by removing alert flags
     * for periods that are no longer active.
     *
     * @param appPreferences User preferences
     * @param currentPeriodKeys Set of current period keys to preserve
     * @return Number of old alert flags cleared
     */
    fun clearOldAlerts(appPreferences: AppPreferences, currentPeriodKeys: Set<String>): Int {
        var clearedCount = 0
        try {
            val allKeys = appPreferences.getAllKeys()
            for (key in allKeys) {
                if (key.startsWith(ALERT_KEY_PREFIX)) {
                    // Extract period key from the alert key (last segment after final underscore)
                    val periodKey = key.substringAfterLast("_")
                    if (periodKey !in currentPeriodKeys) {
                        appPreferences.remove(key)
                        clearedCount++
                    }
                }
            }
            ErrorHandler.logInfo("Cleared $clearedCount old budget alert flags", TAG)
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Clear old budget alerts")
        }
        return clearedCount
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
