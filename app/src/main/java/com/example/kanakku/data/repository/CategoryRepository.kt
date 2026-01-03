package com.example.kanakku.data.repository

import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.toDomain
import com.example.kanakku.data.database.toEntity
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository for managing custom category persistence and retrieval.
 *
 * This repository acts as a bridge between the domain layer (Category)
 * and the data layer (CustomCategoryEntity), handling all entity-model mapping
 * and database operations for custom categories.
 *
 * Key responsibilities:
 * - Save, update, and delete custom categories
 * - Retrieve categories and subcategories
 * - Seed default categories on first launch
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
 * - Improves performance for common operations like getAllCategories()
 *
 * @param database The Room database instance
 */
class CategoryRepository(private val database: KanakkuDatabase) {

    // DAO for database access
    private val customCategoryDao = database.customCategoryDao()

    // ==================== In-Memory Cache ====================

    /**
     * Cache for frequently accessed data to reduce database queries.
     * All cache access is synchronized using cacheMutex to ensure thread safety.
     */
    private var categoriesCache: List<Category>? = null
    private var categoryCountCache: Int? = null

    /**
     * Mutex for thread-safe cache access.
     * Ensures cache consistency when accessed from multiple coroutines.
     */
    private val cacheMutex = Mutex()

    /**
     * Invalidates all cached data.
     * Should be called after any operation that modifies categories.
     */
    private suspend fun invalidateCache() {
        cacheMutex.withLock {
            categoriesCache = null
            categoryCountCache = null
        }
        ErrorHandler.logDebug("Cache invalidated", "CategoryRepository")
    }

    /**
     * Updates the categories cache with fresh data.
     * Thread-safe operation using cacheMutex.
     *
     * @param categories The categories to cache
     */
    private suspend fun updateCategoriesCache(categories: List<Category>) {
        cacheMutex.withLock {
            categoriesCache = categories
            categoryCountCache = categories.size
        }
        ErrorHandler.logDebug(
            "Cache updated: ${categories.size} categories",
            "CategoryRepository"
        )
    }

    // ==================== Category Operations ====================

    /**
     * Saves a new category to the database.
     * Automatically sets sortOrder to place the category at the end.
     * Invalidates cache after successful save to ensure data consistency.
     *
     * @param category The category to save
     * @param parentId Optional parent category ID for subcategories
     * @param isSystemCategory Whether this is a system (default) category
     * @return Result<Long> containing the ID of the saved category or error information
     */
    suspend fun saveCategory(
        category: Category,
        parentId: Long? = null,
        isSystemCategory: Boolean = false
    ): Result<Long> {
        return ErrorHandler.runSuspendCatching("Save category") {
            // Get next sort order
            val sortOrder = if (parentId != null) {
                customCategoryDao.getMaxSortOrderForParent(parentId) + 1
            } else {
                customCategoryDao.getMaxSortOrder() + 1
            }

            val timestamp = System.currentTimeMillis()
            val entity = category.toEntity(
                parentId = parentId,
                sortOrder = sortOrder,
                createdAt = timestamp,
                updatedAt = timestamp
            )

            val id = customCategoryDao.insert(entity)
            invalidateCache()
            id
        }
    }

    /**
     * Updates an existing category in the database.
     * Only updates the category fields (name, icon, color, keywords).
     * Does not modify structural fields (parentId, isSystemCategory, sortOrder).
     * Invalidates cache after successful update to ensure data consistency.
     *
     * @param categoryId The ID of the category to update
     * @param category The updated category data
     * @return Result<Boolean> indicating if the category was updated or error information
     */
    suspend fun updateCategory(categoryId: Long, category: Category): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Update category") {
            // Fetch existing entity to preserve structural fields
            val existing = customCategoryDao.getCategoryById(categoryId)
                ?: return@runSuspendCatching false

            // Convert category to entity but preserve structural fields from existing
            val updated = category.toEntity(
                parentId = existing.parentId,
                sortOrder = existing.sortOrder,
                createdAt = existing.createdAt,
                updatedAt = System.currentTimeMillis()
            ).copy(id = categoryId) // Preserve the ID

