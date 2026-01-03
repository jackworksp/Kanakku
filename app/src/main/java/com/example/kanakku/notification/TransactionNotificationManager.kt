package com.example.kanakku.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.kanakku.MainActivity
import com.example.kanakku.R
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages transaction notifications for the Kanakku app.
 *
 * This class provides:
 * - Notification channel creation and management (required for Android 8+)
 * - Rich notification building for transaction alerts
 * - Separate styling for debit vs credit transactions
 * - Tap-to-open functionality to launch the app
 *
 * Notification Channels:
 * - TRANSACTIONS: High importance channel for all transaction alerts
 *   - Shows badge, sound, and heads-up notifications
 *   - User can customize notification behavior in system settings
 *
 * Thread-safety: All methods are thread-safe and can be called from any thread.
 * The NotificationManagerCompat handles thread-safety internally.
 *
 * Usage:
 * ```
 * val notificationManager = TransactionNotificationManager(context)
 * notificationManager.createNotificationChannel()
 * notificationManager.showTransactionNotification(parsedTransaction)
 * ```
 *
 * Offline-First Architecture:
 * - All notifications are local-only, no network required
 * - Transaction data never leaves the device
 * - Notifications work in airplane mode
 */
class TransactionNotificationManager(private val context: Context) {

    companion object {
        // Notification Channel IDs
        private const val CHANNEL_ID_TRANSACTIONS = "transactions"

        // Notification IDs (unique per notification type)
        private const val NOTIFICATION_ID_BASE = 1000

        private const val TAG = "TransactionNotificationManager"
    }

