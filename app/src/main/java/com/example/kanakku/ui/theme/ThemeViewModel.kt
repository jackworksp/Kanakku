package com.example.kanakku.ui.theme

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.model.ThemeMode
import com.example.kanakku.data.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing theme state and user theme preferences.
 *
 * This ViewModel handles:
 * - Exposing current theme mode as a reactive StateFlow
 * - Changing theme mode with automatic persistence
 * - Synchronizing theme changes across the app
 *
 * Usage:
 * ```
 * val themeViewModel: ThemeViewModel = viewModel()
 *
 * // Observe theme changes
 * val themeMode by themeViewModel.themeMode.collectAsState()
 *
 * // Change theme
 * themeViewModel.setThemeMode(ThemeMode.DARK)
 * ```
 *
 * The theme preference is persisted using AppPreferences and survives app restarts.
 */
class ThemeViewModel : ViewModel() {

    private var appPreferences: AppPreferences? = null

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    /**
     * Current theme mode as a StateFlow.
     * Emits the current theme mode and all subsequent changes.
     *
     * Possible values:
     * - ThemeMode.LIGHT: Always use light theme
     * - ThemeMode.DARK: Always use dark theme
     * - ThemeMode.SYSTEM: Follow system theme setting
     */
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    /**
     * Error message from theme operations.
     * Null if no error has occurred.
     */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Initializes the ViewModel with application context.
     * Must be called before using the ViewModel.
     *
     * This method:
     * 1. Initializes AppPreferences
     * 2. Loads the saved theme preference
     * 3. Observes theme preference changes from AppPreferences
     *
     * @param context Application context
     */
    fun initialize(context: Context) {
        viewModelScope.launch {
            try {
                appPreferences = AppPreferences.getInstance(context.applicationContext)

                // Load initial theme mode from preferences
                val savedThemeMode = appPreferences?.getThemeMode() ?: ThemeMode.SYSTEM
                _themeMode.value = savedThemeMode

                // Observe theme changes from AppPreferences
                // This ensures the ViewModel stays in sync if preferences are changed elsewhere
                appPreferences?.themeModeFlow?.collect { themeMode ->
                    _themeMode.value = themeMode
                    ErrorHandler.logInfo(
                        "Theme mode updated to: ${themeMode.displayName}",
                        "ThemeViewModel"
                    )
                }
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Initialize ThemeViewModel")
                _errorMessage.value = errorInfo.userMessage
                ErrorHandler.logWarning(
                    "Failed to initialize ThemeViewModel: ${errorInfo.technicalMessage}",
                    "ThemeViewModel.initialize"
                )
            }
        }
    }

    /**
     * Sets the theme mode and persists it to preferences.
     *
     * The change is:
     * 1. Saved to AppPreferences (encrypted storage)
     * 2. Emitted through the themeMode StateFlow
     * 3. Applied immediately to the UI
     *
     * @param mode The theme mode to set (LIGHT, DARK, or SYSTEM)
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            try {
                // Save to preferences (this will also trigger the flow update)
                appPreferences?.setThemeMode(mode)

                // Update local state immediately for responsive UI
                _themeMode.value = mode

                // Clear any previous error
                _errorMessage.value = null

                ErrorHandler.logInfo(
                    "Theme mode set to: ${mode.displayName}",
                    "ThemeViewModel.setThemeMode"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "Set theme mode")
                _errorMessage.value = errorInfo.userMessage
                ErrorHandler.logWarning(
                    "Failed to set theme mode: ${errorInfo.technicalMessage}",
                    "ThemeViewModel.setThemeMode"
                )
            }
        }
    }

    /**
     * Clears any error message.
     * Should be called after the user has acknowledged the error.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
