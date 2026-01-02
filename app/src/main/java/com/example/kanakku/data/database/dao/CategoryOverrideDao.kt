package com.example.kanakku.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kanakku.data.database.entity.CategoryOverrideEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for category override operations.
 * Provides operations for persisting and querying user's manual category assignments.
 */
@Dao
interface CategoryOverrideDao {

    /**
     * Inserts or updates a category override for a transaction.
     * If an override for the same smsId exists, it will be replaced.
     *
     * @param override The category override entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(override: CategoryOverrideEntity)

    /**
     * Inserts multiple category overrides in a single transaction.
     * If overrides with the same smsId exist, they will be replaced.
     *
     * @param overrides List of category override entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(overrides: List<CategoryOverrideEntity>)

    /**
     * Retrieves the category override for a specific transaction.
     *
     * @param smsId The SMS ID of the transaction
     * @return The category override entity, or null if no override exists
     */
    @Query("SELECT * FROM category_overrides WHERE smsId = :smsId")
    suspend fun getOverride(smsId: Long): CategoryOverrideEntity?

    /**
     * Retrieves all category overrides.
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of all category overrides
     */
    @Query("SELECT * FROM category_overrides")
    fun getAllOverrides(): Flow<List<CategoryOverrideEntity>>

    /**
     * Retrieves all category overrides as a one-time snapshot.
     * Useful for creating an in-memory map.
     *
     * @return List of all category overrides
     */
    @Query("SELECT * FROM category_overrides")
    suspend fun getAllOverridesSnapshot(): List<CategoryOverrideEntity>

    /**
     * Deletes the category override for a specific transaction.
     *
     * @param smsId The SMS ID of the transaction
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM category_overrides WHERE smsId = :smsId")
    suspend fun deleteOverride(smsId: Long): Int

    /**
     * Deletes all category overrides from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM category_overrides")
    suspend fun deleteAll(): Int

    /**
     * Checks if a category override exists for the given SMS ID.
     *
     * @param smsId The SMS ID to check
     * @return True if override exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM category_overrides WHERE smsId = :smsId LIMIT 1)")
    suspend fun exists(smsId: Long): Boolean

    /**
     * Gets the total count of category overrides in the database.
     *
     * @return Total number of overrides
     */
    @Query("SELECT COUNT(*) FROM category_overrides")
    suspend fun getOverrideCount(): Int
}
