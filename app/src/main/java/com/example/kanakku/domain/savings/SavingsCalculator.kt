package com.example.kanakku.domain.savings

import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TimePeriod
import com.example.kanakku.data.model.TransactionType

/**
 * Calculator for analyzing transaction patterns and suggesting achievable savings amounts.
 * Analyzes income (credits) and spending (debits) to provide realistic savings recommendations.
 */
class SavingsCalculator {

    /**
     * Calculate savings suggestions based on transaction history
     *
     * @param transactions List of transactions to analyze
     * @param period Time period to analyze (defaults to MONTH)
     * @return SavingsSuggestion with recommended savings amounts
     */
    fun calculateSavingsSuggestion(
        transactions: List<ParsedTransaction>,
        period: TimePeriod = TimePeriod.MONTH
    ): SavingsSuggestion {
        val now = System.currentTimeMillis()
        val startTime = now - (period.days * 24 * 60 * 60 * 1000L)

        val filtered = transactions.filter { it.date >= startTime }

        val totalIncome = filtered
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }

        val totalSpending = filtered
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }

        val netIncome = totalIncome - totalSpending

        // Calculate average daily values
        val daysInPeriod = maxOf(1, period.days)
        val avgDailyIncome = totalIncome / daysInPeriod
        val avgDailySpending = totalSpending / daysInPeriod

        // Calculate spending variance to assess stability
        val variance = calculateSpendingVariance(filtered, avgDailySpending)

        // Generate suggestions based on income, spending, and stability
        return generateSuggestions(
            totalIncome = totalIncome,
            totalSpending = totalSpending,
            netIncome = netIncome,
            avgDailySpending = avgDailySpending,
            variance = variance,
            period = period
        )
    }

    /**
     * Calculate detailed income and expense analysis
     *
     * @param transactions List of transactions to analyze
     * @param period Time period to analyze
     * @return IncomeExpenseAnalysis with detailed breakdown
     */
    fun analyzeIncomeExpense(
        transactions: List<ParsedTransaction>,
        period: TimePeriod = TimePeriod.MONTH
    ): IncomeExpenseAnalysis {
        val now = System.currentTimeMillis()
        val startTime = now - (period.days * 24 * 60 * 60 * 1000L)

        val filtered = transactions.filter { it.date >= startTime }

        val credits = filtered.filter { it.type == TransactionType.CREDIT }
        val debits = filtered.filter { it.type == TransactionType.DEBIT }

        val totalIncome = credits.sumOf { it.amount }
        val totalExpense = debits.sumOf { it.amount }

        val daysInPeriod = maxOf(1, period.days)

        return IncomeExpenseAnalysis(
            period = period,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netIncome = totalIncome - totalExpense,
            incomeTransactionCount = credits.size,
            expenseTransactionCount = debits.size,
            averageDailyIncome = totalIncome / daysInPeriod,
            averageDailyExpense = totalExpense / daysInPeriod,
            savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome) * 100 else 0.0
        )
    }

    /**
     * Calculate recommended monthly savings based on financial goals
     *
     * @param targetAmount Target amount to save
     * @param deadline Deadline timestamp
     * @param currentSavings Current amount saved
     * @param transactions Recent transactions for affordability check
     * @return MonthlySavingsRecommendation with realistic monthly amount
     */
    fun calculateMonthlyRecommendation(
        targetAmount: Double,
        deadline: Long,
        currentSavings: Double = 0.0,
        transactions: List<ParsedTransaction> = emptyList()
    ): MonthlySavingsRecommendation {
        val now = System.currentTimeMillis()
        val timeRemaining = deadline - now
        val monthsRemaining = maxOf(1, (timeRemaining / (30L * 24 * 60 * 60 * 1000)).toInt())

        val remainingAmount = (targetAmount - currentSavings).coerceAtLeast(0.0)
        val requiredMonthly = remainingAmount / monthsRemaining

        // Analyze affordability if transactions provided
        val isAffordable = if (transactions.isNotEmpty()) {
            val analysis = analyzeIncomeExpense(transactions, TimePeriod.MONTH)
            requiredMonthly <= analysis.netIncome
        } else {
            true
        }

        return MonthlySavingsRecommendation(
            requiredMonthly = requiredMonthly,
            monthsRemaining = monthsRemaining,
            isAffordable = isAffordable,
            targetAmount = targetAmount,
            currentSavings = currentSavings
        )
    }

    /**
     * Calculate spending variance to assess financial stability
     */
    private fun calculateSpendingVariance(
        transactions: List<ParsedTransaction>,
        avgDailySpending: Double
    ): Double {
        val debits = transactions.filter { it.type == TransactionType.DEBIT }

        if (debits.isEmpty()) return 0.0

        // Group by day
        val dailySpending = debits
            .groupBy { it.date / (24 * 60 * 60 * 1000) }
            .map { (_, txns) -> txns.sumOf { it.amount } }

        if (dailySpending.isEmpty()) return 0.0

        // Calculate variance
        val variance = dailySpending
            .map { (it - avgDailySpending) * (it - avgDailySpending) }
            .average()

        return variance
    }

    /**
     * Generate savings suggestions based on financial patterns
     */
    private fun generateSuggestions(
        totalIncome: Double,
        totalSpending: Double,
        netIncome: Double,
        avgDailySpending: Double,
        variance: Double,
        period: TimePeriod
    ): SavingsSuggestion {
        // If spending exceeds income, suggest minimal savings
        if (netIncome <= 0) {
            return SavingsSuggestion(
                conservative = 0.0,
                moderate = 0.0,
                aggressive = 0.0,
                recommendedLevel = SavingsLevel.CONSERVATIVE,
                period = period,
                analysis = "Current spending exceeds income. Focus on reducing expenses before setting savings goals."
            )
        }

        // Calculate suggestions as percentages of net income
        // Conservative: 20% of net income (safe for most people)
        val conservative = netIncome * 0.20

        // Moderate: 35% of net income (balanced approach)
        val moderate = netIncome * 0.35

        // Aggressive: 50% of net income (for highly motivated savers)
        val aggressive = netIncome * 0.50

        // Determine recommended level based on spending stability
        // Higher variance means less stable spending, suggest conservative
        val normalizedVariance = if (avgDailySpending > 0) variance / (avgDailySpending * avgDailySpending) else 0.0

        val recommendedLevel = when {
            normalizedVariance > 1.0 -> SavingsLevel.CONSERVATIVE
            netIncome / totalIncome > 0.4 -> SavingsLevel.AGGRESSIVE
            netIncome / totalIncome > 0.25 -> SavingsLevel.MODERATE
            else -> SavingsLevel.CONSERVATIVE
        }

        val analysis = buildAnalysisText(
            totalIncome = totalIncome,
            totalSpending = totalSpending,
            netIncome = netIncome,
            recommendedLevel = recommendedLevel,
            period = period
        )

        return SavingsSuggestion(
            conservative = conservative,
            moderate = moderate,
            aggressive = aggressive,
            recommendedLevel = recommendedLevel,
            period = period,
            analysis = analysis
        )
    }

    /**
     * Build human-readable analysis text
     */
    private fun buildAnalysisText(
        totalIncome: Double,
        totalSpending: Double,
        netIncome: Double,
        recommendedLevel: SavingsLevel,
        period: TimePeriod
    ): String {
        val savingsRate = (netIncome / totalIncome) * 100

        return when (recommendedLevel) {
            SavingsLevel.CONSERVATIVE ->
                "Your ${period.displayName.lowercase()} savings rate is ${savingsRate.toInt()}%. Start with a conservative goal to build the habit."
            SavingsLevel.MODERATE ->
                "With ${savingsRate.toInt()}% savings rate, you're doing well! Consider a moderate savings goal."
            SavingsLevel.AGGRESSIVE ->
                "Excellent! Your ${savingsRate.toInt()}% savings rate allows for aggressive goals. You can save even more!"
        }
    }
}

/**
 * Savings suggestion with three tiers of recommendations
 */
data class SavingsSuggestion(
    val conservative: Double,     // 20% of net income - safe baseline
    val moderate: Double,          // 35% of net income - balanced
    val aggressive: Double,        // 50% of net income - ambitious
    val recommendedLevel: SavingsLevel,
    val period: TimePeriod,
    val analysis: String
)

/**
 * Level of savings aggressiveness
 */
enum class SavingsLevel {
    CONSERVATIVE,
    MODERATE,
    AGGRESSIVE
}

/**
 * Detailed income and expense analysis
 */
data class IncomeExpenseAnalysis(
    val period: TimePeriod,
    val totalIncome: Double,
    val totalExpense: Double,
    val netIncome: Double,
    val incomeTransactionCount: Int,
    val expenseTransactionCount: Int,
    val averageDailyIncome: Double,
    val averageDailyExpense: Double,
    val savingsRate: Double          // Percentage (0-100)
)

/**
 * Monthly savings recommendation for a specific goal
 */
data class MonthlySavingsRecommendation(
    val requiredMonthly: Double,
    val monthsRemaining: Int,
    val isAffordable: Boolean,
    val targetAmount: Double,
    val currentSavings: Double
)
