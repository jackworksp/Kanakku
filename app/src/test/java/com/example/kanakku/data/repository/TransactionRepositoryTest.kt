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
@Config(sdk = [35])
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
        val loaded = repository.getAllTransactionsSnapshot()
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
        val loaded = repository.getAllTransactionsSnapshot()
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
        val newer = repository.getTransactionsAfter(oneHourAgo)

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
        assertTrue(repository.transactionExists(1L))
        assertFalse(repository.transactionExists(999L))
    }

    @Test
    fun deleteTransaction_removesTransaction() = runTest {
        // Given
        val transaction = createTestTransaction(smsId = 1L)
        repository.saveTransaction(transaction)

        // When
        val deleted = repository.deleteTransaction(1L)

        // Then
        assertTrue(deleted)
        assertFalse(repository.transactionExists(1L))
        assertEquals(0, repository.getTransactionCount())
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
        val count = repository.deleteAllTransactions()

        // Then
        assertEquals(2, count)
        assertEquals(0, repository.getTransactionCount())
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
        val latestDate = repository.getLatestTransactionDate()

        // Then
        assertEquals(now, latestDate)
    }

    @Test
    fun getLatestTransactionDate_returnsNullWhenEmpty() = runTest {
        // When
        val latestDate = repository.getLatestTransactionDate()

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
        val loaded = repository.getAllTransactionsSnapshot().first()

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
        val loaded = repository.getAllTransactionsSnapshot().first()

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

        // When
        repository.setCategoryOverride(smsId, categoryId)

        // Then
        val override = repository.getCategoryOverride(smsId)
        assertEquals(categoryId, override)
    }

    @Test
    fun setCategoryOverride_updatesExisting() = runTest {
        // Given
        val smsId = 1L
        repository.setCategoryOverride(smsId, "food")

        // When
        repository.setCategoryOverride(smsId, "transport")

        // Then
        val override = repository.getCategoryOverride(smsId)
        assertEquals("transport", override)
    }

    @Test
    fun getCategoryOverride_returnsNullWhenNotExists() = runTest {
        // When
        val override = repository.getCategoryOverride(999L)

        // Then
        assertNull(override)
    }

    @Test
    fun getAllCategoryOverrides_returnsMap() = runTest {
        // Given
        repository.setCategoryOverride(1L, "food")
        repository.setCategoryOverride(2L, "transport")
        repository.setCategoryOverride(3L, "shopping")

        // When
        val overrides = repository.getAllCategoryOverrides()

        // Then
        assertEquals(3, overrides.size)
        assertEquals("food", overrides[1L])
        assertEquals("transport", overrides[2L])
        assertEquals("shopping", overrides[3L])
    }

    @Test
    fun getAllCategoryOverridesFlow_emitsUpdates() = runTest {
        // Given
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
        repository.setCategoryOverride(1L, "food")

        // When
        val removed = repository.removeCategoryOverride(1L)

        // Then
        assertTrue(removed)
        assertNull(repository.getCategoryOverride(1L))
    }

    @Test
    fun removeCategoryOverride_returnsFalseWhenNotExists() = runTest {
        // When
        val removed = repository.removeCategoryOverride(999L)

        // Then
        assertFalse(removed)
    }

    @Test
    fun removeAllCategoryOverrides_clearsAll() = runTest {
        // Given
        repository.setCategoryOverride(1L, "food")
        repository.setCategoryOverride(2L, "transport")

        // When
        val count = repository.removeAllCategoryOverrides()

        // Then
        assertEquals(2, count)
        assertTrue(repository.getAllCategoryOverrides().isEmpty())
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
        assertNull(repository.getCategoryOverride(1L))
    }

    // ==================== Sync Metadata Tests ====================

    @Test
    fun lastSyncTimestamp_storesAndRetrieves() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()

        // When
        repository.setLastSyncTimestamp(timestamp)

        // Then
        assertEquals(timestamp, repository.getLastSyncTimestamp())
    }

    @Test
    fun lastSyncTimestamp_updatesExisting() = runTest {
        // Given
        repository.setLastSyncTimestamp(1000L)

        // When
        repository.setLastSyncTimestamp(2000L)

        // Then
        assertEquals(2000L, repository.getLastSyncTimestamp())
    }

    @Test
    fun lastSyncTimestamp_returnsNullWhenNotSet() = runTest {
        // When
        val timestamp = repository.getLastSyncTimestamp()

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
        assertEquals(smsId, repository.getLastProcessedSmsId())
    }

    @Test
    fun lastProcessedSmsId_updatesExisting() = runTest {
        // Given
        repository.setLastProcessedSmsId(100L)

        // When
        repository.setLastProcessedSmsId(200L)

        // Then
        assertEquals(200L, repository.getLastProcessedSmsId())
    }

    @Test
    fun lastProcessedSmsId_returnsNullWhenNotSet() = runTest {
        // When
        val smsId = repository.getLastProcessedSmsId()

        // Then
        assertNull(smsId)
    }

    @Test
    fun clearSyncMetadata_removesAllMetadata() = runTest {
        // Given
        repository.setLastSyncTimestamp(System.currentTimeMillis())
        repository.setLastProcessedSmsId(123L)

        // When
        val count = repository.clearSyncMetadata()

        // Then
        assertEquals(2, count)
        assertNull(repository.getLastSyncTimestamp())
        assertNull(repository.getLastProcessedSmsId())
    }

    // ==================== Incremental Sync Logic Tests ====================

    @Test
    fun incrementalSync_simulateFirstSync() = runTest {
        // Given - No previous sync
        assertNull(repository.getLastSyncTimestamp())

        // When - First sync with transactions
        val syncTime = System.currentTimeMillis()
        val transactions = listOf(
            createTestTransaction(smsId = 1L, date = syncTime - 1000),
            createTestTransaction(smsId = 2L, date = syncTime - 500)
        )
        repository.saveTransactions(transactions)
        repository.setLastSyncTimestamp(syncTime)

        // Then
        assertEquals(2, repository.getTransactionCount())
        assertEquals(syncTime, repository.getLastSyncTimestamp())
    }

    @Test
    fun incrementalSync_simulateSubsequentSync() = runTest {
        // Given - Previous sync exists
        val firstSyncTime = 1000L
        repository.saveTransaction(createTestTransaction(smsId = 1L, date = firstSyncTime - 100))
        repository.setLastSyncTimestamp(firstSyncTime)

        // When - Subsequent sync with new transactions
        val secondSyncTime = 2000L
        val newTransactions = repository.getTransactionsAfter(firstSyncTime)
        // Simulate fetching new SMS
        repository.saveTransaction(createTestTransaction(smsId = 2L, date = secondSyncTime - 100))
        repository.setLastSyncTimestamp(secondSyncTime)

        // Then
        assertEquals(2, repository.getTransactionCount())
        assertEquals(secondSyncTime, repository.getLastSyncTimestamp())
    }

    @Test
    fun incrementalSync_onlyProcessesNewTransactions() = runTest {
        // Given - Existing transactions
        val oldTime = 1000L
        repository.saveTransaction(createTestTransaction(smsId = 1L, date = oldTime))
        repository.setLastSyncTimestamp(oldTime)

        // When - Check for new transactions
        val lastSync = repository.getLastSyncTimestamp()!!
        val newTransactions = repository.getTransactionsAfter(lastSync)

        // Then - Should be empty (no new transactions)
        assertTrue(newTransactions.isEmpty())
    }

    // ==================== Integration Tests ====================

    @Test
    fun fullWorkflow_saveLoadUpdateDelete() = runTest {
        // 1. Save transaction
        val transaction = createTestTransaction(smsId = 1L, amount = 100.0)
        repository.saveTransaction(transaction)
        assertEquals(1, repository.getTransactionCount())

        // 2. Add category override
        repository.setCategoryOverride(1L, "food")
        assertEquals("food", repository.getCategoryOverride(1L))

        // 3. Update sync metadata
        val syncTime = System.currentTimeMillis()
        repository.setLastSyncTimestamp(syncTime)
        assertEquals(syncTime, repository.getLastSyncTimestamp())

        // 4. Load and verify
        val loaded = repository.getAllTransactionsSnapshot().first()
        assertEquals(transaction.amount, loaded.amount, 0.01)

        // 5. Delete transaction
        repository.deleteTransaction(1L)
        assertEquals(0, repository.getTransactionCount())
        assertNull(repository.getCategoryOverride(1L)) // Cascade delete
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
        assertEquals(10, repository.getTransactionCount())
        assertEquals(10, repository.getAllCategoryOverrides().size)

        val allTransactions = repository.getAllTransactionsSnapshot()
        assertEquals(10, allTransactions.size)
    }
}
