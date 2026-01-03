package com.example.kanakku.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kanakku.data.database.entity.CustomCategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for custom category operations.
 * Provides CRUD operations for persisting and querying user-defined categories
 * with support for hierarchical subcategories.
 */
@Dao
interface CustomCategoryDao {

    /**
     * Inserts a single custom category into the database.
     * If a category with the same ID exists, it will be replaced.
     *
     * @param category The category entity to insert
     * @return The row ID of the inserted category
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CustomCategoryEntity): Long

    /**
     * Inserts multiple custom categories into the database in a single transaction.
     * If categories with the same ID exist, they will be replaced.
     *
     * @param categories List of category entities to insert
     * @return List of row IDs for the inserted categories
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CustomCategoryEntity>): List<Long>

    /**
     * Updates an existing custom category.
     * The category must have a valid ID.
     *
     * @param category The category entity to update
     * @return Number of rows updated (0 if not found, 1 if updated)
     */
    @Update
    suspend fun update(category: CustomCategoryEntity): Int

    /**
     * Deletes a specific custom category from the database.
     * Due to CASCADE foreign key, all subcategories will also be deleted.
     *
     * @param category The category entity to delete
     * @return Number of rows deleted
     */
    @Delete
    suspend fun delete(category: CustomCategoryEntity): Int

