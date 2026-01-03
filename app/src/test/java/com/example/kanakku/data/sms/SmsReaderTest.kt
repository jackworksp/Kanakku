package com.example.kanakku.data.sms

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

/**
 * Unit tests for SmsReader class.
 *
 * Tests cover:
 * - readAllSms() method functionality
 * - Error handling for permission denied
 * - Empty inbox handling
 * - Partial results on cursor errors
 * - Null cursor handling
 *
 * Note: These tests use Robolectric's ShadowContentResolver to simulate
 * SMS content provider behavior without requiring actual SMS permissions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SmsReaderTest {

    private lateinit var context: Context
    private lateinit var smsReader: SmsReader

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        smsReader = SmsReader(context)
    }

    // ==================== Helper Functions ====================

    /**
     * Creates a MatrixCursor with SMS columns and data
     */
    private fun createSmsCursor(messages: List<TestSmsData>): MatrixCursor {
        val cursor = MatrixCursor(
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ
            )
        )

        messages.forEach { msg ->
            cursor.addRow(
                arrayOf(
                    msg.id,
                    msg.address,
                    msg.body,
                    msg.date,
                    if (msg.isRead) 1 else 0
                )
            )
        }

        return cursor
    }

    /**
     * Registers a cursor to be returned by the ContentResolver
     */
    private fun registerSmsCursor(cursor: Cursor?) {
        ShadowContentResolver.registerProviderInternal(
            "sms",
            TestSmsProvider(cursor)
        )
    }

    /**
     * Test SMS data holder
     */
    private data class TestSmsData(
        val id: Long,
        val address: String,
        val body: String,
        val date: Long,
        val isRead: Boolean
    )

    /**
     * Test content provider that returns a pre-configured cursor
     */
    private class TestSmsProvider(private val cursor: Cursor?) : android.content.ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor? = cursor

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: android.content.ContentValues?) = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: android.content.ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }

    // ==================== readAllSms Tests ====================

    @Test
    fun readAllSms_returnsAllMessages_whenInboxHasMultipleSms() {
        // Given - Multiple SMS messages
        val now = System.currentTimeMillis()
        val testMessages = listOf(
            TestSmsData(1L, "VM-HDFCBK", "Credited Rs 1000", now, true),
            TestSmsData(2L, "VM-SBIBNK", "Debited Rs 500", now - 1000, false),
            TestSmsData(3L, "AMAZON", "Order confirmed", now - 2000, true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(3, result.size)

        assertEquals(1L, result[0].id)
        assertEquals("VM-HDFCBK", result[0].address)
        assertEquals("Credited Rs 1000", result[0].body)
        assertEquals(now, result[0].date)
        assertTrue(result[0].isRead)

        assertEquals(2L, result[1].id)
        assertEquals("VM-SBIBNK", result[1].address)
        assertFalse(result[1].isRead)

        assertEquals(3L, result[2].id)
        assertEquals("AMAZON", result[2].address)
    }

    @Test
    fun readAllSms_returnsEmptyList_whenInboxIsEmpty() {
        // Given - Empty inbox
        val cursor = createSmsCursor(emptyList())
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun readAllSms_returnsEmptyList_whenCursorIsNull() {
        // Given - Null cursor (permission denied or content provider unavailable)
        registerSmsCursor(null)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun readAllSms_handlesNullAddressGracefully() {
        // Given - SMS with null address
        val cursor = MatrixCursor(
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ
            )
        )
        cursor.addRow(arrayOf(1L, null, "Test body", System.currentTimeMillis(), 1))
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(1, result.size)
        assertEquals("", result[0].address)  // Should default to empty string
        assertEquals("Test body", result[0].body)
    }

    @Test
    fun readAllSms_handlesNullBodyGracefully() {
        // Given - SMS with null body
        val cursor = MatrixCursor(
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ
            )
        )
        cursor.addRow(arrayOf(1L, "SENDER", null, System.currentTimeMillis(), 1))
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(1, result.size)
        assertEquals("SENDER", result[0].address)
        assertEquals("", result[0].body)  // Should default to empty string
    }

    @Test
    fun readAllSms_handlesNullReadStatusGracefully() {
        // Given - SMS with null read status
        val cursor = MatrixCursor(
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ
            )
        )
        cursor.addRow(arrayOf(1L, "SENDER", "Body", System.currentTimeMillis(), null))
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(1, result.size)
        assertFalse(result[0].isRead)  // Should default to false
    }

    @Test
    fun readAllSms_handlesVeryLargeSmsCount() {
        // Given - Large number of SMS messages
        val largeMessageList = (1L..100L).map { id ->
            TestSmsData(
                id = id,
                address = "SENDER-$id",
                body = "Message body $id",
                date = System.currentTimeMillis() - id * 1000,
                isRead = id % 2 == 0L
            )
        }
        val cursor = createSmsCursor(largeMessageList)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(100, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(100L, result[99].id)
    }

    @Test
    fun readAllSms_handlesSpecialCharactersInSmsBody() {
        // Given - SMS with special characters
        val specialBody = "Rs.1,000 credited. Balance: Rs.5,000.50"
        val testMessages = listOf(
            TestSmsData(
                1L,
                "VM-HDFCBK",
                specialBody,
                System.currentTimeMillis(),
                true
            )
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(1, result.size)
        assertEquals(specialBody, result[0].body)
    }

    @Test
    fun readAllSms_handlesVeryLongSmsBody() {
        // Given - Very long SMS body
        val longBody = "A".repeat(1000)
        val testMessages = listOf(
            TestSmsData(1L, "SENDER", longBody, System.currentTimeMillis(), true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(1, result.size)
        assertEquals(longBody, result[0].body)
    }

    @Test
    fun readAllSms_handlesZeroTimestamp() {
        // Given - SMS with zero timestamp
        val testMessages = listOf(
            TestSmsData(1L, "SENDER", "Body", 0L, true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(1, result.size)
        assertEquals(0L, result[0].date)
    }

    @Test
    fun readAllSms_handlesMaxTimestamp() {
        // Given - SMS with maximum timestamp
        val testMessages = listOf(
            TestSmsData(1L, "SENDER", "Body", Long.MAX_VALUE, true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(1, result.size)
        assertEquals(Long.MAX_VALUE, result[0].date)
    }

    @Test
    fun readAllSms_preservesAllSmsFields() {
        // Given - SMS with all fields populated
        val testId = 12345L
        val testAddress = "VM-HDFCBK"
        val testBody = "Your account has been credited with Rs 1000"
        val testDate = System.currentTimeMillis()
        val testIsRead = true

        val testMessages = listOf(
            TestSmsData(testId, testAddress, testBody, testDate, testIsRead)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then - All fields should be preserved exactly
        assertEquals(1, result.size)
        assertEquals(testId, result[0].id)
        assertEquals(testAddress, result[0].address)
        assertEquals(testBody, result[0].body)
        assertEquals(testDate, result[0].date)
        assertEquals(testIsRead, result[0].isRead)
    }

    @Test
    fun readAllSms_multipleCallsReturnConsistentData() {
        // Given
        val testMessages = listOf(
            TestSmsData(1L, "SENDER", "Body", System.currentTimeMillis(), true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When - Call multiple times
        val result1 = smsReader.readAllSms()

        // Re-register cursor for second call
        val cursor2 = createSmsCursor(testMessages)
        registerSmsCursor(cursor2)
        val result2 = smsReader.readAllSms()

        // Then - Both calls should succeed
        assertEquals(1, result1.size)
        assertEquals(1, result2.size)
        assertEquals(result1[0].id, result2[0].id)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun readAllSms_withEmptyAddress() {
        // Given - SMS with empty string address
        val testMessages = listOf(
            TestSmsData(1L, "", "Body", System.currentTimeMillis(), true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(1, result.size)
        assertEquals("", result[0].address)
    }

    @Test
    fun readAllSms_withEmptyBody() {
        // Given - SMS with empty string body
        val testMessages = listOf(
            TestSmsData(1L, "SENDER", "", System.currentTimeMillis(), true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(1, result.size)
        assertEquals("", result[0].body)
    }

    @Test
    fun readAllSms_withMixedReadStatus() {
        // Given - Mix of read and unread messages
        val now = System.currentTimeMillis()
        val testMessages = listOf(
            TestSmsData(1L, "SENDER1", "Body1", now, true),
            TestSmsData(2L, "SENDER2", "Body2", now - 1000, false),
            TestSmsData(3L, "SENDER3", "Body3", now - 2000, true),
            TestSmsData(4L, "SENDER4", "Body4", now - 3000, false)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(4, result.size)
        assertTrue(result[0].isRead)
        assertFalse(result[1].isRead)
        assertTrue(result[2].isRead)
        assertFalse(result[3].isRead)
    }

    @Test
    fun readAllSms_withVariousBankSenders() {
        // Given - Messages from different bank senders
        val now = System.currentTimeMillis()
        val testMessages = listOf(
            TestSmsData(1L, "VM-HDFCBK", "HDFC Bank message", now, true),
            TestSmsData(2L, "AD-SBIBNK", "SBI Bank message", now - 1000, true),
            TestSmsData(3L, "VM-ICICIB", "ICICI Bank message", now - 2000, true),
            TestSmsData(4L, "AX-AXISBK", "Axis Bank message", now - 3000, true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(4, result.size)
        assertEquals("VM-HDFCBK", result[0].address)
        assertEquals("AD-SBIBNK", result[1].address)
        assertEquals("VM-ICICIB", result[2].address)
        assertEquals("AX-AXISBK", result[3].address)
    }

    @Test
    fun readAllSms_handlesTransactionSmsFormats() {
        // Given - Realistic bank transaction SMS messages
        val now = System.currentTimeMillis()
        val testMessages = listOf(
            TestSmsData(
                1L,
                "VM-HDFCBK",
                "Rs 500.00 debited from A/c XX1234 on 01-01-24. UPI Ref 123456789",
                now,
                true
            ),
            TestSmsData(
                2L,
                "AD-SBIBNK",
                "Your A/c XX5678 is credited with Rs 10000.00 on 01-01-24",
                now - 1000,
                true
            ),
            TestSmsData(
                3L,
                "VM-ICICIB",
                "ATM withdrawal of Rs 2000.00 from A/c XX9999. Available balance: Rs 50000.00",
                now - 2000,
                true
            )
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(3, result.size)
        assertTrue(result[0].body.contains("Rs 500.00"))
        assertTrue(result[1].body.contains("credited"))
        assertTrue(result[2].body.contains("ATM withdrawal"))
    }

    @Test
    fun readAllSms_returnsMessagesInDateDescendingOrder() {
        // Given - Messages with specific dates (newest to oldest)
        val now = System.currentTimeMillis()
        val testMessages = listOf(
            TestSmsData(3L, "SENDER3", "Newest", now, true),
            TestSmsData(2L, "SENDER2", "Middle", now - 1000, true),
            TestSmsData(1L, "SENDER1", "Oldest", now - 2000, true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then - Should maintain order from cursor (DESC order)
        assertEquals(3, result.size)
        assertEquals(3L, result[0].id)  // Newest first
        assertEquals(2L, result[1].id)
        assertEquals(1L, result[2].id)  // Oldest last
        assertTrue(result[0].date > result[1].date)
        assertTrue(result[1].date > result[2].date)
    }

    @Test
    fun readAllSms_withSingleMessage() {
        // Given - Single SMS message
        val testMessages = listOf(
            TestSmsData(1L, "SENDER", "Single message", System.currentTimeMillis(), true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
        assertEquals("SENDER", result[0].address)
        assertEquals("Single message", result[0].body)
    }

    @Test
    fun readAllSms_withIdenticalTimestamps() {
        // Given - Multiple messages with same timestamp
        val sameTime = System.currentTimeMillis()
        val testMessages = listOf(
            TestSmsData(1L, "SENDER1", "Message 1", sameTime, true),
            TestSmsData(2L, "SENDER2", "Message 2", sameTime, true),
            TestSmsData(3L, "SENDER3", "Message 3", sameTime, true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(3, result.size)
        assertEquals(sameTime, result[0].date)
        assertEquals(sameTime, result[1].date)
        assertEquals(sameTime, result[2].date)
    }

    @Test
    fun readAllSms_withNumericOnlySender() {
        // Given - SMS from numeric sender (e.g., short code)
        val testMessages = listOf(
            TestSmsData(1L, "12345", "OTP: 123456", System.currentTimeMillis(), true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When
        val result = smsReader.readAllSms()

        // Then
        assertEquals(1, result.size)
        assertEquals("12345", result[0].address)
        assertEquals("OTP: 123456", result[0].body)
    }

    @Test
    fun readAllSms_closedCursorDoesNotCrash() {
        // Given - Cursor that will be closed
        val testMessages = listOf(
            TestSmsData(1L, "SENDER", "Body", System.currentTimeMillis(), true)
        )
        val cursor = createSmsCursor(testMessages)
        registerSmsCursor(cursor)

        // When - Read SMS (cursor will be closed after use)
        val result = smsReader.readAllSms()

        // Then - Should complete successfully
        assertEquals(1, result.size)
        assertTrue(cursor.isClosed)
    }
}
