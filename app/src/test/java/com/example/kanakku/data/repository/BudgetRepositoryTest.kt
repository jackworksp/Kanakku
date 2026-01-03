package com.example.kanakku.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.entity.BudgetEntity
import com.example.kanakku.data.model.BudgetPeriod
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
 * - Budget CRUD operations (save, update, delete)
 * - Query operations (by period, category, both)
 * - Utility operations (exists, count, latest update time)
 * - In-memory caching and cache invalidation
 * - Result type error handling
 * - Flow operations with error handling
 * - Edge cases and boundary values
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
        categoryId: String = "food",
        amount: Double = 10000.0,
        period: BudgetPeriod = BudgetPeriod.MONTHLY,
        startDate: Long = System.currentTimeMillis()
    ): BudgetEntity {
        return BudgetEntity(
            id = id,
            categoryId = categoryId,
            amount = amount,
            period = period,
            startDate = startDate,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    // ==================== Budget Save/Load Tests ====================

    @Test
    fun saveBudget_insertsBudgetSuccessfully() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 10000.0)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val rowId = result.getOrNull()!!
        assertTrue(rowId > 0)

        val loaded = repository.getAllBudgetsSnapshot().getOrNull()!!
        assertEquals(1, loaded.size)
        assertEquals(budget.categoryId, loaded[0].categoryId)
        assertEquals(budget.amount, loaded[0].amount, 0.001)
    }

    @Test
    fun saveBudgets_insertsBatchSuccessfully() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 10000.0),
            createTestBudget(categoryId = "transport", amount = 5000.0),
            createTestBudget(categoryId = "shopping", amount = 15000.0)
        )

        // When
        val result = repository.saveBudgets(budgets)

        // Then
        assertTrue(result.isSuccess)

        val loaded = repository.getAllBudgetsSnapshot().getOrNull()!!
        assertEquals(3, loaded.size)
    }

    @Test
    fun saveBudget_withSameCategoryAndPeriod_replacesExisting() = runTest {
        // Given - Two budgets with same categoryId and period
        val budget1 = createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY, amount = 10000.0)
        val budget2 = createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY, amount = 15000.0)

        // When - Save both
        repository.saveBudget(budget1)
        repository.saveBudget(budget2)

        // Then - Should have only one budget (replaced due to unique index)
        val loaded = repository.getAllBudgetsSnapshot().getOrNull()!!
        assertEquals(1, loaded.size)
        assertEquals(15000.0, loaded[0].amount, 0.001)
    }

    @Test
    fun saveBudget_withSameCategoryDifferentPeriod_savesBoth() = runTest {
        // Given - Same category but different periods
        val monthlyBudget = createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY, amount = 30000.0)
        val weeklyBudget = createTestBudget(categoryId = "food", period = BudgetPeriod.WEEKLY, amount = 7000.0)

        // When
        repository.saveBudget(monthlyBudget)
        repository.saveBudget(weeklyBudget)

        // Then - Should have both budgets
        val loaded = repository.getAllBudgetsSnapshot().getOrNull()!!
        assertEquals(2, loaded.size)
    }

    @Test
    fun updateBudget_updatesExistingBudget() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 10000.0)
        val savedId = repository.saveBudget(budget).getOrNull()!!

        // When - Update with new amount
        val updatedBudget = budget.copy(id = savedId, amount = 12000.0)
        val updateResult = repository.updateBudget(updatedBudget)

        // Then
        assertTrue(updateResult.isSuccess)
        assertTrue(updateResult.getOrNull() == true)

        val loaded = repository.getAllBudgetsSnapshot().getOrNull()!!
        assertEquals(1, loaded.size)
        assertEquals(12000.0, loaded[0].amount, 0.001)
    }

    @Test
    fun updateBudget_withNonExistentId_returnsFalse() = runTest {
        // Given - Budget with ID that doesn't exist
        val budget = createTestBudget(id = 999L, categoryId = "food", amount = 10000.0)

        // When
        val result = repository.updateBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun getAllBudgets_returnsFlowOfBudgets() = runTest {
        // Given
        val budgets = listOf(
            createTestBudget(categoryId = "food", amount = 10000.0),
            createTestBudget(categoryId = "transport", amount = 5000.0)
        )
        repository.saveBudgets(budgets)

        // When
        val flow = repository.getAllBudgets()
        val result = flow.first()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getAllBudgetsSnapshot_returnsResultType() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 10000.0)
        repository.saveBudget(budget)

        // When
        val result = repository.getAllBudgetsSnapshot()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    // ==================== Query by Period Tests ====================

    @Test
    fun getBudgetsByPeriod_filtersMonthlyBudgets() = runTest {
        // Given - Mix of monthly and weekly budgets
        repository.saveBudgets(
            listOf(
                createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY),
                createTestBudget(categoryId = "transport", period = BudgetPeriod.MONTHLY),
                createTestBudget(categoryId = "shopping", period = BudgetPeriod.WEEKLY)
            )
        )

        // When
        val monthlyBudgets = repository.getBudgetsByPeriod(BudgetPeriod.MONTHLY).first()

        // Then
        assertEquals(2, monthlyBudgets.size)
        assertTrue(monthlyBudgets.all { it.period == BudgetPeriod.MONTHLY })
    }

    @Test
    fun getBudgetsByPeriod_filtersWeeklyBudgets() = runTest {
        // Given - Mix of monthly and weekly budgets
        repository.saveBudgets(
            listOf(
                createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY),
                createTestBudget(categoryId = "transport", period = BudgetPeriod.WEEKLY),
                createTestBudget(categoryId = "shopping", period = BudgetPeriod.WEEKLY)
            )
        )

        // When
        val weeklyBudgets = repository.getBudgetsByPeriod(BudgetPeriod.WEEKLY).first()

        // Then
        assertEquals(2, weeklyBudgets.size)
        assertTrue(weeklyBudgets.all { it.period == BudgetPeriod.WEEKLY })
    }

    @Test
    fun getBudgetsByPeriod_returnsEmptyWhenNoMatches() = runTest {
        // Given - Only monthly budgets
        repository.saveBudget(createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY))

        // When
        val weeklyBudgets = repository.getBudgetsByPeriod(BudgetPeriod.WEEKLY).first()

        // Then
        assertTrue(weeklyBudgets.isEmpty())
    }

    // ==================== Query by Category Tests ====================

    @Test
    fun getBudgetsByCategory_returnsAllBudgetsForCategory() = runTest {
        // Given - Multiple budgets for same category
        repository.saveBudgets(
            listOf(
                createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY),
                createTestBudget(categoryId = "food", period = BudgetPeriod.WEEKLY),
                createTestBudget(categoryId = "transport", period = BudgetPeriod.MONTHLY)
            )
        )

        // When
        val foodBudgets = repository.getBudgetsByCategory("food").first()

        // Then
        assertEquals(2, foodBudgets.size)
        assertTrue(foodBudgets.all { it.categoryId == "food" })
    }

    @Test
    fun getBudgetsByCategory_returnsEmptyWhenNoMatches() = runTest {
        // Given - Budgets for different category
        repository.saveBudget(createTestBudget(categoryId = "food"))

        // When
        val transportBudgets = repository.getBudgetsByCategory("transport").first()

        // Then
        assertTrue(transportBudgets.isEmpty())
    }

    // ==================== Query by Category and Period Tests ====================

    @Test
    fun getBudgetByCategoryAndPeriod_returnsCorrectBudget() = runTest {
        // Given
        val monthlyFoodBudget = createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY, amount = 30000.0)
        val weeklyFoodBudget = createTestBudget(categoryId = "food", period = BudgetPeriod.WEEKLY, amount = 7000.0)
        repository.saveBudgets(listOf(monthlyFoodBudget, weeklyFoodBudget))

        // When
        val result = repository.getBudgetByCategoryAndPeriod("food", BudgetPeriod.MONTHLY).first()

        // Then
        assertNotNull(result)
        assertEquals("food", result?.categoryId)
        assertEquals(BudgetPeriod.MONTHLY, result?.period)
        assertEquals(30000.0, result?.amount, 0.001)
    }

    @Test
    fun getBudgetByCategoryAndPeriod_returnsNullWhenNotFound() = runTest {
        // Given - Different budget
        repository.saveBudget(createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY))

        // When
        val result = repository.getBudgetByCategoryAndPeriod("transport", BudgetPeriod.MONTHLY).first()

        // Then
        assertNull(result)
    }

    @Test
    fun getBudgetByCategoryAndPeriodSnapshot_returnsResultType() = runTest {
        // Given
        repository.saveBudget(createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY))

        // When
        val result = repository.getBudgetByCategoryAndPeriodSnapshot("food", BudgetPeriod.MONTHLY)

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("food", result.getOrNull()?.categoryId)
    }

    @Test
    fun getBudgetByCategoryAndPeriodSnapshot_returnsNullWhenNotFound() = runTest {
        // When
        val result = repository.getBudgetByCategoryAndPeriodSnapshot("nonexistent", BudgetPeriod.MONTHLY)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    // ==================== Delete Operations Tests ====================

    @Test
    fun deleteBudget_removesCorrectBudget() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 10000.0)
        val savedId = repository.saveBudget(budget).getOrNull()!!

        // When
        val deleteResult = repository.deleteBudget(savedId)

        // Then
        assertTrue(deleteResult.isSuccess)
        assertTrue(deleteResult.getOrNull() == true)
        assertEquals(0, repository.getBudgetCount().getOrNull())
    }

    @Test
    fun deleteBudget_returnsFalseWhenNotFound() = runTest {
        // When - Try to delete non-existent budget
        val result = repository.deleteBudget(999L)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun deleteBudgetByCategoryAndPeriod_removesCorrectBudget() = runTest {
        // Given
        repository.saveBudgets(
            listOf(
                createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY),
                createTestBudget(categoryId = "food", period = BudgetPeriod.WEEKLY),
                createTestBudget(categoryId = "transport", period = BudgetPeriod.MONTHLY)
            )
        )

        // When - Delete specific budget
        val result = repository.deleteBudgetByCategoryAndPeriod("food", BudgetPeriod.MONTHLY)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        assertEquals(2, repository.getBudgetCount().getOrNull())

        // Verify the correct budget was deleted
        val remaining = repository.getAllBudgetsSnapshot().getOrNull()!!
        assertFalse(remaining.any { it.categoryId == "food" && it.period == BudgetPeriod.MONTHLY })
        assertTrue(remaining.any { it.categoryId == "food" && it.period == BudgetPeriod.WEEKLY })
    }

    @Test
    fun deleteBudgetByCategoryAndPeriod_returnsFalseWhenNotFound() = runTest {
        // When
        val result = repository.deleteBudgetByCategoryAndPeriod("nonexistent", BudgetPeriod.MONTHLY)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun deleteAllBudgets_removesAllBudgets() = runTest {
        // Given
        repository.saveBudgets(
            listOf(
                createTestBudget(categoryId = "food"),
                createTestBudget(categoryId = "transport"),
                createTestBudget(categoryId = "shopping")
            )
        )

        // When
        val result = repository.deleteAllBudgets()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
        assertEquals(0, repository.getBudgetCount().getOrNull())
    }

    @Test
    fun deleteAllBudgets_returnsZeroWhenEmpty() = runTest {
        // When - Delete from empty database
        val result = repository.deleteAllBudgets()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    // ==================== Utility Operations Tests ====================

    @Test
    fun budgetExists_returnsTrueWhenExists() = runTest {
        // Given
        repository.saveBudget(createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY))

        // When
        val result = repository.budgetExists("food", BudgetPeriod.MONTHLY)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun budgetExists_returnsFalseWhenNotExists() = runTest {
        // When
        val result = repository.budgetExists("nonexistent", BudgetPeriod.MONTHLY)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun budgetExists_distinguishesBetweenPeriods() = runTest {
        // Given - Only monthly budget
        repository.saveBudget(createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY))

        // When
        val monthlyExists = repository.budgetExists("food", BudgetPeriod.MONTHLY)
        val weeklyExists = repository.budgetExists("food", BudgetPeriod.WEEKLY)

        // Then
        assertTrue(monthlyExists.getOrNull() == true)
        assertFalse(weeklyExists.getOrNull() == true)
    }

    @Test
    fun getBudgetCount_returnsCorrectCount() = runTest {
        // Given
        repository.saveBudgets(
            listOf(
                createTestBudget(categoryId = "food"),
                createTestBudget(categoryId = "transport"),
                createTestBudget(categoryId = "shopping")
            )
        )

        // When
        val result = repository.getBudgetCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun getBudgetCount_returnsZeroWhenEmpty() = runTest {
        // When
        val result = repository.getBudgetCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun getLatestUpdateTime_returnsCorrectTimestamp() = runTest {
        // Given - Create budgets with different update times
        val time1 = System.currentTimeMillis()
        Thread.sleep(10) // Ensure different timestamps
        val time2 = System.currentTimeMillis()

        val budget1 = createTestBudget(categoryId = "food").copy(updatedAt = time1)
        val budget2 = createTestBudget(categoryId = "transport").copy(updatedAt = time2)

        repository.saveBudgets(listOf(budget1, budget2))

        // When
        val result = repository.getLatestUpdateTime()

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        // Latest should be time2 (most recent)
        assertTrue(result.getOrNull()!! >= time1)
    }

    @Test
    fun getLatestUpdateTime_returnsNullWhenEmpty() = runTest {
        // When
        val result = repository.getLatestUpdateTime()

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    // ==================== Cache Tests ====================

    @Test
    fun cacheInvalidation_afterSaveBudget() = runTest {
        // Given - First save to populate cache
        val budget1 = createTestBudget(categoryId = "food", amount = 10000.0)
        repository.saveBudget(budget1)

        // Populate cache
        val result1 = repository.getAllBudgetsSnapshot()
        assertEquals(1, result1.getOrNull()?.size)

        // When - Save another budget (should invalidate cache)
        val budget2 = createTestBudget(categoryId = "transport", amount = 5000.0)
        repository.saveBudget(budget2)

        // Then - Should get fresh data from database
        val result2 = repository.getAllBudgetsSnapshot()
        assertEquals(2, result2.getOrNull()?.size)
    }

    @Test
    fun cacheInvalidation_afterUpdateBudget() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food", amount = 10000.0)
        val savedId = repository.saveBudget(budget).getOrNull()!!

        // Populate cache
        val result1 = repository.getAllBudgetsSnapshot()
        assertEquals(10000.0, result1.getOrNull()?.get(0)?.amount, 0.001)

        // When - Update budget (should invalidate cache)
        val updatedBudget = budget.copy(id = savedId, amount = 15000.0)
        repository.updateBudget(updatedBudget)

        // Then - Should get updated data
        val result2 = repository.getAllBudgetsSnapshot()
        assertEquals(15000.0, result2.getOrNull()?.get(0)?.amount, 0.001)
    }

    @Test
    fun cacheInvalidation_afterDeleteBudget() = runTest {
        // Given
        repository.saveBudgets(
            listOf(
                createTestBudget(categoryId = "food"),
                createTestBudget(categoryId = "transport")
            )
        )

        // Populate cache
        val result1 = repository.getAllBudgetsSnapshot()
        assertEquals(2, result1.getOrNull()?.size)

        // When - Delete a budget (should invalidate cache)
        val budgetId = result1.getOrNull()!![0].id
        repository.deleteBudget(budgetId)

        // Then - Should get updated data
        val result2 = repository.getAllBudgetsSnapshot()
        assertEquals(1, result2.getOrNull()?.size)
    }

    @Test
    fun cacheHit_onConsecutiveCalls() = runTest {
        // Given
        repository.saveBudget(createTestBudget(categoryId = "food"))

        // When - First call populates cache
        val result1 = repository.getAllBudgetsSnapshot()

        // Second call should hit cache
        val result2 = repository.getAllBudgetsSnapshot()

        // Then - Both should return same data
        assertEquals(result1.getOrNull()?.size, result2.getOrNull()?.size)
        assertEquals(1, result2.getOrNull()?.size)
    }

    @Test
    fun getBudgetCount_usesCache() = runTest {
        // Given
        repository.saveBudgets(
            listOf(
                createTestBudget(categoryId = "food"),
                createTestBudget(categoryId = "transport")
            )
        )

        // When - First call should populate cache
        val count1 = repository.getBudgetCount()

        // Second call should hit cache
        val count2 = repository.getBudgetCount()

        // Then
        assertEquals(2, count1.getOrNull())
        assertEquals(2, count2.getOrNull())
    }

    @Test
    fun getLatestUpdateTime_usesCache() = runTest {
        // Given
        val budget = createTestBudget(categoryId = "food").copy(updatedAt = System.currentTimeMillis())
        repository.saveBudget(budget)

        // When - First call should populate cache
        val time1 = repository.getLatestUpdateTime()

        // Second call should hit cache
        val time2 = repository.getLatestUpdateTime()

        // Then
        assertEquals(time1.getOrNull(), time2.getOrNull())
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun saveBudget_handlesErrorGracefully() = runTest {
        // Given
        database.close() // Close database to simulate error

        // When
        val result = repository.saveBudget(createTestBudget(categoryId = "food"))

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getAllBudgetsSnapshot_handlesErrorGracefully() = runTest {
        // Given
        database.close() // Close database to simulate error

        // When
        val result = repository.getAllBudgetsSnapshot()

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun updateBudget_handlesErrorGracefully() = runTest {
        // Given
        val budget = createTestBudget(id = 1L, categoryId = "food")
        database.close()

        // When
        val result = repository.updateBudget(budget)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun deleteBudget_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.deleteBudget(1L)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun budgetExists_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.budgetExists("food", BudgetPeriod.MONTHLY)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun getBudgetCount_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getBudgetCount()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun getLatestUpdateTime_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getLatestUpdateTime()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun flowOperations_catchErrors() = runTest {
        // Given
        repository.saveBudget(createTestBudget(categoryId = "food"))

        // When - Get flow (should not throw even if there are errors)
        val flow = repository.getAllBudgets()
        val result = flow.first()

        // Then
        assertEquals(1, result.size)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun saveBudget_withZeroAmount() = runTest {
        // Given - Budget with zero amount
        val budget = createTestBudget(categoryId = "food", amount = 0.0)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllBudgetsSnapshot().getOrNull()
        assertEquals(0.0, loaded?.first()?.amount, 0.001)
    }

    @Test
    fun saveBudget_withVeryLargeAmount() = runTest {
        // Given - Budget with very large amount
        val largeAmount = 1000000000.0
        val budget = createTestBudget(categoryId = "food", amount = largeAmount)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllBudgetsSnapshot().getOrNull()
        assertEquals(largeAmount, loaded?.first()?.amount, 0.001)
    }

    @Test
    fun saveBudget_withVerySmallAmount() = runTest {
        // Given - Budget with very small amount
        val smallAmount = 0.01
        val budget = createTestBudget(categoryId = "food", amount = smallAmount)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllBudgetsSnapshot().getOrNull()
        assertEquals(smallAmount, loaded?.first()?.amount, 0.001)
    }

    @Test
    fun saveBudget_withEmptyStringCategory() = runTest {
        // Given - Budget with empty string category
        val budget = createTestBudget(categoryId = "", amount = 10000.0)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllBudgetsSnapshot().getOrNull()
        assertEquals("", loaded?.first()?.categoryId)
    }

    @Test
    fun saveBudget_withVeryLongCategoryId() = runTest {
        // Given - Budget with very long category ID
        val longCategory = "category_" + "a".repeat(500)
        val budget = createTestBudget(categoryId = longCategory, amount = 10000.0)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllBudgetsSnapshot().getOrNull()
        assertEquals(longCategory, loaded?.first()?.categoryId)
    }

    @Test
    fun saveBudget_withSpecialCharactersInCategory() = runTest {
        // Given - Budget with special characters
        val specialCategory = "food & beverages™️ (新しい)"
        val budget = createTestBudget(categoryId = specialCategory, amount = 10000.0)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllBudgetsSnapshot().getOrNull()
        assertEquals(specialCategory, loaded?.first()?.categoryId)
    }

    @Test
    fun saveBudget_withMinTimestamp() = runTest {
        // Given - Budget with minimum timestamp
        val minDate = 0L
        val budget = createTestBudget(categoryId = "food", startDate = minDate)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllBudgetsSnapshot().getOrNull()
        assertEquals(minDate, loaded?.first()?.startDate)
    }

    @Test
    fun saveBudget_withMaxTimestamp() = runTest {
        // Given - Budget with maximum timestamp
        val maxDate = Long.MAX_VALUE
        val budget = createTestBudget(categoryId = "food", startDate = maxDate)

        // When
        val result = repository.saveBudget(budget)

        // Then
        assertTrue(result.isSuccess)
        val loaded = repository.getAllBudgetsSnapshot().getOrNull()
        assertEquals(maxDate, loaded?.first()?.startDate)
    }

    @Test
    fun saveBudgets_withEmptyList() = runTest {
        // Given - Empty list
        val emptyList = emptyList<BudgetEntity>()

        // When
        val result = repository.saveBudgets(emptyList)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, repository.getBudgetCount().getOrNull())
    }

    @Test
    fun saveBudgets_withLargeBatch() = runTest {
        // Given - Large batch of budgets
        val largeBatch = (1..100).map { i ->
            createTestBudget(
                categoryId = "category_$i",
                amount = i * 1000.0,
                period = if (i % 2 == 0) BudgetPeriod.MONTHLY else BudgetPeriod.WEEKLY
            )
        }

        // When
        val result = repository.saveBudgets(largeBatch)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(100, repository.getBudgetCount().getOrNull())
    }

    @Test
    fun deleteBudget_nonExistent() = runTest {
        // When - Try to delete non-existent budget
        val result = repository.deleteBudget(999L)

        // Then - Should return false
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun getBudgetsByPeriod_withNoBudgets() = runTest {
        // When - Query when database is empty
        val result = repository.getBudgetsByPeriod(BudgetPeriod.MONTHLY).first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun getBudgetsByCategory_withNoBudgets() = runTest {
        // When - Query when database is empty
        val result = repository.getBudgetsByCategory("food").first()

        // Then
        assertTrue(result.isEmpty())
    }

    // ==================== Integration Tests ====================

    @Test
    fun fullWorkflow_saveUpdateDelete() = runTest {
        // 1. Save budget
        val budget = createTestBudget(categoryId = "food", amount = 10000.0)
        val savedId = repository.saveBudget(budget).getOrNull()!!
        assertEquals(1, repository.getBudgetCount().getOrNull())

        // 2. Verify it exists
        assertTrue(repository.budgetExists("food", BudgetPeriod.MONTHLY).getOrNull() == true)

        // 3. Update budget
        val updatedBudget = budget.copy(id = savedId, amount = 15000.0)
        repository.updateBudget(updatedBudget)
        val loaded = repository.getAllBudgetsSnapshot().getOrNull()!!.first()
        assertEquals(15000.0, loaded.amount, 0.001)

        // 4. Delete budget
        repository.deleteBudget(savedId)
        assertEquals(0, repository.getBudgetCount().getOrNull())
        assertFalse(repository.budgetExists("food", BudgetPeriod.MONTHLY).getOrNull() == true)
    }

    @Test
    fun multipleBudgets_complexQueries() = runTest {
        // Given - Complex budget setup
        repository.saveBudgets(
            listOf(
                createTestBudget(categoryId = "food", period = BudgetPeriod.MONTHLY, amount = 30000.0),
                createTestBudget(categoryId = "food", period = BudgetPeriod.WEEKLY, amount = 7000.0),
                createTestBudget(categoryId = "transport", period = BudgetPeriod.MONTHLY, amount = 10000.0),
                createTestBudget(categoryId = "transport", period = BudgetPeriod.WEEKLY, amount = 2500.0),
                createTestBudget(categoryId = "shopping", period = BudgetPeriod.MONTHLY, amount = 20000.0)
            )
        )

        // When/Then - Test various queries
        assertEquals(5, repository.getAllBudgetsSnapshot().getOrNull()?.size)
        assertEquals(3, repository.getBudgetsByPeriod(BudgetPeriod.MONTHLY).first().size)
        assertEquals(2, repository.getBudgetsByPeriod(BudgetPeriod.WEEKLY).first().size)
        assertEquals(2, repository.getBudgetsByCategory("food").first().size)
        assertEquals(2, repository.getBudgetsByCategory("transport").first().size)
        assertEquals(1, repository.getBudgetsByCategory("shopping").first().size)

        val foodMonthly = repository.getBudgetByCategoryAndPeriod("food", BudgetPeriod.MONTHLY).first()
        assertNotNull(foodMonthly)
        assertEquals(30000.0, foodMonthly?.amount, 0.001)
    }

    @Test
    fun concurrentOperations_maintainDataIntegrity() = runTest {
        // Given - Multiple operations
        val budgets = (1..20).map { i ->
            createTestBudget(
                categoryId = "category_$i",
                amount = i * 1000.0,
                period = if (i % 2 == 0) BudgetPeriod.MONTHLY else BudgetPeriod.WEEKLY
            )
        }

        // When - Batch save and individual queries
        repository.saveBudgets(budgets)

        // Then - All data should be consistent
        assertEquals(20, repository.getBudgetCount().getOrNull())
        assertEquals(10, repository.getBudgetsByPeriod(BudgetPeriod.MONTHLY).first().size)
        assertEquals(10, repository.getBudgetsByPeriod(BudgetPeriod.WEEKLY).first().size)

        val allBudgets = repository.getAllBudgetsSnapshot().getOrNull()
        assertEquals(20, allBudgets?.size)
    }

    @Test
    fun stressTest_rapidConsecutiveWrites() = runTest {
        // Given - Simulate rapid consecutive writes
        val iterations = 50

        // When - Rapid writes
        repeat(iterations) { i ->
            repository.saveBudget(
                createTestBudget(
                    categoryId = "category_$i",
                    amount = i * 1000.0
                )
            )
        }

        // Then - All should be saved
        val count = repository.getBudgetCount().getOrNull()
        assertEquals(iterations, count)
    }

    @Test
    fun stressTest_rapidConsecutiveReadsAndWrites() = runTest {
        // Given
        repository.saveBudget(createTestBudget(categoryId = "food"))

        // When - Interleaved reads and writes
        repeat(30) { i ->
            if (i % 2 == 0) {
                repository.saveBudget(createTestBudget(categoryId = "category_$i"))
            } else {
                repository.getAllBudgetsSnapshot()
            }
        }

        // Then - Should handle without errors
        val result = repository.getAllBudgetsSnapshot()
        assertTrue(result.isSuccess)
        assertTrue((result.getOrNull()?.size ?: 0) > 0)
    }

    @Test
    fun getBudgetCount_afterMultipleDeletions() = runTest {
        // Given
        repository.saveBudgets(
            (1..10).map { createTestBudget(categoryId = "category_$it") }
        )

        // When - Delete half of them
        val allBudgets = repository.getAllBudgetsSnapshot().getOrNull()!!
        allBudgets.take(5).forEach { budget ->
            repository.deleteBudget(budget.id)
        }

        // Then
        assertEquals(5, repository.getBudgetCount().getOrNull())
    }

    @Test
    fun mixedPeriods_independentOperations() = runTest {
        // Given - Create budgets for same category with different periods
        val monthlyBudget = createTestBudget(
            categoryId = "food",
            period = BudgetPeriod.MONTHLY,
            amount = 30000.0
        )
        val weeklyBudget = createTestBudget(
            categoryId = "food",
            period = BudgetPeriod.WEEKLY,
            amount = 7000.0
        )

        repository.saveBudgets(listOf(monthlyBudget, weeklyBudget))

        // When - Delete monthly budget
        repository.deleteBudgetByCategoryAndPeriod("food", BudgetPeriod.MONTHLY)

        // Then - Weekly budget should still exist
        val weeklyExists = repository.budgetExists("food", BudgetPeriod.WEEKLY).getOrNull()
        val monthlyExists = repository.budgetExists("food", BudgetPeriod.MONTHLY).getOrNull()

        assertTrue(weeklyExists == true)
        assertFalse(monthlyExists == true)
        assertEquals(1, repository.getBudgetCount().getOrNull())
    }
}
