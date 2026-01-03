package com.example.kanakku.data.model

/**
 * Domain model representing a budget for overall spending or a specific category.
 *
 * A null categoryId represents the overall monthly budget.
 * Non-null categoryId represents a category-specific budget.
 */
data class Budget(
    val id: Long = 0,
    val categoryId: String?,
    val amount: Double,
    val month: Int,
    val year: Int,
    val createdAt: Long
)

/**
 * Enum representing the status of a budget based on spending.
 *
 * UNDER_BUDGET: Spending is less than 80% of budget (green)
 * APPROACHING: Spending is between 80-100% of budget (yellow/amber)
 * EXCEEDED: Spending has exceeded the budget (red)
 */
enum class BudgetStatus {
    UNDER_BUDGET,
    APPROACHING,
    EXCEEDED
}
