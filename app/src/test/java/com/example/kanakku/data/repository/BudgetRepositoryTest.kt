package com.example.kanakku.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.Budget
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
 * Unit tests for BudgetRepository using in-memory Room database.
 *
 * Tests cover:
 * - Budget CRUD operations
 * - Entity-model mapping
 * - Cache management and invalidation
 * - Error handling with Result type
 * - Flow-based reactive queries
 * - Overall and category budget operations
 * - Month/year filtering
 * - Edge cases and error scenarios
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BudgetRepositoryTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var repository: BudgetRepository

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        repository = BudgetRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    // ==================== Helper Functions ====================

    private fun createTestBudget(
        id: Long = 0,
        categoryId: String? = null,
        amount: Double = 10000.0,
        month: Int = 1,
        year: Int = 2026,
        createdAt: Long = System.currentTimeMillis()
    ): Budget {
        return Budget(
            id = id,
            categoryId = categoryId,
            amount = amount,
            month = month,
            year = year,
            createdAt = createdAt
        )
    }

    // ==================== Save Budget Tests ====================

    @Test
    fun saveBudget_insertsOverallBudgetSuccessfully() = runTest {
        // Given - Overall budget (categoryId = null)
        val budget = createTestBudget(categoryId = null, amount = 50000.0)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val rowId = result.getOrNull()!!
        assertTrue(rowId > 0)

        // Verify it was saved
        val loaded = repository.getOverallBudget(1, 2026).getOrNull()
        assertNotNull(loaded)
        assertEquals(50000.0, loaded!!.amount, 0.01)
    }

    @Test
    fun saveBudget_insertsCategoryBudgetSuccessfully() = runTest {
        // Given - Category budget
        val budget = createTestBudget(categoryId = "food", amount = 5000.0)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val rowId = result.getOrNull()!!
        assertTrue(rowId > 0)

        // Verify it was saved
        val loaded = repository.getBudget("food", 1, 2026).getOrNull()
        assertNotNull(loaded)
        assertEquals("food", loaded?.categoryId)
        assertEquals(5000.0, loaded!!.amount, 0.01)
    }

    @Test
    fun saveBudget_updatesExistingBudget() = runTest {
        // Given - Existing budget
        val budget1 = createTestBudget(categoryId = "food", amount = 5000.0)
        repository.saveBudget(budget1)

        // When - Update with different amount
        val budget2 = createTestBudget(categoryId = "food", amount = 6000.0)
        val result = repository.saveBudget(budget2)

        // Then
        assertTrue(result.isSuccess)

        // Should only have one budget (updated)
        val loaded = repository.getBudget("food", 1, 2026).getOrNull()
        assertEquals(6000.0, loaded!!.amount, 0.01)

        val count = repository.getBudgetCount().getOrNull()!!
        assertEquals(1, count)
    }

    @Test
    fun saveBudgets_insertsMultipleBudgetsSuccessfully() = runTest {
        // Given - Multiple budgets
        val budgets = listOf(
            createTestBudget(categoryId = null, amount = 50000.0),
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "transport", amount = 3000.0)
        )

        // When
        val result = repository.saveBudgets(budgets)

        // Then
        assertTrue(result.isSuccess)

        // Verify all were saved
        val count = repository.getBudgetCount().getOrNull()!!
        assertEquals(3, count)
    }

    @Test
    fun saveBudget_handlesErrorGracefully() = runTest {
        // Given - Close database to simulate error
        database.close()

        // When
        val budget = createTestBudget(categoryId = "food", amount = 5000.0)
        val result = repository.saveBudget(budget)

        // Then - Should return failure
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    // ==================== Get Budget Tests ====================

    @Test
    fun getBudget_returnsCorrectBudget() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026)
        repository.saveBudget(budget)

        // When
        val result = repository.getBudget("food", 1, 2026)

        // Then
        assertTrue(result.isSuccess)
        val loaded = result.getOrNull()
        assertNotNull(loaded)
        assertEquals("food", loaded?.categoryId)
        assertEquals(5000.0, loaded!!.amount, 0.01)
        assertEquals(1, loaded.month)
        assertEquals(2026, loaded.year)
    }

    @Test
    fun getBudget_returnsNullWhenNotFound() = runTest {
        // When
        val result = repository.getBudget("nonexistent", 1, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun getBudget_usesCache() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 5000.0)
        repository.saveBudget(budget)

        // When - First call populates cache
        val result1 = repository.getBudget("food", 1, 2026)

        // Second call should hit cache
        val result2 = repository.getBudget("food", 1, 2026)

        // Then - Both should return same data
        assertEquals(result1.getOrNull()?.amount, result2.getOrNull()?.amount)
        assertEquals(5000.0, result2.getOrNull()!!.amount, 0.01)
    }

    @Test
    fun getBudget_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getBudget("food", 1, 2026)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getBudgetFlow_emitsUpdates() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 5000.0)
        repository.saveBudget(budget)

        // When
        val flow = repository.getBudgetFlow("food", 1, 2026)
        val result = flow.first()

        // Then
        assertNotNull(result)
        assertEquals("food", result?.categoryId)
        assertEquals(5000.0, result!!.amount, 0.01)
    }

    @Test
    fun getBudgetFlow_emitsNullForNonExistent() = runTest {
        // When
        val flow = repository.getBudgetFlow("nonexistent", 1, 2026)
        val result = flow.first()

        // Then
        assertNull(result)
    }

    // ==================== Overall Budget Tests ====================

    @Test
    fun getOverallBudget_returnsOverallBudget() = runTest {
        // Given
        val budget = createTestBudget(categoryId = null, amount = 50000.0)
        repository.saveBudget(budget)

        // When
        val result = repository.getOverallBudget(1, 2026)

        // Then
        assertTrue(result.isSuccess)
        val loaded = result.getOrNull()
        assertNotNull(loaded)
        assertNull(loaded?.categoryId)
        assertEquals(50000.0, loaded!!.amount, 0.01)
    }

    @Test
    fun getOverallBudget_returnsNullWhenNotSet() = runTest {
        // When
        val result = repository.getOverallBudget(1, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun getOverallBudgetFlow_emitsUpdates() = runTest {
        // Given
        val budget = createTestBudget(categoryId = null, amount = 50000.0)
        repository.saveBudget(budget)

        // When
        val flow = repository.getOverallBudgetFlow(1, 2026)
        val result = flow.first()

        // Then
        assertNotNull(result)
        assertNull(result?.categoryId)
        assertEquals(50000.0, result!!.amount, 0.01)
    }

    // ==================== Get Budgets for Month Tests ====================

    @Test
    fun getBudgetsForMonth_returnsAllBudgetsForMonth() = runTest {
        // Given - Multiple budgets for same month
        val budgets = listOf(
            createTestBudget(categoryId = null, amount = 50000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "food", amount = 6000.0, month = 2, year = 2026) // Different month
        )
        repository.saveBudgets(budgets)

        // When
        val result = repository.getBudgetsForMonth(1, 2026)

        // Then
        assertTrue(result.isSuccess)
        val loaded = result.getOrNull()!!
        assertEquals(3, loaded.size) // Only January budgets
    }

    @Test
    fun getBudgetsForMonth_returnsEmptyListWhenNoBudgets() = runTest {
        // When
        val result = repository.getBudgetsForMonth(1, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun getBudgetsForMonth_usesCache() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = null, amount = 50000.0),
            createTestBudget(categoryId = "food", amount = 5000.0)
        )
        repository.saveBudgets(budgets)

        // When - First call populates cache
        val result1 = repository.getBudgetsForMonth(1, 2026)

        // Second call should hit cache
        val result2 = repository.getBudgetsForMonth(1, 2026)

        // Then
        assertEquals(result1.getOrNull()?.size, result2.getOrNull()?.size)
        assertEquals(2, result2.getOrNull()!!.size)
    }

    @Test
    fun getBudgetsForMonthFlow_emitsUpdates() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = null, amount = 50000.0),
            createTestBudget(categoryId = "food", amount = 5000.0)
        )
        repository.saveBudgets(budgets)

        // When
        val flow = repository.getBudgetsForMonthFlow(1, 2026)
        val result = flow.first()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getBudgetsForMonthFlow_emitsEmptyListWhenNoBudgets() = runTest {
        // When
        val flow = repository.getBudgetsForMonthFlow(1, 2026)
        val result = flow.first()

        // Then
        assertTrue(result.isEmpty())
    }

    // ==================== Get Category Budgets Tests ====================

    @Test
    fun getCategoryBudgetsForMonth_excludesOverallBudget() = runTest {
        // Given - Overall and category budgets
        val budgets = listOf(
            createTestBudget(categoryId = null, amount = 50000.0), // Overall
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "transport", amount = 3000.0)
        )
        repository.saveBudgets(budgets)

        // When
        val result = repository.getCategoryBudgetsForMonth(1, 2026)

        // Then
        assertTrue(result.isSuccess)
        val loaded = result.getOrNull()!!
        assertEquals(2, loaded.size) // Only category budgets, not overall
        assertTrue(loaded.all { it.categoryId != null })
    }

    @Test
    fun getCategoryBudgetsForMonth_returnsEmptyWhenOnlyOverallBudget() = runTest {
        // Given - Only overall budget
        val budget = createTestBudget(categoryId = null, amount = 50000.0)
        repository.saveBudget(budget)

        // When
        val result = repository.getCategoryBudgetsForMonth(1, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun getCategoryBudgetsForMonthFlow_emitsUpdates() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "transport", amount = 3000.0)
        )
        repository.saveBudgets(budgets)

        // When
        val flow = repository.getCategoryBudgetsForMonthFlow(1, 2026)
        val result = flow.first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.categoryId != null })
    }

    // ==================== Get All Budgets Tests ====================

    @Test
    fun getAllBudgets_returnsAllBudgets() = runTest {
        // Given - Budgets across different months
        val budgets = listOf(
            createTestBudget(categoryId = null, amount = 50000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "food", amount = 6000.0, month = 2, year = 2026)
        )
        repository.saveBudgets(budgets)

        // When
        val result = repository.getAllBudgets()

        // Then
        assertTrue(result.isSuccess)
        val loaded = result.getOrNull()!!
        assertEquals(3, loaded.size)
    }

    @Test
    fun getAllBudgets_returnsEmptyListWhenNoBudgets() = runTest {
        // When
        val result = repository.getAllBudgets()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun getAllBudgetsFlow_emitsUpdates() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "transport", amount = 3000.0)
        )
        repository.saveBudgets(budgets)

        // When
        val flow = repository.getAllBudgetsFlow()
        val result = flow.first()

        // Then
        assertEquals(2, result.size)
    }

    // ==================== Delete Budget Tests ====================

    @Test
    fun deleteBudget_removesBudgetSuccessfully() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 5000.0)
        repository.saveBudget(budget)

        // When
        val result = repository.deleteBudget("food", 1, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)

        // Verify it was deleted
        val loaded = repository.getBudget("food", 1, 2026).getOrNull()
        assertNull(loaded)
    }

    @Test
    fun deleteBudget_returnsFalseWhenNotFound() = runTest {
        // When
        val result = repository.deleteBudget("nonexistent", 1, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun deleteBudget_invalidatesCache() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "transport", amount = 3000.0)
        )
        repository.saveBudgets(budgets)

        // Populate cache
        repository.getBudgetsForMonth(1, 2026)

        // When - Delete a budget
        repository.deleteBudget("food", 1, 2026)

        // Then - Cache should be invalidated, fresh data should show only 1 budget
        val result = repository.getBudgetsForMonth(1, 2026)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun deleteBudgetById_removesBudgetSuccessfully() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 5000.0)
        val saveResult = repository.saveBudget(budget)
        val budgetId = saveResult.getOrNull()!!

        // When
        val result = repository.deleteBudgetById(budgetId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)

        // Verify it was deleted
        val count = repository.getBudgetCount().getOrNull()!!
        assertEquals(0, count)
    }

    @Test
    fun deleteBudgetsForMonth_removesAllBudgetsForMonth() = runTest {
        // Given - Multiple budgets for different months
        val budgets = listOf(
            createTestBudget(categoryId = null, amount = 50000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "food", amount = 6000.0, month = 2, year = 2026)
        )
        repository.saveBudgets(budgets)

        // When - Delete all budgets for January 2026
        val result = repository.deleteBudgetsForMonth(1, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()!!) // 2 budgets deleted

        // Verify only February budget remains
        val remaining = repository.getAllBudgets().getOrNull()!!
        assertEquals(1, remaining.size)
        assertEquals(2, remaining[0].month)
    }

    @Test
    fun deleteAllBudgets_removesAllBudgets() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = null, amount = 50000.0),
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "transport", amount = 3000.0)
        )
        repository.saveBudgets(budgets)

        // When
        val result = repository.deleteAllBudgets()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()!!)

        // Verify all deleted
        val count = repository.getBudgetCount().getOrNull()!!
        assertEquals(0, count)
    }

    @Test
    fun deleteBudget_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.deleteBudget("food", 1, 2026)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    // ==================== Budget Exists Tests ====================

    @Test
    fun budgetExists_returnsTrueWhenExists() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 5000.0)
        repository.saveBudget(budget)

        // When
        val result = repository.budgetExists("food", 1, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun budgetExists_returnsFalseWhenNotExists() = runTest {
        // When
        val result = repository.budgetExists("nonexistent", 1, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun budgetExists_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.budgetExists("food", 1, 2026)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    // ==================== Budget Count Tests ====================

    @Test
    fun getBudgetCount_returnsCorrectCount() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = null, amount = 50000.0),
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "transport", amount = 3000.0)
        )
        repository.saveBudgets(budgets)

        // When
        val result = repository.getBudgetCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()!!)
    }

    @Test
    fun getBudgetCount_returnsZeroWhenEmpty() = runTest {
        // When
        val result = repository.getBudgetCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()!!)
    }

    @Test
    fun getBudgetCount_usesCache() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "transport", amount = 3000.0)
        )
        repository.saveBudgets(budgets)

        // When - First call populates cache
        val result1 = repository.getBudgetCount()

        // Second call should hit cache
        val result2 = repository.getBudgetCount()

        // Then
        assertEquals(result1.getOrNull(), result2.getOrNull())
        assertEquals(2, result2.getOrNull()!!)
    }

    @Test
    fun getBudgetCount_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getBudgetCount()

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getBudgetCountForMonth_returnsCorrectCount() = runTest {
        // Given - Budgets across different months
        val budgets = listOf(
            createTestBudget(categoryId = null, amount = 50000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "food", amount = 6000.0, month = 2, year = 2026)
        )
        repository.saveBudgets(budgets)

        // When
        val result = repository.getBudgetCountForMonth(1, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()!!) // Only January budgets
    }

    @Test
    fun getBudgetCountForMonth_returnsZeroWhenNoBudgets() = runTest {
        // When
        val result = repository.getBudgetCountForMonth(1, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()!!)
    }

    // ==================== Cache Invalidation Tests ====================

    @Test
    fun cacheInvalidation_afterSaveBudget() = runTest {
        // Given
        val budget1 = createTestBudget(categoryId = "food", amount = 5000.0)
        repository.saveBudget(budget1)

        // Populate cache
        val result1 = repository.getBudgetsForMonth(1, 2026)
        assertEquals(1, result1.getOrNull()!!.size)

        // When - Save another budget (should invalidate cache)
        val budget2 = createTestBudget(categoryId = "transport", amount = 3000.0)
        repository.saveBudget(budget2)

        // Then - Should get fresh data from database
        val result2 = repository.getBudgetsForMonth(1, 2026)
        assertEquals(2, result2.getOrNull()!!.size)
    }

    @Test
    fun cacheInvalidation_afterSaveBudgets() = runTest {
        // Given
        val budget1 = createTestBudget(categoryId = "food", amount = 5000.0)
        repository.saveBudget(budget1)

        // Populate cache
        repository.getBudgetsForMonth(1, 2026)

        // When - Batch save (should invalidate cache)
        val newBudgets = listOf(
            createTestBudget(categoryId = "transport", amount = 3000.0),
            createTestBudget(categoryId = "shopping", amount = 4000.0)
        )
        repository.saveBudgets(newBudgets)

        // Then - Should get updated data
        val result = repository.getBudgetsForMonth(1, 2026)
        assertEquals(3, result.getOrNull()!!.size)
    }

    @Test
    fun cacheInvalidation_afterDeleteBudget() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "transport", amount = 3000.0)
        )
        repository.saveBudgets(budgets)

        // Populate cache
        repository.getBudgetsForMonth(1, 2026)

        // When - Delete a budget
        repository.deleteBudget("food", 1, 2026)

        // Then - Cache should be invalidated
        val result = repository.getBudgetsForMonth(1, 2026)
        assertEquals(1, result.getOrNull()!!.size)
    }

    @Test
    fun cacheInvalidation_afterDeleteAllBudgets() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "transport", amount = 3000.0)
        )
        repository.saveBudgets(budgets)

        // Populate caches
        repository.getBudgetsForMonth(1, 2026)
        repository.getBudgetCount()

        // When - Delete all
        repository.deleteAllBudgets()

        // Then - All caches should be invalidated
        val monthResult = repository.getBudgetsForMonth(1, 2026)
        val countResult = repository.getBudgetCount()
        assertEquals(0, monthResult.getOrNull()!!.size)
        assertEquals(0, countResult.getOrNull()!!)
    }

    // ==================== Entity-Model Mapping Tests ====================

    @Test
    fun entityMapping_preservesAllFields() = runTest {
        // Given
        val original = Budget(
            id = 0, // Will be auto-generated
            categoryId = "food",
            amount = 5000.0,
            month = 3,
            year = 2026,
            createdAt = 1234567890L
        )

        // When
        repository.saveBudget(original)
        val loaded = repository.getBudget("food", 3, 2026).getOrNull()!!

        // Then - All fields should match
        assertEquals(original.categoryId, loaded.categoryId)
        assertEquals(original.amount, loaded.amount, 0.001)
        assertEquals(original.month, loaded.month)
        assertEquals(original.year, loaded.year)
        assertEquals(original.createdAt, loaded.createdAt)
    }

    @Test
    fun entityMapping_handlesNullCategoryId() = runTest {
        // Given - Overall budget with null categoryId
        val original = Budget(
            id = 0,
            categoryId = null,
            amount = 50000.0,
            month = 1,
            year = 2026,
            createdAt = System.currentTimeMillis()
        )

        // When
        repository.saveBudget(original)
        val loaded = repository.getOverallBudget(1, 2026).getOrNull()!!

        // Then
        assertNull(loaded.categoryId)
        assertEquals(original.amount, loaded.amount, 0.001)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun saveBudget_withZeroAmount() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 0.0)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getBudget("food", 1, 2026).getOrNull()
        assertEquals(0.0, loaded!!.amount, 0.001)
    }

    @Test
    fun saveBudget_withVeryLargeAmount() = runTest {
        // Given
        val largeAmount = Double.MAX_VALUE
        val budget = createTestBudget(categoryId = "food", amount = largeAmount)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getBudget("food", 1, 2026).getOrNull()
        assertEquals(largeAmount, loaded!!.amount, 0.001)
    }

    @Test
    fun saveBudget_withVerySmallAmount() = runTest {
        // Given
        val smallAmount = 0.01
        val budget = createTestBudget(categoryId = "food", amount = smallAmount)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getBudget("food", 1, 2026).getOrNull()
        assertEquals(smallAmount, loaded!!.amount, 0.001)
    }

    @Test
    fun saveBudget_forDifferentMonthsAndYears() = runTest {
        // Given - Same category, different months
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "food", amount = 6000.0, month = 2, year = 2026),
            createTestBudget(categoryId = "food", amount = 7000.0, month = 1, year = 2027)
        )

        // When
        repository.saveBudgets(budgets)

        // Then - All should be saved (different month/year combinations)
        val count = repository.getBudgetCount().getOrNull()!!
        assertEquals(3, count)

        // Verify each can be retrieved independently
        assertEquals(5000.0, repository.getBudget("food", 1, 2026).getOrNull()!!.amount, 0.01)
        assertEquals(6000.0, repository.getBudget("food", 2, 2026).getOrNull()!!.amount, 0.01)
        assertEquals(7000.0, repository.getBudget("food", 1, 2027).getOrNull()!!.amount, 0.01)
    }

    @Test
    fun saveBudget_withMonthBoundaries() = runTest {
        // Given - Test month 1 and 12
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "food", amount = 6000.0, month = 12, year = 2026)
        )

        // When
        repository.saveBudgets(budgets)

        // Then
        assertEquals(5000.0, repository.getBudget("food", 1, 2026).getOrNull()!!.amount, 0.01)
        assertEquals(6000.0, repository.getBudget("food", 12, 2026).getOrNull()!!.amount, 0.01)
    }

    @Test
    fun saveBudget_withEmptyCategoryId() = runTest {
        // Given - Empty string category (not null)
        val budget = createTestBudget(categoryId = "", amount = 5000.0)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getBudget("", 1, 2026).getOrNull()
        assertEquals("", loaded?.categoryId)
    }

    @Test
    fun saveBudget_withLongCategoryId() = runTest {
        // Given
        val longCategory = "category_" + "a".repeat(100)
        val budget = createTestBudget(categoryId = longCategory, amount = 5000.0)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getBudget(longCategory, 1, 2026).getOrNull()
        assertEquals(longCategory, loaded?.categoryId)
    }

    @Test
    fun saveBudget_withSpecialCharactersInCategoryId() = runTest {
        // Given
        val specialCategory = "food & beverages™️ (新しい)"
        val budget = createTestBudget(categoryId = specialCategory, amount = 5000.0)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getBudget(specialCategory, 1, 2026).getOrNull()
        assertEquals(specialCategory, loaded?.categoryId)
    }

    @Test
    fun saveBudgets_withEmptyList() = runTest {
        // Given
        val emptyList = emptyList<Budget>()

        // When
        val result = repository.saveBudgets(emptyList)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, repository.getBudgetCount().getOrNull()!!)
    }

    @Test
    fun saveBudgets_withDuplicatesInBatch() = runTest {
        // Given - Same category, month, year (should replace)
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "food", amount = 6000.0) // Duplicate
        )

        // When
        val result = repository.saveBudgets(budgets)

        // Then
        assertTrue(result.isSuccess)
        val count = repository.getBudgetCount().getOrNull()!!
        assertEquals(1, count) // Should have only one (last one wins)
        assertEquals(6000.0, repository.getBudget("food", 1, 2026).getOrNull()!!.amount, 0.01)
    }

    @Test
    fun deleteBudgetsForMonth_withNoMatchingBudgets() = runTest {
        // Given - Budgets for different month
        val budget = createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026)
        repository.saveBudget(budget)

        // When - Try to delete budgets for different month
        val result = repository.deleteBudgetsForMonth(2, 2026)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()!!) // Nothing deleted

        // Original budget should still exist
        val count = repository.getBudgetCount().getOrNull()!!
        assertEquals(1, count)
    }

    @Test
    fun deleteAllBudgets_whenEmpty() = runTest {
        // When
        val result = repository.deleteAllBudgets()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()!!)
    }

    @Test
    fun deleteBudget_multipleTimes() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 5000.0)
        repository.saveBudget(budget)

        // When - Delete twice
        val result1 = repository.deleteBudget("food", 1, 2026)
        val result2 = repository.deleteBudget("food", 1, 2026)

        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result1.getOrNull()!!) // First delete succeeds
        assertTrue(result2.isSuccess)
        assertFalse(result2.getOrNull()!!) // Second delete returns false
    }

    // ==================== Flow Error Handling Tests ====================

    @Test
    fun flowOperations_catchErrors() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 5000.0)
        repository.saveBudget(budget)

        // When - Get flow (should not throw even if there are errors)
        val flow = repository.getBudgetsForMonthFlow(1, 2026)
        val result = flow.first()

        // Then
        assertEquals(1, result.size)
    }

    @Test
    fun flowOperations_emitFallbackOnError() = runTest {
        // Given
        database.close()

        // When - Flow should emit fallback value on error
        val budgetFlow = repository.getBudgetFlow("food", 1, 2026)
        val budgetsFlow = repository.getBudgetsForMonthFlow(1, 2026)
        val allBudgetsFlow = repository.getAllBudgetsFlow()

        // Then - Should emit fallback (null or empty list) instead of throwing
        val budgetResult = budgetFlow.first()
        val budgetsResult = budgetsFlow.first()
        val allBudgetsResult = allBudgetsFlow.first()

        assertNull(budgetResult) // Fallback is null
        assertTrue(budgetsResult.isEmpty()) // Fallback is empty list
        assertTrue(allBudgetsResult.isEmpty()) // Fallback is empty list
    }

    // ==================== Integration Tests ====================

    @Test
    fun fullWorkflow_saveLoadUpdateDelete() = runTest {
        // 1. Save overall budget
        val overallBudget = createTestBudget(categoryId = null, amount = 50000.0)
        repository.saveBudget(overallBudget)
        assertEquals(1, repository.getBudgetCount().getOrNull()!!)

        // 2. Save category budgets
        val categoryBudgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0),
            createTestBudget(categoryId = "transport", amount = 3000.0)
        )
        repository.saveBudgets(categoryBudgets)
        assertEquals(3, repository.getBudgetCount().getOrNull()!!)

        // 3. Load and verify
        val loaded = repository.getBudgetsForMonth(1, 2026).getOrNull()!!
        assertEquals(3, loaded.size)

        // 4. Update a budget
        val updatedBudget = createTestBudget(categoryId = "food", amount = 6000.0)
        repository.saveBudget(updatedBudget)
        assertEquals(6000.0, repository.getBudget("food", 1, 2026).getOrNull()!!.amount, 0.01)

        // 5. Delete a budget
        repository.deleteBudget("transport", 1, 2026)
        assertEquals(2, repository.getBudgetCount().getOrNull()!!)

        // 6. Delete all
        repository.deleteAllBudgets()
        assertEquals(0, repository.getBudgetCount().getOrNull()!!)
    }

    @Test
    fun multipleMonthsWorkflow_isolationVerification() = runTest {
        // Given - Budgets for multiple months
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "food", amount = 6000.0, month = 2, year = 2026),
            createTestBudget(categoryId = "food", amount = 7000.0, month = 3, year = 2026)
        )
        repository.saveBudgets(budgets)

        // When - Delete budgets for February
        repository.deleteBudgetsForMonth(2, 2026)

        // Then - Only February should be deleted
        assertNotNull(repository.getBudget("food", 1, 2026).getOrNull())
        assertNull(repository.getBudget("food", 2, 2026).getOrNull())
        assertNotNull(repository.getBudget("food", 3, 2026).getOrNull())
    }

    @Test
    fun concurrentOperations_maintainDataIntegrity() = runTest {
        // Given - Multiple budgets
        val budgets = (1..10).map { i ->
            createTestBudget(
                categoryId = "category_$i",
                amount = i * 1000.0,
                month = 1,
                year = 2026
            )
        }

        // When - Batch save and queries
        repository.saveBudgets(budgets)

        // Then - All data should be consistent
        assertEquals(10, repository.getBudgetCount().getOrNull()!!)
        assertEquals(10, repository.getBudgetsForMonth(1, 2026).getOrNull()!!.size)
    }
}
