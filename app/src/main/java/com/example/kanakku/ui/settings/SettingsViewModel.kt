package com.example.kanakku.ui.settings

/**
 * UI state for the Settings screen.
 *
 * This data class holds all settings-related state that is displayed
 * and managed through the Settings screen.
 *
 * @property isDarkMode Dark mode preference: true = dark, false = light, null = system default
 * @property isDynamicColors Whether Material You dynamic colors are enabled
 * @property isCompactView Whether to show transaction lists in compact view
 * @property showOfflineBadge Whether to display the offline badge in the UI
 * @property isNotificationsEnabled Whether SMS notifications are enabled
 * @property defaultAnalyticsPeriod Default time period for analytics (DAY, WEEK, MONTH, YEAR)
 * @property currencySymbol Currency symbol to display throughout the app
 * @property isAutoCategorize Whether automatic categorization is enabled
 * @property appVersion Current app version string for display
 * @property isClearing Loading state indicator while clearing data
 */
data class SettingsUiState(
    val isDarkMode: Boolean? = null,
    val isDynamicColors: Boolean = true,
    val isCompactView: Boolean = false,
    val showOfflineBadge: Boolean = true,
    val isNotificationsEnabled: Boolean = true,
    val defaultAnalyticsPeriod: String = "MONTH",
    val currencySymbol: String = "₹",
    val isAutoCategorize: Boolean = true,
    val appVersion: String = "",
    val isClearing: Boolean = false
)
