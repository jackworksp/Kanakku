package com.example.kanakku.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.sms.BankSmsParser
import com.example.kanakku.service.SmsProcessingService

/**
 * BroadcastReceiver for intercepting incoming SMS messages in real-time.
 *
 * This receiver:
 * - Listens for SMS_RECEIVED broadcasts from the Android system
 * - Extracts SMS messages from PDU (Protocol Data Unit) format
 * - Uses goAsync() for extended execution time beyond the normal 10-second limit
 * - Handles errors gracefully with comprehensive error handling
 *
 * The receiver is registered in AndroidManifest.xml with high priority (999)
 * to ensure we receive SMS before other apps can consume them.
 *
 * Thread Safety: onReceive() is called on the main thread. Long-running
 * operations should be offloaded to a background service or worker.
 *
 * Battery Efficiency: This is event-driven (no polling), so battery impact
 * is minimal. Only triggered when SMS arrives.
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    // BankSmsParser for filtering and parsing bank transaction SMS
    private val bankSmsParser = BankSmsParser()

    /**
     * Called when an SMS_RECEIVED broadcast is received.
     *
     * This method:
     * 1. Calls goAsync() to extend execution time to ~30 seconds
     * 2. Extracts PDU data from the intent
     * 3. Converts PDUs to SmsMessage objects
     * 4. Logs the received messages for debugging
     *
     * Note: Actual processing (parsing, database save, notification) will be
     * handled by SmsProcessingService in a later phase.
     *
     * @param context Application context
     * @param intent The broadcast intent containing SMS data
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        // Validate inputs
        if (context == null || intent == null) {
            ErrorHandler.logWarning(
                "onReceive called with null context or intent",
                "SmsBroadcastReceiver"
            )
            return
        }

        // Check if this is an SMS_RECEIVED action
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            ErrorHandler.logWarning(
                "Received intent with unexpected action: ${intent.action}",
                "SmsBroadcastReceiver"
            )
            return
        }

        // Use goAsync() to extend execution time beyond 10 seconds
        // This is critical for reliable processing without ANR (Application Not Responding)
        val pendingResult = goAsync()

        try {
            // Extract SMS messages from the intent
            val messages = extractSmsMessages(intent)

            if (messages.isEmpty()) {
                ErrorHandler.logWarning(
                    "No SMS messages could be extracted from intent",
                    "SmsBroadcastReceiver"
                )
                return
            }

            // Log received SMS count for debugging
            ErrorHandler.logInfo(
                "Received ${messages.size} SMS message(s)",
                "SmsBroadcastReceiver"
            )

            // Process each SMS message
            for (message in messages) {
                processSmsMessage(context, message)
            }
        } catch (e: Exception) {
            // Catch all exceptions to prevent receiver from crashing
            ErrorHandler.handleError(e, "SmsBroadcastReceiver.onReceive")
        } finally {
            // Always finish the async work to release system resources
            pendingResult.finish()
        }
    }

    /**
     * Extracts SMS messages from the broadcast intent.
     *
     * This method handles both modern (API 19+) and legacy approaches:
     * - API 19+: Use Telephony.Sms.Intents.getMessagesFromIntent()
     * - API < 19: Manually extract PDUs from the "pdus" extra
     *
     * @param intent The SMS_RECEIVED intent
     * @return List of extracted SmsMessage objects, or empty list on error
     */
    private fun extractSmsMessages(intent: Intent): List<SmsMessage> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Modern approach (API 19+): Use Telephony API
                extractSmsMessagesModern(intent)
            } else {
                // Legacy approach (API < 19): Manual PDU extraction
                extractSmsMessagesLegacy(intent)
            }
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "extractSmsMessages")
            emptyList()
        }
    }

    /**
     * Extracts SMS messages using the modern Telephony API (API 19+).
     *
     * @param intent The SMS_RECEIVED intent
     * @return List of extracted SmsMessage objects
     */
    private fun extractSmsMessagesModern(intent: Intent): List<SmsMessage> {
        return try {
            // Use Telephony.Sms.Intents.getMessagesFromIntent() for reliable extraction
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages == null || messages.isEmpty()) {
                ErrorHandler.logWarning(
                    "getMessagesFromIntent returned null or empty array",
                    "extractSmsMessagesModern"
                )
                return emptyList()
            }

            // Convert SmsMessage array to list
            messages.toList()
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "extractSmsMessagesModern")
            emptyList()
        }
    }

    /**
     * Extracts SMS messages using legacy PDU extraction (API < 19).
     *
     * This method:
     * 1. Gets the "pdus" extra from the intent (array of byte arrays)
     * 2. Gets the format extra (e.g., "3gpp" or "3gpp2")
     * 3. Creates SmsMessage objects from each PDU
     *
     * @param intent The SMS_RECEIVED intent
     * @return List of extracted SmsMessage objects
     */
    @Suppress("DEPRECATION")
    private fun extractSmsMessagesLegacy(intent: Intent): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()

        try {
            // Get PDUs from intent extras
            val pdus = intent.extras?.get("pdus") as? Array<*>
            if (pdus == null || pdus.isEmpty()) {
                ErrorHandler.logWarning(
                    "No PDUs found in intent extras",
                    "extractSmsMessagesLegacy"
                )
                return emptyList()
            }

            // Get format (optional, for API 23+)
            val format = intent.extras?.getString("format")

            // Convert each PDU to SmsMessage
            for (pdu in pdus) {
                if (pdu !is ByteArray) {
                    ErrorHandler.logWarning(
                        "PDU is not a byte array, skipping",
                        "extractSmsMessagesLegacy"
                    )
                    continue
                }

                try {
                    val smsMessage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        // API 23+: Use format parameter
                        SmsMessage.createFromPdu(pdu, format)
                    } else {
                        // API < 23: No format parameter
                        SmsMessage.createFromPdu(pdu)
                    }

                    if (smsMessage != null) {
                        messages.add(smsMessage)
                    } else {
                        ErrorHandler.logWarning(
                            "createFromPdu returned null for a PDU",
                            "extractSmsMessagesLegacy"
                        )
                    }
                } catch (e: Exception) {
                    // Log error but continue processing other PDUs (partial results)
                    ErrorHandler.handleError(e, "Processing individual PDU")
                }
            }
        } catch (e: Exception) {
            ErrorHandler.handleError(e, "extractSmsMessagesLegacy")
        }

        return messages
    }

    /**
     * Processes a single SMS message.
     *
     * This method:
     * 1. Converts the Android SmsMessage to our app's SmsMessage data model
     * 2. Checks if the SMS is a bank transaction message using BankSmsParser
     * 3. Enqueues background work via SmsProcessingService if bank SMS detected
     *
     * The SmsProcessingService (WorkManager Worker) handles:
     * - Parsing transaction using BankSmsParser
     * - Checking for duplicate transactions
     * - Saving to database via TransactionRepository
     * - Notification triggering (Phase 3)
     * - UI update events (Phase 5)
     *
     * @param context Application context
     * @param message The Android SmsMessage to process
     */
    private fun processSmsMessage(context: Context, message: SmsMessage) {
        try {
            // Extract basic SMS information from Android SmsMessage
            val sender = message.originatingAddress ?: "Unknown"
            val body = message.messageBody ?: ""
            val timestamp = message.timestampMillis

            // Skip empty messages
            if (body.isBlank()) {
                ErrorHandler.logDebug(
                    "Skipping empty SMS from: $sender",
                    "processSmsMessage"
                )
                return
            }

            // Convert Android SmsMessage to our app's SmsMessage data model
            // Use timestamp as temporary ID since we don't have database ID yet
            val appSmsMessage = com.example.kanakku.data.model.SmsMessage(
                id = timestamp,  // Temporary ID, will be replaced when saved to database
                address = sender,
                body = body,
                date = timestamp,
                isRead = false  // Newly received SMS is unread
            )

            // Check if this is a bank transaction SMS using BankSmsParser
            val isBankTransaction = bankSmsParser.isBankTransactionSms(appSmsMessage)

            if (isBankTransaction) {
                // This is a bank transaction SMS - log details
                ErrorHandler.logInfo(
                    "Bank transaction SMS detected from: $sender, body length: ${body.length} chars",
                    "processSmsMessage"
                )

                // Start SmsProcessingService for background work
                // The service will:
                // - Parse transaction using bankSmsParser.parseSms()
                // - Save to database via TransactionRepository
                // - Trigger notification if enabled in preferences (Phase 3)
                SmsProcessingService.enqueueWork(
                    context = context,
                    smsId = timestamp,
                    sender = sender,
                    body = body,
                    timestamp = timestamp
                )

            } else {
                // Not a bank transaction SMS - skip silently for efficiency
                // (only log in debug builds to avoid spam)
                ErrorHandler.logDebug(
                    "Non-bank SMS from: $sender (filtered out)",
                    "processSmsMessage"
                )
            }

        } catch (e: Exception) {
            // Catch all exceptions to prevent receiver from crashing
            // Even if one SMS fails, others should still be processed
            ErrorHandler.handleError(e, "processSmsMessage")
        }
    }
}
