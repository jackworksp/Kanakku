package com.example.kanakku

import android.app.Application
import android.util.Log
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.database.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom Application class for Kanakku app.
 *
 * This class initializes the app-wide resources and performs critical setup tasks
 * that must happen before any Activity starts.
 *
 * Key Responsibilities:
 * - Initialize the database on first launch
 * - Seed default categories if the database is empty
 * - Set up app-wide resources and configurations
 *
 * Initialization Strategy:
 * - Uses a CoroutineScope with SupervisorJob for background initialization
 * - Does not block the main thread during database seeding
 * - Handles all errors gracefully with ErrorHandler
 * - Logs all initialization steps for debugging
 *
 * This ensures categories are available immediately when the app starts,
 * rather than lazily when the ViewModel loads data for the first time.
 */
class KanakkuApplication : Application() {

    companion object {
        private const val TAG = "KanakkuApplication"
    }

    // Application-scoped coroutine scope for background work
    // Uses SupervisorJob to prevent child failures from canceling the entire scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Kanakku application starting...")

        // Initialize database and seed default categories asynchronously
        // This doesn't block app startup but ensures categories are ready quickly
        applicationScope.launch {
            initializeDatabase()
        }
    }

    /**
     * Initializes the database and seeds default categories if needed.
     *
     * This method:
     * 1. Gets the CategoryRepository instance (initializes database if needed)
     * 2. Checks if the categories table is empty
     * 3. Seeds default categories if the table is empty
     * 4. Logs all steps for debugging
     * 5. Handles all errors gracefully without crashing the app
     *
     * Runs on IO dispatcher to avoid blocking the main thread.
     */
    private suspend fun initializeDatabase() {
        try {
            Log.d(TAG, "Initializing database...")

            // Get CategoryRepository instance - this also initializes the database
            val categoryRepository = DatabaseProvider.getCategoryRepository(this@KanakkuApplication)

            Log.d(TAG, "Database initialized successfully")

            // Check if categories table is empty
            categoryRepository.isCategoryDatabaseEmpty()
                .onSuccess { isEmpty ->
                    if (isEmpty) {
                        Log.i(TAG, "Categories table is empty, seeding default categories...")

                        // Seed default categories
                        categoryRepository.seedDefaultCategories()
                            .onSuccess { count ->
                                Log.i(TAG, "Successfully seeded $count default categories")
                            }
                            .onFailure { throwable ->
                                val errorInfo = ErrorHandler.handleError(
                                    throwable as? Exception ?: Exception(throwable),
                                    "Seed default categories"
                                )
                                Log.e(
                                    TAG,
                                    "Failed to seed default categories: ${errorInfo.technicalMessage}",
                                    throwable
                                )
                            }
                    } else {
                        Log.d(TAG, "Categories table already populated, skipping seed")
                    }
                }
                .onFailure { throwable ->
                    val errorInfo = ErrorHandler.handleError(
                        throwable as? Exception ?: Exception(throwable),
                        "Check if categories database is empty"
                    )
                    Log.e(
                        TAG,
                        "Failed to check categories table: ${errorInfo.technicalMessage}",
                        throwable
                    )
                }

        } catch (e: Exception) {
            // Catch-all for unexpected errors during initialization
            val errorInfo = ErrorHandler.handleError(e, "Database initialization")
            Log.e(
                TAG,
                "Database initialization failed: ${errorInfo.technicalMessage}",
                e
            )
            // Don't crash the app - database will be initialized lazily when needed
        }
    }
}
