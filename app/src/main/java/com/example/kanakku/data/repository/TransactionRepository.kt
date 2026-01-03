package com.example.kanakku.data.repository

import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.entity.CategoryOverrideEntity
import com.example.kanakku.data.database.entity.SyncMetadataEntity
import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.database.toDomain
import com.example.kanakku.data.database.toEntity
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionFilter
import com.example.kanakku.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
 * - Manage merchant-to-category mappings for automatic categorization
 * - Track sync metadata for incremental updates
 * - Provide reactive data streams via Flow
 * - Handle all database errors gracefully with comprehensive error handling
 * - Provide in-memory caching for frequently accessed data to improve performance
 *
 * Error Handling Strategy:
 * - All database operations wrapped in try-catch blocks via ErrorHandler
 * - Errors logged for debugging with appropriate context
 * - Suspend functions return Result<T> for error propagation to callers
 * - Flow operations include .catch() to emit fallback values on error
 * - No uncaught exceptions - all errors are handled gracefully
 *
 * Caching Strategy:
 * - Frequently accessed data cached in memory to reduce database reads
 * - Cache invalidated automatically when data changes (save, delete operations)
 * - Thread-safe cache access using Mutex for coroutine synchronization
 * - Cache failures are transparent - falls back to database on cache miss
 * - Improves performance for common operations like getAllTransactions()
 *
 * @param database The Room database instance
 */
class TransactionRepository(private val database: KanakkuDatabase) {

    // DAOs for database access
    private val transactionDao = database.transactionDao()
    private val categoryOverrideDao = database.categoryOverrideDao()
    private val syncMetadataDao = database.syncMetadataDao()
    private val merchantCategoryMappingDao = database.merchantCategoryMappingDao()

    // ==================== In-Memory Cache ====================

    /**
     * Cache for frequently accessed data to reduce database queries.
     * All cache access is synchronized using cacheMutex to ensure thread safety.
     */
    private var transactionsCache: List<ParsedTransaction>? = null
    private var transactionCountCache: Int? = null
    private var latestTransactionDateCache: Long? = null

    /**
     * Mutex for thread-safe cache access.
     * Ensures cache consistency when accessed from multiple coroutines.
     */
    private val cacheMutex = Mutex()

    /**
     * Invalidates all cached data.
     * Should be called after any operation that modifies transactions.
     */
    private suspend fun invalidateCache() {
        cacheMutex.withLock {
            transactionsCache = null
            transactionCountCache = null
            latestTransactionDateCache = null
        }
        ErrorHandler.logDebug("Cache invalidated", "TransactionRepository")
    }

    /**
     * Updates the transactions cache with fresh data.
     * Thread-safe operation using cacheMutex.
     *
     * @param transactions The transactions to cache
     */
    private suspend fun updateTransactionsCache(transactions: List<ParsedTransaction>) {
        cacheMutex.withLock {
            transactionsCache = transactions
            transactionCountCache = transactions.size
            // Update latest date from cached transactions
            latestTransactionDateCache = transactions.maxOfOrNull { it.date }
        }
        ErrorHandler.logDebug(
            "Cache updated: ${transactions.size} transactions",
            "TransactionRepository"
        )
    }

    // Metadata keys
    companion object {
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_LAST_PROCESSED_SMS_ID = "last_processed_sms_id"
    }

    // ==================== Transaction Operations ====================

    /**
     * Saves a single transaction to the database.
     * Converts the domain model to an entity before persisting.
     * Invalidates cache after successful save to ensure data consistency.
     *
     * @param transaction The parsed transaction to save
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun saveTransaction(transaction: ParsedTransaction): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Save transaction") {
            transactionDao.insert(transaction.toEntity())
            invalidateCache()
        }
    }

    /**
     * Saves multiple transactions to the database in a single operation.
     * More efficient than saving individually for bulk imports.
     * Invalidates cache after successful save to ensure data consistency.
     *
     * @param transactions List of parsed transactions to save
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun saveTransactions(transactions: List<ParsedTransaction>): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Save transactions") {
            transactionDao.insertAll(transactions.map { it.toEntity() })
            invalidateCache()
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
     * Uses in-memory cache to reduce database reads - falls back to database on cache miss.
     *
     * @return Result<List<ParsedTransaction>> containing transactions or error information
     */
    suspend fun getAllTransactionsSnapshot(): Result<List<ParsedTransaction>> {
        return ErrorHandler.runSuspendCatching("Get all transactions snapshot") {
            // Check cache first
            val cached = cacheMutex.withLock { transactionsCache }
            if (cached != null) {
                ErrorHandler.logDebug("Cache hit: getAllTransactionsSnapshot", "TransactionRepository")
                return@runSuspendCatching cached
            }

            // Cache miss - fetch from database
            ErrorHandler.logDebug("Cache miss: getAllTransactionsSnapshot", "TransactionRepository")
            val transactions = transactionDao.getAllTransactionsSnapshot()
                .map { it.toDomain() }

            // Update cache with fresh data
            updateTransactionsCache(transactions)

            transactions
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
     * Invalidates cache after successful deletion to ensure data consistency.
     *
     * @param smsId The SMS ID of the transaction to delete
     * @return Result<Boolean> indicating if transaction was deleted or error information
     */
    suspend fun deleteTransaction(smsId: Long): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Delete transaction") {
            val deleted = transactionDao.deleteById(smsId) > 0
            if (deleted) {
                invalidateCache()
            }
            deleted
        }
    }

