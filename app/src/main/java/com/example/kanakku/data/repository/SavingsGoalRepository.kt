package com.example.kanakku.data.repository

import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.database.toDomain
import com.example.kanakku.data.database.toEntity
import com.example.kanakku.data.model.GoalContribution
import com.example.kanakku.data.model.GoalProgress
import com.example.kanakku.data.model.SavingsGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Repository for managing savings goals and contributions.
 *
 * This repository acts as a bridge between the domain layer (SavingsGoal, GoalContribution)
 * and the data layer (SavingsGoalEntity, GoalContributionEntity), handling all entity-model
 * mapping and database operations.
 *
 * Key responsibilities:
 * - CRUD operations for savings goals
 * - Manage contributions to goals
 * - Calculate goal progress and statistics
 * - Provide reactive data streams via Flow
 *
 * @param database The Room database instance
 */
class SavingsGoalRepository(private val database: KanakkuDatabase) {

    // DAOs for database access
    private val savingsGoalDao = database.savingsGoalDao()
    private val goalContributionDao = database.goalContributionDao()

    // ==================== Goal CRUD Operations ====================

    /**
     * Creates a new savings goal.
     * Converts the domain model to an entity before persisting.
     *
     * @param goal The savings goal to create
     * @return The ID of the newly created goal
     */
    suspend fun createGoal(goal: SavingsGoal): Long {
        return savingsGoalDao.insert(goal.toEntity())
    }

    /**
     * Updates an existing savings goal.
     *
     * @param goal The savings goal to update
     * @return True if goal was updated, false if not found
     */
    suspend fun updateGoal(goal: SavingsGoal): Boolean {
        return savingsGoalDao.update(goal.toEntity()) > 0
    }

    /**
     * Deletes a savings goal by its ID.
     * Note: Related contributions will be automatically deleted due to CASCADE constraint.
     *
     * @param goalId The ID of the goal to delete
     * @return True if goal was deleted, false if not found
     */
    suspend fun deleteGoal(goalId: Long): Boolean {
        return savingsGoalDao.deleteById(goalId) > 0
    }

    /**
     * Retrieves a single savings goal by its ID.
     *
     * @param goalId The ID of the goal to retrieve
     * @return The savings goal, or null if not found
     */
    suspend fun getGoal(goalId: Long): SavingsGoal? {
        return savingsGoalDao.getGoalById(goalId)?.toDomain()
    }

    /**
     * Retrieves a single savings goal by its ID as a reactive Flow.
     *
     * @param goalId The ID of the goal to retrieve
     * @return Flow emitting the savings goal, or null if not found
     */
    fun getGoalFlow(goalId: Long): Flow<SavingsGoal?> {
        return savingsGoalDao.getGoalByIdFlow(goalId)
            .map { it?.toDomain() }
    }

    /**
     * Retrieves all savings goals as a reactive Flow.
     * Automatically converts entities to domain models.
     *
     * @return Flow emitting list of savings goals, sorted by creation date (newest first)
     */
    fun getAllGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getAllGoals()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Retrieves all savings goals as a one-time snapshot.
     * Useful for non-reactive operations.
     *
     * @return List of savings goals, sorted by creation date (newest first)
     */
    suspend fun getAllGoalsSnapshot(): List<SavingsGoal> {
        return savingsGoalDao.getAllGoalsSnapshot()
            .map { it.toDomain() }
    }

    /**
     * Retrieves only active (not completed) savings goals.
     *
     * @return Flow emitting list of active savings goals, sorted by deadline (closest first)
     */
    fun getActiveGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getActiveGoals()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Retrieves only completed savings goals.
     *
     * @return Flow emitting list of completed savings goals, sorted by completion date
     */
    fun getCompletedGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getCompletedGoals()
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Checks if a savings goal with the given ID exists.
     *
     * @param goalId The goal ID to check
     * @return True if goal exists, false otherwise
     */
    suspend fun goalExists(goalId: Long): Boolean {
        return savingsGoalDao.exists(goalId)
    }

    // ==================== Contribution Operations ====================

