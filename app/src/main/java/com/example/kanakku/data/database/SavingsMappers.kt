package com.example.kanakku.data.database

import com.example.kanakku.data.database.entity.GoalContributionEntity
import com.example.kanakku.data.database.entity.SavingsGoalEntity
import com.example.kanakku.data.model.GoalContribution
import com.example.kanakku.data.model.SavingsGoal

/**
 * Extension functions for mapping between savings-related database entities and domain models.
 *
 * These mappers provide bidirectional conversion between:
 * - SavingsGoalEntity <-> SavingsGoal
 * - GoalContributionEntity <-> GoalContribution
 *
 * The mapping is 1:1 with no data loss or transformation.
 */

/**
 * Converts a SavingsGoal domain model to a SavingsGoalEntity for database storage.
 *
 * @return SavingsGoalEntity with all fields mapped from the domain model
 */
fun SavingsGoal.toEntity(): SavingsGoalEntity {
    return SavingsGoalEntity(
        id = this.id,
        name = this.name,
        targetAmount = this.targetAmount,
        currentAmount = this.currentAmount,
        deadline = this.deadline,
        createdAt = this.createdAt,
        isCompleted = this.isCompleted,
        completedAt = this.completedAt,
        icon = this.icon,
        color = this.color
    )
}

/**
 * Converts a SavingsGoalEntity from the database to a SavingsGoal domain model.
 *
 * @return SavingsGoal with all fields mapped from the database entity
 */
fun SavingsGoalEntity.toDomain(): SavingsGoal {
    return SavingsGoal(
        id = this.id,
        name = this.name,
        targetAmount = this.targetAmount,
        currentAmount = this.currentAmount,
        deadline = this.deadline,
        createdAt = this.createdAt,
        isCompleted = this.isCompleted,
        completedAt = this.completedAt,
        icon = this.icon,
        color = this.color
    )
}

/**
 * Converts a GoalContribution domain model to a GoalContributionEntity for database storage.
 *
 * @return GoalContributionEntity with all fields mapped from the domain model
 */
fun GoalContribution.toEntity(): GoalContributionEntity {
    return GoalContributionEntity(
        id = this.id,
        goalId = this.goalId,
        amount = this.amount,
        date = this.date,
        note = this.note
    )
}

/**
 * Converts a GoalContributionEntity from the database to a GoalContribution domain model.
 *
 * @return GoalContribution with all fields mapped from the database entity
 */
fun GoalContributionEntity.toDomain(): GoalContribution {
    return GoalContribution(
        id = this.id,
        goalId = this.goalId,
        amount = this.amount,
        date = this.date,
        note = this.note
    )
}
