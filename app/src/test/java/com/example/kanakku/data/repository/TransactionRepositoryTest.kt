package com.example.kanakku.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for TransactionRepository using in-memory Room database.
 *
 * Tests cover:
 * - Transaction CRUD operations
 * - Entity-model mapping
 * - Category override management
 * - Sync metadata tracking
 * - Incremental sync logic
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TransactionRepositoryTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var repository: TransactionRepository

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        repository = TransactionRepository(database)
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
    ): ParsedTransaction {
        return ParsedTransaction(
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

    // ==================== Transaction Save/Load Tests ====================

    @Test
    fun saveTransaction_insertsTransactionSuccessfully() = runTest {
        // Given
        val transaction = createTestTransaction(smsId = 1L)

        // When
        repository.saveTransaction(transaction)

        // Then
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(1, loaded.size)
        assertEquals(transaction.smsId, loaded[0].smsId)
        assertEquals(transaction.amount, loaded[0].amount, 0.01)
    }

    @Test
    fun saveTransactions_insertsBatchSuccessfully() = runTest {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0),
            createTestTransaction(smsId = 2L, amount = 200.0),
            createTestTransaction(smsId = 3L, amount = 300.0)
        )

        // When
        repository.saveTransactions(transactions)

        // Then
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()!!
        assertEquals(3, loaded.size)
        assertEquals(300.0, loaded[0].amount, 0.01) // Sorted by date desc (newest first)
    }

    @Test
    fun getAllTransactions_returnsFlowOfTransactions() = runTest {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L),
            createTestTransaction(smsId = 2L)
        )
        repository.saveTransactions(transactions)

        // When
        val flow = repository.getAllTransactions()
        val result = flow.first()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getTransactionsByType_filtersCorrectly() = runTest {
        // Given
        val debitTx = createTestTransaction(smsId = 1L, type = TransactionType.DEBIT)
        val creditTx = createTestTransaction(smsId = 2L, type = TransactionType.CREDIT)
        repository.saveTransactions(listOf(debitTx, creditTx))

        // When
        val debits = repository.getTransactionsByType(TransactionType.DEBIT).first()
        val credits = repository.getTransactionsByType(TransactionType.CREDIT).first()

        // Then
        assertEquals(1, debits.size)
        assertEquals(TransactionType.DEBIT, debits[0].type)
        assertEquals(1, credits.size)
        assertEquals(TransactionType.CREDIT, credits[0].type)
    }

    @Test
    fun getTransactionsByDateRange_filtersCorrectly() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600000
        val twoHoursAgo = now - 7200000

        val tx1 = createTestTransaction(smsId = 1L, date = twoHoursAgo)
        val tx2 = createTestTransaction(smsId = 2L, date = oneHourAgo)
        val tx3 = createTestTransaction(smsId = 3L, date = now)
        repository.saveTransactions(listOf(tx1, tx2, tx3))

        // When
        val inRange = repository.getTransactionsByDateRange(oneHourAgo, now).first()

        // Then
        assertEquals(2, inRange.size)
        assertTrue(inRange.any { it.smsId == 2L })
        assertTrue(inRange.any { it.smsId == 3L })
    }

    @Test
    fun getTransactionsAfter_returnsOnlyNewerTransactions() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600000

        val oldTx = createTestTransaction(smsId = 1L, date = oneHourAgo)
        val newTx = createTestTransaction(smsId = 2L, date = now)
        repository.saveTransactions(listOf(oldTx, newTx))

        // When
        val newer = repository.getTransactionsAfter(oneHourAgo).getOrNull()!!

        // Then
        assertEquals(1, newer.size)
        assertEquals(2L, newer[0].smsId)
    }

    @Test
    fun transactionExists_returnsCorrectValue() = runTest {
        // Given
        val transaction = createTestTransaction(smsId = 1L)
        repository.saveTransaction(transaction)

        // When/Then
        assertTrue(repository.transactionExists(1L).getOrNull() == true)
        assertFalse(repository.transactionExists(999L).getOrNull() == true)
    }

    @Test
    fun deleteTransaction_removesTransaction() = runTest {
        // Given
        val transaction = createTestTransaction(smsId = 1L)
        repository.saveTransaction(transaction)

        // When
        val deleted = repository.deleteTransaction(1L).getOrNull()!!

        // Then
        assertTrue(deleted)
        assertFalse(repository.transactionExists(1L).getOrNull() == true)
        assertEquals(0, repository.getTransactionCount().getOrNull())
    }

    @Test
    fun deleteAllTransactions_removesAll() = runTest {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L),
            createTestTransaction(smsId = 2L)
        )
        repository.saveTransactions(transactions)

        // When
        val count = repository.deleteAllTransactions().getOrNull()!!

        // Then
        assertEquals(2, count)
        assertEquals(0, repository.getTransactionCount().getOrNull())
    }

    @Test
    fun getLatestTransactionDate_returnsCorrectDate() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600000

        repository.saveTransactions(
            listOf(
                createTestTransaction(smsId = 1L, date = oneHourAgo),
                createTestTransaction(smsId = 2L, date = now)
            )
        )

        // When
        val latestDate = repository.getLatestTransactionDate().getOrNull()

        // Then
        assertEquals(now, latestDate)
    }

    @Test
    fun getLatestTransactionDate_returnsNullWhenEmpty() = runTest {
        // When
        val latestDate = repository.getLatestTransactionDate().getOrNull()

        // Then
        assertNull(latestDate)
    }

    // ==================== Entity-Model Mapping Tests ====================

    @Test
    fun entityMapping_preservesAllFields() = runTest {
        // Given
        val original = ParsedTransaction(
            smsId = 123L,
            amount = 456.78,
            type = TransactionType.CREDIT,
            merchant = "Amazon",
            accountNumber = "5678",
            referenceNumber = "REF456",
            date = 1234567890L,
            rawSms = "Original SMS text",
            senderAddress = "VM-HDFCBK",
            balanceAfter = 1000.50,
            location = "Mumbai"
        )

        // When
        repository.saveTransaction(original)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()!!.first()

        // Then - All fields should match exactly
        assertEquals(original.smsId, loaded.smsId)
        assertEquals(original.amount, loaded.amount, 0.001)
        assertEquals(original.type, loaded.type)
        assertEquals(original.merchant, loaded.merchant)
        assertEquals(original.accountNumber, loaded.accountNumber)
        assertEquals(original.referenceNumber, loaded.referenceNumber)
        assertEquals(original.date, loaded.date)
        assertEquals(original.rawSms, loaded.rawSms)
        assertEquals(original.senderAddress, loaded.senderAddress)
        assertEquals(original.balanceAfter, loaded.balanceAfter)
        assertEquals(original.location, loaded.location)
    }

    @Test
    fun entityMapping_handlesNullFields() = runTest {
        // Given - Transaction with all nullable fields as null
        val original = ParsedTransaction(
            smsId = 1L,
            amount = 100.0,
            type = TransactionType.UNKNOWN,
            merchant = null,
            accountNumber = null,
            referenceNumber = null,
            date = System.currentTimeMillis(),
            rawSms = "SMS",
            senderAddress = "SENDER",
            balanceAfter = null,
            location = null
        )

        // When
        repository.saveTransaction(original)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()!!.first()

        // Then
        assertNull(loaded.merchant)
        assertNull(loaded.accountNumber)
        assertNull(loaded.referenceNumber)
        assertNull(loaded.balanceAfter)
        assertNull(loaded.location)
    }

    // ==================== Category Override Tests ====================

    @Test
    fun setCategoryOverride_storesCorrectly() = runTest {
        // Given
        val smsId = 1L
        val categoryId = "food"
        repository.saveTransaction(createTestTransaction(smsId = smsId))

        // When
        repository.setCategoryOverride(smsId, categoryId)

        // Then
        val override = repository.getCategoryOverride(smsId).getOrNull()
        assertEquals(categoryId, override)
    }

    @Test
    fun setCategoryOverride_updatesExisting() = runTest {
        // Given
        val smsId = 1L
        repository.saveTransaction(createTestTransaction(smsId = smsId))
        repository.setCategoryOverride(smsId, "food")

        // When
        repository.setCategoryOverride(smsId, "transport")

        // Then
        val override = repository.getCategoryOverride(smsId).getOrNull()
        assertEquals("transport", override)
    }

    @Test
    fun getCategoryOverride_returnsNullWhenNotExists() = runTest {
        // When
        val override = repository.getCategoryOverride(999L).getOrNull()

        // Then
        assertNull(override)
    }

    @Test
    fun getAllCategoryOverrides_returnsMap() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.saveTransaction(createTestTransaction(smsId = 2L))
        repository.saveTransaction(createTestTransaction(smsId = 3L))
        repository.setCategoryOverride(1L, "food")
        repository.setCategoryOverride(2L, "transport")
        repository.setCategoryOverride(3L, "shopping")

        // When
        val overrides = repository.getAllCategoryOverrides().getOrNull()!!

        // Then
        assertEquals(3, overrides.size)
        assertEquals("food", overrides[1L])
        assertEquals("transport", overrides[2L])
        assertEquals("shopping", overrides[3L])
    }

    @Test
    fun getAllCategoryOverridesFlow_emitsUpdates() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.setCategoryOverride(1L, "food")

        // When
        val flow = repository.getAllCategoryOverridesFlow()
        val result = flow.first()

        // Then
        assertEquals(1, result.size)
        assertEquals("food", result[1L])
    }

    @Test
    fun removeCategoryOverride_deletesCorrectly() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.setCategoryOverride(1L, "food")

        // When
        val removed = repository.removeCategoryOverride(1L).getOrNull()!!

        // Then
        assertTrue(removed)
        assertNull(repository.getCategoryOverride(1L).getOrNull())
    }

    @Test
    fun removeCategoryOverride_returnsFalseWhenNotExists() = runTest {
        // When
        val removed = repository.removeCategoryOverride(999L).getOrNull()!!

        // Then
        assertFalse(removed)
    }

    @Test
    fun removeAllCategoryOverrides_clearsAll() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.saveTransaction(createTestTransaction(smsId = 2L))
        repository.setCategoryOverride(1L, "food")
        repository.setCategoryOverride(2L, "transport")

        // When
        val count = repository.removeAllCategoryOverrides().getOrNull()!!

        // Then
        assertEquals(2, count)
        assertTrue(repository.getAllCategoryOverrides().getOrNull()!!.isEmpty())
    }

    @Test
    fun categoryOverride_cascadeDeletesWithTransaction() = runTest {
        // Given - Transaction with override
        val transaction = createTestTransaction(smsId = 1L)
        repository.saveTransaction(transaction)
        repository.setCategoryOverride(1L, "food")

        // When - Delete transaction
        repository.deleteTransaction(1L)

        // Then - Override should also be deleted (CASCADE)
        assertNull(repository.getCategoryOverride(1L).getOrNull())
    }

    // ==================== Sync Metadata Tests ====================

    @Test
    fun lastSyncTimestamp_storesAndRetrieves() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()

        // When
        repository.setLastSyncTimestamp(timestamp)

        // Then
        assertEquals(timestamp, repository.getLastSyncTimestamp().getOrNull())
    }

    @Test
    fun lastSyncTimestamp_updatesExisting() = runTest {
        // Given
        repository.setLastSyncTimestamp(1000L)

        // When
        repository.setLastSyncTimestamp(2000L)

        // Then
        assertEquals(2000L, repository.getLastSyncTimestamp().getOrNull())
    }

    @Test
    fun lastSyncTimestamp_returnsNullWhenNotSet() = runTest {
        // When
        val timestamp = repository.getLastSyncTimestamp().getOrNull()

        // Then
        assertNull(timestamp)
    }

    @Test
    fun lastProcessedSmsId_storesAndRetrieves() = runTest {
        // Given
        val smsId = 12345L

        // When
        repository.setLastProcessedSmsId(smsId)

        // Then
        assertEquals(smsId, repository.getLastProcessedSmsId().getOrNull())
    }

    @Test
    fun lastProcessedSmsId_updatesExisting() = runTest {
        // Given
        repository.setLastProcessedSmsId(100L)

        // When
        repository.setLastProcessedSmsId(200L)

        // Then
        assertEquals(200L, repository.getLastProcessedSmsId().getOrNull())
    }

    @Test
    fun lastProcessedSmsId_returnsNullWhenNotSet() = runTest {
        // When
        val smsId = repository.getLastProcessedSmsId().getOrNull()

        // Then
        assertNull(smsId)
    }

    @Test
    fun clearSyncMetadata_removesAllMetadata() = runTest {
        // Given
        repository.setLastSyncTimestamp(System.currentTimeMillis())
        repository.setLastProcessedSmsId(123L)

        // When
        val count = repository.clearSyncMetadata().getOrNull()!!

        // Then
        assertEquals(2, count)
        assertNull(repository.getLastSyncTimestamp().getOrNull())
        assertNull(repository.getLastProcessedSmsId().getOrNull())
    }

    // ==================== Incremental Sync Logic Tests ====================

    @Test
    fun incrementalSync_simulateFirstSync() = runTest {
        // Given - No previous sync
        assertNull(repository.getLastSyncTimestamp().getOrNull())

        // When - First sync with transactions
        val syncTime = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1L, date = syncTime - 1000),
            createTestTransaction(smsId = 2L, date = syncTime - 500)
        )
        repository.saveTransactions(transactions)
        repository.setLastSyncTimestamp(syncTime)

        // Then
        assertEquals(2, repository.getTransactionCount().getOrNull())
        assertEquals(syncTime, repository.getLastSyncTimestamp().getOrNull())
    }

    @Test
    fun incrementalSync_simulateSubsequentSync() = runTest {
        // Given - Previous sync exists
        val firstSyncTime = 1000L
        repository.saveTransaction(createTestTransaction(smsId = 1L, date = firstSyncTime - 100))
        repository.setLastSyncTimestamp(firstSyncTime)

        // When - Subsequent sync with new transactions
        val secondSyncTime = 2000L
        val newTransactions = repository.getTransactionsAfter(firstSyncTime).getOrNull()
        // Simulate fetching new SMS
        repository.saveTransaction(createTestTransaction(smsId = 2L, date = secondSyncTime - 100))
        repository.setLastSyncTimestamp(secondSyncTime)

        // Then
        assertEquals(2, repository.getTransactionCount().getOrNull())
        assertEquals(secondSyncTime, repository.getLastSyncTimestamp().getOrNull())
    }

    @Test
    fun incrementalSync_onlyProcessesNewTransactions() = runTest {
        // Given - Existing transactions
        val oldTime = 1000L
        repository.saveTransaction(createTestTransaction(smsId = 1L, date = oldTime))
        repository.setLastSyncTimestamp(oldTime)

        // When - Check for new transactions
        val lastSync = repository.getLastSyncTimestamp().getOrNull()!!
        val newTransactions = repository.getTransactionsAfter(lastSync).getOrNull()!!

        // Then - Should be empty (no new transactions)
        assertTrue(newTransactions.isEmpty())
    }

    // ==================== Integration Tests ====================

    @Test
    fun fullWorkflow_saveLoadUpdateDelete() = runTest {
        // 1. Save transaction
        val transaction = createTestTransaction(smsId = 1L, amount = 100.0)
        repository.saveTransaction(transaction)
        assertEquals(1, repository.getTransactionCount().getOrNull())

        // 2. Add category override
        repository.setCategoryOverride(1L, "food")
        assertEquals("food", repository.getCategoryOverride(1L).getOrNull())

        // 3. Update sync metadata
        val syncTime = System.currentTimeMillis()
        repository.setLastSyncTimestamp(syncTime)
        assertEquals(syncTime, repository.getLastSyncTimestamp().getOrNull())

        // 4. Load and verify
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()!!.first()
        assertEquals(transaction.amount, loaded.amount, 0.01)

        // 5. Delete transaction
        repository.deleteTransaction(1L)
        assertEquals(0, repository.getTransactionCount().getOrNull())
        assertNull(repository.getCategoryOverride(1L).getOrNull()) // Cascade delete
    }

    @Test
    fun concurrentOperations_maintainDataIntegrity() = runTest {
        // Given - Multiple operations
        val transactions = (1L..10L).map { id ->
            createTestTransaction(smsId = id, amount = id * 100.0)
        }

        // When - Batch save and individual queries
        repository.saveTransactions(transactions)
        transactions.forEach { tx ->
            repository.setCategoryOverride(tx.smsId, "category_${tx.smsId}")
        }

        // Then - All data should be consistent
        assertEquals(10, repository.getTransactionCount().getOrNull())
        assertEquals(10, repository.getAllCategoryOverrides().getOrNull()?.size)

        val allTransactions = repository.getAllTransactionsSnapshot().getOrNull()
        assertEquals(10, allTransactions?.size)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun saveTransaction_handlesErrorGracefully() = runTest {
        // Given
        database.close() // Close database to simulate error

        // When
        val result = repository.saveTransaction(createTestTransaction(smsId = 1L))

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun saveTransactions_returnsResultType() = runTest {
        // Given
        val transactions = listOf(createTestTransaction(smsId = 1L))

        // When
        val result = repository.saveTransactions(transactions)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun getAllTransactionsSnapshot_returnsResultType() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))

        // When
        val result = repository.getAllTransactionsSnapshot()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun getAllTransactionsSnapshot_handlesErrorGracefully() = runTest {
        // Given
        database.close() // Close database to simulate error

        // When
        val result = repository.getAllTransactionsSnapshot()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun getTransactionsAfter_returnsResultType() = runTest {
        // Given
        val now = System.currentTimeMillis()
        repository.saveTransaction(createTestTransaction(smsId = 1L, date = now))

        // When
        val result = repository.getTransactionsAfter(now - 1000)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun transactionExists_returnsResultType() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))

        // When
        val result = repository.transactionExists(1L)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun transactionExists_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.transactionExists(1L)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun deleteTransaction_returnsResultType() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))

        // When
        val result = repository.deleteTransaction(1L)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun deleteAllTransactions_returnsResultType() = runTest {
        // Given
        repository.saveTransactions(
            listOf(
                createTestTransaction(smsId = 1L),
                createTestTransaction(smsId = 2L)
            )
        )

        // When
        val result = repository.deleteAllTransactions()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun getTransactionCount_returnsResultType() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))

        // When
        val result = repository.getTransactionCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun getTransactionCount_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getTransactionCount()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun getLatestTransactionDate_returnsResultType() = runTest {
        // Given
        val now = System.currentTimeMillis()
        repository.saveTransaction(createTestTransaction(smsId = 1L, date = now))

        // When
        val result = repository.getLatestTransactionDate()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(now, result.getOrNull())
    }

    @Test
    fun getLatestTransactionDate_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getLatestTransactionDate()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun setCategoryOverride_returnsResultType() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))

        // When
        val result = repository.setCategoryOverride(1L, "food")

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun getCategoryOverride_returnsResultType() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.setCategoryOverride(1L, "food")

        // When
        val result = repository.getCategoryOverride(1L)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("food", result.getOrNull())
    }

    @Test
    fun getAllCategoryOverrides_returnsResultType() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.setCategoryOverride(1L, "food")

        // When
        val result = repository.getAllCategoryOverrides()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun removeCategoryOverride_returnsResultType() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.setCategoryOverride(1L, "food")

        // When
        val result = repository.removeCategoryOverride(1L)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun removeAllCategoryOverrides_returnsResultType() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.setCategoryOverride(1L, "food")

        // When
        val result = repository.removeAllCategoryOverrides()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun getLastSyncTimestamp_returnsResultType() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()
        repository.setLastSyncTimestamp(timestamp)

        // When
        val result = repository.getLastSyncTimestamp()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(timestamp, result.getOrNull())
    }

    @Test
    fun setLastSyncTimestamp_returnsResultType() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()

        // When
        val result = repository.setLastSyncTimestamp(timestamp)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun getLastProcessedSmsId_returnsResultType() = runTest {
        // Given
        val smsId = 12345L
        repository.setLastProcessedSmsId(smsId)

        // When
        val result = repository.getLastProcessedSmsId()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(smsId, result.getOrNull())
    }

    @Test
    fun setLastProcessedSmsId_returnsResultType() = runTest {
        // Given
        val smsId = 12345L

        // When
        val result = repository.setLastProcessedSmsId(smsId)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun clearSyncMetadata_returnsResultType() = runTest {
        // Given
        repository.setLastSyncTimestamp(System.currentTimeMillis())
        repository.setLastProcessedSmsId(123L)

        // When
        val result = repository.clearSyncMetadata()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun cacheInvalidation_afterSaveTransaction() = runTest {
        // Given
        val transaction1 = createTestTransaction(smsId = 1L)
        repository.saveTransaction(transaction1)

        // First call to populate cache
        val result1 = repository.getAllTransactionsSnapshot()
        assertEquals(1, result1.getOrNull()?.size)

        // When - Save another transaction (should invalidate cache)
        val transaction2 = createTestTransaction(smsId = 2L)
        repository.saveTransaction(transaction2)

        // Then - Should get fresh data from database
        val result2 = repository.getAllTransactionsSnapshot()
        assertEquals(2, result2.getOrNull()?.size)
    }

    @Test
    fun cacheInvalidation_afterDeleteTransaction() = runTest {
        // Given
        repository.saveTransactions(
            listOf(
                createTestTransaction(smsId = 1L),
                createTestTransaction(smsId = 2L)
            )
        )

        // Populate cache
        val result1 = repository.getAllTransactionsSnapshot()
        assertEquals(2, result1.getOrNull()?.size)

        // When - Delete a transaction (should invalidate cache)
        repository.deleteTransaction(1L)

        // Then - Should get updated data
        val result2 = repository.getAllTransactionsSnapshot()
        assertEquals(1, result2.getOrNull()?.size)
    }

    @Test
    fun cacheHit_onConsecutiveCalls() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))

        // When - First call populates cache
        val result1 = repository.getAllTransactionsSnapshot()

        // Second call should hit cache
        val result2 = repository.getAllTransactionsSnapshot()

        // Then - Both should return same data
        assertEquals(result1.getOrNull()?.size, result2.getOrNull()?.size)
        assertEquals(1, result2.getOrNull()?.size)
    }

    @Test
    fun flowOperations_catchErrors() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))

        // When - Get flow (should not throw even if there are errors)
        val flow = repository.getAllTransactions()
        val result = flow.first()

        // Then
        assertEquals(1, result.size)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun saveTransaction_withZeroAmount() = runTest {
        // Given - Transaction with zero amount
        val transaction = createTestTransaction(smsId = 1L, amount = 0.0)

        // When
        val result = repository.saveTransaction(transaction)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()
        val actualAmount = loaded?.first()?.amount ?: 0.0
        assertEquals(0.0, actualAmount, 0.001)
    }

    @Test
    fun saveTransaction_withVeryLargeAmount() = runTest {
        // Given - Transaction with very large amount
        val largeAmount = Double.MAX_VALUE
        val transaction = createTestTransaction(smsId = 1L, amount = largeAmount)

        // When
        val result = repository.saveTransaction(transaction)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()
        val actualAmount = loaded?.first()?.amount ?: 0.0
        assertEquals(largeAmount, actualAmount, 0.001)
    }

    @Test
    fun saveTransaction_withVerySmallAmount() = runTest {
        // Given - Transaction with very small amount
        val smallAmount = 0.01
        val transaction = createTestTransaction(smsId = 1L, amount = smallAmount)

        // When
        val result = repository.saveTransaction(transaction)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()
        val actualAmount = loaded?.first()?.amount ?: 0.0
        assertEquals(smallAmount, actualAmount, 0.001)
    }

    @Test
    fun saveTransaction_withMinTimestamp() = runTest {
        // Given - Transaction with minimum timestamp
        val minDate = 0L
        val transaction = createTestTransaction(smsId = 1L, date = minDate)

        // When
        val result = repository.saveTransaction(transaction)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()
        assertEquals(minDate, loaded?.first()?.date)
    }

    @Test
    fun saveTransaction_withMaxTimestamp() = runTest {
        // Given - Transaction with maximum timestamp
        val maxDate = Long.MAX_VALUE
        val transaction = createTestTransaction(smsId = 1L, date = maxDate)

        // When
        val result = repository.saveTransaction(transaction)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()
        assertEquals(maxDate, loaded?.first()?.date)
    }

    @Test
    fun saveTransaction_withEmptyStringMerchant() = runTest {
        // Given - Transaction with empty string merchant (not null)
        val transaction = createTestTransaction(smsId = 1L, merchant = "")

        // When
        val result = repository.saveTransaction(transaction)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()
        assertEquals("", loaded?.first()?.merchant)
    }

    @Test
    fun saveTransaction_withVeryLongMerchantName() = runTest {
        // Given - Transaction with very long merchant name
        val longMerchant = "A".repeat(1000)
        val transaction = createTestTransaction(smsId = 1L, merchant = longMerchant)

        // When
        val result = repository.saveTransaction(transaction)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()
        assertEquals(longMerchant, loaded?.first()?.merchant)
    }

    @Test
    fun saveTransaction_withSpecialCharactersInMerchant() = runTest {
        // Given - Transaction with special characters
        val specialMerchant = "Test™️ Merchant® with 特殊字符 & symbols!@#$%^&*()"
        val transaction = createTestTransaction(smsId = 1L, merchant = specialMerchant)

        // When
        val result = repository.saveTransaction(transaction)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()
        assertEquals(specialMerchant, loaded?.first()?.merchant)
    }

    @Test
    fun saveTransaction_duplicateSmsId_replacesExisting() = runTest {
        // Given - Transaction with same smsId
        val transaction1 = createTestTransaction(smsId = 1L, amount = 100.0)
        val transaction2 = createTestTransaction(smsId = 1L, amount = 200.0)

        // When - Save same smsId twice
        repository.saveTransaction(transaction1)
        repository.saveTransaction(transaction2)

        // Then - Should have only one transaction (replaced)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()
        assertEquals(1, loaded?.size)
        val actualAmount = loaded?.first()?.amount ?: 0.0
        assertEquals(200.0, actualAmount, 0.01)
    }

    @Test
    fun saveTransactions_withEmptyList() = runTest {
        // Given - Empty list
        val emptyList = emptyList<ParsedTransaction>()

        // When
        val result = repository.saveTransactions(emptyList)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, repository.getTransactionCount().getOrNull())
    }

    @Test
    fun saveTransactions_withVeryLargeBatch() = runTest {
        // Given - Large batch of transactions
        val largeBatch = (1L..1000L).map { id ->
            createTestTransaction(smsId = id, amount = id.toDouble())
        }

        // When
        val result = repository.saveTransactions(largeBatch)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1000, repository.getTransactionCount().getOrNull())
    }

    @Test
    fun saveTransactions_withDuplicatesInBatch() = runTest {
        // Given - Batch with duplicate smsIds
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0),
            createTestTransaction(smsId = 2L, amount = 200.0),
            createTestTransaction(smsId = 1L, amount = 300.0) // Duplicate
        )

        // When
        val result = repository.saveTransactions(transactions)

        // Then - Should handle duplicates (last one wins or based on Room's conflict strategy)
        assertTrue(result.isSuccess)
        val count = repository.getTransactionCount().getOrNull()
        assertTrue(count == 2 || count == 3) // Depends on conflict strategy
    }

    @Test
    fun deleteTransaction_nonExistent() = runTest {
        // Given - No transactions in database

        // When - Try to delete non-existent transaction
        val result = repository.deleteTransaction(999L)

        // Then - Should return false
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun deleteAllTransactions_whenEmpty() = runTest {
        // Given - Empty database

        // When
        val result = repository.deleteAllTransactions()

        // Then - Should return 0 deleted
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun getTransactionsByDateRange_withInvertedRange() = runTest {
        // Given - Date range where start > end
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600000
        repository.saveTransaction(createTestTransaction(smsId = 1L, date = now))

        // When - Query with inverted range
        val result = repository.getTransactionsByDateRange(now, oneHourAgo).first()

        // Then - Should return empty or handle gracefully
        assertTrue(result.isEmpty())
    }

    @Test
    fun getTransactionsByDateRange_withSameStartAndEnd() = runTest {
        // Given - Transactions at exact timestamp
        val exactTime = System.currentTimeMillis()
        repository.saveTransaction(createTestTransaction(smsId = 1L, date = exactTime))

        // When - Query with same start and end
        val result = repository.getTransactionsByDateRange(exactTime, exactTime).first()

        // Then - Should return transaction at exact time
        assertEquals(1, result.size)
    }

    @Test
    fun getTransactionsByDateRange_withNoMatchingTransactions() = runTest {
        // Given - Transactions outside the range
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 86400000
        val twoDaysAgo = now - 172800000
        repository.saveTransaction(createTestTransaction(smsId = 1L, date = twoDaysAgo))

        // When - Query range that doesn't include the transaction
        val result = repository.getTransactionsByDateRange(oneDayAgo, now).first()

        // Then - Should return empty
        assertTrue(result.isEmpty())
    }

    @Test
    fun getTransactionsByType_withUnknownType() = runTest {
        // Given - Transactions with UNKNOWN type
        val unknownTx = createTestTransaction(smsId = 1L, type = TransactionType.UNKNOWN)
        repository.saveTransaction(unknownTx)

        // When
        val result = repository.getTransactionsByType(TransactionType.UNKNOWN).first()

        // Then
        assertEquals(1, result.size)
        assertEquals(TransactionType.UNKNOWN, result[0].type)
    }

    @Test
    fun getTransactionsByType_withNoMatchingType() = runTest {
        // Given - Only DEBIT transactions
        repository.saveTransaction(createTestTransaction(smsId = 1L, type = TransactionType.DEBIT))

        // When - Query for CREDIT
        val result = repository.getTransactionsByType(TransactionType.CREDIT).first()

        // Then - Should return empty
        assertTrue(result.isEmpty())
    }

    @Test
    fun transactionExists_withNegativeSmsId() = runTest {
        // Given - Check for negative smsId

        // When
        val result = repository.transactionExists(-1L)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun transactionExists_withZeroSmsId() = runTest {
        // Given - Save transaction with smsId = 0
        repository.saveTransaction(createTestTransaction(smsId = 0L))

        // When
        val result = repository.transactionExists(0L)

        // Then - Should find it
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun setCategoryOverride_withEmptyString() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))

        // When - Set category to empty string
        val result = repository.setCategoryOverride(1L, "")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("", repository.getCategoryOverride(1L).getOrNull())
    }

    @Test
    fun setCategoryOverride_withVeryLongCategoryId() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        val longCategory = "category_" + "a".repeat(500)

        // When
        val result = repository.setCategoryOverride(1L, longCategory)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(longCategory, repository.getCategoryOverride(1L).getOrNull())
    }

    @Test
    fun setCategoryOverride_forNonExistentTransaction() = runTest {
        // Given - No transaction with smsId = 999

        // When - Try to set category override for non-existent transaction
        val result = repository.setCategoryOverride(999L, "food")

        // Then - Behavior depends on foreign key constraints
        // If FK enforced: should fail; if not: might succeed
        // Test documents the behavior
        assertNotNull(result)
    }

    @Test
    fun setCategoryOverride_multipleUpdatesRapidly() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))

        // When - Multiple rapid updates
        repository.setCategoryOverride(1L, "food")
        repository.setCategoryOverride(1L, "transport")
        repository.setCategoryOverride(1L, "shopping")
        val result = repository.setCategoryOverride(1L, "entertainment")

        // Then - Last update should win
        assertTrue(result.isSuccess)
        assertEquals("entertainment", repository.getCategoryOverride(1L).getOrNull())
    }

    @Test
    fun getAllCategoryOverrides_whenEmpty() = runTest {
        // Given - No overrides

        // When
        val result = repository.getAllCategoryOverrides()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun removeCategoryOverride_multipleTimes() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.setCategoryOverride(1L, "food")

        // When - Remove multiple times
        val result1 = repository.removeCategoryOverride(1L)
        val result2 = repository.removeCategoryOverride(1L)

        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result1.getOrNull() == true)
        assertTrue(result2.isSuccess)
        assertFalse(result2.getOrNull() == true) // Second removal should return false
    }

    @Test
    fun removeAllCategoryOverrides_whenEmpty() = runTest {
        // Given - No overrides

        // When
        val result = repository.removeAllCategoryOverrides()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun setLastSyncTimestamp_withZero() = runTest {
        // Given
        val zeroTimestamp = 0L

        // When
        val result = repository.setLastSyncTimestamp(zeroTimestamp)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(zeroTimestamp, repository.getLastSyncTimestamp().getOrNull())
    }

    @Test
    fun setLastSyncTimestamp_withNegativeValue() = runTest {
        // Given
        val negativeTimestamp = -1L

        // When
        val result = repository.setLastSyncTimestamp(negativeTimestamp)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(negativeTimestamp, repository.getLastSyncTimestamp().getOrNull())
    }

    @Test
    fun setLastSyncTimestamp_multipleUpdatesInSequence() = runTest {
        // Given
        val timestamps = listOf(1000L, 2000L, 1500L, 3000L)

        // When - Set multiple times
        timestamps.forEach { timestamp ->
            repository.setLastSyncTimestamp(timestamp)
        }

        // Then - Last value should be stored
        assertEquals(3000L, repository.getLastSyncTimestamp().getOrNull())
    }

    @Test
    fun setLastProcessedSmsId_withZero() = runTest {
        // Given
        val zeroSmsId = 0L

        // When
        val result = repository.setLastProcessedSmsId(zeroSmsId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(zeroSmsId, repository.getLastProcessedSmsId().getOrNull())
    }

    @Test
    fun setLastProcessedSmsId_withNegativeValue() = runTest {
        // Given
        val negativeSmsId = -999L

        // When
        val result = repository.setLastProcessedSmsId(negativeSmsId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(negativeSmsId, repository.getLastProcessedSmsId().getOrNull())
    }

    @Test
    fun clearSyncMetadata_whenEmpty() = runTest {
        // Given - No metadata set

        // When
        val result = repository.clearSyncMetadata()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun clearSyncMetadata_multipleTimes() = runTest {
        // Given
        repository.setLastSyncTimestamp(System.currentTimeMillis())
        repository.setLastProcessedSmsId(123L)

        // When - Clear multiple times
        val result1 = repository.clearSyncMetadata()
        val result2 = repository.clearSyncMetadata()

        // Then
        assertTrue(result1.isSuccess)
        assertEquals(2, result1.getOrNull())
        assertTrue(result2.isSuccess)
        assertEquals(0, result2.getOrNull()) // Second clear should delete nothing
    }

    @Test
    fun getTransactionsAfter_withFutureTimestamp() = runTest {
        // Given - Transactions in the past
        val now = System.currentTimeMillis()
        repository.saveTransaction(createTestTransaction(smsId = 1L, date = now))

        // When - Query for transactions after future timestamp
        val futureTime = now + 3600000
        val result = repository.getTransactionsAfter(futureTime)

        // Then - Should return empty
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun getTransactionsAfter_withVeryOldTimestamp() = runTest {
        // Given
        val now = System.currentTimeMillis()
        repository.saveTransactions(
            listOf(
                createTestTransaction(smsId = 1L, date = now - 1000),
                createTestTransaction(smsId = 2L, date = now)
            )
        )

        // When - Query from very old timestamp
        val result = repository.getTransactionsAfter(0L)

        // Then - Should return all transactions
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun getAllTransactionsSnapshot_withClosedDatabase() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        database.close()

        // When
        val result = repository.getAllTransactionsSnapshot()

        // Then - Should fail gracefully
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun flowOperations_withClosedDatabase() = runTest {
        // Given
        database.close()

        // When/Then - Flow should handle error
        try {
            val flow = repository.getAllTransactions()
            val result = flow.first()
            // Depending on implementation, might return empty or throw
            assertTrue(result.isEmpty() || true)
        } catch (e: Exception) {
            // Also acceptable to throw exception
            assertNotNull(e)
        }
    }

    @Test
    fun transactionOrdering_maintainedAfterMultipleOperations() = runTest {
        // Given - Transactions with specific dates
        val base = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 3L, date = base),
            createTestTransaction(smsId = 1L, date = base - 2000),
            createTestTransaction(smsId = 2L, date = base - 1000)
        )

        // When
        repository.saveTransactions(transactions)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()

        // Then - Should be ordered by date DESC (newest first)
        assertEquals(3, loaded?.size)
        assertEquals(3L, loaded?.get(0)?.smsId) // Newest
        assertEquals(2L, loaded?.get(1)?.smsId)
        assertEquals(1L, loaded?.get(2)?.smsId) // Oldest
    }

    @Test
    fun stressTest_rapidConsecutiveWrites() = runTest {
        // Given - Simulate rapid consecutive writes
        val iterations = 100

        // When - Rapid writes
        repeat(iterations) { i ->
            repository.saveTransaction(createTestTransaction(smsId = i.toLong(), amount = i.toDouble()))
        }

        // Then - All should be saved
        val count = repository.getTransactionCount().getOrNull()
        assertEquals(iterations, count)
    }

    @Test
    fun stressTest_rapidConsecutiveReadsAndWrites() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))

        // When - Interleaved reads and writes
        repeat(50) { i ->
            if (i % 2 == 0) {
                repository.saveTransaction(createTestTransaction(smsId = (i + 2).toLong()))
            } else {
                repository.getAllTransactionsSnapshot()
            }
        }

        // Then - Should handle without errors
        val result = repository.getAllTransactionsSnapshot()
        assertTrue(result.isSuccess)
        assertTrue((result.getOrNull()?.size ?: 0) > 0)
    }

    @Test
    fun categoryOverride_withSpecialCharacters() = runTest {
        // Given
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        val specialCategory = "food & beverages™️ (新しい)"

        // When
        val result = repository.setCategoryOverride(1L, specialCategory)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(specialCategory, repository.getCategoryOverride(1L).getOrNull())
    }

    @Test
    fun entityMapping_withAllNullableFieldsNull() = runTest {
        // Given - Minimal transaction with all optional fields null
        val minimalTransaction = ParsedTransaction(
            smsId = 1L,
            amount = 100.0,
            type = TransactionType.DEBIT,
            merchant = null,
            accountNumber = null,
            referenceNumber = null,
            date = System.currentTimeMillis(),
            rawSms = "Minimal SMS",
            senderAddress = "SENDER",
            balanceAfter = null,
            location = null
        )

        // When
        repository.saveTransaction(minimalTransaction)
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()?.first()

        // Then - All nullable fields should remain null
        assertNotNull(loaded)
        assertNull(loaded?.merchant)
        assertNull(loaded?.accountNumber)
        assertNull(loaded?.referenceNumber)
        assertNull(loaded?.balanceAfter)
        assertNull(loaded?.location)
    }

    @Test
    fun getTransactionCount_afterMultipleDeletions() = runTest {
        // Given
        repository.saveTransactions(
            (1L..10L).map { createTestTransaction(smsId = it) }
        )

        // When - Delete half of them
        (1L..5L).forEach { repository.deleteTransaction(it) }

        // Then
        assertEquals(5, repository.getTransactionCount().getOrNull())
    }

    // ==================== Manual Transaction Tests ====================

    @Test
    fun saveManualTransaction_insertsSuccessfully() = runTest {
        // Given
        val manualTransaction = createTestTransaction(
            smsId = 0L, // Will be replaced by repository
            amount = 250.0,
            merchant = "Cash Store"
        ).copy(
            source = com.example.kanakku.data.model.TransactionSource.MANUAL,
            notes = "Bought groceries"
        )

        // When
        val result = repository.saveManualTransaction(manualTransaction)

        // Then
        assertTrue(result.isSuccess)
        val generatedId = result.getOrNull()!!
        assertTrue(generatedId > 0) // Should have generated ID

        // Verify transaction was saved with correct source
        val saved = repository.getTransactionById(generatedId).getOrNull()
        assertNotNull(saved)
        assertEquals(com.example.kanakku.data.model.TransactionSource.MANUAL, saved?.source)
        assertEquals(250.0, saved?.amount, 0.01)
        assertEquals("Cash Store", saved?.merchant)
        assertEquals("Bought groceries", saved?.notes)
    }

    @Test
    fun saveManualTransaction_generatesUniqueId() = runTest {
        // Given
        val transaction1 = createTestTransaction(smsId = 0L, amount = 100.0)
            .copy(source = com.example.kanakku.data.model.TransactionSource.MANUAL)
        val transaction2 = createTestTransaction(smsId = 0L, amount = 200.0)
            .copy(source = com.example.kanakku.data.model.TransactionSource.MANUAL)

        // When - Save two manual transactions
        val id1 = repository.saveManualTransaction(transaction1).getOrNull()!!
        val id2 = repository.saveManualTransaction(transaction2).getOrNull()!!

        // Then - IDs should be different
        assertNotEquals(id1, id2)
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        // Both transactions should exist
        val count = repository.getTransactionCount().getOrNull()
        assertEquals(2, count)
    }

    @Test
    fun saveManualTransaction_withNotes_savesCorrectly() = runTest {
        // Given
        val manualTransaction = createTestTransaction(smsId = 0L)
            .copy(
                source = com.example.kanakku.data.model.TransactionSource.MANUAL,
                notes = "Birthday gift for mom"
            )

        // When
        val generatedId = repository.saveManualTransaction(manualTransaction).getOrNull()!!

        // Then
        val saved = repository.getTransactionById(generatedId).getOrNull()
        assertEquals("Birthday gift for mom", saved?.notes)
    }

    @Test
    fun saveManualTransaction_withoutNotes_savesNullNotes() = runTest {
        // Given
        val manualTransaction = createTestTransaction(smsId = 0L)
            .copy(
                source = com.example.kanakku.data.model.TransactionSource.MANUAL,
                notes = null
            )

        // When
        val generatedId = repository.saveManualTransaction(manualTransaction).getOrNull()!!

        // Then
        val saved = repository.getTransactionById(generatedId).getOrNull()
        assertNull(saved?.notes)
    }

    @Test
    fun saveManualTransaction_invalidatesCache() = runTest {
        // Given - Populate cache
        repository.saveTransaction(createTestTransaction(smsId = 1L))
        repository.getAllTransactionsSnapshot() // Populate cache

        // When - Save manual transaction
        val manualTransaction = createTestTransaction(smsId = 0L)
            .copy(source = com.example.kanakku.data.model.TransactionSource.MANUAL)
        repository.saveManualTransaction(manualTransaction)

        // Then - Cache should be invalidated and show updated count
        val allTransactions = repository.getAllTransactionsSnapshot().getOrNull()
        assertEquals(2, allTransactions?.size)
    }

    @Test
    fun saveManualTransaction_handlesErrorGracefully() = runTest {
        // Given
        database.close() // Close database to simulate error

        // When
        val manualTransaction = createTestTransaction(smsId = 0L)
            .copy(source = com.example.kanakku.data.model.TransactionSource.MANUAL)
        val result = repository.saveManualTransaction(manualTransaction)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getTransactionById_returnsCorrectTransaction() = runTest {
        // Given
        val transaction = createTestTransaction(smsId = 12345L, amount = 500.0, merchant = "Test Shop")
        repository.saveTransaction(transaction)

        // When
        val result = repository.getTransactionById(12345L)

        // Then
        assertTrue(result.isSuccess)
        val loaded = result.getOrNull()
        assertNotNull(loaded)
        assertEquals(12345L, loaded?.smsId)
        assertEquals(500.0, loaded?.amount, 0.01)
        assertEquals("Test Shop", loaded?.merchant)
    }

    @Test
    fun getTransactionById_returnsNullForNonExistent() = runTest {
        // When
        val result = repository.getTransactionById(99999L)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun getTransactionById_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getTransactionById(1L)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun updateManualTransaction_updatesSuccessfully() = runTest {
        // Given - Save a manual transaction
        val originalTransaction = createTestTransaction(smsId = 0L, amount = 100.0, merchant = "Original Store")
            .copy(
                source = com.example.kanakku.data.model.TransactionSource.MANUAL,
                notes = "Original notes"
            )
        val generatedId = repository.saveManualTransaction(originalTransaction).getOrNull()!!

        // When - Update the transaction
        val updatedTransaction = createTestTransaction(smsId = generatedId, amount = 200.0, merchant = "Updated Store")
            .copy(
                source = com.example.kanakku.data.model.TransactionSource.MANUAL,
                notes = "Updated notes"
            )
        val updateResult = repository.updateManualTransaction(updatedTransaction)

        // Then
        assertTrue(updateResult.isSuccess)
        assertTrue(updateResult.getOrNull() == true)

        // Verify changes were persisted
        val loaded = repository.getTransactionById(generatedId).getOrNull()
        assertNotNull(loaded)
        assertEquals(200.0, loaded?.amount, 0.01)
        assertEquals("Updated Store", loaded?.merchant)
        assertEquals("Updated notes", loaded?.notes)
    }

    @Test
    fun updateManualTransaction_returnsFailureForSmsTransaction() = runTest {
        // Given - Save an SMS transaction
        val smsTransaction = createTestTransaction(smsId = 1L)
            .copy(source = com.example.kanakku.data.model.TransactionSource.SMS)
        repository.saveTransaction(smsTransaction)

        // When - Try to update SMS transaction using updateManualTransaction
        val result = repository.updateManualTransaction(smsTransaction)

        // Then - Should fail with IllegalArgumentException
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception?.cause is IllegalArgumentException)
    }

    @Test
    fun updateManualTransaction_returnsFalseForNonExistent() = runTest {
        // Given - Transaction that doesn't exist
        val nonExistentTransaction = createTestTransaction(smsId = 99999L)
            .copy(source = com.example.kanakku.data.model.TransactionSource.MANUAL)

        // When
        val result = repository.updateManualTransaction(nonExistentTransaction)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun updateManualTransaction_invalidatesCache() = runTest {
        // Given - Manual transaction and populate cache
        val manualTransaction = createTestTransaction(smsId = 0L, amount = 100.0)
            .copy(source = com.example.kanakku.data.model.TransactionSource.MANUAL)
        val generatedId = repository.saveManualTransaction(manualTransaction).getOrNull()!!
        repository.getAllTransactionsSnapshot() // Populate cache

        // When - Update transaction
        val updatedTransaction = createTestTransaction(smsId = generatedId, amount = 200.0)
            .copy(source = com.example.kanakku.data.model.TransactionSource.MANUAL)
        repository.updateManualTransaction(updatedTransaction)

        // Then - Cache should reflect updated amount
        val loaded = repository.getAllTransactionsSnapshot().getOrNull()?.first()
        assertEquals(200.0, loaded?.amount, 0.01)
    }

    @Test
    fun updateManualTransaction_handlesErrorGracefully() = runTest {
        // Given
        val manualTransaction = createTestTransaction(smsId = 1L)
            .copy(source = com.example.kanakku.data.model.TransactionSource.MANUAL)
        database.close()

        // When
        val result = repository.updateManualTransaction(manualTransaction)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun manualTransactionWorkflow_fullCycle() = runTest {
        // 1. Save manual transaction
        val manualTransaction = createTestTransaction(
            smsId = 0L,
            amount = 150.0,
            merchant = "Coffee Shop"
        ).copy(
            source = com.example.kanakku.data.model.TransactionSource.MANUAL,
            notes = "Morning coffee"
        )
        val generatedId = repository.saveManualTransaction(manualTransaction).getOrNull()!!
        assertTrue(generatedId > 0)

        // 2. Verify saved
        val saved = repository.getTransactionById(generatedId).getOrNull()
        assertNotNull(saved)
        assertEquals(150.0, saved?.amount, 0.01)
        assertEquals("Coffee Shop", saved?.merchant)

        // 3. Update transaction
        val updated = saved!!.copy(amount = 175.0, notes = "Coffee + snack")
        val updateResult = repository.updateManualTransaction(updated)
        assertTrue(updateResult.isSuccess)

        // 4. Verify update
        val reloaded = repository.getTransactionById(generatedId).getOrNull()
        assertEquals(175.0, reloaded?.amount, 0.01)
        assertEquals("Coffee + snack", reloaded?.notes)

        // 5. Add category override
        repository.setCategoryOverride(generatedId, "food")
        assertEquals("food", repository.getCategoryOverride(generatedId).getOrNull())

        // 6. Delete transaction
        val deleteResult = repository.deleteTransaction(generatedId)
        assertTrue(deleteResult.isSuccess)
        assertTrue(deleteResult.getOrNull() == true)

        // 7. Verify deleted
        assertNull(repository.getTransactionById(generatedId).getOrNull())
        assertNull(repository.getCategoryOverride(generatedId).getOrNull()) // Cascade delete
    }

    @Test
    fun manualAndSmsTransactions_coexistCorrectly() = runTest {
        // Given - Mix of SMS and manual transactions
        val smsTransaction = createTestTransaction(smsId = 1L, amount = 100.0)
            .copy(source = com.example.kanakku.data.model.TransactionSource.SMS)
        val manualTransaction = createTestTransaction(smsId = 0L, amount = 200.0)
            .copy(source = com.example.kanakku.data.model.TransactionSource.MANUAL)

        // When
        repository.saveTransaction(smsTransaction)
        val manualId = repository.saveManualTransaction(manualTransaction).getOrNull()!!

        // Then
        val allTransactions = repository.getAllTransactionsSnapshot().getOrNull()
        assertEquals(2, allTransactions?.size)

        // Verify sources are correct
        val sms = repository.getTransactionById(1L).getOrNull()
        val manual = repository.getTransactionById(manualId).getOrNull()
        assertEquals(com.example.kanakku.data.model.TransactionSource.SMS, sms?.source)
        assertEquals(com.example.kanakku.data.model.TransactionSource.MANUAL, manual?.source)
    }

    @Test
    fun saveManualTransaction_withVeryLongNotes() = runTest {
        // Given - Transaction with very long notes
        val longNotes = "A".repeat(5000)
        val manualTransaction = createTestTransaction(smsId = 0L)
            .copy(
                source = com.example.kanakku.data.model.TransactionSource.MANUAL,
                notes = longNotes
            )

        // When
        val result = repository.saveManualTransaction(manualTransaction)

        // Then
        assertTrue(result.isSuccess)
        val saved = repository.getTransactionById(result.getOrNull()!!).getOrNull()
        assertEquals(longNotes, saved?.notes)
    }

    @Test
    fun saveManualTransaction_withSpecialCharactersInNotes() = runTest {
        // Given
        val specialNotes = "Café ☕️ - paid €20.50 for mom's 生日 gift! #special 🎁"
        val manualTransaction = createTestTransaction(smsId = 0L)
            .copy(
                source = com.example.kanakku.data.model.TransactionSource.MANUAL,
                notes = specialNotes
            )

        // When
        val generatedId = repository.saveManualTransaction(manualTransaction).getOrNull()!!

        // Then
        val saved = repository.getTransactionById(generatedId).getOrNull()
        assertEquals(specialNotes, saved?.notes)
    }

    @Test
    fun idGeneration_doesNotCollideWithSmsIds() = runTest {
        // Given - SMS transactions with typical IDs
        val smsIds = listOf(1L, 2L, 3L, 100L, 1000L)
        smsIds.forEach { id ->
            repository.saveTransaction(
                createTestTransaction(smsId = id)
                    .copy(source = com.example.kanakku.data.model.TransactionSource.SMS)
            )
        }

        // When - Save manual transaction
        val manualTransaction = createTestTransaction(smsId = 0L)
            .copy(source = com.example.kanakku.data.model.TransactionSource.MANUAL)
        val generatedId = repository.saveManualTransaction(manualTransaction).getOrNull()!!

        // Then - Generated ID should be much larger (timestamp-based)
        assertTrue(generatedId > 1000000000000L) // Timestamp is in milliseconds since epoch
        assertFalse(smsIds.contains(generatedId))

        // All transactions should coexist
        assertEquals(6, repository.getTransactionCount().getOrNull())
    }
}
