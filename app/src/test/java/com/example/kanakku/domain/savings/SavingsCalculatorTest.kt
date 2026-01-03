package com.example.kanakku.domain.savings

import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TimePeriod
import com.example.kanakku.data.model.TransactionType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SavingsCalculator
 *
 * Tests cover:
 * - Savings suggestions with various income/spending patterns
 * - Income/expense analysis
 * - Monthly savings recommendations
 * - Edge cases and boundary conditions
 */
class SavingsCalculatorTest {

    private lateinit var calculator: SavingsCalculator

    @Before
    fun setup() {
        calculator = SavingsCalculator()
    }

    // ==================== Helper Functions ====================

    /**
     * Create a test transaction
     */
    private fun createTransaction(
        amount: Double,
        type: TransactionType,
        daysAgo: Int = 0
    ): ParsedTransaction {
        val now = System.currentTimeMillis()
        val date = now - (daysAgo * 24 * 60 * 60 * 1000L)

        return ParsedTransaction(
            smsId = 1L,
            amount = amount,
            type = type,
            merchant = "Test Merchant",
            accountNumber = "1234",
            referenceNumber = "REF123",
            date = date,
            rawSms = "Test SMS",
            senderAddress = "BANK"
        )
    }

    /**
     * Create a list of transactions with income and spending
     */
    private fun createBalancedTransactions(
        monthlyIncome: Double,
        monthlySpending: Double
    ): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()

        // Add income transactions (spread over the month)
        transactions.add(createTransaction(monthlyIncome / 2, TransactionType.CREDIT, daysAgo = 5))
        transactions.add(createTransaction(monthlyIncome / 2, TransactionType.CREDIT, daysAgo = 15))

        // Add spending transactions (spread over the month)
        val spendingPerWeek = monthlySpending / 4
        for (week in 0..3) {
            transactions.add(createTransaction(spendingPerWeek, TransactionType.DEBIT, daysAgo = week * 7 + 2))
        }

