package com.example.kanakku.domain.analytics

import com.example.kanakku.data.model.*
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsCalculator {

    fun calculatePeriodSummary(
        transactions: List<ParsedTransaction>,
        categoryMap: Map<Long, Category>,
        period: TimePeriod
    ): PeriodSummary {
        val now = System.currentTimeMillis()
        val startTime = now - (period.days * 24 * 60 * 60 * 1000L)

        val filtered = transactions.filter { it.date >= startTime }

        val totalSpent = filtered
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }

        val totalReceived = filtered
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }

        val categoryTotals = getCategoryBreakdown(filtered, categoryMap)
        val topCategory = categoryTotals.maxByOrNull { it.totalAmount }?.category

        val daysInPeriod = maxOf(1, period.days)
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

    fun getSpendingTrend(
        transactions: List<ParsedTransaction>,
        period: TimePeriod
    ): List<TrendPoint> {
        val now = System.currentTimeMillis()
        val startTime = now - (period.days * 24 * 60 * 60 * 1000L)

        val filtered = transactions
            .filter { it.date >= startTime && it.type == TransactionType.DEBIT }

        val dateFormat = when (period) {
            TimePeriod.DAY -> SimpleDateFormat("HH:00", Locale.getDefault())
            TimePeriod.WEEK -> SimpleDateFormat("EEE", Locale.getDefault())
            TimePeriod.MONTH -> SimpleDateFormat("dd", Locale.getDefault())
            TimePeriod.YEAR -> SimpleDateFormat("MMM", Locale.getDefault())
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

    fun getDailySpending(
        transactions: List<ParsedTransaction>,
        period: TimePeriod
    ): List<DaySpending> {
        val now = System.currentTimeMillis()
        val startTime = now - (period.days * 24 * 60 * 60 * 1000L)
        val dayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val filtered = transactions.filter { it.date >= startTime }

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
