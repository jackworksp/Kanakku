package com.example.kanakku.data.model

import java.time.LocalDate
import java.time.ZoneId

/**
 * Status of a savings goal
 */
enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    OVERDUE
}

/**
 * Domain model for a savings goal
 */
data class SavingsGoal(
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadline: Long,
    val createdAt: Long,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val icon: String?,
    val color: String?
) {
    /**
     * Calculate the current status of the goal
     */
    val status: GoalStatus
        get() = when {
            isCompleted -> GoalStatus.COMPLETED
            System.currentTimeMillis() > deadline -> GoalStatus.OVERDUE
            else -> GoalStatus.ACTIVE
        }

    /**
     * Calculate completion percentage (0.0 to 1.0)
     */
    val progressPercentage: Double
        get() = if (targetAmount > 0) {
            (currentAmount / targetAmount).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

    /**
     * Calculate remaining amount to reach goal
     */
    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)

    /**
     * Calculate days remaining until deadline
     */
    val daysRemaining: Long
        get() {
            val now = System.currentTimeMillis()
            val diff = deadline - now
            return (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
        }

    /**
     * Check if the goal is on track to be completed on time
     */
    val isOnTrack: Boolean
        get() {
            if (isCompleted) return true
            if (daysRemaining == 0L) return currentAmount >= targetAmount

            val daysPassed = (System.currentTimeMillis() - createdAt) / (1000 * 60 * 60 * 24)
            val totalDays = (deadline - createdAt) / (1000 * 60 * 60 * 24)

            if (totalDays <= 0) return false

            val expectedProgress = daysPassed.toDouble() / totalDays.toDouble()
            return progressPercentage >= expectedProgress
        }
}

/**
 * Domain model for a manual contribution to a savings goal
 */
data class GoalContribution(
    val id: Long = 0,
    val goalId: Long,
    val amount: Double,
    val date: Long,
    val note: String?
)

/**
 * Detailed progress information for a savings goal
 */
data class GoalProgress(
    val goal: SavingsGoal,
    val contributions: List<GoalContribution>
) {
    /**
     * Total amount contributed manually
     */
    val totalContributions: Double
        get() = contributions.sumOf { it.amount }

    /**
     * Completion percentage (0.0 to 100.0 for display)
     */
    val percentageComplete: Double
        get() = goal.progressPercentage * 100.0

    /**
     * Required daily savings to reach goal on time
     */
    val requiredDailySavings: Double
        get() {
            if (goal.isCompleted || goal.daysRemaining == 0L) return 0.0
            return goal.remainingAmount / goal.daysRemaining
        }

    /**
     * Required weekly savings to reach goal on time
     */
    val requiredWeeklySavings: Double
        get() = requiredDailySavings * 7

    /**
     * Required monthly savings to reach goal on time
     */
    val requiredMonthlySavings: Double
        get() = requiredDailySavings * 30

    /**
     * Projected completion date based on current progress rate
     */
    val projectedCompletionDate: Long?
        get() {
            if (goal.isCompleted) return goal.completedAt
            if (goal.currentAmount <= 0) return null

            val daysPassed = (System.currentTimeMillis() - goal.createdAt) / (1000 * 60 * 60 * 24)
            if (daysPassed <= 0) return null

            val dailyRate = goal.currentAmount / daysPassed
            if (dailyRate <= 0) return null

            val daysNeeded = (goal.targetAmount / dailyRate).toLong()
            return goal.createdAt + (daysNeeded * 24 * 60 * 60 * 1000)
        }

    /**
     * Average contribution amount
     */
    val averageContribution: Double
        get() = if (contributions.isNotEmpty()) {
            totalContributions / contributions.size
        } else {
            0.0
        }

    /**
     * Latest contribution date
     */
    val lastContributionDate: Long?
        get() = contributions.maxOfOrNull { it.date }
}
