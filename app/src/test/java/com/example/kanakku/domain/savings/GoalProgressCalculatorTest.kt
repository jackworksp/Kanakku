package com.example.kanakku.domain.savings

import com.example.kanakku.data.model.GoalContribution
import com.example.kanakku.data.model.SavingsGoal
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GoalProgressCalculator
 *
 * Tests cover:
 * - Progress metrics (percentage, days remaining, required savings)
 * - Deadline calculations (days remaining, overdue scenarios)
 * - Required savings calculations (daily/weekly/monthly)
 * - Aggregate statistics across multiple goals
 * - Milestone projections
 * - Contribution trend analysis
 */
class GoalProgressCalculatorTest {

    private lateinit var calculator: GoalProgressCalculator

    @Before
    fun setup() {
        calculator = GoalProgressCalculator()
    }

    // ==================== Helper Functions ====================

    /**
     * Create a test savings goal
     */
    private fun createGoal(
        id: Long = 1L,
        name: String = "Test Goal",
        targetAmount: Double = 100000.0,
        currentAmount: Double = 0.0,
        deadline: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // 30 days from now
        createdAt: Long = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000), // 10 days ago
        isCompleted: Boolean = false,
        completedAt: Long? = null,
        icon: String? = "💰",
        color: String? = "#4CAF50"
    ): SavingsGoal {
        return SavingsGoal(
            id = id,
            name = name,
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            deadline = deadline,
            createdAt = createdAt,
            isCompleted = isCompleted,
            completedAt = completedAt,
            icon = icon,
            color = color
        )
    }

    /**
     * Create a test contribution
     */
    private fun createContribution(
        id: Long = 1L,
        goalId: Long = 1L,
        amount: Double = 5000.0,
        daysAgo: Int = 0,
        note: String? = "Test contribution"
    ): GoalContribution {
        val date = System.currentTimeMillis() - (daysAgo * 24 * 60 * 60 * 1000L)
        return GoalContribution(
            id = id,
            goalId = goalId,
            amount = amount,
            date = date,
            note = note
        )
    }

    // ==================== calculateProgressMetrics Tests ====================

    @Test
    fun `calculateProgressMetrics with basic goal returns correct metrics`() {
        // Given: Goal with 50% progress (50,000 / 100,000)
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 50000.0,
            deadline = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // 30 days
            createdAt = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)  // 30 days ago
        )
        val contributions = listOf(
            createContribution(amount = 50000.0, daysAgo = 5)
        )

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then
        assertEquals(50.0, metrics.percentageComplete, 0.01)
        assertEquals(50000.0, metrics.remainingAmount, 0.01)
        assertEquals(1, metrics.contributionCount)
        assertFalse(metrics.isOnTrack) // 50% progress in 50% time, but slightly behind
    }

    @Test
    fun `calculateProgressMetrics with completed goal returns zero required savings`() {
        // Given: Completed goal
        val now = System.currentTimeMillis()
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 100000.0,
            isCompleted = true,
            completedAt = now
        )
        val contributions = emptyList<GoalContribution>()

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then
        assertEquals(100.0, metrics.percentageComplete, 0.01)
        assertEquals(0L, metrics.daysRemaining)
        assertEquals(0.0, metrics.requiredDailySavings, 0.01)
        assertEquals(0.0, metrics.requiredWeeklySavings, 0.01)
        assertEquals(0.0, metrics.requiredMonthlySavings, 0.01)
        assertTrue(metrics.isOnTrack)
        assertEquals(0.0, metrics.remainingAmount, 0.01)
    }

    @Test
    fun `calculateProgressMetrics with no contributions uses fallback progress rate`() {
        // Given: Goal with progress but no contributions tracked
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 30000.0,
            deadline = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
            createdAt = System.currentTimeMillis() - (15L * 24 * 60 * 60 * 1000) // 15 days ago
        )
        val contributions = emptyList<GoalContribution>()

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then: Progress rate = 30,000 / 15 = 2,000 per day
        assertEquals(30.0, metrics.percentageComplete, 0.01)
        assertEquals(70000.0, metrics.remainingAmount, 0.01)
        assertEquals(2000.0, metrics.progressRate, 100.0) // Allow variance
        assertEquals(0, metrics.contributionCount)
    }

    @Test
    fun `calculateProgressMetrics calculates required daily savings correctly`() {
        // Given: Goal with 60,000 remaining and 30 days left
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 40000.0,
            deadline = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        )
        val contributions = emptyList<GoalContribution>()

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then: Required daily = 60,000 / 30 = 2,000
        assertEquals(2000.0, metrics.requiredDailySavings, 50.0) // Allow small variance for time
        assertEquals(2000.0 * 7, metrics.requiredWeeklySavings, 350.0)
        assertEquals(2000.0 * 30, metrics.requiredMonthlySavings, 1500.0)
    }

    @Test
    fun `calculateProgressMetrics with zero target amount returns zero percentage`() {
        // Given: Goal with zero target (edge case)
        val goal = createGoal(
            targetAmount = 0.0,
            currentAmount = 5000.0
        )
        val contributions = emptyList<GoalContribution>()

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then
        assertEquals(0.0, metrics.percentageComplete, 0.01)
    }

    @Test
    fun `calculateProgressMetrics with multiple contributions calculates rate from history`() {
        // Given: Goal with multiple contributions over time
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 30000.0,
            deadline = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        )
        val contributions = listOf(
            createContribution(id = 1, amount = 10000.0, daysAgo = 20),
            createContribution(id = 2, amount = 10000.0, daysAgo = 10),
            createContribution(id = 3, amount = 10000.0, daysAgo = 5)
        )

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then: Progress rate = 30,000 / (20 - 5) = 2,000 per day
        assertEquals(30.0, metrics.percentageComplete, 0.01)
        assertEquals(3, metrics.contributionCount)
        assertEquals(2000.0, metrics.progressRate, 100.0) // Allow variance
    }

    @Test
    fun `calculateProgressMetrics with goal very close to deadline`() {
        // Given: Goal with only 2 days remaining
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 80000.0,
            deadline = System.currentTimeMillis() + (2L * 24 * 60 * 60 * 1000),
            createdAt = System.currentTimeMillis() - (28L * 24 * 60 * 60 * 1000)
        )
        val contributions = emptyList<GoalContribution>()

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then: Required daily = 20,000 / 2 = 10,000
        assertEquals(80.0, metrics.percentageComplete, 0.01)
        assertTrue(metrics.daysRemaining <= 2L)
        assertTrue(metrics.requiredDailySavings >= 9000.0) // High daily requirement
    }

    @Test
    fun `calculateProgressMetrics with overdue goal returns zero days remaining`() {
        // Given: Goal past its deadline
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 60000.0,
            deadline = System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000), // 5 days ago
            createdAt = System.currentTimeMillis() - (35L * 24 * 60 * 60 * 1000)
        )
        val contributions = emptyList<GoalContribution>()

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then
        assertEquals(60.0, metrics.percentageComplete, 0.01)
        assertEquals(0L, metrics.daysRemaining) // Coerced to 0
        assertEquals(0.0, metrics.requiredDailySavings, 0.01) // No days remaining
        assertFalse(metrics.isOnTrack) // Past deadline
    }

    @Test
    fun `calculateProgressMetrics determines on-track status correctly`() {
        // Given: Two goals - one on-track, one behind
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
        val thirtyDaysFromNow = now + (30L * 24 * 60 * 60 * 1000)

        // Goal 1: 50% progress in 50% time (on track)
        val onTrackGoal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 50000.0,
            createdAt = thirtyDaysAgo,
            deadline = thirtyDaysFromNow
        )

        // Goal 2: 20% progress in 50% time (behind)
        val behindGoal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 20000.0,
            createdAt = thirtyDaysAgo,
            deadline = thirtyDaysFromNow
        )

        // When
        val onTrackMetrics = calculator.calculateProgressMetrics(onTrackGoal, emptyList())
        val behindMetrics = calculator.calculateProgressMetrics(behindGoal, emptyList())

        // Then
        assertTrue(onTrackMetrics.isOnTrack)
        assertFalse(behindMetrics.isOnTrack)
    }

    @Test
    fun `calculateProgressMetrics calculates projected completion date`() {
        // Given: Goal with steady progress
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 40000.0,
            createdAt = System.currentTimeMillis() - (20L * 24 * 60 * 60 * 1000)
        )
        val contributions = listOf(
            createContribution(amount = 40000.0, daysAgo = 10)
        )

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then: Should have a projected completion date
        assertNotNull(metrics.projectedCompletionDate)
    }

    @Test
    fun `calculateProgressMetrics with zero progress rate returns null projected date`() {
        // Given: Goal with no progress
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 0.0,
            createdAt = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000)
        )
        val contributions = emptyList<GoalContribution>()

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then
        assertNull(metrics.projectedCompletionDate)
    }

    @Test
    fun `calculateProgressMetrics with amount exceeding target shows 100 percent`() {
        // Given: Goal with current amount exceeding target
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 120000.0
        )
        val contributions = emptyList<GoalContribution>()

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then: Percentage should be clamped at 100
        assertEquals(100.0, metrics.percentageComplete, 0.01)
        assertEquals(0.0, metrics.remainingAmount, 0.01)
    }

    // ==================== calculateAggregateStats Tests ====================

    @Test
    fun `calculateAggregateStats with empty list returns zero stats`() {
        // Given: Empty goals list
        val goals = emptyList<SavingsGoal>()

        // When
        val stats = calculator.calculateAggregateStats(goals)

        // Then
        assertEquals(0, stats.totalGoals)
        assertEquals(0, stats.activeGoals)
        assertEquals(0, stats.completedGoals)
        assertEquals(0, stats.overdueGoals)
        assertEquals(0.0, stats.totalTargetAmount, 0.01)
        assertEquals(0.0, stats.totalCurrentAmount, 0.01)
        assertEquals(0.0, stats.totalRemainingAmount, 0.01)
        assertEquals(0.0, stats.overallProgressPercentage, 0.01)
        assertEquals(0.0, stats.averageProgressPercentage, 0.01)
    }

    @Test
    fun `calculateAggregateStats with single goal returns correct stats`() {
        // Given: Single active goal
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 30000.0,
            deadline = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        )
        val goals = listOf(goal)

        // When
        val stats = calculator.calculateAggregateStats(goals)

        // Then
        assertEquals(1, stats.totalGoals)
        assertEquals(1, stats.activeGoals)
        assertEquals(0, stats.completedGoals)
        assertEquals(0, stats.overdueGoals)
        assertEquals(100000.0, stats.totalTargetAmount, 0.01)
        assertEquals(30000.0, stats.totalCurrentAmount, 0.01)
        assertEquals(70000.0, stats.totalRemainingAmount, 0.01)
        assertEquals(30.0, stats.overallProgressPercentage, 0.01)
        assertEquals(30.0, stats.averageProgressPercentage, 0.01)
    }

    @Test
    fun `calculateAggregateStats with multiple goals calculates correctly`() {
        // Given: Mix of active, completed, and overdue goals
        val now = System.currentTimeMillis()
        val goals = listOf(
            // Active goal
            createGoal(
                id = 1,
                targetAmount = 100000.0,
                currentAmount = 50000.0,
                deadline = now + (30L * 24 * 60 * 60 * 1000)
            ),
            // Completed goal
            createGoal(
                id = 2,
                targetAmount = 50000.0,
                currentAmount = 50000.0,
                deadline = now + (10L * 24 * 60 * 60 * 1000),
                isCompleted = true,
                completedAt = now - (5L * 24 * 60 * 60 * 1000)
            ),
            // Overdue goal
            createGoal(
                id = 3,
                targetAmount = 80000.0,
                currentAmount = 20000.0,
                deadline = now - (5L * 24 * 60 * 60 * 1000)
            )
        )

        // When
        val stats = calculator.calculateAggregateStats(goals)

        // Then
        assertEquals(3, stats.totalGoals)
        assertEquals(1, stats.activeGoals)
        assertEquals(1, stats.completedGoals)
        assertEquals(1, stats.overdueGoals)
        assertEquals(230000.0, stats.totalTargetAmount, 0.01) // 100k + 50k + 80k
        assertEquals(120000.0, stats.totalCurrentAmount, 0.01) // 50k + 50k + 20k
        assertEquals(110000.0, stats.totalRemainingAmount, 0.01) // 50k + 0 + 60k
        assertEquals(52.17, stats.overallProgressPercentage, 0.5) // (120k / 230k) * 100
    }

    @Test
    fun `calculateAggregateStats calculates weighted vs simple average correctly`() {
        // Given: Two goals with different target amounts but same percentage
        val goals = listOf(
            createGoal(id = 1, targetAmount = 100000.0, currentAmount = 50000.0), // 50%
            createGoal(id = 2, targetAmount = 200000.0, currentAmount = 100000.0)  // 50%
        )

        // When
        val stats = calculator.calculateAggregateStats(goals)

        // Then
        // Overall (weighted): (150k / 300k) * 100 = 50%
        // Average (simple): (50% + 50%) / 2 = 50%
        assertEquals(50.0, stats.overallProgressPercentage, 0.01)
        assertEquals(50.0, stats.averageProgressPercentage, 0.01)
    }

    @Test
    fun `calculateAggregateStats with different progress percentages`() {
        // Given: Goals with different progress levels
        val goals = listOf(
            createGoal(id = 1, targetAmount = 100000.0, currentAmount = 80000.0), // 80%
            createGoal(id = 2, targetAmount = 100000.0, currentAmount = 20000.0)  // 20%
        )

        // When
        val stats = calculator.calculateAggregateStats(goals)

        // Then
        // Overall (weighted): (100k / 200k) * 100 = 50%
        // Average (simple): (80% + 20%) / 2 = 50%
        assertEquals(50.0, stats.overallProgressPercentage, 0.01)
        assertEquals(50.0, stats.averageProgressPercentage, 0.01)
    }

    @Test
    fun `calculateAggregateStats with zero target amount handles gracefully`() {
        // Given: Goal with zero target
        val goals = listOf(
            createGoal(targetAmount = 0.0, currentAmount = 5000.0)
        )

        // When
        val stats = calculator.calculateAggregateStats(goals)

        // Then: Should handle without error
        assertEquals(1, stats.totalGoals)
        assertEquals(0.0, stats.totalTargetAmount, 0.01)
    }

    @Test
    fun `calculateAggregateStats counts goal statuses correctly`() {
        // Given: Multiple goals in different states
        val now = System.currentTimeMillis()
        val goals = listOf(
            createGoal(id = 1, deadline = now + (10L * 24 * 60 * 60 * 1000)), // Active
            createGoal(id = 2, deadline = now + (20L * 24 * 60 * 60 * 1000)), // Active
            createGoal(id = 3, isCompleted = true), // Completed
            createGoal(id = 4, isCompleted = true), // Completed
            createGoal(id = 5, isCompleted = true), // Completed
            createGoal(id = 6, deadline = now - (5L * 24 * 60 * 60 * 1000)), // Overdue
            createGoal(id = 7, deadline = now - (10L * 24 * 60 * 60 * 1000))  // Overdue
        )

        // When
        val stats = calculator.calculateAggregateStats(goals)

        // Then
        assertEquals(7, stats.totalGoals)
        assertEquals(2, stats.activeGoals)
        assertEquals(3, stats.completedGoals)
        assertEquals(2, stats.overdueGoals)
    }

    // ==================== calculateMilestoneProjection Tests ====================

    @Test
    fun `calculateMilestoneProjection when milestone already reached`() {
        // Given: Current amount exceeds milestone
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 60000.0
        )
        val milestoneAmount = 50000.0
        val contributions = emptyList<GoalContribution>()

        // When
        val projection = calculator.calculateMilestoneProjection(goal, milestoneAmount, contributions)

        // Then
        assertTrue(projection.isReached)
        assertEquals(50000.0, projection.milestoneAmount, 0.01)
        assertEquals(0, projection.daysToMilestone)
        assertNotNull(projection.estimatedDate)
    }

    @Test
    fun `calculateMilestoneProjection with positive progress rate`() {
        // Given: Goal with steady progress (2000 per day)
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 40000.0,
            createdAt = System.currentTimeMillis() - (20L * 24 * 60 * 60 * 1000)
        )
        val milestoneAmount = 60000.0
        val contributions = listOf(
            createContribution(amount = 20000.0, daysAgo = 15),
            createContribution(amount = 20000.0, daysAgo = 5)
        )

        // When
        val projection = calculator.calculateMilestoneProjection(goal, milestoneAmount, contributions)

        // Then: Needs 20,000 more at 2,000/day = 10 days
        assertFalse(projection.isReached)
        assertEquals(60000.0, projection.milestoneAmount, 0.01)
        assertEquals(10L, projection.daysToMilestone)
        assertNotNull(projection.estimatedDate)
    }

    @Test
    fun `calculateMilestoneProjection with zero progress rate returns null`() {
        // Given: Goal with no progress
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 0.0
        )
        val milestoneAmount = 50000.0
        val contributions = emptyList<GoalContribution>()

        // When
        val projection = calculator.calculateMilestoneProjection(goal, milestoneAmount, contributions)

        // Then
        assertFalse(projection.isReached)
        assertEquals(50000.0, projection.milestoneAmount, 0.01)
        assertNull(projection.estimatedDate)
        assertNull(projection.daysToMilestone)
    }

    @Test
    fun `calculateMilestoneProjection for target amount`() {
        // Given: Goal with progress
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 40000.0,
            createdAt = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000)
        )
        val contributions = listOf(
            createContribution(amount = 40000.0, daysAgo = 5)
        )

        // When
        val projection = calculator.calculateMilestoneProjection(goal, goal.targetAmount, contributions)

        // Then
        assertFalse(projection.isReached)
        assertEquals(100000.0, projection.milestoneAmount, 0.01)
        assertNotNull(projection.estimatedDate)
        assertNotNull(projection.daysToMilestone)
    }

    @Test
    fun `calculateMilestoneProjection with exact current amount as milestone`() {
        // Given: Current amount equals milestone
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 50000.0
        )
        val milestoneAmount = 50000.0
        val contributions = emptyList<GoalContribution>()

        // When
        val projection = calculator.calculateMilestoneProjection(goal, milestoneAmount, contributions)

        // Then
        assertTrue(projection.isReached)
        assertEquals(0, projection.daysToMilestone)
    }

    // ==================== calculateContributionTrend Tests ====================

    @Test
    fun `calculateContributionTrend with empty list returns zero values`() {
        // Given: Empty contributions
        val contributions = emptyList<GoalContribution>()

        // When
        val trend = calculator.calculateContributionTrend(contributions)

        // Then
        assertEquals(0.0, trend.averageAmount, 0.01)
        assertEquals(0.0, trend.totalAmount, 0.01)
        assertEquals(0, trend.contributionCount)
        assertNull(trend.averageFrequencyDays)
        assertFalse(trend.isIncreasing)
        assertEquals(0.0, trend.recentAverage, 0.01)
    }

    @Test
    fun `calculateContributionTrend with single contribution`() {
        // Given: Single contribution
        val contributions = listOf(
            createContribution(amount = 5000.0, daysAgo = 5)
        )

        // When
        val trend = calculator.calculateContributionTrend(contributions)

        // Then
        assertEquals(5000.0, trend.averageAmount, 0.01)
        assertEquals(5000.0, trend.totalAmount, 0.01)
        assertEquals(1, trend.contributionCount)
        assertNull(trend.averageFrequencyDays) // Need at least 2 for frequency
        assertFalse(trend.isIncreasing) // Recent average equals overall average
        assertEquals(5000.0, trend.recentAverage, 0.01)
    }

    @Test
    fun `calculateContributionTrend with multiple contributions calculates averages`() {
        // Given: Multiple contributions
        val contributions = listOf(
            createContribution(id = 1, amount = 3000.0, daysAgo = 20),
            createContribution(id = 2, amount = 4000.0, daysAgo = 15),
            createContribution(id = 3, amount = 5000.0, daysAgo = 10),
            createContribution(id = 4, amount = 6000.0, daysAgo = 5)
        )

        // When
        val trend = calculator.calculateContributionTrend(contributions)

        // Then
        assertEquals(4500.0, trend.averageAmount, 0.01) // (3k + 4k + 5k + 6k) / 4
        assertEquals(18000.0, trend.totalAmount, 0.01)
        assertEquals(4, trend.contributionCount)
        assertNotNull(trend.averageFrequencyDays)
    }

    @Test
    fun `calculateContributionTrend with increasing trend`() {
        // Given: Contributions increasing over time
        val contributions = listOf(
            createContribution(id = 1, amount = 2000.0, daysAgo = 20),
            createContribution(id = 2, amount = 3000.0, daysAgo = 15),
            createContribution(id = 3, amount = 5000.0, daysAgo = 10),
            createContribution(id = 4, amount = 7000.0, daysAgo = 5)
        )

        // When
        val trend = calculator.calculateContributionTrend(contributions)

        // Then: Recent average (5k + 7k) / 2 = 6k > overall average 4.25k
        assertTrue(trend.isIncreasing)
        assertEquals(6000.0, trend.recentAverage, 0.01) // Last 2 contributions
    }

    @Test
    fun `calculateContributionTrend with decreasing trend`() {
        // Given: Contributions decreasing over time
        val contributions = listOf(
            createContribution(id = 1, amount = 7000.0, daysAgo = 20),
            createContribution(id = 2, amount = 5000.0, daysAgo = 15),
            createContribution(id = 3, amount = 3000.0, daysAgo = 10),
            createContribution(id = 4, amount = 2000.0, daysAgo = 5)
        )

        // When
        val trend = calculator.calculateContributionTrend(contributions)

        // Then: Recent average (3k + 2k) / 2 = 2.5k < overall average 4.25k
        assertFalse(trend.isIncreasing)
        assertEquals(2500.0, trend.recentAverage, 0.01)
    }

    @Test
    fun `calculateContributionTrend calculates frequency correctly`() {
        // Given: Contributions spaced 5 days apart
        val contributions = listOf(
            createContribution(id = 1, amount = 5000.0, daysAgo = 15),
            createContribution(id = 2, amount = 5000.0, daysAgo = 10),
            createContribution(id = 3, amount = 5000.0, daysAgo = 5),
            createContribution(id = 4, amount = 5000.0, daysAgo = 0)
        )

        // When
        val trend = calculator.calculateContributionTrend(contributions)

        // Then: Frequency = 15 days / 3 intervals = 5 days
        assertEquals(5.0, trend.averageFrequencyDays!!, 0.5)
    }

    @Test
    fun `calculateContributionTrend with custom period days`() {
        // Given: Contributions over time
        val contributions = listOf(
            createContribution(id = 1, amount = 3000.0, daysAgo = 20),
            createContribution(id = 2, amount = 4000.0, daysAgo = 10)
        )

        // When: Calculate with 30-day period
        val trend = calculator.calculateContributionTrend(contributions, periodDays = 30)

        // Then: Should still calculate correctly
        assertEquals(3500.0, trend.averageAmount, 0.01)
        assertEquals(7000.0, trend.totalAmount, 0.01)
    }

    @Test
    fun `calculateContributionTrend with two contributions calculates frequency`() {
        // Given: Exactly 2 contributions
        val contributions = listOf(
            createContribution(id = 1, amount = 5000.0, daysAgo = 14),
            createContribution(id = 2, amount = 5000.0, daysAgo = 7)
        )

        // When
        val trend = calculator.calculateContributionTrend(contributions)

        // Then: Frequency = 7 days / 1 interval = 7 days
        assertEquals(7.0, trend.averageFrequencyDays!!, 0.5)
        assertEquals(5000.0, trend.averageAmount, 0.01)
    }

    @Test
    fun `calculateContributionTrend with stable amounts`() {
        // Given: Same amount each time
        val contributions = listOf(
            createContribution(id = 1, amount = 5000.0, daysAgo = 15),
            createContribution(id = 2, amount = 5000.0, daysAgo = 10),
            createContribution(id = 3, amount = 5000.0, daysAgo = 5)
        )

        // When
        val trend = calculator.calculateContributionTrend(contributions)

        // Then: Recent average equals overall average
        assertEquals(5000.0, trend.averageAmount, 0.01)
        assertEquals(5000.0, trend.recentAverage, 0.01)
        assertFalse(trend.isIncreasing) // Equal, not increasing
    }

    // ==================== Edge Cases and Integration Tests ====================

    @Test
    fun `progress metrics with very large amounts`() {
        // Given: Very large goal
        val goal = createGoal(
            targetAmount = 10000000.0, // 10 million
            currentAmount = 5000000.0   // 5 million
        )
        val contributions = emptyList<GoalContribution>()

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then: Should handle large amounts correctly
        assertEquals(50.0, metrics.percentageComplete, 0.01)
        assertEquals(5000000.0, metrics.remainingAmount, 0.01)
    }

    @Test
    fun `progress metrics with very small amounts`() {
        // Given: Very small goal
        val goal = createGoal(
            targetAmount = 100.0,
            currentAmount = 50.0
        )
        val contributions = emptyList<GoalContribution>()

        // When
        val metrics = calculator.calculateProgressMetrics(goal, contributions)

        // Then: Should handle small amounts correctly
        assertEquals(50.0, metrics.percentageComplete, 0.01)
        assertEquals(50.0, metrics.remainingAmount, 0.01)
    }

    @Test
    fun `aggregate stats with all completed goals`() {
        // Given: All goals completed
        val goals = listOf(
            createGoal(id = 1, targetAmount = 50000.0, currentAmount = 50000.0, isCompleted = true),
            createGoal(id = 2, targetAmount = 100000.0, currentAmount = 100000.0, isCompleted = true)
        )

        // When
        val stats = calculator.calculateAggregateStats(goals)

        // Then
        assertEquals(2, stats.totalGoals)
        assertEquals(0, stats.activeGoals)
        assertEquals(2, stats.completedGoals)
        assertEquals(0, stats.overdueGoals)
        assertEquals(100.0, stats.overallProgressPercentage, 0.01)
    }

    @Test
    fun `aggregate stats with all overdue goals`() {
        // Given: All goals overdue
        val now = System.currentTimeMillis()
        val goals = listOf(
            createGoal(id = 1, deadline = now - (5L * 24 * 60 * 60 * 1000)),
            createGoal(id = 2, deadline = now - (10L * 24 * 60 * 60 * 1000))
        )

        // When
        val stats = calculator.calculateAggregateStats(goals)

        // Then
        assertEquals(2, stats.totalGoals)
        assertEquals(0, stats.activeGoals)
        assertEquals(0, stats.completedGoals)
        assertEquals(2, stats.overdueGoals)
    }

    @Test
    fun `milestone projection with multiple milestones`() {
        // Given: Goal with steady progress
        val goal = createGoal(
            targetAmount = 100000.0,
            currentAmount = 20000.0,
            createdAt = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000)
        )
        val contributions = listOf(
            createContribution(amount = 20000.0, daysAgo = 5)
        )

        // When: Check multiple milestones
        val milestone25k = calculator.calculateMilestoneProjection(goal, 25000.0, contributions)
        val milestone50k = calculator.calculateMilestoneProjection(goal, 50000.0, contributions)
        val milestone75k = calculator.calculateMilestoneProjection(goal, 75000.0, contributions)

        // Then: Each should have different projections
        assertFalse(milestone25k.isReached)
        assertFalse(milestone50k.isReached)
        assertFalse(milestone75k.isReached)
        assertTrue(milestone25k.daysToMilestone!! < milestone50k.daysToMilestone!!)
        assertTrue(milestone50k.daysToMilestone!! < milestone75k.daysToMilestone!!)
    }

    @Test
    fun `contribution trend with irregular frequency`() {
        // Given: Contributions at irregular intervals
        val contributions = listOf(
            createContribution(id = 1, amount = 5000.0, daysAgo = 30),
            createContribution(id = 2, amount = 5000.0, daysAgo = 25),
            createContribution(id = 3, amount = 5000.0, daysAgo = 15),
            createContribution(id = 4, amount = 5000.0, daysAgo = 5)
        )

        // When
        val trend = calculator.calculateContributionTrend(contributions)

        // Then: Should calculate average frequency across all intervals
        assertEquals(5000.0, trend.averageAmount, 0.01)
        assertEquals(20000.0, trend.totalAmount, 0.01)
        assertNotNull(trend.averageFrequencyDays)
        // Frequency = 25 days / 3 intervals = 8.33 days average
        assertEquals(8.33, trend.averageFrequencyDays!!, 1.0)
    }
}
