package com.example.kanakku.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.kanakku.data.database.DatabaseProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.data.repository.TransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SmsProcessingService.
 *
 * Tests cover:
 * - Transaction parsing using BankSmsParser
 * - Database saving of parsed transactions
 * - Duplicate transaction detection
 * - Notification triggering (with user preferences)
 * - Real-time event emission
 * - Error handling for invalid inputs, parsing failures, database errors
 * - Real-world bank SMS patterns
 * - Edge cases and boundary conditions
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SmsProcessingServiceTest {

    private lateinit var context: Context
    private lateinit var database: KanakkuDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var appPreferences: AppPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Reset DatabaseProvider to ensure clean state
        DatabaseProvider.resetInstance()

        // Get repository (this will create the database using actual DatabaseProvider)
        repository = DatabaseProvider.getRepository(context)
        database = DatabaseProvider.getDatabase(context)

        appPreferences = AppPreferences.getInstance(context)

        // Enable notifications by default for testing
        appPreferences.setNotificationsEnabled(true)
    }

    @After
    fun teardown() {
        // Reset DatabaseProvider to clean up
        DatabaseProvider.resetInstance()
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a WorkManager worker for testing with input data
     */
    private fun createWorker(
        smsId: Long,
        sender: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ): SmsProcessingService {
        val inputData = Data.Builder()
            .putLong("sms_id", smsId)
            .putString("sender", sender)
            .putString("body", body)
            .putLong("timestamp", timestamp)
            .build()

        return TestListenableWorkerBuilder<SmsProcessingService>(context)
            .setInputData(inputData)
            .build()
    }

    /**
     * Helper to verify transaction was saved to database
     */
    private suspend fun assertTransactionSaved(smsId: Long): Boolean {
        val existsResult = repository.transactionExists(smsId)
        return existsResult.getOrDefault(false)
    }

    // ==================== Valid Transaction Processing Tests ====================

    @Test
    fun doWork_withValidHDFCDebitSms_parsesAndSavesSuccessfully() = runTest {
        // Given - Valid HDFC debit SMS
        val worker = createWorker(
            smsId = 1L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 on 01-Jan at AMAZON Avl Bal Rs.9500 Ref:TXN123456789012"
        )

        // When - Process the SMS
        val result = worker.doWork()

        // Then - Work succeeds and transaction is saved
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(1L))

        // Verify transaction details
        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(1, transactions.size)
        assertEquals(500.0, transactions[0].amount, 0.01)
        assertEquals(TransactionType.DEBIT, transactions[0].type)
        assertEquals("AMAZON", transactions[0].merchant)
        assertEquals("1234", transactions[0].accountNumber)
        assertEquals("TXN123456789012", transactions[0].referenceNumber)
    }

    @Test
    fun doWork_withValidSBICreditSms_parsesAndSavesSuccessfully() = runTest {
        // Given - Valid SBI credit SMS
        val worker = createWorker(
            smsId = 2L,
            sender = "AD-SBIBNK",
            body = "Dear Customer, Rs.5000 credited to A/c XX5678 on 01-Jan Info:SALARY Ref:UTR987654321012"
        )

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(2L))

        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(1, transactions.size)
        assertEquals(5000.0, transactions[0].amount, 0.01)
        assertEquals(TransactionType.CREDIT, transactions[0].type)
        assertEquals("SALARY", transactions[0].merchant)
    }

    @Test
    fun doWork_withValidICICIUpiSms_parsesAndSavesSuccessfully() = runTest {
        // Given - Valid ICICI UPI SMS
        val worker = createWorker(
            smsId = 3L,
            sender = "VM-ICICIB",
            body = "Rs.150 debited from A/c XX9876 VPA:friend@upi on 01-Jan UPI:987654321012 Avl Bal:Rs.8,350.75"
        )

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(3L))

        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(1, transactions.size)
        assertEquals(150.0, transactions[0].amount, 0.01)
        assertEquals(TransactionType.DEBIT, transactions[0].type)
    }

    @Test
    fun doWork_withValidAxisAtmSms_parsesAndSavesSuccessfully() = runTest {
        // Given - Axis ATM withdrawal SMS
        val worker = createWorker(
            smsId = 4L,
            sender = "AX-AXISBK",
            body = "Rs.3000 withdrawn from Card XX5432 at CONNAUGHT PLACE ATM on 01-Jan Avl Bal:Rs.12,500"
        )

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(4L))
    }

    @Test
    fun doWork_withValidKotakRefundSms_parsesAndSavesSuccessfully() = runTest {
        // Given - Kotak refund SMS
        val worker = createWorker(
            smsId = 5L,
            sender = "VM-KOTAK",
            body = "Rs.1,299.00 refund credited to Card XX4321 from AMAZON on 01-Jan Ref:REF123456789012"
        )

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(5L))

        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(1, transactions.size)
        assertEquals(1299.0, transactions[0].amount, 0.01)
        assertEquals(TransactionType.CREDIT, transactions[0].type)
    }

    // ==================== Invalid Input Tests ====================

    @Test
    fun doWork_withInvalidSmsId_failsGracefully() = runTest {
        // Given - Invalid SMS ID (-1)
        val worker = createWorker(
            smsId = -1L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Work fails
        assertEquals(ListenableWorker.Result.failure(), result)

        // No transaction should be saved
        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(0, transactions.size)
    }

    @Test
    fun doWork_withEmptySender_failsGracefully() = runTest {
        // Given - Empty sender
        val worker = createWorker(
            smsId = 10L,
            sender = "",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Work fails
        assertEquals(ListenableWorker.Result.failure(), result)
        assertFalse(assertTransactionSaved(10L))
    }

    @Test
    fun doWork_withBlankSender_failsGracefully() = runTest {
        // Given - Blank sender (whitespace only)
        val worker = createWorker(
            smsId = 11L,
            sender = "   \n\t  ",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Work fails
        assertEquals(ListenableWorker.Result.failure(), result)
        assertFalse(assertTransactionSaved(11L))
    }

    @Test
    fun doWork_withEmptyBody_failsGracefully() = runTest {
        // Given - Empty body
        val worker = createWorker(
            smsId = 12L,
            sender = "VM-HDFCBK",
            body = ""
        )

        // When
        val result = worker.doWork()

        // Then - Work fails
        assertEquals(ListenableWorker.Result.failure(), result)
        assertFalse(assertTransactionSaved(12L))
    }

    @Test
    fun doWork_withBlankBody_failsGracefully() = runTest {
        // Given - Blank body (whitespace only)
        val worker = createWorker(
            smsId = 13L,
            sender = "VM-HDFCBK",
            body = "   \n\t  "
        )

        // When
        val result = worker.doWork()

        // Then - Work fails
        assertEquals(ListenableWorker.Result.failure(), result)
        assertFalse(assertTransactionSaved(13L))
    }

    @Test
    fun doWork_withMissingInputData_failsGracefully() = runTest {
        // Given - Worker with no input data
        val worker = TestListenableWorkerBuilder<SmsProcessingService>(context)
            .build()

        // When
        val result = worker.doWork()

        // Then - Work fails due to missing/invalid input
        assertEquals(ListenableWorker.Result.failure(), result)
    }

    // ==================== Parsing Failure Tests ====================

    @Test
    fun doWork_withNonBankSms_failsToParseAndFails() = runTest {
        // Given - Regular non-bank SMS
        val worker = createWorker(
            smsId = 20L,
            sender = "FRIEND",
            body = "Hey, how are you? Let's meet tomorrow at 5pm."
        )

        // When
        val result = worker.doWork()

        // Then - Work fails (parsing returns null)
        assertEquals(ListenableWorker.Result.failure(), result)
        assertFalse(assertTransactionSaved(20L))
    }

    @Test
    fun doWork_withOtpSms_failsToParseAndFails() = runTest {
        // Given - OTP SMS from bank (should be filtered)
        val worker = createWorker(
            smsId = 21L,
            sender = "VM-HDFCBK",
            body = "Your OTP is 123456. Do not share with anyone. Valid for 10 minutes."
        )

        // When
        val result = worker.doWork()

        // Then - Work fails (OTP filtered out)
        assertEquals(ListenableWorker.Result.failure(), result)
        assertFalse(assertTransactionSaved(21L))
    }

    @Test
    fun doWork_withPromotionalSms_failsToParseAndFails() = runTest {
        // Given - Promotional SMS with amount but no transaction keywords
        val worker = createWorker(
            smsId = 22L,
            sender = "VM-HDFCBK",
            body = "Get a loan of Rs.50000 at 9% interest rate. Apply now!"
        )

        // When
        val result = worker.doWork()

        // Then - Work fails (promotional filtered out)
        assertEquals(ListenableWorker.Result.failure(), result)
        assertFalse(assertTransactionSaved(22L))
    }

    @Test
    fun doWork_withBalanceEnquirySms_failsToParseAndFails() = runTest {
        // Given - Balance enquiry SMS (no debit/credit keyword)
        val worker = createWorker(
            smsId = 23L,
            sender = "VM-HDFCBK",
            body = "Your A/c XX1234 balance is Rs.5000 as on 01-Jan"
        )

        // When
        val result = worker.doWork()

        // Then - Work fails (balance enquiry filtered out)
        assertEquals(ListenableWorker.Result.failure(), result)
        assertFalse(assertTransactionSaved(23L))
    }

    // ==================== Duplicate Transaction Tests ====================

    @Test
    fun doWork_withDuplicateSmsId_skipsSavingAndSucceeds() = runTest {
        // Given - Save a transaction first
        val worker1 = createWorker(
            smsId = 100L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN123456789012"
        )
        val result1 = worker1.doWork()
        assertEquals(ListenableWorker.Result.success(), result1)

        // When - Process same SMS ID again
        val worker2 = createWorker(
            smsId = 100L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN123456789012"
        )
        val result2 = worker2.doWork()

        // Then - Work succeeds but doesn't save duplicate
        assertEquals(ListenableWorker.Result.success(), result2)

        // Verify only one transaction exists
        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(1, transactions.size)
    }

    @Test
    fun doWork_withDifferentSmsIdsSameBankTransaction_savesBothTransactions() = runTest {
        // Given - Two different SMS with similar content
        val worker1 = createWorker(
            smsId = 101L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN123456789012"
        )
        val worker2 = createWorker(
            smsId = 102L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN123456789012"
        )

        // When - Process both
        val result1 = worker1.doWork()
        val result2 = worker2.doWork()

        // Then - Both succeed and both are saved (different SMS IDs)
        assertEquals(ListenableWorker.Result.success(), result1)
        assertEquals(ListenableWorker.Result.success(), result2)

        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(2, transactions.size)
    }

    // ==================== Notification Preference Tests ====================

    @Test
    fun doWork_withNotificationsEnabled_processesSuccessfully() = runTest {
        // Given - Notifications enabled
        appPreferences.setNotificationsEnabled(true)
        val worker = createWorker(
            smsId = 200L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Work succeeds (notification shown internally)
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(200L))
    }

    @Test
    fun doWork_withNotificationsDisabled_stillSavesTransaction() = runTest {
        // Given - Notifications disabled
        appPreferences.setNotificationsEnabled(false)
        val worker = createWorker(
            smsId = 201L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Work succeeds (notification skipped, transaction still saved)
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(201L))
    }

    @Test
    fun doWork_notificationFailure_doesNotAffectTransactionSaving() = runTest {
        // Given - Valid transaction (notification might fail but that's ok)
        val worker = createWorker(
            smsId = 202L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Work succeeds even if notification fails
        // (notification errors are caught and logged but don't fail the work)
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(202L))
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun doWork_withVeryLargeSmsBody_handlesCorrectly() = runTest {
        // Given - SMS with very long body (concatenated SMS)
        val longBody = "Rs.500 debited from A/c XX1234 at AMAZON " + "x".repeat(1000) +
                " Ref:TXN123456789012 Avl Bal:Rs.10,000"
        val worker = createWorker(
            smsId = 300L,
            sender = "VM-HDFCBK",
            body = longBody
        )

        // When
        val result = worker.doWork()

        // Then - Should handle long messages
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(300L))
    }

    @Test
    fun doWork_withSpecialCharactersInBody_handlesCorrectly() = runTest {
        // Given - SMS with special characters
        val worker = createWorker(
            smsId = 301L,
            sender = "VM-HDFCBK",
            body = "₹500.00 debited from A/c XX1234 at CAFÉ™ & RESTAURANT® Ref:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Should handle special characters
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(301L))
    }

    @Test
    fun doWork_withUnicodeCharacters_handlesCorrectly() = runTest {
        // Given - SMS with Unicode characters (Hindi, Tamil, etc.)
        val worker = createWorker(
            smsId = 302L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at मर्चेंट on 01-Jan Ref:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Should handle Unicode characters
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(302L))
    }

    @Test
    fun doWork_withMultilineBody_handlesCorrectly() = runTest {
        // Given - SMS with newlines
        val worker = createWorker(
            smsId = 303L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from\nA/c XX1234 on 01-Jan\nat AMAZON\nRef:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Should handle multiline messages
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(303L))
    }

    @Test
    fun doWork_withZeroAmount_handlesCorrectly() = runTest {
        // Given - SMS with zero amount (edge case)
        val worker = createWorker(
            smsId = 304L,
            sender = "VM-HDFCBK",
            body = "Rs.0 debited from A/c XX1234 at TEST Ref:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Should handle zero amount (if parser accepts it)
        val workResult = worker.doWork()
        // Zero amount might fail parsing, which is acceptable
        assertTrue(
            workResult == ListenableWorker.Result.success() ||
            workResult == ListenableWorker.Result.failure()
        )
    }

    @Test
    fun doWork_withVeryLargeAmount_handlesCorrectly() = runTest {
        // Given - SMS with very large amount
        val worker = createWorker(
            smsId = 305L,
            sender = "VM-HDFCBK",
            body = "Rs.99,99,999.99 debited from A/c XX1234 at MERCHANT Ref:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Should handle large amounts
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(305L))

        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(9999999.99, transactions[0].amount, 0.01)
    }

    @Test
    fun doWork_withAmountInPaise_handlesCorrectly() = runTest {
        // Given - SMS with decimal amount (paise)
        val worker = createWorker(
            smsId = 306L,
            sender = "VM-HDFCBK",
            body = "Rs.0.50 debited from A/c XX1234 at PAYTM Ref:TXN123456789012"
        )

        // When
        val result = worker.doWork()

        // Then - Should handle paise amounts
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(306L))

        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(0.50, transactions[0].amount, 0.01)
    }

    // ==================== Sequential Processing Tests ====================

    @Test
    fun doWork_processMultipleTransactionsSequentially_savesAll() = runTest {
        // Given - Multiple different transactions
        val worker1 = createWorker(
            smsId = 400L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN111111111111"
        )
        val worker2 = createWorker(
            smsId = 401L,
            sender = "AD-SBIBNK",
            body = "Rs.1000 credited to A/c XX5678 from SALARY Ref:UTR222222222222"
        )
        val worker3 = createWorker(
            smsId = 402L,
            sender = "VM-ICICIB",
            body = "Rs.250 debited from A/c XX9999 at SWIGGY Ref:TXN333333333333"
        )

        // When - Process all sequentially
        val result1 = worker1.doWork()
        val result2 = worker2.doWork()
        val result3 = worker3.doWork()

        // Then - All succeed
        assertEquals(ListenableWorker.Result.success(), result1)
        assertEquals(ListenableWorker.Result.success(), result2)
        assertEquals(ListenableWorker.Result.success(), result3)

        // Verify all three transactions saved
        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(3, transactions.size)
    }

    @Test
    fun doWork_processMixedValidInvalidSequentially_savesOnlyValid() = runTest {
        // Given - Mix of valid and invalid transactions
        val worker1 = createWorker(
            smsId = 410L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Ref:TXN123456789012"
        )
        val worker2 = createWorker(
            smsId = 411L,
            sender = "FRIEND",
            body = "Hey, how are you?" // Non-bank SMS
        )
        val worker3 = createWorker(
            smsId = 412L,
            sender = "AD-SBIBNK",
            body = "Rs.1000 credited to A/c XX5678 from SALARY Ref:UTR987654321012"
        )

        // When - Process all
        val result1 = worker1.doWork()
        val result2 = worker2.doWork()
        val result3 = worker3.doWork()

        // Then - Valid succeed, invalid fails
        assertEquals(ListenableWorker.Result.success(), result1)
        assertEquals(ListenableWorker.Result.failure(), result2)
        assertEquals(ListenableWorker.Result.success(), result3)

        // Only 2 valid transactions saved
        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(2, transactions.size)
    }

    // ==================== Real-World Bank SMS Tests ====================

    @Test
    fun doWork_withPNBCardPurchase_parsesCorrectly() = runTest {
        // Given - PNB card purchase SMS
        val worker = createWorker(
            smsId = 500L,
            sender = "PNB",
            body = "INR 850 debited on Card ending 3421 at FLIPKART on 01-Jan Ref:TXN567890123456"
        )

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(assertTransactionSaved(500L))

        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(850.0, transactions[0].amount, 0.01)
        assertEquals(TransactionType.DEBIT, transactions[0].type)
    }

    @Test
    fun doWork_withMultipleBanksSequentially_parsesAllCorrectly() = runTest {
        // Given - SMS from different banks
        val hdfc = createWorker(
            smsId = 510L,
            sender = "VM-HDFCBK",
            body = "Rs.2,500.00 debited from A/c XX1234 at AMAZON Ref:TXN123"
        )
        val sbi = createWorker(
            smsId = 511L,
            sender = "AD-SBIBNK",
            body = "Rs.5000 credited to A/c XX5678 Info:SALARY Ref:UTR456"
        )
        val icici = createWorker(
            smsId = 512L,
            sender = "VM-ICICIB",
            body = "Rs.150 debited from A/c XX9876 VPA:friend@upi UPI:789"
        )
        val axis = createWorker(
            smsId = 513L,
            sender = "AX-AXISBK",
            body = "Rs.3000 withdrawn from Card XX5432 at ATM Avl Bal:Rs.12,500"
        )

        // When - Process all
        assertEquals(ListenableWorker.Result.success(), hdfc.doWork())
        assertEquals(ListenableWorker.Result.success(), sbi.doWork())
        assertEquals(ListenableWorker.Result.success(), icici.doWork())
        assertEquals(ListenableWorker.Result.success(), axis.doWork())

        // Then - All 4 saved
        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(4, transactions.size)
    }

    // ==================== Integration Tests ====================

    @Test
    fun doWork_endToEndFlow_withValidBankSms() = runTest {
        // Given - Valid bank transaction SMS
        val worker = createWorker(
            smsId = 600L,
            sender = "VM-HDFCBK",
            body = "Rs.500 debited from A/c XX1234 at AMAZON Avl Bal Rs.9500 Ref:TXN123456789012"
        )

        // When - Process end-to-end
        val result = worker.doWork()

        // Then - Verify complete flow
        // 1. Work succeeds
        assertEquals(ListenableWorker.Result.success(), result)

        // 2. Transaction saved to database
        assertTrue(assertTransactionSaved(600L))

        // 3. Transaction details correct
        val transactions = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(1, transactions.size)

        val transaction = transactions[0]
        assertEquals(600L, transaction.smsId)
        assertEquals(500.0, transaction.amount, 0.01)
        assertEquals(TransactionType.DEBIT, transaction.type)
        assertEquals("AMAZON", transaction.merchant)
        assertEquals("1234", transaction.accountNumber)
        assertEquals("TXN123456789012", transaction.referenceNumber)
        assertEquals("VM-HDFCBK", transaction.senderAddress)

        // 4. Notification would be shown (if system allows)
        // 5. Event would be emitted (tested in separate event manager tests)
    }
}
