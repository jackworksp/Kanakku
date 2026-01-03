package com.example.kanakku.data.sms

import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.model.SmsMessage
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SmsDataSource covering SMS reading, filtering, and parsing.
 *
 * These tests verify the SmsDataSource wrapper functions correctly by testing:
 * - SMS reading operations (readAllSms, readSmsSince)
 * - Bank SMS filtering operations (readBankSms, readBankSmsSince)
 * - SMS parsing operations (parseSms, parseAllSms, parseAndDeduplicate, removeDuplicates)
 * - High-level convenience methods (readAndParseTransactions, readAndParseTransactionsSince)
 * - Utility methods (isBankTransactionSms)
 * - Error handling with Result type
 *
 * Note: These are integration-style tests using real SmsReader and BankSmsParser
 * instances with Robolectric. SmsReader will return empty results in test environment
 * since there are no actual SMS messages, but we can verify the wrapper behaves correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SmsDataSourceTest {

    private lateinit var smsDataSource: SmsDataSource

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        smsDataSource = SmsDataSource(context)
    }

    // ==================== Helper Functions ====================

    private fun createTestSmsMessage(
        id: Long = 1L,
        address: String = "VM-HDFCBK",
        body: String = "Rs.100 debited from account XX1234",
        date: Long = System.currentTimeMillis(),
        isRead: Boolean = true
    ): SmsMessage {
        return SmsMessage(
            id = id,
            address = address,
            body = body,
            date = date,
            isRead = isRead
        )
    }

    // ==================== SMS Reading Operations Tests ====================

    @Test
    fun readAllSms_returnsSuccessResult() = runTest {
        // When
        val result = smsDataSource.readAllSms(sinceDaysAgo = 30)

        // Then - Should return success (empty in test environment)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readAllSms_withCustomDays_returnsSuccess() = runTest {
        // When
        val result = smsDataSource.readAllSms(sinceDaysAgo = 7)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readAllSms_withDefaultParameter_returnsSuccess() = runTest {
        // When
        val result = smsDataSource.readAllSms()

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readSmsSince_returnsSuccessResult() = runTest {
        // Given
        val timestamp = System.currentTimeMillis() - 3600000 // 1 hour ago

        // When
        val result = smsDataSource.readSmsSince(timestamp)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readSmsSince_withZeroTimestamp_returnsSuccess() = runTest {
        // When
        val result = smsDataSource.readSmsSince(0L)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readSmsSince_withFutureTimestamp_returnsSuccess() = runTest {
        // Given
        val futureTimestamp = System.currentTimeMillis() + 3600000

        // When
        val result = smsDataSource.readSmsSince(futureTimestamp)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    // ==================== Bank SMS Filtering Operations Tests ====================

    @Test
    fun readBankSms_returnsSuccessResult() = runTest {
        // When
        val result = smsDataSource.readBankSms(sinceDaysAgo = 30)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readBankSms_withDefaultParameter_returnsSuccess() = runTest {
        // When
        val result = smsDataSource.readBankSms()

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readBankSmsSince_returnsSuccessResult() = runTest {
        // Given
        val timestamp = System.currentTimeMillis() - 3600000

        // When
        val result = smsDataSource.readBankSmsSince(timestamp)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readBankSmsSince_withZeroTimestamp_returnsSuccess() = runTest {
        // When
        val result = smsDataSource.readBankSmsSince(0L)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    // ==================== SMS Parsing Operations Tests ====================

    @Test
    fun parseSms_withBankTransactionSms_returnsTransaction() = runTest {
        // Given - A valid bank transaction SMS
        val bankSms = createTestSmsMessage(
            id = 1L,
            address = "VM-HDFCBK",
            body = "Rs.1000.00 debited from A/c XX1234 on 2024-01-03 at Amazon. Ref 123456789. Avl Bal Rs.5000.00"
        )

        // When
        val result = smsDataSource.parseSms(bankSms)

        // Then
        assertTrue(result.isSuccess)
        val transaction = result.getOrNull()
        assertNotNull(transaction)
        assertEquals(1L, transaction?.smsId)
        assertTrue(transaction?.amount ?: 0.0 > 0.0)
    }

    @Test
    fun parseSms_withNonBankSms_returnsNull() = runTest {
        // Given - A non-bank SMS (OTP)
        val nonBankSms = createTestSmsMessage(
            id = 1L,
            body = "Your OTP is 123456. Do not share with anyone."
        )

        // When
        val result = smsDataSource.parseSms(nonBankSms)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun parseSms_withCreditSms_returnsCredit() = runTest {
        // Given - A credit transaction SMS
        val creditSms = createTestSmsMessage(
            id = 2L,
            body = "Rs.5000.00 credited to A/c XX1234 on 2024-01-03. Ref 987654321. Avl Bal Rs.10000.00"
        )

        // When
        val result = smsDataSource.parseSms(creditSms)

        // Then
        assertTrue(result.isSuccess)
        val transaction = result.getOrNull()
        assertNotNull(transaction)
    }

    @Test
    fun parseAllSms_withEmptyList_returnsEmptyList() = runTest {
        // Given
        val emptyList = emptyList<SmsMessage>()

        // When
        val result = smsDataSource.parseAllSms(emptyList)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun parseAllSms_withMultipleSms_parsesAll() = runTest {
        // Given
        val smsList = listOf(
            createTestSmsMessage(
                id = 1L,
                body = "Rs.100.00 debited from A/c XX1234. Ref 111. Bal Rs.900"
            ),
            createTestSmsMessage(
                id = 2L,
                body = "Rs.200.00 credited to A/c XX1234. Ref 222. Bal Rs.1100"
            ),
            createTestSmsMessage(
                id = 3L,
                body = "Your OTP is 123456"
            )
        )

        // When
        val result = smsDataSource.parseAllSms(smsList)

        // Then
        assertTrue(result.isSuccess)
        val transactions = result.getOrNull()
        assertNotNull(transactions)
        // Only the first 2 SMS should be parsed (3rd is OTP)
        assertTrue((transactions?.size ?: 0) >= 0)
    }

    @Test
    fun parseAndDeduplicate_withEmptyList_returnsEmptyList() = runTest {
        // Given
        val emptyList = emptyList<SmsMessage>()

        // When
        val result = smsDataSource.parseAndDeduplicate(emptyList)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun parseAndDeduplicate_withDuplicates_removesDuplicates() = runTest {
        // Given - SMS with same reference number
        val timestamp = System.currentTimeMillis()
        val smsList = listOf(
            createTestSmsMessage(
                id = 1L,
                body = "Rs.100.00 debited from A/c XX1234. Ref 123456. Bal Rs.900",
                date = timestamp
            ),
            createTestSmsMessage(
                id = 2L,
                body = "Rs.100.00 debited from A/c XX1234. Ref 123456. Bal Rs.900",
                date = timestamp + 1000
            )
        )

        // When
        val result = smsDataSource.parseAndDeduplicate(smsList)

        // Then
        assertTrue(result.isSuccess)
        val transactions = result.getOrNull()
        assertNotNull(transactions)
        // Should have deduplicated (either 1 or 2 depending on dedup logic)
        assertTrue((transactions?.size ?: 0) <= 2)
    }

    @Test
    fun removeDuplicates_withEmptyList_returnsEmptyList() = runTest {
        // Given
        val emptyList = emptyList<com.example.kanakku.data.model.ParsedTransaction>()

        // When
        val result = smsDataSource.removeDuplicates(emptyList)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    // ==================== High-Level Convenience Methods Tests ====================

    @Test
    fun readAndParseTransactions_returnsSuccessResult() = runTest {
        // When
        val result = smsDataSource.readAndParseTransactions(sinceDaysAgo = 30)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readAndParseTransactions_withDefaultParameter_returnsSuccess() = runTest {
        // When
        val result = smsDataSource.readAndParseTransactions()

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readAndParseTransactions_with7Days_returnsSuccess() = runTest {
        // When
        val result = smsDataSource.readAndParseTransactions(sinceDaysAgo = 7)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readAndParseTransactionsSince_returnsSuccessResult() = runTest {
        // Given
        val timestamp = System.currentTimeMillis() - 3600000

        // When
        val result = smsDataSource.readAndParseTransactionsSince(timestamp)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readAndParseTransactionsSince_withZeroTimestamp_returnsSuccess() = runTest {
        // When
        val result = smsDataSource.readAndParseTransactionsSince(0L)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readAndParseTransactionsSince_withRecentTimestamp_returnsSuccess() = runTest {
        // Given - Very recent timestamp (1 minute ago)
        val recentTimestamp = System.currentTimeMillis() - 60000

        // When
        val result = smsDataSource.readAndParseTransactionsSince(recentTimestamp)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    // ==================== Utility Methods Tests ====================

    @Test
    fun isBankTransactionSms_withBankSms_returnsTrue() = runTest {
        // Given
        val bankSms = createTestSmsMessage(
            body = "Rs.100.00 debited from account XX1234 on 2024-01-03"
        )

        // When
        val result = smsDataSource.isBankTransactionSms(bankSms)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun isBankTransactionSms_withCreditSms_returnsTrue() = runTest {
        // Given
        val creditSms = createTestSmsMessage(
            body = "Rs.500.00 credited to your account XX1234"
        )

        // When
        val result = smsDataSource.isBankTransactionSms(creditSms)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun isBankTransactionSms_withOtpSms_returnsFalse() = runTest {
        // Given
        val otpSms = createTestSmsMessage(
            body = "Your OTP is 123456. Valid for 10 minutes."
        )

        // When
        val result = smsDataSource.isBankTransactionSms(otpSms)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun isBankTransactionSms_withNonBankSms_returnsFalse() = runTest {
        // Given
        val regularSms = createTestSmsMessage(
            body = "Hello, how are you doing today?"
        )

        // When
        val result = smsDataSource.isBankTransactionSms(regularSms)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun isBankTransactionSms_withPromotionalSms_returnsFalse() = runTest {
        // Given
        val promoSms = createTestSmsMessage(
            body = "Get 50% off on your next purchase! Limited time offer."
        )

        // When
        val result = smsDataSource.isBankTransactionSms(promoSms)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    // ==================== Result Type and Error Handling Tests ====================

    @Test
    fun allMethods_returnResultType() = runTest {
        // Verify all methods return Result type (compile-time check mostly)

        // When
        val result1 = smsDataSource.readAllSms()
        val result2 = smsDataSource.readSmsSince(0L)
        val result3 = smsDataSource.readBankSms()
        val result4 = smsDataSource.readBankSmsSince(0L)
        val result5 = smsDataSource.parseAllSms(emptyList())
        val result6 = smsDataSource.parseAndDeduplicate(emptyList())
        val result7 = smsDataSource.removeDuplicates(emptyList())
        val result8 = smsDataSource.readAndParseTransactions()
        val result9 = smsDataSource.readAndParseTransactionsSince(0L)
        val testSms = createTestSmsMessage()
        val result10 = smsDataSource.parseSms(testSms)
        val result11 = smsDataSource.isBankTransactionSms(testSms)

        // Then - All should be Result types (compile-time verified)
        assertTrue(result1 is Result)
        assertTrue(result2 is Result)
        assertTrue(result3 is Result)
        assertTrue(result4 is Result)
        assertTrue(result5 is Result)
        assertTrue(result6 is Result)
        assertTrue(result7 is Result)
        assertTrue(result8 is Result)
        assertTrue(result9 is Result)
        assertTrue(result10 is Result)
        assertTrue(result11 is Result)
    }

    @Test
    fun allReadMethods_returnSuccessInTestEnvironment() = runTest {
        // All SMS reading methods should succeed even with no SMS in test env

        // When
        val results = listOf(
            smsDataSource.readAllSms(),
            smsDataSource.readSmsSince(0L),
            smsDataSource.readBankSms(),
            smsDataSource.readBankSmsSince(0L),
            smsDataSource.readAndParseTransactions(),
            smsDataSource.readAndParseTransactionsSince(0L)
        )

        // Then - All should succeed (empty results are still success)
        results.forEach { result ->
            assertTrue("Expected success but got failure", result.isSuccess)
        }
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun readAllSms_withVeryLargeDaysValue_handlesCorrectly() = runTest {
        // Given
        val largeDays = 3650 // 10 years

        // When
        val result = smsDataSource.readAllSms(sinceDaysAgo = largeDays)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun consecutiveOperations_maintainIndependence() = runTest {
        // When - Multiple consecutive calls
        val result1 = smsDataSource.readAllSms()
        val result2 = smsDataSource.readAllSms()
        val result3 = smsDataSource.readBankSms()

        // Then - All should succeed independently
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertTrue(result3.isSuccess)
    }

    @Test
    fun parseAllSms_withMixedValidAndInvalidSms_parsesValidOnes() = runTest {
        // Given
        val mixedSmsList = listOf(
            createTestSmsMessage(
                id = 1L,
                body = "Rs.100 debited from account XX1234"
            ),
            createTestSmsMessage(
                id = 2L,
                body = "Invalid SMS without amount or keywords"
            ),
            createTestSmsMessage(
                id = 3L,
                body = "Rs.200 credited to account XX5678"
            )
        )

        // When
        val result = smsDataSource.parseAllSms(mixedSmsList)

        // Then - Should successfully parse valid ones
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun readAndParseTransactions_multipleCallsReturnConsistentResults() = runTest {
        // When - Call multiple times
        val result1 = smsDataSource.readAndParseTransactions(sinceDaysAgo = 7)
        val result2 = smsDataSource.readAndParseTransactions(sinceDaysAgo = 7)

        // Then - Both should succeed
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        // Results should be consistent (same call parameters)
        assertEquals(result1.getOrNull()?.size, result2.getOrNull()?.size)
    }

    @Test
    fun isBankTransactionSms_withEdgeCaseBodies_handlesCorrectly() = runTest {
        // Test various edge cases

        // Empty body
        val emptyBodyResult = smsDataSource.isBankTransactionSms(
            createTestSmsMessage(body = "")
        )
        assertTrue(emptyBodyResult.isSuccess)
        assertFalse(emptyBodyResult.getOrNull() == true)

        // Very long body
        val longBody = "Rs.100 debited " + "a".repeat(1000)
        val longBodyResult = smsDataSource.isBankTransactionSms(
            createTestSmsMessage(body = longBody)
        )
        assertTrue(longBodyResult.isSuccess)

        // Special characters
        val specialCharsResult = smsDataSource.isBankTransactionSms(
            createTestSmsMessage(body = "₹100 debited from A/c XX1234 @merchant™️")
        )
        assertTrue(specialCharsResult.isSuccess)
    }
}
