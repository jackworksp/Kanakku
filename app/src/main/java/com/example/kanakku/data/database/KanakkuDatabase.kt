package com.example.kanakku.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.kanakku.data.database.dao.CategoryOverrideDao
import com.example.kanakku.data.database.dao.SyncMetadataDao
import com.example.kanakku.data.database.dao.TransactionDao
import com.example.kanakku.data.database.dao.UnreportedSmsDao
import com.example.kanakku.data.database.entity.CategoryOverrideEntity
import com.example.kanakku.data.database.entity.SyncMetadataEntity
import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.database.entity.UnreportedSmsEntity

/**
 * Main Room database for the Kanakku application.
 * Manages all entities and provides access to DAOs for database operations.
 *
 * This database stores:
 * - Parsed transactions from SMS messages
 * - User's manual category assignments
 * - Synchronization metadata for incremental updates
 * - User-reported undetected SMS messages for pattern improvement
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryOverrideEntity::class,
        SyncMetadataEntity::class,
        UnreportedSmsEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KanakkuDatabase : RoomDatabase() {

    /**
     * Provides access to transaction data operations.
     * @return TransactionDao instance for CRUD operations on transactions
     */
    abstract fun transactionDao(): TransactionDao

    /**
     * Provides access to category override operations.
     * @return CategoryOverrideDao instance for managing manual category assignments
     */
    abstract fun categoryOverrideDao(): CategoryOverrideDao

    /**
     * Provides access to sync metadata operations.
     * @return SyncMetadataDao instance for tracking synchronization state
     */
    abstract fun syncMetadataDao(): SyncMetadataDao

    /**
     * Provides access to unreported SMS operations.
     * @return UnreportedSmsDao instance for managing user-reported undetected SMS
     */
    abstract fun unreportedSmsDao(): UnreportedSmsDao
}
