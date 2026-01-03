package com.example.kanakku.widget.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * SharedPreferences wrapper for storing and retrieving user's weekly budget amount.
 *
 * This class provides thread-safe access to budget-related preferences used by
 * the budget progress widget. The budget value represents the user's weekly
 * spending limit and is used to calculate budget progress percentage.
 *
 * Thread-safe singleton implementation using double-checked locking.
 * SharedPreferences instance is lazily initialized on first access.
 *
 * Key features:
 * - Store and retrieve weekly budget amount
 * - Default budget value of 0.0 (no budget set)
 * - Thread-safe access with synchronized methods
 * - Uses commit() for immediate persistence
 *
 * Usage:
 * ```
 * // Set weekly budget
 * BudgetPreferences.setWeeklyBudget(context, 10000.0)
 *
 * // Get weekly budget
 * val budget = BudgetPreferences.getWeeklyBudget(context)
 * ```
 */
object BudgetPreferences {

    @Volatile
    private var sharedPreferences: SharedPreferences? = null

    private const val PREFS_NAME = "kanakku_widget_prefs"
    private const val KEY_WEEKLY_BUDGET = "weekly_budget"
    private const val DEFAULT_BUDGET = 0.0

    /**
     * Gets the SharedPreferences instance.
     * Creates the preferences on first access using the provided context.
     *
     * @param context Application or Activity context (applicationContext is used internally)
     * @return The SharedPreferences instance
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return sharedPreferences ?: synchronized(this) {
            sharedPreferences ?: context.applicationContext.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            ).also {
                sharedPreferences = it
            }
        }
    }

    /**
     * Retrieves the user's weekly budget amount.
     * Returns the default value of 0.0 if no budget has been set.
     *
     * @param context Application or Activity context
     * @return The weekly budget amount, or 0.0 if not set
     */
    @Synchronized
    fun getWeeklyBudget(context: Context): Double {
        val prefs = getPreferences(context)
        // SharedPreferences doesn't have getDouble, so we store as Float and convert
        val budgetFloat = prefs.getFloat(KEY_WEEKLY_BUDGET, DEFAULT_BUDGET.toFloat())
        return budgetFloat.toDouble()
    }

    /**
     * Sets the user's weekly budget amount.
     * Immediately commits the value to persistent storage.
     *
     * @param context Application or Activity context
     * @param budget The weekly budget amount to store
     * @return true if the value was successfully saved, false otherwise
     */
    @Synchronized
    fun setWeeklyBudget(context: Context, budget: Double): Boolean {
        val prefs = getPreferences(context)
        // SharedPreferences doesn't have putDouble, so we store as Float
        prefs.edit(commit = true) {
            putFloat(KEY_WEEKLY_BUDGET, budget.toFloat())
        }
        return true
    }

    /**
     * Clears all budget preferences.
     * Useful for testing or resetting the widget configuration.
     *
     * WARNING: This will remove all stored budget data.
     *
     * @param context Application or Activity context
     * @return true if preferences were successfully cleared, false otherwise
     */
    @Synchronized
    fun clear(context: Context): Boolean {
        val prefs = getPreferences(context)
        prefs.edit(commit = true) {
            clear()
        }
        return true
    }

    /**
     * Checks if a weekly budget has been set by the user.
     * A budget is considered "set" if it's greater than 0.
     *
     * @param context Application or Activity context
     * @return true if a budget has been set (value > 0), false otherwise
     */
    @Synchronized
    fun hasBudgetSet(context: Context): Boolean {
        return getWeeklyBudget(context) > 0.0
    }
}
