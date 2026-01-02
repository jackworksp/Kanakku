package com.example.kanakku.data.sms

import android.content.ContentResolver
import android.content.Context
import android.provider.Telephony
import com.example.kanakku.data.model.SmsMessage

class SmsReader(private val context: Context) {

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
            contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val readIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)

                while (cursor.moveToNext()) {
                    val sms = SmsMessage(
                        id = cursor.getLong(idIndex),
                        address = cursor.getString(addressIndex) ?: "",
                        body = cursor.getString(bodyIndex) ?: "",
                        date = cursor.getLong(dateIndex),
                        isRead = cursor.getInt(readIndex) == 1
                    )
                    messages.add(sms)
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
            e.printStackTrace()
        }

        return messages
    }

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
            contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val readIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)

                while (cursor.moveToNext()) {
                    val sms = SmsMessage(
                        id = cursor.getLong(idIndex),
                        address = cursor.getString(addressIndex) ?: "",
                        body = cursor.getString(bodyIndex) ?: "",
                        date = cursor.getLong(dateIndex),
                        isRead = cursor.getInt(readIndex) == 1
                    )
                    messages.add(sms)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        return messages
    }
}
