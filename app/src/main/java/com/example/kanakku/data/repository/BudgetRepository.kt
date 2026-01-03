package com.example.kanakku.data.repository

import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.entity.BudgetEntity
import com.example.kanakku.data.model.BudgetPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository for managing budget persistence and retrieval.
 *
 * This repository handles all budget-related database operations, providing
 * a clean interface for the domain layer to interact with budget data.
 *
 * Key responsibilities:
 * - Save, update, and delete budgets
 * - Query budgets by category, period, or both
 * - Provide reactive data streams via Flow
 * - Handle all database errors gracefully with comprehensive error handling
 * - Provide in-memory caching for frequently accessed data to improve performance
 *
 * Error Handling Strategy:
 * - All database operations wrapped in try-catch blocks via ErrorHandler
 * - Errors logged for debugging with appropriate context
 * - Suspend functions return Result<T> for error propagation to callers
 * - Flow operations include .catch() to emit fallback values on error
 * - No uncaught exceptions - all errors are handled gracefully
 *
 * Caching Strategy:
 * - Frequently accessed data cached in memory to reduce database reads
 * - Cache invalidated automatically when data changes (save, update, delete operations)
 * - Thread-safe cache access using Mutex for coroutine synchronization
 * - Cache failures are transparent - falls back to database on cache miss
 * - Improves performance for common operations like getAllBudgets()
 *
 * @param database The Room database instance
 */
class BudgetRepository(private val database: KanakkuDatabase) {

    // DAO for database access
    private val budgetDao = database.budgetDao()

    // ==================== In-Memory Cache ====================

    /**
     * Cache for frequently accessed data to reduce database queries.
     * All cache access is synchronized using cacheMutex to ensure thread safety.
     */
    private var budgetsCache: List<BudgetEntity>? = null
    private var budgetCountCache: Int? = null
    private var latestUpdateTimeCache: Long? = null

    /**
     * Mutex for thread-safe cache access.
     * Ensures cache consistency when accessed from multiple coroutines.
     */
    private val cacheMutex = Mutex()

    /**
     * Invalidates all cached data.
     * Should be called after any operation that modifies budgets.
     */
    private suspend fun invalidateCache() {
        cacheMutex.withLock {
            budgetsCache = null
            budgetCountCache = null
            latestUpdateTimeCache = null
        }
        ErrorHandler.logDebug("Cache invalidated", "BudgetRepository")
    }

    /**
     * Updates the budgets cache with fresh data.
     * Thread-safe operation using cacheMutex.
     *
     * @param budgets The budgets to cache
     */
    private suspend fun updateBudgetsCache(budgets: List<BudgetEntity>) {
        cacheMutex.withLock {
            budgetsCache = budgets
            budgetCountCache = budgets.size
            // Update latest update time from cached budgets
            latestUpdateTimeCache = budgets.maxOfOrNull { it.updatedAt }
        }
        ErrorHandler.logDebug(
            "Cache updated: ${budgets.size} budgets",
            "BudgetRepository"
        )
    }

    // ==================== Budget Operations ====================

    /**
     * Saves a single budget to the database.
     * If a budget with the same categoryId and period exists, it will be replaced.
     * Invalidates cache after successful save to ensure data consistency.
     *
     * @param budget The budget entity to save
     * @return Result<Long> containing the row ID of the inserted budget or error information
     */
    suspend fun saveBudget(budget: BudgetEntity): Result<Long> {
        return ErrorHandler.runSuspendCatching("Save budget") {
            val rowId = budgetDao.insert(budget)
            invalidateCache()
            rowId
        }
    }

