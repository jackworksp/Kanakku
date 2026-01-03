package com.example.kanakku.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.database.DatabaseProvider
import com.example.kanakku.data.model.SmsMessage
import com.example.kanakku.data.sms.BankSmsParser

/**
 * Background service for processing bank transaction SMS messages.
 *
 * This WorkManager Worker handles the heavy lifting of:
 * - Parsing bank SMS using BankSmsParser
 * - Checking for duplicate transactions
 * - Saving parsed transactions to the database
 * - Preparing for notification triggering (Phase 3)
 *
 * WorkManager is used instead of IntentService (deprecated in API 30+) because:
 * - Automatically handles battery optimization and doze mode
 * - Survives app restarts and system reboots (if needed)
 * - Provides built-in retry and backoff mechanisms
 * - Thread-safe and follows modern Android best practices
 *
 * The service is lightweight and completes quickly (< 1 second typical):
 * 1. Parse SMS message (regex matching)
 * 2. Check if transaction already exists (single DB query)
 * 3. Save transaction if new (single DB insert)
 * 4. Log results
 *
 * Thread Safety: WorkManager executes work on a background thread automatically.
 * Battery Efficiency: Event-driven (triggered by SMS), completes in < 1 second.
 *
 * Usage from BroadcastReceiver:
 * ```kotlin
 * SmsProcessingService.enqueueWork(
 *     context = context,
 *     smsId = sms.id,
 *     sender = sms.address,
 *     body = sms.body,
 *     timestamp = sms.date
 * )
 * ```
 */
class SmsProcessingService(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        // Input parameter keys
        private const val KEY_SMS_ID = "sms_id"
        private const val KEY_SENDER = "sender"
        private const val KEY_BODY = "body"
        private const val KEY_TIMESTAMP = "timestamp"

        /**
         * Enqueues SMS processing work in the background.
         *
         * This method creates a one-time work request and schedules it with WorkManager.
         * The work will execute as soon as possible on a background thread.
         *
         * @param context Application or Activity context
         * @param smsId The unique SMS message ID
         * @param sender The sender address (e.g., "VM-HDFCBK")
         * @param body The SMS message body
         * @param timestamp The SMS timestamp in milliseconds
         */
        fun enqueueWork(
            context: Context,
            smsId: Long,
            sender: String,
            body: String,
            timestamp: Long
        ) {
            try {
                // Create input data for the worker
                val inputData = Data.Builder()
                    .putLong(KEY_SMS_ID, smsId)
                    .putString(KEY_SENDER, sender)
                    .putString(KEY_BODY, body)
                    .putLong(KEY_TIMESTAMP, timestamp)
                    .build()

                // Create one-time work request
                val workRequest = OneTimeWorkRequestBuilder<SmsProcessingService>()
                    .setInputData(inputData)
                    .build()

                // Enqueue work with WorkManager
                WorkManager.getInstance(context).enqueue(workRequest)

                ErrorHandler.logInfo(
                    "SMS processing work enqueued for SMS ID: $smsId",
                    "SmsProcessingService"
                )

            } catch (e: Exception) {
                // Log error but don't crash - SMS will not be processed
                ErrorHandler.handleError(e, "SmsProcessingService.enqueueWork")
            }
        }
    }

    // BankSmsParser for parsing transaction SMS
    private val bankSmsParser = BankSmsParser()

    /**
     * Performs the background work of processing the SMS message.
     *
     * This method:
     * 1. Extracts SMS data from input parameters
     * 2. Creates SmsMessage data model
     * 3. Parses SMS using BankSmsParser
     * 4. Checks for duplicate transactions
     * 5. Saves new transaction to database
     * 6. Logs results for debugging
     *
     * Returns:
     * - Result.success() if processing completed successfully
     * - Result.failure() if an error occurred (WorkManager may retry)
     * - Result.retry() is not used since SMS processing is time-sensitive
     *
     * @return Result indicating success or failure
     */
    override suspend fun doWork(): Result {
        return try {
            ErrorHandler.logInfo(
                "Starting SMS processing work",
                "SmsProcessingService.doWork"
            )

            // Extract input parameters
            val smsId = inputData.getLong(KEY_SMS_ID, -1L)
            val sender = inputData.getString(KEY_SENDER) ?: ""
            val body = inputData.getString(KEY_BODY) ?: ""
            val timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis())

            // Validate input parameters
            if (smsId == -1L || sender.isBlank() || body.isBlank()) {
                ErrorHandler.logWarning(
                    "Invalid SMS data: smsId=$smsId, sender=$sender, body length=${body.length}",
                    "SmsProcessingService.doWork"
                )
                return Result.failure()
            }

            // Create SmsMessage data model
            val smsMessage = SmsMessage(
                id = smsId,
                address = sender,
                body = body,
                date = timestamp,
                isRead = false
            )

            // Parse SMS into transaction
            val parsedTransaction = bankSmsParser.parseSms(smsMessage)

            if (parsedTransaction == null) {
                // SMS is not a valid bank transaction or parsing failed
                ErrorHandler.logWarning(
                    "Failed to parse SMS ID: $smsId from: $sender",
                    "SmsProcessingService.doWork"
                )
                return Result.failure()
            }

            // Get repository instance
            val repository = DatabaseProvider.getRepository(applicationContext)

            // Check if transaction already exists (prevent duplicates)
            val existsResult = repository.transactionExists(smsId)

            if (existsResult.isFailure) {
                // Database error checking for duplicates
                val error = existsResult.exceptionOrNull()
                ErrorHandler.handleError(
                    error as? Exception ?: Exception("Unknown error checking transaction existence"),
                    "SmsProcessingService.doWork - check exists"
                )
                return Result.failure()
            }

            val alreadyExists = existsResult.getOrDefault(false)

            if (alreadyExists) {
                // Transaction already exists - skip to prevent duplicates
                ErrorHandler.logInfo(
                    "Transaction already exists for SMS ID: $smsId, skipping",
                    "SmsProcessingService.doWork"
                )
                return Result.success()
            }

            // Save new transaction to database
            val saveResult = repository.saveTransaction(parsedTransaction)

            if (saveResult.isFailure) {
                // Database error saving transaction
                val error = saveResult.exceptionOrNull()
                ErrorHandler.handleError(
                    error as? Exception ?: Exception("Unknown error saving transaction"),
                    "SmsProcessingService.doWork - save transaction"
                )
                return Result.failure()
            }

            // Successfully processed and saved transaction
            ErrorHandler.logInfo(
                "Transaction saved successfully: ${parsedTransaction.type} ${parsedTransaction.amount} " +
                        "from ${parsedTransaction.merchant ?: parsedTransaction.senderAddress}",
                "SmsProcessingService.doWork"
            )

            // TODO (Phase 3): Trigger notification if enabled in preferences
            // val notificationManager = TransactionNotificationManager(applicationContext)
            // if (AppPreferences.getInstance(applicationContext).isNotificationsEnabled()) {
            //     notificationManager.showTransactionNotification(parsedTransaction)
            // }

            // TODO (Phase 5): Broadcast transaction update to UI if app is open
            // val intent = Intent(ACTION_NEW_TRANSACTION)
            // intent.putExtra(EXTRA_TRANSACTION_ID, parsedTransaction.smsId)
            // LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

            Result.success()

        } catch (e: Exception) {
            // Catch all exceptions to prevent worker from crashing
            ErrorHandler.handleError(e, "SmsProcessingService.doWork")
            Result.failure()
        }
    }
}
