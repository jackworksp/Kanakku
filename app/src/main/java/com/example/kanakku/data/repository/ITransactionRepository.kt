package com.example.kanakku.data.repository

import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Data class representing the result of an SMS sync operation.
 *
 * @property totalSmsRead Total number of SMS messages read from inbox
 * @property bankSmsFound Number of SMS identified as bank transaction messages
 * @property newTransactionsSaved Number of new transactions saved to database
 * @property duplicatesRemoved Number of duplicate transactions filtered out
 * @property syncTimestamp Timestamp when the sync completed
 * @property isIncremental Whether this was an incremental sync (true) or full sync (false)
 */
data class SyncResult(
    val totalSmsRead: Int,
    val bankSmsFound: Int,
    val newTransactionsSaved: Int,
    val duplicatesRemoved: Int,
    val syncTimestamp: Long,
    val isIncremental: Boolean
)

/**
 * Repository interface for managing transaction persistence and retrieval.
 *
 * This interface defines the contract for transaction repository operations,
 * enabling easier mocking and testing. All implementations should handle:
 * - Transaction CRUD operations
 * - Category overrides
 * - Sync metadata tracking
 * - SMS sync capabilities
 *
 * All suspend functions return Result<T> for explicit error handling.
 * All Flow operations should emit fallback values on error rather than throwing.
 */
interface ITransactionRepository {

    // ==================== Transaction Operations ====================

    /**
     * Saves a single transaction to the database.
     *
     * @param transaction The parsed transaction to save
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun saveTransaction(transaction: ParsedTransaction): Result<Unit>

    /**
     * Saves multiple transactions to the database in a single operation.
     * More efficient than saving individually for bulk imports.
     *
     * @param transactions List of parsed transactions to save
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun saveTransactions(transactions: List<ParsedTransaction>): Result<Unit>

    /**
     * Retrieves all transactions as a reactive Flow.
     * Automatically converts entities to domain models.
     *
     * @return Flow emitting list of parsed transactions, sorted by date (newest first)
     */
    fun getAllTransactions(): Flow<List<ParsedTransaction>>

    /**
     * Retrieves all transactions as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return Result<List<ParsedTransaction>> containing transactions or error information
     */
    suspend fun getAllTransactionsSnapshot(): Result<List<ParsedTransaction>>

    /**
     * Retrieves transactions filtered by type.
     *
     * @param type The transaction type to filter by (DEBIT/CREDIT/UNKNOWN)
     * @return Flow emitting list of transactions matching the type
     */
    fun getTransactionsByType(type: TransactionType): Flow<List<ParsedTransaction>>

    /**
     * Retrieves transactions within a specific date range.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Flow emitting list of transactions within the date range
     */
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<ParsedTransaction>>

    /**
     * Retrieves transactions after a specific timestamp.
     * Useful for incremental sync operations.
     *
     * @param timestamp The timestamp to query after
     * @return Result<List<ParsedTransaction>> containing transactions or error information
     */
    suspend fun getTransactionsAfter(timestamp: Long): Result<List<ParsedTransaction>>

    /**
     * Checks if a transaction with the given SMS ID exists.
     *
     * @param smsId The SMS ID to check
     * @return Result<Boolean> indicating if transaction exists or error information
     */
    suspend fun transactionExists(smsId: Long): Result<Boolean>

    /**
     * Deletes a transaction by its SMS ID.
     *
     * @param smsId The SMS ID of the transaction to delete
     * @return Result<Boolean> indicating if transaction was deleted or error information
     */
    suspend fun deleteTransaction(smsId: Long): Result<Boolean>

    /**
     * Deletes all transactions from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Result<Int> containing number of transactions deleted or error information
     */
    suspend fun deleteAllTransactions(): Result<Int>

    /**
     * Gets the total count of transactions in the database.
     *
     * @return Result<Int> containing total number of transactions or error information
     */
    suspend fun getTransactionCount(): Result<Int>

    /**
     * Gets the most recent transaction date.
     * Useful for determining last sync time.
     *
     * @return Result<Long?> containing timestamp of most recent transaction or error information
     */
    suspend fun getLatestTransactionDate(): Result<Long?>

