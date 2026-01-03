package com.example.kanakku.ui.savings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.data.database.DatabaseProvider
import com.example.kanakku.data.model.GoalContribution
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.SavingsGoal
import com.example.kanakku.data.model.TimePeriod
import com.example.kanakku.data.repository.SavingsGoalRepository
import com.example.kanakku.data.repository.TransactionRepository
import com.example.kanakku.domain.savings.AggregateGoalStats
import com.example.kanakku.domain.savings.GoalProgressCalculator
import com.example.kanakku.domain.savings.ProgressMetrics
import com.example.kanakku.domain.savings.SavingsCalculator
import com.example.kanakku.domain.savings.SavingsSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UI state for the savings goals feature
 */
data class SavingsGoalsUiState(
    val isLoading: Boolean = false,
    val allGoals: List<SavingsGoal> = emptyList(),
    val activeGoals: List<SavingsGoal> = emptyList(),
    val completedGoals: List<SavingsGoal> = emptyList(),
    val selectedGoal: SavingsGoal? = null,
    val selectedGoalContributions: List<GoalContribution> = emptyList(),
    val selectedGoalProgress: ProgressMetrics? = null,
    val aggregateStats: AggregateGoalStats? = null,
    val savingsSuggestion: SavingsSuggestion? = null,
    val errorMessage: String? = null,
    val celebratingGoal: SavingsGoal? = null,
    val showCreateGoalSheet: Boolean = false,
    val showEditGoalSheet: Boolean = false,
    val showAddContributionDialog: Boolean = false
)

/**
 * ViewModel for managing savings goals state and business logic.
 *
 * This ViewModel handles:
 * - Loading and managing savings goals
 * - Creating, updating, and deleting goals
 * - Adding contributions to goals
 * - Calculating progress and statistics
 * - Generating savings suggestions based on transaction history
 * - Handling celebration animations when goals are completed
 *
 * The ViewModel observes both savings goals and transactions to provide
 * comprehensive savings insights to the UI.
 */
class SavingsGoalsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsGoalsUiState())
    val uiState: StateFlow<SavingsGoalsUiState> = _uiState.asStateFlow()

    private var savingsGoalRepository: SavingsGoalRepository? = null
    private var transactionRepository: TransactionRepository? = null

    private val savingsCalculator = SavingsCalculator()
    private val progressCalculator = GoalProgressCalculator()

    // ==================== Initialization ====================

    /**
     * Initializes the ViewModel with required repositories.
     * Must be called before using any other methods.
     *
     * @param context Application context for database access
     */
    fun initialize(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Initialize repositories
                val database = DatabaseProvider.getDatabase(context)
                savingsGoalRepository = SavingsGoalRepository(database)
                transactionRepository = DatabaseProvider.getRepository(context)

                // Load initial data
                loadGoals()
                loadSavingsSuggestion()

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error initializing: ${e.message}"
                )
            }
        }
    }

    // ==================== Goal Loading ====================

    /**
     * Loads all savings goals from the repository.
     * Sets up reactive Flow to automatically update when goals change.
     */
    fun loadGoals() {
        viewModelScope.launch {
            try {
                val repository = savingsGoalRepository ?: return@launch

                // Observe all goals
                repository.getAllGoals().collectLatest { allGoals ->
                    val activeGoals = allGoals.filter { !it.isCompleted }
                    val completedGoals = allGoals.filter { it.isCompleted }

                    // Calculate aggregate stats
                    val stats = progressCalculator.calculateAggregateStats(allGoals)

                    _uiState.value = _uiState.value.copy(
                        allGoals = allGoals,
                        activeGoals = activeGoals,
                        completedGoals = completedGoals,
                        aggregateStats = stats
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error loading goals: ${e.message}"
                )
            }
        }
    }

    /**
     * Loads details for a specific goal including contributions and progress metrics.
     *
     * @param goalId The ID of the goal to load
     */
    fun loadGoalDetails(goalId: Long) {
        viewModelScope.launch {
            try {
                val repository = savingsGoalRepository ?: return@launch

                // Get the goal
                val goal = repository.getGoal(goalId)
                if (goal == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Goal not found"
                    )
                    return@launch
                }

                // Get contributions
                val contributions = repository.getContributionsSnapshot(goalId)

                // Calculate progress metrics
                val progress = progressCalculator.calculateProgressMetrics(goal, contributions)

                _uiState.value = _uiState.value.copy(
                    selectedGoal = goal,
                    selectedGoalContributions = contributions,
                    selectedGoalProgress = progress
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error loading goal details: ${e.message}"
                )
            }
        }
    }

    // ==================== Goal CRUD Operations ====================

    /**
     * Creates a new savings goal.
     *
     * @param goal The goal to create
     * @return The ID of the newly created goal, or null if failed
     */
    suspend fun createGoal(goal: SavingsGoal): Long? {
        return try {
            val repository = savingsGoalRepository ?: return null
            val goalId = repository.createGoal(goal)

            _uiState.value = _uiState.value.copy(
                showCreateGoalSheet = false,
                errorMessage = null
            )

            goalId
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error creating goal: ${e.message}"
            )
            null
        }
    }

    /**
     * Updates an existing savings goal.
     *
     * @param goal The goal to update
     * @return True if updated successfully, false otherwise
     */
    suspend fun updateGoal(goal: SavingsGoal): Boolean {
        return try {
            val repository = savingsGoalRepository ?: return false
            val success = repository.updateGoal(goal)

            if (success) {
                _uiState.value = _uiState.value.copy(
                    showEditGoalSheet = false,
                    errorMessage = null
                )

                // Reload details if this is the selected goal
                if (_uiState.value.selectedGoal?.id == goal.id) {
                    loadGoalDetails(goal.id)
                }
            }

            success
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error updating goal: ${e.message}"
            )
            false
        }
    }

    /**
     * Deletes a savings goal.
     * Related contributions will be automatically deleted due to CASCADE constraint.
     *
     * @param goalId The ID of the goal to delete
     * @return True if deleted successfully, false otherwise
     */
    suspend fun deleteGoal(goalId: Long): Boolean {
        return try {
            val repository = savingsGoalRepository ?: return false
            val success = repository.deleteGoal(goalId)

            if (success) {
                _uiState.value = _uiState.value.copy(
                    selectedGoal = null,
                    selectedGoalContributions = emptyList(),
                    selectedGoalProgress = null,
                    errorMessage = null
                )
            }

            success
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error deleting goal: ${e.message}"
            )
            false
        }
    }

    // ==================== Contribution Management ====================

    /**
     * Adds a contribution to a savings goal.
     * Automatically updates goal progress and checks for completion.
     *
     * @param contribution The contribution to add
     * @return The ID of the newly created contribution, or null if failed
     */
    suspend fun addContribution(contribution: GoalContribution): Long? {
        return try {
            val repository = savingsGoalRepository ?: return null

            // Get the goal before adding contribution
            val goalBefore = repository.getGoal(contribution.goalId)

            // Add the contribution
            val contributionId = repository.addContribution(contribution)

            // Get the goal after adding contribution
            val goalAfter = repository.getGoal(contribution.goalId)

            // Check if goal was just completed (trigger celebration)
            if (goalBefore != null && goalAfter != null) {
                if (!goalBefore.isCompleted && goalAfter.isCompleted) {
                    _uiState.value = _uiState.value.copy(
                        celebratingGoal = goalAfter
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                showAddContributionDialog = false,
                errorMessage = null
            )

            // Reload details if this is the selected goal
            if (_uiState.value.selectedGoal?.id == contribution.goalId) {
                loadGoalDetails(contribution.goalId)
            }

            contributionId
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error adding contribution: ${e.message}"
            )
            null
        }
    }

    /**
     * Deletes a contribution.
     * Automatically updates goal progress.
     *
     * @param contributionId The ID of the contribution to delete
     * @return True if deleted successfully, false otherwise
     */
    suspend fun deleteContribution(contributionId: Long): Boolean {
        return try {
            val repository = savingsGoalRepository ?: return false
            val success = repository.deleteContribution(contributionId)

            if (success) {
                // Reload details if we have a selected goal
                _uiState.value.selectedGoal?.let { goal ->
                    loadGoalDetails(goal.id)
                }
            }

            success
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Error deleting contribution: ${e.message}"
            )
            false
        }
    }

    // ==================== Savings Suggestions ====================

    /**
     * Loads savings suggestions based on recent transaction history.
     * Analyzes income and spending patterns to suggest achievable savings amounts.
     *
     * @param period The time period to analyze (defaults to MONTH)
     */
    fun loadSavingsSuggestion(period: TimePeriod = TimePeriod.MONTH) {
        viewModelScope.launch {
            try {
                val repository = transactionRepository ?: return@launch

                // Get recent transactions
                val transactions = repository.getAllTransactionsSnapshot()

                // Calculate savings suggestion
                val suggestion = savingsCalculator.calculateSavingsSuggestion(transactions, period)

                _uiState.value = _uiState.value.copy(
                    savingsSuggestion = suggestion
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error calculating savings suggestion: ${e.message}"
                )
            }
        }
    }

    /**
     * Gets recent transactions for savings calculations.
     * Used by UI components that need transaction data.
     *
     * @return List of recent transactions, or empty list if not available
     */
    suspend fun getRecentTransactions(): List<ParsedTransaction> {
        return try {
            transactionRepository?.getAllTransactionsSnapshot() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== Celebration Handling ====================

    /**
     * Clears the celebration state.
     * Should be called after the celebration animation is complete.
     */
    fun clearCelebration() {
        _uiState.value = _uiState.value.copy(
            celebratingGoal = null
        )
    }

    /**
     * Manually triggers celebration for a completed goal.
     * Useful for replaying celebration or testing.
     *
     * @param goal The completed goal to celebrate
     */
    fun triggerCelebration(goal: SavingsGoal) {
        if (goal.isCompleted) {
            _uiState.value = _uiState.value.copy(
                celebratingGoal = goal
            )
        }
    }

    // ==================== UI State Management ====================

    /**
     * Shows the create goal bottom sheet.
     */
    fun showCreateGoalSheet() {
        _uiState.value = _uiState.value.copy(
            showCreateGoalSheet = true
        )
    }

    /**
     * Hides the create goal bottom sheet.
     */
    fun hideCreateGoalSheet() {
        _uiState.value = _uiState.value.copy(
            showCreateGoalSheet = false
        )
    }

    /**
     * Shows the edit goal bottom sheet.
     */
    fun showEditGoalSheet() {
        _uiState.value = _uiState.value.copy(
            showEditGoalSheet = true
        )
    }

    /**
     * Hides the edit goal bottom sheet.
     */
    fun hideEditGoalSheet() {
        _uiState.value = _uiState.value.copy(
            showEditGoalSheet = false
        )
    }

    /**
     * Shows the add contribution dialog.
     */
    fun showAddContributionDialog() {
        _uiState.value = _uiState.value.copy(
            showAddContributionDialog = true
        )
    }

    /**
     * Hides the add contribution dialog.
     */
    fun hideAddContributionDialog() {
        _uiState.value = _uiState.value.copy(
            showAddContributionDialog = false
        )
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }

    /**
     * Clears the selected goal.
     */
    fun clearSelectedGoal() {
        _uiState.value = _uiState.value.copy(
            selectedGoal = null,
            selectedGoalContributions = emptyList(),
            selectedGoalProgress = null
        )
    }
}
