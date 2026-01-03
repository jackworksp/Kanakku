package com.example.kanakku.data.repository

import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.toDomain
import com.example.kanakku.data.database.toEntity
import com.example.kanakku.data.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository for managing budget persistence and retrieval.
 *
 * This repository acts as a bridge between the domain layer (Budget)
 * and the data layer (BudgetEntity), handling all entity-model mapping
 * and database operations.
 *
 * Key responsibilities:
 * - Save and retrieve budgets (overall and category-specific)
 * - Query budgets by month/year
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
 * - Cache invalidated automatically when data changes (save, delete operations)
 * - Thread-safe cache access using Mutex for coroutine synchronization
 * - Cache failures are transparent - falls back to database on cache miss
 * - Improves performance for common operations like getting budgets for current month
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
    private var budgetsCache: Map<BudgetCacheKey, Budget>? = null
    private var monthBudgetsCache: Map<MonthYear, List<Budget>>? = null
    private var budgetCountCache: Int? = null

    /**
     * Mutex for thread-safe cache access.
     * Ensures cache consistency when accessed from multiple coroutines.
     */
    private val cacheMutex = Mutex()

    /**
     * Cache key for individual budgets.
     * Uniquely identifies a budget by categoryId, month, and year.
     */
    private data class BudgetCacheKey(
        val categoryId: String?,
        val month: Int,
        val year: Int
    )

    /**
     * Cache key for month-based budget queries.
     */
    private data class MonthYear(val month: Int, val year: Int)

    /**
     * Invalidates all cached data.
     * Should be called after any operation that modifies budgets.
     */
    private suspend fun invalidateCache() {
        cacheMutex.withLock {
            budgetsCache = null
            monthBudgetsCache = null
            budgetCountCache = null
        }
        ErrorHandler.logDebug("Cache invalidated", "BudgetRepository")
    }

    /**
     * Updates the budgets cache with fresh data.
     * Thread-safe operation using cacheMutex.
     *
     * @param key The cache key
     * @param budget The budget to cache
     */
    private suspend fun updateBudgetCache(key: BudgetCacheKey, budget: Budget?) {
        cacheMutex.withLock {
            if (budget != null) {
                val currentCache = budgetsCache?.toMutableMap() ?: mutableMapOf()
                currentCache[key] = budget
                budgetsCache = currentCache
                ErrorHandler.logDebug("Budget cached: $key", "BudgetRepository")
            } else {
                // Remove from cache if budget is null
                budgetsCache = budgetsCache?.filterKeys { it != key }
                ErrorHandler.logDebug("Budget removed from cache: $key", "BudgetRepository")
            }
        }
    }

    /**
     * Updates the month budgets cache with fresh data.
     * Thread-safe operation using cacheMutex.
     *
     * @param monthYear The month/year key
     * @param budgets The list of budgets to cache
     */
    private suspend fun updateMonthBudgetsCache(monthYear: MonthYear, budgets: List<Budget>) {
        cacheMutex.withLock {
            val currentCache = monthBudgetsCache?.toMutableMap() ?: mutableMapOf()
            currentCache[monthYear] = budgets
            monthBudgetsCache = currentCache
            ErrorHandler.logDebug(
                "Month budgets cached: $monthYear (${budgets.size} budgets)",
                "BudgetRepository"
            )
        }
    }

    // ==================== Budget Operations ====================

    /**
     * Saves or updates a budget in the database.
     * Converts the domain model to an entity before persisting.
     * Invalidates cache after successful save to ensure data consistency.
     *
     * @param budget The budget to save or update
     * @return Result<Long> containing the row ID or error information
     */
    suspend fun saveBudget(budget: Budget): Result<Long> {
        return ErrorHandler.runSuspendCatching("Save budget") {
            val rowId = budgetDao.insertOrUpdate(budget.toEntity())
            invalidateCache()
            rowId
        }
    }

    /**
     * Saves or updates multiple budgets in a single operation.
     * More efficient than saving individually for bulk operations.
     * Invalidates cache after successful save to ensure data consistency.
     *
     * @param budgets List of budgets to save or update
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun saveBudgets(budgets: List<Budget>): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Save budgets") {
            budgetDao.insertOrUpdateAll(budgets.map { it.toEntity() })
            invalidateCache()
        }
    }

    /**
     * Retrieves a budget for a specific category, month, and year.
     * Uses in-memory cache to reduce database reads - falls back to database on cache miss.
     *
     * @param categoryId The category ID (null for overall budget)
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Result<Budget?> containing the budget or null if not found, or error information
     */
    suspend fun getBudget(categoryId: String?, month: Int, year: Int): Result<Budget?> {
        return ErrorHandler.runSuspendCatching("Get budget") {
            val key = BudgetCacheKey(categoryId, month, year)

            // Check cache first
            val cached = cacheMutex.withLock { budgetsCache?.get(key) }
            if (cached != null) {
                ErrorHandler.logDebug("Cache hit: getBudget($key)", "BudgetRepository")
                return@runSuspendCatching cached
            }

            // Cache miss - fetch from database
            ErrorHandler.logDebug("Cache miss: getBudget($key)", "BudgetRepository")
            val budget = budgetDao.getBudget(categoryId, month, year)?.toDomain()

            // Update cache with fresh data
            updateBudgetCache(key, budget)

            budget
        }
    }

    /**
     * Retrieves a budget for a specific category, month, and year as a Flow.
     * Emits updates whenever the budget changes.
     * Errors are logged and null is emitted on failure.
     *
     * @param categoryId The category ID (null for overall budget)
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Flow emitting the budget or null if not found
     */
    fun getBudgetFlow(categoryId: String?, month: Int, year: Int): Flow<Budget?> {
        return budgetDao.getBudgetFlow(categoryId, month, year)
            .map { entity -> entity?.toDomain() }
            .catch { throwable ->
                ErrorHandler.handleError(
                    throwable as Exception,
                    "Get budget flow (categoryId=$categoryId, month=$month, year=$year)"
                )
                emit(null)
            }
    }

    /**
     * Retrieves the overall monthly budget for a specific month and year.
     * Overall budget is identified by null categoryId.
     * Uses in-memory cache to reduce database reads.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Result<Budget?> containing the overall budget or null if not set, or error information
     */
    suspend fun getOverallBudget(month: Int, year: Int): Result<Budget?> {
        return getBudget(null, month, year)
    }

    /**
     * Retrieves the overall monthly budget as a Flow.
     * Emits updates whenever the overall budget changes.
     * Errors are logged and null is emitted on failure.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Flow emitting the overall budget or null if not set
     */
    fun getOverallBudgetFlow(month: Int, year: Int): Flow<Budget?> {
        return getBudgetFlow(null, month, year)
    }

    /**
     * Retrieves all budgets for a specific month and year.
     * Includes both overall and category-specific budgets.
     * Uses in-memory cache to reduce database reads.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Result<List<Budget>> containing all budgets for the month or error information
     */
    suspend fun getBudgetsForMonth(month: Int, year: Int): Result<List<Budget>> {
        return ErrorHandler.runSuspendCatching("Get budgets for month") {
            val monthYear = MonthYear(month, year)

            // Check cache first
            val cached = cacheMutex.withLock { monthBudgetsCache?.get(monthYear) }
            if (cached != null) {
                ErrorHandler.logDebug("Cache hit: getBudgetsForMonth($monthYear)", "BudgetRepository")
                return@runSuspendCatching cached
            }

            // Cache miss - fetch from database
            ErrorHandler.logDebug("Cache miss: getBudgetsForMonth($monthYear)", "BudgetRepository")
            val budgets = budgetDao.getBudgetsForMonth(month, year)
                .map { it.toDomain() }

            // Update cache with fresh data
            updateMonthBudgetsCache(monthYear, budgets)

            budgets
        }
    }

    /**
     * Retrieves all budgets for a specific month and year as a Flow.
     * Emits updates whenever any budget for the month changes.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Flow emitting list of all budgets for the month
     */
    fun getBudgetsForMonthFlow(month: Int, year: Int): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonthFlow(month, year)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(
                    throwable as Exception,
                    "Get budgets for month flow (month=$month, year=$year)"
                )
                emit(emptyList())
            }
    }

    /**
     * Retrieves all category budgets for a specific month and year.
     * Excludes the overall budget (categoryId IS NOT NULL).
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Result<List<Budget>> containing category budgets or error information
     */
    suspend fun getCategoryBudgetsForMonth(month: Int, year: Int): Result<List<Budget>> {
        return ErrorHandler.runSuspendCatching("Get category budgets for month") {
            budgetDao.getCategoryBudgetsForMonth(month, year)
                .map { it.toDomain() }
        }
    }

    /**
     * Retrieves all category budgets for a specific month and year as a Flow.
     * Emits updates whenever any category budget for the month changes.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Flow emitting list of category budgets for the month
     */
    fun getCategoryBudgetsForMonthFlow(month: Int, year: Int): Flow<List<Budget>> {
        return budgetDao.getCategoryBudgetsForMonthFlow(month, year)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(
                    throwable as Exception,
                    "Get category budgets for month flow (month=$month, year=$year)"
                )
                emit(emptyList())
            }
    }

    /**
     * Retrieves all budgets in the database.
     * Useful for backup/restore operations.
     *
     * @return Result<List<Budget>> containing all budgets or error information
     */
    suspend fun getAllBudgets(): Result<List<Budget>> {
        return ErrorHandler.runSuspendCatching("Get all budgets") {
            budgetDao.getAllBudgets()
                .map { it.toDomain() }
        }
    }

    /**
     * Retrieves all budgets as a Flow.
     * Emits updates whenever any budget changes.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @return Flow emitting list of all budgets
     */
    fun getAllBudgetsFlow(): Flow<List<Budget>> {
        return budgetDao.getAllBudgetsFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get all budgets flow")
                emit(emptyList())
            }
    }

    /**
     * Deletes a specific budget entry.
     * Invalidates cache after successful deletion to ensure data consistency.
     *
     * @param categoryId The category ID (null for overall budget)
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Result<Boolean> indicating if budget was deleted or error information
     */
    suspend fun deleteBudget(categoryId: String?, month: Int, year: Int): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Delete budget") {
            val deleted = budgetDao.deleteBudget(categoryId, month, year) > 0
            if (deleted) {
                invalidateCache()
            }
            deleted
        }
    }

    /**
     * Deletes a budget by its ID.
     * Invalidates cache after successful deletion to ensure data consistency.
     *
     * @param id The budget ID
     * @return Result<Boolean> indicating if budget was deleted or error information
     */
    suspend fun deleteBudgetById(id: Long): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Delete budget by ID") {
            val deleted = budgetDao.deleteBudgetById(id) > 0
            if (deleted) {
                invalidateCache()
            }
            deleted
        }
    }

    /**
     * Deletes all budgets for a specific month and year.
     * Useful for resetting budgets for a specific month.
     * Invalidates cache after successful deletion to ensure data consistency.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Result<Int> containing number of budgets deleted or error information
     */
    suspend fun deleteBudgetsForMonth(month: Int, year: Int): Result<Int> {
        return ErrorHandler.runSuspendCatching("Delete budgets for month") {
            val deletedCount = budgetDao.deleteBudgetsForMonth(month, year)
            if (deletedCount > 0) {
                invalidateCache()
            }
            deletedCount
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
     * Checks if a budget exists for the given parameters.
     *
     * @param categoryId The category ID (null for overall budget)
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Result<Boolean> indicating if budget exists or error information
     */
    suspend fun budgetExists(categoryId: String?, month: Int, year: Int): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Check budget exists") {
            budgetDao.exists(categoryId, month, year)
        }
    }

    /**
     * Gets the total count of budgets in the database.
     * Uses in-memory cache to reduce database reads.
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
     * Gets the count of budgets for a specific month and year.
     *
     * @param month The month (1-12)
     * @param year The year (e.g., 2024)
     * @return Result<Int> containing number of budgets for the month or error information
     */
    suspend fun getBudgetCountForMonth(month: Int, year: Int): Result<Int> {
        return ErrorHandler.runSuspendCatching("Get budget count for month") {
            budgetDao.getBudgetCountForMonth(month, year)
        }
    }
}
