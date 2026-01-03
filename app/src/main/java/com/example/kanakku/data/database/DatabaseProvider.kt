package com.example.kanakku.data.database

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kanakku.data.repository.TransactionRepository
import com.example.kanakku.data.sms.SmsDataSource
import java.io.File

/**
 * Singleton provider for database instance and repository.
 *
 * @deprecated This singleton pattern is deprecated in favor of Hilt dependency injection.
 * Use Hilt to inject KanakkuDatabase and TransactionRepository instead.
 *
 * **Migration Guide:**
 *
 * Old approach (DatabaseProvider):
 * ```kotlin
 * val repository = DatabaseProvider.getRepository(context)
 * val database = DatabaseProvider.getDatabase(context)
 * ```
 *
 * New approach (Hilt injection):
 * ```kotlin
 * // In ViewModel:
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val repository: TransactionRepository,
 *     private val database: KanakkuDatabase
 * ) : ViewModel() {
 *     // Use repository and database directly
 * }
 *
 * // In Activity/Fragment:
 * @AndroidEntryPoint
 * class MyActivity : ComponentActivity() {
 *     @Inject lateinit var repository: TransactionRepository
 *     @Inject lateinit var database: KanakkuDatabase
 * }
 * ```
 *
 * **Benefits of Hilt:**
 * - Better testability with easy mock injection
 * - Compile-time dependency validation
 * - Automatic lifecycle management
 * - No manual context passing required
 * - Follows Android best practices
 *
 * **See Also:**
 * - [DatabaseModule] - Provides database dependencies via Hilt
 * - [RepositoryModule] - Provides repository dependencies via Hilt
 * - [AppModule] - Provides other app-level dependencies via Hilt
 *
 * This class is maintained for backward compatibility but should not be used in new code.
 * All new features should use Hilt dependency injection.
 *
 * @see com.example.kanakku.di.DatabaseModule
 * @see com.example.kanakku.di.RepositoryModule
 */
@Deprecated(
    message = "Use Hilt dependency injection instead. Inject KanakkuDatabase or TransactionRepository via constructor.",
    replaceWith = ReplaceWith(
        "Inject dependencies via Hilt. See DatabaseModule and RepositoryModule.",
        "com.example.kanakku.di.DatabaseModule",
        "com.example.kanakku.di.RepositoryModule"
    ),
    level = DeprecationLevel.WARNING
)
object DatabaseProvider {

    private const val TAG = "DatabaseProvider"
    private const val DATABASE_NAME = "kanakku_database"

    @Volatile
    private var database: KanakkuDatabase? = null

    @Volatile
    private var smsDataSource: SmsDataSource? = null

    @Volatile
    private var repository: TransactionRepository? = null

    @Volatile
    private var budgetRepository: BudgetRepository? = null

    /**
     * Migration from database version 1 to version 2.
     * Adds the merchant_category_mappings table for storing learned merchant-to-category mappings.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Log.i(TAG, "Migrating database from version 1 to version 2")

            // Create merchant_category_mappings table
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS merchant_category_mappings (
                    merchantName TEXT PRIMARY KEY NOT NULL,
                    categoryId TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            Log.i(TAG, "Migration 1->2 completed successfully")
        }
    }

    /**
     * Migration from database version 1 to version 2.
     * Adds support for manual transaction entry by adding:
     * - source column (SMS or MANUAL) with default value 'SMS' for existing records
     * - notes column (nullable text) for transaction notes
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Log.d(TAG, "Migrating database from version 1 to 2")

            // Add source column with default value 'SMS' for existing transactions
            database.execSQL(
                "ALTER TABLE transactions ADD COLUMN source TEXT NOT NULL DEFAULT 'SMS'"
            )

            // Add notes column (nullable) for optional transaction notes
            database.execSQL(
                "ALTER TABLE transactions ADD COLUMN notes TEXT"
            )

            Log.i(TAG, "Database migration 1→2 completed successfully")
        }
    }

    /**
     * Gets the singleton database instance.
     * Creates the database on first access using the provided context.
     *
     * @deprecated Use Hilt to inject KanakkuDatabase instead.
     * See [DatabaseModule.provideKanakkuDatabase] for the recommended approach.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     * @return The KanakkuDatabase instance
     * @throws DatabaseInitializationException if database cannot be created after recovery attempts
     * @see com.example.kanakku.di.DatabaseModule.provideKanakkuDatabase
     */
    @Deprecated(
        message = "Use Hilt to inject KanakkuDatabase. Annotate your class with @AndroidEntryPoint and inject the database.",
        replaceWith = ReplaceWith("Injected KanakkuDatabase via Hilt constructor or field injection"),
        level = DeprecationLevel.WARNING
    )
    fun getDatabase(context: Context): KanakkuDatabase {
        return database ?: synchronized(this) {
            database ?: buildDatabase(context.applicationContext).also {
                database = it
                Log.i(TAG, "Database initialized successfully")
            }
        }
    }

