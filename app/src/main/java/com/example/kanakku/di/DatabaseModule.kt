package com.example.kanakku.di

import android.content.Context
import androidx.room.Room
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.dao.CategoryOverrideDao
import com.example.kanakku.data.database.dao.SyncMetadataDao
import com.example.kanakku.data.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 *
 * This module is installed in the SingletonComponent, meaning all provided dependencies
 * will have application-level scope and be created only once during the app's lifetime.
 *
 * Provides:
 * - KanakkuDatabase instance with proper Room configuration
 * - TransactionDao for transaction CRUD operations
 * - CategoryOverrideDao for category override operations
 * - SyncMetadataDao for sync metadata operations
 *
 * The database is configured with:
 * - Fallback to destructive migration (for development)
 * - Application context to prevent memory leaks
 *
 * @see KanakkuDatabase
 * @see SingletonComponent
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "kanakku_database"

    /**
     * Provides the singleton KanakkuDatabase instance.
     *
     * The database is built using Room with the following configuration:
     * - Uses application context to prevent memory leaks
     * - Enables fallback to destructive migration (for development)
     * - Named "kanakku_database"
     *
     * This method is called once by Hilt and the instance is cached for the
     * lifetime of the application.
     *
     * @param context Application context injected by Hilt
     * @return Configured KanakkuDatabase instance
     */
    @Provides
    @Singleton
    fun provideKanakkuDatabase(
        @ApplicationContext context: Context
    ): KanakkuDatabase {
        return Room.databaseBuilder(
            context,
            KanakkuDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development - TODO: implement proper migrations for production
            .build()
    }

    /**
     * Provides the TransactionDao instance.
     *
     * The DAO is obtained from the database instance provided by provideKanakkuDatabase().
     * Since the database is a singleton, the DAO will also effectively be a singleton.
     *
     * @param database KanakkuDatabase instance injected by Hilt
     * @return TransactionDao instance for transaction operations
     */
    @Provides
    @Singleton
    fun provideTransactionDao(database: KanakkuDatabase): TransactionDao {
        return database.transactionDao()
    }

    /**
     * Provides the CategoryOverrideDao instance.
     *
     * The DAO is obtained from the database instance provided by provideKanakkuDatabase().
     * Since the database is a singleton, the DAO will also effectively be a singleton.
     *
     * @param database KanakkuDatabase instance injected by Hilt
     * @return CategoryOverrideDao instance for category override operations
     */
    @Provides
    @Singleton
    fun provideCategoryOverrideDao(database: KanakkuDatabase): CategoryOverrideDao {
        return database.categoryOverrideDao()
    }

    /**
     * Provides the SyncMetadataDao instance.
     *
     * The DAO is obtained from the database instance provided by provideKanakkuDatabase().
     * Since the database is a singleton, the DAO will also effectively be a singleton.
     *
     * @param database KanakkuDatabase instance injected by Hilt
     * @return SyncMetadataDao instance for sync metadata operations
     */
    @Provides
    @Singleton
    fun provideSyncMetadataDao(database: KanakkuDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }
}
