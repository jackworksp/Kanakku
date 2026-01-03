package com.example.kanakku.data.sms

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.model.SmsMessage

/**
 * SMS reader for accessing SMS messages from the device inbox.
 *
 * This class handles reading SMS messages with comprehensive error handling
 * to gracefully manage edge cases like:
 * - Permission denied or revoked mid-read
 * - Corrupted SMS data or cursor
 * - Invalid column indexes
 * - Null or malformed data
 *
 * All errors are logged via ErrorHandler and partial results are returned
 * when possible to ensure the app remains functional even with data issues.
 *
 * @param context Android context for accessing content resolver
 */
class SmsReader(private val context: Context) {

    /**
     * Reads SMS messages from inbox for the specified number of days.
     *
     * This method:
     * - Queries SMS inbox with date filter
     * - Handles permission errors gracefully
     * - Returns partial results if cursor fails mid-read
     * - Logs all errors for debugging
     *
     * @param sinceDaysAgo Number of days to look back (default: 30)
     * @return List of SMS messages, or empty list on permission/error
     */
    fun readInboxSms(sinceDaysAgo: Int = 30): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val contentResolver: ContentResolver = context.contentResolver

        // Calculate timestamp for filtering (e.g., last 30 days)
        val sinceTimestamp = System.currentTimeMillis() - (sinceDaysAgo * 24 * 60 * 60 * 1000L)

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ
        )

        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(sinceTimestamp.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            // Query SMS inbox
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            // Handle null cursor (permission denied or content provider unavailable)
            if (cursor == null) {
                ErrorHandler.logWarning(
                    "SMS query returned null cursor - permission may be denied or content provider unavailable",
                    "readInboxSms"
                )
                return emptyList()
            }

            try {
                cursor.use {
                    // Get column indexes with error handling
                    val columnIndexes = getColumnIndexes(cursor)
                    if (columnIndexes == null) {
                        ErrorHandler.logWarning(
                            "Failed to get column indexes - cursor may be corrupted",
                            "readInboxSms"
                        )
                        return emptyList()
                    }

                    // Read messages with per-row error handling
                    while (cursor.moveToNext()) {
                        try {
                            val sms = extractSmsFromCursor(cursor, columnIndexes)
                            if (sms != null) {
                                messages.add(sms)
                            }
                        } catch (e: Exception) {
                            // Log error but continue reading other messages (partial results)
                            ErrorHandler.handleError(e, "Reading individual SMS")
                            // Continue to next message
                        }
                    }
                }
            } catch (e: IllegalStateException) {
                // Cursor closed or invalid state
                ErrorHandler.handleError(e, "readInboxSms cursor processing")
                // Return partial results
            }
        } catch (e: SecurityException) {
            // Permission denied - log and return empty list
            ErrorHandler.handleError(e, "readInboxSms - SMS permission denied")
        } catch (e: IllegalArgumentException) {
            // Invalid arguments to query
            ErrorHandler.handleError(e, "readInboxSms - invalid query arguments")
        } catch (e: Exception) {
            // Catch-all for unexpected errors
            ErrorHandler.handleError(e, "readInboxSms - unexpected error")
        }

        // Log success with count
        ErrorHandler.logDebug("Successfully read ${messages.size} SMS messages", "readInboxSms")
        return messages
    }

    /**
     * Reads SMS messages since a specific timestamp.
     *
     * This method:
     * - Queries SMS inbox with timestamp filter (exclusive)
     * - Handles permission errors gracefully
     * - Returns partial results if cursor fails mid-read
     * - Logs all errors for debugging
     *
     * @param sinceTimestamp Unix timestamp in milliseconds (exclusive)
     * @return List of SMS messages after the timestamp, or empty list on permission/error
     */
    fun readSmsSince(sinceTimestamp: Long): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ
        )

        val selection = "${Telephony.Sms.DATE} > ?"
        val selectionArgs = arrayOf(sinceTimestamp.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            // Query SMS inbox
            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            // Handle null cursor (permission denied or content provider unavailable)
            if (cursor == null) {
                ErrorHandler.logWarning(
                    "SMS query returned null cursor - permission may be denied or content provider unavailable",
                    "readSmsSince"
                )
                return emptyList()
            }

            try {
                cursor.use {
                    // Get column indexes with error handling
                    val columnIndexes = getColumnIndexes(cursor)
                    if (columnIndexes == null) {
                        ErrorHandler.logWarning(
                            "Failed to get column indexes - cursor may be corrupted",
                            "readSmsSince"
                        )
                        return emptyList()
                    }

                    // Read messages with per-row error handling
                    while (cursor.moveToNext()) {
                        try {
                            val sms = extractSmsFromCursor(cursor, columnIndexes)
                            if (sms != null) {
                                messages.add(sms)
                            }
                        } catch (e: Exception) {
                            // Log error but continue reading other messages (partial results)
                            ErrorHandler.handleError(e, "Reading individual SMS")
                            // Continue to next message
                        }
                    }
                }
            } catch (e: IllegalStateException) {
                // Cursor closed or invalid state
                ErrorHandler.handleError(e, "readSmsSince cursor processing")
                // Return partial results
            }
        } catch (e: SecurityException) {
            // Permission denied - log and return empty list
            ErrorHandler.handleError(e, "readSmsSince - SMS permission denied")
        } catch (e: IllegalArgumentException) {
            // Invalid arguments to query
            ErrorHandler.handleError(e, "readSmsSince - invalid query arguments")
        } catch (e: Exception) {
            // Catch-all for unexpected errors
            ErrorHandler.handleError(e, "readSmsSince - unexpected error")
        }

        // Log success with count
        ErrorHandler.logDebug("Successfully read ${messages.size} SMS messages since timestamp $sinceTimestamp", "readSmsSince")
        return messages
    }

    /**
     * Column indexes holder for SMS cursor columns.
     * Caching indexes improves performance and makes null-safety easier.
     */
    private data class ColumnIndexes(
        val idIndex: Int,
        val addressIndex: Int,
        val bodyIndex: Int,
        val dateIndex: Int,
        val readIndex: Int
    )

    /**
     * Safely retrieves column indexes from cursor.
     * Returns null if any column is not found (indicates corrupted cursor).
     *
     * @param cursor The cursor to get indexes from
     * @return ColumnIndexes if all columns found, null otherwise
     */
    private fun getColumnIndexes(cursor: Cursor): ColumnIndexes? {
        return try {
            ColumnIndexes(
                idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID),
                addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS),
                bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY),
                dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE),
                readIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
            )
        } catch (e: IllegalArgumentException) {
            // Column not found - corrupted cursor
            ErrorHandler.handleError(e, "getColumnIndexes - cursor missing expected columns")
            null
        }
    }

    /**
     * Safely extracts an SmsMessage from a cursor row.
     * Handles null values and data corruption gracefully.
     *
     * @param cursor The cursor positioned at a row
     * @param indexes The column indexes
     * @return SmsMessage if successfully extracted, null if data is corrupted
     */
    private fun extractSmsFromCursor(cursor: Cursor, indexes: ColumnIndexes): SmsMessage? {
        return try {
            // Get ID (required field)
            val id = cursor.getLong(indexes.idIndex)

            // Get address with null handling (default to empty string)
            val address = if (cursor.isNull(indexes.addressIndex)) {
                ErrorHandler.logDebug("SMS $id has null address", "extractSmsFromCursor")
                ""
            } else {
                cursor.getString(indexes.addressIndex) ?: ""
            }

            // Get body with null handling (default to empty string)
            val body = if (cursor.isNull(indexes.bodyIndex)) {
                ErrorHandler.logDebug("SMS $id has null body", "extractSmsFromCursor")
                ""
            } else {
                cursor.getString(indexes.bodyIndex) ?: ""
            }

            // Get date (required field)
            val date = cursor.getLong(indexes.dateIndex)

            // Get read status with null handling (default to false)
            val isRead = if (cursor.isNull(indexes.readIndex)) {
                false
            } else {
                cursor.getInt(indexes.readIndex) == 1
            }

            SmsMessage(
                id = id,
                address = address,
                body = body,
                date = date,
                isRead = isRead
            )
        } catch (e: IllegalStateException) {
            // Cursor in invalid state
            ErrorHandler.handleError(e, "extractSmsFromCursor - cursor in invalid state")
            null
        } catch (e: NullPointerException) {
            // Unexpected null value
            ErrorHandler.handleError(e, "extractSmsFromCursor - unexpected null value")
            null
        } catch (e: Exception) {
            // Other unexpected errors
            ErrorHandler.handleError(e, "extractSmsFromCursor - unexpected error")
            null
        }
    }
}
