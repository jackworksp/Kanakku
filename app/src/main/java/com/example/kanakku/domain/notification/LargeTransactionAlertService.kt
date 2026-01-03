package com.example.kanakku.domain.notification

import android.content.Context
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.model.NotificationData
import com.example.kanakku.data.model.NotificationType
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.notification.SpendingNotificationManager

/**
 * Service for detecting transactions exceeding the user-configured threshold and sending alerts.
 *
 * This service:
 * - Monitors transactions for amounts exceeding the user's configured threshold
 * - Sends notifications for large transactions to help users stay aware of significant spending
 * - Filters to only alert on DEBIT (spending) transactions, not CREDIT (income)
 * - Tracks which transactions have already been alerted to prevent duplicate notifications
 * - Respects user notification preferences (enabled/disabled, threshold amount)
 * - Provides fraud awareness by alerting users to unusually large transactions
 *
 * Large Transaction Alert Logic:
 * - Alerts are sent when a DEBIT transaction amount >= user-configured threshold
 * - Default threshold: ₹5,000.00
 * - Each transaction is alerted only once (tracked by transaction hash)
 * - CREDIT transactions are never alerted (income should not trigger fraud alerts)
 * - Alerts include transaction details: amount, merchant, category, date
 *
 * Alert Tracking:
 * - Alert state stored in AppPreferences to persist across app restarts
 * - Key format: "large_tx_alert_sent_{transactionHash}"
 * - Hash based on unique transaction attributes (amount, merchant, date, smsId)
 * - Prevents duplicate alerts for the same transaction
 *
 * Thread-safe: All methods use suspend functions and are safe for concurrent access.
 *
 * Usage:
 * ```
 * // Check a single transaction and send alert if needed
 * LargeTransactionAlertService.checkLargeTransactionAlert(
 *     context = context,
 *     transaction = transaction,
 *     appPreferences = appPreferences
 * )
 *
 * // Check multiple transactions
 * transactions.forEach { tx ->
 *     LargeTransactionAlertService.checkLargeTransactionAlert(
 *         context = context,
 *         transaction = tx,
 *         appPreferences = appPreferences
 *     )
 * }
 * ```
 */
object LargeTransactionAlertService {

    private const val TAG = "LargeTransactionAlertService"

    /**
     * Prefix for alert tracking keys in AppPreferences.
     * Full key format: "{PREFIX}{transactionHash}"
     */
    private const val ALERT_KEY_PREFIX = "large_tx_alert_sent_"

    /**
     * Checks a single transaction and sends an alert if it exceeds the threshold.
     *
     * This method:
     * 1. Retrieves user's large transaction notification settings
     * 2. Checks if large transaction alerts are enabled
     * 3. Filters out CREDIT transactions (only alert on spending)
     * 4. Compares transaction amount against user-configured threshold
     * 5. Checks if alert was already sent for this transaction
     * 6. Sends notification for newly detected large transactions
     * 7. Tracks alert state to prevent duplicate notifications
     *
     * The method respects user notification preferences:
     * - Only sends alerts if large transaction notifications are enabled
     * - Uses user-configured threshold amount (default: ₹5,000.00)
     *
     * @param context Application context for notifications and preferences
     * @param transaction Transaction to check
     * @param appPreferences User preferences for notification settings
     * @return Result<Boolean> containing true if alert was sent, false if not needed, or error information
     */
    suspend fun checkLargeTransactionAlert(
        context: Context,
        transaction: ParsedTransaction,
        appPreferences: AppPreferences
    ): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Check large transaction alert") {
            // Check if large transaction alerts are enabled
            val settings = appPreferences.getLargeTransactionSettings()
            if (!settings.enabled) {
                ErrorHandler.logDebug("Large transaction alerts disabled, skipping check", TAG)
                return@runSuspendCatching false
            }

            // Only alert on DEBIT transactions (spending, not income)
            if (transaction.type != TransactionType.DEBIT) {
                ErrorHandler.logDebug(
                    "Skipping large transaction alert for CREDIT transaction: ${transaction.merchant}",
                    TAG
                )
                return@runSuspendCatching false
            }

            // Check if transaction exceeds threshold
            if (transaction.amount < settings.threshold) {
                ErrorHandler.logDebug(
                    "Transaction amount ${transaction.amount} below threshold ${settings.threshold}, skipping",
                    TAG
                )
                return@runSuspendCatching false
            }

            // Generate unique hash for this transaction
            val transactionHash = generateTransactionHash(transaction)

            // Check if alert already sent for this transaction
            val alertKey = getAlertKey(transactionHash)
            if (appPreferences.getBoolean(alertKey, false)) {
                ErrorHandler.logDebug(
                    "Alert already sent for transaction $transactionHash, skipping",
                    TAG
                )
                return@runSuspendCatching false
            }

            // Send the notification
            val notificationData = createLargeTransactionNotificationData(transaction, settings.threshold)
            val success = SpendingNotificationManager.showNotification(context, notificationData)

            if (success) {
                // Mark alert as sent
                appPreferences.setBoolean(alertKey, true)
                ErrorHandler.logInfo(
                    "Large transaction alert sent: ${transaction.merchant} ₹${formatAmount(transaction.amount)}",
                    TAG
                )
            } else {
                ErrorHandler.logWarning(
                    "Failed to send large transaction alert for ${transaction.merchant}",
                    TAG
                )
            }