        return transactions
    }

    // ==================== calculateSavingsSuggestion Tests ====================

    @Test
    fun `calculateSavingsSuggestion with positive net income returns valid suggestions`() {
        // Given: Monthly income of 100,000 and spending of 60,000
        val transactions = createBalancedTransactions(
            monthlyIncome = 100000.0,
            monthlySpending = 60000.0
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Net income is 40,000
        val expectedNetIncome = 40000.0
        assertEquals(expectedNetIncome * 0.20, suggestion.conservative, 0.01)
        assertEquals(expectedNetIncome * 0.35, suggestion.moderate, 0.01)
        assertEquals(expectedNetIncome * 0.50, suggestion.aggressive, 0.01)
        assertEquals(TimePeriod.MONTH, suggestion.period)
        assertTrue(suggestion.analysis.isNotEmpty())
    }

    @Test
    fun `calculateSavingsSuggestion with spending exceeding income suggests zero savings`() {
        // Given: Spending exceeds income
        val transactions = createBalancedTransactions(
            monthlyIncome = 50000.0,
            monthlySpending = 70000.0
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Should suggest zero savings
        assertEquals(0.0, suggestion.conservative, 0.01)
        assertEquals(0.0, suggestion.moderate, 0.01)
        assertEquals(0.0, suggestion.aggressive, 0.01)
        assertEquals(SavingsLevel.CONSERVATIVE, suggestion.recommendedLevel)
        assertTrue(suggestion.analysis.contains("spending exceeds income", ignoreCase = true))
    }

    @Test
    fun `calculateSavingsSuggestion with high savings rate recommends aggressive`() {
        // Given: High savings rate (50%)
        val transactions = createBalancedTransactions(
            monthlyIncome = 100000.0,
            monthlySpending = 40000.0 // 60% net income
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Should recommend aggressive savings
        assertEquals(SavingsLevel.AGGRESSIVE, suggestion.recommendedLevel)
        assertTrue(suggestion.analysis.contains("aggressive", ignoreCase = true))
    }

    @Test
    fun `calculateSavingsSuggestion with moderate savings rate recommends moderate`() {
        // Given: Moderate savings rate (30%)
        val transactions = createBalancedTransactions(
            monthlyIncome = 100000.0,
            monthlySpending = 70000.0 // 30% net income
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Should recommend moderate savings
        assertEquals(SavingsLevel.MODERATE, suggestion.recommendedLevel)
        assertTrue(suggestion.analysis.contains("moderate", ignoreCase = true))
    }

    @Test
    fun `calculateSavingsSuggestion with low savings rate recommends conservative`() {
        // Given: Low savings rate (15%)
        val transactions = createBalancedTransactions(
            monthlyIncome = 100000.0,
            monthlySpending = 85000.0 // 15% net income
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Should recommend conservative savings
        assertEquals(SavingsLevel.CONSERVATIVE, suggestion.recommendedLevel)
        assertTrue(suggestion.analysis.contains("conservative", ignoreCase = true))
    }

    @Test
    fun `calculateSavingsSuggestion with high spending variance recommends conservative`() {
        // Given: High variance in daily spending
        val transactions = mutableListOf<ParsedTransaction>()
        transactions.add(createTransaction(50000.0, TransactionType.CREDIT, daysAgo = 15))

        // Add highly variable spending (some days high, some days low)
        transactions.add(createTransaction(20000.0, TransactionType.DEBIT, daysAgo = 1))
        transactions.add(createTransaction(500.0, TransactionType.DEBIT, daysAgo = 5))
        transactions.add(createTransaction(15000.0, TransactionType.DEBIT, daysAgo = 10))
        transactions.add(createTransaction(100.0, TransactionType.DEBIT, daysAgo = 20))

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Should recommend conservative due to high variance
        assertEquals(SavingsLevel.CONSERVATIVE, suggestion.recommendedLevel)
    }

    @Test
    fun `calculateSavingsSuggestion with empty transaction list returns zero suggestions`() {
        // Given: Empty transaction list
        val transactions = emptyList<ParsedTransaction>()

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Should return zero suggestions
        assertEquals(0.0, suggestion.conservative, 0.01)
        assertEquals(0.0, suggestion.moderate, 0.01)
        assertEquals(0.0, suggestion.aggressive, 0.01)
    }

    @Test
    fun `calculateSavingsSuggestion with only income transactions`() {
        // Given: Only income, no spending
        val transactions = listOf(
            createTransaction(50000.0, TransactionType.CREDIT, daysAgo = 5),
            createTransaction(30000.0, TransactionType.CREDIT, daysAgo = 15)
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Net income equals total income (80,000)
        val expectedNetIncome = 80000.0
        assertEquals(expectedNetIncome * 0.20, suggestion.conservative, 0.01)
        assertEquals(expectedNetIncome * 0.35, suggestion.moderate, 0.01)
        assertEquals(expectedNetIncome * 0.50, suggestion.aggressive, 0.01)
        assertEquals(SavingsLevel.AGGRESSIVE, suggestion.recommendedLevel)
    }

    @Test
    fun `calculateSavingsSuggestion with only debit transactions`() {
        // Given: Only spending, no income
        val transactions = listOf(
            createTransaction(20000.0, TransactionType.DEBIT, daysAgo = 5),
            createTransaction(15000.0, TransactionType.DEBIT, daysAgo = 15)
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Should suggest zero savings (negative net income)
        assertEquals(0.0, suggestion.conservative, 0.01)
        assertEquals(0.0, suggestion.moderate, 0.01)
        assertEquals(0.0, suggestion.aggressive, 0.01)
    }

    @Test
    fun `calculateSavingsSuggestion works with WEEK period`() {
        // Given: Weekly transactions
        val transactions = listOf(
            createTransaction(25000.0, TransactionType.CREDIT, daysAgo = 2),
            createTransaction(15000.0, TransactionType.DEBIT, daysAgo = 3)
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.WEEK)

        // Then: Should calculate based on week period
        assertEquals(TimePeriod.WEEK, suggestion.period)
        val expectedNetIncome = 10000.0
        assertEquals(expectedNetIncome * 0.20, suggestion.conservative, 0.01)
    }

    @Test
    fun `calculateSavingsSuggestion works with DAY period`() {
        // Given: Daily transactions
        val transactions = listOf(
            createTransaction(5000.0, TransactionType.CREDIT, daysAgo = 0),
            createTransaction(3000.0, TransactionType.DEBIT, daysAgo = 0)
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.DAY)

        // Then: Should calculate based on day period
        assertEquals(TimePeriod.DAY, suggestion.period)
        val expectedNetIncome = 2000.0
        assertEquals(expectedNetIncome * 0.20, suggestion.conservative, 0.01)
    }

    @Test
    fun `calculateSavingsSuggestion filters transactions by period correctly`() {
        // Given: Transactions both within and outside the period
        val transactions = listOf(
            // Within month period
            createTransaction(50000.0, TransactionType.CREDIT, daysAgo = 10),
            createTransaction(30000.0, TransactionType.DEBIT, daysAgo = 15),
            // Outside month period (should be filtered out)
            createTransaction(100000.0, TransactionType.CREDIT, daysAgo = 50),
            createTransaction(80000.0, TransactionType.DEBIT, daysAgo = 60)
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Should only consider transactions within month (net income = 20,000)
        val expectedNetIncome = 20000.0
        assertEquals(expectedNetIncome * 0.20, suggestion.conservative, 0.01)
    }

    @Test
    fun `calculateSavingsSuggestion with zero income returns zero suggestions`() {
        // Given: Zero income
        val transactions = listOf(
            createTransaction(5000.0, TransactionType.DEBIT, daysAgo = 5)
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Should return zero suggestions
        assertEquals(0.0, suggestion.conservative, 0.01)
        assertEquals(0.0, suggestion.moderate, 0.01)
        assertEquals(0.0, suggestion.aggressive, 0.01)
    }

    @Test
    fun `calculateSavingsSuggestion percentage calculations are correct`() {
        // Given: Known net income
        val netIncome = 10000.0
        val transactions = createBalancedTransactions(
            monthlyIncome = 25000.0,
            monthlySpending = 15000.0
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Verify percentage calculations
        assertEquals(netIncome * 0.20, suggestion.conservative, 0.01) // 2,000
        assertEquals(netIncome * 0.35, suggestion.moderate, 0.01)     // 3,500
        assertEquals(netIncome * 0.50, suggestion.aggressive, 0.01)   // 5,000
    }

    // ==================== analyzeIncomeExpense Tests ====================

    @Test
    fun `analyzeIncomeExpense returns correct breakdown`() {
        // Given: Balanced transactions
        val transactions = createBalancedTransactions(
            monthlyIncome = 100000.0,
            monthlySpending = 60000.0
        )

        // When
        val analysis = calculator.analyzeIncomeExpense(transactions, TimePeriod.MONTH)

        // Then
        assertEquals(TimePeriod.MONTH, analysis.period)
        assertEquals(100000.0, analysis.totalIncome, 0.01)
        assertEquals(60000.0, analysis.totalExpense, 0.01)
        assertEquals(40000.0, analysis.netIncome, 0.01)
        assertEquals(2, analysis.incomeTransactionCount) // 2 income transactions
        assertEquals(4, analysis.expenseTransactionCount) // 4 expense transactions
        assertEquals(100000.0 / 30, analysis.averageDailyIncome, 0.01)
        assertEquals(60000.0 / 30, analysis.averageDailyExpense, 0.01)
        assertEquals(40.0, analysis.savingsRate, 0.01) // 40% savings rate
    }

    @Test
    fun `analyzeIncomeExpense with only income transactions`() {
        // Given: Only income
        val transactions = listOf(
            createTransaction(50000.0, TransactionType.CREDIT, daysAgo = 5),
            createTransaction(30000.0, TransactionType.CREDIT, daysAgo = 15)
        )

        // When
        val analysis = calculator.analyzeIncomeExpense(transactions, TimePeriod.MONTH)

        // Then
        assertEquals(80000.0, analysis.totalIncome, 0.01)
        assertEquals(0.0, analysis.totalExpense, 0.01)
        assertEquals(80000.0, analysis.netIncome, 0.01)
        assertEquals(2, analysis.incomeTransactionCount)
        assertEquals(0, analysis.expenseTransactionCount)
        assertEquals(100.0, analysis.savingsRate, 0.01) // 100% savings rate
    }

    @Test
    fun `analyzeIncomeExpense with only expense transactions`() {
        // Given: Only expenses
        val transactions = listOf(
            createTransaction(20000.0, TransactionType.DEBIT, daysAgo = 5),
            createTransaction(15000.0, TransactionType.DEBIT, daysAgo = 15)
        )

        // When
        val analysis = calculator.analyzeIncomeExpense(transactions, TimePeriod.MONTH)

        // Then
        assertEquals(0.0, analysis.totalIncome, 0.01)
        assertEquals(35000.0, analysis.totalExpense, 0.01)
        assertEquals(-35000.0, analysis.netIncome, 0.01)
        assertEquals(0, analysis.incomeTransactionCount)
        assertEquals(2, analysis.expenseTransactionCount)
        assertEquals(0.0, analysis.savingsRate, 0.01) // 0% savings rate (no income)
    }

    @Test
    fun `analyzeIncomeExpense with empty transaction list`() {
        // Given: Empty list
        val transactions = emptyList<ParsedTransaction>()

        // When
        val analysis = calculator.analyzeIncomeExpense(transactions, TimePeriod.MONTH)

        // Then: All values should be zero
        assertEquals(0.0, analysis.totalIncome, 0.01)
        assertEquals(0.0, analysis.totalExpense, 0.01)
        assertEquals(0.0, analysis.netIncome, 0.01)
        assertEquals(0, analysis.incomeTransactionCount)
        assertEquals(0, analysis.expenseTransactionCount)
        assertEquals(0.0, analysis.averageDailyIncome, 0.01)
        assertEquals(0.0, analysis.averageDailyExpense, 0.01)
        assertEquals(0.0, analysis.savingsRate, 0.01)
    }

    @Test
    fun `analyzeIncomeExpense works with different periods`() {
        // Given: Same transactions
        val transactions = createBalancedTransactions(100000.0, 60000.0)

        // When: Analyze for different periods
        val monthAnalysis = calculator.analyzeIncomeExpense(transactions, TimePeriod.MONTH)
        val weekAnalysis = calculator.analyzeIncomeExpense(transactions, TimePeriod.WEEK)

        // Then: Period affects averages
        assertEquals(100000.0 / 30, monthAnalysis.averageDailyIncome, 0.01)
        assertEquals(100000.0 / 7, weekAnalysis.averageDailyIncome, 0.01)
    }

    @Test
    fun `analyzeIncomeExpense calculates savings rate correctly`() {
        // Given: Various scenarios
        val highSavings = createBalancedTransactions(100000.0, 20000.0) // 80% savings
        val lowSavings = createBalancedTransactions(100000.0, 90000.0)  // 10% savings
        val noSavings = createBalancedTransactions(100000.0, 100000.0)  // 0% savings

        // When
        val highAnalysis = calculator.analyzeIncomeExpense(highSavings, TimePeriod.MONTH)
        val lowAnalysis = calculator.analyzeIncomeExpense(lowSavings, TimePeriod.MONTH)
        val noAnalysis = calculator.analyzeIncomeExpense(noSavings, TimePeriod.MONTH)

        // Then
        assertEquals(80.0, highAnalysis.savingsRate, 0.01)
        assertEquals(10.0, lowAnalysis.savingsRate, 0.01)
        assertEquals(0.0, noAnalysis.savingsRate, 0.01)
    }

    @Test
    fun `analyzeIncomeExpense filters old transactions`() {
        // Given: Transactions outside the period
        val transactions = listOf(
            createTransaction(50000.0, TransactionType.CREDIT, daysAgo = 5), // Within week
            createTransaction(30000.0, TransactionType.DEBIT, daysAgo = 3),  // Within week
            createTransaction(100000.0, TransactionType.CREDIT, daysAgo = 20), // Outside week
            createTransaction(80000.0, TransactionType.DEBIT, daysAgo = 25)   // Outside week
        )

        // When: Analyze for week period
        val analysis = calculator.analyzeIncomeExpense(transactions, TimePeriod.WEEK)

        // Then: Should only include transactions within week
        assertEquals(50000.0, analysis.totalIncome, 0.01)
        assertEquals(30000.0, analysis.totalExpense, 0.01)
        assertEquals(1, analysis.incomeTransactionCount)
        assertEquals(1, analysis.expenseTransactionCount)
    }

    // ==================== calculateMonthlyRecommendation Tests ====================

    @Test
    fun `calculateMonthlyRecommendation with basic inputs`() {
        // Given: Goal parameters
        val targetAmount = 120000.0
        val now = System.currentTimeMillis()
        val deadline = now + (6L * 30 * 24 * 60 * 60 * 1000) // 6 months from now
        val currentSavings = 0.0

        // When
        val recommendation = calculator.calculateMonthlyRecommendation(
            targetAmount = targetAmount,
            deadline = deadline,
            currentSavings = currentSavings
        )

        // Then: Should recommend 20,000 per month (120,000 / 6)
        assertEquals(20000.0, recommendation.requiredMonthly, 100.0) // Allow small variance due to time
        assertEquals(6, recommendation.monthsRemaining)
        assertTrue(recommendation.isAffordable) // No transactions provided, defaults to affordable
        assertEquals(targetAmount, recommendation.targetAmount, 0.01)
        assertEquals(currentSavings, recommendation.currentSavings, 0.01)
    }

    @Test
    fun `calculateMonthlyRecommendation with current savings`() {
        // Given: Already saved some amount
        val targetAmount = 100000.0
        val now = System.currentTimeMillis()
        val deadline = now + (5L * 30 * 24 * 60 * 60 * 1000) // 5 months
        val currentSavings = 40000.0

        // When
        val recommendation = calculator.calculateMonthlyRecommendation(
            targetAmount = targetAmount,
            deadline = deadline,
            currentSavings = currentSavings
        )

        // Then: Should recommend 12,000 per month (60,000 / 5)
        assertEquals(12000.0, recommendation.requiredMonthly, 100.0)
        assertEquals(5, recommendation.monthsRemaining)
    }

    @Test
    fun `calculateMonthlyRecommendation when goal already achieved`() {
        // Given: Current savings exceed target
        val targetAmount = 50000.0
        val now = System.currentTimeMillis()
        val deadline = now + (3L * 30 * 24 * 60 * 60 * 1000)
        val currentSavings = 60000.0

        // When
        val recommendation = calculator.calculateMonthlyRecommendation(
            targetAmount = targetAmount,
            deadline = deadline,
            currentSavings = currentSavings
        )

        // Then: Should require 0 monthly savings
        assertEquals(0.0, recommendation.requiredMonthly, 0.01)
    }

    @Test
    fun `calculateMonthlyRecommendation with affordability check - affordable`() {
        // Given: Goal is affordable based on transactions
        val transactions = createBalancedTransactions(100000.0, 60000.0) // Net: 40,000/month
        val targetAmount = 100000.0
        val now = System.currentTimeMillis()
        val deadline = now + (5L * 30 * 24 * 60 * 60 * 1000) // 5 months

        // When
        val recommendation = calculator.calculateMonthlyRecommendation(
            targetAmount = targetAmount,
            deadline = deadline,
            currentSavings = 0.0,
            transactions = transactions
        )

        // Then: Required is 20,000/month, net income is 40,000/month - affordable
        assertTrue(recommendation.isAffordable)
    }

    @Test
    fun `calculateMonthlyRecommendation with affordability check - not affordable`() {
        // Given: Goal is not affordable based on transactions
        val transactions = createBalancedTransactions(100000.0, 90000.0) // Net: 10,000/month
        val targetAmount = 120000.0
        val now = System.currentTimeMillis()
        val deadline = now + (3L * 30 * 24 * 60 * 60 * 1000) // 3 months

        // When
        val recommendation = calculator.calculateMonthlyRecommendation(
            targetAmount = targetAmount,
            deadline = deadline,
            currentSavings = 0.0,
            transactions = transactions
        )

        // Then: Required is 40,000/month, net income is 10,000/month - not affordable
        assertFalse(recommendation.isAffordable)
    }

    @Test
    fun `calculateMonthlyRecommendation with very short deadline`() {
        // Given: Deadline is very soon (less than a month)
        val targetAmount = 50000.0
        val now = System.currentTimeMillis()
        val deadline = now + (15L * 24 * 60 * 60 * 1000) // 15 days

        // When
        val recommendation = calculator.calculateMonthlyRecommendation(
            targetAmount = targetAmount,
            deadline = deadline,
            currentSavings = 0.0
        )

        // Then: Should still calculate (minimum 1 month)
        assertEquals(1, recommendation.monthsRemaining)
        assertEquals(50000.0, recommendation.requiredMonthly, 0.01)
    }

    @Test
    fun `calculateMonthlyRecommendation with long deadline`() {
        // Given: Long deadline (2 years)
        val targetAmount = 240000.0
        val now = System.currentTimeMillis()
        val deadline = now + (24L * 30 * 24 * 60 * 60 * 1000) // 24 months

        // When
        val recommendation = calculator.calculateMonthlyRecommendation(
            targetAmount = targetAmount,
            deadline = deadline,
            currentSavings = 0.0
        )

        // Then: Should spread over 24 months
        assertEquals(24, recommendation.monthsRemaining)
        assertEquals(10000.0, recommendation.requiredMonthly, 100.0)
    }

    @Test
    fun `calculateMonthlyRecommendation with past deadline`() {
        // Given: Deadline in the past
        val targetAmount = 60000.0
        val now = System.currentTimeMillis()
        val deadline = now - (10L * 24 * 60 * 60 * 1000) // 10 days ago

        // When
        val recommendation = calculator.calculateMonthlyRecommendation(
            targetAmount = targetAmount,
            deadline = deadline,
            currentSavings = 0.0
        )

        // Then: Should still calculate (minimum 1 month)
        assertEquals(1, recommendation.monthsRemaining)
        assertEquals(60000.0, recommendation.requiredMonthly, 0.01)
    }

    @Test
    fun `calculateMonthlyRecommendation without transactions defaults to affordable`() {
        // Given: No transactions provided
        val targetAmount = 100000.0
        val now = System.currentTimeMillis()
        val deadline = now + (5L * 30 * 24 * 60 * 60 * 1000)

        // When
        val recommendation = calculator.calculateMonthlyRecommendation(
            targetAmount = targetAmount,
            deadline = deadline,
            currentSavings = 0.0,
            transactions = emptyList()
        )

        // Then: Should default to affordable
        assertTrue(recommendation.isAffordable)
    }

    @Test
    fun `calculateMonthlyRecommendation with exact match savings`() {
        // Given: Current savings exactly match target
        val targetAmount = 75000.0
        val now = System.currentTimeMillis()
        val deadline = now + (3L * 30 * 24 * 60 * 60 * 1000)

        // When
        val recommendation = calculator.calculateMonthlyRecommendation(
            targetAmount = targetAmount,
            deadline = deadline,
            currentSavings = 75000.0
        )

        // Then: Should require 0 monthly savings
        assertEquals(0.0, recommendation.requiredMonthly, 0.01)
    }

    // ==================== Edge Cases and Boundary Tests ====================

    @Test
    fun `savings suggestion handles very small amounts`() {
        // Given: Very small transaction amounts
        val transactions = listOf(
            createTransaction(10.0, TransactionType.CREDIT, daysAgo = 5),
            createTransaction(5.0, TransactionType.DEBIT, daysAgo = 3)
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Should still calculate correctly
        assertEquals(5.0 * 0.20, suggestion.conservative, 0.01)
        assertEquals(5.0 * 0.35, suggestion.moderate, 0.01)
        assertEquals(5.0 * 0.50, suggestion.aggressive, 0.01)
    }

    @Test
    fun `savings suggestion handles very large amounts`() {
        // Given: Very large transaction amounts
        val transactions = listOf(
            createTransaction(10000000.0, TransactionType.CREDIT, daysAgo = 5),
            createTransaction(3000000.0, TransactionType.DEBIT, daysAgo = 3)
        )

        // When
        val suggestion = calculator.calculateSavingsSuggestion(transactions, TimePeriod.MONTH)

        // Then: Should still calculate correctly
        val netIncome = 7000000.0
        assertEquals(netIncome * 0.20, suggestion.conservative, 0.01)
        assertEquals(netIncome * 0.35, suggestion.moderate, 0.01)
        assertEquals(netIncome * 0.50, suggestion.aggressive, 0.01)
    }

    @Test
    fun `analyzeIncomeExpense handles mixed transaction types`() {
        // Given: Multiple mixed transactions
        val transactions = listOf(
            createTransaction(50000.0, TransactionType.CREDIT, daysAgo = 1),
            createTransaction(10000.0, TransactionType.DEBIT, daysAgo = 2),
            createTransaction(30000.0, TransactionType.CREDIT, daysAgo = 5),
            createTransaction(15000.0, TransactionType.DEBIT, daysAgo = 7),
            createTransaction(5000.0, TransactionType.DEBIT, daysAgo = 10)
        )

        // When
        val analysis = calculator.analyzeIncomeExpense(transactions, TimePeriod.MONTH)

        // Then
        assertEquals(80000.0, analysis.totalIncome, 0.01) // 50k + 30k
        assertEquals(30000.0, analysis.totalExpense, 0.01) // 10k + 15k + 5k
        assertEquals(50000.0, analysis.netIncome, 0.01)
        assertEquals(2, analysis.incomeTransactionCount)
        assertEquals(3, analysis.expenseTransactionCount)
    }

    @Test
    fun `calculateMonthlyRecommendation handles negative net income`() {
        // Given: Spending exceeds income
        val transactions = createBalancedTransactions(50000.0, 80000.0) // Net: -30,000/month
        val targetAmount = 100000.0
        val now = System.currentTimeMillis()
        val deadline = now + (5L * 30 * 24 * 60 * 60 * 1000)

        // When
        val recommendation = calculator.calculateMonthlyRecommendation(
            targetAmount = targetAmount,
            deadline = deadline,
            currentSavings = 0.0,
            transactions = transactions
        )

        // Then: Should not be affordable
        assertFalse(recommendation.isAffordable)
    }
}
