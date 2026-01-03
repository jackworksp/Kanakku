package com.example.kanakku.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.BuildConfig
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.core.error.toErrorInfo
import com.example.kanakku.data.database.DatabaseProvider
import com.example.kanakku.data.preferences.AppPreferences
import com.example.kanakku.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
 * @property errorMessage Error message to display to user, null if no error
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
    val isClearing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for managing Settings screen state and business logic.
 *
 * This ViewModel:
 * - Loads current preferences from AppPreferences on initialization
 * - Exposes UI state via StateFlow for reactive updates
 * - Provides methods to update individual preferences
 * - Handles data clearing operations with repository integration
 * - Uses ErrorHandler for comprehensive error handling
 *
 * Error Handling:
 * - All operations wrapped in try-catch with ErrorHandler
 * - User-friendly error messages exposed via uiState.errorMessage
 * - Technical details logged for debugging
 * - Graceful degradation on errors
 */
class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var appPreferences: AppPreferences? = null
    private var repository: TransactionRepository? = null

    /**
     * Initializes the ViewModel by loading current preferences.
     * Should be called from the UI after ViewModel creation.
     *
     * @param context Application context for accessing preferences
     */
    fun initialize(context: Context) {
        viewModelScope.launch {
            try {
                // Initialize AppPreferences
                appPreferences = AppPreferences.getInstance(context)

                // Initialize repository
                repository = DatabaseProvider.getRepository(context)

                // Load current preferences
                loadPreferences()

                ErrorHandler.logInfo("SettingsViewModel initialized", "SettingsViewModel")
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Initialize SettingsViewModel")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Loads all preferences from AppPreferences and updates UI state.
     */
    private fun loadPreferences() {
        try {
            val prefs = appPreferences ?: return

            _uiState.value = _uiState.value.copy(
                isDarkMode = prefs.isDarkModeEnabled(),
                isDynamicColors = prefs.isDynamicColorsEnabled(),
                isCompactView = prefs.isCompactViewEnabled(),
                showOfflineBadge = prefs.shouldShowOfflineBadge(),
                isNotificationsEnabled = prefs.isNotificationsEnabled(),
                defaultAnalyticsPeriod = prefs.getDefaultAnalyticsPeriod(),
                currencySymbol = prefs.getCurrencySymbol(),
                isAutoCategorize = prefs.isAutoCategorizeEnabled(),
                appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            )

            ErrorHandler.logDebug("Preferences loaded successfully", "SettingsViewModel")
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Load preferences")
            _uiState.value = _uiState.value.copy(
                errorMessage = errorInfo.userMessage
            )
        }
    }

    // ============================================
    // Preference Update Methods
    // ============================================

    /**
     * Updates dark mode preference.
     *
     * @param enabled true for dark mode, false for light mode, null for system default
     */
    fun updateDarkMode(enabled: Boolean?) {
        try {
            appPreferences?.setDarkModeEnabled(enabled)
            _uiState.value = _uiState.value.copy(isDarkMode = enabled)
            ErrorHandler.logDebug("Dark mode updated to: $enabled", "SettingsViewModel")
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Update dark mode")
            _uiState.value = _uiState.value.copy(
                errorMessage = errorInfo.userMessage
            )
        }
    }

    /**
     * Updates dynamic colors preference.
     *
     * @param enabled true to enable dynamic colors, false to disable
     */
    fun updateDynamicColors(enabled: Boolean) {
        try {
            appPreferences?.setDynamicColorsEnabled(enabled)
            _uiState.value = _uiState.value.copy(isDynamicColors = enabled)
            ErrorHandler.logDebug("Dynamic colors updated to: $enabled", "SettingsViewModel")
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Update dynamic colors")
            _uiState.value = _uiState.value.copy(
                errorMessage = errorInfo.userMessage
            )
        }
    }

    /**
     * Updates compact view preference.
     *
     * @param enabled true for compact view, false for normal view
     */
    fun updateCompactView(enabled: Boolean) {
        try {
            appPreferences?.setCompactViewEnabled(enabled)
            _uiState.value = _uiState.value.copy(isCompactView = enabled)
            ErrorHandler.logDebug("Compact view updated to: $enabled", "SettingsViewModel")
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Update compact view")
            _uiState.value = _uiState.value.copy(
                errorMessage = errorInfo.userMessage
            )
        }
    }

    /**
     * Updates offline badge visibility preference.
     *
     * @param show true to show offline badge, false to hide
     */
    fun updateShowOfflineBadge(show: Boolean) {
        try {
            appPreferences?.setShowOfflineBadge(show)
            _uiState.value = _uiState.value.copy(showOfflineBadge = show)
            ErrorHandler.logDebug("Show offline badge updated to: $show", "SettingsViewModel")
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Update offline badge visibility")
            _uiState.value = _uiState.value.copy(
                errorMessage = errorInfo.userMessage
            )
        }
    }

    /**
     * Updates notifications preference.
     *
     * @param enabled true to enable notifications, false to disable
     */
    fun updateNotificationsEnabled(enabled: Boolean) {
        try {
            appPreferences?.setNotificationsEnabled(enabled)
            _uiState.value = _uiState.value.copy(isNotificationsEnabled = enabled)
            ErrorHandler.logDebug("Notifications updated to: $enabled", "SettingsViewModel")
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Update notifications")
            _uiState.value = _uiState.value.copy(
                errorMessage = errorInfo.userMessage
            )
        }
    }

    /**
     * Updates default analytics period preference.
     *
     * @param period Default time period (DAY, WEEK, MONTH, or YEAR)
     */
    fun updateDefaultAnalyticsPeriod(period: String) {
        try {
            appPreferences?.setDefaultAnalyticsPeriod(period)
            _uiState.value = _uiState.value.copy(defaultAnalyticsPeriod = period)
            ErrorHandler.logDebug("Default analytics period updated to: $period", "SettingsViewModel")
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Update default analytics period")
            _uiState.value = _uiState.value.copy(
                errorMessage = errorInfo.userMessage
            )
        }
    }

    /**
     * Updates currency symbol preference.
     *
     * @param symbol Currency symbol to use in the app
     */
    fun updateCurrencySymbol(symbol: String) {
        try {
            appPreferences?.setCurrencySymbol(symbol)
            _uiState.value = _uiState.value.copy(currencySymbol = symbol)
            ErrorHandler.logDebug("Currency symbol updated to: $symbol", "SettingsViewModel")
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Update currency symbol")
            _uiState.value = _uiState.value.copy(
                errorMessage = errorInfo.userMessage
            )
        }
    }

    /**
     * Updates auto-categorize preference.
     *
     * @param enabled true to enable auto-categorize, false to disable
     */
    fun updateAutoCategorize(enabled: Boolean) {
        try {
            appPreferences?.setAutoCategorizeEnabled(enabled)
            _uiState.value = _uiState.value.copy(isAutoCategorize = enabled)
            ErrorHandler.logDebug("Auto-categorize updated to: $enabled", "SettingsViewModel")
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Update auto-categorize")
            _uiState.value = _uiState.value.copy(
                errorMessage = errorInfo.userMessage
            )
        }
    }

    // ============================================
    // Data Management
    // ============================================

    /**
     * Clears all app data including transactions, category overrides, and sync metadata.
     * Preserves theme and display preferences for better user experience.
     *
     * This operation:
     * 1. Deletes all transactions from database
     * 2. Removes all category overrides
     * 3. Clears sync metadata
     * 4. Preserves theme preferences (dark mode, dynamic colors)
     * 5. Updates UI state to reflect completion
     *
     * Error Handling:
     * - Partial success: Some operations may succeed while others fail
     * - User is notified of any errors via errorMessage
     * - Loading state managed via isClearing flag
     */
    fun clearAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClearing = true, errorMessage = null)

            try {
                val repo = repository ?: run {
                    val errorInfo = ErrorHandler.handleError(
                        IllegalStateException("Repository not initialized"),
                        "Clear all data"
                    )
                    _uiState.value = _uiState.value.copy(
                        isClearing = false,
                        errorMessage = errorInfo.userMessage
                    )
                    return@launch
                }

                // Step 1: Delete all transactions
                val transactionsDeleted = repo.deleteAllTransactions()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to delete transactions: ${errorInfo.technicalMessage}",
                            "clearAllData"
                        )
                        _uiState.value = _uiState.value.copy(
                            isClearing = false,
                            errorMessage = "Failed to delete transactions: ${errorInfo.userMessage}"
                        )
                        return@launch
                    }
                    .getOrElse { 0 }

                // Step 2: Remove all category overrides
                val overridesRemoved = repo.removeAllCategoryOverrides()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to remove category overrides: ${errorInfo.technicalMessage}",
                            "clearAllData"
                        )
                        // Continue - not critical
                    }
                    .getOrElse { 0 }

                // Step 3: Clear sync metadata
                val metadataCleared = repo.clearSyncMetadata()
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to clear sync metadata: ${errorInfo.technicalMessage}",
                            "clearAllData"
                        )
                        // Continue - not critical
                    }
                    .getOrElse { 0 }

                // Step 4: Selectively clear some preferences while preserving theme settings
                appPreferences?.let { prefs ->
                    // Preserve theme preferences:
                    // - Dark mode
                    // - Dynamic colors
                    // - Compact view
                    // - Show offline badge

                    // Reset other preferences to defaults
                    prefs.setNotificationsEnabled(true)
                    prefs.setDefaultAnalyticsPeriod("MONTH")
                    prefs.setCurrencySymbol("₹")
                    prefs.setAutoCategorizeEnabled(true)

                    // Reload preferences to update UI
                    loadPreferences()
                }

                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    errorMessage = null
                )

                ErrorHandler.logInfo(
                    "Data cleared: $transactionsDeleted transactions, $overridesRemoved overrides, $metadataCleared metadata",
                    "clearAllData"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Clear all data")
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Clears any error message from the UI state.
     * Should be called after the user has acknowledged the error.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
