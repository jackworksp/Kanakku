package com.example.kanakku.ui.budget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.core.error.toErrorInfo
import com.example.kanakku.data.database.DatabaseProvider
import com.example.kanakku.data.model.*
import com.example.kanakku.data.repository.BudgetRepository
import com.example.kanakku.data.repository.TransactionRepository
import com.example.kanakku.domain.budget.BudgetCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * UI state for budget management screen.
 *
 * @param isLoading Indicates if data is being loaded
 * @param budgets List of budgets for the current month
 * @param budgetProgresses Map of budget ID to calculated progress
 * @param categoryBudgetProgresses List of category-specific budget progress for UI display
 * @param overallBudgetProgress Overall monthly budget progress (null if not set)
 * @param errorMessage Error message to display to user (null if no error)
 * @param currentMonth Current month being displayed (1-12)
 * @param currentYear Current year being displayed
 * @param editingBudget Budget currently being edited (null if not editing)
 * @param isEditMode Whether the UI is in edit mode
 * @param isEditingOverallBudget Whether the edit dialog is for overall budget (true) or category budget (false)
 */
data class BudgetUiState(
    val isLoading: Boolean = false,
    val budgets: List<Budget> = emptyList(),
    val budgetProgresses: Map<Long, BudgetProgress> = emptyMap(),
    val categoryBudgetProgresses: List<CategoryBudgetProgress> = emptyList(),
    val overallBudgetProgress: BudgetProgress? = null,
    val errorMessage: String? = null,
    val currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val editingBudget: Budget? = null,
    val isEditMode: Boolean = false,
    val isEditingOverallBudget: Boolean = true
)

/**
 * ViewModel for budget management functionality.
 *
 * This ViewModel manages the budget management screen, providing:
 * - Loading budgets for the current month
 * - Saving/updating overall and category budgets
 * - Deleting budgets
 * - Calculating budget progress with current spending
 * - Error handling with user-friendly messages
 *
 * Error Handling:
 * - All database operations wrapped with ErrorHandler
 * - Repository operations return Result<T> for explicit error handling
 * - Errors displayed as user-friendly messages in UI state
 * - All errors logged for debugging
 *
 * Usage:
 * ```
 * val viewModel = BudgetViewModel()
 * viewModel.initialize(context)
 * viewModel.loadBudgets()
 * ```
 */
class BudgetViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private var budgetRepository: BudgetRepository? = null
    private var transactionRepository: TransactionRepository? = null
    private val budgetCalculator = BudgetCalculator()

    // Cache for category map (loaded from transactions)
    private var categoryMap: Map<Long, Category> = emptyMap()

    // Cache for transactions (for budget progress calculation)
    private var transactions: List<ParsedTransaction> = emptyList()

    /**
     * Initialize the ViewModel with repositories.
     * Must be called before any other operations.
     *
     * @param context Application or Activity context
     */
    fun initialize(context: Context) {
        viewModelScope.launch {
            try {
                budgetRepository = DatabaseProvider.getBudgetRepository(context)
                transactionRepository = DatabaseProvider.getRepository(context)
                ErrorHandler.logInfo("BudgetViewModel initialized successfully", "BudgetViewModel")
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Initialize BudgetViewModel")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Loads budgets for the current month along with transaction data for progress calculation.
     *
     * Flow:
     * 1. Load budgets from repository for current month
     * 2. Load transactions for current month
     * 3. Calculate budget progress for each budget
     * 4. Update UI state with budgets and progress
     *
     * Error Handling:
     * - Database read failures
     * - Progress calculation errors
     * - All errors provide user-friendly messages
     */
    fun loadBudgets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val budgetRepo = budgetRepository
                val transactionRepo = transactionRepository

                if (budgetRepo == null || transactionRepo == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Repository not initialized. Please restart the app."
                    )
                    return@launch
                }

                val currentMonth = _uiState.value.currentMonth
                val currentYear = _uiState.value.currentYear

                // Step 1: Load budgets for current month
                val budgets = budgetRepo.getBudgetsForMonth(currentMonth, currentYear)
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load budgets: ${errorInfo.userMessage}"
                        )
                        return@launch
                    }
                    .getOrElse { emptyList() }

                // Step 2: Load transactions for current month
                val allTransactions = transactionRepo.getAllTransactionsSnapshot()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to load transactions: ${errorInfo.technicalMessage}",
                            "loadBudgets"
                        )
                        // Continue with empty list - not critical
                    }
                    .getOrElse { emptyList() }

                // Filter transactions for current month
                transactions = try {
                    budgetCalculator.filterTransactionsByMonth(
                        allTransactions,
                        currentMonth,
                        currentYear
                    )
                } catch (e: Exception) {
                    val errorInfo = ErrorHandler.handleError(e, "Filter transactions by month")
                    ErrorHandler.logWarning(
                        "Failed to filter transactions: ${errorInfo.technicalMessage}",
                        "loadBudgets"
                    )
                    emptyList()
                }

                // Step 3: Calculate budget progress
                val budgetProgresses = calculateBudgetProgresses(budgets, transactions)

                // Step 4: Prepare category budget progresses for UI
                val categoryBudgetProgresses = prepareCategoryBudgetProgresses(budgets, budgetProgresses)

                // Step 5: Get overall budget progress
                val overallBudget = budgets.find { it.categoryId == null }
                val overallProgress = if (overallBudget != null) {
                    budgetProgresses[overallBudget.id]
                } else {
                    null
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    budgets = budgets,
                    budgetProgresses = budgetProgresses,
                    categoryBudgetProgresses = categoryBudgetProgresses,
                    overallBudgetProgress = overallProgress
                )

                ErrorHandler.logInfo(
                    "Successfully loaded ${budgets.size} budgets for $currentMonth/$currentYear",
                    "loadBudgets"
                )

            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "loadBudgets")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Saves or updates a budget (overall or category-specific).
     *
     * @param budget The budget to save or update
     */
    fun saveBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                val budgetRepo = budgetRepository

                if (budgetRepo == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Repository not initialized. Please restart the app."
                    )
                    return@launch
                }

                budgetRepo.saveBudget(budget)
                    .onSuccess {
                        ErrorHandler.logInfo(
                            "Budget saved successfully: ${budget.categoryId ?: "overall"}",
                            "saveBudget"
                        )
                        // Reload budgets to reflect changes
                        loadBudgets()
                        // Exit edit mode
                        _uiState.value = _uiState.value.copy(
                            isEditMode = false,
                            editingBudget = null,
                            isEditingOverallBudget = true
                        )
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to save budget: ${errorInfo.userMessage}"
                        )
                    }

            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "saveBudget")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Saves a budget from the edit dialog with amount and categoryId.
     * Creates a new Budget object or updates the existing one.
     *
     * @param amount The budget amount
     * @param categoryId The category ID (null for overall budget)
     */
    fun saveBudgetFromDialog(amount: Double, categoryId: String?) {
        val currentMonth = _uiState.value.currentMonth
        val currentYear = _uiState.value.currentYear
        val editingBudget = _uiState.value.editingBudget

        val budget = if (editingBudget != null) {
            // Update existing budget
            editingBudget.copy(
                amount = amount,
                categoryId = categoryId
            )
        } else {
            // Create new budget
            Budget(
                id = 0, // Auto-generated by database
                categoryId = categoryId,
                amount = amount,
                month = currentMonth,
                year = currentYear,
                createdAt = System.currentTimeMillis()
            )
        }

        saveBudget(budget)
    }

    /**
     * Deletes a budget.
     *
     * @param budget The budget to delete
     */
    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                val budgetRepo = budgetRepository

                if (budgetRepo == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Repository not initialized. Please restart the app."
                    )
                    return@launch
                }

                budgetRepo.deleteBudget(budget.categoryId, budget.month, budget.year)
                    .onSuccess { deleted ->
                        if (deleted) {
                            ErrorHandler.logInfo(
                                "Budget deleted successfully: ${budget.categoryId ?: "overall"}",
                                "deleteBudget"
                            )
                            // Reload budgets to reflect changes
                            loadBudgets()
                        } else {
                            ErrorHandler.logWarning(
                                "Budget not found for deletion: ${budget.categoryId}",
                                "deleteBudget"
                            )
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Budget not found"
                            )
                        }
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Failed to delete budget: ${errorInfo.userMessage}"
                        )
                    }

            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "deleteBudget")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Enters edit mode for a specific budget.
     *
     * @param budget The budget to edit (null for creating new budget)
     * @param isOverallBudget Whether this is for overall budget (true) or category budget (false)
     */
    fun startEditBudget(budget: Budget?, isOverallBudget: Boolean = true) {
        _uiState.value = _uiState.value.copy(
            isEditMode = true,
            editingBudget = budget,
            isEditingOverallBudget = isOverallBudget
        )
    }

    /**
     * Exits edit mode without saving.
     */
    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(
            isEditMode = false,
            editingBudget = null,
            isEditingOverallBudget = true
        )
    }

    /**
     * Changes the displayed month/year and reloads budgets.
     *
     * @param month Month (1-12)
     * @param year Year (e.g., 2026)
     */
    fun changeMonth(month: Int, year: Int) {
        _uiState.value = _uiState.value.copy(
            currentMonth = month,
            currentYear = year
        )
        loadBudgets()
    }

    /**
     * Clears any error message from the UI state.
     * Should be called after the user has acknowledged the error.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Updates the cached category map for budget calculations.
     * Should be called when categories are loaded or changed.
     *
     * @param newCategoryMap Map of SMS ID to Category
     */
    fun updateCategoryMap(newCategoryMap: Map<Long, Category>) {
        categoryMap = newCategoryMap
    }

    // ==================== Private Helper Functions ====================

    /**
     * Calculates budget progress for all budgets using current spending data.
     *
     * @param budgets List of budgets to calculate progress for
     * @param transactions List of transactions for the current month
     * @return Map of budget ID to BudgetProgress
     */
    private fun calculateBudgetProgresses(
        budgets: List<Budget>,
        transactions: List<ParsedTransaction>
    ): Map<Long, BudgetProgress> {
        return try {
            // Calculate spending by category
            val spentByCategory = budgetCalculator.getSpentByCategory(transactions, categoryMap)
            val totalSpent = budgetCalculator.getTotalSpent(transactions)

            budgets.associate { budget ->
                val spent = if (budget.categoryId == null) {
                    // Overall budget - use total spending
                    totalSpent
                } else {
                    // Category budget - use category spending
                    spentByCategory[budget.categoryId] ?: 0.0
                }

                val progress = budgetCalculator.calculateBudgetProgress(budget, spent)
                budget.id to progress
            }
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Calculate budget progresses")
            ErrorHandler.logWarning(
                "Failed to calculate budget progresses: ${errorInfo.technicalMessage}",
                "calculateBudgetProgresses"
            )
            emptyMap()
        }
    }

    /**
     * Prepares category budget progresses for UI display.
     * Combines category information with budget and progress data.
     *
     * @param budgets List of all budgets
     * @param budgetProgresses Map of budget ID to progress
     * @return List of CategoryBudgetProgress for UI rendering
     */
    private fun prepareCategoryBudgetProgresses(
        budgets: List<Budget>,
        budgetProgresses: Map<Long, BudgetProgress>
    ): List<CategoryBudgetProgress> {
        return try {
            budgets
                .filter { it.categoryId != null } // Only category budgets, not overall
                .mapNotNull { budget ->
                    val categoryId = budget.categoryId ?: return@mapNotNull null
                    val category = DefaultCategories.ALL.find { it.id == categoryId }
                        ?: return@mapNotNull null
                    val progress = budgetProgresses[budget.id] ?: return@mapNotNull null

                    CategoryBudgetProgress(
                        category = category,
                        budget = budget,
                        progress = progress
                    )
                }
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Prepare category budget progresses")
            ErrorHandler.logWarning(
                "Failed to prepare category budget progresses: ${errorInfo.technicalMessage}",
                "prepareCategoryBudgetProgresses"
            )
            emptyList()
        }
    }
}
