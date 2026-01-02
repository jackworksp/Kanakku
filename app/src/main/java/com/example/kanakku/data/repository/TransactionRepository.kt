package com.example.kanakku.data.repository

import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.entity.CategoryOverrideEntity
import com.example.kanakku.data.database.entity.SyncMetadataEntity
import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
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
     */
    suspend fun saveTransaction(transaction: ParsedTransaction) {
        transactionDao.insert(transaction.toEntity())
    }

    /**
     * Saves multiple transactions to the database in a single operation.
     * More efficient than saving individually for bulk imports.
     *
     * @param transactions List of parsed transactions to save
     */
    suspend fun saveTransactions(transactions: List<ParsedTransaction>) {
        transactionDao.insertAll(transactions.map { it.toEntity() })
    }

    /**
     * Retrieves all transactions as a reactive Flow.
     * Automatically converts entities to domain models.
     *
     * @return Flow emitting list of parsed transactions, sorted by date (newest first)
     */
    fun getAllTransactions(): Flow<List<ParsedTransaction>> {
        return transactionDao.getAllTransactions()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Retrieves all transactions as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return List of parsed transactions, sorted by date (newest first)
     */
    suspend fun getAllTransactionsSnapshot(): List<ParsedTransaction> {
        return transactionDao.getAllTransactionsSnapshot()
            .map { it.toDomain() }
    }

    /**
     * Retrieves transactions filtered by type.
     *
     * @param type The transaction type to filter by (DEBIT/CREDIT/UNKNOWN)
     * @return Flow emitting list of transactions matching the type
     */
    fun getTransactionsByType(type: TransactionType): Flow<List<ParsedTransaction>> {
        return transactionDao.getTransactionsByType(type)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Retrieves transactions within a specific date range.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Flow emitting list of transactions within the date range
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<ParsedTransaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Retrieves transactions after a specific timestamp.
     * Useful for incremental sync operations.
     *
     * @param timestamp The timestamp to query after
     * @return List of transactions after the specified timestamp
     */
    suspend fun getTransactionsAfter(timestamp: Long): List<ParsedTransaction> {
        return transactionDao.getTransactionsAfter(timestamp)
            .map { it.toDomain() }
    }

    /**
     * Checks if a transaction with the given SMS ID exists.
     *
     * @param smsId The SMS ID to check
     * @return True if transaction exists, false otherwise
     */
    suspend fun transactionExists(smsId: Long): Boolean {
        return transactionDao.exists(smsId)
    }

    /**
     * Deletes a transaction by its SMS ID.
     *
     * @param smsId The SMS ID of the transaction to delete
     * @return True if transaction was deleted, false if not found
     */
    suspend fun deleteTransaction(smsId: Long): Boolean {
        return transactionDao.deleteById(smsId) > 0
    }

    /**
     * Deletes all transactions from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of transactions deleted
     */
    suspend fun deleteAllTransactions(): Int {
        return transactionDao.deleteAll()
    }

    /**
     * Gets the total count of transactions in the database.
     *
     * @return Total number of transactions
     */
    suspend fun getTransactionCount(): Int {
        return transactionDao.getTransactionCount()
    }

    /**
     * Gets the most recent transaction date.
     * Useful for determining last sync time.
     *
     * @return The timestamp of the most recent transaction, or null if no transactions exist
     */
    suspend fun getLatestTransactionDate(): Long? {
        return transactionDao.getLatestTransactionDate()
    }

    // ==================== Category Override Operations ====================

    /**
     * Sets a category override for a specific transaction.
     *
     * @param smsId The SMS ID of the transaction
     * @param categoryId The category ID to override with
     */
    suspend fun setCategoryOverride(smsId: Long, categoryId: String) {
        categoryOverrideDao.insert(CategoryOverrideEntity(smsId, categoryId))
    }

    /**
     * Gets the category override for a specific transaction.
     *
     * @param smsId The SMS ID of the transaction
     * @return The category ID override, or null if no override exists
     */
    suspend fun getCategoryOverride(smsId: Long): String? {
        return categoryOverrideDao.getOverride(smsId)?.categoryId
    }

    /**
     * Gets all category overrides as a Map.
     *
     * @return Map of SMS ID to category ID
     */
    suspend fun getAllCategoryOverrides(): Map<Long, String> {
        return categoryOverrideDao.getAllOverridesSnapshot()
            .associate { it.smsId to it.categoryId }
    }

    /**
     * Gets all category overrides as a reactive Flow.
     *
     * @return Flow emitting Map of SMS ID to category ID
     */
    fun getAllCategoryOverridesFlow(): Flow<Map<Long, String>> {
        return categoryOverrideDao.getAllOverrides()
            .map { overrides -> overrides.associate { it.smsId to it.categoryId } }
    }

    /**
     * Removes a category override for a specific transaction.
     *
     * @param smsId The SMS ID of the transaction
     * @return True if override was removed, false if no override existed
     */
    suspend fun removeCategoryOverride(smsId: Long): Boolean {
        return categoryOverrideDao.deleteOverride(smsId) > 0
    }

    /**
     * Removes all category overrides.
     *
     * @return Number of overrides removed
     */
    suspend fun removeAllCategoryOverrides(): Int {
        return categoryOverrideDao.deleteAll()
    }

    // ==================== Sync Metadata Operations ====================

    /**
     * Gets the last sync timestamp.
     * This indicates when transactions were last synced from SMS.
     *
     * @return The last sync timestamp in milliseconds, or null if never synced
     */
    suspend fun getLastSyncTimestamp(): Long? {
        return syncMetadataDao.getValue(KEY_LAST_SYNC_TIMESTAMP)?.toLongOrNull()
    }

    /**
     * Sets the last sync timestamp.
     * Should be called after successfully syncing transactions.
     *
     * @param timestamp The sync timestamp in milliseconds
     */
    suspend fun setLastSyncTimestamp(timestamp: Long) {
        syncMetadataDao.insert(
            SyncMetadataEntity(KEY_LAST_SYNC_TIMESTAMP, timestamp.toString())
        )
    }

    /**
     * Gets the last processed SMS ID.
     * This can be used to track which SMS messages have been parsed.
     *
     * @return The last processed SMS ID, or null if none processed
     */
    suspend fun getLastProcessedSmsId(): Long? {
        return syncMetadataDao.getValue(KEY_LAST_PROCESSED_SMS_ID)?.toLongOrNull()
    }

    /**
     * Sets the last processed SMS ID.
     * Should be called after successfully processing an SMS.
     *
     * @param smsId The SMS ID that was processed
     */
    suspend fun setLastProcessedSmsId(smsId: Long) {
        syncMetadataDao.insert(
            SyncMetadataEntity(KEY_LAST_PROCESSED_SMS_ID, smsId.toString())
        )
    }

    /**
     * Clears all sync metadata.
     * Useful for forcing a full re-sync.
     *
     * @return Number of metadata entries removed
     */
    suspend fun clearSyncMetadata(): Int {
        return syncMetadataDao.deleteAll()
    }

    // ==================== Entity-Model Mapping ====================

    /**
     * Converts a ParsedTransaction domain model to a TransactionEntity.
     */
    private fun ParsedTransaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            smsId = this.smsId,
            amount = this.amount,
            type = this.type,
            merchant = this.merchant,
            accountNumber = this.accountNumber,
            referenceNumber = this.referenceNumber,
            date = this.date,
            rawSms = this.rawSms,
            senderAddress = this.senderAddress,
            balanceAfter = this.balanceAfter,
            location = this.location
        )
    }

    /**
     * Converts a TransactionEntity to a ParsedTransaction domain model.
     */
    private fun TransactionEntity.toDomain(): ParsedTransaction {
        return ParsedTransaction(
            smsId = this.smsId,
            amount = this.amount,
            type = this.type,
            merchant = this.merchant,
            accountNumber = this.accountNumber,
            referenceNumber = this.referenceNumber,
            date = this.date,
            rawSms = this.rawSms,
            senderAddress = this.senderAddress,
            balanceAfter = this.balanceAfter,
            location = this.location
        )
    }
}