    /**
     * Deletes a category by its ID.
     * Due to CASCADE foreign key, all subcategories will also be deleted.
     *
     * @param categoryId The ID of the category to delete
     * @return Number of rows deleted (0 if not found, 1+ if deleted with subcategories)
     */
    @Query("DELETE FROM custom_categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long): Int

    /**
     * Retrieves all custom categories sorted by sortOrder and name.
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of all categories
     */
    @Query("SELECT * FROM custom_categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CustomCategoryEntity>>

    /**
     * Retrieves all custom categories as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return List of all categories
     */
    @Query("SELECT * FROM custom_categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllCategoriesSnapshot(): List<CustomCategoryEntity>

    /**
     * Retrieves a specific category by its ID.
     *
     * @param categoryId The ID of the category to retrieve
     * @return The category entity, or null if not found
     */
    @Query("SELECT * FROM custom_categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): CustomCategoryEntity?

    /**
     * Retrieves a specific category by its ID.
     * Returns a Flow for reactive updates.
     *
     * @param categoryId The ID of the category to retrieve
     * @return Flow emitting the category entity, or null if not found
     */
    @Query("SELECT * FROM custom_categories WHERE id = :categoryId")
    fun getCategoryByIdFlow(categoryId: Long): Flow<CustomCategoryEntity?>

    /**
     * Retrieves all root-level categories (categories without a parent).
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of root categories
     */
    @Query("SELECT * FROM custom_categories WHERE parentId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun getRootCategories(): Flow<List<CustomCategoryEntity>>

    /**
     * Retrieves all root-level categories as a one-time snapshot.
     *
     * @return List of root categories
     */
    @Query("SELECT * FROM custom_categories WHERE parentId IS NULL ORDER BY sortOrder ASC, name ASC")
    suspend fun getRootCategoriesSnapshot(): List<CustomCategoryEntity>

    /**
     * Retrieves all subcategories for a given parent category ID.
     * Returns a Flow for reactive updates.
     *
     * @param parentId The ID of the parent category
     * @return Flow emitting list of subcategories
     */
    @Query("SELECT * FROM custom_categories WHERE parentId = :parentId ORDER BY sortOrder ASC, name ASC")
    fun getSubcategories(parentId: Long): Flow<List<CustomCategoryEntity>>

    /**
     * Retrieves all subcategories for a given parent category ID as a one-time snapshot.
     *
     * @param parentId The ID of the parent category
     * @return List of subcategories
     */
    @Query("SELECT * FROM custom_categories WHERE parentId = :parentId ORDER BY sortOrder ASC, name ASC")
    suspend fun getSubcategoriesSnapshot(parentId: Long): List<CustomCategoryEntity>

    /**
     * Searches for categories by name (case-insensitive partial match).
     * Returns a Flow for reactive updates.
     *
     * @param searchQuery The search query to match against category names
     * @return Flow emitting list of matching categories
     */
    @Query("SELECT * FROM custom_categories WHERE name LIKE '%' || :searchQuery || '%' ORDER BY sortOrder ASC, name ASC")
    fun searchByName(searchQuery: String): Flow<List<CustomCategoryEntity>>

    /**
     * Searches for categories by name (case-insensitive partial match) as a one-time snapshot.
     *
     * @param searchQuery The search query to match against category names
     * @return List of matching categories
     */
    @Query("SELECT * FROM custom_categories WHERE name LIKE '%' || :searchQuery || '%' ORDER BY sortOrder ASC, name ASC")
    suspend fun searchByNameSnapshot(searchQuery: String): List<CustomCategoryEntity>

    /**
     * Retrieves all system (default) categories.
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of system categories
     */
    @Query("SELECT * FROM custom_categories WHERE isSystemCategory = 1 ORDER BY sortOrder ASC, name ASC")
    fun getSystemCategories(): Flow<List<CustomCategoryEntity>>

    /**
     * Retrieves all system (default) categories as a one-time snapshot.
     *
     * @return List of system categories
     */
    @Query("SELECT * FROM custom_categories WHERE isSystemCategory = 1 ORDER BY sortOrder ASC, name ASC")
    suspend fun getSystemCategoriesSnapshot(): List<CustomCategoryEntity>

    /**
     * Retrieves all user-created (custom) categories.
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of custom categories
     */
    @Query("SELECT * FROM custom_categories WHERE isSystemCategory = 0 ORDER BY sortOrder ASC, name ASC")
    fun getCustomCategories(): Flow<List<CustomCategoryEntity>>

    /**
     * Retrieves all user-created (custom) categories as a one-time snapshot.
     *
     * @return List of custom categories
     */
    @Query("SELECT * FROM custom_categories WHERE isSystemCategory = 0 ORDER BY sortOrder ASC, name ASC")
    suspend fun getCustomCategoriesSnapshot(): List<CustomCategoryEntity>

    /**
     * Checks if a category with the given ID exists.
     *
     * @param categoryId The category ID to check
     * @return True if category exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM custom_categories WHERE id = :categoryId LIMIT 1)")
    suspend fun exists(categoryId: Long): Boolean

    /**
     * Checks if a category with the given name exists (case-insensitive).
     *
     * @param name The category name to check
     * @return True if category exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM custom_categories WHERE LOWER(name) = LOWER(:name) LIMIT 1)")
    suspend fun existsByName(name: String): Boolean

    /**
     * Gets the total count of categories in the database.
     *
     * @return Total number of categories
     */
    @Query("SELECT COUNT(*) FROM custom_categories")
    suspend fun getCategoryCount(): Int

    /**
     * Gets the count of subcategories for a given parent category.
     *
     * @param parentId The ID of the parent category
     * @return Number of subcategories
     */
    @Query("SELECT COUNT(*) FROM custom_categories WHERE parentId = :parentId")
    suspend fun getSubcategoryCount(parentId: Long): Int

    /**
     * Deletes all categories from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM custom_categories")
    suspend fun deleteAll(): Int

    /**
     * Deletes all user-created categories, preserving system categories.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM custom_categories WHERE isSystemCategory = 0")
    suspend fun deleteAllCustomCategories(): Int

    /**
     * Gets the highest sort order value in the database.
     * Useful for adding new categories at the end.
     *
     * @return The maximum sort order, or 0 if no categories exist
     */
    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM custom_categories")
    suspend fun getMaxSortOrder(): Int

    /**
     * Gets the highest sort order value for subcategories of a given parent.
     * Useful for adding new subcategories at the end.
     *
     * @param parentId The ID of the parent category
     * @return The maximum sort order for subcategories, or 0 if none exist
     */
    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM custom_categories WHERE parentId = :parentId")
    suspend fun getMaxSortOrderForParent(parentId: Long): Int
}
