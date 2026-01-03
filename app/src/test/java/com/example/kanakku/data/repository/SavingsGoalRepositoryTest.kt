package com.example.kanakku.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.GoalContribution
import com.example.kanakku.data.model.GoalStatus
import com.example.kanakku.data.model.SavingsGoal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SavingsGoalRepository using in-memory Room database.
 *
 * Tests cover:
 * - Goal CRUD operations
 * - Contribution management
 * - Progress calculation
 * - Statistics
 * - Entity-model mapping
 * - Cascade deletes
 * - Automatic goal completion
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SavingsGoalRepositoryTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var repository: SavingsGoalRepository

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        repository = SavingsGoalRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    // ==================== Helper Functions ====================

    private fun createTestGoal(
        id: Long = 0,
        name: String = "Test Goal",
        targetAmount: Double = 10000.0,
        currentAmount: Double = 0.0,
        deadline: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // 30 days from now
        createdAt: Long = System.currentTimeMillis(),
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

    private fun createTestContribution(
        id: Long = 0,
        goalId: Long,
        amount: Double = 1000.0,
        date: Long = System.currentTimeMillis(),
        note: String? = "Test contribution"
    ): GoalContribution {
        return GoalContribution(
            id = id,
            goalId = goalId,
            amount = amount,
            date = date,
            note = note
        )
    }

    // ==================== Goal CRUD Tests ====================

    @Test
    fun createGoal_insertsGoalSuccessfully() = runTest {
        // Given
        val goal = createTestGoal(name = "Vacation Fund")

        // When
        val goalId = repository.createGoal(goal)

        // Then
        assertTrue(goalId > 0)
        val loaded = repository.getGoal(goalId)
        assertNotNull(loaded)
        assertEquals("Vacation Fund", loaded?.name)
        assertEquals(10000.0, loaded?.targetAmount, 0.01)
    }

    @Test
    fun updateGoal_updatesExistingGoal() = runTest {
        // Given
        val goal = createTestGoal(name = "Original Name")
        val goalId = repository.createGoal(goal)

        // When
        val updatedGoal = goal.copy(id = goalId, name = "Updated Name", targetAmount = 15000.0)
        val success = repository.updateGoal(updatedGoal)

        // Then
        assertTrue(success)
        val loaded = repository.getGoal(goalId)
        assertEquals("Updated Name", loaded?.name)
        assertEquals(15000.0, loaded?.targetAmount, 0.01)
    }

    @Test
    fun updateGoal_returnsFalseForNonExistentGoal() = runTest {
        // Given
        val goal = createTestGoal(id = 999L)

        // When
        val success = repository.updateGoal(goal)

        // Then
        assertFalse(success)
    }

    @Test
    fun deleteGoal_removesGoal() = runTest {
        // Given
        val goal = createTestGoal()
        val goalId = repository.createGoal(goal)

        // When
        val deleted = repository.deleteGoal(goalId)

        // Then
        assertTrue(deleted)
        assertNull(repository.getGoal(goalId))
        assertEquals(0, repository.getGoalCount())
    }

    @Test
    fun deleteGoal_returnsFalseForNonExistentGoal() = runTest {
        // When
        val deleted = repository.deleteGoal(999L)

        // Then
        assertFalse(deleted)
    }

    @Test
    fun getGoal_returnsNullForNonExistentGoal() = runTest {
        // When
        val goal = repository.getGoal(999L)

        // Then
        assertNull(goal)
    }

    @Test
    fun getGoalFlow_emitsGoalUpdates() = runTest {
        // Given
        val goal = createTestGoal(name = "Test Goal")
        val goalId = repository.createGoal(goal)

        // When
        val flow = repository.getGoalFlow(goalId)
        val result = flow.first()

        // Then
        assertNotNull(result)
        assertEquals("Test Goal", result?.name)
    }

    @Test
    fun getAllGoals_returnsAllGoals() = runTest {
        // Given
        repository.createGoal(createTestGoal(name = "Goal 1"))
        repository.createGoal(createTestGoal(name = "Goal 2"))
        repository.createGoal(createTestGoal(name = "Goal 3"))

        // When
        val goals = repository.getAllGoals().first()

        // Then
        assertEquals(3, goals.size)
    }

    @Test
    fun getAllGoalsSnapshot_returnsAllGoals() = runTest {
        // Given
        repository.createGoal(createTestGoal(name = "Goal 1"))
        repository.createGoal(createTestGoal(name = "Goal 2"))

        // When
        val goals = repository.getAllGoalsSnapshot()

        // Then
        assertEquals(2, goals.size)
    }

    @Test
    fun getActiveGoals_returnsOnlyActiveGoals() = runTest {
        // Given
        val activeGoal = createTestGoal(name = "Active", isCompleted = false)
        val completedGoal = createTestGoal(name = "Completed", isCompleted = true, completedAt = System.currentTimeMillis())
        repository.createGoal(activeGoal)
        repository.createGoal(completedGoal)

        // When
        val activeGoals = repository.getActiveGoals().first()

        // Then
        assertEquals(1, activeGoals.size)
        assertEquals("Active", activeGoals[0].name)
        assertFalse(activeGoals[0].isCompleted)
    }

    @Test
    fun getCompletedGoals_returnsOnlyCompletedGoals() = runTest {
        // Given
        val activeGoal = createTestGoal(name = "Active", isCompleted = false)
        val completedGoal = createTestGoal(name = "Completed", isCompleted = true, completedAt = System.currentTimeMillis())
        repository.createGoal(activeGoal)
        repository.createGoal(completedGoal)

        // When
        val completedGoals = repository.getCompletedGoals().first()

        // Then
        assertEquals(1, completedGoals.size)
        assertEquals("Completed", completedGoals[0].name)
        assertTrue(completedGoals[0].isCompleted)
    }

    @Test
    fun goalExists_returnsCorrectValue() = runTest {
        // Given
        val goal = createTestGoal()
        val goalId = repository.createGoal(goal)

        // When/Then
        assertTrue(repository.goalExists(goalId))
        assertFalse(repository.goalExists(999L))
    }

    @Test
    fun deleteAllGoals_removesAllGoals() = runTest {
        // Given
        repository.createGoal(createTestGoal(name = "Goal 1"))
        repository.createGoal(createTestGoal(name = "Goal 2"))

        // When
        val count = repository.deleteAllGoals()

        // Then
        assertEquals(2, count)
        assertEquals(0, repository.getGoalCount())
    }

    // ==================== Contribution Tests ====================

    @Test
    fun addContribution_insertsContributionSuccessfully() = runTest {
        // Given
        val goal = createTestGoal(targetAmount = 10000.0, currentAmount = 0.0)
        val goalId = repository.createGoal(goal)
        val contribution = createTestContribution(goalId = goalId, amount = 1000.0)

        // When
        val contributionId = repository.addContribution(contribution)

        // Then
        assertTrue(contributionId > 0)
        val contributions = repository.getContributionsSnapshot(goalId)
        assertEquals(1, contributions.size)
        assertEquals(1000.0, contributions[0].amount, 0.01)
    }

    @Test
    fun addContribution_updatesGoalProgress() = runTest {
        // Given
        val goal = createTestGoal(targetAmount = 10000.0, currentAmount = 0.0)
        val goalId = repository.createGoal(goal)
        val contribution = createTestContribution(goalId = goalId, amount = 2000.0)

        // When
        repository.addContribution(contribution)

        // Then
        val updatedGoal = repository.getGoal(goalId)
        assertEquals(2000.0, updatedGoal?.currentAmount, 0.01)
    }

    @Test
    fun addContribution_marksGoalAsCompletedWhenTargetReached() = runTest {
        // Given
        val goal = createTestGoal(targetAmount = 10000.0, currentAmount = 8000.0)
        val goalId = repository.createGoal(goal)
        val contribution = createTestContribution(goalId = goalId, amount = 2000.0)

        // When
        repository.addContribution(contribution)

        // Then
        val updatedGoal = repository.getGoal(goalId)
        assertEquals(10000.0, updatedGoal?.currentAmount, 0.01)
        assertTrue(updatedGoal?.isCompleted == true)
        assertNotNull(updatedGoal?.completedAt)
    }

    @Test
    fun addContribution_marksGoalAsCompletedWhenExceedingTarget() = runTest {
        // Given
        val goal = createTestGoal(targetAmount = 10000.0, currentAmount = 9000.0)
        val goalId = repository.createGoal(goal)
        val contribution = createTestContribution(goalId = goalId, amount = 2000.0)

        // When
        repository.addContribution(contribution)

        // Then
        val updatedGoal = repository.getGoal(goalId)
        assertEquals(11000.0, updatedGoal?.currentAmount, 0.01)
        assertTrue(updatedGoal?.isCompleted == true)
        assertNotNull(updatedGoal?.completedAt)
    }

    @Test
    fun getContributions_returnsAllContributionsForGoal() = runTest {
        // Given
        val goalId = repository.createGoal(createTestGoal())
        repository.addContribution(createTestContribution(goalId = goalId, amount = 1000.0))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 2000.0))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 3000.0))

        // When
        val contributions = repository.getContributions(goalId).first()

        // Then
        assertEquals(3, contributions.size)
    }

    @Test
    fun getContributionsSnapshot_returnsAllContributionsForGoal() = runTest {
        // Given
        val goalId = repository.createGoal(createTestGoal())
        repository.addContribution(createTestContribution(goalId = goalId, amount = 1000.0))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 2000.0))

        // When
        val contributions = repository.getContributionsSnapshot(goalId)

        // Then
        assertEquals(2, contributions.size)
    }

    @Test
    fun deleteContribution_removesContribution() = runTest {
        // Given
        val goalId = repository.createGoal(createTestGoal(currentAmount = 0.0))
        val contributionId = repository.addContribution(createTestContribution(goalId = goalId, amount = 1000.0))

        // When
        val deleted = repository.deleteContribution(contributionId)

        // Then
        assertTrue(deleted)
        val contributions = repository.getContributionsSnapshot(goalId)
        assertEquals(0, contributions.size)
    }

    @Test
    fun deleteContribution_updatesGoalProgress() = runTest {
        // Given
        val goalId = repository.createGoal(createTestGoal(currentAmount = 0.0))
        val contributionId = repository.addContribution(createTestContribution(goalId = goalId, amount = 2000.0))

        // When
        repository.deleteContribution(contributionId)

        // Then
        val updatedGoal = repository.getGoal(goalId)
        assertEquals(0.0, updatedGoal?.currentAmount, 0.01)
    }

    @Test
    fun deleteContribution_marksGoalAsIncompleteWhenBelowTarget() = runTest {
        // Given
        val goal = createTestGoal(targetAmount = 10000.0, currentAmount = 8000.0)
        val goalId = repository.createGoal(goal)

        // Add two contributions to reach target
        val contribution1Id = repository.addContribution(createTestContribution(goalId = goalId, amount = 1000.0))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 1000.0))

        // Verify goal is completed
        var updatedGoal = repository.getGoal(goalId)
        assertTrue(updatedGoal?.isCompleted == true)

        // When - Delete one contribution, dropping below target
        repository.deleteContribution(contribution1Id)

        // Then - Goal should be marked as incomplete
        updatedGoal = repository.getGoal(goalId)
        assertEquals(9000.0, updatedGoal?.currentAmount, 0.01)
        assertFalse(updatedGoal?.isCompleted == true)
        assertNull(updatedGoal?.completedAt)
    }

    @Test
    fun deleteContribution_returnsFalseForNonExistentContribution() = runTest {
        // When
        val deleted = repository.deleteContribution(999L)

        // Then
        assertFalse(deleted)
    }

    @Test
    fun getTotalContributions_returnsCorrectSum() = runTest {
        // Given
        val goalId = repository.createGoal(createTestGoal(currentAmount = 0.0))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 1000.0))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 2000.0))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 3000.0))

        // When
        val total = repository.getTotalContributions(goalId)

        // Then
        assertEquals(6000.0, total, 0.01)
    }

    @Test
    fun getTotalContributionsFlow_emitsCorrectSum() = runTest {
        // Given
        val goalId = repository.createGoal(createTestGoal(currentAmount = 0.0))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 1000.0))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 2000.0))

        // When
        val total = repository.getTotalContributionsFlow(goalId).first()

        // Then
        assertEquals(3000.0, total, 0.01)
    }

    @Test
    fun deleteGoal_cascadeDeletesContributions() = runTest {
        // Given
        val goalId = repository.createGoal(createTestGoal())
        repository.addContribution(createTestContribution(goalId = goalId, amount = 1000.0))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 2000.0))

        // When
        repository.deleteGoal(goalId)

        // Then
        val contributions = repository.getContributionsSnapshot(goalId)
        assertEquals(0, contributions.size)
    }

    // ==================== Progress Calculation Tests ====================

    @Test
    fun getGoalProgress_returnsCorrectProgressMetrics() = runTest {
        // Given
        val goal = createTestGoal(targetAmount = 10000.0, currentAmount = 0.0)
        val goalId = repository.createGoal(goal)
        repository.addContribution(createTestContribution(goalId = goalId, amount = 2000.0))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 3000.0))

        // When
        val progress = repository.getGoalProgress(goalId)

        // Then
        assertNotNull(progress)
        assertEquals(5000.0, progress?.totalContributions, 0.01)
        assertEquals(2, progress?.contributions?.size)
        assertEquals(50.0, progress?.percentageComplete, 0.01)
    }

    @Test
    fun getGoalProgress_returnsNullForNonExistentGoal() = runTest {
        // When
        val progress = repository.getGoalProgress(999L)

        // Then
        assertNull(progress)
    }

    @Test
    fun getGoalProgressFlow_emitsProgressUpdates() = runTest {
        // Given
        val goal = createTestGoal(targetAmount = 10000.0, currentAmount = 0.0)
        val goalId = repository.createGoal(goal)
        repository.addContribution(createTestContribution(goalId = goalId, amount = 3000.0))

        // When
        val progress = repository.getGoalProgressFlow(goalId).first()

        // Then
        assertNotNull(progress)
        assertEquals(3000.0, progress?.totalContributions, 0.01)
        assertEquals(30.0, progress?.percentageComplete, 0.01)
    }

    @Test
    fun markGoalAsCompleted_marksGoalCorrectly() = runTest {
        // Given
        val goal = createTestGoal(isCompleted = false)
        val goalId = repository.createGoal(goal)

        // When
        val success = repository.markGoalAsCompleted(goalId)

        // Then
        assertTrue(success)
        val updatedGoal = repository.getGoal(goalId)
        assertTrue(updatedGoal?.isCompleted == true)
        assertNotNull(updatedGoal?.completedAt)
    }

    @Test
    fun markGoalAsCompleted_returnsFalseForNonExistentGoal() = runTest {
        // When
        val success = repository.markGoalAsCompleted(999L)

        // Then
        assertFalse(success)
    }

    @Test
    fun updateGoalProgress_updatesCurrentAmount() = runTest {
        // Given
        val goal = createTestGoal(currentAmount = 0.0)
        val goalId = repository.createGoal(goal)

        // When
        val success = repository.updateGoalProgress(goalId, 5000.0)

        // Then
        assertTrue(success)
        val updatedGoal = repository.getGoal(goalId)
        assertEquals(5000.0, updatedGoal?.currentAmount, 0.01)
    }

    @Test
    fun updateGoalProgress_marksAsCompletedWhenTargetReached() = runTest {
        // Given
        val goal = createTestGoal(targetAmount = 10000.0, currentAmount = 0.0)
        val goalId = repository.createGoal(goal)

        // When
        repository.updateGoalProgress(goalId, 10000.0)

        // Then
        val updatedGoal = repository.getGoal(goalId)
        assertTrue(updatedGoal?.isCompleted == true)
        assertNotNull(updatedGoal?.completedAt)
    }

    @Test
    fun updateGoalProgress_returnsFalseForNonExistentGoal() = runTest {
        // When
        val success = repository.updateGoalProgress(999L, 5000.0)

        // Then
        assertFalse(success)
    }

    // ==================== Statistics Tests ====================

    @Test
    fun getGoalStatistics_returnsCorrectStatistics() = runTest {
        // Given
        repository.createGoal(createTestGoal(name = "Goal 1", targetAmount = 10000.0, currentAmount = 5000.0, isCompleted = false))
        repository.createGoal(createTestGoal(name = "Goal 2", targetAmount = 20000.0, currentAmount = 20000.0, isCompleted = true))
        repository.createGoal(createTestGoal(name = "Goal 3", targetAmount = 15000.0, currentAmount = 7500.0, isCompleted = false))

        // When
        val stats = repository.getGoalStatistics()

        // Then
        assertEquals(3, stats["totalGoals"])
        assertEquals(2, stats["activeGoals"])
        assertEquals(1, stats["completedGoals"])
        assertEquals(45000.0, stats["totalTargetAmount"] as Double, 0.01)
        assertEquals(32500.0, stats["totalCurrentAmount"] as Double, 0.01)
        assertEquals(32500.0, stats["totalSaved"] as Double, 0.01)
        assertEquals(12500.0, stats["totalRemaining"] as Double, 0.01)
        assertEquals(20000.0, stats["totalCompletedAmount"] as Double, 0.01)

        val overallProgress = stats["overallProgress"] as Double
        assertTrue(overallProgress > 70.0 && overallProgress < 75.0)
    }

    @Test
    fun getGoalStatistics_handlesEmptyDatabase() = runTest {
        // When
        val stats = repository.getGoalStatistics()

        // Then
        assertEquals(0, stats["totalGoals"])
        assertEquals(0, stats["activeGoals"])
        assertEquals(0, stats["completedGoals"])
        assertEquals(0.0, stats["overallProgress"] as Double, 0.01)
    }

    @Test
    fun getTotalSaved_returnsCorrectSum() = runTest {
        // Given
        repository.createGoal(createTestGoal(currentAmount = 5000.0))
        repository.createGoal(createTestGoal(currentAmount = 3000.0))
        repository.createGoal(createTestGoal(currentAmount = 2000.0))

        // When
        val totalSaved = repository.getTotalSaved()

        // Then
        assertEquals(10000.0, totalSaved, 0.01)
    }

    @Test
    fun getActiveGoalCount_returnsCorrectCount() = runTest {
        // Given
        repository.createGoal(createTestGoal(isCompleted = false))
        repository.createGoal(createTestGoal(isCompleted = false))
        repository.createGoal(createTestGoal(isCompleted = true))

        // When
        val count = repository.getActiveGoalCount()

        // Then
        assertEquals(2, count)
    }

    @Test
    fun getCompletedGoalCount_returnsCorrectCount() = runTest {
        // Given
        repository.createGoal(createTestGoal(isCompleted = false))
        repository.createGoal(createTestGoal(isCompleted = true))
        repository.createGoal(createTestGoal(isCompleted = true))

        // When
        val count = repository.getCompletedGoalCount()

        // Then
        assertEquals(2, count)
    }

    @Test
    fun getGoalCount_returnsCorrectCount() = runTest {
        // Given
        repository.createGoal(createTestGoal())
        repository.createGoal(createTestGoal())
        repository.createGoal(createTestGoal())

        // When
        val count = repository.getGoalCount()

        // Then
        assertEquals(3, count)
    }

    // ==================== Entity-Model Mapping Tests ====================

    @Test
    fun entityMapping_preservesAllFields() = runTest {
        // Given
        val original = SavingsGoal(
            id = 0,
            name = "Vacation Fund",
            targetAmount = 25000.0,
            currentAmount = 12500.0,
            deadline = 1735689600000L,
            createdAt = 1704067200000L,
            isCompleted = false,
            completedAt = null,
            icon = "✈️",
            color = "#2196F3"
        )

        // When
        val goalId = repository.createGoal(original)
        val loaded = repository.getGoal(goalId)

        // Then - All fields should match exactly
        assertNotNull(loaded)
        assertEquals(goalId, loaded?.id)
        assertEquals(original.name, loaded?.name)
        assertEquals(original.targetAmount, loaded?.targetAmount, 0.001)
        assertEquals(original.currentAmount, loaded?.currentAmount, 0.001)
        assertEquals(original.deadline, loaded?.deadline)
        assertEquals(original.createdAt, loaded?.createdAt)
        assertEquals(original.isCompleted, loaded?.isCompleted)
        assertEquals(original.completedAt, loaded?.completedAt)
        assertEquals(original.icon, loaded?.icon)
        assertEquals(original.color, loaded?.color)
    }

    @Test
    fun entityMapping_handlesNullFields() = runTest {
        // Given - Goal with all nullable fields as null
        val original = SavingsGoal(
            id = 0,
            name = "Simple Goal",
            targetAmount = 10000.0,
            currentAmount = 0.0,
            deadline = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            isCompleted = false,
            completedAt = null,
            icon = null,
            color = null
        )

        // When
        val goalId = repository.createGoal(original)
        val loaded = repository.getGoal(goalId)

        // Then
        assertNotNull(loaded)
        assertNull(loaded?.completedAt)
        assertNull(loaded?.icon)
        assertNull(loaded?.color)
    }

    @Test
    fun contributionMapping_preservesAllFields() = runTest {
        // Given
        val goalId = repository.createGoal(createTestGoal())
        val original = GoalContribution(
            id = 0,
            goalId = goalId,
            amount = 2500.0,
            date = 1704067200000L,
            note = "Monthly savings"
        )

        // When
        val contributionId = repository.addContribution(original)
        val contributions = repository.getContributionsSnapshot(goalId)
        val loaded = contributions.firstOrNull { it.id == contributionId }

        // Then - All fields should match exactly
        assertNotNull(loaded)
        assertEquals(contributionId, loaded?.id)
        assertEquals(goalId, loaded?.goalId)
        assertEquals(original.amount, loaded?.amount, 0.001)
        assertEquals(original.date, loaded?.date)
        assertEquals(original.note, loaded?.note)
    }

    @Test
    fun contributionMapping_handlesNullNote() = runTest {
        // Given
        val goalId = repository.createGoal(createTestGoal())
        val original = GoalContribution(
            id = 0,
            goalId = goalId,
            amount = 1000.0,
            date = System.currentTimeMillis(),
            note = null
        )

        // When
        val contributionId = repository.addContribution(original)
        val contributions = repository.getContributionsSnapshot(goalId)
        val loaded = contributions.firstOrNull { it.id == contributionId }

        // Then
        assertNotNull(loaded)
        assertNull(loaded?.note)
    }

    // ==================== Domain Model Calculated Properties Tests ====================

    @Test
    fun savingsGoal_calculatesStatusCorrectly() = runTest {
        // Given - Active goal
        val activeGoal = createTestGoal(
            isCompleted = false,
            deadline = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
        )
        val activeId = repository.createGoal(activeGoal)

        // Given - Completed goal
        val completedGoal = createTestGoal(
            isCompleted = true,
            completedAt = System.currentTimeMillis()
        )
        val completedId = repository.createGoal(completedGoal)

        // Given - Overdue goal
        val overdueGoal = createTestGoal(
            isCompleted = false,
            deadline = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000)
        )
        val overdueId = repository.createGoal(overdueGoal)

        // When
        val active = repository.getGoal(activeId)
        val completed = repository.getGoal(completedId)
        val overdue = repository.getGoal(overdueId)

        // Then
        assertEquals(GoalStatus.ACTIVE, active?.status)
        assertEquals(GoalStatus.COMPLETED, completed?.status)
        assertEquals(GoalStatus.OVERDUE, overdue?.status)
    }

    @Test
    fun savingsGoal_calculatesProgressPercentageCorrectly() = runTest {
        // Given
        val goal = createTestGoal(targetAmount = 10000.0, currentAmount = 2500.0)
        val goalId = repository.createGoal(goal)

        // When
        val loaded = repository.getGoal(goalId)

        // Then
        assertEquals(0.25, loaded?.progressPercentage, 0.01)
    }

    @Test
    fun savingsGoal_calculatesRemainingAmountCorrectly() = runTest {
        // Given
        val goal = createTestGoal(targetAmount = 10000.0, currentAmount = 3000.0)
        val goalId = repository.createGoal(goal)

        // When
        val loaded = repository.getGoal(goalId)

        // Then
        assertEquals(7000.0, loaded?.remainingAmount, 0.01)
    }

    // ==================== Integration Tests ====================

    @Test
    fun fullWorkflow_createGoalAddContributionsComplete() = runTest {
        // 1. Create goal
        val goal = createTestGoal(
            name = "Emergency Fund",
            targetAmount = 10000.0,
            currentAmount = 0.0
        )
        val goalId = repository.createGoal(goal)
        assertEquals(1, repository.getGoalCount())

        // 2. Add contributions
        repository.addContribution(createTestContribution(goalId = goalId, amount = 3000.0, note = "First deposit"))
        repository.addContribution(createTestContribution(goalId = goalId, amount = 2000.0, note = "Second deposit"))

        var updatedGoal = repository.getGoal(goalId)
        assertEquals(5000.0, updatedGoal?.currentAmount, 0.01)
        assertFalse(updatedGoal?.isCompleted == true)

        // 3. Add final contribution to complete goal
        repository.addContribution(createTestContribution(goalId = goalId, amount = 5000.0, note = "Final deposit"))

        updatedGoal = repository.getGoal(goalId)
        assertEquals(10000.0, updatedGoal?.currentAmount, 0.01)
        assertTrue(updatedGoal?.isCompleted == true)
        assertNotNull(updatedGoal?.completedAt)

        // 4. Verify progress
        val progress = repository.getGoalProgress(goalId)
        assertEquals(3, progress?.contributions?.size)
        assertEquals(10000.0, progress?.totalContributions, 0.01)
        assertEquals(100.0, progress?.percentageComplete, 0.01)

        // 5. Verify statistics
        val stats = repository.getGoalStatistics()
        assertEquals(1, stats["totalGoals"])
        assertEquals(0, stats["activeGoals"])
        assertEquals(1, stats["completedGoals"])
        assertEquals(10000.0, stats["totalSaved"] as Double, 0.01)
    }

    @Test
    fun fullWorkflow_updateGoalAndDeleteContribution() = runTest {
        // 1. Create goal and add contributions
        val goal = createTestGoal(targetAmount = 10000.0, currentAmount = 0.0)
        val goalId = repository.createGoal(goal)
        val contrib1Id = repository.addContribution(createTestContribution(goalId = goalId, amount = 3000.0))
        val contrib2Id = repository.addContribution(createTestContribution(goalId = goalId, amount = 4000.0))

        var updatedGoal = repository.getGoal(goalId)
        assertEquals(7000.0, updatedGoal?.currentAmount, 0.01)

        // 2. Update goal target
        val modified = updatedGoal?.copy(targetAmount = 15000.0)
        repository.updateGoal(modified!!)

        updatedGoal = repository.getGoal(goalId)
        assertEquals(15000.0, updatedGoal?.targetAmount, 0.01)

        // 3. Delete a contribution
        repository.deleteContribution(contrib1Id)

        updatedGoal = repository.getGoal(goalId)
        assertEquals(4000.0, updatedGoal?.currentAmount, 0.01)

        // 4. Verify contributions
        val contributions = repository.getContributionsSnapshot(goalId)
        assertEquals(1, contributions.size)
        assertEquals(contrib2Id, contributions[0].id)
    }

    @Test
    fun multipleGoals_maintainIndependence() = runTest {
        // Given - Create multiple goals with contributions
        val goal1Id = repository.createGoal(createTestGoal(name = "Goal 1", targetAmount = 10000.0))
        val goal2Id = repository.createGoal(createTestGoal(name = "Goal 2", targetAmount = 20000.0))
        val goal3Id = repository.createGoal(createTestGoal(name = "Goal 3", targetAmount = 15000.0))

        repository.addContribution(createTestContribution(goalId = goal1Id, amount = 2000.0))
        repository.addContribution(createTestContribution(goalId = goal1Id, amount = 3000.0))
        repository.addContribution(createTestContribution(goalId = goal2Id, amount = 5000.0))
        repository.addContribution(createTestContribution(goalId = goal3Id, amount = 1000.0))

        // When - Query each goal
        val total1 = repository.getTotalContributions(goal1Id)
        val total2 = repository.getTotalContributions(goal2Id)
        val total3 = repository.getTotalContributions(goal3Id)

        // Then - Each goal maintains its own contributions
        assertEquals(5000.0, total1, 0.01)
        assertEquals(5000.0, total2, 0.01)
        assertEquals(1000.0, total3, 0.01)

        // When - Delete one goal
        repository.deleteGoal(goal2Id)

        // Then - Other goals remain unaffected
        assertEquals(3, repository.getGoalCount())
        assertEquals(2, repository.getAllGoals().first().size + 1) // 2 remaining + 1 deleted
        assertEquals(5000.0, repository.getTotalContributions(goal1Id), 0.01)
        assertEquals(1000.0, repository.getTotalContributions(goal3Id), 0.01)
    }
}
