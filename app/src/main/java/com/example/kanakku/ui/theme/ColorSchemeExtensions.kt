package com.example.kanakku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Extension properties for accessing theme-aware custom colors.
 * These colors automatically adapt to light/dark mode based on the current theme.
 */

/**
 * Theme-aware color for income/credit transactions.
 * Returns lighter green in dark mode, darker green in light mode.
 */
val MaterialTheme.colorScheme.incomeColor: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val isDark = background.luminance() < 0.5f
        return if (isDark) IncomeGreen80 else IncomeGreen40
    }

/**
 * Theme-aware color for expense/debit transactions.
 * Returns lighter red in dark mode, darker red in light mode.
 */
val MaterialTheme.colorScheme.expenseColor: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val isDark = background.luminance() < 0.5f
        return if (isDark) ExpenseRed80 else ExpenseRed40
    }

/**
 * Theme-aware background color for income transaction cards.
 * Returns dark-tinted background in dark mode, light background in light mode.
 */
val MaterialTheme.colorScheme.incomeBackground: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val isDark = background.luminance() < 0.5f
        return if (isDark) IncomeBackgroundGreen80 else IncomeBackgroundGreen40
    }

/**
 * Theme-aware background color for expense transaction cards.
 * Returns dark-tinted background in dark mode, light background in light mode.
 */
val MaterialTheme.colorScheme.expenseBackground: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val isDark = background.luminance() < 0.5f
        return if (isDark) ExpenseBackgroundRed80 else ExpenseBackgroundRed40
    }

/**
 * Theme-aware blue color for analytics charts.
 * Returns lighter blue in dark mode, darker blue in light mode.
 */
val MaterialTheme.colorScheme.chartBlue: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val isDark = background.luminance() < 0.5f
        return if (isDark) ChartBlue80 else ChartBlue40
    }

/**
 * Theme-aware purple color for analytics charts.
 * Returns lighter purple in dark mode, darker purple in light mode.
 */
val MaterialTheme.colorScheme.chartPurple: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val isDark = background.luminance() < 0.5f
        return if (isDark) ChartPurple80 else ChartPurple40
    }

/**
 * Theme-aware gold color for first place rankings.
 * Returns darker gold in dark mode for better contrast.
 */
val MaterialTheme.colorScheme.goldRanking: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val isDark = background.luminance() < 0.5f
        return if (isDark) GoldDark else Gold
    }

/**
 * Theme-aware silver color for second place rankings.
 * Returns darker silver in dark mode for better contrast.
 */
val MaterialTheme.colorScheme.silverRanking: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val isDark = background.luminance() < 0.5f
        return if (isDark) SilverDark else Silver
    }

/**
 * Theme-aware bronze color for third place rankings.
 * Returns darker bronze in dark mode for better contrast.
 */
val MaterialTheme.colorScheme.bronzeRanking: Color
    @Composable
    @ReadOnlyComposable
    get() {
        val isDark = background.luminance() < 0.5f
        return if (isDark) BronzeDark else Bronze
    }
