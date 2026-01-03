package com.example.kanakku.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kanakku.data.database.dao.CategoryOverrideDao
import com.example.kanakku.data.database.dao.RecurringTransactionDao
import com.example.kanakku.data.database.dao.SyncMetadataDao
import com.example.kanakku.data.database.dao.TransactionDao
import com.example.kanakku.data.database.entity.CategoryOverrideEntity
import com.example.kanakku.data.database.entity.RecurringTransactionEntity
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
 * - Recurring transaction patterns (subscriptions, EMIs, salaries, etc.)
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryOverrideEntity::class,
        SyncMetadataEntity::class,
        RecurringTransactionEntity::class
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
     * Provides access to recurring transaction operations.
     * @return RecurringTransactionDao instance for managing recurring transaction patterns
     */
    abstract fun recurringTransactionDao(): RecurringTransactionDao
}

/**
 * Migration from database version 1 to version 2.
 * Adds the recurring_transactions table for storing detected recurring transaction patterns.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create recurring_transactions table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recurring_transactions` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `merchantPattern` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `frequency` TEXT NOT NULL,
                `averageInterval` INTEGER NOT NULL,
                `lastOccurrence` INTEGER NOT NULL,
                `nextExpected` INTEGER NOT NULL,
                `transactionIds` TEXT NOT NULL,
                `isUserConfirmed` INTEGER NOT NULL,
                `type` TEXT NOT NULL
            )
            """.trimIndent()
        )

        // Create index on merchantPattern for optimized pattern matching queries
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_recurring_transactions_merchantPattern`
            ON `recurring_transactions` (`merchantPattern`)
            """.trimIndent()
        )

        // Create index on nextExpected for optimized upcoming transaction queries
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_recurring_transactions_nextExpected`
            ON `recurring_transactions` (`nextExpected`)
            """.trimIndent()
        )
    }
}