            val rowsUpdated = customCategoryDao.update(updated)
            if (rowsUpdated > 0) {
                invalidateCache()
            }
            rowsUpdated > 0
        }
    }

    /**
     * Deletes a category from the database by its ID.
     * Due to CASCADE foreign key, all subcategories will also be deleted.
     * Invalidates cache after successful deletion to ensure data consistency.
     *
     * @param categoryId The ID of the category to delete
     * @return Result<Boolean> indicating if the category was deleted or error information
     */
    suspend fun deleteCategory(categoryId: Long): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Delete category") {
            val rowsDeleted = customCategoryDao.deleteById(categoryId)
            if (rowsDeleted > 0) {
                invalidateCache()
            }
            rowsDeleted > 0
        }
    }

    /**
     * Retrieves all categories as a reactive Flow.
     * Automatically converts entities to domain models.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @return Flow emitting list of categories sorted by sortOrder and name
     */
    fun getAllCategories(): Flow<List<Category>> {
        return customCategoryDao.getAllCategories()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get all categories")
                emit(emptyList())
            }
    }

    /**
     * Retrieves all categories as a one-time snapshot.
     * Useful for non-reactive operations.
     * Uses in-memory cache to reduce database reads - falls back to database on cache miss.
     *
     * @return Result<List<Category>> containing categories or error information
     */
    suspend fun getAllCategoriesSnapshot(): Result<List<Category>> {
        return ErrorHandler.runSuspendCatching("Get all categories snapshot") {
            // Check cache first
            val cached = cacheMutex.withLock { categoriesCache }
            if (cached != null) {
                ErrorHandler.logDebug("Cache hit: getAllCategoriesSnapshot", "CategoryRepository")
                return@runSuspendCatching cached
            }

            // Cache miss - fetch from database
            ErrorHandler.logDebug("Cache miss: getAllCategoriesSnapshot", "CategoryRepository")
            val categories = customCategoryDao.getAllCategoriesSnapshot()
                .map { it.toDomain() }

            // Update cache with fresh data
            updateCategoriesCache(categories)

            categories
        }
    }

    /**
     * Retrieves all root-level categories (categories without a parent).
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of root categories
     */
    fun getRootCategories(): Flow<List<Category>> {
        return customCategoryDao.getRootCategories()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get root categories")
                emit(emptyList())
            }
    }

    /**
     * Retrieves all root-level categories as a one-time snapshot.
     *
     * @return Result<List<Category>> containing root categories or error information
     */
    suspend fun getRootCategoriesSnapshot(): Result<List<Category>> {
        return ErrorHandler.runSuspendCatching("Get root categories snapshot") {
            customCategoryDao.getRootCategoriesSnapshot()
                .map { it.toDomain() }
        }
    }

    /**
     * Retrieves all subcategories for a given parent category ID.
     * Returns a Flow for reactive updates.
     *
     * @param parentId The ID of the parent category
     * @return Flow emitting list of subcategories sorted by sortOrder and name
     */
    fun getSubcategories(parentId: Long): Flow<List<Category>> {
        return customCategoryDao.getSubcategories(parentId)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get subcategories for parent $parentId")
                emit(emptyList())
            }
    }

    /**
     * Retrieves all subcategories for a given parent category ID as a one-time snapshot.
     *
     * @param parentId The ID of the parent category
     * @return Result<List<Category>> containing subcategories or error information
     */
    suspend fun getSubcategoriesSnapshot(parentId: Long): Result<List<Category>> {
        return ErrorHandler.runSuspendCatching("Get subcategories snapshot") {
            customCategoryDao.getSubcategoriesSnapshot(parentId)
                .map { it.toDomain() }
        }
    }

    /**
     * Retrieves a specific category by its ID as a one-time snapshot.
     *
     * @param categoryId The ID of the category to retrieve
     * @return Result<Category?> containing the category or null if not found, or error information
     */
    suspend fun getCategoryById(categoryId: Long): Result<Category?> {
        return ErrorHandler.runSuspendCatching("Get category by ID") {
            customCategoryDao.getCategoryById(categoryId)?.toDomain()
        }
    }

    /**
     * Retrieves a specific category by its ID.
     * Returns a Flow for reactive updates.
     *
     * @param categoryId The ID of the category to retrieve
     * @return Flow emitting the category or null if not found
     */
    fun getCategoryByIdFlow(categoryId: Long): Flow<Category?> {
        return customCategoryDao.getCategoryByIdFlow(categoryId)
            .map { entity -> entity?.toDomain() }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get category by ID flow: $categoryId")
                emit(null)
            }
    }

    /**
     * Searches for categories by name (case-insensitive partial match).
     * Returns a Flow for reactive updates.
     *
     * @param searchQuery The search query to match against category names
     * @return Flow emitting list of matching categories
     */
    fun searchByName(searchQuery: String): Flow<List<Category>> {
        return customCategoryDao.searchByName(searchQuery)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Search categories by name: $searchQuery")
                emit(emptyList())
            }
    }

    /**
     * Checks if the category database is empty (no categories exist).
     * Useful for determining if default categories need to be seeded.
     *
     * @return Result<Boolean> indicating if database is empty or error information
     */
    suspend fun isCategoryDatabaseEmpty(): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Check if category database is empty") {
            customCategoryDao.getCategoryCount() == 0
        }
    }

    /**
     * Gets the total count of categories in the database.
     * Uses in-memory cache to reduce database reads - falls back to database on cache miss.
     *
     * @return Result<Int> containing total number of categories or error information
     */
    suspend fun getCategoryCount(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Get category count") {
            // Check cache first
            val cached = cacheMutex.withLock { categoryCountCache }
            if (cached != null) {
                ErrorHandler.logDebug("Cache hit: getCategoryCount", "CategoryRepository")
                return@runSuspendCatching cached
            }

            // Cache miss - fetch from database
            ErrorHandler.logDebug("Cache miss: getCategoryCount", "CategoryRepository")
            val count = customCategoryDao.getCategoryCount()

            // Update cache
            cacheMutex.withLock { categoryCountCache = count }

            count
        }
    }

    /**
     * Seeds the database with default system categories from DefaultCategories.
     * Should only be called once on first app launch when the database is empty.
     * Marks all seeded categories with isSystemCategory = true.
     * Invalidates cache after successful seeding to ensure data consistency.
     *
     * This method:
     * 1. Checks if categories already exist (safety check)
     * 2. Converts default categories to entities
     * 3. Inserts all default categories in a single transaction
     * 4. Invalidates cache to reflect new data
     *
     * @return Result<Int> containing number of categories seeded or error information
     */
    suspend fun seedDefaultCategories(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Seed default categories") {
            // Safety check - don't seed if categories already exist
            val count = customCategoryDao.getCategoryCount()
            if (count > 0) {
                ErrorHandler.logDebug(
                    "Categories already exist ($count), skipping seed",
                    "CategoryRepository"
                )
                return@runSuspendCatching 0
            }

            // Convert default categories to entities
            val timestamp = System.currentTimeMillis()
            val entities = DefaultCategories.ALL.mapIndexed { index, category ->
                category.toEntity(
                    parentId = null, // All default categories are root-level
                    sortOrder = index, // Preserve original order
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
            }

            // Insert all default categories
            val ids = customCategoryDao.insertAll(entities)
            invalidateCache()

            ErrorHandler.logDebug(
                "Seeded ${ids.size} default categories",
                "CategoryRepository"
            )

            ids.size
        }
    }

    /**
     * Deletes all user-created categories, preserving system (default) categories.
     * Invalidates cache after successful deletion to ensure data consistency.
     *
     * @return Result<Int> containing number of categories deleted or error information
     */
    suspend fun deleteAllCustomCategories(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Delete all custom categories") {
            val rowsDeleted = customCategoryDao.deleteAllCustomCategories()
            if (rowsDeleted > 0) {
                invalidateCache()
            }
            rowsDeleted
        }
    }
}
