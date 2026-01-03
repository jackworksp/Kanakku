package com.example.kanakku.domain.notification

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.entity.BudgetEntity
import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.model.BudgetAlertSettings
import com.example.kanakku.data.model.BudgetPeriod
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.data.repository.BudgetRepository
import com.example.kanakku.data.repository.TransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Unit tests for BudgetAlertService threshold detection logic.
 *
 * Tests cover:
 * - Budget threshold detection (80%, 100%)
 * - Alert tracking and duplicate prevention
 * - Budget period handling (MONTHLY, WEEKLY)
 * - Spending calculation for budget periods
 * - Notification creation and sending
 * - Alert clearing and reset logic
 * - Period key generation
 * - Edge cases and integration scenarios
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BudgetAlertServiceTest {

    private lateinit var context: Context
    private lateinit var database: KanakkuDatabase
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var appPreferences: AppPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Reset and initialize AppPreferences
        AppPreferences.resetInstance()
        appPreferences = AppPreferences.getInstance(context)
        appPreferences.clearAll()

        // Enable budget alerts by default
        appPreferences.setBudgetAlertSettings(
            BudgetAlertSettings(
                enabled = true,
                notifyAt80Percent = true,
                notifyAt100Percent = true
            )
        )

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        budgetRepository = BudgetRepository(database)
        transactionRepository = TransactionRepository(database)
    }

    @After
    fun teardown() {
        database.close()
        appPreferences.clearAll()
        AppPreferences.resetInstance()
    }

    // ==================== Helper Functions ====================

    private fun createTestBudget(
        categoryId: String = "food",
        amount: Double = 10000.0,
        period: BudgetPeriod = BudgetPeriod.MONTHLY,
        startDate: Long = System.currentTimeMillis()
    ): BudgetEntity {
        return BudgetEntity(
            id = 0,
            categoryId = categoryId,
            amount = amount,
            period = period,
            startDate = startDate,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun createTestTransaction(
        amount: Double,
        categoryId: String = "food",
        type: TransactionType = TransactionType.DEBIT,
        date: Long = System.currentTimeMillis(),
        merchant: String = "Test Merchant"
    ): TransactionEntity {
        return TransactionEntity(
            id = 0,
            amount = amount,
            categoryId = categoryId,
            type = type,
            merchant = merchant,
            date = date,
            smsId = System.currentTimeMillis().toString(),
            accountNumber = "1234",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun getCurrentMonthStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getCurrentWeekStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // ==================== Basic Alert Check Tests ====================

    @Test
    fun checkBudgetAlerts_whenAlertsDisabled_returnsZero() = runTest {
        // Given: Budget alerts disabled
        appPreferences.setBudgetAlertSettings(
            BudgetAlertSettings(
                enabled = false,
                notifyAt80Percent = true,
                notifyAt100Percent = true
            )
        )

        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create transaction at 90% of budget
        val transaction = createTestTransaction(amount = 9000.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: No alerts sent
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_whenNoBudgets_returnsZero() = runTest {
        // Given: No budgets configured
        // (budgetRepository is empty)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: No alerts sent
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_whenNoTransactions_returnsZero() = runTest {
        // Given: Budget exists but no transactions
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: No alerts sent (spending is 0%)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    // ==================== 80% Threshold Tests ====================

    @Test
    fun checkBudgetAlerts_when80PercentThresholdReached_sendsWarningAlert() = runTest {
        // Given: Budget of 10000
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create transaction for exactly 8000 (80%)
        val transaction = createTestTransaction(amount = 8000.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: 1 alert sent (80% threshold)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_when80PercentThresholdDisabled_doesNotSendAlert() = runTest {
        // Given: 80% threshold disabled
        appPreferences.setBudgetAlertSettings(
            BudgetAlertSettings(
                enabled = true,
                notifyAt80Percent = false,  // Disabled
                notifyAt100Percent = true
            )
        )

        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create transaction for 8500 (85% - above 80% threshold)
        val transaction = createTestTransaction(amount = 8500.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: No alert sent (80% threshold disabled)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_whenSpendingJustBelow80Percent_doesNotSendAlert() = runTest {
        // Given: Budget of 10000
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create transaction for 7999 (just below 80%)
        val transaction = createTestTransaction(amount = 7999.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: No alert sent
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_whenSpendingAbove80Percent_sendsWarningAlert() = runTest {
        // Given: Budget of 10000
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create transaction for 9000 (90% - well above 80%)
        val transaction = createTestTransaction(amount = 9000.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: 1 alert sent (80% threshold crossed)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    // ==================== 100% Threshold Tests ====================

    @Test
    fun checkBudgetAlerts_when100PercentThresholdReached_sendsLimitAlert() = runTest {
        // Given: Budget of 10000
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create transaction for exactly 10000 (100%)
        val transaction = createTestTransaction(amount = 10000.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: 2 alerts sent (both 80% and 100% thresholds)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_when100PercentThresholdDisabled_doesNotSendLimitAlert() = runTest {
        // Given: 100% threshold disabled
        appPreferences.setBudgetAlertSettings(
            BudgetAlertSettings(
                enabled = true,
                notifyAt80Percent = true,
                notifyAt100Percent = false  // Disabled
            )
        )

        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create transaction for 10000 (100%)
        val transaction = createTestTransaction(amount = 10000.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: 1 alert sent (only 80% threshold)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_whenSpendingExceeds100Percent_sendsLimitAlert() = runTest {
        // Given: Budget of 10000
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create transaction for 12000 (120% - over budget)
        val transaction = createTestTransaction(amount = 12000.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: 2 alerts sent (both thresholds)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    // ==================== Alert Tracking Tests ====================

    @Test
    fun checkBudgetAlerts_whenAlertAlreadySent_doesNotSendDuplicate() = runTest {
        // Given: Budget and transaction at 80%
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        val transaction = createTestTransaction(amount = 8000.0)
        transactionRepository.saveTransaction(transaction)

        // First check - should send alert
        val firstResult = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // When: Second check without new transactions
        val secondResult = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: First check sends alert, second check doesn't
        assertEquals(1, firstResult.getOrNull())
        assertEquals(0, secondResult.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_whenSpendingDropsBelowThreshold_clearsAlert() = runTest {
        // Given: Budget and transaction at 80%
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        val transaction1 = createTestTransaction(amount = 8000.0, date = getCurrentMonthStart())
        transactionRepository.saveTransaction(transaction1)

        // Send initial alert
        BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Remove the transaction (simulate spending drop)
        transactionRepository.deleteTransaction(transaction1.smsId)

        // Add small transaction below threshold
        val transaction2 = createTestTransaction(amount = 5000.0)
        transactionRepository.saveTransaction(transaction2)

        // When: Check again with spending below threshold
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Alert should be cleared and can be sent again later
        assertTrue(result.isSuccess)
        // Spending is now 5000 (50%), so no alerts
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_afterClearAllAlerts_canSendAlertsAgain() = runTest {
        // Given: Budget and alert already sent
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        val transaction = createTestTransaction(amount = 8000.0)
        transactionRepository.saveTransaction(transaction)

        // Send initial alert
        val firstResult = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )
        assertEquals(1, firstResult.getOrNull())

        // When: Clear all alerts
        val clearedCount = BudgetAlertService.clearAllAlerts(appPreferences)

        // Then: Alert tracking cleared
        assertEquals(1, clearedCount)

        // And: Can send alert again
        val secondResult = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )
        assertEquals(1, secondResult.getOrNull())
    }

    // ==================== Multiple Budgets Tests ====================

    @Test
    fun checkBudgetAlerts_withMultipleBudgets_checksAllBudgets() = runTest {
        // Given: Two budgets with different categories
        val foodBudget = createTestBudget(categoryId = "food", amount = 10000.0)
        val transportBudget = createTestBudget(categoryId = "transport", amount = 5000.0)
        budgetRepository.saveBudget(foodBudget)
        budgetRepository.saveBudget(transportBudget)

        // Create transactions exceeding 80% for both categories
        val foodTx = createTestTransaction(amount = 8500.0, categoryId = "food")
        val transportTx = createTestTransaction(amount = 4500.0, categoryId = "transport")
        transactionRepository.saveTransaction(foodTx)
        transactionRepository.saveTransaction(transportTx)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: 2 alerts sent (80% threshold for each budget)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_withMultipleBudgets_tracksSeparately() = runTest {
        // Given: Two budgets
        val foodBudget = createTestBudget(categoryId = "food", amount = 10000.0)
        val transportBudget = createTestBudget(categoryId = "transport", amount = 5000.0)
        budgetRepository.saveBudget(foodBudget)
        budgetRepository.saveBudget(transportBudget)

        // Only food category exceeds threshold
        val foodTx = createTestTransaction(amount = 8500.0, categoryId = "food")
        transactionRepository.saveTransaction(foodTx)

        // When: First check
        val firstResult = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Add transport spending
        val transportTx = createTestTransaction(amount = 4500.0, categoryId = "transport")
        transactionRepository.saveTransaction(transportTx)

        // When: Second check
        val secondResult = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: First check sends 1 alert, second check sends 1 alert (for transport only)
        assertEquals(1, firstResult.getOrNull())
        assertEquals(1, secondResult.getOrNull())
    }

    // ==================== Budget Period Tests ====================

    @Test
    fun checkBudgetAlerts_withMonthlyBudget_checksCurrentMonth() = runTest {
        // Given: Monthly budget
        val budget = createTestBudget(amount = 10000.0, period = BudgetPeriod.MONTHLY)
        budgetRepository.saveBudget(budget)

        // Create transaction in current month at 90%
        val transaction = createTestTransaction(amount = 9000.0, date = getCurrentMonthStart())
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Alert sent for current month
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_withWeeklyBudget_checksCurrentWeek() = runTest {
        // Given: Weekly budget
        val budget = createTestBudget(amount = 5000.0, period = BudgetPeriod.WEEKLY)
        budgetRepository.saveBudget(budget)

        // Create transaction in current week at 90%
        val transaction = createTestTransaction(amount = 4500.0, date = getCurrentWeekStart())
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Alert sent for current week
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_withMonthlyBudget_ignoresOldTransactions() = runTest {
        // Given: Monthly budget
        val budget = createTestBudget(amount = 10000.0, period = BudgetPeriod.MONTHLY)
        budgetRepository.saveBudget(budget)

        // Create transaction from 3 months ago (should be ignored)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -3)
        val oldTransaction = createTestTransaction(amount = 9000.0, date = calendar.timeInMillis)
        transactionRepository.saveTransaction(oldTransaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: No alert sent (old transaction not in current month)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    // ==================== Spending Calculation Tests ====================

    @Test
    fun checkBudgetAlerts_calculatesSpending_debitOnly() = runTest {
        // Given: Budget of 10000
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create DEBIT and CREDIT transactions
        val debit = createTestTransaction(amount = 8000.0, type = TransactionType.DEBIT)
        val credit = createTestTransaction(amount = 5000.0, type = TransactionType.CREDIT)
        transactionRepository.saveTransaction(debit)
        transactionRepository.saveTransaction(credit)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Only DEBIT counted (8000), so 80% threshold reached
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_calculatesSpending_sumMultipleTransactions() = runTest {
        // Given: Budget of 10000
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create multiple transactions totaling 8500 (85%)
        val tx1 = createTestTransaction(amount = 3000.0)
        val tx2 = createTestTransaction(amount = 2500.0)
        val tx3 = createTestTransaction(amount = 3000.0)
        transactionRepository.saveTransaction(tx1)
        transactionRepository.saveTransaction(tx2)
        transactionRepository.saveTransaction(tx3)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Total spending is 8500 (85%), so 80% threshold reached
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_calculatesSpending_filtersByCategory() = runTest {
        // Given: Food budget of 10000
        val budget = createTestBudget(categoryId = "food", amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create transactions in different categories
        val foodTx = createTestTransaction(amount = 8000.0, categoryId = "food")
        val transportTx = createTestTransaction(amount = 5000.0, categoryId = "transport")
        transactionRepository.saveTransaction(foodTx)
        transactionRepository.saveTransaction(transportTx)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Only food spending counted (8000), so 80% threshold reached
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    // ==================== Both Thresholds Tests ====================

    @Test
    fun checkBudgetAlerts_whenBothThresholdsEnabled_sendsBothAlerts() = runTest {
        // Given: Budget of 10000, both thresholds enabled
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create transaction for 10000 (100%)
        val transaction = createTestTransaction(amount = 10000.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: 2 alerts sent (80% and 100%)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_whenBothThresholdsDisabled_sendsNoAlerts() = runTest {
        // Given: Both thresholds disabled
        appPreferences.setBudgetAlertSettings(
            BudgetAlertSettings(
                enabled = true,
                notifyAt80Percent = false,
                notifyAt100Percent = false
            )
        )

        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Create transaction for 12000 (120%)
        val transaction = createTestTransaction(amount = 12000.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: No alerts sent
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_progressiveThresholds_sendsAlertsIncrementally() = runTest {
        // Given: Budget of 10000
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Start with 70% spending
        val tx1 = createTestTransaction(amount = 7000.0, date = getCurrentMonthStart())
        transactionRepository.saveTransaction(tx1)

        // Check 1: No alerts at 70%
        val result1 = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )
        assertEquals(0, result1.getOrNull())

        // Increase to 85% spending
        val tx2 = createTestTransaction(amount = 1500.0)
        transactionRepository.saveTransaction(tx2)

        // Check 2: 80% alert at 85%
        val result2 = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )
        assertEquals(1, result2.getOrNull())

        // Increase to 105% spending
        val tx3 = createTestTransaction(amount = 2000.0)
        transactionRepository.saveTransaction(tx3)

        // Check 3: 100% alert at 105% (80% already sent)
        val result3 = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )
        assertEquals(1, result3.getOrNull())
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun checkBudgetAlerts_withZeroBudget_handlesGracefully() = runTest {
        // Given: Budget with zero amount
        val budget = createTestBudget(amount = 0.0)
        budgetRepository.saveBudget(budget)

        val transaction = createTestTransaction(amount = 100.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should handle gracefully (spending / 0 would be infinity)
        assertTrue(result.isSuccess)
        // With 100 spending and 0 budget, both thresholds should trigger
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_withVerySmallBudget_handlesCorrectly() = runTest {
        // Given: Very small budget
        val budget = createTestBudget(amount = 0.01)
        budgetRepository.saveBudget(budget)

        val transaction = createTestTransaction(amount = 0.01)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should detect 100% threshold
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_withVeryLargeBudget_handlesCorrectly() = runTest {
        // Given: Very large budget
        val budget = createTestBudget(amount = 10000000.0)
        budgetRepository.saveBudget(budget)

        // 80% of 10M is 8M
        val transaction = createTestTransaction(amount = 8000000.0)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should detect 80% threshold
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun checkBudgetAlerts_withDecimalThresholds_calculatesCorrectly() = runTest {
        // Given: Budget that creates decimal thresholds
        val budget = createTestBudget(amount = 12345.67)
        budgetRepository.saveBudget(budget)

        // 80% of 12345.67 is 9876.536
        val transaction = createTestTransaction(amount = 9876.54)
        transactionRepository.saveTransaction(transaction)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: Should detect 80% threshold (9876.54 >= 9876.536)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    // ==================== Clear Old Alerts Tests ====================

    @Test
    fun clearOldAlerts_removesOldPeriodAlerts_keepsCurrentPeriod() = runTest {
        // Given: Alerts for current and old periods
        appPreferences.setBoolean("budget_alert_sent_food_MONTHLY_80_2025-12", true)
        appPreferences.setBoolean("budget_alert_sent_food_MONTHLY_80_2026-01", true)
        appPreferences.setBoolean("budget_alert_sent_transport_WEEKLY_100_2025-W52", true)

        // When: Clear alerts, keeping only current period keys
        val currentKeys = setOf("2026-01")
        val clearedCount = BudgetAlertService.clearOldAlerts(appPreferences, currentKeys)

        // Then: Old alerts cleared
        assertEquals(2, clearedCount)
        assertFalse(appPreferences.contains("budget_alert_sent_food_MONTHLY_80_2025-12"))
        assertTrue(appPreferences.contains("budget_alert_sent_food_MONTHLY_80_2026-01"))
        assertFalse(appPreferences.contains("budget_alert_sent_transport_WEEKLY_100_2025-W52"))
    }

    @Test
    fun clearOldAlerts_withNoOldAlerts_returnsZero() = runTest {
        // Given: Only current period alerts
        appPreferences.setBoolean("budget_alert_sent_food_MONTHLY_80_2026-01", true)

        // When: Clear old alerts
        val currentKeys = setOf("2026-01")
        val clearedCount = BudgetAlertService.clearOldAlerts(appPreferences, currentKeys)

        // Then: Nothing cleared
        assertEquals(0, clearedCount)
        assertTrue(appPreferences.contains("budget_alert_sent_food_MONTHLY_80_2026-01"))
    }

    @Test
    fun clearAllAlerts_removesAllAlertFlags() = runTest {
        // Given: Multiple alert flags
        appPreferences.setBoolean("budget_alert_sent_food_MONTHLY_80_2026-01", true)
        appPreferences.setBoolean("budget_alert_sent_transport_WEEKLY_100_2026-W01", true)
        appPreferences.setBoolean("some_other_preference", true)

        // When
        val clearedCount = BudgetAlertService.clearAllAlerts(appPreferences)

        // Then: All budget alerts cleared, other preferences remain
        assertEquals(2, clearedCount)
        assertFalse(appPreferences.contains("budget_alert_sent_food_MONTHLY_80_2026-01"))
        assertFalse(appPreferences.contains("budget_alert_sent_transport_WEEKLY_100_2026-W01"))
        assertTrue(appPreferences.contains("some_other_preference"))
    }

    // ==================== Integration Tests ====================

    @Test
    fun integration_fullWorkflow_budgetToAlertToTracking() = runTest {
        // Given: Create budget and initial spending
        val budget = createTestBudget(categoryId = "food", amount = 10000.0)
        budgetRepository.saveBudget(budget)

        // Step 1: 70% spending - no alerts
        val tx1 = createTestTransaction(amount = 7000.0)
        transactionRepository.saveTransaction(tx1)

        val result1 = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )
        assertEquals(0, result1.getOrNull())

        // Step 2: Increase to 85% - 80% alert sent
        val tx2 = createTestTransaction(amount = 1500.0)
        transactionRepository.saveTransaction(tx2)

        val result2 = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )
        assertEquals(1, result2.getOrNull())

        // Step 3: Check again - no duplicate alert
        val result3 = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )
        assertEquals(0, result3.getOrNull())

        // Step 4: Increase to 105% - 100% alert sent
        val tx3 = createTestTransaction(amount = 2000.0)
        transactionRepository.saveTransaction(tx3)

        val result4 = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )
        assertEquals(1, result4.getOrNull())

        // Step 5: Clear all alerts
        val cleared = BudgetAlertService.clearAllAlerts(appPreferences)
        assertEquals(2, cleared)

        // Step 6: Alerts can be sent again
        val result5 = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )
        assertEquals(2, result5.getOrNull())
    }

    @Test
    fun integration_multipleBudgetsAndCategories() = runTest {
        // Given: Multiple budgets for different categories and periods
        val foodMonthly = createTestBudget(
            categoryId = "food",
            amount = 10000.0,
            period = BudgetPeriod.MONTHLY
        )
        val transportWeekly = createTestBudget(
            categoryId = "transport",
            amount = 2000.0,
            period = BudgetPeriod.WEEKLY
        )
        val shoppingMonthly = createTestBudget(
            categoryId = "shopping",
            amount = 15000.0,
            period = BudgetPeriod.MONTHLY
        )

        budgetRepository.saveBudget(foodMonthly)
        budgetRepository.saveBudget(transportWeekly)
        budgetRepository.saveBudget(shoppingMonthly)

        // Create transactions exceeding different thresholds
        val foodTx = createTestTransaction(amount = 8500.0, categoryId = "food") // 85%
        val transportTx = createTestTransaction(amount = 2100.0, categoryId = "transport") // 105%
        val shoppingTx = createTestTransaction(amount = 7000.0, categoryId = "shopping") // 46.7%

        transactionRepository.saveTransaction(foodTx)
        transactionRepository.saveTransaction(transportTx)
        transactionRepository.saveTransaction(shoppingTx)

        // When
        val result = BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Then: 3 alerts total (food: 80%, transport: 80% + 100%)
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun integration_periodTransition_alertsReset() = runTest {
        // This test demonstrates that alerts are tracked per period
        // In a real scenario, when a new month/week starts, old alerts
        // would be cleared by clearOldAlerts()

        // Given: Budget and alert sent for "2026-01"
        val budget = createTestBudget(amount = 10000.0)
        budgetRepository.saveBudget(budget)

        val transaction = createTestTransaction(amount = 8500.0)
        transactionRepository.saveTransaction(transaction)

        // Send alert for current period
        BudgetAlertService.checkBudgetAlerts(
            context = context,
            budgetRepository = budgetRepository,
            transactionRepository = transactionRepository,
            appPreferences = appPreferences
        )

        // Verify alert tracking exists
        val alertKeys = appPreferences.getAllKeys()
            .filter { it.startsWith("budget_alert_sent_") }
        assertTrue(alertKeys.isNotEmpty())

        // Simulate period transition by clearing old alerts
        // In production, this would be called when detecting a new period
        val currentPeriodKey = "2026-02" // New month
        val clearedCount = BudgetAlertService.clearOldAlerts(
            appPreferences,
            setOf(currentPeriodKey)
        )

        // Then: Old alerts cleared
        assertTrue(clearedCount > 0)

        // And: New alert can be sent for the new period
        // (In this test, we're still in the same period, but the tracking is cleared)
        val newAlertKeys = appPreferences.getAllKeys()
            .filter { it.startsWith("budget_alert_sent_") && !it.contains(currentPeriodKey) }
        assertTrue(newAlertKeys.isEmpty())
    }
}
