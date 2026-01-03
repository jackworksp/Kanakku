package com.example.kanakku.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.RecurringFrequency
import com.example.kanakku.data.model.RecurringTransaction
import com.example.kanakku.data.model.RecurringType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Unit tests for RecurringTransactionRepository using in-memory Room database.
 *
 * Tests cover:
 * - Recurring transaction CRUD operations
 * - Query operations (by frequency, type, upcoming, merchant pattern)
 * - Update operations (confirmation status, next expected date)
 * - Statistics operations (count, monthly total calculations)
 * - Error handling with Result types
 * - Flow operations with error catching
 * - Edge cases and boundary conditions
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecurringTransactionRepositoryTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var repository: RecurringTransactionRepository

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        repository = RecurringTransactionRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    // ==================== Helper Functions ====================

    private fun createTestRecurringTransaction(
        id: String = UUID.randomUUID().toString(),
        merchantPattern: String = "NETFLIX",
        amount: Double = 199.0,
        frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
        averageInterval: Int = 30,
        lastOccurrence: Long = System.currentTimeMillis() - 2592000000L, // 30 days ago
        nextExpected: Long = System.currentTimeMillis() + 2592000000L,   // 30 days ahead
        transactionIds: List<Long> = listOf(1L, 2L, 3L),
        isUserConfirmed: Boolean = false,
        type: RecurringType = RecurringType.SUBSCRIPTION
    ): RecurringTransaction {
        return RecurringTransaction(
            id = id,
            merchantPattern = merchantPattern,
            amount = amount,
            frequency = frequency,
            averageInterval = averageInterval,
            lastOccurrence = lastOccurrence,
            nextExpected = nextExpected,
            transactionIds = transactionIds,
            isUserConfirmed = isUserConfirmed,
            type = type
        )
    }

    // ==================== CRUD Operations Tests ====================

    @Test
    fun saveRecurringTransaction_insertsSuccessfully() = runTest {
        // Given
        val recurring = createTestRecurringTransaction()

        // When
        val result = repository.saveRecurringTransaction(recurring)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllRecurringTransactionsSnapshot().getOrNull()!!
        assertEquals(1, loaded.size)
        assertEquals(recurring.merchantPattern, loaded[0].merchantPattern)
        assertEquals(recurring.amount, loaded[0].amount, 0.01)
    }

    @Test
    fun saveRecurringTransactions_insertsBatchSuccessfully() = runTest {
        // Given
        val recurringTransactions = listOf(
            createTestRecurringTransaction(merchantPattern = "NETFLIX", amount = 199.0),
            createTestRecurringTransaction(merchantPattern = "SPOTIFY", amount = 119.0),
            createTestRecurringTransaction(merchantPattern = "ADOBE", amount = 599.0)
        )

        // When
        val result = repository.saveRecurringTransactions(recurringTransactions)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllRecurringTransactionsSnapshot().getOrNull()!!
        assertEquals(3, loaded.size)
    }

    @Test
    fun getAllRecurringTransactions_returnsFlowOfRecurringTransactions() = runTest {
        // Given
        val recurring1 = createTestRecurringTransaction(merchantPattern = "NETFLIX")
        val recurring2 = createTestRecurringTransaction(merchantPattern = "SPOTIFY")
        repository.saveRecurringTransactions(listOf(recurring1, recurring2))

        // When
        val flow = repository.getAllRecurringTransactions()
        val result = flow.first()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getAllRecurringTransactionsSnapshot_returnsResultType() = runTest {
        // Given
        val recurring = createTestRecurringTransaction()
        repository.saveRecurringTransaction(recurring)

        // When
        val result = repository.getAllRecurringTransactionsSnapshot()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun getRecurringTransactionById_returnsCorrectTransaction() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        val recurring = createTestRecurringTransaction(id = id, merchantPattern = "NETFLIX")
        repository.saveRecurringTransaction(recurring)

        // When
        val result = repository.getRecurringTransactionById(id)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("NETFLIX", result.getOrNull()?.merchantPattern)
    }

    @Test
    fun getRecurringTransactionById_returnsNullWhenNotExists() = runTest {
        // When
        val result = repository.getRecurringTransactionById("non-existent-id")

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun deleteRecurringTransaction_removesTransaction() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        val recurring = createTestRecurringTransaction(id = id)
        repository.saveRecurringTransaction(recurring)

        // When
        val deleted = repository.deleteRecurringTransaction(id).getOrNull()!!

        // Then
        assertTrue(deleted)
        assertFalse(repository.recurringTransactionExists(id).getOrNull() == true)
        assertEquals(0, repository.getRecurringTransactionCount().getOrNull())
    }

    @Test
    fun deleteRecurringTransaction_returnsFalseWhenNotExists() = runTest {
        // When
        val result = repository.deleteRecurringTransaction("non-existent-id")

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun deleteAllRecurringTransactions_removesAll() = runTest {
        // Given
        val recurring1 = createTestRecurringTransaction(merchantPattern = "NETFLIX")
        val recurring2 = createTestRecurringTransaction(merchantPattern = "SPOTIFY")
        repository.saveRecurringTransactions(listOf(recurring1, recurring2))

        // When
        val count = repository.deleteAllRecurringTransactions().getOrNull()!!

        // Then
        assertEquals(2, count)
        assertEquals(0, repository.getRecurringTransactionCount().getOrNull())
    }

    @Test
    fun deleteAllRecurringTransactions_whenEmpty() = runTest {
        // When
        val result = repository.deleteAllRecurringTransactions()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun recurringTransactionExists_returnsCorrectValue() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        val recurring = createTestRecurringTransaction(id = id)
        repository.saveRecurringTransaction(recurring)

        // When/Then
        assertTrue(repository.recurringTransactionExists(id).getOrNull() == true)
        assertFalse(repository.recurringTransactionExists("non-existent-id").getOrNull() == true)
    }

    // ==================== Query Operations Tests ====================

    @Test
    fun getRecurringTransactionsByFrequency_filtersCorrectly() = runTest {
        // Given
        val monthly1 = createTestRecurringTransaction(merchantPattern = "NETFLIX", frequency = RecurringFrequency.MONTHLY)
        val monthly2 = createTestRecurringTransaction(merchantPattern = "SPOTIFY", frequency = RecurringFrequency.MONTHLY)
        val weekly = createTestRecurringTransaction(merchantPattern = "SWIGGY", frequency = RecurringFrequency.WEEKLY)
        repository.saveRecurringTransactions(listOf(monthly1, monthly2, weekly))

        // When
        val monthlyTransactions = repository.getRecurringTransactionsByFrequency(RecurringFrequency.MONTHLY).first()
        val weeklyTransactions = repository.getRecurringTransactionsByFrequency(RecurringFrequency.WEEKLY).first()

        // Then
        assertEquals(2, monthlyTransactions.size)
        assertEquals(RecurringFrequency.MONTHLY, monthlyTransactions[0].frequency)
        assertEquals(1, weeklyTransactions.size)
        assertEquals(RecurringFrequency.WEEKLY, weeklyTransactions[0].frequency)
    }

    @Test
    fun getRecurringTransactionsByFrequency_returnsEmptyWhenNoMatches() = runTest {
        // Given
        val monthly = createTestRecurringTransaction(frequency = RecurringFrequency.MONTHLY)
        repository.saveRecurringTransaction(monthly)

        // When
        val result = repository.getRecurringTransactionsByFrequency(RecurringFrequency.ANNUAL).first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun getRecurringTransactionsByType_filtersCorrectly() = runTest {
        // Given
        val subscription1 = createTestRecurringTransaction(merchantPattern = "NETFLIX", type = RecurringType.SUBSCRIPTION)
        val subscription2 = createTestRecurringTransaction(merchantPattern = "SPOTIFY", type = RecurringType.SUBSCRIPTION)
        val emi = createTestRecurringTransaction(merchantPattern = "HDFC EMI", type = RecurringType.EMI)
        repository.saveRecurringTransactions(listOf(subscription1, subscription2, emi))

        // When
        val subscriptions = repository.getRecurringTransactionsByType(RecurringType.SUBSCRIPTION).first()
        val emis = repository.getRecurringTransactionsByType(RecurringType.EMI).first()

        // Then
        assertEquals(2, subscriptions.size)
        assertEquals(RecurringType.SUBSCRIPTION, subscriptions[0].type)
        assertEquals(1, emis.size)
        assertEquals(RecurringType.EMI, emis[0].type)
    }

    @Test
    fun getRecurringTransactionsByType_returnsEmptyWhenNoMatches() = runTest {
        // Given
        val subscription = createTestRecurringTransaction(type = RecurringType.SUBSCRIPTION)
        repository.saveRecurringTransaction(subscription)

        // When
        val result = repository.getRecurringTransactionsByType(RecurringType.SALARY).first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun getUpcomingRecurringTransactions_returnsOnlyFutureTransactions() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val upcoming = createTestRecurringTransaction(
            merchantPattern = "NETFLIX",
            nextExpected = now + 86400000L // 1 day ahead
        )
        val overdue = createTestRecurringTransaction(
            merchantPattern = "SPOTIFY",
            nextExpected = now - 86400000L // 1 day ago
        )
        repository.saveRecurringTransactions(listOf(upcoming, overdue))

        // When
        val result = repository.getUpcomingRecurringTransactions(now).first()

        // Then
        assertEquals(1, result.size)
        assertEquals("NETFLIX", result[0].merchantPattern)
    }

    @Test
    fun getUpcomingRecurringTransactions_returnsEmptyWhenNoneUpcoming() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val overdue = createTestRecurringTransaction(nextExpected = now - 86400000L)
        repository.saveRecurringTransaction(overdue)

        // When
        val result = repository.getUpcomingRecurringTransactions(now).first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun getRecurringTransactionsInWindow_filtersCorrectly() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val startWindow = now
        val endWindow = now + 2592000000L // 30 days

        val inWindow = createTestRecurringTransaction(
            merchantPattern = "NETFLIX",
            nextExpected = now + 1296000000L // 15 days ahead
        )
        val beforeWindow = createTestRecurringTransaction(
            merchantPattern = "SPOTIFY",
            nextExpected = now - 86400000L // 1 day ago
        )
        val afterWindow = createTestRecurringTransaction(
            merchantPattern = "ADOBE",
            nextExpected = now + 3456000000L // 40 days ahead
        )
        repository.saveRecurringTransactions(listOf(inWindow, beforeWindow, afterWindow))

        // When
        val result = repository.getRecurringTransactionsInWindow(startWindow, endWindow).first()

        // Then
        assertEquals(1, result.size)
        assertEquals("NETFLIX", result[0].merchantPattern)
    }

    @Test
    fun getRecurringTransactionsInWindow_returnsEmptyWhenNoneInWindow() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val recurring = createTestRecurringTransaction(nextExpected = now + 10000000000L)
        repository.saveRecurringTransaction(recurring)

        // When
        val result = repository.getRecurringTransactionsInWindow(now, now + 1000000000L).first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun getRecurringTransactionsByMerchantPattern_returnsMatches() = runTest {
        // Given
        val netflix1 = createTestRecurringTransaction(merchantPattern = "NETFLIX")
        val netflix2 = createTestRecurringTransaction(merchantPattern = "NETFLIX")
        val spotify = createTestRecurringTransaction(merchantPattern = "SPOTIFY")
        repository.saveRecurringTransactions(listOf(netflix1, netflix2, spotify))

        // When
        val result = repository.getRecurringTransactionsByMerchantPattern("NETFLIX")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertTrue(result.getOrNull()?.all { it.merchantPattern == "NETFLIX" } == true)
    }

    @Test
    fun getRecurringTransactionsByMerchantPattern_returnsEmptyWhenNoMatches() = runTest {
        // Given
        val netflix = createTestRecurringTransaction(merchantPattern = "NETFLIX")
        repository.saveRecurringTransaction(netflix)

        // When
        val result = repository.getRecurringTransactionsByMerchantPattern("AMAZON")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun getConfirmedRecurringTransactions_returnsOnlyConfirmed() = runTest {
        // Given
        val confirmed1 = createTestRecurringTransaction(merchantPattern = "NETFLIX", isUserConfirmed = true)
        val confirmed2 = createTestRecurringTransaction(merchantPattern = "SPOTIFY", isUserConfirmed = true)
        val unconfirmed = createTestRecurringTransaction(merchantPattern = "ADOBE", isUserConfirmed = false)
        repository.saveRecurringTransactions(listOf(confirmed1, confirmed2, unconfirmed))

        // When
        val result = repository.getConfirmedRecurringTransactions().first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.isUserConfirmed })
    }

    @Test
    fun getConfirmedRecurringTransactions_returnsEmptyWhenNoneConfirmed() = runTest {
        // Given
        val unconfirmed = createTestRecurringTransaction(isUserConfirmed = false)
        repository.saveRecurringTransaction(unconfirmed)

        // When
        val result = repository.getConfirmedRecurringTransactions().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun getUnconfirmedRecurringTransactions_returnsOnlyUnconfirmed() = runTest {
        // Given
        val unconfirmed1 = createTestRecurringTransaction(merchantPattern = "NETFLIX", isUserConfirmed = false)
        val unconfirmed2 = createTestRecurringTransaction(merchantPattern = "SPOTIFY", isUserConfirmed = false)
        val confirmed = createTestRecurringTransaction(merchantPattern = "ADOBE", isUserConfirmed = true)
        repository.saveRecurringTransactions(listOf(unconfirmed1, unconfirmed2, confirmed))

        // When
        val result = repository.getUnconfirmedRecurringTransactions().first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { !it.isUserConfirmed })
    }

    @Test
    fun getUnconfirmedRecurringTransactions_returnsEmptyWhenAllConfirmed() = runTest {
        // Given
        val confirmed = createTestRecurringTransaction(isUserConfirmed = true)
        repository.saveRecurringTransaction(confirmed)

        // When
        val result = repository.getUnconfirmedRecurringTransactions().first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun getNextUpcomingTransaction_returnsClosestFuture() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val farthest = createTestRecurringTransaction(
            merchantPattern = "NETFLIX",
            nextExpected = now + 2592000000L // 30 days ahead
        )
        val closest = createTestRecurringTransaction(
            merchantPattern = "SPOTIFY",
            nextExpected = now + 86400000L // 1 day ahead
        )
        repository.saveRecurringTransactions(listOf(farthest, closest))

        // When
        val result = repository.getNextUpcomingTransaction(now)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("SPOTIFY", result.getOrNull()?.merchantPattern)
    }

    @Test
    fun getNextUpcomingTransaction_returnsNullWhenNoneUpcoming() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val overdue = createTestRecurringTransaction(nextExpected = now - 86400000L)
        repository.saveRecurringTransaction(overdue)

        // When
        val result = repository.getNextUpcomingTransaction(now)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    // ==================== Update Operations Tests ====================

    @Test
    fun updateConfirmationStatus_updatesSuccessfully() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        val recurring = createTestRecurringTransaction(id = id, isUserConfirmed = false)
        repository.saveRecurringTransaction(recurring)

        // When
        val result = repository.updateConfirmationStatus(id, true)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        val updated = repository.getRecurringTransactionById(id).getOrNull()!!
        assertTrue(updated.isUserConfirmed)
    }

    @Test
    fun updateConfirmationStatus_returnsFalseWhenNotExists() = runTest {
        // When
        val result = repository.updateConfirmationStatus("non-existent-id", true)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun updateConfirmationStatus_canToggle() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        val recurring = createTestRecurringTransaction(id = id, isUserConfirmed = false)
        repository.saveRecurringTransaction(recurring)

        // When - Confirm
        repository.updateConfirmationStatus(id, true)
        val confirmed = repository.getRecurringTransactionById(id).getOrNull()!!.isUserConfirmed

        // Then
        assertTrue(confirmed)

        // When - Unconfirm
        repository.updateConfirmationStatus(id, false)
        val unconfirmed = repository.getRecurringTransactionById(id).getOrNull()!!.isUserConfirmed

        // Then
        assertFalse(unconfirmed)
    }

    @Test
    fun updateNextExpected_updatesSuccessfully() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        val oldNextExpected = System.currentTimeMillis() + 86400000L
        val recurring = createTestRecurringTransaction(id = id, nextExpected = oldNextExpected)
        repository.saveRecurringTransaction(recurring)

        // When
        val newNextExpected = System.currentTimeMillis() + 2592000000L
        val result = repository.updateNextExpected(id, newNextExpected)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        val updated = repository.getRecurringTransactionById(id).getOrNull()!!
        assertEquals(newNextExpected, updated.nextExpected)
    }

    @Test
    fun updateNextExpected_returnsFalseWhenNotExists() = runTest {
        // When
        val result = repository.updateNextExpected("non-existent-id", System.currentTimeMillis())

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    // ==================== Statistics Operations Tests ====================

    @Test
    fun getRecurringTransactionCount_returnsCorrectCount() = runTest {
        // Given
        val recurring1 = createTestRecurringTransaction()
        val recurring2 = createTestRecurringTransaction()
        val recurring3 = createTestRecurringTransaction()
        repository.saveRecurringTransactions(listOf(recurring1, recurring2, recurring3))

        // When
        val result = repository.getRecurringTransactionCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun getRecurringTransactionCount_returnsZeroWhenEmpty() = runTest {
        // When
        val result = repository.getRecurringTransactionCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun getConfirmedRecurringTransactionCount_returnsCorrectCount() = runTest {
        // Given
        val confirmed1 = createTestRecurringTransaction(isUserConfirmed = true)
        val confirmed2 = createTestRecurringTransaction(isUserConfirmed = true)
        val unconfirmed = createTestRecurringTransaction(isUserConfirmed = false)
        repository.saveRecurringTransactions(listOf(confirmed1, confirmed2, unconfirmed))

        // When
        val result = repository.getConfirmedRecurringTransactionCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun getConfirmedRecurringTransactionCount_returnsZeroWhenNoneConfirmed() = runTest {
        // Given
        val unconfirmed = createTestRecurringTransaction(isUserConfirmed = false)
        repository.saveRecurringTransaction(unconfirmed)

        // When
        val result = repository.getConfirmedRecurringTransactionCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun calculateMonthlyRecurringTotal_calculatesWeeklyCorrectly() = runTest {
        // Given - Weekly recurring of 100
        val weekly = createTestRecurringTransaction(
            amount = 100.0,
            frequency = RecurringFrequency.WEEKLY
        )
        repository.saveRecurringTransaction(weekly)

        // When
        val result = repository.calculateMonthlyRecurringTotal()

        // Then
        assertTrue(result.isSuccess)
        // Weekly to monthly: 100 * 4.33 = 433
        assertEquals(433.0, result.getOrNull()!!, 1.0)
    }

    @Test
    fun calculateMonthlyRecurringTotal_calculatesBiWeeklyCorrectly() = runTest {
        // Given - Bi-weekly recurring of 100
        val biWeekly = createTestRecurringTransaction(
            amount = 100.0,
            frequency = RecurringFrequency.BI_WEEKLY
        )
        repository.saveRecurringTransaction(biWeekly)

        // When
        val result = repository.calculateMonthlyRecurringTotal()

        // Then
        assertTrue(result.isSuccess)
        // Bi-weekly to monthly: 100 * 2.17 = 217
        assertEquals(217.0, result.getOrNull()!!, 1.0)
    }

    @Test
    fun calculateMonthlyRecurringTotal_calculatesMonthlyCorrectly() = runTest {
        // Given - Monthly recurring of 100
        val monthly = createTestRecurringTransaction(
            amount = 100.0,
            frequency = RecurringFrequency.MONTHLY
        )
        repository.saveRecurringTransaction(monthly)

        // When
        val result = repository.calculateMonthlyRecurringTotal()

        // Then
        assertTrue(result.isSuccess)
        // Monthly stays same: 100
        assertEquals(100.0, result.getOrNull()!!, 0.01)
    }

    @Test
    fun calculateMonthlyRecurringTotal_calculatesQuarterlyCorrectly() = runTest {
        // Given - Quarterly recurring of 300
        val quarterly = createTestRecurringTransaction(
            amount = 300.0,
            frequency = RecurringFrequency.QUARTERLY
        )
        repository.saveRecurringTransaction(quarterly)

        // When
        val result = repository.calculateMonthlyRecurringTotal()

        // Then
        assertTrue(result.isSuccess)
        // Quarterly to monthly: 300 / 3 = 100
        assertEquals(100.0, result.getOrNull()!!, 0.01)
    }

    @Test
    fun calculateMonthlyRecurringTotal_calculatesAnnualCorrectly() = runTest {
        // Given - Annual recurring of 1200
        val annual = createTestRecurringTransaction(
            amount = 1200.0,
            frequency = RecurringFrequency.ANNUAL
        )
        repository.saveRecurringTransaction(annual)

        // When
        val result = repository.calculateMonthlyRecurringTotal()

        // Then
        assertTrue(result.isSuccess)
        // Annual to monthly: 1200 / 12 = 100
        assertEquals(100.0, result.getOrNull()!!, 0.01)
    }

    @Test
    fun calculateMonthlyRecurringTotal_sumsMultipleFrequencies() = runTest {
        // Given
        val weekly = createTestRecurringTransaction(amount = 100.0, frequency = RecurringFrequency.WEEKLY)      // 433
        val biWeekly = createTestRecurringTransaction(amount = 200.0, frequency = RecurringFrequency.BI_WEEKLY) // 434
        val monthly = createTestRecurringTransaction(amount = 500.0, frequency = RecurringFrequency.MONTHLY)    // 500
        val quarterly = createTestRecurringTransaction(amount = 300.0, frequency = RecurringFrequency.QUARTERLY)// 100
        val annual = createTestRecurringTransaction(amount = 1200.0, frequency = RecurringFrequency.ANNUAL)     // 100
        repository.saveRecurringTransactions(listOf(weekly, biWeekly, monthly, quarterly, annual))

        // When
        val result = repository.calculateMonthlyRecurringTotal()

        // Then
        assertTrue(result.isSuccess)
        // Total: 433 + 434 + 500 + 100 + 100 = 1567
        assertEquals(1567.0, result.getOrNull()!!, 5.0)
    }

    @Test
    fun calculateMonthlyRecurringTotal_returnsZeroWhenEmpty() = runTest {
        // When
        val result = repository.calculateMonthlyRecurringTotal()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0.0, result.getOrNull()!!, 0.01)
    }

    @Test
    fun calculateConfirmedMonthlyRecurringTotal_onlyCountsConfirmed() = runTest {
        // Given
        val confirmed1 = createTestRecurringTransaction(amount = 100.0, frequency = RecurringFrequency.MONTHLY, isUserConfirmed = true)
        val confirmed2 = createTestRecurringTransaction(amount = 200.0, frequency = RecurringFrequency.MONTHLY, isUserConfirmed = true)
        val unconfirmed = createTestRecurringTransaction(amount = 1000.0, frequency = RecurringFrequency.MONTHLY, isUserConfirmed = false)
        repository.saveRecurringTransactions(listOf(confirmed1, confirmed2, unconfirmed))

        // When
        val result = repository.calculateConfirmedMonthlyRecurringTotal()

        // Then
        assertTrue(result.isSuccess)
        // Only confirmed: 100 + 200 = 300 (unconfirmed 1000 is excluded)
        assertEquals(300.0, result.getOrNull()!!, 0.01)
    }

    @Test
    fun calculateConfirmedMonthlyRecurringTotal_returnsZeroWhenNoneConfirmed() = runTest {
        // Given
        val unconfirmed = createTestRecurringTransaction(amount = 1000.0, isUserConfirmed = false)
        repository.saveRecurringTransaction(unconfirmed)

        // When
        val result = repository.calculateConfirmedMonthlyRecurringTotal()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0.0, result.getOrNull()!!, 0.01)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun saveRecurringTransaction_handlesErrorGracefully() = runTest {
        // Given
        database.close() // Close database to simulate error

        // When
        val result = repository.saveRecurringTransaction(createTestRecurringTransaction())

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun saveRecurringTransactions_returnsResultType() = runTest {
        // Given
        val recurring = createTestRecurringTransaction()

        // When
        val result = repository.saveRecurringTransactions(listOf(recurring))

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun getAllRecurringTransactionsSnapshot_handlesErrorGracefully() = runTest {
        // Given
        database.close() // Close database to simulate error

        // When
        val result = repository.getAllRecurringTransactionsSnapshot()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun getRecurringTransactionById_returnsResultType() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        val recurring = createTestRecurringTransaction(id = id)
        repository.saveRecurringTransaction(recurring)

        // When
        val result = repository.getRecurringTransactionById(id)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun getRecurringTransactionById_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getRecurringTransactionById("any-id")

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun deleteRecurringTransaction_returnsResultType() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        val recurring = createTestRecurringTransaction(id = id)
        repository.saveRecurringTransaction(recurring)

        // When
        val result = repository.deleteRecurringTransaction(id)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun deleteRecurringTransaction_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.deleteRecurringTransaction("any-id")

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun deleteAllRecurringTransactions_returnsResultType() = runTest {
        // Given
        repository.saveRecurringTransaction(createTestRecurringTransaction())

        // When
        val result = repository.deleteAllRecurringTransactions()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun recurringTransactionExists_returnsResultType() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        repository.saveRecurringTransaction(createTestRecurringTransaction(id = id))

        // When
        val result = repository.recurringTransactionExists(id)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun recurringTransactionExists_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.recurringTransactionExists("any-id")

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun getRecurringTransactionsByMerchantPattern_returnsResultType() = runTest {
        // Given
        repository.saveRecurringTransaction(createTestRecurringTransaction(merchantPattern = "NETFLIX"))

        // When
        val result = repository.getRecurringTransactionsByMerchantPattern("NETFLIX")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun getRecurringTransactionsByMerchantPattern_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getRecurringTransactionsByMerchantPattern("NETFLIX")

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun getNextUpcomingTransaction_returnsResultType() = runTest {
        // Given
        val now = System.currentTimeMillis()
        repository.saveRecurringTransaction(createTestRecurringTransaction(nextExpected = now + 86400000L))

        // When
        val result = repository.getNextUpcomingTransaction(now)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun getNextUpcomingTransaction_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getNextUpcomingTransaction()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun updateConfirmationStatus_returnsResultType() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        repository.saveRecurringTransaction(createTestRecurringTransaction(id = id))

        // When
        val result = repository.updateConfirmationStatus(id, true)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun updateConfirmationStatus_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.updateConfirmationStatus("any-id", true)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun updateNextExpected_returnsResultType() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        repository.saveRecurringTransaction(createTestRecurringTransaction(id = id))

        // When
        val result = repository.updateNextExpected(id, System.currentTimeMillis())

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun updateNextExpected_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.updateNextExpected("any-id", System.currentTimeMillis())

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun getRecurringTransactionCount_returnsResultType() = runTest {
        // Given
        repository.saveRecurringTransaction(createTestRecurringTransaction())

        // When
        val result = repository.getRecurringTransactionCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun getRecurringTransactionCount_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getRecurringTransactionCount()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun getConfirmedRecurringTransactionCount_returnsResultType() = runTest {
        // Given
        repository.saveRecurringTransaction(createTestRecurringTransaction(isUserConfirmed = true))

        // When
        val result = repository.getConfirmedRecurringTransactionCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun getConfirmedRecurringTransactionCount_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getConfirmedRecurringTransactionCount()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun calculateMonthlyRecurringTotal_returnsResultType() = runTest {
        // Given
        repository.saveRecurringTransaction(createTestRecurringTransaction(amount = 100.0))

        // When
        val result = repository.calculateMonthlyRecurringTotal()

        // Then
        assertTrue(result.isSuccess)
        assertTrue((result.getOrNull() ?: 0.0) > 0.0)
    }

    @Test
    fun calculateMonthlyRecurringTotal_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.calculateMonthlyRecurringTotal()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun calculateConfirmedMonthlyRecurringTotal_returnsResultType() = runTest {
        // Given
        repository.saveRecurringTransaction(createTestRecurringTransaction(amount = 100.0, isUserConfirmed = true))

        // When
        val result = repository.calculateConfirmedMonthlyRecurringTotal()

        // Then
        assertTrue(result.isSuccess)
        assertTrue((result.getOrNull() ?: 0.0) > 0.0)
    }

    @Test
    fun calculateConfirmedMonthlyRecurringTotal_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.calculateConfirmedMonthlyRecurringTotal()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun flowOperations_catchErrors() = runTest {
        // Given
        repository.saveRecurringTransaction(createTestRecurringTransaction())

        // When - Get flow (should not throw even if there are errors)
        val flow = repository.getAllRecurringTransactions()
        val result = flow.first()

        // Then
        assertEquals(1, result.size)
    }

    @Test
    fun flowOperations_withClosedDatabase() = runTest {
        // Given
        database.close()

        // When/Then - Flow should handle error
        val flow = repository.getAllRecurringTransactions()
        val result = flow.first()

        // Should return empty on error (catch block)
        assertTrue(result.isEmpty())
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun saveRecurringTransaction_withZeroAmount() = runTest {
        // Given
        val recurring = createTestRecurringTransaction(amount = 0.0)

        // When
        val result = repository.saveRecurringTransaction(recurring)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllRecurringTransactionsSnapshot().getOrNull()
        assertEquals(0.0, loaded?.first()?.amount ?: -1.0, 0.001)
    }

    @Test
    fun saveRecurringTransaction_withVeryLargeAmount() = runTest {
        // Given
        val largeAmount = 999999.99
        val recurring = createTestRecurringTransaction(amount = largeAmount)

        // When
        val result = repository.saveRecurringTransaction(recurring)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllRecurringTransactionsSnapshot().getOrNull()
        assertEquals(largeAmount, loaded?.first()?.amount ?: 0.0, 0.01)
    }

    @Test
    fun saveRecurringTransaction_withEmptyMerchantPattern() = runTest {
        // Given
        val recurring = createTestRecurringTransaction(merchantPattern = "")

        // When
        val result = repository.saveRecurringTransaction(recurring)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllRecurringTransactionsSnapshot().getOrNull()
        assertEquals("", loaded?.first()?.merchantPattern)
    }

    @Test
    fun saveRecurringTransaction_withVeryLongMerchantPattern() = runTest {
        // Given
        val longMerchant = "A".repeat(500)
        val recurring = createTestRecurringTransaction(merchantPattern = longMerchant)

        // When
        val result = repository.saveRecurringTransaction(recurring)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllRecurringTransactionsSnapshot().getOrNull()
        assertEquals(longMerchant, loaded?.first()?.merchantPattern)
    }

    @Test
    fun saveRecurringTransaction_withEmptyTransactionIds() = runTest {
        // Given
        val recurring = createTestRecurringTransaction(transactionIds = emptyList())

        // When
        val result = repository.saveRecurringTransaction(recurring)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllRecurringTransactionsSnapshot().getOrNull()
        assertTrue(loaded?.first()?.transactionIds?.isEmpty() == true)
    }

    @Test
    fun saveRecurringTransaction_withManyTransactionIds() = runTest {
        // Given
        val manyIds = (1L..100L).toList()
        val recurring = createTestRecurringTransaction(transactionIds = manyIds)

        // When
        val result = repository.saveRecurringTransaction(recurring)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllRecurringTransactionsSnapshot().getOrNull()
        assertEquals(100, loaded?.first()?.transactionIds?.size)
    }

    @Test
    fun saveRecurringTransaction_duplicateId_replacesExisting() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        val recurring1 = createTestRecurringTransaction(id = id, amount = 100.0)
        val recurring2 = createTestRecurringTransaction(id = id, amount = 200.0)

        // When - Save same ID twice
        repository.saveRecurringTransaction(recurring1)
        repository.saveRecurringTransaction(recurring2)

        // Then - Should have only one (replaced)
        val loaded = repository.getAllRecurringTransactionsSnapshot().getOrNull()
        assertEquals(1, loaded?.size)
        assertEquals(200.0, loaded?.first()?.amount ?: 0.0, 0.01)
    }

    @Test
    fun saveRecurringTransactions_withEmptyList() = runTest {
        // Given
        val emptyList = emptyList<RecurringTransaction>()

        // When
        val result = repository.saveRecurringTransactions(emptyList)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, repository.getRecurringTransactionCount().getOrNull())
    }

    @Test
    fun saveRecurringTransactions_withLargeBatch() = runTest {
        // Given
        val largeBatch = (1..100).map {
            createTestRecurringTransaction(merchantPattern = "MERCHANT_$it")
        }

        // When
        val result = repository.saveRecurringTransactions(largeBatch)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(100, repository.getRecurringTransactionCount().getOrNull())
    }

    @Test
    fun getRecurringTransactionsInWindow_withSameStartAndEnd() = runTest {
        // Given
        val exactTime = System.currentTimeMillis()
        val recurring = createTestRecurringTransaction(nextExpected = exactTime)
        repository.saveRecurringTransaction(recurring)

        // When
        val result = repository.getRecurringTransactionsInWindow(exactTime, exactTime).first()

        // Then - Should return transaction at exact time
        assertEquals(1, result.size)
    }

    @Test
    fun getRecurringTransactionsInWindow_withInvertedRange() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val future = now + 86400000L
        repository.saveRecurringTransaction(createTestRecurringTransaction(nextExpected = now))

        // When - Query with inverted range
        val result = repository.getRecurringTransactionsInWindow(future, now).first()

        // Then - Should return empty or handle gracefully
        assertTrue(result.isEmpty())
    }

    @Test
    fun calculateMonthlyRecurringTotal_withMixedTypes() = runTest {
        // Given - Different types should all be counted
        val subscription = createTestRecurringTransaction(amount = 100.0, frequency = RecurringFrequency.MONTHLY, type = RecurringType.SUBSCRIPTION)
        val emi = createTestRecurringTransaction(amount = 200.0, frequency = RecurringFrequency.MONTHLY, type = RecurringType.EMI)
        val salary = createTestRecurringTransaction(amount = 300.0, frequency = RecurringFrequency.MONTHLY, type = RecurringType.SALARY)
        repository.saveRecurringTransactions(listOf(subscription, emi, salary))

        // When
        val result = repository.calculateMonthlyRecurringTotal()

        // Then - All types should be summed
        assertTrue(result.isSuccess)
        assertEquals(600.0, result.getOrNull()!!, 0.01)
    }

    @Test
    fun updateConfirmationStatus_multipleUpdatesRapidly() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        repository.saveRecurringTransaction(createTestRecurringTransaction(id = id, isUserConfirmed = false))

        // When - Multiple rapid updates
        repository.updateConfirmationStatus(id, true)
        repository.updateConfirmationStatus(id, false)
        repository.updateConfirmationStatus(id, true)
        val result = repository.updateConfirmationStatus(id, false)

        // Then - Last update should win
        assertTrue(result.isSuccess)
        val updated = repository.getRecurringTransactionById(id).getOrNull()!!
        assertFalse(updated.isUserConfirmed)
    }

    @Test
    fun updateNextExpected_multipleUpdates() = runTest {
        // Given
        val id = UUID.randomUUID().toString()
        repository.saveRecurringTransaction(createTestRecurringTransaction(id = id))

        // When - Multiple updates
        val time1 = System.currentTimeMillis() + 1000000L
        val time2 = System.currentTimeMillis() + 2000000L
        val time3 = System.currentTimeMillis() + 3000000L

        repository.updateNextExpected(id, time1)
        repository.updateNextExpected(id, time2)
        val result = repository.updateNextExpected(id, time3)

        // Then - Last update should win
        assertTrue(result.isSuccess)
        val updated = repository.getRecurringTransactionById(id).getOrNull()!!
        assertEquals(time3, updated.nextExpected)
    }

    @Test
    fun fullWorkflow_saveLoadUpdateDelete() = runTest {
        // 1. Save recurring transaction
        val id = UUID.randomUUID().toString()
        val recurring = createTestRecurringTransaction(id = id, amount = 199.0, isUserConfirmed = false)
        repository.saveRecurringTransaction(recurring)
        assertEquals(1, repository.getRecurringTransactionCount().getOrNull())

        // 2. Update confirmation status
        repository.updateConfirmationStatus(id, true)
        assertTrue(repository.getRecurringTransactionById(id).getOrNull()?.isUserConfirmed == true)

        // 3. Update next expected date
        val newNextExpected = System.currentTimeMillis() + 5000000L
        repository.updateNextExpected(id, newNextExpected)
        assertEquals(newNextExpected, repository.getRecurringTransactionById(id).getOrNull()?.nextExpected)

        // 4. Load and verify
        val loaded = repository.getAllRecurringTransactionsSnapshot().getOrNull()!!.first()
        assertEquals(recurring.amount, loaded.amount, 0.01)
        assertTrue(loaded.isUserConfirmed)

        // 5. Delete recurring transaction
        repository.deleteRecurringTransaction(id)
        assertEquals(0, repository.getRecurringTransactionCount().getOrNull())
    }

    @Test
    fun concurrentOperations_maintainDataIntegrity() = runTest {
        // Given - Multiple operations
        val recurringTransactions = (1..10).map { i ->
            createTestRecurringTransaction(
                merchantPattern = "MERCHANT_$i",
                amount = i * 100.0
            )
        }

        // When - Batch save and individual queries
        repository.saveRecurringTransactions(recurringTransactions)
        recurringTransactions.forEach { recurring ->
            repository.updateConfirmationStatus(recurring.id, i % 2 == 0)
        }

        // Then - All data should be consistent
        assertEquals(10, repository.getRecurringTransactionCount().getOrNull())
        val allRecurring = repository.getAllRecurringTransactionsSnapshot().getOrNull()
        assertEquals(10, allRecurring?.size)
    }

    @Test
    fun allFrequencies_supportedCorrectly() = runTest {
        // Given - One of each frequency
        val frequencies = RecurringFrequency.values().map { frequency ->
            createTestRecurringTransaction(
                merchantPattern = "MERCHANT_${frequency.name}",
                frequency = frequency
            )
        }
        repository.saveRecurringTransactions(frequencies)

        // When/Then - Each frequency can be queried
        RecurringFrequency.values().forEach { frequency ->
            val result = repository.getRecurringTransactionsByFrequency(frequency).first()
            assertEquals(1, result.size)
            assertEquals(frequency, result[0].frequency)
        }
    }

    @Test
    fun allTypes_supportedCorrectly() = runTest {
        // Given - One of each type
        val types = RecurringType.values().map { type ->
            createTestRecurringTransaction(
                merchantPattern = "MERCHANT_${type.name}",
                type = type
            )
        }
        repository.saveRecurringTransactions(types)

        // When/Then - Each type can be queried
        RecurringType.values().forEach { type ->
            val result = repository.getRecurringTransactionsByType(type).first()
            assertEquals(1, result.size)
            assertEquals(type, result[0].type)
        }
    }
}
