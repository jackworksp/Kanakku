package com.example.kanakku.data.database

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.Room
import com.example.kanakku.data.repository.RecurringTransactionRepository
import com.example.kanakku.data.repository.TransactionRepository
import java.io.File

/**
 * Singleton provider for database instance and repository.
 *
 * This class manages the lifecycle of the Room database and provides
 * centralized access to database operations through the repository pattern.
 *
 * Thread-safe singleton implementation using double-checked locking.
 * Database is lazily initialized on first access.
 *
 * Usage:
 * ```
 * val repository = DatabaseProvider.getRepository(context)
 * ```
 */
object DatabaseProvider {

    private const val TAG = "DatabaseProvider"
    private const val DATABASE_NAME = "kanakku_database"

    @Volatile
    private var database: KanakkuDatabase? = null

    @Volatile
    private var repository: TransactionRepository? = null

    @Volatile
    private var recurringRepository: RecurringTransactionRepository? = null

    /**
     * Gets the singleton database instance.
     * Creates the database on first access using the provided context.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     * @return The KanakkuDatabase instance
     * @throws DatabaseInitializationException if database cannot be created after recovery attempts
     */
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
     * Creates both database and repository on first access.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     * @return The TransactionRepository instance
     * @throws DatabaseInitializationException if database cannot be created after recovery attempts
     */
    fun getRepository(context: Context): TransactionRepository {
        return repository ?: synchronized(this) {
            repository ?: TransactionRepository(getDatabase(context)).also {
                repository = it
                Log.i(TAG, "Repository initialized successfully")
            }
        }
    }

    /**
     * Gets the singleton recurring transaction repository instance.
     * Creates both database and repository on first access.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     * @return The RecurringTransactionRepository instance
     * @throws DatabaseInitializationException if database cannot be created after recovery attempts
     */
    fun getRecurringRepository(context: Context): RecurringTransactionRepository {
        return recurringRepository ?: synchronized(this) {
            recurringRepository ?: RecurringTransactionRepository(getDatabase(context)).also {
                recurringRepository = it
                Log.i(TAG, "RecurringTransactionRepository initialized successfully")
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
            .fallbackToDestructiveMigration() // Fallback for any unmapped migrations
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
     * WARNING: This should only be used in tests or controlled scenarios.
     * Calling this while the database is in use may cause issues.
     */
    @Synchronized
    fun resetInstance() {
        try {
            database?.close()
            Log.d(TAG, "Database instance reset")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing database during reset: ${e.message}", e)
        }
        database = null
        repository = null
        recurringRepository = null
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
