package com.example.kanakku.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisting savings goals.
 * Represents a user's savings goal with target amount, deadline, and progress tracking.
 */
@Entity(
    tableName = "savings_goals",
    indices = [
        Index(value = ["deadline"]),
        Index(value = ["isCompleted"]),
        Index(value = ["createdAt"])
    ]
)
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
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
)
