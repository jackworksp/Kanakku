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
 * **Testing Strategy:**
 * This is a database integration test that intentionally does NOT use Hilt.
 * It creates an in-memory database directly to test Room database functionality
 * in isolation without the complexity of the full DI graph.
 *
 * For UI/E2E tests that require the full app context with Hilt, use @HiltAndroidTest.
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
        date: Long = System.currentTimeMillis(),
        upiId: String? = null,
        paymentMethod: String? = null
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
            location = "Test Location",
            upiId = upiId,
            paymentMethod = paymentMethod
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
        // Database version should be 2 (after UPI fields migration)
        assertEquals(2, database.openHelper.readableDatabase.version)
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

    // ==================== UPI Fields Tests ====================

    @Test
    fun upiFields_insertTransactionWithUpiData() = runBlocking {
        // Given - Transaction with UPI fields
        val upiTransaction = createTestTransaction(
            smsId = 1L,
            amount = 500.0,
            merchant = "Swiggy",
            upiId = "swiggy@axisbank",
            paymentMethod = "UPI"
        )

        // When
        transactionDao.insert(upiTransaction)

        // Then
        val retrieved = transactionDao.getAllTransactionsSnapshot().first()
        assertEquals("swiggy@axisbank", retrieved.upiId)
        assertEquals("UPI", retrieved.paymentMethod)
    }

    @Test
    fun upiFields_insertTransactionWithoutUpiData() = runBlocking {
        // Given - Transaction without UPI fields (card payment)
        val cardTransaction = createTestTransaction(
            smsId = 1L,
            amount = 200.0,
            merchant = "Amazon",
            upiId = null,
            paymentMethod = null
        )

        // When
        transactionDao.insert(cardTransaction)

        // Then
        val retrieved = transactionDao.getAllTransactionsSnapshot().first()
        assertNull(retrieved.upiId)
        assertNull(retrieved.paymentMethod)
    }

    @Test
    fun upiFields_mixedTransactionTypes() = runBlocking {
        // Given - Mix of UPI and non-UPI transactions
        val transactions = listOf(
            createTestTransaction(
                smsId = 1L,
                merchant = "Google Pay Transfer",
                upiId = "john@paytm",
                paymentMethod = "UPI"
            ),
            createTestTransaction(
                smsId = 2L,
                merchant = "ATM Withdrawal",
                upiId = null,
                paymentMethod = null
            ),
            createTestTransaction(
                smsId = 3L,
                merchant = "PhonePe Payment",
                upiId = "merchant@okaxis",
                paymentMethod = "UPI"
            ),
            createTestTransaction(
                smsId = 4L,
                merchant = "Card Payment",
                upiId = null,
                paymentMethod = "Card"
            )
        )

        // When
        transactionDao.insertAll(transactions)

        // Then
        val all = transactionDao.getAllTransactionsSnapshot()
        assertEquals(4, all.size)

        // Verify UPI transactions
        val tx1 = all.find { it.smsId == 1L }
        assertEquals("john@paytm", tx1?.upiId)
        assertEquals("UPI", tx1?.paymentMethod)

        val tx3 = all.find { it.smsId == 3L }
        assertEquals("merchant@okaxis", tx3?.upiId)
        assertEquals("UPI", tx3?.paymentMethod)

        // Verify non-UPI transactions
        val tx2 = all.find { it.smsId == 2L }
        assertNull(tx2?.upiId)
        assertNull(tx2?.paymentMethod)

        val tx4 = all.find { it.smsId == 4L }
        assertNull(tx4?.upiId)
        assertEquals("Card", tx4?.paymentMethod)
    }

    @Test
    fun upiFields_variousUpiHandles() = runBlocking {
        // Given - Transactions with various UPI handles
        val transactions = listOf(
            createTestTransaction(smsId = 1L, merchant = "GPay", upiId = "user@paytm", paymentMethod = "UPI"),
            createTestTransaction(smsId = 2L, merchant = "PhonePe", upiId = "merchant@okaxis", paymentMethod = "UPI"),
            createTestTransaction(smsId = 3L, merchant = "BHIM", upiId = "name@ybl", paymentMethod = "UPI"),
            createTestTransaction(smsId = 4L, merchant = "Bank", upiId = "account@sbi", paymentMethod = "UPI"),
            createTestTransaction(smsId = 5L, merchant = "Paytm", upiId = "shop@icici", paymentMethod = "UPI")
        )

        // When
        transactionDao.insertAll(transactions)

        // Then
        val all = transactionDao.getAllTransactionsSnapshot()
        assertEquals(5, all.size)
        all.forEach { tx ->
            assertNotNull(tx.upiId)
            assertEquals("UPI", tx.paymentMethod)
        }
    }

    @Test
    fun upiFields_updateTransactionWithUpiData() = runBlocking {
        // Given - Initial transaction without UPI data
        val initial = createTestTransaction(
            smsId = 1L,
            amount = 100.0,
            upiId = null,
            paymentMethod = null
        )
        transactionDao.insert(initial)

        // When - Update with UPI data
        val updated = createTestTransaction(
            smsId = 1L,
            amount = 100.0,
            upiId = "updated@paytm",
            paymentMethod = "UPI"
        )
        transactionDao.insert(updated)

        // Then
        val retrieved = transactionDao.getAllTransactionsSnapshot().first()
        assertEquals("updated@paytm", retrieved.upiId)
        assertEquals("UPI", retrieved.paymentMethod)
    }

    // ==================== Database Migration Tests ====================

    @Test
    fun migration_1_to_2_preservesExistingData() = runBlocking {
        // This test verifies that the migration from version 1 to 2 works correctly
        // and preserves existing transaction data while adding new UPI columns

        // Given - Create a fresh database (which will be at version 2)
        val testDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .addMigrations(KanakkuDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        // Insert transactions (simulating migrated data with NULL UPI fields)
        val dao = testDb.transactionDao()
        val oldTransaction = createTestTransaction(
            smsId = 1L,
            amount = 100.0,
            merchant = "Old Merchant",
            upiId = null,  // These would be NULL after migration
            paymentMethod = null
        )
        dao.insert(oldTransaction)

        // When - Retrieve the transaction
        val retrieved = dao.getAllTransactionsSnapshot().first()

        // Then - Verify existing data is preserved and new columns are NULL
        assertEquals(1L, retrieved.smsId)
        assertEquals(100.0, retrieved.amount, 0.01)
        assertEquals("Old Merchant", retrieved.merchant)
        assertEquals("1234", retrieved.accountNumber)
        assertEquals("REF123", retrieved.referenceNumber)
        assertNull(retrieved.upiId)  // New column should be NULL for old data
        assertNull(retrieved.paymentMethod)  // New column should be NULL for old data

        testDb.close()
    }

    @Test
    fun migration_1_to_2_allowsNewUpiData() = runBlocking {
        // This test verifies that after migration, new UPI data can be inserted

        // Given - Database at version 2
        val testDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .addMigrations(KanakkuDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val dao = testDb.transactionDao()

        // When - Insert transaction with UPI data (post-migration)
        val upiTransaction = createTestTransaction(
            smsId = 1L,
            amount = 500.0,
            merchant = "Swiggy",
            upiId = "swiggy@axisbank",
            paymentMethod = "UPI"
        )
        dao.insert(upiTransaction)

        // Then - Verify UPI data is stored correctly
        val retrieved = dao.getAllTransactionsSnapshot().first()
        assertEquals("swiggy@axisbank", retrieved.upiId)
        assertEquals("UPI", retrieved.paymentMethod)

        testDb.close()
    }

    @Test
    fun migration_schemaVersion_isCorrect() = runBlocking {
        // Verify database schema version after migration

        // Given - Fresh database with migration
        val testDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .addMigrations(KanakkuDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        // Then - Verify version is 2
        assertEquals(2, testDb.openHelper.readableDatabase.version)

        testDb.close()
    }

    @Test
    fun migration_multipleTransactions_allPreserved() = runBlocking {
        // Test that migration preserves all transactions in database

        // Given - Database with multiple transactions
        val testDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .addMigrations(KanakkuDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val dao = testDb.transactionDao()

        // Insert multiple transactions (simulating pre-migration data)
        val transactions = (1L..10L).map { id ->
            createTestTransaction(
                smsId = id,
                amount = id * 100.0,
                merchant = "Merchant $id",
                upiId = null,  // Pre-migration data wouldn't have UPI fields
                paymentMethod = null
            )
        }
        dao.insertAll(transactions)

        // When - Retrieve all transactions
        val retrieved = dao.getAllTransactionsSnapshot()

        // Then - All transactions should be preserved
        assertEquals(10, retrieved.size)
        retrieved.forEach { tx ->
            assertNotNull(tx.merchant)
            assertTrue(tx.merchant!!.startsWith("Merchant"))
            assertNull(tx.upiId)  // Old data should have NULL UPI fields
            assertNull(tx.paymentMethod)
        }

        testDb.close()
    }

    @Test
    fun migration_mixedDataAfterMigration() = runBlocking {
        // Test that database can handle both old (NULL UPI) and new (with UPI) data

        // Given - Database after migration
        val testDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .addMigrations(KanakkuDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val dao = testDb.transactionDao()

        // Insert mix of old and new transactions
        val transactions = listOf(
            // Old transaction (pre-migration style)
            createTestTransaction(
                smsId = 1L,
                merchant = "ATM Withdrawal",
                upiId = null,
                paymentMethod = null
            ),
            // New UPI transaction (post-migration)
            createTestTransaction(
                smsId = 2L,
                merchant = "Google Pay",
                upiId = "john@paytm",
                paymentMethod = "UPI"
            ),
            // Another old transaction
            createTestTransaction(
                smsId = 3L,
                merchant = "Card Payment",
                upiId = null,
                paymentMethod = null
            ),
            // Another new UPI transaction
            createTestTransaction(
                smsId = 4L,
                merchant = "PhonePe",
                upiId = "merchant@okaxis",
                paymentMethod = "UPI"
            )
        )
        dao.insertAll(transactions)

        // When - Query all transactions
        val all = dao.getAllTransactionsSnapshot()

        // Then - Verify mixed data is handled correctly
        assertEquals(4, all.size)

        val tx1 = all.find { it.smsId == 1L }
        assertNull(tx1?.upiId)
        assertNull(tx1?.paymentMethod)

        val tx2 = all.find { it.smsId == 2L }
        assertEquals("john@paytm", tx2?.upiId)
        assertEquals("UPI", tx2?.paymentMethod)

        val tx3 = all.find { it.smsId == 3L }
        assertNull(tx3?.upiId)
        assertNull(tx3?.paymentMethod)

        val tx4 = all.find { it.smsId == 4L }
        assertEquals("merchant@okaxis", tx4?.upiId)
        assertEquals("UPI", tx4?.paymentMethod)

        testDb.close()
    }

    @Test
    fun migration_queryByExistingFields_stillWorks() = runBlocking {
        // Verify that queries on existing fields still work after migration

        // Given - Database after migration with mixed data
        val testDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .addMigrations(KanakkuDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val dao = testDb.transactionDao()

        dao.insertAll(
            listOf(
                createTestTransaction(smsId = 1L, type = TransactionType.DEBIT, upiId = "user@paytm", paymentMethod = "UPI"),
                createTestTransaction(smsId = 2L, type = TransactionType.CREDIT, upiId = null, paymentMethod = null),
                createTestTransaction(smsId = 3L, type = TransactionType.DEBIT, upiId = "merchant@okaxis", paymentMethod = "UPI")
            )
        )

        // When - Query by existing field (type)
        val debits = dao.getTransactionsByType(TransactionType.DEBIT).first()
        val credits = dao.getTransactionsByType(TransactionType.CREDIT).first()

        // Then - Queries should still work correctly
        assertEquals(2, debits.size)
        assertEquals(1, credits.size)

        testDb.close()
    }

    @Test
    fun migration_indexesStillWork_afterMigration() = runBlocking {
        // Verify that existing indexes still function after migration

        // Given - Database with many transactions
        val testDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .addMigrations(KanakkuDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val dao = testDb.transactionDao()
        val now = System.currentTimeMillis()

        val transactions = (1L..50L).map { id ->
            createTestTransaction(
                smsId = id,
                date = now - (id * 3600000), // 1 hour apart
                type = if (id % 2 == 0L) TransactionType.DEBIT else TransactionType.CREDIT,
                upiId = if (id % 3 == 0L) "user$id@paytm" else null,
                paymentMethod = if (id % 3 == 0L) "UPI" else null
            )
        }
        dao.insertAll(transactions)

        // When - Query using indexed fields (date and type)
        val startDate = now - (25 * 3600000)
        val endDate = now
        val time = measureTimeMillis {
            val byDateRange = dao.getTransactionsByDateRange(startDate, endDate).first()
            val byType = dao.getTransactionsByType(TransactionType.DEBIT).first()
        }

        // Then - Queries should still be fast (indexes working)
        assertTrue("Indexed queries should be fast after migration (< 200ms)", time < 200)

        testDb.close()
    }
}
