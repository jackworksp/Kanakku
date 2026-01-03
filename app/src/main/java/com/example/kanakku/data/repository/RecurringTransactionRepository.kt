package com.example.kanakku.data.repository

import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.toDomain
import com.example.kanakku.data.database.toEntity
import com.example.kanakku.data.model.RecurringFrequency
import com.example.kanakku.data.model.RecurringTransaction
import com.example.kanakku.data.model.RecurringType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Repository for managing recurring transaction patterns.
 *
 * This repository acts as a bridge between the domain layer (RecurringTransaction)
 * and the data layer (RecurringTransactionEntity), handling all entity-model mapping
 * and database operations.
 *
 * Key responsibilities:
 * - Save and retrieve recurring transaction patterns
 * - Filter recurring transactions by frequency and type
 * - Manage upcoming recurring transactions
 * - Handle user confirmation/removal of recurring patterns
 * - Update next expected dates after transaction occurrences
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
class RecurringTransactionRepository(private val database: KanakkuDatabase) {

    // DAO for database access
    private val recurringTransactionDao = database.recurringTransactionDao()

    // ==================== CRUD Operations ====================

    /**
     * Saves a single recurring transaction pattern to the database.
     * Converts the domain model to an entity before persisting.
     *
     * @param recurringTransaction The recurring transaction to save
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun saveRecurringTransaction(recurringTransaction: RecurringTransaction): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Save recurring transaction") {
            recurringTransactionDao.insert(recurringTransaction.toEntity())
        }
    }

    /**
     * Saves multiple recurring transactions to the database in a single operation.
     * More efficient than saving individually for bulk operations.
     *
     * @param recurringTransactions List of recurring transactions to save
     * @return Result<Unit> indicating success or failure with error information
     */
    suspend fun saveRecurringTransactions(recurringTransactions: List<RecurringTransaction>): Result<Unit> {
        return ErrorHandler.runSuspendCatching("Save recurring transactions") {
            recurringTransactionDao.insertAll(recurringTransactions.map { it.toEntity() })
        }
    }

