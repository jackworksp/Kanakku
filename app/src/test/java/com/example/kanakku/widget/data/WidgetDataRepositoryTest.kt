package com.example.kanakku.widget.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.model.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Unit tests for WidgetDataRepository using in-memory Room database.
 *
 * Tests cover:
 * - Today's spending calculation
 * - Weekly budget progress calculation
 * - Recent transactions fetching
 * - Edge cases: no transactions, future dates, zero amounts
 * - Date range calculations
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WidgetDataRepositoryTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var repository: WidgetDataRepository

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        repository = WidgetDataRepository(ApplicationProvider.getApplicationContext())
    }

    @After
    fun teardown() {
        database.close()
    }

    // ==================== Helper Functions ====================

    private fun createTestTransaction(
        smsId: Long = 1L,
        amount: Double = 100.0,
        type: TransactionType = TransactionType.DEBIT,
        merchant: String? = "Test Merchant",
        date: Long = System.currentTimeMillis()
    ): TransactionEntity {
        return TransactionEntity(
            smsId = smsId,
            amount = amount,
            type = type,
            merchant = merchant,
            accountNumber = "1234",
            referenceNumber = "REF123",
            date = date,
            rawSms = "Test SMS",
            senderAddress = "VM-TESTBK",
            balanceAfter = 500.0,
            location = "Test Location"
        )
    }

    private fun getTodayDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Start of day (00:00:00.000)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        // End of day (23:59:59.999)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }

    private fun getWeekDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Calculate start of week (Monday)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis

        // Calculate end of week (Sunday)
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfWeek = calendar.timeInMillis

        return Pair(startOfWeek, endOfWeek)
    }

    // ==================== getTodaySpending Tests ====================

    @Test
    fun getTodaySpending_returnsCorrectSumForTodayTransactions() = runTest {
        // Given - Multiple debit transactions today
        val (startOfDay, endOfDay) = getTodayDateRange()
        val todayMidday = startOfDay + ((endOfDay - startOfDay) / 2)

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = todayMidday),
            createTestTransaction(smsId = 2L, amount = 250.0, type = TransactionType.DEBIT, date = todayMidday + 1000),
            createTestTransaction(smsId = 3L, amount = 50.0, type = TransactionType.DEBIT, date = todayMidday + 2000)
        )
        database.transactionDao().insertAll(transactions)

        // When
        val result = repository.getTodaySpending()

        // Then
        assertEquals(400.0, result.todayTotal, 0.01)
        assertEquals("₹", result.currency)
        assertTrue(result.lastUpdated > 0)
    }

    @Test
    fun getTodaySpending_excludesCreditTransactions() = runTest {
        // Given - Mix of debit and credit transactions today
        val (startOfDay, _) = getTodayDateRange()
        val todayMidday = startOfDay + 12 * 3600000 // Noon

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = todayMidday),
            createTestTransaction(smsId = 2L, amount = 500.0, type = TransactionType.CREDIT, date = todayMidday + 1000),
            createTestTransaction(smsId = 3L, amount = 50.0, type = TransactionType.DEBIT, date = todayMidday + 2000)
        )
        database.transactionDao().insertAll(transactions)

        // When
        val result = repository.getTodaySpending()

        // Then - Only debits counted (100 + 50 = 150)
        assertEquals(150.0, result.todayTotal, 0.01)
    }

    @Test
    fun getTodaySpending_excludesYesterdayTransactions() = runTest {
        // Given - Transactions from yesterday and today
        val (startOfDay, _) = getTodayDateRange()
        val todayMidday = startOfDay + 12 * 3600000 // Noon
        val yesterday = startOfDay - 24 * 3600000 // Yesterday

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = yesterday),
            createTestTransaction(smsId = 2L, amount = 250.0, type = TransactionType.DEBIT, date = todayMidday)
        )
        database.transactionDao().insertAll(transactions)

        // When
        val result = repository.getTodaySpending()

        // Then - Only today's transaction (250)
        assertEquals(250.0, result.todayTotal, 0.01)
    }

    @Test
    fun getTodaySpending_returnsZeroWhenNoTransactions() = runTest {
        // Given - Empty database
        // When
        val result = repository.getTodaySpending()

        // Then
        assertEquals(0.0, result.todayTotal, 0.01)
        assertEquals("₹", result.currency)
    }

    @Test
    fun getTodaySpending_returnsZeroWhenOnlyOldTransactions() = runTest {
        // Given - Only old transactions (last week)
        val lastWeek = System.currentTimeMillis() - 7 * 24 * 3600000
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = lastWeek),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT, date = lastWeek + 1000)
        )
        database.transactionDao().insertAll(transactions)

        // When
        val result = repository.getTodaySpending()

        // Then
        assertEquals(0.0, result.todayTotal, 0.01)
    }

    @Test
    fun getTodaySpending_handlesStartAndEndOfDayCorrectly() = runTest {
        // Given - Transactions at the very start and very end of day
        val (startOfDay, endOfDay) = getTodayDateRange()

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = startOfDay),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT, date = endOfDay)
        )
        database.transactionDao().insertAll(transactions)

        // When
        val result = repository.getTodaySpending()

        // Then - Both should be included
        assertEquals(300.0, result.todayTotal, 0.01)
    }

    // ==================== getWeeklyBudgetProgress Tests ====================

    @Test
    fun getWeeklyBudgetProgress_calculatesCorrectPercentage() = runTest {
        // Given - Transactions this week totaling 500
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val midWeek = startOfWeek + ((endOfWeek - startOfWeek) / 2)

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 200.0, type = TransactionType.DEBIT, date = midWeek),
            createTestTransaction(smsId = 2L, amount = 300.0, type = TransactionType.DEBIT, date = midWeek + 1000)
        )
        database.transactionDao().insertAll(transactions)

        // When - Budget is 1000
        val result = repository.getWeeklyBudgetProgress(budget = 1000.0)

        // Then - 500/1000 = 50%
        assertEquals(500.0, result.spent, 0.01)
        assertEquals(1000.0, result.budget, 0.01)
        assertEquals(50.0, result.percentage, 0.01)
        assertEquals("This Week", result.periodLabel)
    }

    @Test
    fun getWeeklyBudgetProgress_capsPercentageAt100() = runTest {
        // Given - Spending exceeds budget (1200 spent vs 1000 budget)
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val midWeek = startOfWeek + ((endOfWeek - startOfWeek) / 2)

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 700.0, type = TransactionType.DEBIT, date = midWeek),
            createTestTransaction(smsId = 2L, amount = 500.0, type = TransactionType.DEBIT, date = midWeek + 1000)
        )
        database.transactionDao().insertAll(transactions)

        // When - Budget is 1000 but spent 1200
        val result = repository.getWeeklyBudgetProgress(budget = 1000.0)

        // Then - Percentage capped at 100%
        assertEquals(1200.0, result.spent, 0.01)
        assertEquals(1000.0, result.budget, 0.01)
        assertEquals(100.0, result.percentage, 0.01)
    }

    @Test
    fun getWeeklyBudgetProgress_handlesZeroBudget() = runTest {
        // Given - Transactions exist but no budget set
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val midWeek = startOfWeek + ((endOfWeek - startOfWeek) / 2)

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = midWeek)
        )
        database.transactionDao().insertAll(transactions)

        // When - Budget is 0 (division by zero scenario)
        val result = repository.getWeeklyBudgetProgress(budget = 0.0)

        // Then - Should handle gracefully
        assertEquals(100.0, result.spent, 0.01)
        assertEquals(0.0, result.budget, 0.01)
        assertEquals(0.0, result.percentage, 0.01) // Avoid division by zero
    }

    @Test
    fun getWeeklyBudgetProgress_excludesCreditTransactions() = runTest {
        // Given - Mix of debit and credit transactions this week
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val midWeek = startOfWeek + ((endOfWeek - startOfWeek) / 2)

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 200.0, type = TransactionType.DEBIT, date = midWeek),
            createTestTransaction(smsId = 2L, amount = 500.0, type = TransactionType.CREDIT, date = midWeek + 1000),
            createTestTransaction(smsId = 3L, amount = 100.0, type = TransactionType.DEBIT, date = midWeek + 2000)
        )
        database.transactionDao().insertAll(transactions)

        // When
        val result = repository.getWeeklyBudgetProgress(budget = 1000.0)

        // Then - Only debits counted (200 + 100 = 300)
        assertEquals(300.0, result.spent, 0.01)
        assertEquals(30.0, result.percentage, 0.01)
    }

    @Test
    fun getWeeklyBudgetProgress_excludesLastWeekTransactions() = runTest {
        // Given - Transactions from last week and this week
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val midWeek = startOfWeek + ((endOfWeek - startOfWeek) / 2)
        val lastWeek = startOfWeek - 7 * 24 * 3600000

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 500.0, type = TransactionType.DEBIT, date = lastWeek),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT, date = midWeek)
        )
        database.transactionDao().insertAll(transactions)

        // When
        val result = repository.getWeeklyBudgetProgress(budget = 1000.0)

        // Then - Only this week's transaction (200)
        assertEquals(200.0, result.spent, 0.01)
        assertEquals(20.0, result.percentage, 0.01)
    }

    @Test
    fun getWeeklyBudgetProgress_returnsZeroWhenNoTransactions() = runTest {
        // Given - Empty database
        // When
        val result = repository.getWeeklyBudgetProgress(budget = 1000.0)

        // Then
        assertEquals(0.0, result.spent, 0.01)
        assertEquals(1000.0, result.budget, 0.01)
        assertEquals(0.0, result.percentage, 0.01)
        assertEquals("This Week", result.periodLabel)
    }

    @Test
    fun getWeeklyBudgetProgress_handlesStartAndEndOfWeekCorrectly() = runTest {
        // Given - Transactions at the very start (Monday) and very end (Sunday) of week
        val (startOfWeek, endOfWeek) = getWeekDateRange()

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = startOfWeek),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT, date = endOfWeek)
        )
        database.transactionDao().insertAll(transactions)

        // When
        val result = repository.getWeeklyBudgetProgress(budget = 1000.0)

        // Then - Both should be included
        assertEquals(300.0, result.spent, 0.01)
        assertEquals(30.0, result.percentage, 0.01)
    }

    // ==================== getRecentTransactions Tests ====================

    @Test
    fun getRecentTransactions_returnsCorrectLimit() = runTest {
        // Given - 10 transactions
        val now = System.currentTimeMillis()
        val transactions = (1L..10L).map { id ->
            createTestTransaction(
                smsId = id,
                amount = id * 100.0,
                date = now - (id * 1000) // Older as id increases
            )
        }
        database.transactionDao().insertAll(transactions)

        // When - Request 5 most recent
        val result = repository.getRecentTransactions(limit = 5)

        // Then - Should get exactly 5 transactions
        assertEquals(5, result.transactions.size)
        assertTrue(result.lastUpdated > 0)
    }

    @Test
    fun getRecentTransactions_returnsNewestFirst() = runTest {
        // Given - Transactions with different dates
        val now = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, date = now - 3000),
            createTestTransaction(smsId = 2L, amount = 200.0, date = now - 2000),
            createTestTransaction(smsId = 3L, amount = 300.0, date = now - 1000),
            createTestTransaction(smsId = 4L, amount = 400.0, date = now)
        )
        database.transactionDao().insertAll(transactions)

        // When
        val result = repository.getRecentTransactions(limit = 4)

        // Then - Should be sorted newest first
        assertEquals(4, result.transactions.size)
        assertEquals(4L, result.transactions[0].id)
        assertEquals(400.0, result.transactions[0].amount, 0.01)
        assertEquals(1L, result.transactions[3].id)
        assertEquals(100.0, result.transactions[3].amount, 0.01)
    }

    @Test
    fun getRecentTransactions_includesBothDebitAndCredit() = runTest {
        // Given - Mix of transaction types
        val now = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = now - 3000),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.CREDIT, date = now - 2000),
            createTestTransaction(smsId = 3L, amount = 300.0, type = TransactionType.DEBIT, date = now - 1000)
        )
        database.transactionDao().insertAll(transactions)

        // When
        val result = repository.getRecentTransactions(limit = 3)

        // Then - Should include both types
        assertEquals(3, result.transactions.size)
        assertEquals(TransactionType.DEBIT, result.transactions[0].type)
        assertEquals(TransactionType.CREDIT, result.transactions[1].type)
        assertEquals(TransactionType.DEBIT, result.transactions[2].type)
    }

    @Test
    fun getRecentTransactions_returnsEmptyListWhenNoTransactions() = runTest {
        // Given - Empty database
        // When
        val result = repository.getRecentTransactions(limit = 5)

        // Then
        assertTrue(result.transactions.isEmpty())
        assertTrue(result.lastUpdated > 0)
    }

    @Test
    fun getRecentTransactions_returnsAllWhenFewerThanLimit() = runTest {
        // Given - Only 3 transactions but requesting 5
        val now = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, date = now - 2000),
            createTestTransaction(smsId = 2L, amount = 200.0, date = now - 1000),
            createTestTransaction(smsId = 3L, amount = 300.0, date = now)
        )
        database.transactionDao().insertAll(transactions)

        // When
        val result = repository.getRecentTransactions(limit = 5)

        // Then - Should return all 3 (not fail or pad)
        assertEquals(3, result.transactions.size)
    }

    @Test
    fun getRecentTransactions_mapsFieldsCorrectly() = runTest {
        // Given - Transaction with specific values
        val now = System.currentTimeMillis()
        val transaction = createTestTransaction(
            smsId = 123L,
            merchant = "Amazon",
            amount = 456.78,
            type = TransactionType.CREDIT,
            date = now
        )
        database.transactionDao().insertAll(listOf(transaction))

        // When
        val result = repository.getRecentTransactions(limit = 1)

        // Then - All fields should be mapped correctly
        assertEquals(1, result.transactions.size)
        val widgetTx = result.transactions[0]
        assertEquals(123L, widgetTx.id)
        assertEquals("Amazon", widgetTx.merchant)
        assertEquals(456.78, widgetTx.amount, 0.01)
        assertEquals(TransactionType.CREDIT, widgetTx.type)
        assertEquals(now, widgetTx.date)
    }

    @Test
    fun getRecentTransactions_handlesNullMerchant() = runTest {
        // Given - Transaction with null merchant
        val now = System.currentTimeMillis()
        val transaction = createTestTransaction(
            smsId = 1L,
            merchant = null,
            amount = 100.0,
            date = now
        )
        database.transactionDao().insertAll(listOf(transaction))

        // When
        val result = repository.getRecentTransactions(limit = 1)

        // Then - Should default to "Unknown"
        assertEquals(1, result.transactions.size)
        assertEquals("Unknown", result.transactions[0].merchant)
    }

    @Test
    fun getRecentTransactions_respectsDifferentLimits() = runTest {
        // Given - 10 transactions
        val now = System.currentTimeMillis()
        val transactions = (1L..10L).map { id ->
            createTestTransaction(smsId = id, amount = id * 100.0, date = now - (id * 1000))
        }
        database.transactionDao().insertAll(transactions)

        // When - Request different limits
        val result1 = repository.getRecentTransactions(limit = 1)
        val result3 = repository.getRecentTransactions(limit = 3)
        val result10 = repository.getRecentTransactions(limit = 10)

        // Then
        assertEquals(1, result1.transactions.size)
        assertEquals(3, result3.transactions.size)
        assertEquals(10, result10.transactions.size)
    }

    // ==================== Edge Cases and Integration Tests ====================

    @Test
    fun multipleMethodCalls_workIndependently() = runTest {
        // Given - Transactions spanning multiple time periods
        val (startOfDay, _) = getTodayDateRange()
        val (startOfWeek, _) = getWeekDateRange()
        val now = System.currentTimeMillis()

        val transactions = listOf(
            // Today's transactions
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = startOfDay + 1000),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT, date = now),
            // This week but not today
            createTestTransaction(smsId = 3L, amount = 300.0, type = TransactionType.DEBIT, date = startOfWeek + 1000),
            // Old transaction
            createTestTransaction(smsId = 4L, amount = 400.0, type = TransactionType.DEBIT, date = startOfWeek - 100000)
        )
        database.transactionDao().insertAll(transactions)

        // When - Call all methods
        val todayResult = repository.getTodaySpending()
        val weekResult = repository.getWeeklyBudgetProgress(budget = 1000.0)
        val recentResult = repository.getRecentTransactions(limit = 5)

        // Then - Each should return correct data
        assertEquals(300.0, todayResult.todayTotal, 0.01) // 100 + 200
        assertEquals(600.0, weekResult.spent, 0.01) // 100 + 200 + 300
        assertEquals(4, recentResult.transactions.size)
    }

    @Test
    fun allMethods_handleEmptyDatabase() = runTest {
        // Given - Empty database
        // When
        val todayResult = repository.getTodaySpending()
        val weekResult = repository.getWeeklyBudgetProgress(budget = 1000.0)
        val recentResult = repository.getRecentTransactions(limit = 5)

        // Then - All should return empty/zero results gracefully
        assertEquals(0.0, todayResult.todayTotal, 0.01)
        assertEquals(0.0, weekResult.spent, 0.01)
        assertEquals(0.0, weekResult.percentage, 0.01)
        assertTrue(recentResult.transactions.isEmpty())
    }

    @Test
    fun allMethods_handleUnknownTransactionType() = runTest {
        // Given - Transaction with UNKNOWN type
        val (startOfDay, _) = getTodayDateRange()
        val transaction = createTestTransaction(
            smsId = 1L,
            amount = 100.0,
            type = TransactionType.UNKNOWN,
            date = startOfDay + 1000
        )
        database.transactionDao().insertAll(listOf(transaction))

        // When
        val todayResult = repository.getTodaySpending()
        val weekResult = repository.getWeeklyBudgetProgress(budget = 1000.0)
        val recentResult = repository.getRecentTransactions(limit = 5)

        // Then - Should exclude UNKNOWN from spending calculations
        assertEquals(0.0, todayResult.todayTotal, 0.01)
        assertEquals(0.0, weekResult.spent, 0.01)
        // But should appear in recent transactions
        assertEquals(1, recentResult.transactions.size)
        assertEquals(TransactionType.UNKNOWN, recentResult.transactions[0].type)
    }
}
