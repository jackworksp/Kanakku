package com.example.kanakku.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kanakku.data.database.dao.BudgetDao
import com.example.kanakku.data.database.dao.CategoryOverrideDao
import com.example.kanakku.data.database.dao.MerchantCategoryMappingDao
import com.example.kanakku.data.database.dao.SyncMetadataDao
import com.example.kanakku.data.database.dao.TransactionDao
import com.example.kanakku.data.database.entity.BudgetEntity
import com.example.kanakku.data.database.entity.CategoryOverrideEntity
import com.example.kanakku.data.database.entity.MerchantCategoryMappingEntity
import com.example.kanakku.data.database.entity.SyncMetadataEntity
import com.example.kanakku.data.database.entity.TransactionEntity

/**
 * Main Room database for the Kanakku application.
 * Manages all entities and provides access to DAOs for database operations.
 *
 * This database stores:
 * - Parsed transactions from SMS messages
 * - User's manual category assignments
 * - Learned merchant-to-category mappings
 * - Synchronization metadata for incremental updates
 * - Monthly budgets (overall and per-category)
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryOverrideEntity::class,
        SyncMetadataEntity::class,
        MerchantCategoryMappingEntity::class
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
     * Provides access to merchant category mapping operations.
     * @return MerchantCategoryMappingDao instance for managing learned merchant-to-category mappings
     */
    abstract fun merchantCategoryMappingDao(): MerchantCategoryMappingDao
}
