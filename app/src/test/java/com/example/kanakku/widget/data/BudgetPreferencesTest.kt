package com.example.kanakku.widget.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BudgetPreferences using Robolectric.
 *
 * Tests cover:
 * - Setting and retrieving budget amounts
 * - Default values when budget not set
 * - Persistence across multiple accesses
 * - Clear functionality
 * - Budget status checking
 * - Edge cases: zero values, large values, precision
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BudgetPreferencesTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear preferences before each test
        BudgetPreferences.clear(context)
    }

    @After
    fun teardown() {
        // Clean up after each test
        BudgetPreferences.clear(context)
    }

    // ==================== Set and Get Tests ====================

    @Test
    fun setWeeklyBudget_storesValueSuccessfully() {
        // Given - A budget amount
        val budget = 10000.0

        // When
        val result = BudgetPreferences.setWeeklyBudget(context, budget)

        // Then
        assertTrue(result)
        assertEquals(10000.0, BudgetPreferences.getWeeklyBudget(context), 0.01)
    }

    @Test
    fun getWeeklyBudget_retrievesStoredValue() {
        // Given - Budget has been set
        BudgetPreferences.setWeeklyBudget(context, 5000.0)

        // When
        val budget = BudgetPreferences.getWeeklyBudget(context)

        // Then
        assertEquals(5000.0, budget, 0.01)
    }

    @Test
    fun setWeeklyBudget_updatesExistingValue() {
        // Given - Budget already set to 5000
        BudgetPreferences.setWeeklyBudget(context, 5000.0)

        // When - Update to new value
        BudgetPreferences.setWeeklyBudget(context, 7500.0)

        // Then - Should return new value
        assertEquals(7500.0, BudgetPreferences.getWeeklyBudget(context), 0.01)
    }

    // ==================== Default Value Tests ====================

    @Test
    fun getWeeklyBudget_returnsDefaultWhenNotSet() {
        // Given - No budget has been set (cleared in setup)
        // When
        val budget = BudgetPreferences.getWeeklyBudget(context)

        // Then - Should return default value of 0.0
        assertEquals(0.0, budget, 0.01)
    }

    @Test
    fun hasBudgetSet_returnsFalseWhenNotSet() {
        // Given - No budget has been set
        // When
        val hasbudget = BudgetPreferences.hasBudgetSet(context)

        // Then
        assertFalse(hasbudget)
    }

    @Test
    fun hasBudgetSet_returnsTrueWhenSet() {
        // Given - Budget has been set
        BudgetPreferences.setWeeklyBudget(context, 10000.0)

        // When
        val hasBudget = BudgetPreferences.hasBudgetSet(context)

        // Then
        assertTrue(hasBudget)
    }

    // ==================== Persistence Tests ====================

    @Test
    fun budget_persistsAcrossMultipleRetrievals() {
        // Given - Budget is set
        BudgetPreferences.setWeeklyBudget(context, 8000.0)

        // When - Retrieved multiple times
        val budget1 = BudgetPreferences.getWeeklyBudget(context)
        val budget2 = BudgetPreferences.getWeeklyBudget(context)
        val budget3 = BudgetPreferences.getWeeklyBudget(context)

        // Then - All should return same value
        assertEquals(8000.0, budget1, 0.01)
        assertEquals(8000.0, budget2, 0.01)
        assertEquals(8000.0, budget3, 0.01)
    }

    @Test
    fun budget_persistsAfterContextRecreation() {
        // Given - Budget is set
        BudgetPreferences.setWeeklyBudget(context, 12000.0)

        // When - Get new context instance
        val newContext = ApplicationProvider.getApplicationContext<Context>()
        val budget = BudgetPreferences.getWeeklyBudget(newContext)

        // Then - Should still retrieve same value
        assertEquals(12000.0, budget, 0.01)
    }

    // ==================== Clear Tests ====================

    @Test
    fun clear_removesStoredBudget() {
        // Given - Budget has been set
        BudgetPreferences.setWeeklyBudget(context, 10000.0)
        assertEquals(10000.0, BudgetPreferences.getWeeklyBudget(context), 0.01)

        // When - Clear preferences
        val result = BudgetPreferences.clear(context)

        // Then - Should return to default
        assertTrue(result)
        assertEquals(0.0, BudgetPreferences.getWeeklyBudget(context), 0.01)
        assertFalse(BudgetPreferences.hasBudgetSet(context))
    }

    @Test
    fun clear_worksWhenNothingStored() {
        // Given - No budget set (cleared in setup)
        // When
        val result = BudgetPreferences.clear(context)

        // Then - Should succeed even when nothing to clear
        assertTrue(result)
        assertEquals(0.0, BudgetPreferences.getWeeklyBudget(context), 0.01)
    }

    // ==================== Edge Cases ====================

    @Test
    fun setWeeklyBudget_handlesZeroValue() {
        // Given - Zero budget
        // When
        BudgetPreferences.setWeeklyBudget(context, 0.0)

        // Then - Should store and retrieve zero
        assertEquals(0.0, BudgetPreferences.getWeeklyBudget(context), 0.01)
        // Zero is not considered "set"
        assertFalse(BudgetPreferences.hasBudgetSet(context))
    }

    @Test
    fun setWeeklyBudget_handlesVerySmallValue() {
        // Given - Small budget amount
        val budget = 0.01

        // When
        BudgetPreferences.setWeeklyBudget(context, budget)

        // Then - Should store and retrieve small value
        assertEquals(0.01, BudgetPreferences.getWeeklyBudget(context), 0.001)
        assertTrue(BudgetPreferences.hasBudgetSet(context))
    }

    @Test
    fun setWeeklyBudget_handlesLargeValue() {
        // Given - Large budget amount
        val budget = 999999.99

        // When
        BudgetPreferences.setWeeklyBudget(context, budget)

        // Then - Should store and retrieve large value
        // Note: Float precision may cause slight differences
        assertEquals(999999.99, BudgetPreferences.getWeeklyBudget(context), 1.0)
    }

    @Test
    fun setWeeklyBudget_handlesDecimalPrecision() {
        // Given - Budget with decimal places
        val budget = 12345.67

        // When
        BudgetPreferences.setWeeklyBudget(context, budget)

        // Then - Should maintain reasonable precision
        // Note: Stored as Float, so some precision loss is expected
        assertEquals(12345.67, BudgetPreferences.getWeeklyBudget(context), 0.1)
    }

    @Test
    fun setWeeklyBudget_handlesNegativeValue() {
        // Given - Negative budget (edge case, should not happen in practice)
        val budget = -1000.0

        // When
        BudgetPreferences.setWeeklyBudget(context, budget)

        // Then - Should store negative value
        assertEquals(-1000.0, BudgetPreferences.getWeeklyBudget(context), 0.01)
        // Negative is considered "not set" by hasBudgetSet (> 0 check)
        assertFalse(BudgetPreferences.hasBudgetSet(context))
    }

    // ==================== Multiple Operations Tests ====================

    @Test
    fun multipleSetOperations_lastValueWins() {
        // Given - Multiple budget updates
        BudgetPreferences.setWeeklyBudget(context, 1000.0)
        BudgetPreferences.setWeeklyBudget(context, 2000.0)
        BudgetPreferences.setWeeklyBudget(context, 3000.0)

        // When
        val budget = BudgetPreferences.getWeeklyBudget(context)

        // Then - Should return last set value
        assertEquals(3000.0, budget, 0.01)
    }

    @Test
    fun setThenClearThenSet_worksCorrectly() {
        // Given - Set budget
        BudgetPreferences.setWeeklyBudget(context, 5000.0)
        assertEquals(5000.0, BudgetPreferences.getWeeklyBudget(context), 0.01)

        // When - Clear then set new value
        BudgetPreferences.clear(context)
        assertEquals(0.0, BudgetPreferences.getWeeklyBudget(context), 0.01)
        BudgetPreferences.setWeeklyBudget(context, 8000.0)

        // Then - Should have new value
        assertEquals(8000.0, BudgetPreferences.getWeeklyBudget(context), 0.01)
        assertTrue(BudgetPreferences.hasBudgetSet(context))
    }

    // ==================== Typical User Scenarios ====================

    @Test
    fun typicalScenario_firstTimeUser() {
        // Simulate first-time user checking budget
        // When - User hasn't set budget yet
        val hasbudget = BudgetPreferences.hasBudgetSet(context)
        val defaultBudget = BudgetPreferences.getWeeklyBudget(context)

        // Then - Should indicate no budget set
        assertFalse(hasbudget)
        assertEquals(0.0, defaultBudget, 0.01)
    }

    @Test
    fun typicalScenario_userSetsBudget() {
        // Simulate user setting weekly budget
        // When - User sets budget to 15,000
        BudgetPreferences.setWeeklyBudget(context, 15000.0)

        // Then - Widget can retrieve it
        assertTrue(BudgetPreferences.hasBudgetSet(context))
        assertEquals(15000.0, BudgetPreferences.getWeeklyBudget(context), 0.01)
    }

    @Test
    fun typicalScenario_userUpdatesBudget() {
        // Simulate user updating existing budget
        // Given - User has budget of 10,000
        BudgetPreferences.setWeeklyBudget(context, 10000.0)

        // When - User increases to 12,000
        BudgetPreferences.setWeeklyBudget(context, 12000.0)

        // Then - Should reflect new amount
        assertEquals(12000.0, BudgetPreferences.getWeeklyBudget(context), 0.01)
    }

    @Test
    fun typicalScenario_userResetsBudget() {
        // Simulate user resetting/removing budget
        // Given - User has budget set
        BudgetPreferences.setWeeklyBudget(context, 10000.0)

        // When - User clears budget
        BudgetPreferences.clear(context)

        // Then - Should return to default state
        assertFalse(BudgetPreferences.hasBudgetSet(context))
        assertEquals(0.0, BudgetPreferences.getWeeklyBudget(context), 0.01)
    }
}
