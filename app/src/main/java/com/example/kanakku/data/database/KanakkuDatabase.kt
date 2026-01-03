package com.example.kanakku.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kanakku.data.database.dao.BudgetDao
import com.example.kanakku.data.database.dao.CategoryOverrideDao
import com.example.kanakku.data.database.dao.SyncMetadataDao
import com.example.kanakku.data.database.dao.TransactionDao
import com.example.kanakku.data.database.entity.BudgetEntity
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
 * - Monthly budgets (overall and per-category)
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryOverrideEntity::class,
        SyncMetadataEntity::class,
        BudgetEntity::class
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
     * Provides access to budget data operations.
     * @return BudgetDao instance for managing monthly budgets
     */
    abstract fun budgetDao(): BudgetDao

    companion object {
        /**
         * Migration from database version 1 to version 2.
         * Adds the budgets table for monthly budget tracking.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create budgets table with proper schema
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `categoryId` TEXT,
                        `amount` REAL NOT NULL,
                        `month` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // Create index on month and year for efficient queries
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_budgets_month_year`
                    ON `budgets` (`month`, `year`)
                    """.trimIndent()
                )

                // Create unique index on categoryId, month, and year to prevent duplicate budgets
                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_categoryId_month_year`
                    ON `budgets` (`categoryId`, `month`, `year`)
                    """.trimIndent()
                )
            }
        }
    }
}
