package com.example.kanakku.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kanakku.data.database.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for sync metadata operations.
 * Provides operations for persisting and querying synchronization metadata in key-value format.
 */
@Dao
interface SyncMetadataDao {

    /**
     * Inserts or updates a metadata entry.
     * If an entry with the same key exists, it will be replaced.
     *
     * @param metadata The sync metadata entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SyncMetadataEntity)

    /**
     * Inserts multiple metadata entries in a single transaction.
     * If entries with the same keys exist, they will be replaced.
     *
     * @param metadataList List of sync metadata entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metadataList: List<SyncMetadataEntity>)

    /**
     * Retrieves the value for a specific metadata key.
     *
     * @param key The metadata key to retrieve
     * @return The metadata value, or null if key doesn't exist
     */
    @Query("SELECT value FROM sync_metadata WHERE key = :key")
    suspend fun getValue(key: String): String?

    /**
     * Retrieves the complete metadata entry for a specific key.
     *
     * @param key The metadata key to retrieve
     * @return The sync metadata entity, or null if key doesn't exist
     */
    @Query("SELECT * FROM sync_metadata WHERE key = :key")
    suspend fun getMetadata(key: String): SyncMetadataEntity?

    /**
     * Retrieves all metadata entries.
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of all sync metadata entries
     */
    @Query("SELECT * FROM sync_metadata")
    fun getAllMetadata(): Flow<List<SyncMetadataEntity>>

    /**
     * Retrieves all metadata entries as a one-time snapshot.
     * Useful for creating an in-memory map.
     *
     * @return List of all sync metadata entries
     */
    @Query("SELECT * FROM sync_metadata")
    suspend fun getAllMetadataSnapshot(): List<SyncMetadataEntity>

    /**
     * Deletes a metadata entry by key.
     *
     * @param key The key of the metadata entry to delete
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM sync_metadata WHERE key = :key")
    suspend fun delete(key: String): Int

    /**
     * Deletes all metadata entries from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM sync_metadata")
    suspend fun deleteAll(): Int

    /**
     * Checks if a metadata entry exists for the given key.
     *
     * @param key The key to check
     * @return True if entry exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM sync_metadata WHERE key = :key LIMIT 1)")
    suspend fun exists(key: String): Boolean

    /**
     * Gets the total count of metadata entries in the database.
     *
     * @return Total number of metadata entries
     */
    @Query("SELECT COUNT(*) FROM sync_metadata")
    suspend fun getMetadataCount(): Int
}
