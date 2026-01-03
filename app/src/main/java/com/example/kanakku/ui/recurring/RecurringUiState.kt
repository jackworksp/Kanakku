package com.example.kanakku.ui.recurring

import com.example.kanakku.data.model.RecurringTransaction

/**
 * UI state for the recurring transactions screen.
 *
 * This state contains all data needed to display recurring transactions,
 * including the list of detected patterns, summary statistics, and error states.
 *
 * @property isLoading Whether data is currently being loaded
 * @property recurringTransactions List of all detected recurring transaction patterns
 * @property errorMessage Error message to display to user, null if no error
 * @property totalMonthlyRecurring Estimated total monthly recurring expenses
 * @property confirmedCount Number of user-confirmed recurring patterns
 * @property upcomingCount Number of recurring transactions expected in next 7 days
 * @property subscriptionCount Number of subscription-type recurring transactions
 * @property emiCount Number of EMI-type recurring transactions
 * @property salaryCount Number of salary-type recurring transactions
 * @property rentCount Number of rent-type recurring transactions
 * @property utilityCount Number of utility-type recurring transactions
 * @property otherCount Number of other-type recurring transactions
 */
data class RecurringUiState(
    val isLoading: Boolean = false,
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val errorMessage: String? = null,
    val totalMonthlyRecurring: Double = 0.0,
    val confirmedCount: Int = 0,
    val upcomingCount: Int = 0,
    val subscriptionCount: Int = 0,
    val emiCount: Int = 0,
    val salaryCount: Int = 0,
    val rentCount: Int = 0,
    val utilityCount: Int = 0,
    val otherCount: Int = 0
)
