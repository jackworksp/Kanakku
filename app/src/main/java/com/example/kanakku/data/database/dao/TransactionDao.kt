package com.example.kanakku.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for transaction operations.
 * Provides CRUD operations for persisting and querying transactions.
 */
@Dao
interface TransactionDao {

    /**
     * Inserts a single transaction into the database.
     * If a transaction with the same smsId exists, it will be replaced.
     *
     * @param transaction The transaction entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    /**
     * Inserts multiple transactions into the database in a single transaction.
     * If transactions with the same smsId exist, they will be replaced.
     *
     * @param transactions List of transaction entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    /**
     * Retrieves all transactions sorted by date in descending order (newest first).
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of all transactions
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    /**
     * Retrieves all transactions as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return List of all transactions
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity>

    /**
     * Retrieves transactions filtered by type (DEBIT/CREDIT/UNKNOWN).
     * Returns a Flow for reactive updates.
     *
     * @param type The transaction type to filter by
     * @return Flow emitting list of transactions matching the type
     */
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    /**
     * Retrieves transactions within a specific date range.
     * Both start and end timestamps are inclusive.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Flow emitting list of transactions within the date range
     */
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    /**
     * Retrieves transactions after a specific timestamp.
     * Useful for incremental sync operations.
     *
     * @param timestamp The timestamp to query after
     * @return List of transactions after the specified timestamp
     */
    @Query("SELECT * FROM transactions WHERE date > :timestamp ORDER BY date ASC")
    suspend fun getTransactionsAfter(timestamp: Long): List<TransactionEntity>

    /**
     * Deletes a transaction by its SMS ID.
     *
     * @param smsId The SMS ID of the transaction to delete
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM transactions WHERE smsId = :smsId")
    suspend fun deleteById(smsId: Long): Int

    /**
     * Deletes all transactions from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM transactions")
    suspend fun deleteAll(): Int

    /**
     * Checks if a transaction with the given SMS ID exists.
     *
     * @param smsId The SMS ID to check
     * @return True if transaction exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE smsId = :smsId LIMIT 1)")
    suspend fun exists(smsId: Long): Boolean

    /**
     * Gets the total count of transactions in the database.
     *
     * @return Total number of transactions
     */
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    /**
     * Gets the most recent transaction date.
     * Useful for determining last sync time.
     *
     * @return The timestamp of the most recent transaction, or null if no transactions exist
     */
    @Query("SELECT MAX(date) FROM transactions")
    suspend fun getLatestTransactionDate(): Long?

    /**
     * Gets the oldest transaction date.
     * Useful for determining the start of transaction history.
     *
     * @return The timestamp of the oldest transaction, or null if no transactions exist
     */
    @Query("SELECT MIN(date) FROM transactions")
    suspend fun getOldestTransactionDate(): Long?

    /**
     * Gets the count of transactions within a specific date range.
     * Both start and end timestamps are inclusive.
     * Uses the indexed date column for optimal performance.
     *
     * @param startDate Start timestamp (inclusive)
     * @param endDate End timestamp (inclusive)
     * @return Count of transactions within the date range
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTransactionCountByDateRange(startDate: Long, endDate: Long): Int
}
