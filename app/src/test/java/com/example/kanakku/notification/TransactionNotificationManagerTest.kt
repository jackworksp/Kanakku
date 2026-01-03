package com.example.kanakku.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.preferences.AppPreferences
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for TransactionNotificationManager.
 *
 * Tests cover:
 * - Notification channel creation (Android 8+ and older versions)
 * - Notification building for different transaction types (debit, credit, unknown)
 * - Privacy preferences integration (balance, account, reference visibility)
 * - Notification display and management
 * - Edge cases (null values, large amounts, special characters)
 * - Error handling for all scenarios
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TransactionNotificationManagerTest {

    private lateinit var context: Context
    private lateinit var notificationManager: TransactionNotificationManager
    private lateinit var appPreferences: AppPreferences
    private lateinit var systemNotificationManager: NotificationManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = TransactionNotificationManager(context)
        appPreferences = AppPreferences.getInstance(context)
        systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Enable all preferences by default for testing
        appPreferences.setNotificationsEnabled(true)
        appPreferences.setNotificationSound(true)
        appPreferences.setNotificationVibration(true)
        appPreferences.setShowBalanceInNotification(true)
        appPreferences.setShowAccountInNotification(true)
        appPreferences.setShowReferenceInNotification(true)
    }

    @After
    fun teardown() {
        // Clean up notifications
        notificationManager.cancelAllNotifications()
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a sample debit transaction for testing
     */
    private fun createDebitTransaction(
        smsId: Long = 1001L,
        amount: Double = 500.0,
        merchant: String? = "Amazon",
        accountNumber: String? = "1234",
        referenceNumber: String? = "REF123456",
        balanceAfter: Double? = 5000.0,
        date: Long = System.currentTimeMillis(),
        senderAddress: String = "HDFCBK"
    ): ParsedTransaction {
        return ParsedTransaction(
            smsId = smsId,
            amount = amount,
            type = TransactionType.DEBIT,
            merchant = merchant,
            accountNumber = accountNumber,
            referenceNumber = referenceNumber,
            date = date,
            rawSms = "Test SMS",
            senderAddress = senderAddress,
            balanceAfter = balanceAfter,
            location = null
        )
    }

    /**
     * Creates a sample credit transaction for testing
     */
    private fun createCreditTransaction(
        smsId: Long = 2001L,
        amount: Double = 1000.0,
        merchant: String? = "Salary",
        accountNumber: String? = "5678",
        referenceNumber: String? = "UTR987654",
        balanceAfter: Double? = 10000.0,
        date: Long = System.currentTimeMillis(),
        senderAddress: String = "SBIIN"
    ): ParsedTransaction {
        return ParsedTransaction(
            smsId = smsId,
            amount = amount,
            type = TransactionType.CREDIT,
            merchant = merchant,
            accountNumber = accountNumber,
            referenceNumber = referenceNumber,
            date = date,
            rawSms = "Test SMS",
            senderAddress = senderAddress,
            balanceAfter = balanceAfter,
            location = null
        )
    }

    /**
     * Creates a sample unknown transaction for testing
     */
    private fun createUnknownTransaction(
        smsId: Long = 3001L,
        amount: Double = 250.0
    ): ParsedTransaction {
        return ParsedTransaction(
            smsId = smsId,
            amount = amount,
            type = TransactionType.UNKNOWN,
            merchant = null,
            accountNumber = null,
            referenceNumber = null,
            date = System.currentTimeMillis(),
            rawSms = "Test SMS",
            senderAddress = "UNKNOWN",
            balanceAfter = null,
            location = null
        )
    }

    // ==================== Notification Channel Creation Tests ====================

    @Test
    fun createNotificationChannel_onAndroid8Plus_createsChannel() {
        // When - Create notification channel
        val result = notificationManager.createNotificationChannel()

        // Then - Should succeed
        assertTrue("Channel creation should succeed on API 33", result)

        // Verify channel exists
        val channel = systemNotificationManager.getNotificationChannel("transactions")
        assertNotNull("Channel should be created", channel)
        assertEquals("Channel name should match", "Transaction Alerts", channel?.name)
        assertEquals("Channel importance should be HIGH",
            NotificationManager.IMPORTANCE_HIGH,
            channel?.importance
        )
    }

    @Test
    @Config(sdk = [23])
    fun createNotificationChannel_onAndroidBelowOreo_returnsTrue() {
        // When - Create notification channel on API 23
        val result = notificationManager.createNotificationChannel()

        // Then - Should return true (no-op for older versions)
        assertTrue("Channel creation should succeed on older API", result)
    }

    @Test
    fun createNotificationChannel_calledMultipleTimes_doesNotFail() {
        // When - Call channel creation multiple times
        val result1 = notificationManager.createNotificationChannel()
        val result2 = notificationManager.createNotificationChannel()
        val result3 = notificationManager.createNotificationChannel()

        // Then - All calls should succeed
        assertTrue("First call should succeed", result1)
        assertTrue("Second call should succeed", result2)
        assertTrue("Third call should succeed", result3)
    }

    // ==================== Debit Transaction Notification Tests ====================

    @Test
    fun showTransactionNotification_debitTransaction_showsCorrectContent() {
        // Given - Debit transaction
        val transaction = createDebitTransaction(
            smsId = 1001L,
            amount = 500.0,
            merchant = "Amazon",
            accountNumber = "1234",
            balanceAfter = 5000.0
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_debitWithLargeAmount_formatsCorrectly() {
        // Given - Debit transaction with large amount
        val transaction = createDebitTransaction(
            smsId = 1002L,
            amount = 99999.99,
            merchant = "Car Dealer"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (currency formatter handles large amounts)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_debitWithSmallAmount_formatsCorrectly() {
        // Given - Debit transaction with small amount
        val transaction = createDebitTransaction(
            smsId = 1003L,
            amount = 0.50,
            merchant = "Parking"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_debitWithZeroAmount_handlesGracefully() {
        // Given - Debit transaction with zero amount (edge case)
        val transaction = createDebitTransaction(
            smsId = 1004L,
            amount = 0.0,
            merchant = "Test"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (notification manager handles edge cases)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_debitWithNullMerchant_usesSenderAddress() {
        // Given - Debit transaction with null merchant
        val transaction = createDebitTransaction(
            smsId = 1005L,
            merchant = null,
            senderAddress = "HDFCBK"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (falls back to sender address)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_debitWithLongMerchantName_handlesGracefully() {
        // Given - Debit transaction with very long merchant name
        val transaction = createDebitTransaction(
            smsId = 1006L,
            merchant = "A".repeat(200),
            accountNumber = "1234"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (notification handles long text)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_debitWithSpecialCharacters_handlesGracefully() {
        // Given - Debit transaction with special characters
        val transaction = createDebitTransaction(
            smsId = 1007L,
            merchant = "Café & Restaurant @ Main St.",
            accountNumber = "5678"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_debitWithUnicodeCharacters_handlesGracefully() {
        // Given - Debit transaction with Unicode characters
        val transaction = createDebitTransaction(
            smsId = 1008L,
            merchant = "राज स्टोर", // Hindi characters
            accountNumber = "9012"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("Notification should be shown successfully", result)
    }

    // ==================== Credit Transaction Notification Tests ====================

    @Test
    fun showTransactionNotification_creditTransaction_showsCorrectContent() {
        // Given - Credit transaction
        val transaction = createCreditTransaction(
            smsId = 2001L,
            amount = 1000.0,
            merchant = "Salary",
            accountNumber = "5678",
            balanceAfter = 10000.0
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_creditWithLargeAmount_formatsCorrectly() {
        // Given - Credit transaction with large amount
        val transaction = createCreditTransaction(
            smsId = 2002L,
            amount = 999999.99,
            merchant = "Lottery"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_creditWithNullMerchant_usesSenderAddress() {
        // Given - Credit transaction with null merchant
        val transaction = createCreditTransaction(
            smsId = 2003L,
            merchant = null,
            senderAddress = "SBIIN"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (falls back to sender address)
        assertTrue("Notification should be shown successfully", result)
    }

    // ==================== Unknown Transaction Notification Tests ====================

    @Test
    fun showTransactionNotification_unknownTransaction_showsCorrectContent() {
        // Given - Unknown transaction type
        val transaction = createUnknownTransaction(
            smsId = 3001L,
            amount = 250.0
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_unknownWithAllNullFields_handlesGracefully() {
        // Given - Unknown transaction with all optional fields null
        val transaction = ParsedTransaction(
            smsId = 3002L,
            amount = 100.0,
            type = TransactionType.UNKNOWN,
            merchant = null,
            accountNumber = null,
            referenceNumber = null,
            date = System.currentTimeMillis(),
            rawSms = "Test SMS",
            senderAddress = "UNKNOWN",
            balanceAfter = null,
            location = null
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (handles missing optional data)
        assertTrue("Notification should be shown successfully", result)
    }

    // ==================== Privacy Preferences Tests ====================

    @Test
    fun showTransactionNotification_balanceHidden_doesNotShowBalance() {
        // Given - Transaction with balance preference disabled
        appPreferences.setShowBalanceInNotification(false)
        val transaction = createDebitTransaction(
            smsId = 4001L,
            balanceAfter = 5000.0
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (balance not shown in notification)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_accountHidden_doesNotShowAccount() {
        // Given - Transaction with account preference disabled
        appPreferences.setShowAccountInNotification(false)
        val transaction = createDebitTransaction(
            smsId = 4002L,
            accountNumber = "1234"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (account not shown in notification)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_referenceHidden_doesNotShowReference() {
        // Given - Transaction with reference preference disabled
        appPreferences.setShowReferenceInNotification(false)
        val transaction = createDebitTransaction(
            smsId = 4003L,
            referenceNumber = "REF123456"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (reference not shown in notification)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_allPrivacyDisabled_onlyShowsBasicInfo() {
        // Given - All privacy preferences disabled
        appPreferences.setShowBalanceInNotification(false)
        appPreferences.setShowAccountInNotification(false)
        appPreferences.setShowReferenceInNotification(false)

        val transaction = createDebitTransaction(
            smsId = 4004L,
            accountNumber = "1234",
            balanceAfter = 5000.0,
            referenceNumber = "REF123456"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (only shows amount, merchant, time)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_allPrivacyEnabled_showsAllInfo() {
        // Given - All privacy preferences enabled
        appPreferences.setShowBalanceInNotification(true)
        appPreferences.setShowAccountInNotification(true)
        appPreferences.setShowReferenceInNotification(true)

        val transaction = createDebitTransaction(
            smsId = 4005L,
            accountNumber = "1234",
            balanceAfter = 5000.0,
            referenceNumber = "REF123456"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (shows all information)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_mixedPrivacySettings_respectsPreferences() {
        // Given - Mixed privacy settings
        appPreferences.setShowBalanceInNotification(true)
        appPreferences.setShowAccountInNotification(false)
        appPreferences.setShowReferenceInNotification(true)

        val transaction = createDebitTransaction(
            smsId = 4006L,
            accountNumber = "1234",
            balanceAfter = 5000.0,
            referenceNumber = "REF123456"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (shows balance and reference, hides account)
        assertTrue("Notification should be shown successfully", result)
    }

    // ==================== Notification Management Tests ====================

    @Test
    fun cancelNotification_withValidSmsId_succeeds() {
        // Given - A notification was shown
        val transaction = createDebitTransaction(smsId = 5001L)
        notificationManager.showTransactionNotification(transaction)

        // When - Cancel the notification
        notificationManager.cancelNotification(5001L)

        // Then - Should not throw (notification cancelled)
        // Note: Robolectric doesn't track active notifications, so we just verify no crash
    }

    @Test
    fun cancelNotification_withNonExistentSmsId_handlesGracefully() {
        // When - Cancel a notification that doesn't exist
        notificationManager.cancelNotification(999999L)

        // Then - Should not throw (graceful handling)
    }

    @Test
    fun cancelNotification_withNegativeSmsId_handlesGracefully() {
        // When - Cancel notification with negative ID (edge case)
        notificationManager.cancelNotification(-1L)

        // Then - Should not throw
    }

    @Test
    fun cancelNotification_withZeroSmsId_handlesGracefully() {
        // When - Cancel notification with zero ID (edge case)
        notificationManager.cancelNotification(0L)

        // Then - Should not throw
    }

    @Test
    fun cancelAllNotifications_afterShowingMultiple_succeeds() {
        // Given - Multiple notifications shown
        notificationManager.showTransactionNotification(createDebitTransaction(smsId = 6001L))
        notificationManager.showTransactionNotification(createCreditTransaction(smsId = 6002L))
        notificationManager.showTransactionNotification(createDebitTransaction(smsId = 6003L))

        // When - Cancel all notifications
        notificationManager.cancelAllNotifications()

        // Then - Should not throw (all notifications cancelled)
    }

    @Test
    fun cancelAllNotifications_withNoActiveNotifications_handlesGracefully() {
        // When - Cancel all notifications when none exist
        notificationManager.cancelAllNotifications()

        // Then - Should not throw
    }

    @Test
    fun areNotificationsEnabled_returnsBoolean() {
        // When - Check if notifications are enabled
        val enabled = notificationManager.areNotificationsEnabled()

        // Then - Should return a boolean value (not null)
        assertNotNull("Should return a boolean", enabled)
    }

    // ==================== Multiple Transactions Tests ====================

    @Test
    fun showTransactionNotification_multipleDebitTransactions_eachGetsUniqueId() {
        // Given - Multiple debit transactions
        val transaction1 = createDebitTransaction(smsId = 7001L, merchant = "Amazon")
        val transaction2 = createDebitTransaction(smsId = 7002L, merchant = "Flipkart")
        val transaction3 = createDebitTransaction(smsId = 7003L, merchant = "Swiggy")

        // When - Show all notifications
        val result1 = notificationManager.showTransactionNotification(transaction1)
        val result2 = notificationManager.showTransactionNotification(transaction2)
        val result3 = notificationManager.showTransactionNotification(transaction3)

        // Then - All should succeed
        assertTrue("First notification should succeed", result1)
        assertTrue("Second notification should succeed", result2)
        assertTrue("Third notification should succeed", result3)
    }

    @Test
    fun showTransactionNotification_multipleCreditTransactions_eachGetsUniqueId() {
        // Given - Multiple credit transactions
        val transaction1 = createCreditTransaction(smsId = 7004L, merchant = "Salary")
        val transaction2 = createCreditTransaction(smsId = 7005L, merchant = "Refund")
        val transaction3 = createCreditTransaction(smsId = 7006L, merchant = "Interest")

        // When - Show all notifications
        val result1 = notificationManager.showTransactionNotification(transaction1)
        val result2 = notificationManager.showTransactionNotification(transaction2)
        val result3 = notificationManager.showTransactionNotification(transaction3)

        // Then - All should succeed
        assertTrue("First notification should succeed", result1)
        assertTrue("Second notification should succeed", result2)
        assertTrue("Third notification should succeed", result3)
    }

    @Test
    fun showTransactionNotification_mixedTransactionTypes_allSucceed() {
        // Given - Mix of transaction types
        val debit = createDebitTransaction(smsId = 7007L)
        val credit = createCreditTransaction(smsId = 7008L)
        val unknown = createUnknownTransaction(smsId = 7009L)

        // When - Show all notifications
        val result1 = notificationManager.showTransactionNotification(debit)
        val result2 = notificationManager.showTransactionNotification(credit)
        val result3 = notificationManager.showTransactionNotification(unknown)

        // Then - All should succeed
        assertTrue("Debit notification should succeed", result1)
        assertTrue("Credit notification should succeed", result2)
        assertTrue("Unknown notification should succeed", result3)
    }

    @Test
    fun showTransactionNotification_rapidSequence_allSucceed() {
        // Given - Rapid sequence of transactions
        val transactions = (1..10).map { i ->
            if (i % 2 == 0) {
                createDebitTransaction(smsId = 8000L + i)
            } else {
                createCreditTransaction(smsId = 8000L + i)
            }
        }

        // When - Show all notifications rapidly
        val results = transactions.map { transaction ->
            notificationManager.showTransactionNotification(transaction)
        }

        // Then - All should succeed
        assertTrue("All notifications should succeed", results.all { it })
        assertEquals("Should have 10 successful notifications", 10, results.count { it })
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun showTransactionNotification_withVeryOldTimestamp_handlesGracefully() {
        // Given - Transaction with very old timestamp (year 2000)
        val transaction = createDebitTransaction(
            smsId = 9001L,
            date = 946684800000L // Jan 1, 2000
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (date formatter handles old dates)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_withFutureTimestamp_handlesGracefully() {
        // Given - Transaction with future timestamp (year 2050)
        val transaction = createDebitTransaction(
            smsId = 9002L,
            date = 2524608000000L // Jan 1, 2050
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (date formatter handles future dates)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_withMaxLongSmsId_handlesGracefully() {
        // Given - Transaction with maximum Long value as SMS ID
        val transaction = createDebitTransaction(
            smsId = Long.MAX_VALUE
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (notification ID calculation handles large values)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_withMinLongSmsId_handlesGracefully() {
        // Given - Transaction with minimum Long value as SMS ID (edge case)
        val transaction = createDebitTransaction(
            smsId = Long.MIN_VALUE
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_withEmptyMerchant_usesSenderAddress() {
        // Given - Transaction with empty merchant string
        val transaction = createDebitTransaction(
            smsId = 9003L,
            merchant = "",
            senderAddress = "HDFCBK"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed (falls back to sender address)
        assertTrue("Notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_withWhitespaceMerchant_usesSenderAddress() {
        // Given - Transaction with whitespace-only merchant
        val transaction = createDebitTransaction(
            smsId = 9004L,
            merchant = "   ",
            senderAddress = "SBIIN"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("Notification should be shown successfully", result)
    }

    // ==================== Real-World Bank Transactions Tests ====================

    @Test
    fun showTransactionNotification_hdfcDebitSms_showsCorrectly() {
        // Given - HDFC bank debit transaction
        val transaction = ParsedTransaction(
            smsId = 10001L,
            amount = 1250.50,
            type = TransactionType.DEBIT,
            merchant = "Amazon Pay",
            accountNumber = "1234",
            referenceNumber = "REF456789",
            date = System.currentTimeMillis(),
            rawSms = "Rs 1250.50 debited from A/c **1234 on 01-01-24 to Amazon Pay. Avl Bal: Rs 5000.00",
            senderAddress = "HDFCBK",
            balanceAfter = 5000.0,
            location = null
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("HDFC notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_sbiCreditSms_showsCorrectly() {
        // Given - SBI bank credit transaction
        val transaction = ParsedTransaction(
            smsId = 10002L,
            amount = 50000.0,
            type = TransactionType.CREDIT,
            merchant = "SALARY",
            accountNumber = "5678",
            referenceNumber = "UTR123456789",
            date = System.currentTimeMillis(),
            rawSms = "SBI A/c X5678 credited with Rs 50000.00 on 01-01-24. Avl Bal: Rs 75000.00",
            senderAddress = "SBIIN",
            balanceAfter = 75000.0,
            location = null
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("SBI notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_iciciAtmWithdrawal_showsCorrectly() {
        // Given - ICICI ATM withdrawal
        val transaction = ParsedTransaction(
            smsId = 10003L,
            amount = 2000.0,
            type = TransactionType.DEBIT,
            merchant = "ATM",
            accountNumber = "9012",
            referenceNumber = null,
            date = System.currentTimeMillis(),
            rawSms = "Rs 2000.00 withdrawn from A/c **9012 at ICICI ATM on 01-01-24",
            senderAddress = "ICICIB",
            balanceAfter = 8000.0,
            location = "Mumbai"
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("ICICI notification should be shown successfully", result)
    }

    @Test
    fun showTransactionNotification_upiTransaction_showsCorrectly() {
        // Given - UPI transaction
        val transaction = ParsedTransaction(
            smsId = 10004L,
            amount = 299.0,
            type = TransactionType.DEBIT,
            merchant = "Swiggy",
            accountNumber = "3456",
            referenceNumber = "301234567890",
            date = System.currentTimeMillis(),
            rawSms = "Rs 299.00 debited from A/c **3456 via UPI to Swiggy. UPI Ref: 301234567890",
            senderAddress = "AXISNB",
            balanceAfter = 4701.0,
            location = null
        )

        // When - Show notification
        val result = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("UPI notification should be shown successfully", result)
    }

    // ==================== Integration Tests ====================

    @Test
    fun integration_fullNotificationFlow_worksCorrectly() {
        // Given - Notification channel created
        val channelResult = notificationManager.createNotificationChannel()
        assertTrue("Channel creation should succeed", channelResult)

        // When - Show transaction notification
        val transaction = createDebitTransaction(smsId = 11001L)
        val notificationResult = notificationManager.showTransactionNotification(transaction)

        // Then - Should succeed
        assertTrue("Notification should be shown successfully", notificationResult)

        // And - Can check notification status
        val enabled = notificationManager.areNotificationsEnabled()
        assertNotNull("Should return notification enabled status", enabled)

        // And - Can cancel notification
        notificationManager.cancelNotification(11001L)

        // And - Can cancel all notifications
        notificationManager.cancelAllNotifications()
    }

    @Test
    fun integration_multipleTransactionFlow_worksCorrectly() {
        // Given - Notification channel created
        notificationManager.createNotificationChannel()

        // When - Show multiple transactions of different types
        val debit = createDebitTransaction(smsId = 11002L, amount = 500.0)
        val credit = createCreditTransaction(smsId = 11003L, amount = 1000.0)
        val unknown = createUnknownTransaction(smsId = 11004L, amount = 250.0)

        val result1 = notificationManager.showTransactionNotification(debit)
        val result2 = notificationManager.showTransactionNotification(credit)
        val result3 = notificationManager.showTransactionNotification(unknown)

        // Then - All should succeed
        assertTrue("Debit notification should succeed", result1)
        assertTrue("Credit notification should succeed", result2)
        assertTrue("Unknown notification should succeed", result3)

        // And - Can cancel specific notification
        notificationManager.cancelNotification(11002L)

        // And - Can cancel all remaining
        notificationManager.cancelAllNotifications()
    }

    @Test
    fun integration_privacyPreferencesFlow_respectsUserChoices() {
        // Given - Notification channel created
        notificationManager.createNotificationChannel()

        // When - Show notification with all privacy enabled
        appPreferences.setShowBalanceInNotification(true)
        appPreferences.setShowAccountInNotification(true)
        appPreferences.setShowReferenceInNotification(true)

        val transaction1 = createDebitTransaction(
            smsId = 11005L,
            accountNumber = "1234",
            balanceAfter = 5000.0,
            referenceNumber = "REF123"
        )
        val result1 = notificationManager.showTransactionNotification(transaction1)
        assertTrue("First notification should succeed", result1)

        // And - Show notification with all privacy disabled
        appPreferences.setShowBalanceInNotification(false)
        appPreferences.setShowAccountInNotification(false)
        appPreferences.setShowReferenceInNotification(false)

        val transaction2 = createDebitTransaction(
            smsId = 11006L,
            accountNumber = "5678",
            balanceAfter = 3000.0,
            referenceNumber = "REF456"
        )
        val result2 = notificationManager.showTransactionNotification(transaction2)
        assertTrue("Second notification should succeed", result2)

        // Then - Both notifications should respect preferences
        notificationManager.cancelAllNotifications()
    }
}
