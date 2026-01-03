package com.example.kanakku.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisting budget data.
 * Stores monthly budgets for overall spending and per-category limits.
 *
 * A null categoryId represents the overall monthly budget.
 * Non-null categoryId represents a category-specific budget.
 */
@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["month", "year"]),
        Index(value = ["categoryId", "month", "year"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: String?,
    val amount: Double,
    val month: Int,
    val year: Int,
    val createdAt: Long
)
