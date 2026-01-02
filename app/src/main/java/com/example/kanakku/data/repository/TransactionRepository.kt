package com.example.kanakku.data.repository

import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.entity.CategoryOverrideEntity
import com.example.kanakku.data.database.entity.SyncMetadataEntity
import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.database.toDomain
import com.example.kanakku.data.database.toEntity
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Repository for managing transaction persistence and retrieval.
 *
 * This repository acts as a bridge between the domain layer (ParsedTransaction)
 * and the data layer (TransactionEntity), handling all entity-model mapping
 * and database operations.
 *
 * Key responsibilities:
 * - Save and retrieve transactions
 * - Manage category overrides
 * - Track sync metadata for incremental updates
 * - Provide reactive data streams via Flow
 * - Handle all database errors gracefully with comprehensive error handling
 *
 * Error Handling Strategy:
 * - All database operations wrapped in try-catch blocks via ErrorHandler
 * - Errors logged for debugging with appropriate context
 * - Suspend functions return Result<T> for error propagation to callers
 * - Flow operations include .catch() to emit fallback values on error
 * - No uncaught exceptions - all errors are handled gracefully
 *
 * @param database The Room database instance
 */
class TransactionRepository(private val database: KanakkuDatabase) {

    // DAOs for database access
    private val transactionDao = database.transactionDao()
    private val categoryOverrideDao = database.categoryOverrideDao()
    private val syncMetadataDao = database.syncMetadataDao()

    // Metadata keys
    companion object {
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_LAST_PROCESSED_SMS_ID = "last_processed_sms_id"
    }

    // ==================== Transaction Operations ====================

