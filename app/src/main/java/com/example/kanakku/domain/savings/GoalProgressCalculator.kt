package com.example.kanakku.domain.savings

import com.example.kanakku.data.model.GoalContribution
import com.example.kanakku.data.model.SavingsGoal

/**
 * Calculator for analyzing savings goal progress and providing progress metrics.
 * Provides calculations for tracking progress, projecting completion, and analyzing trends.
 */
class GoalProgressCalculator {

    /**
     * Calculate detailed progress metrics for a goal
     *
     * @param goal The savings goal to analyze
     * @param contributions List of contributions made to the goal
     * @return ProgressMetrics with comprehensive progress information
     */
    fun calculateProgressMetrics(
        goal: SavingsGoal,
        contributions: List<GoalContribution>
    ): ProgressMetrics {
        val percentageComplete = if (goal.targetAmount > 0) {
            ((goal.currentAmount / goal.targetAmount) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val daysRemaining = calculateDaysRemaining(goal)
        val requiredSavings = calculateRequiredSavings(goal, daysRemaining)
        val progressRate = calculateProgressRate(goal, contributions)
        val projectedCompletion = calculateProjectedCompletion(goal, progressRate)
        val isOnTrack = calculateIsOnTrack(goal, percentageComplete)

        return ProgressMetrics(
            percentageComplete = percentageComplete,
            daysRemaining = daysRemaining,
            requiredDailySavings = requiredSavings.daily,
            requiredWeeklySavings = requiredSavings.weekly,
            requiredMonthlySavings = requiredSavings.monthly,
            progressRate = progressRate,
            projectedCompletionDate = projectedCompletion,
            isOnTrack = isOnTrack,
            remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0),
            contributionCount = contributions.size
        )
    }

    /**
     * Calculate aggregate statistics for multiple goals
     *
     * @param goals List of savings goals
     * @return AggregateGoalStats with combined statistics
     */
    fun calculateAggregateStats(goals: List<SavingsGoal>): AggregateGoalStats {
        if (goals.isEmpty()) {
            return AggregateGoalStats(
                totalGoals = 0,
                activeGoals = 0,
                completedGoals = 0,
                overdueGoals = 0,
                totalTargetAmount = 0.0,
                totalCurrentAmount = 0.0,
                totalRemainingAmount = 0.0,
                overallProgressPercentage = 0.0,
                averageProgressPercentage = 0.0
            )
        }

        val activeGoals = goals.count { !it.isCompleted && System.currentTimeMillis() <= it.deadline }
        val completedGoals = goals.count { it.isCompleted }
        val overdueGoals = goals.count { !it.isCompleted && System.currentTimeMillis() > it.deadline }

        val totalTargetAmount = goals.sumOf { it.targetAmount }
        val totalCurrentAmount = goals.sumOf { it.currentAmount }
        val totalRemainingAmount = goals.sumOf { (it.targetAmount - it.currentAmount).coerceAtLeast(0.0) }

        val overallProgressPercentage = if (totalTargetAmount > 0) {
            ((totalCurrentAmount / totalTargetAmount) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        val averageProgressPercentage = goals.map { goal ->
            if (goal.targetAmount > 0) {
                ((goal.currentAmount / goal.targetAmount) * 100.0).coerceIn(0.0, 100.0)
            } else {
                0.0
            }
        }.average()

        return AggregateGoalStats(
            totalGoals = goals.size,
            activeGoals = activeGoals,
            completedGoals = completedGoals,
            overdueGoals = overdueGoals,
            totalTargetAmount = totalTargetAmount,
            totalCurrentAmount = totalCurrentAmount,
            totalRemainingAmount = totalRemainingAmount,
            overallProgressPercentage = overallProgressPercentage,
            averageProgressPercentage = averageProgressPercentage
        )
    }

    /**
     * Calculate when a specific milestone (amount) will be reached
     *
     * @param goal The savings goal
     * @param milestoneAmount The milestone amount to reach
     * @param contributions List of contributions for rate calculation
     * @return MilestoneProjection with estimated date and days remaining
     */
    fun calculateMilestoneProjection(
        goal: SavingsGoal,
        milestoneAmount: Double,
        contributions: List<GoalContribution>
    ): MilestoneProjection {
        if (goal.currentAmount >= milestoneAmount) {
            return MilestoneProjection(
                milestoneAmount = milestoneAmount,
                isReached = true,
                estimatedDate = System.currentTimeMillis(),
                daysToMilestone = 0
            )
        }

        val progressRate = calculateProgressRate(goal, contributions)
        if (progressRate <= 0) {
            return MilestoneProjection(
                milestoneAmount = milestoneAmount,
                isReached = false,
                estimatedDate = null,
                daysToMilestone = null
            )
        }

        val remainingAmount = milestoneAmount - goal.currentAmount
        val daysNeeded = (remainingAmount / progressRate).toLong()
        val estimatedDate = System.currentTimeMillis() + (daysNeeded * 24 * 60 * 60 * 1000)

        return MilestoneProjection(
            milestoneAmount = milestoneAmount,
            isReached = false,
            estimatedDate = estimatedDate,
            daysToMilestone = daysNeeded
        )
    }

    /**
     * Calculate contribution trend over time
     *
     * @param contributions List of contributions to analyze
     * @param periodDays Number of days to group contributions by (default 7 for weekly)
     * @return ContributionTrend with trend analysis
     */
    fun calculateContributionTrend(
        contributions: List<GoalContribution>,
        periodDays: Int = 7
    ): ContributionTrend {
        if (contributions.isEmpty()) {
            return ContributionTrend(
                averageAmount = 0.0,
                totalAmount = 0.0,
                contributionCount = 0,
                averageFrequencyDays = null,
                isIncreasing = false,
                recentAverage = 0.0
            )
        }

        val sortedContributions = contributions.sortedBy { it.date }
        val totalAmount = sortedContributions.sumOf { it.amount }
        val averageAmount = totalAmount / sortedContributions.size

        // Calculate average frequency
        val averageFrequencyDays = if (sortedContributions.size >= 2) {
            val firstDate = sortedContributions.first().date
            val lastDate = sortedContributions.last().date
            val daysDiff = ((lastDate - firstDate) / (24 * 60 * 60 * 1000)).toDouble()
            daysDiff / (sortedContributions.size - 1)
        } else {
            null
        }

        // Calculate if trend is increasing by comparing recent vs older averages
        val recentCount = (sortedContributions.size / 2).coerceAtLeast(1)
        val recentContributions = sortedContributions.takeLast(recentCount)
        val recentAverage = recentContributions.sumOf { it.amount } / recentContributions.size

        val isIncreasing = recentAverage > averageAmount

        return ContributionTrend(
            averageAmount = averageAmount,
            totalAmount = totalAmount,
            contributionCount = sortedContributions.size,
            averageFrequencyDays = averageFrequencyDays,
            isIncreasing = isIncreasing,
            recentAverage = recentAverage
        )
    }

    /**
     * Calculate days remaining until deadline
     */
    private fun calculateDaysRemaining(goal: SavingsGoal): Long {
        if (goal.isCompleted) return 0L

        val now = System.currentTimeMillis()
        val diff = goal.deadline - now
        return (diff / (24 * 60 * 60 * 1000)).coerceAtLeast(0L)
    }

    /**
     * Calculate required savings amounts
     */
    private fun calculateRequiredSavings(goal: SavingsGoal, daysRemaining: Long): RequiredSavings {
        if (goal.isCompleted || daysRemaining == 0L) {
            return RequiredSavings(daily = 0.0, weekly = 0.0, monthly = 0.0)
        }

        val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val dailyAmount = remainingAmount / daysRemaining

        return RequiredSavings(
            daily = dailyAmount,
            weekly = dailyAmount * 7,
            monthly = dailyAmount * 30
        )
    }

    /**
     * Calculate daily progress rate based on contribution history
     */
    private fun calculateProgressRate(goal: SavingsGoal, contributions: List<GoalContribution>): Double {
        if (goal.currentAmount <= 0 || contributions.isEmpty()) {
            // Fallback to simple rate based on total progress
            val daysPassed = (System.currentTimeMillis() - goal.createdAt) / (24 * 60 * 60 * 1000)
            return if (daysPassed > 0) {
                goal.currentAmount / daysPassed
            } else {
                0.0
            }
        }

        // Calculate rate based on contribution history
        val sortedContributions = contributions.sortedBy { it.date }
        val firstContribution = sortedContributions.first()
        val lastContribution = sortedContributions.last()

        val daysBetween = ((lastContribution.date - firstContribution.date) / (24 * 60 * 60 * 1000)).coerceAtLeast(1)
        val totalContributions = contributions.sumOf { it.amount }

        return totalContributions / daysBetween
    }

    /**
     * Calculate projected completion date based on current progress rate
     */
    private fun calculateProjectedCompletion(goal: SavingsGoal, progressRate: Double): Long? {
        if (goal.isCompleted) return goal.completedAt
        if (progressRate <= 0) return null

        val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val daysNeeded = (remainingAmount / progressRate).toLong()

        return System.currentTimeMillis() + (daysNeeded * 24 * 60 * 60 * 1000)
    }

    /**
     * Determine if the goal is on track to complete on time
     */
    private fun calculateIsOnTrack(goal: SavingsGoal, percentageComplete: Double): Boolean {
        if (goal.isCompleted) return true

        val now = System.currentTimeMillis()
        if (now > goal.deadline) return false

        val totalDuration = goal.deadline - goal.createdAt
        val elapsed = now - goal.createdAt

        if (totalDuration <= 0) return false

        val expectedProgress = ((elapsed.toDouble() / totalDuration.toDouble()) * 100.0)
        return percentageComplete >= expectedProgress
    }
}

/**
 * Comprehensive progress metrics for a savings goal
 */
data class ProgressMetrics(
    val percentageComplete: Double,           // 0.0 to 100.0
    val daysRemaining: Long,
    val requiredDailySavings: Double,
    val requiredWeeklySavings: Double,
    val requiredMonthlySavings: Double,
    val progressRate: Double,                 // Daily progress rate
    val projectedCompletionDate: Long?,       // Null if no progress
    val isOnTrack: Boolean,
    val remainingAmount: Double,
    val contributionCount: Int
)

/**
 * Internal data class for required savings amounts
 */
private data class RequiredSavings(
    val daily: Double,
    val weekly: Double,
    val monthly: Double
)

/**
 * Aggregate statistics across multiple goals
 */
data class AggregateGoalStats(
    val totalGoals: Int,
    val activeGoals: Int,
    val completedGoals: Int,
    val overdueGoals: Int,
    val totalTargetAmount: Double,
    val totalCurrentAmount: Double,
    val totalRemainingAmount: Double,
    val overallProgressPercentage: Double,    // Weighted by target amounts
    val averageProgressPercentage: Double     // Simple average across goals
)

/**
 * Projection for reaching a specific milestone
 */
data class MilestoneProjection(
    val milestoneAmount: Double,
    val isReached: Boolean,
    val estimatedDate: Long?,                 // Null if cannot project
    val daysToMilestone: Long?                // Null if cannot project
)

/**
 * Analysis of contribution trends over time
 */
data class ContributionTrend(
    val averageAmount: Double,
    val totalAmount: Double,
    val contributionCount: Int,
    val averageFrequencyDays: Double?,        // Null if < 2 contributions
    val isIncreasing: Boolean,                // Recent average > overall average
    val recentAverage: Double
)
