package com.example.kanakku.data.database

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
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

    @Volatile
    private var database: KanakkuDatabase? = null

    @Volatile
    private var repository: TransactionRepository? = null

    private const val DATABASE_NAME = "kanakku_database"

    /**
     * Gets the singleton database instance.
     * Creates the database on first access using the provided context.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     * @return The KanakkuDatabase instance
     */
    fun getDatabase(context: Context): KanakkuDatabase {
        return database ?: synchronized(this) {
            database ?: buildDatabase(context.applicationContext).also {
                database = it
            }
        }
    }

    /**
     * Gets the singleton repository instance.
     * Creates both database and repository on first access.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     * @return The TransactionRepository instance
     */
    fun getRepository(context: Context): TransactionRepository {
        return repository ?: synchronized(this) {
            repository ?: TransactionRepository(getDatabase(context)).also {
                repository = it
            }
        }
    }

    /**
     * Builds the Room database with proper configuration and error handling.
     *
     * Handles database corruption by attempting to recover:
     * 1. First attempt: Try to build database normally
     * 2. On corruption: Delete corrupted database files and retry
     * 3. Log all errors for debugging
     *
     * @param context Application context
     * @return Configured KanakkuDatabase instance
     * @throws DatabaseInitializationException if database cannot be initialized after retry
     */
    private fun buildDatabase(context: Context): KanakkuDatabase {
        return try {
            // Attempt to build database normally
            buildDatabaseInternal(context)
        } catch (e: SQLiteException) {
            // Database corruption or error - attempt recovery
            System.err.println("Database initialization failed: ${e.message}")
            e.printStackTrace()

            // Try to recover by deleting corrupted database
            handleDatabaseCorruption(context)

            try {
                // Retry building database after cleanup
                buildDatabaseInternal(context)
            } catch (retryException: Exception) {
                // If retry fails, throw a more descriptive exception
                System.err.println("Database recovery failed: ${retryException.message}")
                retryException.printStackTrace()
                throw DatabaseInitializationException(
                    "Failed to initialize database after corruption recovery",
                    retryException
                )
            }
        } catch (e: IllegalStateException) {
            // Invalid database state
            System.err.println("Database state error: ${e.message}")
            e.printStackTrace()
            throw DatabaseInitializationException("Database is in an invalid state", e)
        } catch (e: Exception) {
            // Catch-all for unexpected errors
            System.err.println("Unexpected database error: ${e.message}")
            e.printStackTrace()
            throw DatabaseInitializationException("Unexpected error during database initialization", e)
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
            .fallbackToDestructiveMigration() // For development - TODO: implement proper migrations for production
            .build()
    }

    /**
     * Handles database corruption by deleting corrupted database files.
     * This allows the app to recover by creating a fresh database.
     *
     * @param context Application context
     */
    private fun handleDatabaseCorruption(context: Context) {
        try {
            System.err.println("Attempting to recover from database corruption...")

            // Get database file path
            val dbFile = context.getDatabasePath(DATABASE_NAME)

            // Delete main database file
            if (dbFile.exists() && dbFile.delete()) {
                System.err.println("Deleted corrupted database: ${dbFile.absolutePath}")
            }

            // Delete WAL (Write-Ahead Logging) file if it exists
            val walFile = File(dbFile.parent, "$DATABASE_NAME-wal")
            if (walFile.exists() && walFile.delete()) {
                System.err.println("Deleted WAL file: ${walFile.absolutePath}")
            }

            // Delete SHM (Shared Memory) file if it exists
            val shmFile = File(dbFile.parent, "$DATABASE_NAME-shm")
            if (shmFile.exists() && shmFile.delete()) {
                System.err.println("Deleted SHM file: ${shmFile.absolutePath}")
            }

            System.err.println("Database corruption cleanup completed")
        } catch (e: Exception) {
            System.err.println("Error during corruption cleanup: ${e.message}")
            e.printStackTrace()
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
        database?.close()
        database = null
        repository = null
    }
}

/**
 * Exception thrown when database initialization fails.
 * This exception indicates a critical error that prevents the app from functioning.
 *
 * @param message Descriptive error message
 * @param cause The underlying cause of the initialization failure
 */
class DatabaseInitializationException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