    /**
     * Adds a contribution to a savings goal.
     * Automatically updates the goal's current amount.
     *
     * @param contribution The contribution to add
     * @return The ID of the newly created contribution
     */
    suspend fun addContribution(contribution: GoalContribution): Long {
        val contributionId = goalContributionDao.insert(contribution.toEntity())

        // Update the goal's current amount
        val goal = savingsGoalDao.getGoalById(contribution.goalId)
        if (goal != null) {
            val newAmount = goal.currentAmount + contribution.amount
            savingsGoalDao.updateProgress(contribution.goalId, newAmount)

            // Check if goal is now completed
            if (newAmount >= goal.targetAmount && !goal.isCompleted) {
                savingsGoalDao.markAsCompleted(contribution.goalId, System.currentTimeMillis())
            }
        }

        return contributionId
    }

    /**
     * Retrieves all contributions for a specific goal.
     *
     * @param goalId The ID of the goal
     * @return Flow emitting list of contributions, sorted by date (newest first)
     */
    fun getContributions(goalId: Long): Flow<List<GoalContribution>> {
        return goalContributionDao.getContributionsByGoal(goalId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Retrieves all contributions for a specific goal as a one-time snapshot.
     *
     * @param goalId The ID of the goal
     * @return List of contributions, sorted by date (newest first)
     */
    suspend fun getContributionsSnapshot(goalId: Long): List<GoalContribution> {
        return goalContributionDao.getContributionsByGoalSnapshot(goalId)
            .map { it.toDomain() }
    }

    /**
     * Deletes a contribution by its ID.
     * Automatically updates the goal's current amount.
     *
     * @param contributionId The ID of the contribution to delete
     * @return True if contribution was deleted, false if not found
     */
    suspend fun deleteContribution(contributionId: Long): Boolean {
        val contribution = goalContributionDao.getContributionById(contributionId)
        if (contribution != null) {
            val deleted = goalContributionDao.deleteById(contributionId) > 0
            if (deleted) {
                // Update the goal's current amount
                val goal = savingsGoalDao.getGoalById(contribution.goalId)
                if (goal != null) {
                    val newAmount = (goal.currentAmount - contribution.amount).coerceAtLeast(0.0)
                    savingsGoalDao.updateProgress(contribution.goalId, newAmount)

                    // If goal was completed but is no longer at target, mark as incomplete
                    if (goal.isCompleted && newAmount < goal.targetAmount) {
                        val updatedGoal = goal.copy(isCompleted = false, completedAt = null)
                        savingsGoalDao.update(updatedGoal)
                    }
                }
            }
            return deleted
        }
        return false
    }

    /**
     * Gets the total amount contributed to a specific goal.
     *
     * @param goalId The ID of the goal
     * @return Total contribution amount
     */
    suspend fun getTotalContributions(goalId: Long): Double {
        return goalContributionDao.getTotalContributionsForGoal(goalId)
    }

    /**
     * Gets the total amount contributed to a specific goal as a reactive Flow.
     *
     * @param goalId The ID of the goal
     * @return Flow emitting total contribution amount
     */
    fun getTotalContributionsFlow(goalId: Long): Flow<Double> {
        return goalContributionDao.getTotalContributionsForGoalFlow(goalId)
    }

    // ==================== Progress Calculation ====================

    /**
     * Calculates detailed progress information for a savings goal.
     * Combines goal data with contributions to provide comprehensive progress metrics.
     *
     * @param goalId The ID of the goal
     * @return GoalProgress with calculated metrics, or null if goal not found
     */
    suspend fun getGoalProgress(goalId: Long): GoalProgress? {
        val goal = savingsGoalDao.getGoalById(goalId)?.toDomain() ?: return null
        val contributions = goalContributionDao.getContributionsByGoalSnapshot(goalId)
            .map { it.toDomain() }
        return GoalProgress(goal, contributions)
    }

    /**
     * Calculates detailed progress information for a savings goal as a reactive Flow.
     *
     * @param goalId The ID of the goal
     * @return Flow emitting GoalProgress with calculated metrics
     */
    fun getGoalProgressFlow(goalId: Long): Flow<GoalProgress?> {
        return combine(
            savingsGoalDao.getGoalByIdFlow(goalId),
            goalContributionDao.getContributionsByGoal(goalId)
        ) { goalEntity, contributionEntities ->
            val goal = goalEntity?.toDomain() ?: return@combine null
            val contributions = contributionEntities.map { it.toDomain() }
            GoalProgress(goal, contributions)
        }
    }

    /**
     * Manually marks a goal as completed.
     *
     * @param goalId The ID of the goal to mark as completed
     * @return True if goal was marked as completed, false if not found
     */
    suspend fun markGoalAsCompleted(goalId: Long): Boolean {
        return savingsGoalDao.markAsCompleted(goalId, System.currentTimeMillis()) > 0
    }

    /**
     * Updates the current progress amount for a goal.
     * This can be used for manual adjustments without adding a contribution.
     *
     * @param goalId The ID of the goal
     * @param currentAmount The new current amount
     * @return True if progress was updated, false if not found
     */
    suspend fun updateGoalProgress(goalId: Long, currentAmount: Double): Boolean {
        val updated = savingsGoalDao.updateProgress(goalId, currentAmount) > 0
        if (updated) {
            val goal = savingsGoalDao.getGoalById(goalId)
            if (goal != null && currentAmount >= goal.targetAmount && !goal.isCompleted) {
                savingsGoalDao.markAsCompleted(goalId, System.currentTimeMillis())
            }
        }
        return updated
    }

    // ==================== Statistics ====================

    /**
     * Gets statistics for all savings goals.
     *
     * @return Map containing various statistics
     */
    suspend fun getGoalStatistics(): Map<String, Any> {
        val totalGoals = savingsGoalDao.getGoalCount()
        val activeGoals = savingsGoalDao.getActiveGoalCount()
        val completedGoals = savingsGoalDao.getCompletedGoalCount()
        val allGoals = savingsGoalDao.getAllGoalsSnapshot().map { it.toDomain() }

        val totalTargetAmount = allGoals.sumOf { it.targetAmount }
        val totalCurrentAmount = allGoals.sumOf { it.currentAmount }
        val totalSaved = totalCurrentAmount
        val totalRemaining = allGoals.sumOf { it.remainingAmount }

        val completedGoalsList = allGoals.filter { it.isCompleted }
        val totalCompletedAmount = completedGoalsList.sumOf { it.targetAmount }

        return mapOf(
            "totalGoals" to totalGoals,
            "activeGoals" to activeGoals,
            "completedGoals" to completedGoals,
            "totalTargetAmount" to totalTargetAmount,
            "totalCurrentAmount" to totalCurrentAmount,
            "totalSaved" to totalSaved,
            "totalRemaining" to totalRemaining,
            "totalCompletedAmount" to totalCompletedAmount,
            "overallProgress" to if (totalTargetAmount > 0) {
                (totalCurrentAmount / totalTargetAmount * 100).coerceIn(0.0, 100.0)
            } else {
                0.0
            }
        )
    }

    /**
     * Gets the total amount saved across all goals.
     *
     * @return Total current amount across all goals
     */
    suspend fun getTotalSaved(): Double {
        val allGoals = savingsGoalDao.getAllGoalsSnapshot().map { it.toDomain() }
        return allGoals.sumOf { it.currentAmount }
    }

    /**
     * Gets the count of active savings goals.
     *
     * @return Number of active goals
     */
    suspend fun getActiveGoalCount(): Int {
        return savingsGoalDao.getActiveGoalCount()
    }

    /**
     * Gets the count of completed savings goals.
     *
     * @return Number of completed goals
     */
    suspend fun getCompletedGoalCount(): Int {
        return savingsGoalDao.getCompletedGoalCount()
    }

    /**
     * Gets the total count of all savings goals.
     *
     * @return Total number of goals
     */
    suspend fun getGoalCount(): Int {
        return savingsGoalDao.getGoalCount()
    }

    /**
     * Deletes all savings goals and contributions.
     * Use with caution - this cannot be undone.
     *
     * @return Number of goals deleted
     */
    suspend fun deleteAllGoals(): Int {
        return savingsGoalDao.deleteAll()
    }
}
