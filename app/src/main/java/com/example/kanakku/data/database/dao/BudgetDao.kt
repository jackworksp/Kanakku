package com.example.kanakku.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kanakku.data.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for budget operations.
 * Provides CRUD operations for persisting and querying monthly budgets.
 */
@Dao
interface BudgetDao {

    /**
     * Inserts or updates a budget entry.
     * If a budget for the same categoryId, month, and year exists, it will be replaced.
     * This provides upsert functionality for budget management.
     *
     * @param budget The budget entity to insert or update
     * @return The row ID of the inserted/updated budget
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: BudgetEntity): Long

    /**
     * Inserts or updates multiple budgets in a single transaction.
     * If budgets with the same categoryId, month, and year exist, they will be replaced.
     *
     * @param budgets List of budget entities to insert or update
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(budgets: List<BudgetEntity>)

    /**
     * Retrieves a budget for a specific category, month, and year.
     * Returns null if no budget is set for the specified parameters.
     *
     * @param categoryId The category ID (null for overall budget)
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return The budget entity, or null if not found
     */
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year LIMIT 1")
    suspend fun getBudget(categoryId: String?, month: Int, year: Int): BudgetEntity?

    /**
     * Retrieves a budget for a specific category, month, and year as a Flow.
     * Emits updates whenever the budget changes.
     *
     * @param categoryId The category ID (null for overall budget)
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Flow emitting the budget entity, or null if not found
     */
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year LIMIT 1")
    fun getBudgetFlow(categoryId: String?, month: Int, year: Int): Flow<BudgetEntity?>

    /**
     * Retrieves the overall monthly budget for a specific month and year.
     * Overall budget is identified by null categoryId.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return The overall budget entity, or null if not set
     */
    @Query("SELECT * FROM budgets WHERE categoryId IS NULL AND month = :month AND year = :year LIMIT 1")
    suspend fun getOverallBudget(month: Int, year: Int): BudgetEntity?

    /**
     * Retrieves the overall monthly budget as a Flow.
     * Emits updates whenever the overall budget changes.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Flow emitting the overall budget entity, or null if not set
     */
    @Query("SELECT * FROM budgets WHERE categoryId IS NULL AND month = :month AND year = :year LIMIT 1")
    fun getOverallBudgetFlow(month: Int, year: Int): Flow<BudgetEntity?>

    /**
     * Retrieves all budgets for a specific month and year.
     * Includes both overall and category-specific budgets.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return List of all budget entities for the specified month/year
     */
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year ORDER BY categoryId")
    suspend fun getBudgetsForMonth(month: Int, year: Int): List<BudgetEntity>

    /**
     * Retrieves all budgets for a specific month and year as a Flow.
     * Emits updates whenever any budget for the month changes.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Flow emitting list of all budget entities for the specified month/year
     */
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year ORDER BY categoryId")
    fun getBudgetsForMonthFlow(month: Int, year: Int): Flow<List<BudgetEntity>>

    /**
     * Retrieves all category budgets for a specific month and year.
     * Excludes the overall budget (categoryId IS NOT NULL).
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return List of category budget entities for the specified month/year
     */
    @Query("SELECT * FROM budgets WHERE categoryId IS NOT NULL AND month = :month AND year = :year ORDER BY categoryId")
    suspend fun getCategoryBudgetsForMonth(month: Int, year: Int): List<BudgetEntity>

    /**
     * Retrieves all category budgets for a specific month and year as a Flow.
     * Emits updates whenever any category budget for the month changes.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Flow emitting list of category budget entities for the specified month/year
     */
    @Query("SELECT * FROM budgets WHERE categoryId IS NOT NULL AND month = :month AND year = :year ORDER BY categoryId")
    fun getCategoryBudgetsForMonthFlow(month: Int, year: Int): Flow<List<BudgetEntity>>

    /**
     * Retrieves all budgets in the database.
     * Useful for backup/restore operations.
     *
     * @return List of all budget entities
     */
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC, categoryId")
    suspend fun getAllBudgets(): List<BudgetEntity>

    /**
     * Retrieves all budgets as a Flow.
     * Emits updates whenever any budget changes.
     *
     * @return Flow emitting list of all budget entities
     */
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC, categoryId")
    fun getAllBudgetsFlow(): Flow<List<BudgetEntity>>

    /**
     * Deletes a specific budget entry.
     *
     * @param categoryId The category ID (null for overall budget)
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year")
    suspend fun deleteBudget(categoryId: String?, month: Int, year: Int): Int

    /**
     * Deletes a budget by its ID.
     *
     * @param id The budget ID
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long): Int

    /**
     * Deletes all budgets for a specific month and year.
     * Useful for resetting budgets for a specific month.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Number of rows deleted
     */
    @Query("DELETE FROM budgets WHERE month = :month AND year = :year")
    suspend fun deleteBudgetsForMonth(month: Int, year: Int): Int

    /**
     * Deletes all budgets from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM budgets")
    suspend fun deleteAll(): Int

    /**
     * Checks if a budget exists for the given parameters.
     *
     * @param categoryId The category ID (null for overall budget)
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return True if budget exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year LIMIT 1)")
    suspend fun exists(categoryId: String?, month: Int, year: Int): Boolean

    /**
     * Gets the total count of budgets in the database.
     *
     * @return Total number of budgets
     */
    @Query("SELECT COUNT(*) FROM budgets")
    suspend fun getBudgetCount(): Int

    /**
     * Gets the count of budgets for a specific month and year.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Number of budgets for the specified month/year
     */
    @Query("SELECT COUNT(*) FROM budgets WHERE month = :month AND year = :year")
    suspend fun getBudgetCountForMonth(month: Int, year: Int): Int
}
