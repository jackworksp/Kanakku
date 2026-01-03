package com.example.kanakku.widget.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.dao.TransactionDao
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
 * Unit tests for widget-specific TransactionDao queries using in-memory Room database.
 *
 * Tests cover the three widget-optimized queries:
 * - getTodayDebitTotal() - Today's spending calculation
 * - getWeekDebitTotal() - Weekly spending calculation
 * - getRecentTransactionsSnapshot() - Recent transactions list
 *
 * These queries are optimized for widget performance by performing
 * aggregation and filtering at the database level.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WidgetDaoQueriesTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        transactionDao = database.transactionDao()
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

    // ==================== getTodayDebitTotal Tests ====================

    @Test
    fun getTodayDebitTotal_returnsCorrectSumForTodayTransactions() = runTest {
        // Given - Multiple debit transactions today
        val (startOfDay, endOfDay) = getTodayDateRange()
        val todayMidday = startOfDay + ((endOfDay - startOfDay) / 2)

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = todayMidday),
            createTestTransaction(smsId = 2L, amount = 250.0, type = TransactionType.DEBIT, date = todayMidday + 1000),
            createTestTransaction(smsId = 3L, amount = 50.0, type = TransactionType.DEBIT, date = todayMidday + 2000)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getTodayDebitTotal(startOfDay, endOfDay)

        // Then - Should sum all debit amounts (100 + 250 + 50 = 400)
        assertEquals(400.0, total, 0.01)
    }

    @Test
    fun getTodayDebitTotal_excludesCreditTransactions() = runTest {
        // Given - Mix of debit and credit transactions today
        val (startOfDay, endOfDay) = getTodayDateRange()
        val todayMidday = startOfDay + 12 * 3600000 // Noon

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = todayMidday),
            createTestTransaction(smsId = 2L, amount = 500.0, type = TransactionType.CREDIT, date = todayMidday + 1000),
            createTestTransaction(smsId = 3L, amount = 50.0, type = TransactionType.DEBIT, date = todayMidday + 2000)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getTodayDebitTotal(startOfDay, endOfDay)

        // Then - Only debits counted (100 + 50 = 150), credit excluded
        assertEquals(150.0, total, 0.01)
    }

    @Test
    fun getTodayDebitTotal_excludesUnknownTransactions() = runTest {
        // Given - Mix of debit and unknown transactions today
        val (startOfDay, endOfDay) = getTodayDateRange()
        val todayMidday = startOfDay + 12 * 3600000

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = todayMidday),
            createTestTransaction(smsId = 2L, amount = 300.0, type = TransactionType.UNKNOWN, date = todayMidday + 1000),
            createTestTransaction(smsId = 3L, amount = 50.0, type = TransactionType.DEBIT, date = todayMidday + 2000)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getTodayDebitTotal(startOfDay, endOfDay)

        // Then - Only debits counted (100 + 50 = 150), unknown excluded
        assertEquals(150.0, total, 0.01)
    }

    @Test
    fun getTodayDebitTotal_excludesYesterdayTransactions() = runTest {
        // Given - Transactions from yesterday and today
        val (startOfDay, endOfDay) = getTodayDateRange()
        val todayMidday = startOfDay + 12 * 3600000 // Noon
        val yesterday = startOfDay - 24 * 3600000 // Yesterday

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = yesterday),
            createTestTransaction(smsId = 2L, amount = 250.0, type = TransactionType.DEBIT, date = todayMidday)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getTodayDebitTotal(startOfDay, endOfDay)

        // Then - Only today's transaction (250)
        assertEquals(250.0, total, 0.01)
    }

    @Test
    fun getTodayDebitTotal_excludesTomorrowTransactions() = runTest {
        // Given - Transactions from today and tomorrow
        val (startOfDay, endOfDay) = getTodayDateRange()
        val todayMidday = startOfDay + 12 * 3600000 // Noon
        val tomorrow = endOfDay + 1000 // Tomorrow (just after end of day)

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = todayMidday),
            createTestTransaction(smsId = 2L, amount = 250.0, type = TransactionType.DEBIT, date = tomorrow)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getTodayDebitTotal(startOfDay, endOfDay)

        // Then - Only today's transaction (100)
        assertEquals(100.0, total, 0.01)
    }

    @Test
    fun getTodayDebitTotal_returnsZeroWhenNoTransactions() = runTest {
        // Given - Empty database
        val (startOfDay, endOfDay) = getTodayDateRange()

        // When
        val total = transactionDao.getTodayDebitTotal(startOfDay, endOfDay)

        // Then - Should return 0.0 (COALESCE behavior)
        assertEquals(0.0, total, 0.01)
    }

    @Test
    fun getTodayDebitTotal_returnsZeroWhenOnlyOldTransactions() = runTest {
        // Given - Only old transactions (last week)
        val (startOfDay, endOfDay) = getTodayDateRange()
        val lastWeek = System.currentTimeMillis() - 7 * 24 * 3600000

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = lastWeek),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT, date = lastWeek + 1000)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getTodayDebitTotal(startOfDay, endOfDay)

        // Then - Should return 0.0
        assertEquals(0.0, total, 0.01)
    }

    @Test
    fun getTodayDebitTotal_handlesStartAndEndOfDayCorrectly() = runTest {
        // Given - Transactions at the very start and very end of day
        val (startOfDay, endOfDay) = getTodayDateRange()

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = startOfDay),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT, date = endOfDay)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getTodayDebitTotal(startOfDay, endOfDay)

        // Then - Both should be included (inclusive boundaries)
        assertEquals(300.0, total, 0.01)
    }

    @Test
    fun getTodayDebitTotal_handlesDecimalAmounts() = runTest {
        // Given - Transactions with decimal amounts
        val (startOfDay, endOfDay) = getTodayDateRange()
        val todayMidday = startOfDay + 12 * 3600000

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 123.45, type = TransactionType.DEBIT, date = todayMidday),
            createTestTransaction(smsId = 2L, amount = 67.89, type = TransactionType.DEBIT, date = todayMidday + 1000)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getTodayDebitTotal(startOfDay, endOfDay)

        // Then - Should sum decimals correctly (123.45 + 67.89 = 191.34)
        assertEquals(191.34, total, 0.01)
    }

    // ==================== getWeekDebitTotal Tests ====================

    @Test
    fun getWeekDebitTotal_returnsCorrectSumForWeekTransactions() = runTest {
        // Given - Multiple debit transactions this week
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val midWeek = startOfWeek + ((endOfWeek - startOfWeek) / 2)

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 200.0, type = TransactionType.DEBIT, date = midWeek),
            createTestTransaction(smsId = 2L, amount = 300.0, type = TransactionType.DEBIT, date = midWeek + 1000),
            createTestTransaction(smsId = 3L, amount = 150.0, type = TransactionType.DEBIT, date = midWeek + 2000)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getWeekDebitTotal(startOfWeek, endOfWeek)

        // Then - Should sum all debit amounts (200 + 300 + 150 = 650)
        assertEquals(650.0, total, 0.01)
    }

    @Test
    fun getWeekDebitTotal_excludesCreditTransactions() = runTest {
        // Given - Mix of debit and credit transactions this week
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val midWeek = startOfWeek + ((endOfWeek - startOfWeek) / 2)

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 200.0, type = TransactionType.DEBIT, date = midWeek),
            createTestTransaction(smsId = 2L, amount = 500.0, type = TransactionType.CREDIT, date = midWeek + 1000),
            createTestTransaction(smsId = 3L, amount = 100.0, type = TransactionType.DEBIT, date = midWeek + 2000)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getWeekDebitTotal(startOfWeek, endOfWeek)

        // Then - Only debits counted (200 + 100 = 300), credit excluded
        assertEquals(300.0, total, 0.01)
    }

    @Test
    fun getWeekDebitTotal_excludesUnknownTransactions() = runTest {
        // Given - Mix of debit and unknown transactions this week
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val midWeek = startOfWeek + ((endOfWeek - startOfWeek) / 2)

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 200.0, type = TransactionType.DEBIT, date = midWeek),
            createTestTransaction(smsId = 2L, amount = 400.0, type = TransactionType.UNKNOWN, date = midWeek + 1000),
            createTestTransaction(smsId = 3L, amount = 100.0, type = TransactionType.DEBIT, date = midWeek + 2000)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getWeekDebitTotal(startOfWeek, endOfWeek)

        // Then - Only debits counted (200 + 100 = 300), unknown excluded
        assertEquals(300.0, total, 0.01)
    }

    @Test
    fun getWeekDebitTotal_excludesLastWeekTransactions() = runTest {
        // Given - Transactions from last week and this week
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val midWeek = startOfWeek + ((endOfWeek - startOfWeek) / 2)
        val lastWeek = startOfWeek - 7 * 24 * 3600000

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 500.0, type = TransactionType.DEBIT, date = lastWeek),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT, date = midWeek)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getWeekDebitTotal(startOfWeek, endOfWeek)

        // Then - Only this week's transaction (200)
        assertEquals(200.0, total, 0.01)
    }

    @Test
    fun getWeekDebitTotal_excludesNextWeekTransactions() = runTest {
        // Given - Transactions from this week and next week
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val midWeek = startOfWeek + ((endOfWeek - startOfWeek) / 2)
        val nextWeek = endOfWeek + 1000 // Just after end of week

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 200.0, type = TransactionType.DEBIT, date = midWeek),
            createTestTransaction(smsId = 2L, amount = 500.0, type = TransactionType.DEBIT, date = nextWeek)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getWeekDebitTotal(startOfWeek, endOfWeek)

        // Then - Only this week's transaction (200)
        assertEquals(200.0, total, 0.01)
    }

    @Test
    fun getWeekDebitTotal_returnsZeroWhenNoTransactions() = runTest {
        // Given - Empty database
        val (startOfWeek, endOfWeek) = getWeekDateRange()

        // When
        val total = transactionDao.getWeekDebitTotal(startOfWeek, endOfWeek)

        // Then - Should return 0.0 (COALESCE behavior)
        assertEquals(0.0, total, 0.01)
    }

    @Test
    fun getWeekDebitTotal_handlesStartAndEndOfWeekCorrectly() = runTest {
        // Given - Transactions at the very start (Monday) and very end (Sunday) of week
        val (startOfWeek, endOfWeek) = getWeekDateRange()

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = startOfWeek),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT, date = endOfWeek)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getWeekDebitTotal(startOfWeek, endOfWeek)

        // Then - Both should be included (inclusive boundaries)
        assertEquals(300.0, total, 0.01)
    }

    @Test
    fun getWeekDebitTotal_handlesDecimalAmounts() = runTest {
        // Given - Transactions with decimal amounts
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val midWeek = startOfWeek + ((endOfWeek - startOfWeek) / 2)

        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 456.78, type = TransactionType.DEBIT, date = midWeek),
            createTestTransaction(smsId = 2L, amount = 123.45, type = TransactionType.DEBIT, date = midWeek + 1000)
        )
        transactionDao.insertAll(transactions)

        // When
        val total = transactionDao.getWeekDebitTotal(startOfWeek, endOfWeek)

        // Then - Should sum decimals correctly (456.78 + 123.45 = 580.23)
        assertEquals(580.23, total, 0.01)
    }

    // ==================== getRecentTransactionsSnapshot Tests ====================

    @Test
    fun getRecentTransactionsSnapshot_returnsCorrectLimit() = runTest {
        // Given - 10 transactions
        val now = System.currentTimeMillis()
        val transactions = (1L..10L).map { id ->
            createTestTransaction(
                smsId = id,
                amount = id * 100.0,
                date = now - (id * 1000) // Older as id increases
            )
        }
        transactionDao.insertAll(transactions)

        // When - Request 5 most recent
        val result = transactionDao.getRecentTransactionsSnapshot(limit = 5)

        // Then - Should get exactly 5 transactions
        assertEquals(5, result.size)
    }

    @Test
    fun getRecentTransactionsSnapshot_returnsNewestFirst() = runTest {
        // Given - Transactions with different dates
        val now = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, date = now - 3000),
            createTestTransaction(smsId = 2L, amount = 200.0, date = now - 2000),
            createTestTransaction(smsId = 3L, amount = 300.0, date = now - 1000),
            createTestTransaction(smsId = 4L, amount = 400.0, date = now)
        )
        transactionDao.insertAll(transactions)

        // When
        val result = transactionDao.getRecentTransactionsSnapshot(limit = 4)

        // Then - Should be sorted newest first (date DESC)
        assertEquals(4, result.size)
        assertEquals(4L, result[0].smsId)
        assertEquals(400.0, result[0].amount, 0.01)
        assertEquals(1L, result[3].smsId)
        assertEquals(100.0, result[3].amount, 0.01)
    }

    @Test
    fun getRecentTransactionsSnapshot_includesBothDebitAndCredit() = runTest {
        // Given - Mix of transaction types
        val now = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = now - 3000),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.CREDIT, date = now - 2000),
            createTestTransaction(smsId = 3L, amount = 300.0, type = TransactionType.DEBIT, date = now - 1000)
        )
        transactionDao.insertAll(transactions)

        // When
        val result = transactionDao.getRecentTransactionsSnapshot(limit = 3)

        // Then - Should include both types
        assertEquals(3, result.size)
        assertEquals(TransactionType.DEBIT, result[0].type) // smsId 3
        assertEquals(TransactionType.CREDIT, result[1].type) // smsId 2
        assertEquals(TransactionType.DEBIT, result[2].type) // smsId 1
    }

    @Test
    fun getRecentTransactionsSnapshot_includesUnknownTransactions() = runTest {
        // Given - Mix including unknown type
        val now = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = now - 2000),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.UNKNOWN, date = now - 1000),
            createTestTransaction(smsId = 3L, amount = 300.0, type = TransactionType.CREDIT, date = now)
        )
        transactionDao.insertAll(transactions)

        // When
        val result = transactionDao.getRecentTransactionsSnapshot(limit = 3)

        // Then - Should include all types including UNKNOWN
        assertEquals(3, result.size)
        assertEquals(TransactionType.CREDIT, result[0].type)
        assertEquals(TransactionType.UNKNOWN, result[1].type)
        assertEquals(TransactionType.DEBIT, result[2].type)
    }

    @Test
    fun getRecentTransactionsSnapshot_returnsEmptyListWhenNoTransactions() = runTest {
        // Given - Empty database
        // When
        val result = transactionDao.getRecentTransactionsSnapshot(limit = 5)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun getRecentTransactionsSnapshot_returnsAllWhenFewerThanLimit() = runTest {
        // Given - Only 3 transactions but requesting 5
        val now = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, date = now - 2000),
            createTestTransaction(smsId = 2L, amount = 200.0, date = now - 1000),
            createTestTransaction(smsId = 3L, amount = 300.0, date = now)
        )
        transactionDao.insertAll(transactions)

        // When
        val result = transactionDao.getRecentTransactionsSnapshot(limit = 5)

        // Then - Should return all 3 (not fail or pad)
        assertEquals(3, result.size)
    }

    @Test
    fun getRecentTransactionsSnapshot_respectsDifferentLimits() = runTest {
        // Given - 10 transactions
        val now = System.currentTimeMillis()
        val transactions = (1L..10L).map { id ->
            createTestTransaction(smsId = id, amount = id * 100.0, date = now - (id * 1000))
        }
        transactionDao.insertAll(transactions)

        // When - Request different limits
        val result1 = transactionDao.getRecentTransactionsSnapshot(limit = 1)
        val result3 = transactionDao.getRecentTransactionsSnapshot(limit = 3)
        val result10 = transactionDao.getRecentTransactionsSnapshot(limit = 10)

        // Then - Each should respect the limit
        assertEquals(1, result1.size)
        assertEquals(3, result3.size)
        assertEquals(10, result10.size)
    }

    @Test
    fun getRecentTransactionsSnapshot_returnsCompleteEntityData() = runTest {
        // Given - Transaction with specific values
        val now = System.currentTimeMillis()
        val transaction = createTestTransaction(
            smsId = 123L,
            merchant = "Amazon",
            amount = 456.78,
            type = TransactionType.CREDIT,
            date = now
        )
        transactionDao.insertAll(listOf(transaction))

        // When
        val result = transactionDao.getRecentTransactionsSnapshot(limit = 1)

        // Then - All fields should be present and correct
        assertEquals(1, result.size)
        val entity = result[0]
        assertEquals(123L, entity.smsId)
        assertEquals("Amazon", entity.merchant)
        assertEquals(456.78, entity.amount, 0.01)
        assertEquals(TransactionType.CREDIT, entity.type)
        assertEquals(now, entity.date)
        assertEquals("1234", entity.accountNumber)
        assertEquals("REF123", entity.referenceNumber)
    }

    @Test
    fun getRecentTransactionsSnapshot_handlesNullMerchant() = runTest {
        // Given - Transaction with null merchant
        val now = System.currentTimeMillis()
        val transaction = createTestTransaction(
            smsId = 1L,
            merchant = null,
            amount = 100.0,
            date = now
        )
        transactionDao.insertAll(listOf(transaction))

        // When
        val result = transactionDao.getRecentTransactionsSnapshot(limit = 1)

        // Then - Should return entity with null merchant (no default value at DAO level)
        assertEquals(1, result.size)
        assertNull(result[0].merchant)
    }

    @Test
    fun getRecentTransactionsSnapshot_limitOfZeroReturnsEmpty() = runTest {
        // Given - Transactions exist
        val now = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0, date = now),
            createTestTransaction(smsId = 2L, amount = 200.0, date = now + 1000)
        )
        transactionDao.insertAll(transactions)

        // When - Limit 0
        val result = transactionDao.getRecentTransactionsSnapshot(limit = 0)

        // Then - Should return empty list
        assertTrue(result.isEmpty())
    }

    // ==================== Cross-Query Integration Tests ====================

    @Test
    fun allQueries_workIndependentlyOnSameDataset() = runTest {
        // Given - Transactions spanning multiple time periods
        val (startOfDay, endOfDay) = getTodayDateRange()
        val (startOfWeek, endOfWeek) = getWeekDateRange()
        val now = System.currentTimeMillis()

        val transactions = listOf(
            // Today's transactions
            createTestTransaction(smsId = 1L, amount = 100.0, type = TransactionType.DEBIT, date = startOfDay + 1000),
            createTestTransaction(smsId = 2L, amount = 200.0, type = TransactionType.DEBIT, date = now),
            // This week but not today
            createTestTransaction(smsId = 3L, amount = 300.0, type = TransactionType.DEBIT, date = startOfWeek + 1000),
            // Old transaction
            createTestTransaction(smsId = 4L, amount = 400.0, type = TransactionType.DEBIT, date = startOfWeek - 100000),
            // Credit transaction today
            createTestTransaction(smsId = 5L, amount = 500.0, type = TransactionType.CREDIT, date = now + 1000)
        )
        transactionDao.insertAll(transactions)

        // When - Call all three queries
        val todayTotal = transactionDao.getTodayDebitTotal(startOfDay, endOfDay)
        val weekTotal = transactionDao.getWeekDebitTotal(startOfWeek, endOfWeek)
        val recentTransactions = transactionDao.getRecentTransactionsSnapshot(limit = 5)

        // Then - Each should return correct data
        assertEquals(300.0, todayTotal, 0.01) // 100 + 200 (today's debits only)
        assertEquals(600.0, weekTotal, 0.01) // 100 + 200 + 300 (week's debits only)
        assertEquals(5, recentTransactions.size) // All 5 transactions
        // Verify recent transactions are sorted newest first
        assertEquals(5L, recentTransactions[0].smsId)
        assertEquals(4L, recentTransactions[4].smsId)
    }

    @Test
    fun allQueries_handleEmptyDatabase() = runTest {
        // Given - Empty database
        val (startOfDay, endOfDay) = getTodayDateRange()
        val (startOfWeek, endOfWeek) = getWeekDateRange()

        // When
        val todayTotal = transactionDao.getTodayDebitTotal(startOfDay, endOfDay)
        val weekTotal = transactionDao.getWeekDebitTotal(startOfWeek, endOfWeek)
        val recentTransactions = transactionDao.getRecentTransactionsSnapshot(limit = 5)

        // Then - All should return empty/zero results gracefully
        assertEquals(0.0, todayTotal, 0.01)
        assertEquals(0.0, weekTotal, 0.01)
        assertTrue(recentTransactions.isEmpty())
    }
}
