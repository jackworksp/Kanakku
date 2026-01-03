package com.example.kanakku.ui.recurring

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.core.error.toErrorInfo
import com.example.kanakku.data.database.DatabaseProvider
import com.example.kanakku.data.model.RecurringTransaction
import com.example.kanakku.data.model.RecurringType
import com.example.kanakku.data.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * ViewModel for managing recurring transactions screen.
 *
 * This ViewModel handles the state and business logic for displaying and managing
 * recurring transaction patterns, including:
 * - Loading recurring transactions from repository
 * - Managing user confirmation/removal actions
 * - Calculating summary statistics (monthly totals, counts by type)
 * - Calculating upcoming payments within time windows
 *
 * Error Handling:
 * - All repository operations return Result<T> and are handled gracefully
 * - Errors displayed to user with clear, actionable messages
 * - Failed operations don't crash the app, partial data is shown when possible
 *
 * Architecture:
 * - Follows MVVM pattern with single source of truth (RecurringUiState)
 * - Reactive UI updates via StateFlow
 * - Repository abstraction for data layer access
 * - Offline-first with no network dependencies
 */
class RecurringViewModel : ViewModel() {

    // Internal mutable state
    private val _uiState = MutableStateFlow(RecurringUiState())

    // Public read-only state for UI
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    // Repository instance (initialized lazily when context is provided)
    private var repository: RecurringTransactionRepository? = null

    /**
     * Loads all recurring transactions from the repository and updates UI state.
     * Calculates summary statistics including monthly totals, counts by type,
     * and upcoming payments count.
     *
     * Flow:
     * 1. Initialize repository if not already done
     * 2. Load all recurring transactions from database
     * 3. Calculate monthly recurring total
     * 4. Calculate statistics (counts by type, upcoming count, confirmed count)
     * 5. Update UI state with loaded data
     *
     * Error Handling:
     * - Repository initialization failures are shown to user
     * - Load failures show error message but don't crash
     * - Statistics calculation errors are logged but don't prevent data display
     *
     * @param context Application context for database initialization
     */
    fun loadRecurringTransactions(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Step 1: Initialize repository if not already done
                if (repository == null) {
                    try {
                        repository = DatabaseProvider.getRecurringRepository(context)
                    } catch (e: Exception) {
                        val errorInfo = ErrorHandler.handleError(e, "Initialize recurring repository")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = errorInfo.userMessage
                        )
                        return@launch
                    }
                }
                val repo = repository!!

                // Step 2: Load all recurring transactions
                val recurringTransactions = repo.getAllRecurringTransactionsSnapshot()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load recurring transactions: ${errorInfo.userMessage}"
                        )
                        return@launch
                    }
                    .getOrElse { emptyList() }

                // Step 3: Calculate monthly recurring total
                val monthlyTotal = repo.calculateMonthlyRecurringTotal()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to calculate monthly total: ${errorInfo.technicalMessage}",
                            "loadRecurringTransactions"
                        )
                        // Continue with 0.0 - not critical
                    }
                    .getOrElse { 0.0 }

                // Step 4: Calculate statistics
                val statistics = calculateStatistics(recurringTransactions)

                // Step 5: Update UI state
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    recurringTransactions = recurringTransactions,
                    totalMonthlyRecurring = monthlyTotal,
                    confirmedCount = statistics.confirmedCount,
                    upcomingCount = statistics.upcomingCount,
                    subscriptionCount = statistics.subscriptionCount,
                    emiCount = statistics.emiCount,
                    salaryCount = statistics.salaryCount,
                    rentCount = statistics.rentCount,
                    utilityCount = statistics.utilityCount,
                    otherCount = statistics.otherCount
                )

                ErrorHandler.logInfo(
                    "Successfully loaded ${recurringTransactions.size} recurring patterns (${statistics.confirmedCount} confirmed)",
                    "loadRecurringTransactions"
                )
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "loadRecurringTransactions")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Confirms a recurring transaction pattern as legitimate.
     * This marks the pattern as user-verified and includes it in confirmed monthly totals.
     *
     * Error Handling:
     * - Confirmation failures show error message to user
     * - Data is reloaded after successful confirmation to update statistics
     *
     * @param context Application context for repository access
     * @param id The ID of the recurring transaction to confirm
     */
    fun confirmRecurringTransaction(context: Context, id: String) {
        viewModelScope.launch {
            try {
                // Ensure repository is initialized
                if (repository == null) {
                    repository = DatabaseProvider.getRecurringRepository(context)
                }
                val repo = repository!!

                // Update confirmation status
                repo.updateConfirmationStatus(id, isConfirmed = true)
                    .onSuccess {
                        // Reload data to update statistics
                        loadRecurringTransactions(context)
                        ErrorHandler.logInfo(
                            "Recurring transaction $id confirmed by user",
                            "confirmRecurringTransaction"
                        )
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to confirm recurring transaction: ${errorInfo.userMessage}"
                        )
                    }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "confirmRecurringTransaction")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Removes a recurring transaction pattern from the system.
     * This is used when a detected pattern is incorrect or no longer relevant.
     *
     * Error Handling:
     * - Deletion failures show error message to user
     * - Data is reloaded after successful deletion to update UI
     *
     * @param context Application context for repository access
     * @param id The ID of the recurring transaction to remove
     */
    fun removeRecurringTransaction(context: Context, id: String) {
        viewModelScope.launch {
            try {
                // Ensure repository is initialized
                if (repository == null) {
                    repository = DatabaseProvider.getRecurringRepository(context)
                }
                val repo = repository!!

                // Delete the recurring transaction
                repo.deleteRecurringTransaction(id)
                    .onSuccess { deleted ->
                        if (deleted) {
                            // Reload data to update UI
                            loadRecurringTransactions(context)
                            ErrorHandler.logInfo(
                                "Recurring transaction $id removed by user",
                                "removeRecurringTransaction"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Recurring transaction not found"
                            )
                        }
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to remove recurring transaction: ${errorInfo.userMessage}"
                        )
                    }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "removeRecurringTransaction")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Clears any error message from the UI state.
     * Should be called after the user has acknowledged the error.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Calculates summary statistics from a list of recurring transactions.
     * This includes counts by type, confirmed count, and upcoming payments count.
     *
     * @param transactions List of recurring transactions to analyze
     * @return RecurringStatistics object containing all calculated statistics
     */
    private fun calculateStatistics(transactions: List<RecurringTransaction>): RecurringStatistics {
        val currentTimestamp = System.currentTimeMillis()
        val sevenDaysFromNow = currentTimestamp + TimeUnit.DAYS.toMillis(7)

        return RecurringStatistics(
            confirmedCount = transactions.count { it.isUserConfirmed },
            upcomingCount = transactions.count { it.nextExpected in currentTimestamp..sevenDaysFromNow },
            subscriptionCount = transactions.count { it.type == RecurringType.SUBSCRIPTION },
            emiCount = transactions.count { it.type == RecurringType.EMI },
            salaryCount = transactions.count { it.type == RecurringType.SALARY },
            rentCount = transactions.count { it.type == RecurringType.RENT },
            utilityCount = transactions.count { it.type == RecurringType.UTILITY },
            otherCount = transactions.count { it.type == RecurringType.OTHER }
        )
    }

    /**
     * Data class to hold calculated statistics.
     * Internal helper class to organize statistics calculation results.
     */
    private data class RecurringStatistics(
        val confirmedCount: Int,
        val upcomingCount: Int,
        val subscriptionCount: Int,
        val emiCount: Int,
        val salaryCount: Int,
        val rentCount: Int,
        val utilityCount: Int,
        val otherCount: Int
    )
}