    // NotificationManagerCompat for backward compatibility
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    // Currency formatter for displaying amounts
    private val currencyFormatter: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }

    // Date formatter for timestamps
    private val dateFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    }

    /**
     * Creates the notification channel for transaction alerts.
     *
     * This method must be called before showing notifications on Android 8.0 (API 26) and above.
     * It's safe to call multiple times - the channel will only be created once.
     *
     * Channel Properties:
     * - ID: "transactions"
     * - Name: "Transaction Alerts"
     * - Importance: HIGH (shows as heads-up notification)
     * - Sound: Default notification sound
     * - Vibration: Enabled
     * - Badge: Shows badge on app icon
     *
     * The channel can be customized by the user in system settings after creation.
     *
     * @return true if channel was created successfully (or not needed), false on error
     */
    fun createNotificationChannel(): Boolean {
        // Notification channels are only required on Android 8.0 (API 26) and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            ErrorHandler.logInfo(
                "Notification channels not required on API ${Build.VERSION.SDK_INT}",
                TAG
            )
            return true
        }

        return try {
            // Create notification channel for transaction alerts
            val channel = NotificationChannel(
                CHANNEL_ID_TRANSACTIONS,
                "Transaction Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new bank transactions detected from SMS"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // Register the channel with the system
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannel(channel)

            ErrorHandler.logInfo(
                "Notification channel created: $CHANNEL_ID_TRANSACTIONS",
                TAG
            )
            true

        } catch (e: Exception) {
            ErrorHandler.handleError(e, "$TAG.createNotificationChannel")
            false
        }
    }

    /**
     * Shows a notification for a new transaction.
     *
     * This method builds and displays a rich notification with:
     * - Transaction type icon (↓ for debit, ↑ for credit)
     * - Amount formatted as currency
     * - Merchant name or sender address
     * - Account number (if available)
     * - Transaction timestamp
     * - Color coding (red for debit, green for credit)
     *
     * The notification will:
     * - Show as a heads-up notification (if importance is HIGH)
     * - Play the default notification sound
     * - Vibrate the device
     * - Show a badge on the app icon
     * - Launch the app when tapped
     *
     * @param transaction The parsed transaction to show
     * @return true if notification was shown successfully, false on error or if notifications are disabled
     */
    fun showTransactionNotification(transaction: ParsedTransaction): Boolean {
        return try {
            ErrorHandler.logInfo(
                "Showing notification for ${transaction.type} transaction: ${transaction.amount}",
                TAG
            )

            // Check if notifications are enabled for this app
            if (!notificationManager.areNotificationsEnabled()) {
                ErrorHandler.logWarning(
                    "Notifications are disabled by user, skipping notification",
                    TAG
                )
                return false
            }

            // Build the notification
            val notification = buildTransactionNotification(transaction)

            // Show the notification with unique ID based on SMS ID
            val notificationId = NOTIFICATION_ID_BASE + (transaction.smsId % Int.MAX_VALUE).toInt()
            notificationManager.notify(notificationId, notification.build())

            ErrorHandler.logInfo(
                "Notification shown successfully with ID: $notificationId",
                TAG
            )
            true

        } catch (e: Exception) {
            ErrorHandler.handleError(e, "$TAG.showTransactionNotification")
            false
        }
    }

    /**
     * Builds a notification for a transaction.
     *
     * This method creates a styled notification with appropriate content based on the
     * transaction type (debit or credit).
     *
     * @param transaction The transaction to build notification for
     * @return NotificationCompat.Builder with the configured notification
     */
    private fun buildTransactionNotification(transaction: ParsedTransaction): NotificationCompat.Builder {
        // Create intent to launch MainActivity when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Format amount as currency
        val formattedAmount = currencyFormatter.format(transaction.amount)

        // Determine notification content based on transaction type
        val (title, contentText, icon, color) = when (transaction.type) {
            TransactionType.DEBIT -> {
                val merchant = transaction.merchant ?: transaction.senderAddress
                Tuple4(
                    "Money Debited: $formattedAmount",
                    "Paid to $merchant",
                    "↓", // Down arrow for debit
                    0xFFE53935.toInt() // Red color for debit
                )
            }
            TransactionType.CREDIT -> {
                val source = transaction.merchant ?: transaction.senderAddress
                Tuple4(
                    "Money Credited: $formattedAmount",
                    "Received from $source",
                    "↑", // Up arrow for credit
                    0xFF43A047.toInt() // Green color for credit
                )
            }
            TransactionType.UNKNOWN -> {
                Tuple4(
                    "Transaction: $formattedAmount",
                    "From ${transaction.senderAddress}",
                    "?",
                    0xFF757575.toInt() // Gray color for unknown
                )
            }
        }

        // Build expanded text with additional details
        val expandedText = buildString {
            append(contentText)

            // Add account number if available
            transaction.accountNumber?.let {
                append("\n")
                append("Account: **** $it")
            }

            // Add balance if available
            transaction.balanceAfter?.let {
                append("\n")
                append("Balance: ${currencyFormatter.format(it)}")
            }

            // Add timestamp
            append("\n")
            append("Time: ${dateFormatter.format(Date(transaction.date))}")

            // Add reference number if available
            transaction.referenceNumber?.let {
                append("\n")
                append("Ref: $it")
            }
        }

        // Build the notification
        return NotificationCompat.Builder(context, CHANNEL_ID_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
            )
            .setColor(color)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // For pre-Android 8 devices
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true) // Dismiss when tapped
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) // Hide sensitive info on lock screen
            .setOnlyAlertOnce(false) // Always alert for new transactions
    }

    /**
     * Cancels a specific notification by ID.
     *
     * @param smsId The SMS ID of the transaction whose notification should be cancelled
     */
    fun cancelNotification(smsId: Long) {
        try {
            val notificationId = NOTIFICATION_ID_BASE + (smsId % Int.MAX_VALUE).toInt()
            notificationManager.cancel(notificationId)
            ErrorHandler.logInfo(
                "Cancelled notification with ID: $notificationId",
                TAG
            )
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "$TAG.cancelNotification")
        }
    }

    /**
     * Cancels all transaction notifications.
     */
    fun cancelAllNotifications() {
        try {
            notificationManager.cancelAll()
            ErrorHandler.logInfo("Cancelled all notifications", TAG)
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "$TAG.cancelAllNotifications")
        }
    }

    /**
     * Checks if notifications are enabled for this app.
     *
     * @return true if notifications are enabled, false otherwise
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
}

/**
 * Simple data class to hold four values.
 * Used to return multiple values from notification builder.
 */
private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
