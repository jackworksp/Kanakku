package com.example.kanakku.data.database

import android.content.Context
import androidx.room.Room
import com.example.kanakku.data.repository.SavingsGoalRepository
import com.example.kanakku.data.repository.TransactionRepository

/**
 * Singleton provider for database instance and repositories.
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
 * val savingsGoalRepository = DatabaseProvider.getSavingsGoalRepository(context)
 * ```
 */
object DatabaseProvider {

    @Volatile
    private var database: KanakkuDatabase? = null

    @Volatile
    private var repository: TransactionRepository? = null

    @Volatile
    private var savingsGoalRepository: SavingsGoalRepository? = null

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
     * Gets the singleton savings goal repository instance.
     * Creates both database and repository on first access.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     * @return The SavingsGoalRepository instance
     */
    fun getSavingsGoalRepository(context: Context): SavingsGoalRepository {
        return savingsGoalRepository ?: synchronized(this) {
            savingsGoalRepository ?: SavingsGoalRepository(getDatabase(context)).also {
                savingsGoalRepository = it
            }
        }
    }

    /**
     * Builds the Room database with proper configuration.
     *
     * @param context Application context
     * @return Configured KanakkuDatabase instance
     */
    private fun buildDatabase(context: Context): KanakkuDatabase {
        return Room.databaseBuilder(
            context,
            KanakkuDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(KanakkuDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration() // Fallback for any unmapped migrations
            .build()
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
        savingsGoalRepository = null
    }
}
