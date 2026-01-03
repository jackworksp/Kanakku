package com.example.kanakku.di

import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository-related dependencies.
 *
 * This module is installed in the SingletonComponent, meaning all provided dependencies
 * will have application-level scope and be created only once during the app's lifetime.
 *
 * Provides:
 * - TransactionRepository for managing transaction persistence and retrieval
 *
 * The repository is configured with:
 * - Singleton scope for consistent data access across the app
 * - Database instance injected by Hilt
 *
 * @see TransactionRepository
 * @see SingletonComponent
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * Provides the singleton TransactionRepository instance.
     *
     * The repository acts as a bridge between the domain layer and data layer,
     * handling all transaction persistence operations with proper error handling
     * and caching.
     *
     * This method is called once by Hilt and the instance is cached for the
     * lifetime of the application.
     *
     * @param database KanakkuDatabase instance injected by Hilt
     * @return Configured TransactionRepository instance
     */
    @Provides
    @Singleton
    fun provideTransactionRepository(
        database: KanakkuDatabase
    ): TransactionRepository {
        return TransactionRepository(database)
    }
}
