package com.example.kanakku.data.model

/**
 * Domain model representing a detected recurring transaction pattern.
 * Used to track subscriptions, EMIs, salaries, rent, and other recurring payments.
 */
data class RecurringTransaction(
    val id: String,                          // Unique identifier (UUID)
    val merchantPattern: String,             // Normalized merchant name pattern
    val amount: Double,                      // Expected transaction amount
    val frequency: RecurringFrequency,       // How often it recurs
    val averageInterval: Int,                // Average days between transactions
    val lastOccurrence: Long,                // Timestamp of last matching transaction
    val nextExpected: Long,                  // Predicted timestamp of next transaction
    val transactionIds: List<Long>,          // SMS IDs of transactions matching this pattern
    val isUserConfirmed: Boolean,            // Whether user confirmed this recurring pattern
    val type: RecurringType                  // Category of recurring transaction
)

/**
 * Frequency patterns for recurring transactions.
 */
enum class RecurringFrequency(val displayName: String) {
    WEEKLY("Weekly"),
    BI_WEEKLY("Bi-weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    ANNUAL("Annual")
}

/**
 * Types of recurring transactions for categorization.
 */
enum class RecurringType(val displayName: String, val icon: String) {
    SUBSCRIPTION("Subscription", "🔄"),
    EMI("EMI", "💳"),
    SALARY("Salary", "💰"),
    RENT("Rent", "🏠"),
    UTILITY("Utility", "⚡"),
    OTHER("Other", "📌")
}
