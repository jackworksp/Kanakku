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

/**
 * Represents the budget progress for a specific category.
 *
 * Combines category information with budget and calculated progress
 * for UI display in category-specific budget cards.
 *
 * @param category The category this budget applies to
 * @param budget The budget configuration for this category
 * @param progress The calculated progress metrics
 */
data class CategoryBudgetProgress(
    val category: Category,
    val budget: Budget,
    val progress: BudgetProgress
)

/**
 * Summary of all budgets for a specific month, used for dashboard display.
 *
 * Provides a comprehensive overview including overall budget (if set),
 * individual category budgets, and aggregate totals.
 *
 * @param month The month (1-12) this summary is for
 * @param year The year this summary is for
 * @param overallProgress Overall monthly budget progress (null if not set)
 * @param categoryProgresses List of category-specific budget progress
 * @param totalSpent Total amount spent across all categories this month
 * @param totalBudget Total budget amount (overall budget if set, otherwise sum of category budgets)
 */
data class BudgetSummary(
    val month: Int,
    val year: Int,
    val overallProgress: BudgetProgress?,
    val categoryProgresses: List<CategoryBudgetProgress>,
    val totalSpent: Double,
    val totalBudget: Double
)