    /**
     * Saves multiple budgets to the database in a single operation.
     * More efficient than saving individually for bulk imports.
     * Invalidates cache after successful save to ensure data consistency.
     *
     * @param budgets List of budget entities to save
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun saveBudgets(budgets: List<BudgetEntity>): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Save budgets") {
            budgetDao.insertAll(budgets)
            invalidateCache()
        }
    }

    /**
     * Updates an existing budget in the database.
     * The budget must have the same ID as an existing budget.
     * Invalidates cache after successful update to ensure data consistency.
     *
     * @param budget The budget entity to update
     * @return Result<Boolean> indicating if budget was updated or error information
     */
    suspend fun updateBudget(budget: BudgetEntity): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Update budget") {
            val updated = budgetDao.update(budget) > 0
            if (updated) {
                invalidateCache()
            }
            updated
        }
    }

    /**
     * Retrieves all budgets as a reactive Flow.
     * Automatically sorted by period and category.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @return Flow emitting list of budget entities, sorted by period and category
     */
    fun getAllBudgets(): Flow<List<BudgetEntity>> {
        return budgetDao.getAllBudgets()
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get all budgets")
                emit(emptyList())
            }
    }

    /**
     * Retrieves all budgets as a one-time snapshot.
     * Useful for non-reactive operations.
     * Uses in-memory cache to reduce database reads - falls back to database on cache miss.
     *
     * @return Result<List<BudgetEntity>> containing budgets or error information
     */
    suspend fun getAllBudgetsSnapshot(): Result<List<BudgetEntity>> {
        return ErrorHandler.runSuspendCatching("Get all budgets snapshot") {
            // Check cache first
            val cached = cacheMutex.withLock { budgetsCache }
            if (cached != null) {
                ErrorHandler.logDebug("Cache hit: getAllBudgetsSnapshot", "BudgetRepository")
                return@runSuspendCatching cached
            }

            // Cache miss - fetch from database
            ErrorHandler.logDebug("Cache miss: getAllBudgetsSnapshot", "BudgetRepository")
            val budgets = budgetDao.getAllBudgetsSnapshot()

            // Update cache with fresh data
            updateBudgetsCache(budgets)

            budgets
        }
    }

    /**
     * Retrieves budgets filtered by period (MONTHLY/WEEKLY).
     * Errors are logged and an empty list is emitted on failure.
     *
     * @param period The budget period to filter by
     * @return Flow emitting list of budgets matching the period
     */
    fun getBudgetsByPeriod(period: BudgetPeriod): Flow<List<BudgetEntity>> {
        return budgetDao.getBudgetsByPeriod(period)
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get budgets by period: $period")
                emit(emptyList())
            }
    }

    /**
     * Retrieves a specific budget by category ID and period.
     * Errors are logged and null is emitted on failure.
     *
     * @param categoryId The category ID to query
     * @param period The budget period to query
     * @return Flow emitting the budget, or null if not found
     */
    fun getBudgetByCategoryAndPeriod(
        categoryId: String,
        period: BudgetPeriod
    ): Flow<BudgetEntity?> {
        return budgetDao.getBudgetByCategoryAndPeriod(categoryId, period)
            .catch { throwable ->
                ErrorHandler.handleError(
                    throwable as Exception,
                    "Get budget by category and period"
                )
                emit(null)
            }
    }

    /**
     * Retrieves a specific budget by category ID and period as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @param categoryId The category ID to query
     * @param period The budget period to query
     * @return Result<BudgetEntity?> containing the budget or error information
     */
    suspend fun getBudgetByCategoryAndPeriodSnapshot(
        categoryId: String,
        period: BudgetPeriod
    ): Result<BudgetEntity?> {
        return ErrorHandler.runSuspendCatching("Get budget by category and period snapshot") {
            budgetDao.getBudgetByCategoryAndPeriodSnapshot(categoryId, period)
        }
    }

    /**
     * Retrieves all budgets for a specific category across all periods.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @param categoryId The category ID to query
     * @return Flow emitting list of budgets for the category
     */
    fun getBudgetsByCategory(categoryId: String): Flow<List<BudgetEntity>> {
        return budgetDao.getBudgetsByCategory(categoryId)
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get budgets by category: $categoryId")
                emit(emptyList())
            }
    }

    /**
     * Deletes a budget by its ID.
     * Invalidates cache after successful deletion to ensure data consistency.
     *
     * @param id The ID of the budget to delete
     * @return Result<Boolean> indicating if budget was deleted or error information
     */
    suspend fun deleteBudget(id: Long): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Delete budget") {
            val deleted = budgetDao.deleteById(id) > 0
            if (deleted) {
                invalidateCache()
            }
            deleted
        }
    }

    /**
     * Deletes a budget by category ID and period.
     * Invalidates cache after successful deletion to ensure data consistency.
     *
     * @param categoryId The category ID of the budget to delete
     * @param period The period of the budget to delete
     * @return Result<Boolean> indicating if budget was deleted or error information
     */
    suspend fun deleteBudgetByCategoryAndPeriod(
        categoryId: String,
        period: BudgetPeriod
    ): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Delete budget by category and period") {
            val deleted = budgetDao.deleteByCategoryAndPeriod(categoryId, period) > 0
            if (deleted) {
                invalidateCache()
            }
            deleted
        }
    }

    /**
     * Deletes all budgets from the database.
     * Use with caution - this cannot be undone.
     * Invalidates cache after successful deletion to ensure data consistency.
     *
     * @return Result<Int> containing number of budgets deleted or error information
     */
    suspend fun deleteAllBudgets(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Delete all budgets") {
            val deletedCount = budgetDao.deleteAll()
            if (deletedCount > 0) {
                invalidateCache()
            }
            deletedCount
        }
    }

    /**
     * Checks if a budget exists for the given category and period.
     *
     * @param categoryId The category ID to check
     * @param period The period to check
     * @return Result<Boolean> indicating if budget exists or error information
     */
    suspend fun budgetExists(categoryId: String, period: BudgetPeriod): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Check budget exists") {
            budgetDao.exists(categoryId, period)
        }
    }

    /**
     * Gets the total count of budgets in the database.
     * Uses in-memory cache to reduce database reads - falls back to database on cache miss.
     *
     * @return Result<Int> containing total number of budgets or error information
     */
    suspend fun getBudgetCount(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Get budget count") {
            // Check cache first
            val cached = cacheMutex.withLock { budgetCountCache }
            if (cached != null) {
                ErrorHandler.logDebug("Cache hit: getBudgetCount", "BudgetRepository")
                return@runSuspendCatching cached
            }

            // Cache miss - fetch from database
            ErrorHandler.logDebug("Cache miss: getBudgetCount", "BudgetRepository")
            val count = budgetDao.getBudgetCount()

            // Update cache
            cacheMutex.withLock { budgetCountCache = count }

            count
        }
    }

    /**
     * Gets the most recently updated budget timestamp.
     * Useful for determining last modification time.
     * Uses in-memory cache to reduce database reads - falls back to database on cache miss.
     *
     * @return Result<Long?> containing timestamp of most recent update or error information
     */
    suspend fun getLatestUpdateTime(): Result<Long?> {
        return ErrorHandler.runSuspendCatching("Get latest update time") {
            // Check cache first
            val cached = cacheMutex.withLock { latestUpdateTimeCache }
            if (cached != null) {
                ErrorHandler.logDebug("Cache hit: getLatestUpdateTime", "BudgetRepository")
                return@runSuspendCatching cached
            }

            // Cache miss - fetch from database
            ErrorHandler.logDebug("Cache miss: getLatestUpdateTime", "BudgetRepository")
            val latestTime = budgetDao.getLatestUpdateTime()

            // Update cache
            cacheMutex.withLock { latestUpdateTimeCache = latestTime }

            latestTime
        }
    }
}
