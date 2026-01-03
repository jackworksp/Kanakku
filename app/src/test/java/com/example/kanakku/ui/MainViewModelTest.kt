package com.example.kanakku.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.category.CategoryManager
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionSource
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for MainViewModel with focus on manual transaction functionality.
 *
 * Tests cover:
 * - Saving manual transactions with all fields
 * - Updating manual transactions with validation
 * - Deleting manual transactions
 * - Error handling for all operations
 * - Category management for manual transactions
 * - UI state management (loading, errors)
 * - Transaction list refresh after mutations
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainViewModelTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // Set up test dispatcher for coroutines
        Dispatchers.setMain(testDispatcher)

        // Create in-memory database for testing
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        repository = TransactionRepository(database)

        // Create ViewModel and initialize repository via reflection
        viewModel = MainViewModel()

        // Use reflection to set the private repository field
        val repositoryField = MainViewModel::class.java.getDeclaredField("repository")
        repositoryField.isAccessible = true
        repositoryField.set(viewModel, repository)
    }

    @After
    fun teardown() {
        database.close()
        Dispatchers.resetMain()
    }

    // ==================== Helper Functions ====================

    private val testCategory = Category(
        id = "food",
        name = "Food & Dining",
        icon = "🍔",
        color = "#FF5722"
    )

    // ==================== Save Manual Transaction Tests ====================

    @Test
    fun saveManualTransaction_savesSuccessfully() = runTest {
        // Given
        var callbackInvoked = false

        // When
        viewModel.saveManualTransaction(
            amount = 150.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Coffee Shop",
            date = System.currentTimeMillis(),
            notes = "Morning coffee",
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertNull(uiState.errorMessage)
        assertEquals(1, uiState.transactions.size)
        assertTrue(callbackInvoked)

        // Verify transaction properties
        val savedTransaction = uiState.transactions.first()
        assertEquals(150.0, savedTransaction.amount, 0.01)
        assertEquals(TransactionType.DEBIT, savedTransaction.type)
        assertEquals("Coffee Shop", savedTransaction.merchant)
        assertEquals("Morning coffee", savedTransaction.notes)
        assertEquals(TransactionSource.MANUAL, savedTransaction.source)
    }

    @Test
    fun saveManualTransaction_withAllFields_savesCorrectly() = runTest {
        // Given
        val testDate = 1704067200000L // 2024-01-01 00:00:00 UTC
        var callbackInvoked = false

        // When
        viewModel.saveManualTransaction(
            amount = 500.50,
            type = TransactionType.CREDIT,
            category = testCategory,
            merchant = "Salary Inc.",
            date = testDate,
            notes = "Monthly salary deposit",
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        // Then
        assertTrue(callbackInvoked)
        val savedTransaction = viewModel.uiState.value.transactions.first()
        assertEquals(500.50, savedTransaction.amount, 0.01)
        assertEquals(TransactionType.CREDIT, savedTransaction.type)
        assertEquals("Salary Inc.", savedTransaction.merchant)
        assertEquals(testDate, savedTransaction.date)
        assertEquals("Monthly salary deposit", savedTransaction.notes)
    }

    @Test
    fun saveManualTransaction_withoutNotes_savesWithNullNotes() = runTest {
        // Given
        var callbackInvoked = false

        // When
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Quick Mart",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        // Then
        assertTrue(callbackInvoked)
        val savedTransaction = viewModel.uiState.value.transactions.first()
        assertNull(savedTransaction.notes)
    }

    @Test
    fun saveManualTransaction_withBlankNotes_savesWithNullNotes() = runTest {
        // Given
        var callbackInvoked = false

        // When
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Quick Mart",
            date = System.currentTimeMillis(),
            notes = "   ", // Blank string
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        // Then
        assertTrue(callbackInvoked)
        val savedTransaction = viewModel.uiState.value.transactions.first()
        assertNull(savedTransaction.notes) // Should be null, not blank
    }

    @Test
    fun saveManualTransaction_setsLoadingState() = runTest {
        // When - Start save operation
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Test",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )

        // Then - Loading should be true initially
        assertTrue(viewModel.uiState.value.isLoading)

        // Complete coroutines
        advanceUntilIdle()

        // Then - Loading should be false after completion
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun saveManualTransaction_appliesCategoryOverride() = runTest {
        // Given
        var savedTransactionId: Long = 0

        // When
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Restaurant",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()

        // Get the saved transaction ID
        savedTransactionId = viewModel.uiState.value.transactions.first().smsId

        // Then - Category override should be applied
        val categoryMap = viewModel.categoryMap.value
        assertTrue(categoryMap.containsKey(savedTransactionId))
        assertEquals(testCategory, categoryMap[savedTransactionId])
    }

    @Test
    fun saveManualTransaction_refreshesTransactionList() = runTest {
        // Given - Save initial transaction
        repository.saveTransaction(
            ParsedTransaction(
                smsId = 1L,
                amount = 50.0,
                type = TransactionType.DEBIT,
                merchant = "Old Store",
                accountNumber = null,
                referenceNumber = null,
                date = System.currentTimeMillis(),
                rawSms = "SMS",
                senderAddress = "VM-BANK",
                source = TransactionSource.SMS
            )
        )

        // When - Save manual transaction
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "New Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()

        // Then - Transaction list should contain both transactions
        val transactions = viewModel.uiState.value.transactions
        assertEquals(2, transactions.size)
    }

    @Test
    fun saveManualTransaction_handlesRepositoryError() = runTest {
        // Given - Close database to cause error
        database.close()
        var callbackInvoked = false

        // When
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Test",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        // Then
        assertFalse(callbackInvoked) // Callback should NOT be invoked on error
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertNotNull(uiState.errorMessage)
        assertTrue(uiState.errorMessage!!.contains("Failed to save"))
    }

    @Test
    fun saveManualTransaction_multipleTransactions_allSavedWithUniqueIds() = runTest {
        // When - Save multiple transactions
        repeat(3) { i ->
            viewModel.saveManualTransaction(
                amount = (i + 1) * 100.0,
                type = TransactionType.DEBIT,
                category = testCategory,
                merchant = "Store $i",
                date = System.currentTimeMillis(),
                notes = "Transaction $i",
                onSuccess = {}
            )
            advanceUntilIdle()
        }

        // Then - All transactions should be saved with unique IDs
        val transactions = viewModel.uiState.value.transactions
        assertEquals(3, transactions.size)

        val ids = transactions.map { it.smsId }.toSet()
        assertEquals(3, ids.size) // All IDs should be unique
    }

    // ==================== Update Manual Transaction Tests ====================

    @Test
    fun updateManualTransaction_updatesSuccessfully() = runTest {
        // Given - Save a manual transaction first
        var savedTransactionId: Long = 0
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Original Store",
            date = System.currentTimeMillis(),
            notes = "Original notes",
            onSuccess = {}
        )
        advanceUntilIdle()
        savedTransactionId = viewModel.uiState.value.transactions.first().smsId

        // When - Update the transaction
        var updateCallbackInvoked = false
        viewModel.updateManualTransaction(
            transactionId = savedTransactionId,
            amount = 200.0,
            type = TransactionType.CREDIT,
            category = testCategory,
            merchant = "Updated Store",
            date = System.currentTimeMillis(),
            notes = "Updated notes",
            onSuccess = { updateCallbackInvoked = true }
        )
        advanceUntilIdle()

        // Then
        assertTrue(updateCallbackInvoked)
        val updatedTransaction = viewModel.uiState.value.transactions.first()
        assertEquals(savedTransactionId, updatedTransaction.smsId) // ID should remain same
        assertEquals(200.0, updatedTransaction.amount, 0.01)
        assertEquals(TransactionType.CREDIT, updatedTransaction.type)
        assertEquals("Updated Store", updatedTransaction.merchant)
        assertEquals("Updated notes", updatedTransaction.notes)
    }

    @Test
    fun updateManualTransaction_setsLoadingState() = runTest {
        // Given - Existing manual transaction
        var transactionId: Long = 0
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()
        transactionId = viewModel.uiState.value.transactions.first().smsId

        // When - Start update operation
        viewModel.updateManualTransaction(
            transactionId = transactionId,
            amount = 200.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )

        // Then - Loading should be true initially
        assertTrue(viewModel.uiState.value.isLoading)

        // Complete coroutines
        advanceUntilIdle()

        // Then - Loading should be false after completion
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun updateManualTransaction_updatesCategoryOverride() = runTest {
        // Given - Manual transaction with category
        val originalCategory = testCategory
        val updatedCategory = Category(
            id = "transport",
            name = "Transport",
            icon = "🚗",
            color = "#2196F3"
        )

        var transactionId: Long = 0
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = originalCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()
        transactionId = viewModel.uiState.value.transactions.first().smsId

        // When - Update with different category
        viewModel.updateManualTransaction(
            transactionId = transactionId,
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = updatedCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()

        // Then - Category should be updated
        val categoryMap = viewModel.categoryMap.value
        assertEquals(updatedCategory, categoryMap[transactionId])
    }

    @Test
    fun updateManualTransaction_nonExistentTransaction_showsError() = runTest {
        // Given
        val nonExistentId = 99999L
        var callbackInvoked = false

        // When - Try to update non-existent transaction
        viewModel.updateManualTransaction(
            transactionId = nonExistentId,
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        // Then
        assertFalse(callbackInvoked)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("not found"))
    }

    @Test
    fun updateManualTransaction_handlesRepositoryError() = runTest {
        // Given - Save transaction first
        var transactionId: Long = 0
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()
        transactionId = viewModel.uiState.value.transactions.first().smsId

        // Close database to cause error
        database.close()
        var callbackInvoked = false

        // When - Try to update
        viewModel.updateManualTransaction(
            transactionId = transactionId,
            amount = 200.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        // Then
        assertFalse(callbackInvoked)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Failed to update"))
    }

    @Test
    fun updateManualTransaction_refreshesTransactionList() = runTest {
        // Given - Save transaction
        var transactionId: Long = 0
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Original",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()
        transactionId = viewModel.uiState.value.transactions.first().smsId
        val originalCount = viewModel.uiState.value.transactions.size

        // When - Update transaction
        viewModel.updateManualTransaction(
            transactionId = transactionId,
            amount = 200.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Updated",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()

        // Then - Transaction list should still have same count but updated values
        assertEquals(originalCount, viewModel.uiState.value.transactions.size)
        assertEquals("Updated", viewModel.uiState.value.transactions.first().merchant)
    }

    // ==================== Delete Transaction Tests ====================

    @Test
    fun deleteTransaction_deletesSuccessfully() = runTest {
        // Given - Save a manual transaction
        var transactionId: Long = 0
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "To Delete",
            date = System.currentTimeMillis(),
            notes = "Will be deleted",
            onSuccess = {}
        )
        advanceUntilIdle()
        transactionId = viewModel.uiState.value.transactions.first().smsId

        // When - Delete the transaction
        var deleteCallbackInvoked = false
        viewModel.deleteTransaction(
            transactionId = transactionId,
            onSuccess = { deleteCallbackInvoked = true }
        )
        advanceUntilIdle()

        // Then
        assertTrue(deleteCallbackInvoked)
        assertEquals(0, viewModel.uiState.value.transactions.size)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun deleteTransaction_removesCategoryOverride() = runTest {
        // Given - Manual transaction with category
        var transactionId: Long = 0
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()
        transactionId = viewModel.uiState.value.transactions.first().smsId

        // Verify category exists
        assertTrue(viewModel.categoryMap.value.containsKey(transactionId))

        // When - Delete transaction
        viewModel.deleteTransaction(transactionId) {}
        advanceUntilIdle()

        // Then - Category override should be removed
        assertFalse(viewModel.categoryMap.value.containsKey(transactionId))
    }

    @Test
    fun deleteTransaction_setsLoadingState() = runTest {
        // Given - Existing transaction
        var transactionId: Long = 0
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()
        transactionId = viewModel.uiState.value.transactions.first().smsId

        // When - Start delete operation
        viewModel.deleteTransaction(transactionId) {}

        // Then - Loading should be true initially
        assertTrue(viewModel.uiState.value.isLoading)

        // Complete coroutines
        advanceUntilIdle()

        // Then - Loading should be false after completion
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun deleteTransaction_nonExistentTransaction_showsError() = runTest {
        // Given
        val nonExistentId = 99999L
        var callbackInvoked = false

        // When - Try to delete non-existent transaction
        viewModel.deleteTransaction(
            transactionId = nonExistentId,
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        // Then
        assertFalse(callbackInvoked)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("not found"))
    }

    @Test
    fun deleteTransaction_handlesRepositoryError() = runTest {
        // Given - Save transaction first
        var transactionId: Long = 0
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()
        transactionId = viewModel.uiState.value.transactions.first().smsId

        // Close database to cause error
        database.close()
        var callbackInvoked = false

        // When - Try to delete
        viewModel.deleteTransaction(
            transactionId = transactionId,
            onSuccess = { callbackInvoked = true }
        )
        advanceUntilIdle()

        // Then
        assertFalse(callbackInvoked)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Failed to delete"))
    }

    @Test
    fun deleteTransaction_refreshesTransactionList() = runTest {
        // Given - Save two transactions
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store 1",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()
        val firstId = viewModel.uiState.value.transactions.first().smsId

        viewModel.saveManualTransaction(
            amount = 200.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store 2",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()

        // When - Delete first transaction
        viewModel.deleteTransaction(firstId) {}
        advanceUntilIdle()

        // Then - Transaction list should have 1 transaction
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("Store 2", viewModel.uiState.value.transactions.first().merchant)
    }

    // ==================== Integration Tests ====================

    @Test
    fun manualTransactionWorkflow_fullCycle() = runTest {
        // 1. Save manual transaction
        var transactionId: Long = 0
        viewModel.saveManualTransaction(
            amount = 150.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Coffee Shop",
            date = System.currentTimeMillis(),
            notes = "Morning coffee",
            onSuccess = {}
        )
        advanceUntilIdle()

        // Verify saved
        assertEquals(1, viewModel.uiState.value.transactions.size)
        val saved = viewModel.uiState.value.transactions.first()
        transactionId = saved.smsId
        assertEquals(150.0, saved.amount, 0.01)
        assertEquals("Morning coffee", saved.notes)

        // 2. Update transaction
        viewModel.updateManualTransaction(
            transactionId = transactionId,
            amount = 175.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Coffee Shop",
            date = System.currentTimeMillis(),
            notes = "Coffee + snack",
            onSuccess = {}
        )
        advanceUntilIdle()

        // Verify updated
        val updated = viewModel.uiState.value.transactions.first()
        assertEquals(175.0, updated.amount, 0.01)
        assertEquals("Coffee + snack", updated.notes)

        // 3. Delete transaction
        viewModel.deleteTransaction(transactionId) {}
        advanceUntilIdle()

        // Verify deleted
        assertEquals(0, viewModel.uiState.value.transactions.size)
        assertFalse(viewModel.categoryMap.value.containsKey(transactionId))
    }

    @Test
    fun clearError_clearsErrorMessage() = runTest {
        // Given - Trigger an error
        database.close()
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()

        // Verify error exists
        assertNotNull(viewModel.uiState.value.errorMessage)

        // When - Clear error
        viewModel.clearError()

        // Then - Error should be cleared
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun multipleOperations_maintainDataIntegrity() = runTest {
        // Given - Save multiple transactions
        val ids = mutableListOf<Long>()

        repeat(5) { i ->
            viewModel.saveManualTransaction(
                amount = (i + 1) * 100.0,
                type = if (i % 2 == 0) TransactionType.DEBIT else TransactionType.CREDIT,
                category = testCategory,
                merchant = "Store $i",
                date = System.currentTimeMillis(),
                notes = "Transaction $i",
                onSuccess = {}
            )
            advanceUntilIdle()
            ids.add(viewModel.uiState.value.transactions.first().smsId)
        }

        // When - Update some, delete others
        viewModel.updateManualTransaction(
            transactionId = ids[0],
            amount = 999.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Updated Store",
            date = System.currentTimeMillis(),
            notes = "Updated",
            onSuccess = {}
        )
        advanceUntilIdle()

        viewModel.deleteTransaction(ids[2]) {}
        advanceUntilIdle()

        viewModel.deleteTransaction(ids[4]) {}
        advanceUntilIdle()

        // Then - Final state should be correct
        assertEquals(3, viewModel.uiState.value.transactions.size)

        // Verify updated transaction
        val updatedTransaction = viewModel.uiState.value.transactions.find { it.smsId == ids[0] }
        assertNotNull(updatedTransaction)
        assertEquals(999.0, updatedTransaction?.amount, 0.01)
    }

    @Test
    fun errorHandling_doesNotLeaveLoadingState() = runTest {
        // Given - Close database to cause errors
        database.close()

        // When - Try various operations that will fail
        viewModel.saveManualTransaction(
            amount = 100.0,
            type = TransactionType.DEBIT,
            category = testCategory,
            merchant = "Store",
            date = System.currentTimeMillis(),
            notes = null,
            onSuccess = {}
        )
        advanceUntilIdle()

        // Then - Loading should be false even after error
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