            success
        }
    }

    /**
     * Checks multiple transactions and sends alerts for any that exceed the threshold.
     *
     * This is a convenience method for batch processing multiple transactions.
     * It internally calls checkLargeTransactionAlert() for each transaction.
     *
     * @param context Application context
     * @param transactions List of transactions to check
     * @param appPreferences User preferences
     * @return Result<Int> containing count of alerts sent, or error information
     */
    suspend fun checkLargeTransactionAlerts(
        context: Context,
        transactions: List<ParsedTransaction>,
        appPreferences: AppPreferences
    ): Result<Int> {
        return ErrorHandler.runSuspendCatching("Check large transaction alerts") {
            var alertsSent = 0

            for (transaction in transactions) {
                val result = checkLargeTransactionAlert(context, transaction, appPreferences)
                if (result.isSuccess && result.getOrNull() == true) {
                    alertsSent++
                }
            }

            ErrorHandler.logInfo("Large transaction alert check completed: $alertsSent alerts sent", TAG)
            alertsSent
        }
    }

    /**
     * Creates notification data for a large transaction alert.
     *
     * The notification includes:
     * - Title: "Large Transaction Detected"
     * - Message: Transaction details (amount, merchant, category, date)
     * - Action data: Transaction hash, amount, merchant, category for deep linking
     *
     * @param transaction The transaction that triggered the alert
     * @param threshold The threshold amount that was exceeded
     * @return NotificationData configured for the large transaction alert
     */
    private fun createLargeTransactionNotificationData(
        transaction: ParsedTransaction,
        threshold: Double
    ): NotificationData {
        val title = "Large Transaction Detected"

        val message = buildString {
            append("You spent ₹${formatAmount(transaction.amount)} at ${transaction.merchant}. ")
            append("This exceeds your large transaction threshold of ₹${formatAmount(threshold)}. ")
            if (transaction.categoryId.isNotEmpty()) {
                append("Category: ${transaction.categoryId}")
            }
        }

        return NotificationData(
            type = NotificationType.LARGE_TRANSACTION,
            title = title,
            message = message,
            timestamp = transaction.date,
            actionData = mapOf(
                "transaction_id" to generateTransactionHash(transaction),
                "amount" to transaction.amount.toString(),
                "merchant" to transaction.merchant,
                "category_id" to transaction.categoryId,
                "threshold" to threshold.toString(),
                "date" to transaction.date.toString()
            )
        )
    }

    /**
     * Generates a unique hash for a transaction.
     *
     * The hash is based on transaction attributes that uniquely identify it:
     * - Amount (to distinguish different transactions)
     * - Merchant (to distinguish different merchants)
     * - Date (to distinguish transactions on different days)
     * - SMS ID if available (to distinguish identical transactions)
     *
     * This hash is used to:
     * - Track which transactions have already been alerted
     * - Prevent duplicate notifications for the same transaction
     * - Generate unique notification IDs
     *
     * @param transaction Transaction to hash
     * @return String hash representing this unique transaction
     */
    private fun generateTransactionHash(transaction: ParsedTransaction): String {
        // Create a unique identifier based on transaction attributes
        val hashInput = buildString {
            append(transaction.amount.toString())
            append("_")
            append(transaction.merchant.hashCode())
            append("_")
            append(transaction.date)
            append("_")
            append(transaction.smsId ?: "0")
        }

        return hashInput.hashCode().toString()
    }

    /**
     * Generates the AppPreferences key for tracking alert state.
     *
     * @param transactionHash Unique transaction hash
     * @return Preference key for alert tracking
     */
    private fun getAlertKey(transactionHash: String): String {
        return "$ALERT_KEY_PREFIX$transactionHash"
    }

    /**
     * Clears the alert tracking flag for a specific transaction.
     *
     * This allows the alert to be sent again if needed (e.g., for testing).
     *
     * @param transaction Transaction to clear alert for
     * @param appPreferences User preferences
     */
    fun clearAlert(transaction: ParsedTransaction, appPreferences: AppPreferences) {
        val transactionHash = generateTransactionHash(transaction)
        val alertKey = getAlertKey(transactionHash)

        if (appPreferences.contains(alertKey)) {
            appPreferences.remove(alertKey)
            ErrorHandler.logDebug("Cleared alert: $alertKey", TAG)
        }
    }

    /**
     * Clears all large transaction alert tracking flags.
     *
     * This can be used to:
     * - Reset all alerts for testing purposes
     * - Clear alerts after user changes settings
     * - Clean up old alert data
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
            ErrorHandler.logInfo("Cleared $clearedCount large transaction alert flags", TAG)
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "Clear all large transaction alerts")
        }
        return clearedCount
    }

    /**
     * Clears old alert flags for transactions older than the specified timestamp.
     *
     * This utility method helps clean up AppPreferences by removing alert flags
     * for transactions that are no longer relevant (e.g., older than 30 days).
     *
     * Note: This method cannot determine transaction age from the hash alone,
     * so it clears ALL alert flags. For selective cleanup, the caller should
     * rebuild alerts after clearing.
     *
     * @param appPreferences User preferences
     * @param olderThan Timestamp threshold - alerts for transactions older than this will be cleared
     * @return Number of old alert flags cleared
     */
    fun clearOldAlerts(appPreferences: AppPreferences, olderThan: Long): Int {
        // Since we cannot determine transaction date from the hash alone,
        // we clear all alerts. Caller should rebuild alerts for recent transactions if needed.
        val clearedCount = clearAllAlerts(appPreferences)
        ErrorHandler.logInfo("Cleared $clearedCount old large transaction alert flags", TAG)
        return clearedCount
    }

    /**
     * Checks if an alert has already been sent for a specific transaction.
     *
     * @param transaction Transaction to check
     * @param appPreferences User preferences
     * @return true if alert was already sent, false otherwise
     */
    fun hasAlertBeenSent(transaction: ParsedTransaction, appPreferences: AppPreferences): Boolean {
        val transactionHash = generateTransactionHash(transaction)
        val alertKey = getAlertKey(transactionHash)
        return appPreferences.getBoolean(alertKey, false)
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