    /**
     * Gets the singleton repository instance.
     * Creates database, SMS data source, and repository on first access.
     *
     * @deprecated Use Hilt to inject TransactionRepository instead.
     * See [RepositoryModule.provideTransactionRepository] for the recommended approach.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     * @return The TransactionRepository instance
     * @throws DatabaseInitializationException if database cannot be created after recovery attempts
     * @see com.example.kanakku.di.RepositoryModule.provideTransactionRepository
     */
    @Deprecated(
        message = "Use Hilt to inject TransactionRepository. Annotate your class with @AndroidEntryPoint and inject the repository.",
        replaceWith = ReplaceWith("Injected TransactionRepository via Hilt constructor or field injection"),
        level = DeprecationLevel.WARNING
    )
    fun getRepository(context: Context): TransactionRepository {
        return repository ?: synchronized(this) {
            repository ?: createRepository(context.applicationContext).also {
                repository = it
                Log.i(TAG, "Repository initialized successfully")
            }
        }
    }

    /**
     * Creates a new repository instance with all required dependencies.
     * This factory method ensures proper dependency injection and initialization.
     *
     * @param context Application context
     * @return TransactionRepository with database and SMS data source dependencies
     */
    private fun createRepository(context: Context): TransactionRepository {
        val db = getDatabase(context)
        val smsSource = getSmsDataSource(context)
        return TransactionRepository(db, smsSource)
    }

    /**
     * Gets the singleton SMS data source instance.
     * Creates the data source on first access using the provided context.
     *
     * @param context Application context
     * @return The SmsDataSource instance
     */
    private fun getSmsDataSource(context: Context): SmsDataSource {
        return smsDataSource ?: synchronized(this) {
            smsDataSource ?: SmsDataSource(context).also {
                smsDataSource = it
                Log.i(TAG, "SmsDataSource initialized successfully")
            }
        }
    }

    /**
     * Builds the Room database with proper configuration and error handling.
     *
     * Handles database corruption by attempting to recover:
     * 1. First attempt: Try to build database normally
     * 2. Run integrity check to verify database health
     * 3. On corruption: Delete corrupted database files and retry
     * 4. Log all errors for debugging
     *
     * @param context Application context
     * @return Configured KanakkuDatabase instance
     * @throws DatabaseInitializationException if database cannot be initialized after retry
     */
    private fun buildDatabase(context: Context): KanakkuDatabase {
        Log.d(TAG, "Building database: $DATABASE_NAME")

        return try {
            // Attempt to build database normally
            val db = buildDatabaseInternal(context)

            // Verify database integrity on first access
            if (!verifyDatabaseIntegrity(db)) {
                Log.w(TAG, "Database integrity check failed - triggering recovery")
                throw SQLiteException("Database integrity check failed")
            }

            db

        } catch (e: SQLiteException) {
            // SQLite-specific errors (corruption, disk I/O, locking issues)
            Log.e(TAG, "SQLiteException during database initialization: ${e.message}", e)
            attemptDatabaseRecovery(context, e)

        } catch (e: SQLException) {
            // General SQL errors
            Log.e(TAG, "SQLException during database initialization: ${e.message}", e)
            attemptDatabaseRecovery(context, e)

        } catch (e: IllegalStateException) {
            // Invalid database state or Room configuration errors
            Log.e(TAG, "IllegalStateException during database initialization: ${e.message}", e)
            throw DatabaseInitializationException(
                "Database is in an invalid state. Please reinstall the app.",
                e
            )

        } catch (e: Exception) {
            // Catch-all for unexpected errors
            Log.e(TAG, "Unexpected error during database initialization: ${e.message}", e)
            throw DatabaseInitializationException(
                "Unexpected error during database initialization. Please contact support.",
                e
            )
        }
    }

