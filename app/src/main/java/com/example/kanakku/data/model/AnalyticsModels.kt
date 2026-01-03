package com.example.kanakku.data.model

enum class TimePeriod(val displayName: String, val days: Int) {
    DAY("Today", 1),
    WEEK("This Week", 7),
    MONTH("This Month", 30),
    YEAR("This Year", 365),
    ALL_TIME("All Time", -1)  // -1 indicates no time filtering
}

data class PeriodSummary(
    val period: TimePeriod? = null,
    val totalSpent: Double,
    val totalReceived: Double,
    val transactionCount: Int,
    val averageDaily: Double,
    val topCategory: Category?
)

data class CategoryTotal(
    val category: Category,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Double
)

data class TrendPoint(
    val label: String,
    val date: Long,
    val amount: Double
)

data class MerchantTotal(
    val merchantName: String,
    val totalAmount: Double,
    val transactionCount: Int
)

data class DaySpending(
    val dayLabel: String,
    val date: Long,
    val spent: Double,
    val received: Double
)
