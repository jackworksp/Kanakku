package com.example.kanakku.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kanakku.data.database.entity.RecurringTransactionEntity
import com.example.kanakku.data.model.RecurringFrequency
import com.example.kanakku.data.model.RecurringType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for recurring transaction operations.
 * Provides CRUD operations for persisting and querying recurring transaction patterns.
 */
@Dao
interface RecurringTransactionDao {

    /**
     * Inserts a single recurring transaction into the database.
     * If a recurring transaction with the same id exists, it will be replaced.
     *
     * @param recurringTransaction The recurring transaction entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringTransaction: RecurringTransactionEntity)

    /**
     * Inserts multiple recurring transactions into the database in a single transaction.
     * If recurring transactions with the same id exist, they will be replaced.
     *
     * @param recurringTransactions List of recurring transaction entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recurringTransactions: List<RecurringTransactionEntity>)

    /**
     * Retrieves all recurring transactions sorted by next expected date in ascending order.
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of all recurring transactions
     */
    @Query("SELECT * FROM recurring_transactions ORDER BY nextExpected ASC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    /**
     * Retrieves all recurring transactions as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return List of all recurring transactions
     */
    @Query("SELECT * FROM recurring_transactions ORDER BY nextExpected ASC")
    suspend fun getAllRecurringTransactionsSnapshot(): List<RecurringTransactionEntity>

    /**
     * Retrieves a specific recurring transaction by its ID.
     *
     * @param id The unique identifier of the recurring transaction
     * @return The recurring transaction entity, or null if not found
     */
    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: String): RecurringTransactionEntity?

    /**
     * Retrieves recurring transactions filtered by frequency type.
     * Returns a Flow for reactive updates.
     *
     * @param frequency The recurring frequency to filter by
     * @return Flow emitting list of recurring transactions matching the frequency
     */
    @Query("SELECT * FROM recurring_transactions WHERE frequency = :frequency ORDER BY nextExpected ASC")
    fun getRecurringTransactionsByFrequency(frequency: RecurringFrequency): Flow<List<RecurringTransactionEntity>>

    /**
     * Retrieves recurring transactions filtered by type (Subscription, EMI, etc.).
     * Returns a Flow for reactive updates.
     *
     * @param type The recurring type to filter by
     * @return Flow emitting list of recurring transactions matching the type
     */
    @Query("SELECT * FROM recurring_transactions WHERE type = :type ORDER BY nextExpected ASC")
    fun getRecurringTransactionsByType(type: RecurringType): Flow<List<RecurringTransactionEntity>>

    /**
     * Retrieves upcoming recurring transactions where next expected date is in the future.
     * Useful for showing users what payments are coming up.
     *
     * @param currentTimestamp Current time in milliseconds
     * @return Flow emitting list of upcoming recurring transactions
     */
    @Query("SELECT * FROM recurring_transactions WHERE nextExpected > :currentTimestamp ORDER BY nextExpected ASC")
    fun getUpcomingRecurringTransactions(currentTimestamp: Long): Flow<List<RecurringTransactionEntity>>

    /**
     * Retrieves upcoming recurring transactions within a specific time window.
     * For example, get all recurring transactions expected in the next 30 days.
     *
     * @param startTimestamp Start timestamp (inclusive)
     * @param endTimestamp End timestamp (inclusive)
     * @return Flow emitting list of recurring transactions expected within the time window
     */
    @Query("SELECT * FROM recurring_transactions WHERE nextExpected BETWEEN :startTimestamp AND :endTimestamp ORDER BY nextExpected ASC")
    fun getRecurringTransactionsInWindow(startTimestamp: Long, endTimestamp: Long): Flow<List<RecurringTransactionEntity>>

    /**
     * Retrieves recurring transactions that match a specific merchant pattern.
     *
     * @param merchantPattern The merchant pattern to search for
     * @return List of recurring transactions matching the merchant pattern
     */
    @Query("SELECT * FROM recurring_transactions WHERE merchantPattern = :merchantPattern")
    suspend fun getByMerchantPattern(merchantPattern: String): List<RecurringTransactionEntity>

    /**
     * Retrieves only user-confirmed recurring transactions.
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of user-confirmed recurring transactions
     */
    @Query("SELECT * FROM recurring_transactions WHERE isUserConfirmed = 1 ORDER BY nextExpected ASC")
    fun getConfirmedRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    /**
     * Retrieves only auto-detected (not user-confirmed) recurring transactions.
     * Returns a Flow for reactive updates.
     *
     * @return Flow emitting list of auto-detected recurring transactions
     */
    @Query("SELECT * FROM recurring_transactions WHERE isUserConfirmed = 0 ORDER BY nextExpected ASC")
    fun getUnconfirmedRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    /**
     * Updates the user confirmation status for a recurring transaction.
     *
     * @param id The ID of the recurring transaction
     * @param isConfirmed Whether the user has confirmed this recurring pattern
     * @return Number of rows updated (0 if not found, 1 if updated)
     */
    @Query("UPDATE recurring_transactions SET isUserConfirmed = :isConfirmed WHERE id = :id")
    suspend fun updateConfirmationStatus(id: String, isConfirmed: Boolean): Int

    /**
     * Updates the next expected date for a recurring transaction.
     * Useful after a transaction occurrence is detected.
     *
     * @param id The ID of the recurring transaction
     * @param nextExpected New next expected timestamp
     * @return Number of rows updated (0 if not found, 1 if updated)
     */
    @Query("UPDATE recurring_transactions SET nextExpected = :nextExpected WHERE id = :id")
    suspend fun updateNextExpected(id: String, nextExpected: Long): Int

    /**
     * Deletes a recurring transaction by its ID.
     *
     * @param id The unique identifier of the recurring transaction to delete
     * @return Number of rows deleted (0 if not found, 1 if deleted)
     */
    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: String): Int

    /**
     * Deletes all recurring transactions from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAll(): Int

    /**
     * Checks if a recurring transaction with the given ID exists.
     *
     * @param id The unique identifier to check
     * @return True if recurring transaction exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM recurring_transactions WHERE id = :id LIMIT 1)")
    suspend fun exists(id: String): Boolean

    /**
     * Gets the total count of recurring transactions in the database.
     *
     * @return Total number of recurring transactions
     */
    @Query("SELECT COUNT(*) FROM recurring_transactions")
    suspend fun getRecurringTransactionCount(): Int

    /**
     * Gets the total count of user-confirmed recurring transactions.
     *
     * @return Total number of confirmed recurring transactions
     */
    @Query("SELECT COUNT(*) FROM recurring_transactions WHERE isUserConfirmed = 1")
    suspend fun getConfirmedRecurringTransactionCount(): Int

    /**
     * Gets the next expected recurring transaction (closest upcoming one).
     *
     * @param currentTimestamp Current time in milliseconds
     * @return The next upcoming recurring transaction, or null if none exist
     */
    @Query("SELECT * FROM recurring_transactions WHERE nextExpected > :currentTimestamp ORDER BY nextExpected ASC LIMIT 1")
    suspend fun getNextUpcomingTransaction(currentTimestamp: Long): RecurringTransactionEntity?
}
