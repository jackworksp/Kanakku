package com.example.kanakku.domain.analytics

import com.example.kanakku.data.model.*
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsCalculator {

    /**
     * Calculate summary for a date range.
     * Overloaded method that accepts DateRange instead of TimePeriod.
     */
    fun calculatePeriodSummary(
        transactions: List<ParsedTransaction>,
        categoryMap: Map<Long, Category>,
        dateRange: DateRange
    ): PeriodSummary {
        val filtered = transactions.filter {
            it.date >= dateRange.startDate && it.date <= dateRange.endDate
        }

        val totalSpent = filtered
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }

        val totalReceived = filtered
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }

        val categoryTotals = getCategoryBreakdown(filtered, categoryMap)
        val topCategory = categoryTotals.maxByOrNull { it.totalAmount }?.category

        val daysInPeriod = maxOf(1, dateRange.durationInDays)
        val averageDaily = totalSpent / daysInPeriod

        return PeriodSummary(
            period = null,
            totalSpent = totalSpent,
            totalReceived = totalReceived,
            transactionCount = filtered.size,
            averageDaily = averageDaily,
            topCategory = topCategory
        )
    }

    /**
     * Calculate summary for a time period.
     * Existing method kept for backward compatibility.
     */
    fun calculatePeriodSummary(
        transactions: List<ParsedTransaction>,
        categoryMap: Map<Long, Category>,
        period: TimePeriod
    ): PeriodSummary {
        // For ALL_TIME, use all transactions; otherwise filter by time period
        val filtered = if (period == TimePeriod.ALL_TIME) {
            transactions
        } else {
            val now = System.currentTimeMillis()
            val startTime = now - (period.days * 24 * 60 * 60 * 1000L)
            transactions.filter { it.date >= startTime }
        }

        val totalSpent = filtered
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }

        val totalReceived = filtered
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }

        val categoryTotals = getCategoryBreakdown(filtered, categoryMap)
        val topCategory = categoryTotals.maxByOrNull { it.totalAmount }?.category

        // Calculate average daily for ALL_TIME based on actual date range
        val daysInPeriod = if (period == TimePeriod.ALL_TIME && filtered.isNotEmpty()) {
            val oldestDate = filtered.minByOrNull { it.date }?.date ?: System.currentTimeMillis()
            val newestDate = filtered.maxByOrNull { it.date }?.date ?: System.currentTimeMillis()
            val daysDiff = ((newestDate - oldestDate) / (24 * 60 * 60 * 1000L)).toInt()
            maxOf(1, daysDiff)
        } else {
            maxOf(1, period.days)
        }
        val averageDaily = totalSpent / daysInPeriod

        return PeriodSummary(
            period = period,
            totalSpent = totalSpent,
            totalReceived = totalReceived,
            transactionCount = filtered.size,
            averageDaily = averageDaily,
            topCategory = topCategory
        )
    }

    fun getCategoryBreakdown(
        transactions: List<ParsedTransaction>,
        categoryMap: Map<Long, Category>
    ): List<CategoryTotal> {
        val debitTransactions = transactions.filter { it.type == TransactionType.DEBIT }
        val totalSpent = debitTransactions.sumOf { it.amount }

        return debitTransactions
            .groupBy { categoryMap[it.smsId] ?: DefaultCategories.OTHER }
            .map { (category, txns) ->
                val amount = txns.sumOf { it.amount }
                CategoryTotal(
                    category = category,
                    totalAmount = amount,
                    transactionCount = txns.size,
                    percentage = if (totalSpent > 0) (amount / totalSpent) * 100 else 0.0
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    /**
     * Get spending trend for a date range with smart date format selection.
     * Overloaded method that accepts DateRange instead of TimePeriod.
     */
    fun getSpendingTrend(
        transactions: List<ParsedTransaction>,
        dateRange: DateRange
    ): List<TrendPoint> {
        val filtered = transactions
            .filter {
                it.date >= dateRange.startDate &&
                it.date <= dateRange.endDate &&
                it.type == TransactionType.DEBIT
            }

        // Smart date format selection based on range duration
        val dateFormat = when {
            dateRange.durationInDays <= 1 -> SimpleDateFormat("HH:00", Locale.getDefault())
            dateRange.durationInDays <= 14 -> SimpleDateFormat("EEE", Locale.getDefault())
            dateRange.durationInDays <= 90 -> SimpleDateFormat("dd", Locale.getDefault())
            else -> SimpleDateFormat("MMM", Locale.getDefault())
        }

        return filtered
            .groupBy { dateFormat.format(Date(it.date)) }
            .map { (label, txns) ->
                TrendPoint(
                    label = label,
                    date = txns.first().date,
                    amount = txns.sumOf { it.amount }
                )
            }
            .sortedBy { it.date }
    }

    /**
     * Get spending trend for a time period.
     * Existing method kept for backward compatibility.
     */
    fun getSpendingTrend(
        transactions: List<ParsedTransaction>,
        period: TimePeriod
    ): List<TrendPoint> {
        // For ALL_TIME, use all transactions; otherwise filter by time period
        val filtered = if (period == TimePeriod.ALL_TIME) {
            transactions.filter { it.type == TransactionType.DEBIT }
        } else {
            val now = System.currentTimeMillis()
            val startTime = now - (period.days * 24 * 60 * 60 * 1000L)
            transactions.filter { it.date >= startTime && it.type == TransactionType.DEBIT }
        }

        // Handle empty data gracefully
        if (filtered.isEmpty()) {
            return emptyList()
        }

        val dateFormat = when (period) {
            TimePeriod.DAY -> SimpleDateFormat("HH:00", Locale.getDefault())
            TimePeriod.WEEK -> SimpleDateFormat("EEE", Locale.getDefault())
            TimePeriod.MONTH -> SimpleDateFormat("dd", Locale.getDefault())
            TimePeriod.YEAR -> SimpleDateFormat("MMM", Locale.getDefault())
            TimePeriod.ALL_TIME -> SimpleDateFormat("MMM yyyy", Locale.getDefault())
        }

        return filtered
            .groupBy { dateFormat.format(Date(it.date)) }
            .map { (label, txns) ->
                TrendPoint(
                    label = label,
                    date = txns.first().date,
                    amount = txns.sumOf { it.amount }
                )
            }
            .sortedBy { it.date }
    }

    /**
     * Get daily spending breakdown for a date range with smart date formatting.
     * Overloaded method that accepts DateRange instead of TimePeriod.
     */
    fun getDailySpending(
        transactions: List<ParsedTransaction>,
        dateRange: DateRange
    ): List<DaySpending> {
        // Smart date format selection based on range duration
        val dayFormat = when {
            dateRange.durationInDays <= 14 -> SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
            dateRange.durationInDays <= 90 -> SimpleDateFormat("dd MMM", Locale.getDefault())
            else -> SimpleDateFormat("MMM yy", Locale.getDefault())
        }
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val filtered = transactions.filter {
            it.date >= dateRange.startDate && it.date <= dateRange.endDate
        }

        val grouped = filtered.groupBy { dayKeyFormat.format(Date(it.date)) }

        return grouped.map { (dayKey, txns) ->
            val spent = txns.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
            val received = txns.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
            val firstDate = txns.first().date

            DaySpending(
                dayLabel = dayFormat.format(Date(firstDate)),
                date = firstDate,
                spent = spent,
                received = received
            )
        }.sortedBy { it.date }
    }

    /**
     * Get daily spending breakdown for a time period.
     * Existing method kept for backward compatibility.
     */
    fun getDailySpending(
        transactions: List<ParsedTransaction>,
        period: TimePeriod
    ): List<DaySpending> {
        // For ALL_TIME, use all transactions; otherwise filter by time period
        val filtered = if (period == TimePeriod.ALL_TIME) {
            transactions
        } else {
            val now = System.currentTimeMillis()
            val startTime = now - (period.days * 24 * 60 * 60 * 1000L)
            transactions.filter { it.date >= startTime }
        }

        // Handle empty data gracefully
        if (filtered.isEmpty()) {
            return emptyList()
        }

        // For ALL_TIME, group by month instead of day for better visualization
        val dayFormat = if (period == TimePeriod.ALL_TIME) {
            SimpleDateFormat("MMM yyyy", Locale.getDefault())
        } else {
            SimpleDateFormat("dd MMM", Locale.getDefault())
        }
        val dayKeyFormat = if (period == TimePeriod.ALL_TIME) {
            SimpleDateFormat("yyyy-MM", Locale.getDefault())
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }

        val grouped = filtered.groupBy { dayKeyFormat.format(Date(it.date)) }

        return grouped.map { (dayKey, txns) ->
            val spent = txns.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
            val received = txns.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
            val firstDate = txns.first().date

            DaySpending(
                dayLabel = dayFormat.format(Date(firstDate)),
                date = firstDate,
                spent = spent,
                received = received
            )
        }.sortedBy { it.date }
    }

    fun getTopMerchants(
        transactions: List<ParsedTransaction>,
        limit: Int = 5
    ): List<MerchantTotal> {
        return transactions
            .filter { it.type == TransactionType.DEBIT && it.merchant != null }
            .groupBy { it.merchant!! }
            .map { (merchant, txns) ->
                MerchantTotal(
                    merchantName = merchant,
                    totalAmount = txns.sumOf { it.amount },
                    transactionCount = txns.size
                )
            }
            .sortedByDescending { it.totalAmount }
            .take(limit)
    }
}
