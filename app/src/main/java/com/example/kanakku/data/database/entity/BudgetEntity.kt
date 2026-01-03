package com.example.kanakku.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.kanakku.data.model.BudgetPeriod

/**
 * Room entity for persisting budget limits per category.
 * Stores monthly/weekly spending limits for budget tracking and alerts.
 */
@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["categoryId", "period"], unique = true),
        Index(value = ["period"])
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: String,
    val amount: Double,
    val period: BudgetPeriod,
    val startDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