    /**
     * Deletes all transactions from the database.
     * Use with caution - this cannot be undone.
     * Invalidates cache after successful deletion to ensure data consistency.
     *
     * @return Result<Int> containing number of transactions deleted or error information
     */
    suspend fun deleteAllTransactions(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Delete all transactions") {
            val deletedCount = transactionDao.deleteAll()
            if (deletedCount > 0) {
                invalidateCache()
            }
            deletedCount
        }
    }

    /**
     * Gets the total count of transactions in the database.
     * Uses in-memory cache to reduce database reads - falls back to database on cache miss.
     *
     * @return Result<Int> containing total number of transactions or error information
     */
    suspend fun getTransactionCount(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Get transaction count") {
            // Check cache first
            val cached = cacheMutex.withLock { transactionCountCache }
            if (cached != null) {
                ErrorHandler.logDebug("Cache hit: getTransactionCount", "TransactionRepository")
                return@runSuspendCatching cached
            }

            // Cache miss - fetch from database
            ErrorHandler.logDebug("Cache miss: getTransactionCount", "TransactionRepository")
            val count = transactionDao.getTransactionCount()

            // Update cache
            cacheMutex.withLock { transactionCountCache = count }

            count
        }
    }

    /**
     * Gets the most recent transaction date.
     * Useful for determining last sync time.
     * Uses in-memory cache to reduce database reads - falls back to database on cache miss.
     *
     * @return Result<Long?> containing timestamp of most recent transaction or error information
     */
    suspend fun getLatestTransactionDate(): Result<Long?> {
        return ErrorHandler.runSuspendCatching("Get latest transaction date") {
            // Check cache first
            val cached = cacheMutex.withLock { latestTransactionDateCache }
            if (cached != null) {
                ErrorHandler.logDebug("Cache hit: getLatestTransactionDate", "TransactionRepository")
                return@runSuspendCatching cached
            }

            // Cache miss - fetch from database
            ErrorHandler.logDebug("Cache miss: getLatestTransactionDate", "TransactionRepository")
            val latestDate = transactionDao.getLatestTransactionDate()

            // Update cache
            cacheMutex.withLock { latestTransactionDateCache = latestDate }

            latestDate
        }
    }

    // ==================== Search and Filter Operations ====================

    /**
     * Retrieves transactions filtered by the provided filter criteria.
     * Supports filtering by search query, transaction type, category, date range, and amount range.
     * All filter parameters are optional - only active filters are applied.
     *
     * @param filter The TransactionFilter containing all filter parameters
     * @return Result<List<ParsedTransaction>> containing filtered transactions or error information
     */
    suspend fun getFilteredTransactions(filter: TransactionFilter): Result<List<ParsedTransaction>> {
        return ErrorHandler.runSuspendCatching("Get filtered transactions") {
            // Extract filter parameters
            val searchQuery = filter.searchQuery?.takeIf { it.isNotBlank() }
            val type = filter.transactionType
            val startDate = filter.dateRange?.first
            val endDate = filter.dateRange?.second
            val minAmount = filter.amountRange?.first
            val maxAmount = filter.amountRange?.second

            // Call DAO with extracted parameters
            val entities = transactionDao.searchAndFilterTransactions(
                searchQuery = searchQuery,
                type = type,
                startDate = startDate,
                endDate = endDate,
                minAmount = minAmount,
                maxAmount = maxAmount
            )

            // Map entities to domain models
            entities.map { it.toDomain() }
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

    // ==================== Merchant Category Mapping Operations ====================

    /**
     * Normalizes a merchant name for consistent mapping storage.
     * Converts to lowercase, trims whitespace, and removes special characters.
     *
     * @param merchantName The raw merchant name from transaction
     * @return Normalized merchant name (lowercase, trimmed, alphanumeric + spaces only)
     */
    private fun normalizeMerchantName(merchantName: String): String {
        return merchantName
            .lowercase()
            .trim()
            .replace(Regex("[^a-z0-9\\s]"), "") // Keep only alphanumeric and spaces
            .replace(Regex("\\s+"), " ") // Normalize multiple spaces to single space
    }

    /**
     * Sets a merchant-to-category mapping.
     * This learns the user's preference for categorizing transactions from a specific merchant.
     * The merchant name is normalized before storing to ensure consistency.
     *
     * @param merchantName The merchant name to map
     * @param categoryId The category ID to associate with this merchant
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun setMerchantCategoryMapping(merchantName: String, categoryId: String): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Set merchant category mapping") {
            val normalizedName = normalizeMerchantName(merchantName)
            if (normalizedName.isBlank()) {
                throw IllegalArgumentException("Merchant name cannot be empty after normalization")
            }
            val mapping = com.example.kanakku.data.database.entity.MerchantCategoryMappingEntity(
                merchantName = normalizedName,
                categoryId = categoryId,
                updatedAt = System.currentTimeMillis()
            )
            merchantCategoryMappingDao.insert(mapping)
        }
    }

    /**
     * Gets the learned category mapping for a specific merchant.
     * The merchant name is normalized before lookup to ensure consistency.
     *
     * @param merchantName The merchant name to look up
     * @return Result<String?> containing category ID if mapping exists, null otherwise, or error information
     */
    suspend fun getMerchantCategoryMapping(merchantName: String): Result<String?> {
        return ErrorHandler.runSuspendCatching("Get merchant category mapping") {
            val normalizedName = normalizeMerchantName(merchantName)
            if (normalizedName.isBlank()) {
                return@runSuspendCatching null
            }
            merchantCategoryMappingDao.getMapping(normalizedName)?.categoryId
        }
    }

    /**
     * Gets all merchant category mappings as a reactive Flow.
     * Returns a map of normalized merchant names to category IDs.
     * Errors are logged and an empty map is emitted on failure.
     *
     * @return Flow emitting Map of merchant name to category ID
     */
    fun getAllMerchantMappings(): Flow<Map<String, String>> {
        return merchantCategoryMappingDao.getAllMappings()
            .map { mappings -> mappings.associate { it.merchantName to it.categoryId } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get all merchant mappings")
                emit(emptyMap())
            }
    }

    /**
     * Gets all merchant category mappings as a one-time snapshot.
     * Returns a map of normalized merchant names to category IDs.
     * Useful for creating an in-memory cache in CategoryManager.
     *
     * @return Result<Map<String, String>> containing map of merchant name to category ID or error information
     */
    suspend fun getAllMerchantMappingsSnapshot(): Result<Map<String, String>> {
        return ErrorHandler.runSuspendCatching("Get all merchant mappings snapshot") {
            merchantCategoryMappingDao.getAllMappingsSnapshot()
                .associate { it.merchantName to it.categoryId }
        }
    }

    /**
     * Removes a merchant category mapping.
     * The merchant name is normalized before deletion to ensure consistency.
     *
     * @param merchantName The merchant name to remove mapping for
     * @return Result<Boolean> indicating if mapping was removed or error information
     */
    suspend fun removeMerchantMapping(merchantName: String): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Remove merchant mapping") {
            val normalizedName = normalizeMerchantName(merchantName)
            if (normalizedName.isBlank()) {
                return@runSuspendCatching false
            }
            merchantCategoryMappingDao.deleteMapping(normalizedName) > 0
        }
    }

    /**
     * Removes all merchant category mappings.
     * Use with caution - this clears all learned merchant preferences.
     *
     * @return Result<Int> containing number of mappings removed or error information
     */
    suspend fun removeAllMerchantMappings(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Remove all merchant mappings") {
            merchantCategoryMappingDao.deleteAll()
        }
    }

    /**
     * Gets the total count of merchant category mappings.
     * Useful for displaying how many merchants have been learned.
     *
     * @return Result<Int> containing total number of merchant mappings or error information
     */
    suspend fun getMerchantMappingCount(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Get merchant mapping count") {
            merchantCategoryMappingDao.getMappingCount()
        }
    }
}
