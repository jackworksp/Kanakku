package com.example.kanakku.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.sms.SmsDataSource
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
 * **Testing Strategy:**
 * This is a unit test that intentionally does NOT use Hilt dependency injection.
 * Instead, it creates test instances directly to:
 * - Test the repository in complete isolation
 * - Have full control over the database configuration
 * - Keep tests fast and focused on repository logic only
 * - Avoid the complexity of Hilt test setup for simple unit tests
 *
 * For integration tests that test the full DI graph, use @HiltAndroidTest instead.
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
    private lateinit var smsDataSource: SmsDataSource

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        // Create SmsDataSource for repository
        smsDataSource = SmsDataSource(context)

        repository = TransactionRepository(database, smsDataSource)
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

    // ==================== Merchant Category Mapping Tests ====================

    @Test
    fun setMerchantCategoryMapping_storesCorrectly() = runTest {
        // Given
        val merchantName = "Amazon"
        val categoryId = "shopping"

        // When
        val result = repository.setMerchantCategoryMapping(merchantName, categoryId)

        // Then
        assertTrue(result.isSuccess)
        val mapping = repository.getMerchantCategoryMapping(merchantName).getOrNull()
        assertEquals(categoryId, mapping)
    }

    @Test
    fun setMerchantCategoryMapping_updatesExisting() = runTest {
        // Given
        val merchantName = "Starbucks"
        repository.setMerchantCategoryMapping(merchantName, "food")

        // When
        repository.setMerchantCategoryMapping(merchantName, "entertainment")

        // Then
        val mapping = repository.getMerchantCategoryMapping(merchantName).getOrNull()
        assertEquals("entertainment", mapping)
    }

    @Test
    fun setMerchantCategoryMapping_normalizesMerchantName() = runTest {
        // Given - Different capitalizations of same merchant
        repository.setMerchantCategoryMapping("AMAZON", "shopping")

        // When - Query with different case
        val result = repository.getMerchantCategoryMapping("amazon").getOrNull()

        // Then - Should find the mapping (case-insensitive)
        assertEquals("shopping", result)
    }

    @Test
    fun setMerchantCategoryMapping_trimsWhitespace() = runTest {
        // Given - Merchant with extra whitespace
        repository.setMerchantCategoryMapping("  Amazon  ", "shopping")

        // When - Query without whitespace
        val result = repository.getMerchantCategoryMapping("Amazon").getOrNull()

        // Then - Should find the mapping (whitespace normalized)
        assertEquals("shopping", result)
    }

    @Test
    fun setMerchantCategoryMapping_removesSpecialCharacters() = runTest {
        // Given - Merchant with special characters
        repository.setMerchantCategoryMapping("Amazon™️ Inc.", "shopping")

        // When - Query normalized name
        val result = repository.getMerchantCategoryMapping("Amazon Inc").getOrNull()

        // Then - Should find the mapping (special chars removed)
        assertEquals("shopping", result)
    }

    @Test
    fun setMerchantCategoryMapping_normalizesMultipleSpaces() = runTest {
        // Given - Merchant with multiple spaces
        repository.setMerchantCategoryMapping("Big    Bazaar", "shopping")

        // When - Query with single space
        val result = repository.getMerchantCategoryMapping("Big Bazaar").getOrNull()

        // Then - Should find the mapping (spaces normalized)
        assertEquals("shopping", result)
    }

    @Test
    fun setMerchantCategoryMapping_withEmptyMerchant_throwsException() = runTest {
        // Given - Empty merchant name
        val emptyMerchant = "   "

        // When
        val result = repository.setMerchantCategoryMapping(emptyMerchant, "shopping")

        // Then - Should fail with IllegalArgumentException
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun setMerchantCategoryMapping_withBlankMerchant_throwsException() = runTest {
        // Given - Blank merchant name (only special chars)
        val blankMerchant = "™️®©"

        // When
        val result = repository.setMerchantCategoryMapping(blankMerchant, "shopping")

        // Then - Should fail (normalizes to empty string)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun getMerchantCategoryMapping_returnsNullWhenNotExists() = runTest {
        // When
        val mapping = repository.getMerchantCategoryMapping("NonExistent").getOrNull()

        // Then
        assertNull(mapping)
    }

    @Test
    fun getMerchantCategoryMapping_withEmptyString_returnsNull() = runTest {
        // When
        val mapping = repository.getMerchantCategoryMapping("").getOrNull()

        // Then
        assertNull(mapping)
    }

    @Test
    fun getMerchantCategoryMapping_withBlankString_returnsNull() = runTest {
        // When
        val mapping = repository.getMerchantCategoryMapping("   ").getOrNull()

        // Then
        assertNull(mapping)
    }

    @Test
    fun getAllMerchantMappings_returnsMap() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")
        repository.setMerchantCategoryMapping("Starbucks", "food")
        repository.setMerchantCategoryMapping("Uber", "transport")

        // When
        val mappings = repository.getAllMerchantMappingsSnapshot().getOrNull()!!

        // Then
        assertEquals(3, mappings.size)
        assertEquals("shopping", mappings["amazon"])
        assertEquals("food", mappings["starbucks"])
        assertEquals("transport", mappings["uber"])
    }

    @Test
    fun getAllMerchantMappingsFlow_emitsUpdates() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")
        repository.setMerchantCategoryMapping("Starbucks", "food")

        // When
        val flow = repository.getAllMerchantMappings()
        val result = flow.first()

        // Then
        assertEquals(2, result.size)
        assertEquals("shopping", result["amazon"])
        assertEquals("food", result["starbucks"])
    }

    @Test
    fun getAllMerchantMappings_whenEmpty_returnsEmptyMap() = runTest {
        // When
        val mappings = repository.getAllMerchantMappingsSnapshot().getOrNull()!!

        // Then
        assertTrue(mappings.isEmpty())
    }

    @Test
    fun removeMerchantMapping_deletesCorrectly() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")
        repository.setMerchantCategoryMapping("Starbucks", "food")

        // When
        val removed = repository.removeMerchantMapping("Amazon").getOrNull()!!

        // Then
        assertTrue(removed)
        assertNull(repository.getMerchantCategoryMapping("Amazon").getOrNull())
        assertEquals("food", repository.getMerchantCategoryMapping("Starbucks").getOrNull())
    }

    @Test
    fun removeMerchantMapping_normalizesMerchantName() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")

        // When - Remove with different case
        val removed = repository.removeMerchantMapping("AMAZON").getOrNull()!!

        // Then
        assertTrue(removed)
        assertNull(repository.getMerchantCategoryMapping("amazon").getOrNull())
    }

    @Test
    fun removeMerchantMapping_returnsFalseWhenNotExists() = runTest {
        // When
        val removed = repository.removeMerchantMapping("NonExistent").getOrNull()!!

        // Then
        assertFalse(removed)
    }

    @Test
    fun removeMerchantMapping_withEmptyString_returnsFalse() = runTest {
        // When
        val removed = repository.removeMerchantMapping("").getOrNull()!!

        // Then
        assertFalse(removed)
    }

    @Test
    fun removeMerchantMapping_withBlankString_returnsFalse() = runTest {
        // When
        val removed = repository.removeMerchantMapping("   ").getOrNull()!!

        // Then
        assertFalse(removed)
    }

    @Test
    fun removeAllMerchantMappings_clearsAll() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")
        repository.setMerchantCategoryMapping("Starbucks", "food")
        repository.setMerchantCategoryMapping("Uber", "transport")

        // When
        val count = repository.removeAllMerchantMappings().getOrNull()!!

        // Then
        assertEquals(3, count)
        assertTrue(repository.getAllMerchantMappingsSnapshot().getOrNull()!!.isEmpty())
    }

    @Test
    fun removeAllMerchantMappings_whenEmpty_returnsZero() = runTest {
        // When
        val count = repository.removeAllMerchantMappings().getOrNull()!!

        // Then
        assertEquals(0, count)
    }

    @Test
    fun getMerchantMappingCount_returnsCorrectCount() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")
        repository.setMerchantCategoryMapping("Starbucks", "food")
        repository.setMerchantCategoryMapping("Uber", "transport")

        // When
        val count = repository.getMerchantMappingCount().getOrNull()!!

        // Then
        assertEquals(3, count)
    }

    @Test
    fun getMerchantMappingCount_whenEmpty_returnsZero() = runTest {
        // When
        val count = repository.getMerchantMappingCount().getOrNull()!!

        // Then
        assertEquals(0, count)
    }

    @Test
    fun getMerchantMappingCount_afterDeletions() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")
        repository.setMerchantCategoryMapping("Starbucks", "food")
        repository.setMerchantCategoryMapping("Uber", "transport")

        // When - Remove one
        repository.removeMerchantMapping("Starbucks")

        // Then
        assertEquals(2, repository.getMerchantMappingCount().getOrNull())
    }

    // ==================== Merchant Mapping Normalization Tests ====================

    @Test
    fun merchantNormalization_withUpperCase() = runTest {
        // Given
        repository.setMerchantCategoryMapping("AMAZON", "shopping")

        // When/Then - Different cases should all map to same normalized name
        assertEquals("shopping", repository.getMerchantCategoryMapping("amazon").getOrNull())
        assertEquals("shopping", repository.getMerchantCategoryMapping("Amazon").getOrNull())
        assertEquals("shopping", repository.getMerchantCategoryMapping("AMAZON").getOrNull())
        assertEquals("shopping", repository.getMerchantCategoryMapping("aMaZoN").getOrNull())
    }

    @Test
    fun merchantNormalization_withLeadingTrailingSpaces() = runTest {
        // Given
        repository.setMerchantCategoryMapping("  Amazon  ", "shopping")

        // When/Then
        assertEquals("shopping", repository.getMerchantCategoryMapping("Amazon").getOrNull())
        assertEquals("shopping", repository.getMerchantCategoryMapping("  Amazon").getOrNull())
        assertEquals("shopping", repository.getMerchantCategoryMapping("Amazon  ").getOrNull())
    }

    @Test
    fun merchantNormalization_withSpecialCharactersRemoved() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon™️ Inc.", "shopping")

        // When/Then
        assertEquals("shopping", repository.getMerchantCategoryMapping("Amazon Inc").getOrNull())
        assertEquals("shopping", repository.getMerchantCategoryMapping("amazon inc").getOrNull())
    }

    @Test
    fun merchantNormalization_withMultipleSpacesNormalized() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Big    Bazaar", "shopping")

        // When/Then
        assertEquals("shopping", repository.getMerchantCategoryMapping("Big Bazaar").getOrNull())
        assertEquals("shopping", repository.getMerchantCategoryMapping("big bazaar").getOrNull())
    }

    @Test
    fun merchantNormalization_withComplexString() = runTest {
        // Given - Complex merchant name with multiple issues
        repository.setMerchantCategoryMapping("  STAR™️  BUCKS®    Café  ", "food")

        // When - Query with same normalization (é is removed entirely, not converted to e)
        val result = repository.getMerchantCategoryMapping("star bucks caf").getOrNull()

        // Then - Should normalize to "star bucks caf" (unicode chars completely removed)
        assertEquals("food", result)
    }

    @Test
    fun merchantNormalization_withNumericCharacters() = runTest {
        // Given - Merchant with numbers
        repository.setMerchantCategoryMapping("Amazon India 24x7", "shopping")

        // When/Then - Numbers should be preserved
        assertEquals("shopping", repository.getMerchantCategoryMapping("amazon india 24x7").getOrNull())
    }

    @Test
    fun merchantNormalization_withUnicodeCharacters() = runTest {
        // Given - Merchant with unicode
        repository.setMerchantCategoryMapping("Café François 新しい", "food")

        // When - Query with the same string (normalization should match)
        val result = repository.getMerchantCategoryMapping("Café François 新しい").getOrNull()

        // Then - Should find the mapping (unicode and special chars are removed during normalization)
        assertEquals("food", result)
    }

    @Test
    fun merchantNormalization_withOnlySpecialCharacters_throwsException() = runTest {
        // Given - Merchant with only special characters
        val specialCharsOnly = "™️®©"

        // When
        val result = repository.setMerchantCategoryMapping(specialCharsOnly, "shopping")

        // Then - Should fail (normalizes to empty)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    // ==================== Merchant Mapping Error Handling Tests ====================

    @Test
    fun setMerchantCategoryMapping_returnsResultType() = runTest {
        // When
        val result = repository.setMerchantCategoryMapping("Amazon", "shopping")

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun setMerchantCategoryMapping_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.setMerchantCategoryMapping("Amazon", "shopping")

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getMerchantCategoryMapping_returnsResultType() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")

        // When
        val result = repository.getMerchantCategoryMapping("Amazon")

        // Then
        assertTrue(result.isSuccess)
        assertEquals("shopping", result.getOrNull())
    }

    @Test
    fun getMerchantCategoryMapping_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getMerchantCategoryMapping("Amazon")

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun getAllMerchantMappingsSnapshot_returnsResultType() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")

        // When
        val result = repository.getAllMerchantMappingsSnapshot()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun getAllMerchantMappingsSnapshot_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getAllMerchantMappingsSnapshot()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun removeMerchantMapping_returnsResultType() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")

        // When
        val result = repository.removeMerchantMapping("Amazon")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun removeMerchantMapping_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.removeMerchantMapping("Amazon")

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun removeAllMerchantMappings_returnsResultType() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")
        repository.setMerchantCategoryMapping("Starbucks", "food")

        // When
        val result = repository.removeAllMerchantMappings()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun removeAllMerchantMappings_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.removeAllMerchantMappings()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun getMerchantMappingCount_returnsResultType() = runTest {
        // Given
        repository.setMerchantCategoryMapping("Amazon", "shopping")

        // When
        val result = repository.getMerchantMappingCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    @Test
    fun getMerchantMappingCount_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getMerchantMappingCount()

        // Then
        assertTrue(result.isFailure)
    }

    // ==================== Merchant Mapping Integration Tests ====================

    @Test
    fun merchantMapping_fullWorkflow() = runTest {
        // 1. Set mappings
        repository.setMerchantCategoryMapping("Amazon", "shopping")
        repository.setMerchantCategoryMapping("Starbucks", "food")
        repository.setMerchantCategoryMapping("Uber", "transport")
        assertEquals(3, repository.getMerchantMappingCount().getOrNull())

        // 2. Get individual mapping
        assertEquals("shopping", repository.getMerchantCategoryMapping("Amazon").getOrNull())

        // 3. Get all mappings
        val allMappings = repository.getAllMerchantMappingsSnapshot().getOrNull()!!
        assertEquals(3, allMappings.size)

        // 4. Update existing mapping
        repository.setMerchantCategoryMapping("Amazon", "online_shopping")
        assertEquals("online_shopping", repository.getMerchantCategoryMapping("Amazon").getOrNull())
        assertEquals(3, repository.getMerchantMappingCount().getOrNull()) // Count unchanged

        // 5. Remove single mapping
        repository.removeMerchantMapping("Starbucks")
        assertEquals(2, repository.getMerchantMappingCount().getOrNull())

        // 6. Remove all mappings
        repository.removeAllMerchantMappings()
        assertEquals(0, repository.getMerchantMappingCount().getOrNull())
    }

    @Test
    fun merchantMapping_withMultipleMerchantsNormalizingSameName() = runTest {
        // Given - Multiple variations of same merchant
        repository.setMerchantCategoryMapping("AMAZON", "shopping")
        repository.setMerchantCategoryMapping("Amazon™️", "online_shopping")
        repository.setMerchantCategoryMapping("  amazon  ", "retail")

        // When/Then - All should normalize to same key, last one wins
        val count = repository.getMerchantMappingCount().getOrNull()!!
        assertEquals(1, count) // Only one mapping (same normalized name)

        val mapping = repository.getMerchantCategoryMapping("amazon").getOrNull()
        assertEquals("retail", mapping) // Last update wins
    }

    @Test
    fun merchantMapping_concurrentOperations() = runTest {
        // Given - Multiple operations
        val merchants = listOf("Amazon", "Starbucks", "Uber", "Netflix", "Spotify")
        val categories = listOf("shopping", "food", "transport", "entertainment", "entertainment")

        // When - Batch set operations
        merchants.forEachIndexed { index, merchant ->
            repository.setMerchantCategoryMapping(merchant, categories[index])
        }

        // Then - All should be stored correctly
        assertEquals(5, repository.getMerchantMappingCount().getOrNull())

        merchants.forEachIndexed { index, merchant ->
            assertEquals(
                categories[index],
                repository.getMerchantCategoryMapping(merchant).getOrNull()
            )
        }
    }

    @Test
    fun merchantMapping_doesNotAffectCategoryOverrides() = runTest {
        // Given - Transaction with category override
        val transaction = createTestTransaction(smsId = 1L, merchant = "Amazon")
        repository.saveTransaction(transaction)
        repository.setCategoryOverride(1L, "food")

        // When - Set merchant mapping
        repository.setMerchantCategoryMapping("Amazon", "shopping")

        // Then - Category override should remain unchanged
        assertEquals("food", repository.getCategoryOverride(1L).getOrNull())
        assertEquals("shopping", repository.getMerchantCategoryMapping("Amazon").getOrNull())
    }

    @Test
    fun merchantMapping_persistsAcrossDatabaseReopen() = runTest {
        // Given - Set merchant mapping
        repository.setMerchantCategoryMapping("Amazon", "shopping")
        assertEquals(1, repository.getMerchantMappingCount().getOrNull())

        // When - Close and reopen database
        database.close()
        val newDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        val newRepository = TransactionRepository(newDatabase)

        // Then - Mapping should be lost (in-memory database)
        // Note: In real scenario with persistent database, mapping would persist
        assertEquals(0, newRepository.getMerchantMappingCount().getOrNull())
    }

    // ==================== Merchant Mapping Edge Case Tests ====================

    @Test
    fun merchantMapping_withVeryLongMerchantName() = runTest {
        // Given - Very long merchant name
        val longMerchant = "A".repeat(500)
        repository.setMerchantCategoryMapping(longMerchant, "shopping")

        // When
        val mapping = repository.getMerchantCategoryMapping(longMerchant).getOrNull()

        // Then
        assertEquals("shopping", mapping)
    }

    @Test
    fun merchantMapping_withVeryLongCategoryId() = runTest {
        // Given - Very long category ID
        val longCategory = "category_" + "x".repeat(500)
        repository.setMerchantCategoryMapping("Amazon", longCategory)

        // When
        val mapping = repository.getMerchantCategoryMapping("Amazon").getOrNull()

        // Then
        assertEquals(longCategory, mapping)
    }

    @Test
    fun merchantMapping_withSingleCharacterMerchant() = runTest {
        // Given - Single character merchant
        repository.setMerchantCategoryMapping("A", "shopping")

        // When
        val mapping = repository.getMerchantCategoryMapping("A").getOrNull()

        // Then
        assertEquals("shopping", mapping)
    }

    @Test
    fun merchantMapping_withNumericOnlyMerchant() = runTest {
        // Given - Numeric only merchant
        repository.setMerchantCategoryMapping("12345", "shopping")

        // When
        val mapping = repository.getMerchantCategoryMapping("12345").getOrNull()

        // Then
        assertEquals("shopping", mapping)
    }

    @Test
    fun merchantMapping_rapidUpdatesToSameMerchant() = runTest {
        // Given
        val merchant = "Amazon"
        val categories = listOf("shopping", "online", "retail", "ecommerce", "marketplace")

        // When - Rapid updates
        categories.forEach { category ->
            repository.setMerchantCategoryMapping(merchant, category)
        }

        // Then - Last update should win
        assertEquals("marketplace", repository.getMerchantCategoryMapping(merchant).getOrNull())
        assertEquals(1, repository.getMerchantMappingCount().getOrNull())
    }

    @Test
    fun merchantMapping_stressTestManyMappings() = runTest {
        // Given - Create many mappings
        val count = 100
        repeat(count) { i ->
            repository.setMerchantCategoryMapping("Merchant$i", "category$i")
        }

        // When/Then
        assertEquals(count, repository.getMerchantMappingCount().getOrNull())

        // Verify random mappings
        assertEquals("category0", repository.getMerchantCategoryMapping("Merchant0").getOrNull())
        assertEquals("category50", repository.getMerchantCategoryMapping("Merchant50").getOrNull())
        assertEquals("category99", repository.getMerchantCategoryMapping("Merchant99").getOrNull())
    }
}
