package com.example.kanakku.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kanakku.data.database.entity.BudgetEntity
import com.example.kanakku.data.model.BudgetPeriod
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for budget operations.
 * Provides CRUD operations for persisting and querying budget limits.
 */
@Dao
interface BudgetDao {

    /**
     * Inserts a single budget into the database.
     * If a budget with the same categoryId and period exists, it will be replaced.
     *
     * @param budget The budget entity to insert
     * @return The row ID of the inserted budget
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity): Long

    /**
     * Inserts multiple budgets into the database in a single transaction.
     * If budgets with the same categoryId and period exist, they will be replaced.
     *
     * @param budgets List of budget entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(budgets: List<BudgetEntity>)

    /**
     * Updates an existing budget in the database.
     *
     * @param budget The budget entity to update
     * @return Number of rows updated (0 if not found, 1 if updated)
     */
    @Update
    suspend fun update(budget: BudgetEntity): Int

    /**
     * Retrieves all budgets sorted by period and category.
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of all budgets
     */
    @Query("SELECT * FROM budgets ORDER BY period, categoryId")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    /**
     * Retrieves all budgets as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return List of all budgets
     */
    @Query("SELECT * FROM budgets ORDER BY period, categoryId")
    suspend fun getAllBudgetsSnapshot(): List<BudgetEntity>

    /**
     * Retrieves budgets filtered by period (MONTHLY/WEEKLY).
     * Returns a Flow for reactive updates.
     *
     * @param period The budget period to filter by
     * @return Flow emitting list of budgets matching the period
     */
    @Query("SELECT * FROM budgets WHERE period = :period ORDER BY categoryId")
    fun getBudgetsByPeriod(period: BudgetPeriod): Flow<List<BudgetEntity>>

    /**
     * Retrieves a specific budget by category ID and period.
     * Returns a Flow for reactive updates.
     *
     * @param categoryId The category ID to query
     * @param period The budget period to query
     * @return Flow emitting the budget, or null if not found
     */
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND period = :period LIMIT 1")
    fun getBudgetByCategoryAndPeriod(categoryId: String, period: BudgetPeriod): Flow<BudgetEntity?>

    /**
     * Retrieves a specific budget by category ID and period as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @param categoryId The category ID to query
     * @param period The budget period to query
     * @return The budget, or null if not found
     */
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND period = :period LIMIT 1")
    suspend fun getBudgetByCategoryAndPeriodSnapshot(categoryId: String, period: BudgetPeriod): BudgetEntity?

    /**
     * Retrieves all budgets for a specific category across all periods.
     *
     * @param categoryId The category ID to query
     * @return Flow emitting list of budgets for the category
     */
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId ORDER BY period")
    fun getBudgetsByCategory(categoryId: String): Flow<List<BudgetEntity>>

    /**
     * Deletes a budget by its ID.
     *
     * @param id The ID of the budget to delete
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * Deletes a budget by category ID and period.
     *
     * @param categoryId The category ID of the budget to delete
     * @param period The period of the budget to delete
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM budgets WHERE categoryId = :categoryId AND period = :period")
    suspend fun deleteByCategoryAndPeriod(categoryId: String, period: BudgetPeriod): Int

    /**
     * Deletes all budgets from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM budgets")
    suspend fun deleteAll(): Int

    /**
     * Checks if a budget exists for the given category and period.
     *
     * @param categoryId The category ID to check
     * @param period The period to check
     * @return True if budget exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM budgets WHERE categoryId = :categoryId AND period = :period LIMIT 1)")
    suspend fun exists(categoryId: String, period: BudgetPeriod): Boolean

    /**
     * Gets the total count of budgets in the database.
     *
     * @return Total number of budgets
     */
    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun getBudgetCount(): Int

    /**
     * Gets the most recently updated budget.
     * Useful for determining last modification time.
     *
     * @return The timestamp of the most recently updated budget, or null if no budgets exist
     */
    @Query("SELECT MAX(updatedAt) FROM budgets")
    suspend fun getLatestUpdateTime(): Long?
}
