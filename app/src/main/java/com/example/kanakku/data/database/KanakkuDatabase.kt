package com.example.kanakku.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kanakku.data.database.dao.CategoryOverrideDao
import com.example.kanakku.data.database.dao.CustomCategoryDao
import com.example.kanakku.data.database.dao.SyncMetadataDao
import com.example.kanakku.data.database.dao.TransactionDao
import com.example.kanakku.data.database.entity.CategoryOverrideEntity
import com.example.kanakku.data.database.entity.CustomCategoryEntity
import com.example.kanakku.data.database.entity.SyncMetadataEntity
import com.example.kanakku.data.database.entity.TransactionEntity

/**
 * Main Room database for the Kanakku application.
 * Manages all entities and provides access to DAOs for database operations.
 *
 * This database stores:
 * - Parsed transactions from SMS messages
 * - User's manual category assignments
 * - Custom user-defined categories with subcategory support
 * - Synchronization metadata for incremental updates
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryOverrideEntity::class,
        SyncMetadataEntity::class,
        CustomCategoryEntity::class
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
     * Provides access to custom category operations.
     * @return CustomCategoryDao instance for managing user-defined categories
     */
    abstract fun customCategoryDao(): CustomCategoryDao
}

/**
 * Migration from database version 1 to version 2.
 * Adds custom_categories table for user-defined categories with subcategory support.
 *
 * Changes:
 * - Creates custom_categories table with hierarchical structure
 * - Adds indices for performance optimization
 * - Sets up foreign key constraint for parent-child relationships
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create custom_categories table with all fields
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS custom_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                icon TEXT NOT NULL,
                colorHex TEXT NOT NULL,
                keywords TEXT NOT NULL,
                parentId INTEGER,
                isSystemCategory INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(parentId) REFERENCES custom_categories(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Create index on name column for faster searches
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_custom_categories_name ON custom_categories(name)"
        )

        // Create index on parentId column for efficient subcategory queries
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_custom_categories_parentId ON custom_categories(parentId)"
        )

        // Create index on isSystemCategory column for filtering system vs custom categories
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_custom_categories_isSystemCategory ON custom_categories(isSystemCategory)"
        )

        // Create index on sortOrder column for efficient ordering
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_custom_categories_sortOrder ON custom_categories(sortOrder)"
        )
    }
}
