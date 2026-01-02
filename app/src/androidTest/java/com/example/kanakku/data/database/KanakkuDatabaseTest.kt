package com.example.kanakku.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kanakku.data.database.dao.CategoryOverrideDao
import com.example.kanakku.data.database.dao.SyncMetadataDao
import com.example.kanakku.data.database.dao.TransactionDao
import com.example.kanakku.data.database.entity.CategoryOverrideEntity
import com.example.kanakku.data.database.entity.SyncMetadataEntity
import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Instrumented tests for KanakkuDatabase running on a real Android device/emulator.
 *
 * Tests cover:
 * - Database creation and initialization
 * - CRUD operations for all entities
 * - Query performance with larger datasets
 * - Foreign key relationships and cascade deletes
 * - Index effectiveness
 */
@RunWith(AndroidJUnit4::class)
class KanakkuDatabaseTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryOverrideDao: CategoryOverrideDao
    private lateinit var syncMetadataDao: SyncMetadataDao

    @Before
    fun setup() {
        // Create in-memory database for testing on real device
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        transactionDao = database.transactionDao()
        categoryOverrideDao = database.categoryOverrideDao()
        syncMetadataDao = database.syncMetadataDao()
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

    // ==================== Database Creation Tests ====================

    @Test
    fun databaseCreation_initializesSuccessfully() {
        // Verify database is not null
        assertNotNull(database)
        assertTrue(database.isOpen)
    }

    @Test
    fun databaseDAOs_areAccessible() {
        // Verify all DAOs are accessible
        assertNotNull(database.transactionDao())
        assertNotNull(database.categoryOverrideDao())
        assertNotNull(database.syncMetadataDao())
    }

    @Test
    fun database_hasCorrectVersion() = runBlocking {
        // Database version should be 1
        assertEquals(1, database.openHelper.readableDatabase.version)
    }

    // ==================== Transaction DAO Tests ====================

    @Test
    fun transactionDao_insertAndRetrieve() = runBlocking {
        // Given
        val transaction = createTestTransaction(smsId = 1L, amount = 100.0)

        // When
        transactionDao.insert(transaction)

        // Then
        val retrieved = transactionDao.getAllTransactionsSnapshot()
        assertEquals(1, retrieved.size)
        assertEquals(transaction.smsId, retrieved[0].smsId)
        assertEquals(transaction.amount, retrieved[0].amount, 0.01)
    }

    @Test
    fun transactionDao_insertMultiple() = runBlocking {
        // Given
        val transactions = listOf(
            createTestTransaction(smsId = 1L, amount = 100.0),
            createTestTransaction(smsId = 2L, amount = 200.0),
            createTestTransaction(smsId = 3L, amount = 300.0)
        )

        // When
        transactionDao.insertAll(transactions)

        // Then
        val count = transactionDao.getTransactionCount()
        assertEquals(3, count)
    }

    @Test
    fun transactionDao_replaceOnConflict() = runBlocking {
        // Given
        val original = createTestTransaction(smsId = 1L, amount = 100.0)
        transactionDao.insert(original)

        // When - Insert with same smsId but different amount
        val updated = createTestTransaction(smsId = 1L, amount = 200.0)
        transactionDao.insert(updated)

        // Then - Should have replaced, not duplicated
        val count = transactionDao.getTransactionCount()
        val retrieved = transactionDao.getAllTransactionsSnapshot().first()
        assertEquals(1, count)
        assertEquals(200.0, retrieved.amount, 0.01)
    }

    @Test
    fun transactionDao_deleteById() = runBlocking {
        // Given
        val transaction = createTestTransaction(smsId = 1L)
        transactionDao.insert(transaction)

        // When
        val deletedCount = transactionDao.deleteById(1L)

        // Then
        assertEquals(1, deletedCount)
        assertEquals(0, transactionDao.getTransactionCount())
    }

    @Test
    fun transactionDao_exists() = runBlocking {
        // Given
        val transaction = createTestTransaction(smsId = 1L)
        transactionDao.insert(transaction)

        // When/Then
        assertTrue(transactionDao.exists(1L))
        assertFalse(transactionDao.exists(999L))
    }

    @Test
    fun transactionDao_getByType() = runBlocking {
        // Given
        transactionDao.insertAll(
            listOf(
                createTestTransaction(smsId = 1L, type = TransactionType.DEBIT),
                createTestTransaction(smsId = 2L, type = TransactionType.CREDIT),
                createTestTransaction(smsId = 3L, type = TransactionType.DEBIT)
            )
        )

        // When
        val debits = transactionDao.getTransactionsByType(TransactionType.DEBIT).first()
        val credits = transactionDao.getTransactionsByType(TransactionType.CREDIT).first()

        // Then
        assertEquals(2, debits.size)
        assertEquals(1, credits.size)
    }

    @Test
    fun transactionDao_getByDateRange() = runBlocking {
        // Given
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600000
        val twoHoursAgo = now - 7200000

        transactionDao.insertAll(
            listOf(
                createTestTransaction(smsId = 1L, date = twoHoursAgo),
                createTestTransaction(smsId = 2L, date = oneHourAgo),
                createTestTransaction(smsId = 3L, date = now)
            )
        )

        // When
        val inRange = transactionDao.getTransactionsByDateRange(oneHourAgo, now).first()

        // Then
        assertEquals(2, inRange.size)
    }

    @Test
    fun transactionDao_getLatestDate() = runBlocking {
        // Given
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600000

        transactionDao.insertAll(
            listOf(
                createTestTransaction(smsId = 1L, date = oneHourAgo),
                createTestTransaction(smsId = 2L, date = now)
            )
        )

        // When
        val latestDate = transactionDao.getLatestTransactionDate()

        // Then
        assertEquals(now, latestDate)
    }

    @Test
    fun transactionDao_reactiveFlow() = runBlocking {
        // Given - Initial state
        val flow = transactionDao.getAllTransactions()
        var emissions = 0

        // When - Collect flow and perform operations
        val initial = flow.first()
        emissions++

        transactionDao.insert(createTestTransaction(smsId = 1L))
        val afterInsert = flow.first()
        emissions++

        // Then - Flow emits updates
        assertTrue(emissions >= 2)
        assertEquals(0, initial.size)
        assertEquals(1, afterInsert.size)
    }

    // ==================== Category Override DAO Tests ====================

    @Test
    fun categoryOverrideDao_insertAndRetrieve() = runBlocking {
        // Given - First insert transaction (foreign key requirement)
        transactionDao.insert(createTestTransaction(smsId = 1L))
        val override = CategoryOverrideEntity(smsId = 1L, categoryId = "food")

        // When
        categoryOverrideDao.insert(override)

        // Then
        val retrieved = categoryOverrideDao.getOverride(1L)
        assertNotNull(retrieved)
        assertEquals("food", retrieved?.categoryId)
    }

    @Test
    fun categoryOverrideDao_updateOnConflict() = runBlocking {
        // Given
        transactionDao.insert(createTestTransaction(smsId = 1L))
        categoryOverrideDao.insert(CategoryOverrideEntity(smsId = 1L, categoryId = "food"))

        // When - Update with same smsId
        categoryOverrideDao.insert(CategoryOverrideEntity(smsId = 1L, categoryId = "transport"))

        // Then - Should have replaced
        val retrieved = categoryOverrideDao.getOverride(1L)
        assertEquals("transport", retrieved?.categoryId)
        assertEquals(1, categoryOverrideDao.getOverrideCount())
    }

    @Test
    fun categoryOverrideDao_getAllAsMap() = runBlocking {
        // Given
        transactionDao.insertAll(
            listOf(
                createTestTransaction(smsId = 1L),
                createTestTransaction(smsId = 2L),
                createTestTransaction(smsId = 3L)
            )
        )
        categoryOverrideDao.insertAll(
            listOf(
                CategoryOverrideEntity(smsId = 1L, categoryId = "food"),
                CategoryOverrideEntity(smsId = 2L, categoryId = "transport"),
                CategoryOverrideEntity(smsId = 3L, categoryId = "shopping")
            )
        )

        // When
        val allOverrides = categoryOverrideDao.getAllOverrides()

        // Then
        assertEquals(3, allOverrides.size)
    }

    @Test
    fun categoryOverrideDao_deleteOverride() = runBlocking {
        // Given
        transactionDao.insert(createTestTransaction(smsId = 1L))
        categoryOverrideDao.insert(CategoryOverrideEntity(smsId = 1L, categoryId = "food"))

        // When
        val deletedCount = categoryOverrideDao.deleteOverride(1L)

        // Then
        assertEquals(1, deletedCount)
        assertNull(categoryOverrideDao.getOverride(1L))
    }

    @Test
    fun categoryOverrideDao_cascadeDelete() = runBlocking {
        // Given - Transaction with override
        transactionDao.insert(createTestTransaction(smsId = 1L))
        categoryOverrideDao.insert(CategoryOverrideEntity(smsId = 1L, categoryId = "food"))

        // When - Delete transaction
        transactionDao.deleteById(1L)

        // Then - Override should be cascade deleted
        assertNull(categoryOverrideDao.getOverride(1L))
        assertEquals(0, categoryOverrideDao.getOverrideCount())
    }

    // ==================== Sync Metadata DAO Tests ====================

    @Test
    fun syncMetadataDao_insertAndRetrieve() = runBlocking {
        // Given
        val metadata = SyncMetadataEntity(key = "lastSyncTimestamp", value = "1234567890")

        // When
        syncMetadataDao.insert(metadata)

        // Then
        val retrieved = syncMetadataDao.getValue("lastSyncTimestamp")
        assertEquals("1234567890", retrieved)
    }

    @Test
    fun syncMetadataDao_updateOnConflict() = runBlocking {
        // Given
        syncMetadataDao.insert(SyncMetadataEntity(key = "lastSyncTimestamp", value = "1000"))

        // When - Update same key
        syncMetadataDao.insert(SyncMetadataEntity(key = "lastSyncTimestamp", value = "2000"))

        // Then
        val retrieved = syncMetadataDao.getValue("lastSyncTimestamp")
        assertEquals("2000", retrieved)
        assertEquals(1, syncMetadataDao.getMetadataCount())
    }

    @Test
    fun syncMetadataDao_multipleKeys() = runBlocking {
        // Given
        syncMetadataDao.insertAll(
            listOf(
                SyncMetadataEntity(key = "lastSyncTimestamp", value = "1000"),
                SyncMetadataEntity(key = "lastProcessedSmsId", value = "999")
            )
        )

        // When
        val allMetadata = syncMetadataDao.getAllMetadata()

        // Then
        assertEquals(2, allMetadata.size)
        assertEquals("1000", syncMetadataDao.getValue("lastSyncTimestamp"))
        assertEquals("999", syncMetadataDao.getValue("lastProcessedSmsId"))
    }

    @Test
    fun syncMetadataDao_deleteByKey() = runBlocking {
        // Given
        syncMetadataDao.insert(SyncMetadataEntity(key = "testKey", value = "testValue"))

        // When
        val deletedCount = syncMetadataDao.deleteByKey("testKey")

        // Then
        assertEquals(1, deletedCount)
        assertNull(syncMetadataDao.getValue("testKey"))
    }

    // ==================== Query Performance Tests ====================

    @Test
    fun performance_batchInsert100Transactions() = runBlocking {
        // Given
        val transactions = (1L..100L).map { id ->
            createTestTransaction(
                smsId = id,
                amount = id * 10.0,
                date = System.currentTimeMillis() - (id * 1000)
            )
        }

        // When
        val time = measureTimeMillis {
            transactionDao.insertAll(transactions)
        }

        // Then
        println("Time to insert 100 transactions: ${time}ms")
        assertEquals(100, transactionDao.getTransactionCount())
        assertTrue("Batch insert should be fast (< 500ms)", time < 500)
    }

    @Test
    fun performance_queryByTypeWithIndex() = runBlocking {
        // Given - Insert mix of transaction types
        val transactions = (1L..100L).map { id ->
            createTestTransaction(
                smsId = id,
                type = if (id % 2 == 0L) TransactionType.DEBIT else TransactionType.CREDIT
            )
        }
        transactionDao.insertAll(transactions)

        // When - Query by type (should use index)
        val time = measureTimeMillis {
            transactionDao.getTransactionsByType(TransactionType.DEBIT).first()
        }

        // Then
        println("Time to query by type: ${time}ms")
        assertTrue("Query by type should be fast (< 100ms)", time < 100)
    }

    @Test
    fun performance_queryByDateRangeWithIndex() = runBlocking {
        // Given - Insert transactions with various dates
        val now = System.currentTimeMillis()
        val transactions = (1L..100L).map { id ->
            createTestTransaction(
                smsId = id,
                date = now - (id * 3600000) // 1 hour apart
            )
        }
        transactionDao.insertAll(transactions)

        // When - Query date range (should use index)
        val startDate = now - (50 * 3600000)
        val endDate = now
        val time = measureTimeMillis {
            transactionDao.getTransactionsByDateRange(startDate, endDate).first()
        }

        // Then
        println("Time to query by date range: ${time}ms")
        assertTrue("Query by date range should be fast (< 100ms)", time < 100)
    }

    @Test
    fun performance_concurrentOperations() = runBlocking {
        // Given
        val transactions = (1L..50L).map { createTestTransaction(smsId = it) }

        // When - Perform multiple operations
        val time = measureTimeMillis {
            transactionDao.insertAll(transactions)

            // Insert overrides for transactions
            transactions.forEach { tx ->
                categoryOverrideDao.insert(
                    CategoryOverrideEntity(smsId = tx.smsId, categoryId = "category_${tx.smsId}")
                )
            }

            // Query operations
            transactionDao.getTransactionCount()
            categoryOverrideDao.getOverrideCount()
            transactionDao.getTransactionsByType(TransactionType.DEBIT).first()
        }

        // Then
        println("Time for concurrent operations: ${time}ms")
        assertEquals(50, transactionDao.getTransactionCount())
        assertEquals(50, categoryOverrideDao.getOverrideCount())
        assertTrue("Concurrent operations should complete reasonably fast (< 1000ms)", time < 1000)
    }

    // ==================== Integration Tests ====================

    @Test
    fun integration_fullTransactionLifecycle() = runBlocking {
        // 1. Insert transaction
        val transaction = createTestTransaction(smsId = 1L, amount = 100.0)
        transactionDao.insert(transaction)
        assertEquals(1, transactionDao.getTransactionCount())

        // 2. Add category override
        categoryOverrideDao.insert(CategoryOverrideEntity(smsId = 1L, categoryId = "food"))
        assertEquals("food", categoryOverrideDao.getOverride(1L)?.categoryId)

        // 3. Add sync metadata
        syncMetadataDao.insert(SyncMetadataEntity(key = "lastSyncTimestamp", value = "123456"))
        assertEquals("123456", syncMetadataDao.getValue("lastSyncTimestamp"))

        // 4. Update transaction (replace)
        val updated = createTestTransaction(smsId = 1L, amount = 200.0)
        transactionDao.insert(updated)
        val retrieved = transactionDao.getAllTransactionsSnapshot().first()
        assertEquals(200.0, retrieved.amount, 0.01)

        // 5. Delete transaction (cascade delete override)
        transactionDao.deleteById(1L)
        assertEquals(0, transactionDao.getTransactionCount())
        assertNull(categoryOverrideDao.getOverride(1L))

        // 6. Metadata should still exist (no cascade)
        assertEquals("123456", syncMetadataDao.getValue("lastSyncTimestamp"))
    }

    @Test
    fun integration_multipleTransactionsWithOverrides() = runBlocking {
        // Given - Multiple transactions with overrides
        val transactions = (1L..10L).map { id ->
            createTestTransaction(
                smsId = id,
                amount = id * 100.0,
                type = if (id % 2 == 0L) TransactionType.DEBIT else TransactionType.CREDIT
            )
        }

        // When
        transactionDao.insertAll(transactions)
        transactions.forEach { tx ->
            categoryOverrideDao.insert(
                CategoryOverrideEntity(smsId = tx.smsId, categoryId = "category_${tx.smsId}")
            )
        }

        // Then
        assertEquals(10, transactionDao.getTransactionCount())
        assertEquals(10, categoryOverrideDao.getOverrideCount())

        val debits = transactionDao.getTransactionsByType(TransactionType.DEBIT).first()
        val credits = transactionDao.getTransactionsByType(TransactionType.CREDIT).first()
        assertEquals(5, debits.size)
        assertEquals(5, credits.size)
    }

    @Test
    fun integration_deleteAllClearsDatabase() = runBlocking {
        // Given - Populated database
        transactionDao.insertAll((1L..10L).map { createTestTransaction(smsId = it) })
        categoryOverrideDao.insertAll((1L..10L).map {
            CategoryOverrideEntity(smsId = it, categoryId = "cat_$it")
        })
        syncMetadataDao.insert(SyncMetadataEntity(key = "test", value = "value"))

        // When - Delete all
        transactionDao.deleteAll()
        categoryOverrideDao.deleteAll()
        syncMetadataDao.deleteAll()

        // Then - Database should be empty
        assertEquals(0, transactionDao.getTransactionCount())
        assertEquals(0, categoryOverrideDao.getOverrideCount())
        assertEquals(0, syncMetadataDao.getMetadataCount())
    }
}
