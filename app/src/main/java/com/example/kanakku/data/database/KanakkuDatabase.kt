package com.example.kanakku.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kanakku.data.database.dao.CategoryOverrideDao
import com.example.kanakku.data.database.dao.SyncMetadataDao
import com.example.kanakku.data.database.dao.TransactionDao
import com.example.kanakku.data.database.entity.CategoryOverrideEntity
import com.example.kanakku.data.database.entity.SyncMetadataEntity
import com.example.kanakku.data.database.entity.TransactionEntity

/**
 * Main Room database for the Kanakku application.
 * Manages all entities and provides access to DAOs for database operations.
 *
 * This database stores:
 * - Parsed transactions from SMS messages
 * - User's manual category assignments
 * - Synchronization metadata for incremental updates
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryOverrideEntity::class,
        SyncMetadataEntity::class
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

    companion object {
        /**
         * Migration from version 1 to 2: Add UPI-specific fields to transactions table.
         *
         * Adds two new nullable columns to support UPI transaction data:
         * - upiId: Stores UPI VPA (Virtual Payment Address) like user@paytm
         * - paymentMethod: Stores payment method (e.g., "UPI", "Card", "Net Banking")
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE transactions ADD COLUMN upiId TEXT DEFAULT NULL"
                )
                database.execSQL(
                    "ALTER TABLE transactions ADD COLUMN paymentMethod TEXT DEFAULT NULL"
                )
            }
        }
    }
}
