package com.example.kanakku.widget.model

import com.example.kanakku.data.model.TransactionType

/**
 * Data model for Today's Spending widget.
 * Displays the total amount spent today.
 */
data class TodaySpendingData(
    val todayTotal: Double,
    val lastUpdated: Long,
    val currency: String = "₹"
)

/**
 * Data model for Budget Progress widget.
 * Shows spending vs budget with percentage completion.
 */
data class BudgetProgressData(
    val spent: Double,
    val budget: Double,
    val percentage: Double,
    val periodLabel: String
)

/**
 * Simplified transaction item for widget display.
 * Contains essential information needed for widget UI.
 */
data class WidgetTransaction(
    val id: Long,
    val merchant: String,
    val amount: Double,
    val type: TransactionType,
    val date: Long
)

/**
 * Data model for Recent Transactions widget.
 * Contains a list of recent transactions for quick overview.
 */
data class RecentTransactionsData(
    val transactions: List<WidgetTransaction>,
    val lastUpdated: Long
)
