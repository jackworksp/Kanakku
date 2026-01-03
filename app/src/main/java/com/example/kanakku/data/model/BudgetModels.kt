package com.example.kanakku.data.model

/**
 * Represents the calculated progress of a budget.
 *
 * @param spent The amount already spent
 * @param limit The budget limit amount
 * @param remaining The remaining budget (limit - spent)
 * @param percentage The percentage spent (0-100+)
 * @param status The budget status based on thresholds
 */
data class BudgetProgress(
    val spent: Double,
    val limit: Double,
    val remaining: Double,
    val percentage: Double,
    val status: BudgetStatus
)