    /**
     * Attempts to recover from database errors by cleaning up corrupted files and retrying.
     *
     * @param context Application context
     * @param originalException The exception that triggered recovery
     * @return A new KanakkuDatabase instance
     * @throws DatabaseInitializationException if recovery fails
     */
    private fun attemptDatabaseRecovery(context: Context, originalException: Exception): KanakkuDatabase {
        Log.w(TAG, "Attempting database recovery after error")

        // Try to recover by deleting corrupted database
        handleDatabaseCorruption(context)

        return try {
            // Retry building database after cleanup
            Log.i(TAG, "Retrying database creation after corruption cleanup")
            buildDatabaseInternal(context)

        } catch (retryException: Exception) {
            // If retry fails, throw a critical exception
            Log.e(TAG, "Database recovery failed: ${retryException.message}", retryException)
            throw DatabaseInitializationException(
                "Database is corrupted and recovery failed. Please reinstall the app to fix this issue. " +
                        "Original error: ${originalException.message}",
                retryException
            )
        }
    }

    /**
     * Internal method to build the database without error handling.
     *
     * Configured with:
     * - Proper migrations to preserve user data across schema changes
     * - Write-Ahead Logging (WAL) for better concurrency
     *
     * @param context Application context
     * @return Configured KanakkuDatabase instance
     */
    private fun buildDatabaseInternal(context: Context): KanakkuDatabase {
        return Room.databaseBuilder(
            context,
            KanakkuDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration() // For any unmapped migrations during development
            .build()
    }

    /**
     * Verifies database integrity using SQLite's PRAGMA quick_check.
     *
     * This performs a lightweight health check on the database to detect corruption early.
     * The quick_check is faster than a full integrity_check and is suitable for startup checks.
     *
     * @param database The database instance to verify
     * @return true if database is healthy, false if corrupted
     */
    private fun verifyDatabaseIntegrity(database: KanakkuDatabase): Boolean {
        return try {
            Log.d(TAG, "Performing database integrity check...")

            // Access the underlying SQLite database
            val sqliteDb = database.openHelper.writableDatabase

            // Run PRAGMA quick_check - returns "ok" if database is healthy
            val isHealthy = sqliteDb.query("PRAGMA quick_check").use { cursor ->
                if (cursor.moveToFirst()) {
                    val result = cursor.getString(0)
                    val healthy = result.equals("ok", ignoreCase = true)

                    if (healthy) {
                        Log.i(TAG, "Database integrity check passed: $result")
                    } else {
                        Log.w(TAG, "Database integrity check failed: $result")
                    }

                    healthy
                } else {
                    Log.w(TAG, "Database integrity check returned no results")
                    false
                }
            }

            isHealthy

        } catch (e: SQLiteException) {
            // Database corruption or access error
            Log.e(TAG, "SQLiteException during integrity check: ${e.message}", e)
            false

        } catch (e: Exception) {
            // Unexpected error during integrity check
            Log.e(TAG, "Unexpected error during integrity check: ${e.message}", e)
            false
        }
    }

    /**
     * Handles database corruption by deleting corrupted database files.
     * This allows the app to recover by creating a fresh database.
     *
     * Deletes:
     * - Main database file (.db)
     * - Write-Ahead Logging file (-wal)
     * - Shared Memory file (-shm)
     *
     * @param context Application context
     */
    private fun handleDatabaseCorruption(context: Context) {
        try {
            Log.w(TAG, "Cleaning up corrupted database files...")

            // Close existing database connection if open
            database?.close()
            database = null

            // Get database file path
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            var filesDeleted = 0

            // Delete main database file
            if (dbFile.exists()) {
                if (dbFile.delete()) {
                    Log.d(TAG, "Deleted corrupted database: ${dbFile.name}")
                    filesDeleted++
                } else {
                    Log.w(TAG, "Failed to delete database file: ${dbFile.name}")
                }
            }

            // Delete WAL (Write-Ahead Logging) file if it exists
            val walFile = File(dbFile.parent, "$DATABASE_NAME-wal")
            if (walFile.exists()) {
                if (walFile.delete()) {
                    Log.d(TAG, "Deleted WAL file: ${walFile.name}")
                    filesDeleted++
                } else {
                    Log.w(TAG, "Failed to delete WAL file: ${walFile.name}")
                }
            }

            // Delete SHM (Shared Memory) file if it exists
            val shmFile = File(dbFile.parent, "$DATABASE_NAME-shm")
            if (shmFile.exists()) {
                if (shmFile.delete()) {
                    Log.d(TAG, "Deleted SHM file: ${shmFile.name}")
                    filesDeleted++
                } else {
                    Log.w(TAG, "Failed to delete SHM file: ${shmFile.name}")
                }
            }

            Log.i(TAG, "Database corruption cleanup completed. Deleted $filesDeleted file(s)")

        } catch (e: Exception) {
            Log.e(TAG, "Error during corruption cleanup: ${e.message}", e)
            // Don't throw - let the retry attempt fail if cleanup didn't work
        }
    }

    /**
     * Clears the singleton instances.
     * Useful for testing or forcing a database reset.
     *
     * @deprecated With Hilt, use Hilt testing utilities instead of manual instance management.
     * For instrumented tests, use HiltAndroidTest and HiltAndroidRule.
     * For unit tests, provide mock dependencies via Hilt test modules.
     *
     * WARNING: This should only be used in tests or controlled scenarios.
     * Calling this while the database is in use may cause issues.
     *
     * @see dagger.hilt.android.testing.HiltAndroidTest
     * @see dagger.hilt.android.testing.HiltAndroidRule
     */
    @Deprecated(
        message = "Use Hilt testing utilities for test dependency management instead of manual singleton resets.",
        level = DeprecationLevel.WARNING
    )
    @Synchronized
    fun resetInstance() {
        try {
            database?.close()
            Log.d(TAG, "Database instance reset")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing database during reset: ${e.message}", e)
        }
        database = null
        smsDataSource = null
        repository = null
        budgetRepository = null
    }

    /**
     * Sets a test repository instance for testing purposes.
     * This allows tests to inject their own repository with test databases.
     *
     * WARNING: This should only be used in tests.
     *
     * @param testRepository The test repository instance to use
     */
    @Synchronized
    fun setTestRepository(testRepository: TransactionRepository) {
        repository = testRepository
        Log.d(TAG, "Test repository injected")
    }
}

/**
 * Exception thrown when database initialization fails critically.
 *
 * This exception indicates a critical error that prevents the app from functioning.
 * It should be caught by the UI layer to inform the user about the issue.
 *
 * Common causes:
 * - Database file corruption
 * - Insufficient disk space
 * - File permission issues
 * - SQLite version incompatibility
 *
 * Recovery steps:
 * 1. Inform user of the issue
 * 2. Suggest app reinstall or data clearing
 * 3. Log details for debugging
 *
 * @param message User-friendly error message
 * @param cause The underlying cause of the initialization failure
 */
class DatabaseInitializationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
