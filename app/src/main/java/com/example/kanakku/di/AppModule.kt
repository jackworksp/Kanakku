package com.example.kanakku.di

import android.content.Context
import com.example.kanakku.data.category.CategoryManager
import com.example.kanakku.data.repository.TransactionRepository
import com.example.kanakku.data.sms.BankSmsParser
import com.example.kanakku.data.sms.SmsReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing application-level dependencies.
 *
 * This module is installed in the SingletonComponent, meaning all provided dependencies
 * will have application-level scope and be created only once during the app's lifetime.
 *
 * Provides:
 * - BankSmsParser for parsing bank transaction SMS messages
 * - CategoryManager for transaction categorization
 * - SmsReader for reading SMS messages from device inbox
 *
 * All dependencies are scoped as @Singleton to ensure consistent behavior and
 * efficient resource usage across the application.
 *
 * @see BankSmsParser
 * @see CategoryManager
 * @see SmsReader
 * @see SingletonComponent
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the singleton BankSmsParser instance.
     *
     * BankSmsParser has no dependencies and is stateless, making it safe
     * to share as a singleton across the application. It provides utilities
     * for parsing bank transaction SMS messages and extracting structured
     * transaction data.
     *
     * This method is called once by Hilt and the instance is cached for the
     * lifetime of the application.
     *
     * @return BankSmsParser instance for parsing bank SMS
     */
    @Provides
    @Singleton
    fun provideBankSmsParser(): BankSmsParser {
        return BankSmsParser()
    }

    /**
     * Provides the singleton CategoryManager instance.
     *
     * CategoryManager handles automatic categorization of transactions based on
     * keywords and supports manual category overrides that persist to database.
     * It requires TransactionRepository for loading and persisting category overrides.
     *
     * This method is called once by Hilt and the instance is cached for the
     * lifetime of the application.
     *
     * @param repository TransactionRepository instance injected by Hilt
     * @return Configured CategoryManager instance
     */
    @Provides
    @Singleton
    fun provideCategoryManager(
        repository: TransactionRepository
    ): CategoryManager {
        return CategoryManager(repository)
    }

    /**
     * Provides the singleton SmsReader instance.
     *
     * SmsReader provides access to SMS messages from the device inbox with
     * comprehensive error handling for permission issues and data corruption.
     * It requires application context to access the SMS content provider.
     *
     * This method is called once by Hilt and the instance is cached for the
     * lifetime of the application.
     *
     * @param context Application context injected by Hilt
     * @return SmsReader instance for reading device SMS
     */
    @Provides
    @Singleton
    fun provideSmsReader(
        @ApplicationContext context: Context
    ): SmsReader {
        return SmsReader(context)
    }
}