    /**
     * Saves a single transaction to the database.
     * Converts the domain model to an entity before persisting.
     *
     * @param transaction The parsed transaction to save
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun saveTransaction(transaction: ParsedTransaction): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Save transaction") {
            transactionDao.insert(transaction.toEntity())
        }
    }

    /**
     * Saves multiple transactions to the database in a single operation.
     * More efficient than saving individually for bulk imports.
     *
     * @param transactions List of parsed transactions to save
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun saveTransactions(transactions: List<ParsedTransaction>): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Save transactions") {
            transactionDao.insertAll(transactions.map { it.toEntity() })
        }
    }

    /**
     * Retrieves all transactions as a reactive Flow.
     * Automatically converts entities to domain models.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @return Flow emitting list of parsed transactions, sorted by date (newest first)
     */
    fun getAllTransactions(): Flow<List<ParsedTransaction>> {
        return transactionDao.getAllTransactions()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get all transactions")
                emit(emptyList())
            }
    }

    /**
     * Retrieves all transactions as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return Result<List<ParsedTransaction>> containing transactions or error information
     */
    suspend fun getAllTransactionsSnapshot(): Result<List<ParsedTransaction>> {
        return ErrorHandler.runSuspendCatching("Get all transactions snapshot") {
            transactionDao.getAllTransactionsSnapshot()
                .map { it.toDomain() }
        }
    }

    /**
     * Retrieves transactions filtered by type.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @param type The transaction type to filter by (DEBIT/CREDIT/UNKNOWN)
     * @return Flow emitting list of transactions matching the type
     */
    fun getTransactionsByType(type: TransactionType): Flow<List<ParsedTransaction>> {
        return transactionDao.getTransactionsByType(type)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get transactions by type: $type")
                emit(emptyList())
            }
    }

    /**
     * Retrieves transactions within a specific date range.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Flow emitting list of transactions within the date range
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<ParsedTransaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get transactions by date range")
                emit(emptyList())
            }
    }

    /**
     * Retrieves transactions after a specific timestamp.
     * Useful for incremental sync operations.
     *
     * @param timestamp The timestamp to query after
     * @return Result<List<ParsedTransaction>> containing transactions or error information
     */
    suspend fun getTransactionsAfter(timestamp: Long): Result<List<ParsedTransaction>> {
        return ErrorHandler.runSuspendCatching("Get transactions after timestamp") {
            transactionDao.getTransactionsAfter(timestamp)
                .map { it.toDomain() }
        }
    }

    /**
     * Checks if a transaction with the given SMS ID exists.
     *
     * @param smsId The SMS ID to check
     * @return Result<Boolean> indicating if transaction exists or error information
     */
    suspend fun transactionExists(smsId: Long): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Check transaction exists") {
            transactionDao.exists(smsId)
        }
    }

    /**
     * Deletes a transaction by its SMS ID.
     *
     * @param smsId The SMS ID of the transaction to delete
     * @return Result<Boolean> indicating if transaction was deleted or error information
     */
    suspend fun deleteTransaction(smsId: Long): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Delete transaction") {
            transactionDao.deleteById(smsId) > 0
        }
    }

    /**
     * Deletes all transactions from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Result<Int> containing number of transactions deleted or error information
     */
    suspend fun deleteAllTransactions(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Delete all transactions") {
            transactionDao.deleteAll()
        }
    }

    /**
     * Gets the total count of transactions in the database.
     *
     * @return Result<Int> containing total number of transactions or error information
     */
    suspend fun getTransactionCount(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Get transaction count") {
            transactionDao.getTransactionCount()
        }
    }

    /**
     * Gets the most recent transaction date.
     * Useful for determining last sync time.
     *
     * @return Result<Long?> containing timestamp of most recent transaction or error information
     */
    suspend fun getLatestTransactionDate(): Result<Long?> {
        return ErrorHandler.runSuspendCatching("Get latest transaction date") {
            transactionDao.getLatestTransactionDate()
        }
    }

    // ==================== Category Override Operations ====================

    /**
     * Sets a category override for a specific transaction.
     *
     * @param smsId The SMS ID of the transaction
     * @param categoryId The category ID to override with
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun setCategoryOverride(smsId: Long, categoryId: String): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Set category override") {
            categoryOverrideDao.insert(CategoryOverrideEntity(smsId, categoryId))
        }
    }

    /**
     * Gets the category override for a specific transaction.
     *
     * @param smsId The SMS ID of the transaction
     * @return Result<String?> containing category ID override or error information
     */
    suspend fun getCategoryOverride(smsId: Long): Result<String?> {
        return ErrorHandler.runSuspendCatching("Get category override") {
            categoryOverrideDao.getOverride(smsId)?.categoryId
        }
    }

    /**
     * Gets all category overrides as a Map.
     *
     * @return Result<Map<Long, String>> containing map of SMS ID to category ID or error information
     */
    suspend fun getAllCategoryOverrides(): Result<Map<Long, String>> {
        return ErrorHandler.runSuspendCatching("Get all category overrides") {
            categoryOverrideDao.getAllOverridesSnapshot()
                .associate { it.smsId to it.categoryId }
        }
    }

    /**
     * Gets all category overrides as a reactive Flow.
     * Errors are logged and an empty map is emitted on failure.
     *
     * @return Flow emitting Map of SMS ID to category ID
     */
    fun getAllCategoryOverridesFlow(): Flow<Map<Long, String>> {
        return categoryOverrideDao.getAllOverrides()
            .map { overrides -> overrides.associate { it.smsId to it.categoryId } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get all category overrides flow")
                emit(emptyMap())
            }
    }

    /**
     * Removes a category override for a specific transaction.
     *
     * @param smsId The SMS ID of the transaction
     * @return Result<Boolean> indicating if override was removed or error information
     */
    suspend fun removeCategoryOverride(smsId: Long): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Remove category override") {
            categoryOverrideDao.deleteOverride(smsId) > 0
        }
    }

    /**
     * Removes all category overrides.
     *
     * @return Result<Int> containing number of overrides removed or error information
     */
    suspend fun removeAllCategoryOverrides(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Remove all category overrides") {
            categoryOverrideDao.deleteAll()
        }
    }

    // ==================== Sync Metadata Operations ====================

    /**
     * Gets the last sync timestamp.
     * This indicates when transactions were last synced from SMS.
     *
     * @return Result<Long?> containing last sync timestamp in milliseconds or error information
     */
    suspend fun getLastSyncTimestamp(): Result<Long?> {
        return ErrorHandler.runSuspendCatching("Get last sync timestamp") {
            syncMetadataDao.getValue(KEY_LAST_SYNC_TIMESTAMP)?.toLongOrNull()
        }
    }

    /**
     * Sets the last sync timestamp.
     * Should be called after successfully syncing transactions.
     *
     * @param timestamp The sync timestamp in milliseconds
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun setLastSyncTimestamp(timestamp: Long): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Set last sync timestamp") {
            syncMetadataDao.insert(
                SyncMetadataEntity(KEY_LAST_SYNC_TIMESTAMP, timestamp.toString())
            )
        }
    }

    /**
     * Gets the last processed SMS ID.
     * This can be used to track which SMS messages have been parsed.
     *
     * @return Result<Long?> containing last processed SMS ID or error information
     */
    suspend fun getLastProcessedSmsId(): Result<Long?> {
        return ErrorHandler.runSuspendCatching("Get last processed SMS ID") {
            syncMetadataDao.getValue(KEY_LAST_PROCESSED_SMS_ID)?.toLongOrNull()
        }
    }

    /**
     * Sets the last processed SMS ID.
     * Should be called after successfully processing an SMS.
     *
     * @param smsId The SMS ID that was processed
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun setLastProcessedSmsId(smsId: Long): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Set last processed SMS ID") {
            syncMetadataDao.insert(
                SyncMetadataEntity(KEY_LAST_PROCESSED_SMS_ID, smsId.toString())
            )
        }
    }

    /**
     * Clears all sync metadata.
     * Useful for forcing a full re-sync.
     *
     * @return Result<Int> containing number of metadata entries removed or error information
     */
    suspend fun clearSyncMetadata(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Clear sync metadata") {
            syncMetadataDao.deleteAll()
        }
    }
}