    // ==================== Category Override Operations ====================

    /**
     * Sets a category override for a specific transaction.
     *
     * @param smsId The SMS ID of the transaction
     * @param categoryId The category ID to override with
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun setCategoryOverride(smsId: Long, categoryId: String): Result<Unit>

    /**
     * Gets the category override for a specific transaction.
     *
     * @param smsId The SMS ID of the transaction
     * @return Result<String?> containing category ID override or error information
     */
    suspend fun getCategoryOverride(smsId: Long): Result<String?>

    /**
     * Gets all category overrides as a Map.
     *
     * @return Result<Map<Long, String>> containing map of SMS ID to category ID or error information
     */
    suspend fun getAllCategoryOverrides(): Result<Map<Long, String>>

    /**
     * Gets all category overrides as a reactive Flow.
     *
     * @return Flow emitting Map of SMS ID to category ID
     */
    fun getAllCategoryOverridesFlow(): Flow<Map<Long, String>>

    /**
     * Removes a category override for a specific transaction.
     *
     * @param smsId The SMS ID of the transaction
     * @return Result<Boolean> indicating if override was removed or error information
     */
    suspend fun removeCategoryOverride(smsId: Long): Result<Boolean>

    /**
     * Removes all category overrides.
     *
     * @return Result<Int> containing number of overrides removed or error information
     */
    suspend fun removeAllCategoryOverrides(): Result<Int>

    // ==================== SMS Sync Operations ====================

    /**
     * Syncs transactions from SMS messages.
     *
     * This method orchestrates the complete SMS sync workflow:
     * 1. Read SMS messages from device inbox (last N days or since last sync)
     * 2. Filter to bank transaction SMS
     * 3. Parse SMS into structured transactions
     * 4. Deduplicate and filter out existing transactions
     * 5. Save new transactions to database
     * 6. Update sync metadata timestamp
     *
     * The method performs incremental sync automatically if last sync timestamp exists,
     * otherwise performs a full sync for the specified number of days.
     *
     * @param daysAgo Number of days to look back for full sync (default: 30)
     * @return Result<SyncResult> containing sync statistics or error information
     */
    suspend fun syncFromSms(daysAgo: Int = 30): Result<SyncResult>

    /**
     * Performs an incremental SMS sync since the last sync timestamp.
     *
     * This is an optimized version of syncFromSms() that only processes new SMS
     * messages since the last successful sync. If no previous sync exists, it falls
     * back to a full sync.
     *
     * @param daysAgoFallback Number of days to look back if no last sync exists (default: 30)
     * @return Result<SyncResult> containing sync statistics or error information
     */
    suspend fun syncFromSmsIncremental(daysAgoFallback: Int = 30): Result<SyncResult>

    // ==================== Sync Metadata Operations ====================

    /**
     * Gets the last sync timestamp.
     * This indicates when transactions were last synced from SMS.
     *
     * @return Result<Long?> containing last sync timestamp in milliseconds or error information
     */
    suspend fun getLastSyncTimestamp(): Result<Long?>

    /**
     * Sets the last sync timestamp.
     * Should be called after successfully syncing transactions.
     *
     * @param timestamp The sync timestamp in milliseconds
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun setLastSyncTimestamp(timestamp: Long): Result<Unit>

    /**
     * Gets the last processed SMS ID.
     * This can be used to track which SMS messages have been parsed.
     *
     * @return Result<Long?> containing last processed SMS ID or error information
     */
    suspend fun getLastProcessedSmsId(): Result<Long?>

    /**
     * Sets the last processed SMS ID.
     * Should be called after successfully processing an SMS.
     *
     * @param smsId The SMS ID that was processed
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun setLastProcessedSmsId(smsId: Long): Result<Unit>

    /**
     * Clears all sync metadata.
     * Useful for forcing a full re-sync.
     *
     * @return Result<Int> containing number of metadata entries removed or error information
     */
    suspend fun clearSyncMetadata(): Result<Int>
}