    /**
     * Retrieves all recurring transactions as a reactive Flow.
     * Automatically converts entities to domain models.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @return Flow emitting list of recurring transactions, sorted by next expected date
     */
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getAllRecurringTransactions()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get all recurring transactions")
                emit(emptyList())
            }
    }

    /**
     * Retrieves all recurring transactions as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return Result<List<RecurringTransaction>> containing recurring transactions or error information
     */
    suspend fun getAllRecurringTransactionsSnapshot(): Result<List<RecurringTransaction>> {
        return ErrorHandler.runSuspendCatching("Get all recurring transactions snapshot") {
            recurringTransactionDao.getAllRecurringTransactionsSnapshot()
                .map { it.toDomain() }
        }
    }

    /**
     * Retrieves a specific recurring transaction by its ID.
     *
     * @param id The unique identifier of the recurring transaction
     * @return Result<RecurringTransaction?> containing the recurring transaction or error information
     */
    suspend fun getRecurringTransactionById(id: String): Result<RecurringTransaction?> {
        return ErrorHandler.runSuspendCatching("Get recurring transaction by ID") {
            recurringTransactionDao.getById(id)?.toDomain()
        }
    }

    /**
     * Deletes a recurring transaction by its ID.
     *
     * @param id The unique identifier of the recurring transaction to delete
     * @return Result<Boolean> indicating if transaction was deleted or error information
     */
    suspend fun deleteRecurringTransaction(id: String): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Delete recurring transaction") {
            recurringTransactionDao.deleteById(id) > 0
        }
    }

    /**
     * Deletes all recurring transactions from the database.
     * Use with caution - this cannot be undone.
     *
     * @return Result<Int> containing number of recurring transactions deleted or error information
     */
    suspend fun deleteAllRecurringTransactions(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Delete all recurring transactions") {
            recurringTransactionDao.deleteAll()
        }
    }

    /**
     * Checks if a recurring transaction with the given ID exists.
     *
     * @param id The ID to check
     * @return Result<Boolean> indicating if recurring transaction exists or error information
     */
    suspend fun recurringTransactionExists(id: String): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Check recurring transaction exists") {
            recurringTransactionDao.exists(id)
        }
    }

    // ==================== Query Operations ====================

    /**
     * Retrieves recurring transactions filtered by frequency type.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @param frequency The recurring frequency to filter by (Weekly, Monthly, etc.)
     * @return Flow emitting list of recurring transactions matching the frequency
     */
    fun getRecurringTransactionsByFrequency(frequency: RecurringFrequency): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getRecurringTransactionsByFrequency(frequency)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get recurring transactions by frequency: $frequency")
                emit(emptyList())
            }
    }

    /**
     * Retrieves recurring transactions filtered by type.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @param type The recurring type to filter by (Subscription, EMI, etc.)
     * @return Flow emitting list of recurring transactions matching the type
     */
    fun getRecurringTransactionsByType(type: RecurringType): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getRecurringTransactionsByType(type)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get recurring transactions by type: $type")
                emit(emptyList())
            }
    }

    /**
     * Retrieves upcoming recurring transactions where next expected date is in the future.
     * Useful for showing users what payments are coming up.
     *
     * @param currentTimestamp Current time in milliseconds (defaults to System.currentTimeMillis())
     * @return Flow emitting list of upcoming recurring transactions
     */
    fun getUpcomingRecurringTransactions(currentTimestamp: Long = System.currentTimeMillis()): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getUpcomingRecurringTransactions(currentTimestamp)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get upcoming recurring transactions")
                emit(emptyList())
            }
    }

    /**
     * Retrieves upcoming recurring transactions within a specific time window.
     * For example, get all recurring transactions expected in the next 30 days.
     *
     * @param startTimestamp Start timestamp (inclusive)
     * @param endTimestamp End timestamp (inclusive)
     * @return Flow emitting list of recurring transactions expected within the time window
     */
    fun getRecurringTransactionsInWindow(startTimestamp: Long, endTimestamp: Long): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getRecurringTransactionsInWindow(startTimestamp, endTimestamp)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get recurring transactions in window")
                emit(emptyList())
            }
    }

    /**
     * Retrieves recurring transactions that match a specific merchant pattern.
     *
     * @param merchantPattern The merchant pattern to search for
     * @return Result<List<RecurringTransaction>> containing matching recurring transactions or error information
     */
    suspend fun getRecurringTransactionsByMerchantPattern(merchantPattern: String): Result<List<RecurringTransaction>> {
        return ErrorHandler.runSuspendCatching("Get recurring transactions by merchant pattern") {
            recurringTransactionDao.getByMerchantPattern(merchantPattern)
                .map { it.toDomain() }
        }
    }

    /**
     * Retrieves only user-confirmed recurring transactions.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @return Flow emitting list of user-confirmed recurring transactions
     */
    fun getConfirmedRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getConfirmedRecurringTransactions()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get confirmed recurring transactions")
                emit(emptyList())
            }
    }

    /**
     * Retrieves only auto-detected (not user-confirmed) recurring transactions.
     * Errors are logged and an empty list is emitted on failure.
     *
     * @return Flow emitting list of auto-detected recurring transactions
     */
    fun getUnconfirmedRecurringTransactions(): Flow<List<RecurringTransaction>> {
        return recurringTransactionDao.getUnconfirmedRecurringTransactions()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { throwable ->
                ErrorHandler.handleError(throwable as Exception, "Get unconfirmed recurring transactions")
                emit(emptyList())
            }
    }

    /**
     * Gets the next expected recurring transaction (closest upcoming one).
     *
     * @param currentTimestamp Current time in milliseconds (defaults to System.currentTimeMillis())
     * @return Result<RecurringTransaction?> containing next recurring transaction or error information
     */
    suspend fun getNextUpcomingTransaction(currentTimestamp: Long = System.currentTimeMillis()): Result<RecurringTransaction?> {
        return ErrorHandler.runSuspendCatching("Get next upcoming transaction") {
            recurringTransactionDao.getNextUpcomingTransaction(currentTimestamp)?.toDomain()
        }
    }

    // ==================== Update Operations ====================

    /**
     * Updates the user confirmation status for a recurring transaction.
     * This allows users to confirm auto-detected patterns or mark patterns as not recurring.
     *
     * @param id The ID of the recurring transaction
     * @param isConfirmed Whether the user has confirmed this recurring pattern
     * @return Result<Boolean> indicating if update was successful or error information
     */
    suspend fun updateConfirmationStatus(id: String, isConfirmed: Boolean): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Update confirmation status") {
            recurringTransactionDao.updateConfirmationStatus(id, isConfirmed) > 0
        }
    }

    /**
     * Updates the next expected date for a recurring transaction.
     * Useful after a transaction occurrence is detected to update the prediction.
     *
     * @param id The ID of the recurring transaction
     * @param nextExpected New next expected timestamp
     * @return Result<Boolean> indicating if update was successful or error information
     */
    suspend fun updateNextExpected(id: String, nextExpected: Long): Result<Boolean> {
        return ErrorHandler.runSuspendCatching("Update next expected date") {
            recurringTransactionDao.updateNextExpected(id, nextExpected) > 0
        }
    }

    // ==================== Statistics Operations ====================

    /**
     * Gets the total count of recurring transactions in the database.
     *
     * @return Result<Int> containing total number of recurring transactions or error information
     */
    suspend fun getRecurringTransactionCount(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Get recurring transaction count") {
            recurringTransactionDao.getRecurringTransactionCount()
        }
    }

    /**
     * Gets the total count of user-confirmed recurring transactions.
     *
     * @return Result<Int> containing total number of confirmed recurring transactions or error information
     */
    suspend fun getConfirmedRecurringTransactionCount(): Result<Int> {
        return ErrorHandler.runSuspendCatching("Get confirmed recurring transaction count") {
            recurringTransactionDao.getConfirmedRecurringTransactionCount()
        }
    }

    /**
     * Calculates the total monthly recurring expense amount.
     * Converts all recurring transactions to monthly equivalents and sums them.
     *
     * @return Result<Double> containing estimated monthly recurring total or error information
     */
    suspend fun calculateMonthlyRecurringTotal(): Result<Double> {
        return ErrorHandler.runSuspendCatching("Calculate monthly recurring total") {
            val recurringTransactions = recurringTransactionDao.getAllRecurringTransactionsSnapshot()
                .map { it.toDomain() }

            recurringTransactions.sumOf { recurring ->
                when (recurring.frequency) {
                    RecurringFrequency.WEEKLY -> recurring.amount * 4.33 // Average weeks per month
                    RecurringFrequency.BI_WEEKLY -> recurring.amount * 2.17 // Bi-weekly to monthly
                    RecurringFrequency.MONTHLY -> recurring.amount
                    RecurringFrequency.QUARTERLY -> recurring.amount / 3.0
                    RecurringFrequency.ANNUAL -> recurring.amount / 12.0
                }
            }
        }
    }

    /**
     * Calculates the total monthly recurring expense amount for confirmed transactions only.
     * Converts all confirmed recurring transactions to monthly equivalents and sums them.
     *
     * @return Result<Double> containing estimated monthly confirmed recurring total or error information
     */
    suspend fun calculateConfirmedMonthlyRecurringTotal(): Result<Double> {
        return ErrorHandler.runSuspendCatching("Calculate confirmed monthly recurring total") {
            val recurringTransactions = recurringTransactionDao.getAllRecurringTransactionsSnapshot()
                .map { it.toDomain() }
                .filter { it.isUserConfirmed }

            recurringTransactions.sumOf { recurring ->
                when (recurring.frequency) {
                    RecurringFrequency.WEEKLY -> recurring.amount * 4.33
                    RecurringFrequency.BI_WEEKLY -> recurring.amount * 2.17
                    RecurringFrequency.MONTHLY -> recurring.amount
                    RecurringFrequency.QUARTERLY -> recurring.amount / 3.0
                    RecurringFrequency.ANNUAL -> recurring.amount / 12.0
                }
            }
        }
    }
}
