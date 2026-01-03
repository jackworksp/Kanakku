package com.example.kanakku.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.model.BudgetAlertSettings
import com.example.kanakku.data.model.DayOfWeek
import com.example.kanakku.data.model.LargeTransactionSettings
import com.example.kanakku.data.model.WeeklySummarySettings
import com.example.kanakku.data.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Settings screen.
 *
 * Holds the current notification preferences and budget settings.
 *
 * @property budgetAlertSettings Current budget alert notification settings
 * @property largeTransactionSettings Current large transaction alert settings
 * @property weeklySummarySettings Current weekly spending summary settings
 * @property isLoading True if settings are being loaded or updated
 * @property errorMessage Error message to display to user, null if no error
 */
data class SettingsUiState(
    val budgetAlertSettings: BudgetAlertSettings = BudgetAlertSettings(),
    val largeTransactionSettings: LargeTransactionSettings = LargeTransactionSettings(),
    val weeklySummarySettings: WeeklySummarySettings = WeeklySummarySettings(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for the Settings screen.
 *
 * Manages notification preferences and budget settings state, providing:
 * - Loading settings from AppPreferences on initialization
 * - Exposing settings as observable StateFlow for UI
 * - Methods to update individual settings
 * - Automatic persistence of changes to AppPreferences
 * - Comprehensive error handling and logging
 *
 * All settings are persisted immediately when changed via encrypted SharedPreferences.
 * The ViewModel maintains synchronization between UI state and persistent storage.
 *
 * Usage:
 * ```
 * val viewModel = SettingsViewModel()
 * viewModel.initialize(context)
 *
 * // Observe settings in Composable
 * val uiState by viewModel.uiState.collectAsState()
 *
 * // Update settings
 * viewModel.updateBudgetAlertsEnabled(true)
 * viewModel.updateLargeTransactionThreshold(10000.0)
 * ```
 *
 * Error Handling:
 * - All operations use ErrorHandler for consistent logging
 * - Errors during settings load/save are logged and shown to user
 * - Graceful degradation: settings revert to defaults on errors
 */
class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var appPreferences: AppPreferences? = null

    /**
     * Initializes the ViewModel with application context.
     * Loads current settings from AppPreferences.
     *
     * This method should be called once after ViewModel creation,
     * typically in the Settings screen's onCreate or composable initialization.
     *
     * @param context Application context for accessing preferences
     *
     * Error Handling:
     * - AppPreferences initialization failures
     * - Settings load errors (falls back to defaults)
     */
    fun initialize(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Initialize AppPreferences
                appPreferences = AppPreferences.getInstance(context)

                // Load current settings
                loadSettings()

                _uiState.value = _uiState.value.copy(isLoading = false)

                ErrorHandler.logInfo(
                    "Settings loaded successfully",
                    "SettingsViewModel.initialize"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Initialize SettingsViewModel")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Loads settings from AppPreferences and updates UI state.
     *
     * This method reads all notification settings from persistent storage
     * and updates the UI state to reflect the current values.
     *
     * Error Handling:
     * - Falls back to default settings if load fails
     * - Logs errors for debugging
     */
    private fun loadSettings() {
        try {
            val prefs = appPreferences ?: run {
                ErrorHandler.logWarning(
                    "AppPreferences not initialized, using defaults",
                    "loadSettings"
                )
                return
            }

            val budgetSettings = prefs.getBudgetAlertSettings()
            val largeTransactionSettings = prefs.getLargeTransactionSettings()
            val weeklySummarySettings = prefs.getWeeklySummarySettings()

            _uiState.value = _uiState.value.copy(
                budgetAlertSettings = budgetSettings,
                largeTransactionSettings = largeTransactionSettings,
                weeklySummarySettings = weeklySummarySettings
            )

            ErrorHandler.logInfo(
                "Settings loaded: budgetAlerts=${budgetSettings.enabled}, " +
                        "largeTransaction=${largeTransactionSettings.enabled}, " +
                        "weeklySummary=${weeklySummarySettings.enabled}",
                "loadSettings"
            )
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Load settings")
            ErrorHandler.logWarning(
                "Failed to load settings: ${errorInfo.technicalMessage}",
                "loadSettings"
            )
            // Continue with default settings
        }
    }

    // ============================================
    // Budget Alert Settings Updates
    // ============================================

    /**
     * Updates whether budget alerts are enabled.
     *
     * When enabled, the app will send notifications when spending reaches
     * configured budget thresholds (80%, 100%).
     *
     * @param enabled True to enable budget alerts, false to disable
     */
    fun updateBudgetAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.budgetAlertSettings.copy(enabled = enabled)
                appPreferences?.setBudgetAlertsEnabled(enabled)

                _uiState.value = _uiState.value.copy(
                    budgetAlertSettings = updatedSettings,
                    errorMessage = null
                )

                ErrorHandler.logInfo(
                    "Budget alerts enabled set to: $enabled",
                    "updateBudgetAlertsEnabled"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Update budget alerts enabled")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Updates whether 80% budget threshold notification is enabled.
     *
     * When enabled, sends a warning notification when spending reaches
     * 80% of the configured budget limit.
     *
     * @param enabled True to enable 80% threshold alert, false to disable
     */
    fun updateBudgetAlert80Percent(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.budgetAlertSettings.copy(
                    notifyAt80Percent = enabled
                )
                appPreferences?.setBudget80PercentAlertEnabled(enabled)

                _uiState.value = _uiState.value.copy(
                    budgetAlertSettings = updatedSettings,
                    errorMessage = null
                )

                ErrorHandler.logInfo(
                    "Budget 80% alert set to: $enabled",
                    "updateBudgetAlert80Percent"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Update budget 80% alert")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Updates whether 100% budget threshold notification is enabled.
     *
     * When enabled, sends an alert notification when spending reaches
     * 100% of the configured budget limit.
     *
     * @param enabled True to enable 100% threshold alert, false to disable
     */
    fun updateBudgetAlert100Percent(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.budgetAlertSettings.copy(
                    notifyAt100Percent = enabled
                )
                appPreferences?.setBudget100PercentAlertEnabled(enabled)

                _uiState.value = _uiState.value.copy(
                    budgetAlertSettings = updatedSettings,
                    errorMessage = null
                )

                ErrorHandler.logInfo(
                    "Budget 100% alert set to: $enabled",
                    "updateBudgetAlert100Percent"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Update budget 100% alert")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    // ============================================
    // Large Transaction Alert Settings Updates
    // ============================================

    /**
     * Updates whether large transaction alerts are enabled.
     *
     * When enabled, the app will send notifications for transactions
     * exceeding the configured threshold amount.
     *
     * @param enabled True to enable large transaction alerts, false to disable
     */
    fun updateLargeTransactionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.largeTransactionSettings.copy(
                    enabled = enabled
                )
                appPreferences?.setLargeTransactionAlertsEnabled(enabled)

                _uiState.value = _uiState.value.copy(
                    largeTransactionSettings = updatedSettings,
                    errorMessage = null
                )

                ErrorHandler.logInfo(
                    "Large transaction alerts enabled set to: $enabled",
                    "updateLargeTransactionEnabled"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Update large transaction enabled")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Updates the threshold amount for large transaction alerts.
     *
     * Transactions with amount greater than or equal to this threshold
     * will trigger a notification (if large transaction alerts are enabled).
     *
     * @param threshold Threshold amount in currency (must be > 0)
     */
    fun updateLargeTransactionThreshold(threshold: Double) {
        viewModelScope.launch {
            try {
                if (threshold <= 0) {
                    ErrorHandler.logWarning(
                        "Invalid threshold amount: $threshold (must be > 0)",
                        "updateLargeTransactionThreshold"
                    )
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Threshold must be greater than 0"
                    )
                    return@launch
                }

                val updatedSettings = _uiState.value.largeTransactionSettings.copy(
                    threshold = threshold
                )
                appPreferences?.setLargeTransactionThreshold(threshold)

                _uiState.value = _uiState.value.copy(
                    largeTransactionSettings = updatedSettings,
                    errorMessage = null
                )

                ErrorHandler.logInfo(
                    "Large transaction threshold set to: $threshold",
                    "updateLargeTransactionThreshold"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Update large transaction threshold")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    // ============================================
    // Weekly Summary Settings Updates
    // ============================================

    /**
     * Updates whether weekly summary notifications are enabled.
     *
     * When enabled, the app will send a weekly spending summary notification
     * on the configured day and time.
     *
     * @param enabled True to enable weekly summaries, false to disable
     */
    fun updateWeeklySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.weeklySummarySettings.copy(
                    enabled = enabled
                )
                appPreferences?.setWeeklySummaryEnabled(enabled)

                _uiState.value = _uiState.value.copy(
                    weeklySummarySettings = updatedSettings,
                    errorMessage = null
                )

                ErrorHandler.logInfo(
                    "Weekly summary enabled set to: $enabled",
                    "updateWeeklySummaryEnabled"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Update weekly summary enabled")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Updates the day of week for weekly summary notifications.
     *
     * The weekly summary will be sent on this day at the configured time.
     *
     * @param dayOfWeek Day of week for weekly summary (MONDAY to SUNDAY)
     */
    fun updateWeeklySummaryDay(dayOfWeek: DayOfWeek) {
        viewModelScope.launch {
            try {
                val updatedSettings = _uiState.value.weeklySummarySettings.copy(
                    dayOfWeek = dayOfWeek
                )
                appPreferences?.setWeeklySummaryDay(dayOfWeek)

                _uiState.value = _uiState.value.copy(
                    weeklySummarySettings = updatedSettings,
                    errorMessage = null
                )

                ErrorHandler.logInfo(
                    "Weekly summary day set to: ${dayOfWeek.displayName}",
                    "updateWeeklySummaryDay"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Update weekly summary day")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Updates the hour of day for weekly summary notifications.
     *
     * The weekly summary will be sent at this hour (in 24-hour format)
     * on the configured day of week.
     *
     * @param hourOfDay Hour of day in 24-hour format (0-23)
     */
    fun updateWeeklySummaryHour(hourOfDay: Int) {
        viewModelScope.launch {
            try {
                // Validate hour range
                if (hourOfDay !in 0..23) {
                    ErrorHandler.logWarning(
                        "Invalid hour: $hourOfDay (must be 0-23)",
                        "updateWeeklySummaryHour"
                    )
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Hour must be between 0 and 23"
                    )
                    return@launch
                }

                val updatedSettings = _uiState.value.weeklySummarySettings.copy(
                    hourOfDay = hourOfDay
                )
                appPreferences?.setWeeklySummaryHour(hourOfDay)

                _uiState.value = _uiState.value.copy(
                    weeklySummarySettings = updatedSettings,
                    errorMessage = null
                )

                ErrorHandler.logInfo(
                    "Weekly summary hour set to: $hourOfDay",
                    "updateWeeklySummaryHour"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Update weekly summary hour")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    // ============================================
    // Utility Methods
    // ============================================

    /**
     * Clears any error message from the UI state.
     *
     * This should be called after the user has acknowledged the error,
     * such as when dismissing an error dialog or snackbar.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
