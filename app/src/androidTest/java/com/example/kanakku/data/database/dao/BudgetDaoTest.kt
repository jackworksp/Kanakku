package com.example.kanakku.data.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.entity.BudgetEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for BudgetDao running on a real Android device/emulator.
 *
 * Tests cover:
 * - Insert and retrieve budget operations
 * - Update existing budgets (upsert functionality)
 * - Delete budget operations
 * - Query by month/year filtering
 * - Overall vs category budget distinction
 * - Flow-based reactive queries
 * - Unique constraint enforcement
 */
@RunWith(AndroidJUnit4::class)
class BudgetDaoTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var budgetDao: BudgetDao

    @Before
    fun setup() {
        // Create in-memory database for testing on real device
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        budgetDao = database.budgetDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ==================== Helper Functions ====================

    private fun createTestBudget(
        categoryId: String? = null,
        amount: Double = 10000.0,
        month: Int = 1,
        year: Int = 2026,
        createdAt: Long = System.currentTimeMillis()
    ): BudgetEntity {
        return BudgetEntity(
            categoryId = categoryId,
            amount = amount,
            month = month,
            year = year,
            createdAt = createdAt
        )
    }

    // ==================== Insert and Retrieve Tests ====================

    @Test
    fun budgetDao_insertAndRetrieve() = runBlocking {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026)

        // When
        val id = budgetDao.insertOrUpdate(budget)

        // Then
        assertTrue(id > 0)
        val retrieved = budgetDao.getBudget("food", 1, 2026)
        assertNotNull(retrieved)
        assertEquals("food", retrieved?.categoryId)
        assertEquals(5000.0, retrieved?.amount, 0.01)
        assertEquals(1, retrieved?.month)
        assertEquals(2026, retrieved?.year)
    }

    @Test
    fun budgetDao_insertOverallBudget() = runBlocking {
        // Given - Overall budget has null categoryId
        val budget = createTestBudget(categoryId = null, amount = 20000.0, month = 1, year = 2026)

        // When
        budgetDao.insertOrUpdate(budget)

        // Then
        val retrieved = budgetDao.getOverallBudget(1, 2026)
        assertNotNull(retrieved)
        assertNull(retrieved?.categoryId)
        assertEquals(20000.0, retrieved?.amount, 0.01)
    }

    @Test
    fun budgetDao_insertMultipleBudgets() = runBlocking {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026),
            createTestBudget(categoryId = "shopping", amount = 4000.0, month = 1, year = 2026)
        )

        // When
        budgetDao.insertOrUpdateAll(budgets)

        // Then
        val count = budgetDao.getBudgetCount()
        assertEquals(3, count)
    }

    // ==================== Upsert (Update) Tests ====================

    @Test
    fun budgetDao_updateExistingBudget() = runBlocking {
        // Given - Insert initial budget
        val original = createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026)
        budgetDao.insertOrUpdate(original)

        // When - Insert with same categoryId, month, year but different amount
        val updated = createTestBudget(categoryId = "food", amount = 7000.0, month = 1, year = 2026)
        budgetDao.insertOrUpdate(updated)

        // Then - Should have replaced, not duplicated
        val count = budgetDao.getBudgetCount()
        val retrieved = budgetDao.getBudget("food", 1, 2026)
        assertEquals(1, count)
        assertEquals(7000.0, retrieved?.amount, 0.01)
    }

    @Test
    fun budgetDao_updateOverallBudget() = runBlocking {
        // Given - Insert overall budget
        budgetDao.insertOrUpdate(createTestBudget(categoryId = null, amount = 20000.0, month = 1, year = 2026))

        // When - Update overall budget for same month/year
        budgetDao.insertOrUpdate(createTestBudget(categoryId = null, amount = 25000.0, month = 1, year = 2026))

        // Then - Should replace the existing overall budget
        val count = budgetDao.getBudgetCountForMonth(1, 2026)
        val retrieved = budgetDao.getOverallBudget(1, 2026)
        assertEquals(1, count)
        assertEquals(25000.0, retrieved?.amount, 0.01)
    }

    @Test
    fun budgetDao_uniqueConstraintEnforcement() = runBlocking {
        // Given - Insert budget for food category
        val budget1 = createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026)
        budgetDao.insertOrUpdate(budget1)

        // When - Insert another budget with same categoryId, month, year
        val budget2 = createTestBudget(categoryId = "food", amount = 6000.0, month = 1, year = 2026)
        budgetDao.insertOrUpdate(budget2)

        // Then - Should have only one budget (replaced due to unique constraint)
        val allBudgets = budgetDao.getBudgetsForMonth(1, 2026)
        assertEquals(1, allBudgets.size)
        assertEquals(6000.0, allBudgets[0].amount, 0.01)
    }

    // ==================== Delete Tests ====================

    @Test
    fun budgetDao_deleteBudget() = runBlocking {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026)
        budgetDao.insertOrUpdate(budget)

        // When
        val deletedCount = budgetDao.deleteBudget("food", 1, 2026)

        // Then
        assertEquals(1, deletedCount)
        assertNull(budgetDao.getBudget("food", 1, 2026))
        assertEquals(0, budgetDao.getBudgetCount())
    }

    @Test
    fun budgetDao_deleteOverallBudget() = runBlocking {
        // Given - Insert overall budget (null categoryId)
        budgetDao.insertOrUpdate(createTestBudget(categoryId = null, amount = 20000.0, month = 1, year = 2026))

        // When
        val deletedCount = budgetDao.deleteBudget(null, 1, 2026)

        // Then
        assertEquals(1, deletedCount)
        assertNull(budgetDao.getOverallBudget(1, 2026))
    }

    @Test
    fun budgetDao_deleteBudgetById() = runBlocking {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026)
        val id = budgetDao.insertOrUpdate(budget)

        // When
        val deletedCount = budgetDao.deleteBudgetById(id)

        // Then
        assertEquals(1, deletedCount)
        assertNull(budgetDao.getBudget("food", 1, 2026))
    }

    @Test
    fun budgetDao_deleteNonExistentBudget() = runBlocking {
        // When - Try to delete budget that doesn't exist
        val deletedCount = budgetDao.deleteBudget("food", 1, 2026)

        // Then - Should return 0
        assertEquals(0, deletedCount)
    }

    @Test
    fun budgetDao_deleteBudgetsForMonth() = runBlocking {
        // Given - Insert multiple budgets for same month
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = null, amount = 20000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026)
            )
        )

        // When - Delete all budgets for January 2026
        val deletedCount = budgetDao.deleteBudgetsForMonth(1, 2026)

        // Then
        assertEquals(3, deletedCount)
        assertEquals(0, budgetDao.getBudgetCountForMonth(1, 2026))
    }

    @Test
    fun budgetDao_deleteAll() = runBlocking {
        // Given - Insert budgets for multiple months
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 5000.0, month = 2, year = 2026),
                createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026)
            )
        )

        // When
        val deletedCount = budgetDao.deleteAll()

        // Then
        assertEquals(3, deletedCount)
        assertEquals(0, budgetDao.getBudgetCount())
    }

    // ==================== Query by Month/Year Tests ====================

    @Test
    fun budgetDao_getBudgetsForMonth() = runBlocking {
        // Given - Insert budgets for different months
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 6000.0, month = 2, year = 2026)
            )
        )

        // When
        val jan2026Budgets = budgetDao.getBudgetsForMonth(1, 2026)
        val feb2026Budgets = budgetDao.getBudgetsForMonth(2, 2026)

        // Then
        assertEquals(2, jan2026Budgets.size)
        assertEquals(1, feb2026Budgets.size)
    }

    @Test
    fun budgetDao_getCategoryBudgetsForMonth() = runBlocking {
        // Given - Insert overall and category budgets
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = null, amount = 20000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026)
            )
        )

        // When
        val categoryBudgets = budgetDao.getCategoryBudgetsForMonth(1, 2026)

        // Then - Should only include category budgets (not overall)
        assertEquals(2, categoryBudgets.size)
        assertTrue(categoryBudgets.none { it.categoryId == null })
    }

    @Test
    fun budgetDao_getOverallBudgetOnly() = runBlocking {
        // Given - Insert overall and category budgets
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = null, amount = 20000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026)
            )
        )

        // When
        val overallBudget = budgetDao.getOverallBudget(1, 2026)

        // Then
        assertNotNull(overallBudget)
        assertNull(overallBudget?.categoryId)
        assertEquals(20000.0, overallBudget?.amount, 0.01)
    }

    @Test
    fun budgetDao_getOverallBudgetWhenNotSet() = runBlocking {
        // Given - Only category budgets, no overall budget
        budgetDao.insertOrUpdate(createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026))

        // When
        val overallBudget = budgetDao.getOverallBudget(1, 2026)

        // Then
        assertNull(overallBudget)
    }

    @Test
    fun budgetDao_getAllBudgets() = runBlocking {
        // Given - Insert budgets across multiple months and years
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 6000.0, month = 2, year = 2026),
                createTestBudget(categoryId = "food", amount = 5500.0, month = 1, year = 2025)
            )
        )

        // When
        val allBudgets = budgetDao.getAllBudgets()

        // Then
        assertEquals(3, allBudgets.size)
        // Should be ordered by year DESC, month DESC, categoryId
        assertEquals(2026, allBudgets[0].year)
    }

    // ==================== Exists and Count Tests ====================

    @Test
    fun budgetDao_exists() = runBlocking {
        // Given
        budgetDao.insertOrUpdate(createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026))

        // When/Then
        assertTrue(budgetDao.exists("food", 1, 2026))
        assertFalse(budgetDao.exists("transport", 1, 2026))
        assertFalse(budgetDao.exists("food", 2, 2026))
    }

    @Test
    fun budgetDao_existsOverallBudget() = runBlocking {
        // Given - Insert overall budget
        budgetDao.insertOrUpdate(createTestBudget(categoryId = null, amount = 20000.0, month = 1, year = 2026))

        // When/Then
        assertTrue(budgetDao.exists(null, 1, 2026))
        assertFalse(budgetDao.exists(null, 2, 2026))
    }

    @Test
    fun budgetDao_getBudgetCount() = runBlocking {
        // Given
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026)
            )
        )

        // When
        val count = budgetDao.getBudgetCount()

        // Then
        assertEquals(2, count)
    }

    @Test
    fun budgetDao_getBudgetCountForMonth() = runBlocking {
        // Given - Insert budgets for different months
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 6000.0, month = 2, year = 2026)
            )
        )

        // When
        val jan2026Count = budgetDao.getBudgetCountForMonth(1, 2026)
        val feb2026Count = budgetDao.getBudgetCountForMonth(2, 2026)
        val mar2026Count = budgetDao.getBudgetCountForMonth(3, 2026)

        // Then
        assertEquals(2, jan2026Count)
        assertEquals(1, feb2026Count)
        assertEquals(0, mar2026Count)
    }

    // ==================== Flow-based Reactive Query Tests ====================

    @Test
    fun budgetDao_getBudgetFlow() = runBlocking {
        // Given - Initial state
        val budget = createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026)

        // When - Insert budget
        budgetDao.insertOrUpdate(budget)

        // Then - Flow should emit the budget
        val retrieved = budgetDao.getBudgetFlow("food", 1, 2026).first()
        assertNotNull(retrieved)
        assertEquals(5000.0, retrieved?.amount, 0.01)
    }

    @Test
    fun budgetDao_getBudgetFlowUpdates() = runBlocking {
        // Given - Insert initial budget
        budgetDao.insertOrUpdate(createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026))
        val flow = budgetDao.getBudgetFlow("food", 1, 2026)

        // When - Get initial value
        val initial = flow.first()
        assertEquals(5000.0, initial?.amount, 0.01)

        // When - Update budget
        budgetDao.insertOrUpdate(createTestBudget(categoryId = "food", amount = 7000.0, month = 1, year = 2026))

        // Then - Flow should emit updated value
        val updated = flow.first()
        assertEquals(7000.0, updated?.amount, 0.01)
    }

    @Test
    fun budgetDao_getOverallBudgetFlow() = runBlocking {
        // Given - Insert overall budget
        budgetDao.insertOrUpdate(createTestBudget(categoryId = null, amount = 20000.0, month = 1, year = 2026))

        // When
        val flow = budgetDao.getOverallBudgetFlow(1, 2026)
        val retrieved = flow.first()

        // Then
        assertNotNull(retrieved)
        assertNull(retrieved?.categoryId)
        assertEquals(20000.0, retrieved?.amount, 0.01)
    }

    @Test
    fun budgetDao_getBudgetsForMonthFlow() = runBlocking {
        // Given - Insert budgets
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026)
            )
        )

        // When
        val flow = budgetDao.getBudgetsForMonthFlow(1, 2026)
        val budgets = flow.first()

        // Then
        assertEquals(2, budgets.size)
    }

    @Test
    fun budgetDao_getCategoryBudgetsForMonthFlow() = runBlocking {
        // Given - Insert overall and category budgets
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = null, amount = 20000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026)
            )
        )

        // When
        val flow = budgetDao.getCategoryBudgetsForMonthFlow(1, 2026)
        val categoryBudgets = flow.first()

        // Then - Should only include category budgets
        assertEquals(2, categoryBudgets.size)
        assertTrue(categoryBudgets.none { it.categoryId == null })
    }

    @Test
    fun budgetDao_getAllBudgetsFlow() = runBlocking {
        // Given - Insert budgets
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 6000.0, month = 2, year = 2026)
            )
        )

        // When
        val flow = budgetDao.getAllBudgetsFlow()
        val allBudgets = flow.first()

        // Then
        assertEquals(2, allBudgets.size)
    }

    // ==================== Integration Tests ====================

    @Test
    fun integration_fullBudgetLifecycle() = runBlocking {
        // 1. Insert overall budget
        val overallBudget = createTestBudget(categoryId = null, amount = 20000.0, month = 1, year = 2026)
        budgetDao.insertOrUpdate(overallBudget)
        assertEquals(1, budgetDao.getBudgetCount())

        // 2. Insert category budgets
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026)
            )
        )
        assertEquals(3, budgetDao.getBudgetCountForMonth(1, 2026))

        // 3. Update a budget
        budgetDao.insertOrUpdate(createTestBudget(categoryId = "food", amount = 7000.0, month = 1, year = 2026))
        val updated = budgetDao.getBudget("food", 1, 2026)
        assertEquals(7000.0, updated?.amount, 0.01)

        // 4. Delete a category budget
        budgetDao.deleteBudget("transport", 1, 2026)
        assertEquals(2, budgetDao.getBudgetCountForMonth(1, 2026))

        // 5. Verify overall budget still exists
        val overall = budgetDao.getOverallBudget(1, 2026)
        assertNotNull(overall)
        assertEquals(20000.0, overall?.amount, 0.01)

        // 6. Delete all budgets for month
        budgetDao.deleteBudgetsForMonth(1, 2026)
        assertEquals(0, budgetDao.getBudgetCount())
    }

    @Test
    fun integration_multipleMonthsBudgets() = runBlocking {
        // Given - Insert budgets across multiple months
        budgetDao.insertOrUpdateAll(
            listOf(
                // January 2026
                createTestBudget(categoryId = null, amount = 20000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                // February 2026
                createTestBudget(categoryId = null, amount = 22000.0, month = 2, year = 2026),
                createTestBudget(categoryId = "food", amount = 6000.0, month = 2, year = 2026),
                // January 2027
                createTestBudget(categoryId = "food", amount = 5500.0, month = 1, year = 2027)
            )
        )

        // When/Then - Verify correct filtering
        assertEquals(2, budgetDao.getBudgetCountForMonth(1, 2026))
        assertEquals(2, budgetDao.getBudgetCountForMonth(2, 2026))
        assertEquals(1, budgetDao.getBudgetCountForMonth(1, 2027))
        assertEquals(0, budgetDao.getBudgetCountForMonth(3, 2026))

        // Verify overall budgets are retrieved correctly
        assertNotNull(budgetDao.getOverallBudget(1, 2026))
        assertNotNull(budgetDao.getOverallBudget(2, 2026))
        assertNull(budgetDao.getOverallBudget(1, 2027))

        // Verify total count
        assertEquals(5, budgetDao.getBudgetCount())
    }

    @Test
    fun integration_budgetOrdering() = runBlocking {
        // Given - Insert budgets in random order
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = "transport", amount = 3000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "shopping", amount = 4000.0, month = 1, year = 2026)
            )
        )

        // When
        val budgets = budgetDao.getBudgetsForMonth(1, 2026)

        // Then - Should be ordered by categoryId
        assertEquals("food", budgets[0].categoryId)
        assertEquals("shopping", budgets[1].categoryId)
        assertEquals("transport", budgets[2].categoryId)
    }

    @Test
    fun integration_edgeCasesAmounts() = runBlocking {
        // Given - Insert budgets with edge case amounts
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = "zero", amount = 0.0, month = 1, year = 2026),
                createTestBudget(categoryId = "large", amount = 999999999.99, month = 1, year = 2026),
                createTestBudget(categoryId = "decimal", amount = 12345.67, month = 1, year = 2026)
            )
        )

        // When/Then - All budgets should be stored and retrieved correctly
        val zeroBudget = budgetDao.getBudget("zero", 1, 2026)
        assertEquals(0.0, zeroBudget?.amount, 0.01)

        val largeBudget = budgetDao.getBudget("large", 1, 2026)
        assertEquals(999999999.99, largeBudget?.amount, 0.01)

        val decimalBudget = budgetDao.getBudget("decimal", 1, 2026)
        assertEquals(12345.67, decimalBudget?.amount, 0.01)
    }

    @Test
    fun integration_monthBoundaries() = runBlocking {
        // Given - Insert budgets for month boundaries
        budgetDao.insertOrUpdateAll(
            listOf(
                createTestBudget(categoryId = "food", amount = 5000.0, month = 1, year = 2026),
                createTestBudget(categoryId = "food", amount = 6000.0, month = 12, year = 2026),
                createTestBudget(categoryId = "food", amount = 5500.0, month = 1, year = 2027)
            )
        )

        // When/Then - Verify correct month/year filtering
        assertEquals(5000.0, budgetDao.getBudget("food", 1, 2026)?.amount, 0.01)
        assertEquals(6000.0, budgetDao.getBudget("food", 12, 2026)?.amount, 0.01)
        assertEquals(5500.0, budgetDao.getBudget("food", 1, 2027)?.amount, 0.01)
    }
}
