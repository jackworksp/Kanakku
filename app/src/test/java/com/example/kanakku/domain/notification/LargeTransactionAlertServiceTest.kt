package com.example.kanakku.domain.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.model.LargeTransactionSettings
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.preferences.AppPreferences
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for LargeTransactionAlertService detection logic.
 *
 * Tests cover:
 * - Large transaction threshold detection
 * - Alert tracking and duplicate prevention
 * - DEBIT vs CREDIT transaction filtering
 * - Transaction hash generation and uniqueness
 * - Notification creation and sending
 * - Alert clearing and reset logic
 * - Batch checking multiple transactions
 * - Edge cases and integration scenarios
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LargeTransactionAlertServiceTest {

    private lateinit var context: Context
    private lateinit var appPreferences: AppPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Reset and initialize AppPreferences
        AppPreferences.resetInstance()
        appPreferences = AppPreferences.getInstance(context)
        appPreferences.clearAll()

        // Enable large transaction alerts by default with threshold of ₹5,000
        appPreferences.setLargeTransactionSettings(
            LargeTransactionSettings(
                enabled = true,
                threshold = 5000.0
            )
        )
    }

    @After
    fun teardown() {
        appPreferences.clearAll()
        AppPreferences.resetInstance()
    }

    // ==================== Helper Functions ====================

    private fun createTestTransaction(
        amount: Double,
        merchant: String = "Test Merchant",
        type: TransactionType = TransactionType.DEBIT,
        date: Long = System.currentTimeMillis(),
        categoryId: String = "food",
        smsId: String? = "12345"
    ): ParsedTransaction {
        return ParsedTransaction(
            amount = amount,
            merchant = merchant,
            type = type,
            date = date,
            categoryId = categoryId,
            smsId = smsId,
            accountNumber = "1234",
            isIncome = type == TransactionType.CREDIT
        )
    }

    // ==================== Basic Alert Check Tests ====================

    @Test
    fun checkLargeTransactionAlert_whenAlertsDisabled_returnsFalse() = runTest {
        // Given: Large transaction alerts disabled
        appPreferences.setLargeTransactionSettings(
            LargeTransactionSettings(enabled = false, threshold = 5000.0)
        )
        val transaction = createTestTransaction(amount = 10000.0)

        // When: Check large transaction alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Returns false (no alert sent)
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_whenBelowThreshold_returnsFalse() = runTest {
        // Given: Transaction below threshold (₹3,000 < ₹5,000)
        val transaction = createTestTransaction(amount = 3000.0)

        // When: Check large transaction alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Returns false (no alert sent)
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_whenCreditTransaction_returnsFalse() = runTest {
        // Given: CREDIT transaction (income) exceeding threshold
        val transaction = createTestTransaction(
            amount = 10000.0,
            type = TransactionType.CREDIT
        )

        // When: Check large transaction alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Returns false (no alert for CREDIT transactions)
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    // ==================== Threshold Detection Tests ====================

    @Test
    fun checkLargeTransactionAlert_whenExactlyAtThreshold_sendsAlert() = runTest {
        // Given: Transaction exactly at threshold (₹5,000 = ₹5,000)
        val transaction = createTestTransaction(amount = 5000.0)

        // When: Check large transaction alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Alert sent successfully
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())

        // Verify alert tracking
        assertTrue(
            LargeTransactionAlertService.hasAlertBeenSent(transaction, appPreferences)
        )
    }

    @Test
    fun checkLargeTransactionAlert_whenAboveThreshold_sendsAlert() = runTest {
        // Given: Transaction above threshold (₹10,000 > ₹5,000)
        val transaction = createTestTransaction(amount = 10000.0)

        // When: Check large transaction alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Alert sent successfully
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())

        // Verify alert tracking
        assertTrue(
            LargeTransactionAlertService.hasAlertBeenSent(transaction, appPreferences)
        )
    }

    @Test
    fun checkLargeTransactionAlert_whenJustBelowThreshold_returnsFalse() = runTest {
        // Given: Transaction just below threshold (₹4,999.99 < ₹5,000)
        val transaction = createTestTransaction(amount = 4999.99)

        // When: Check large transaction alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Returns false (no alert sent)
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())

        // Verify no alert tracking
        assertFalse(
            LargeTransactionAlertService.hasAlertBeenSent(transaction, appPreferences)
        )
    }

    // ==================== Alert Tracking Tests ====================

    @Test
    fun checkLargeTransactionAlert_whenDuplicate_preventsDuplicateAlert() = runTest {
        // Given: Transaction above threshold
        val transaction = createTestTransaction(amount = 10000.0)

        // When: Check alert first time
        val result1 = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: First check sends alert
        assertTrue(result1.isSuccess)
        assertEquals(true, result1.getOrNull())

        // When: Check alert second time (same transaction)
        val result2 = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Second check returns false (duplicate prevented)
        assertTrue(result2.isSuccess)
        assertEquals(false, result2.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_afterClearAlert_allowsResending() = runTest {
        // Given: Transaction with alert already sent
        val transaction = createTestTransaction(amount = 10000.0)
        LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // When: Clear the alert
        LargeTransactionAlertService.clearAlert(transaction, appPreferences)

        // Then: Can send alert again
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun clearAllAlerts_removesAllAlertTracking() = runTest {
        // Given: Multiple transactions with alerts sent
        val tx1 = createTestTransaction(amount = 6000.0, merchant = "Merchant 1")
        val tx2 = createTestTransaction(amount = 7000.0, merchant = "Merchant 2")
        val tx3 = createTestTransaction(amount = 8000.0, merchant = "Merchant 3")

        LargeTransactionAlertService.checkLargeTransactionAlert(context, tx1, appPreferences)
        LargeTransactionAlertService.checkLargeTransactionAlert(context, tx2, appPreferences)
        LargeTransactionAlertService.checkLargeTransactionAlert(context, tx3, appPreferences)

        // Verify all alerts sent
        assertTrue(LargeTransactionAlertService.hasAlertBeenSent(tx1, appPreferences))
        assertTrue(LargeTransactionAlertService.hasAlertBeenSent(tx2, appPreferences))
        assertTrue(LargeTransactionAlertService.hasAlertBeenSent(tx3, appPreferences))

        // When: Clear all alerts
        val clearedCount = LargeTransactionAlertService.clearAllAlerts(appPreferences)

        // Then: All alerts cleared
        assertEquals(3, clearedCount)
        assertFalse(LargeTransactionAlertService.hasAlertBeenSent(tx1, appPreferences))
        assertFalse(LargeTransactionAlertService.hasAlertBeenSent(tx2, appPreferences))
        assertFalse(LargeTransactionAlertService.hasAlertBeenSent(tx3, appPreferences))
    }

    // ==================== Transaction Type Filtering Tests ====================

    @Test
    fun checkLargeTransactionAlert_onlyAlertsOnDebitTransactions() = runTest {
        // Given: DEBIT and CREDIT transactions both above threshold
        val debitTx = createTestTransaction(
            amount = 10000.0,
            type = TransactionType.DEBIT,
            merchant = "Debit Merchant"
        )
        val creditTx = createTestTransaction(
            amount = 10000.0,
            type = TransactionType.CREDIT,
            merchant = "Credit Merchant"
        )

        // When: Check both transactions
        val debitResult = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = debitTx,
            appPreferences = appPreferences
        )
        val creditResult = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = creditTx,
            appPreferences = appPreferences
        )

        // Then: Only DEBIT transaction triggers alert
        assertTrue(debitResult.isSuccess)
        assertEquals(true, debitResult.getOrNull())

        assertTrue(creditResult.isSuccess)
        assertEquals(false, creditResult.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_creditTransactionNeverSendsAlert() = runTest {
        // Given: Very large CREDIT transaction
        val transaction = createTestTransaction(
            amount = 1000000.0,  // ₹1,000,000 (well above threshold)
            type = TransactionType.CREDIT
        )

        // When: Check alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: No alert sent (income should not trigger fraud alerts)
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
        assertFalse(
            LargeTransactionAlertService.hasAlertBeenSent(transaction, appPreferences)
        )
    }

    // ==================== Threshold Configuration Tests ====================

    @Test
    fun checkLargeTransactionAlert_respectsUserThreshold() = runTest {
        // Given: Custom threshold of ₹10,000
        appPreferences.setLargeTransactionSettings(
            LargeTransactionSettings(enabled = true, threshold = 10000.0)
        )
        val transaction = createTestTransaction(amount = 8000.0)

        // When: Check alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: No alert (₹8,000 < ₹10,000)
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_withLowThreshold_sendsAlertForSmallAmount() = runTest {
        // Given: Low threshold of ₹1,000
        appPreferences.setLargeTransactionSettings(
            LargeTransactionSettings(enabled = true, threshold = 1000.0)
        )
        val transaction = createTestTransaction(amount = 1500.0)

        // When: Check alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Alert sent (₹1,500 >= ₹1,000)
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_withHighThreshold_requiresLargeAmount() = runTest {
        // Given: High threshold of ₹100,000
        appPreferences.setLargeTransactionSettings(
            LargeTransactionSettings(enabled = true, threshold = 100000.0)
        )
        val transaction = createTestTransaction(amount = 50000.0)

        // When: Check alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: No alert (₹50,000 < ₹100,000)
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    // ==================== Batch Checking Tests ====================

    @Test
    fun checkLargeTransactionAlerts_checksMultipleTransactions() = runTest {
        // Given: Multiple transactions, some above threshold, some below
        val transactions = listOf(
            createTestTransaction(amount = 10000.0, merchant = "Merchant 1"),  // Above
            createTestTransaction(amount = 3000.0, merchant = "Merchant 2"),   // Below
            createTestTransaction(amount = 7500.0, merchant = "Merchant 3"),   // Above
            createTestTransaction(amount = 4000.0, merchant = "Merchant 4"),   // Below
            createTestTransaction(amount = 15000.0, merchant = "Merchant 5")   // Above
        )

        // When: Check all transactions
        val result = LargeTransactionAlertService.checkLargeTransactionAlerts(
            context = context,
            transactions = transactions,
            appPreferences = appPreferences
        )

        // Then: 3 alerts sent (for transactions above ₹5,000)
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlerts_emptyList_returnsZero() = runTest {
        // Given: Empty transaction list
        val transactions = emptyList<ParsedTransaction>()

        // When: Check transactions
        val result = LargeTransactionAlertService.checkLargeTransactionAlerts(
            context = context,
            transactions = transactions,
            appPreferences = appPreferences
        )

        // Then: Returns zero alerts
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlerts_allBelowThreshold_returnsZero() = runTest {
        // Given: All transactions below threshold
        val transactions = listOf(
            createTestTransaction(amount = 1000.0, merchant = "Merchant 1"),
            createTestTransaction(amount = 2000.0, merchant = "Merchant 2"),
            createTestTransaction(amount = 3000.0, merchant = "Merchant 3")
        )

        // When: Check transactions
        val result = LargeTransactionAlertService.checkLargeTransactionAlerts(
            context = context,
            transactions = transactions,
            appPreferences = appPreferences
        )

        // Then: No alerts sent
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlerts_withDuplicates_preventsMultipleAlerts() = runTest {
        // Given: Same transaction multiple times
        val transaction = createTestTransaction(amount = 10000.0, merchant = "Test")
        val transactions = listOf(transaction, transaction, transaction)

        // When: Check all (duplicate) transactions
        val result = LargeTransactionAlertService.checkLargeTransactionAlerts(
            context = context,
            transactions = transactions,
            appPreferences = appPreferences
        )

        // Then: Only 1 alert sent (duplicates prevented)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    // ==================== Transaction Hash Tests ====================

    @Test
    fun hasAlertBeenSent_differentTransactions_returnsFalseForNew() = runTest {
        // Given: One transaction with alert sent
        val tx1 = createTestTransaction(amount = 10000.0, merchant = "Merchant 1")
        val tx2 = createTestTransaction(amount = 10000.0, merchant = "Merchant 2")

        LargeTransactionAlertService.checkLargeTransactionAlert(context, tx1, appPreferences)

        // When: Check if alerts sent
        val tx1Sent = LargeTransactionAlertService.hasAlertBeenSent(tx1, appPreferences)
        val tx2Sent = LargeTransactionAlertService.hasAlertBeenSent(tx2, appPreferences)

        // Then: Only tx1 has alert sent
        assertTrue(tx1Sent)
        assertFalse(tx2Sent)
    }

    @Test
    fun hasAlertBeenSent_sameTransactionAttributes_detectsDuplicate() = runTest {
        // Given: Two transactions with same attributes
        val tx1 = createTestTransaction(
            amount = 10000.0,
            merchant = "Test Merchant",
            date = 1234567890000L,
            smsId = "12345"
        )
        val tx2 = createTestTransaction(
            amount = 10000.0,
            merchant = "Test Merchant",
            date = 1234567890000L,
            smsId = "12345"
        )

        // When: Send alert for first transaction
        LargeTransactionAlertService.checkLargeTransactionAlert(context, tx1, appPreferences)

        // Then: Second transaction recognized as duplicate
        assertTrue(LargeTransactionAlertService.hasAlertBeenSent(tx2, appPreferences))
    }

    @Test
    fun hasAlertBeenSent_differentAmount_treatsSeparately() = runTest {
        // Given: Same merchant, different amounts
        val tx1 = createTestTransaction(amount = 10000.0, merchant = "Test", smsId = "1")
        val tx2 = createTestTransaction(amount = 15000.0, merchant = "Test", smsId = "2")

        // When: Send alert for first
        LargeTransactionAlertService.checkLargeTransactionAlert(context, tx1, appPreferences)

        // Then: Second treated as separate (different amount)
        assertFalse(LargeTransactionAlertService.hasAlertBeenSent(tx2, appPreferences))
    }

    // ==================== Clear Old Alerts Tests ====================

    @Test
    fun clearOldAlerts_removesAllAlerts() = runTest {
        // Given: Multiple transactions with alerts
        val tx1 = createTestTransaction(amount = 10000.0, merchant = "Old", date = 1000000000000L)
        val tx2 = createTestTransaction(amount = 10000.0, merchant = "New", date = System.currentTimeMillis())

        LargeTransactionAlertService.checkLargeTransactionAlert(context, tx1, appPreferences)
        LargeTransactionAlertService.checkLargeTransactionAlert(context, tx2, appPreferences)

        // When: Clear old alerts (30 days ago)
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val clearedCount = LargeTransactionAlertService.clearOldAlerts(appPreferences, thirtyDaysAgo)

        // Then: All alerts cleared (method clears all since hash doesn't contain date)
        assertEquals(2, clearedCount)
        assertFalse(LargeTransactionAlertService.hasAlertBeenSent(tx1, appPreferences))
        assertFalse(LargeTransactionAlertService.hasAlertBeenSent(tx2, appPreferences))
    }

    @Test
    fun clearOldAlerts_whenNoAlerts_returnsZero() = runTest {
        // Given: No alerts sent

        // When: Clear old alerts
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val clearedCount = LargeTransactionAlertService.clearOldAlerts(appPreferences, thirtyDaysAgo)

        // Then: Returns zero
        assertEquals(0, clearedCount)
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun checkLargeTransactionAlert_zeroThreshold_sendsAlertForAnyDebit() = runTest {
        // Given: Zero threshold (alert on any spending)
        appPreferences.setLargeTransactionSettings(
            LargeTransactionSettings(enabled = true, threshold = 0.0)
        )
        val transaction = createTestTransaction(amount = 0.01)

        // When: Check alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Alert sent (₹0.01 >= ₹0.00)
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_veryLargeAmount_handlesCorrectly() = runTest {
        // Given: Very large transaction
        val transaction = createTestTransaction(amount = 10000000.0)  // ₹10,000,000

        // When: Check alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Alert sent successfully
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_decimalAmount_calculatesCorrectly() = runTest {
        // Given: Decimal amount exactly at threshold
        appPreferences.setLargeTransactionSettings(
            LargeTransactionSettings(enabled = true, threshold = 5000.50)
        )
        val transaction = createTestTransaction(amount = 5000.50)

        // When: Check alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Alert sent (exact match)
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_verySmallAmount_belowThreshold() = runTest {
        // Given: Very small transaction
        val transaction = createTestTransaction(amount = 0.01)

        // When: Check alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: No alert (₹0.01 < ₹5,000)
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_nullSmsId_handlesCorrectly() = runTest {
        // Given: Transaction with null smsId
        val transaction = createTestTransaction(
            amount = 10000.0,
            smsId = null
        )

        // When: Check alert
        val result = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Alert sent successfully (null smsId handled)
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    // ==================== Integration Tests ====================

    @Test
    fun checkLargeTransactionAlert_fullWorkflow() = runTest {
        // Given: Complete workflow scenario
        val transaction = createTestTransaction(
            amount = 15000.0,
            merchant = "Expensive Store",
            categoryId = "shopping"
        )

        // When: First check - alert sent
        val result1 = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Alert sent
        assertTrue(result1.isSuccess)
        assertEquals(true, result1.getOrNull())
        assertTrue(LargeTransactionAlertService.hasAlertBeenSent(transaction, appPreferences))

        // When: Second check - duplicate prevented
        val result2 = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )

        // Then: Duplicate prevented
        assertTrue(result2.isSuccess)
        assertEquals(false, result2.getOrNull())

        // When: Clear alert
        LargeTransactionAlertService.clearAlert(transaction, appPreferences)

        // Then: Can send again
        assertFalse(LargeTransactionAlertService.hasAlertBeenSent(transaction, appPreferences))

        val result3 = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = transaction,
            appPreferences = appPreferences
        )
        assertTrue(result3.isSuccess)
        assertEquals(true, result3.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_multipleCategories_tracksIndependently() = runTest {
        // Given: Same amount in different categories
        val foodTx = createTestTransaction(
            amount = 10000.0,
            categoryId = "food",
            merchant = "Restaurant",
            smsId = "1"
        )
        val shoppingTx = createTestTransaction(
            amount = 10000.0,
            categoryId = "shopping",
            merchant = "Store",
            smsId = "2"
        )

        // When: Check both
        val result1 = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = foodTx,
            appPreferences = appPreferences
        )
        val result2 = LargeTransactionAlertService.checkLargeTransactionAlert(
            context = context,
            transaction = shoppingTx,
            appPreferences = appPreferences
        )

        // Then: Both alerts sent (different transactions)
        assertTrue(result1.isSuccess)
        assertEquals(true, result1.getOrNull())
        assertTrue(result2.isSuccess)
        assertEquals(true, result2.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlerts_mixedTypesAndAmounts() = runTest {
        // Given: Mix of DEBIT/CREDIT, above/below threshold
        val transactions = listOf(
            createTestTransaction(amount = 10000.0, type = TransactionType.DEBIT, merchant = "M1"),   // Alert
            createTestTransaction(amount = 10000.0, type = TransactionType.CREDIT, merchant = "M2"),  // No alert (CREDIT)
            createTestTransaction(amount = 3000.0, type = TransactionType.DEBIT, merchant = "M3"),    // No alert (below threshold)
            createTestTransaction(amount = 7500.0, type = TransactionType.DEBIT, merchant = "M4"),    // Alert
            createTestTransaction(amount = 50000.0, type = TransactionType.CREDIT, merchant = "M5"),  // No alert (CREDIT)
            createTestTransaction(amount = 6000.0, type = TransactionType.DEBIT, merchant = "M6")     // Alert
        )

        // When: Check all
        val result = LargeTransactionAlertService.checkLargeTransactionAlerts(
            context = context,
            transactions = transactions,
            appPreferences = appPreferences
        )

        // Then: 3 alerts sent (only DEBIT transactions >= ₹5,000)
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun clearAllAlerts_allowsResendingAllAlerts() = runTest {
        // Given: Multiple transactions with alerts sent
        val transactions = listOf(
            createTestTransaction(amount = 6000.0, merchant = "M1", smsId = "1"),
            createTestTransaction(amount = 7000.0, merchant = "M2", smsId = "2"),
            createTestTransaction(amount = 8000.0, merchant = "M3", smsId = "3")
        )

        // Send all alerts
        LargeTransactionAlertService.checkLargeTransactionAlerts(context, transactions, appPreferences)

        // When: Clear all alerts and check again
        LargeTransactionAlertService.clearAllAlerts(appPreferences)
        val result = LargeTransactionAlertService.checkLargeTransactionAlerts(
            context = context,
            transactions = transactions,
            appPreferences = appPreferences
        )

        // Then: All 3 alerts sent again
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun checkLargeTransactionAlert_stressTest_manyTransactions() = runTest {
        // Given: Many transactions
        val transactions = (1..50).map { i ->
            createTestTransaction(
                amount = 5000.0 + (i * 100),  // All above threshold
                merchant = "Merchant $i",
                smsId = i.toString()
            )
        }

        // When: Check all
        val result = LargeTransactionAlertService.checkLargeTransactionAlerts(
            context = context,
            transactions = transactions,
            appPreferences = appPreferences
        )

        // Then: All 50 alerts sent
        assertTrue(result.isSuccess)
        assertEquals(50, result.getOrNull())

        // Verify all tracked
        transactions.forEach { tx ->
            assertTrue(LargeTransactionAlertService.hasAlertBeenSent(tx, appPreferences))
        }
    }
}
