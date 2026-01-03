package com.example.kanakku.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kanakku.data.database.entity.MerchantCategoryMappingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for merchant category mapping operations.
 * Provides operations for persisting and querying learned merchant-to-category mappings
 * used for automatic categorization of transactions.
 */
@Dao
interface MerchantCategoryMappingDao {

    /**
     * Inserts or updates a merchant category mapping.
     * If a mapping for the same merchant name exists, it will be replaced.
     *
     * @param mapping The merchant category mapping entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: MerchantCategoryMappingEntity)

    /**
     * Inserts multiple merchant category mappings in a single transaction.
     * If mappings with the same merchant names exist, they will be replaced.
     *
     * @param mappings List of merchant category mapping entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mappings: List<MerchantCategoryMappingEntity>)

    /**
     * Retrieves the category mapping for a specific merchant.
     *
     * @param merchantName The normalized merchant name
     * @return The merchant category mapping entity, or null if no mapping exists
     */
    @Query("SELECT * FROM merchant_category_mappings WHERE merchantName = :merchantName")
    suspend fun getMapping(merchantName: String): MerchantCategoryMappingEntity?

    /**
     * Retrieves all merchant category mappings.
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of all merchant category mappings
     */
    @Query("SELECT * FROM merchant_category_mappings")
    fun getAllMappings(): Flow<List<MerchantCategoryMappingEntity>>

    /**
     * Retrieves all merchant category mappings as a one-time snapshot.
     * Useful for creating an in-memory map.
     *
     * @return List of all merchant category mappings
     */
    @Query("SELECT * FROM merchant_category_mappings")
    suspend fun getAllMappingsSnapshot(): List<MerchantCategoryMappingEntity>

    /**
     * Deletes the merchant category mapping for a specific merchant.
     *
     * @param merchantName The normalized merchant name
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM merchant_category_mappings WHERE merchantName = :merchantName")
    suspend fun deleteMapping(merchantName: String): Int

    /**
     * Deletes all merchant category mappings from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM merchant_category_mappings")
    suspend fun deleteAll(): Int

    /**
     * Checks if a merchant category mapping exists for the given merchant.
     *
     * @param merchantName The normalized merchant name to check
     * @return True if mapping exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM merchant_category_mappings WHERE merchantName = :merchantName LIMIT 1)")
    suspend fun exists(merchantName: String): Boolean

    /**
     * Gets the total count of merchant category mappings in the database.
     *
     * @return Total number of mappings
     */
    @Query("SELECT COUNT(*) FROM merchant_category_mappings")
    suspend fun getMappingCount(): Int
}
