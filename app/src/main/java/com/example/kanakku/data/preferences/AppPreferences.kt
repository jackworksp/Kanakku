package com.example.kanakku.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Manages application preferences using EncryptedSharedPreferences for secure storage.
 *
 * This class provides:
 * - Encrypted storage for sensitive user preferences
 * - First launch tracking
 * - UI preferences (theme, display options)
 * - Sync metadata and settings
 * - Reactive theme preference observation via StateFlow
 *
 * All preference data is encrypted at rest using the Android Keystore system through
 * EncryptedSharedPreferences. This ensures that user settings are protected even if
 * the device is compromised.
 *
 * Thread-safety: All methods are thread-safe as they operate on SharedPreferences
 * which handles synchronization internally.
 *
 * Usage:
 * ```
 * val appPrefs = AppPreferences.getInstance(context)
 *
 * // Check first launch
 * if (appPrefs.isFirstLaunch()) {
 *     // Show onboarding
 *     appPrefs.setFirstLaunchComplete()
 * }
 *
 * // Get/set theme mode
 * appPrefs.setThemeMode(ThemeMode.DARK)
 * val themeMode = appPrefs.getThemeMode()
 *
 * // Observe theme changes reactively
 * viewModelScope.launch {
 *     appPrefs.themeModeFlow.collect { themeMode ->
 *         // Update UI based on theme mode
 *     }
 * }
 * ```
 *
 * Fallback Mechanism:
 * If EncryptedSharedPreferences initialization fails (e.g., Keystore issues),
 * the class automatically falls back to regular SharedPreferences with a warning.
 * This ensures the app continues to function even on devices with Keystore problems.
 */
class AppPreferences private constructor(context: Context) {

    companion object {
        private const val TAG = "AppPreferences"
        private const val ENCRYPTED_PREFS_NAME = "kanakku_encrypted_prefs"
        private const val FALLBACK_PREFS_NAME = "kanakku_prefs"

        // Preference Keys
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PRIVACY_DIALOG_SHOWN = "privacy_dialog_shown"
        private const val KEY_DARK_MODE_ENABLED = "dark_mode_enabled"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_USE_DYNAMIC_COLORS = "use_dynamic_colors"
        private const val KEY_COMPACT_VIEW = "compact_view"
        private const val KEY_SHOW_OFFLINE_BADGE = "show_offline_badge"
        private const val KEY_AUTO_CATEGORIZE = "auto_categorize"
        private const val KEY_LAST_APP_VERSION = "last_app_version"

        @Volatile
        private var instance: AppPreferences? = null

        /**
         * Gets the singleton instance of AppPreferences.
         *
         * This method is thread-safe and uses double-checked locking to ensure
         * only one instance is created even in multi-threaded scenarios.
         *
         * @param context Application context
         * @return The singleton AppPreferences instance
         */
        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Resets the singleton instance. Used primarily for testing.
         * WARNING: This should not be called in production code.
         */
        internal fun resetInstance() {
            synchronized(this) {
                instance = null
            }
        }
    }

    private val sharedPreferences: SharedPreferences = initializePreferences(context.applicationContext)
    private var isUsingEncryption: Boolean = true

    // ==================== Reactive Theme Preference ====================

    /**
     * Private mutable state flow for theme mode changes.
     * Updated whenever theme preference changes.
     */
    private val _themeModeFlow = MutableStateFlow(loadThemeMode())

    /**
     * Reactive flow of current theme mode.
     * Emits the current theme mode and all subsequent changes.
     * Subscribers will be notified immediately when theme preference changes.
     *
     * Usage:
     * ```
     * viewModelScope.launch {
     *     appPrefs.themeModeFlow.collect { themeMode ->
     *         // Update UI based on theme mode
     *     }
     * }
     * ```
     */
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    /**
     * Listener for SharedPreferences changes to keep StateFlow in sync.
     * Automatically updates the theme mode flow when preferences change externally.
     */
    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_THEME_MODE || key == KEY_DARK_MODE_ENABLED) {
            val newThemeMode = loadThemeMode()
            _themeModeFlow.value = newThemeMode
            Log.d(TAG, "Theme mode changed to: $newThemeMode")
        }
    }

    init {
        // Register listener to keep StateFlow synchronized with preference changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    /**
     * Initializes SharedPreferences with encryption if possible, falls back to regular prefs if not.
     *
     * @param context Application context
     * @return SharedPreferences instance (encrypted or regular)
     */
    private fun initializePreferences(context: Context): SharedPreferences {
        return try {
            // Create or retrieve the master key for encryption
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            // Create encrypted shared preferences
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also {
                isUsingEncryption = true
                Log.i(TAG, "Successfully initialized EncryptedSharedPreferences")
            }

        } catch (e: GeneralSecurityException) {
            // Keystore/security error - fall back to regular SharedPreferences
            ErrorHandler.handleError(e, "Initialize EncryptedSharedPreferences")
            Log.w(TAG, "Failed to initialize EncryptedSharedPreferences, falling back to regular SharedPreferences", e)
            isUsingEncryption = false
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)

        } catch (e: IOException) {
            // File I/O error - fall back to regular SharedPreferences
            ErrorHandler.handleError(e, "Initialize EncryptedSharedPreferences")
            Log.w(TAG, "I/O error during EncryptedSharedPreferences initialization, falling back to regular SharedPreferences", e)
            isUsingEncryption = false
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)

        } catch (e: Exception) {
            // Unexpected error - fall back to regular SharedPreferences
            ErrorHandler.handleError(e, "Initialize EncryptedSharedPreferences")
            Log.e(TAG, "Unexpected error during EncryptedSharedPreferences initialization, falling back to regular SharedPreferences", e)
            isUsingEncryption = false
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Checks if preferences are being stored with encryption.
     *
     * @return true if using EncryptedSharedPreferences, false if using fallback
     */
    fun isUsingEncryption(): Boolean = isUsingEncryption

    // ============================================
    // First Launch and Onboarding
    // ============================================

    /**
     * Checks if this is the first time the app is being launched.
     *
     * @return true if this is the first launch, false otherwise
     */
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * Marks the first launch as complete.
     * This should be called after the user has completed onboarding.
     */
    fun setFirstLaunchComplete() {
        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        Log.d(TAG, "First launch marked as complete")
    }

    /**
     * Checks if the privacy dialog has been shown to the user.
     *
     * @return true if the privacy dialog has been shown, false otherwise
     */
    fun isPrivacyDialogShown(): Boolean {
        return sharedPreferences.getBoolean(KEY_PRIVACY_DIALOG_SHOWN, false)
    }

    /**
     * Marks the privacy dialog as shown.
     * This should be called when the user dismisses the privacy information dialog.
     */
    fun setPrivacyDialogShown() {
        sharedPreferences.edit().putBoolean(KEY_PRIVACY_DIALOG_SHOWN, true).apply()
        Log.d(TAG, "Privacy dialog marked as shown")
    }

    // ============================================
    // UI Preferences
    // ============================================

    /**
     * Loads the current theme mode from SharedPreferences.
     * Handles migration from old Boolean-based dark mode setting to new ThemeMode enum.
     *
     * @return Current ThemeMode (LIGHT, DARK, or SYSTEM)
     */
    private fun loadThemeMode(): ThemeMode {
        // First check for new KEY_THEME_MODE preference
        val themeModeString = sharedPreferences.getString(KEY_THEME_MODE, null)
        if (themeModeString != null) {
            return try {
                ThemeMode.valueOf(themeModeString)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid theme mode value: $themeModeString, defaulting to SYSTEM")
                ThemeMode.SYSTEM
            }
        }

        // Fallback: migrate from old KEY_DARK_MODE_ENABLED preference
        return if (sharedPreferences.contains(KEY_DARK_MODE_ENABLED)) {
            val isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE_ENABLED, false)
            if (isDarkMode) ThemeMode.DARK else ThemeMode.LIGHT
        } else {
            ThemeMode.SYSTEM // Default to system if no preference is set
        }
    }

    /**
     * Gets the current theme mode.
     *
     * @return Current ThemeMode (LIGHT, DARK, or SYSTEM)
     */
    fun getThemeMode(): ThemeMode {
        return loadThemeMode()
    }

    /**
     * Sets the theme mode preference and updates the reactive flow.
     *
     * @param themeMode The theme mode to set (LIGHT, DARK, or SYSTEM)
     */
    fun setThemeMode(themeMode: ThemeMode) {
        sharedPreferences.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        _themeModeFlow.value = themeMode
        Log.d(TAG, "Theme mode set to: ${themeMode.displayName}")
    }

    /**
     * Checks if dark mode is enabled.
     * DEPRECATED: Use getThemeMode() instead for better type safety.
     *
     * @return true if dark mode is enabled, false for light mode, null for system default
     */
    @Deprecated(
        message = "Use getThemeMode() instead",
        replaceWith = ReplaceWith("getThemeMode()"),
        level = DeprecationLevel.WARNING
    )
    fun isDarkModeEnabled(): Boolean? {
        return when (loadThemeMode()) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> null
        }
    }

    /**
     * Sets dark mode preference.
     * DEPRECATED: Use setThemeMode() instead for better type safety.
     *
     * @param enabled true for dark mode, false for light mode, null for system default
     */
    @Deprecated(
        message = "Use setThemeMode() instead",
        replaceWith = ReplaceWith("setThemeMode(when(enabled) { true -> ThemeMode.DARK; false -> ThemeMode.LIGHT; null -> ThemeMode.SYSTEM })"),
        level = DeprecationLevel.WARNING
    )
    fun setDarkModeEnabled(enabled: Boolean?) {
        val themeMode = when (enabled) {
            true -> ThemeMode.DARK
            false -> ThemeMode.LIGHT
            null -> ThemeMode.SYSTEM
        }
        setThemeMode(themeMode)
    }

    /**
     * Checks if dynamic colors (Material You) are enabled.
     *
     * @return true if dynamic colors are enabled, false otherwise (default: true)
     */
    fun isDynamicColorsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_USE_DYNAMIC_COLORS, true)
    }

    /**
     * Sets dynamic colors (Material You) preference.
     *
     * @param enabled true to enable dynamic colors, false to use fixed theme colors
     */
    fun setDynamicColorsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_USE_DYNAMIC_COLORS, enabled).apply()
        Log.d(TAG, "Dynamic colors set to: $enabled")
    }

    /**
     * Checks if compact view is enabled for transaction lists.
     *
     * @return true if compact view is enabled, false for normal view (default: false)
     */
    fun isCompactViewEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_COMPACT_VIEW, false)
    }

    /**
     * Sets compact view preference for transaction lists.
     *
     * @param enabled true for compact view, false for normal view
     */
    fun setCompactViewEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_COMPACT_VIEW, enabled).apply()
        Log.d(TAG, "Compact view set to: $enabled")
    }

    /**
     * Checks if the offline badge should be shown in the UI.
     *
     * @return true if offline badge should be shown, false otherwise (default: true)
     */
    fun shouldShowOfflineBadge(): Boolean {
        return sharedPreferences.getBoolean(KEY_SHOW_OFFLINE_BADGE, true)
    }

    /**
     * Sets whether to show the offline badge in the UI.
     *
     * @param show true to show offline badge, false to hide it
     */
    fun setShowOfflineBadge(show: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SHOW_OFFLINE_BADGE, show).apply()
        Log.d(TAG, "Show offline badge set to: $show")
    }

    // ============================================
    // Feature Preferences
    // ============================================

    /**
     * Checks if automatic categorization is enabled.
     *
     * @return true if auto-categorize is enabled, false otherwise (default: true)
     */
    fun isAutoCategorizeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_CATEGORIZE, true)
    }

    /**
     * Sets automatic categorization preference.
     *
     * @param enabled true to enable auto-categorize, false to disable
     */
    fun setAutoCategorizeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_CATEGORIZE, enabled).apply()
        Log.d(TAG, "Auto-categorize set to: $enabled")
    }

    // ============================================
    // App Metadata
    // ============================================

    /**
     * Gets the last app version that was run.
     * Useful for detecting version upgrades and running migrations.
     *
     * @return Last app version code, or 0 if never set
     */
    fun getLastAppVersion(): Int {
        return sharedPreferences.getInt(KEY_LAST_APP_VERSION, 0)
    }

    /**
     * Sets the current app version.
     * Should be called on app startup after version-specific migrations are complete.
     *
     * @param versionCode Current app version code
     */
    fun setLastAppVersion(versionCode: Int) {
        sharedPreferences.edit().putInt(KEY_LAST_APP_VERSION, versionCode).apply()
        Log.d(TAG, "Last app version set to: $versionCode")
    }

    // ============================================
    // Utility Methods
    // ============================================

    /**
     * Gets a custom string preference.
     * Useful for storing additional settings not covered by the predefined methods.
     *
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return The preference value or default
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    /**
     * Sets a custom string preference.
     *
     * @param key Preference key
     * @param value Value to store
     */
    fun setString(key: String, value: String?) {
        if (value == null) {
            sharedPreferences.edit().remove(key).apply()
        } else {
            sharedPreferences.edit().putString(key, value).apply()
        }
    }

    /**
     * Gets a custom integer preference.
     *
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return The preference value or default
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    /**
     * Sets a custom integer preference.
     *
     * @param key Preference key
     * @param value Value to store
     */
    fun setInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    /**
     * Gets a custom boolean preference.
     *
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return The preference value or default
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    /**
     * Sets a custom boolean preference.
     *
     * @param key Preference key
     * @param value Value to store
     */
    fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    /**
     * Gets a custom long preference.
     *
     * @param key Preference key
     * @param defaultValue Default value if key doesn't exist
     * @return The preference value or default
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    /**
     * Sets a custom long preference.
     *
     * @param key Preference key
     * @param value Value to store
     */
    fun setLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    /**
     * Checks if a preference key exists.
     *
     * @param key Preference key to check
     * @return true if the key exists, false otherwise
     */
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    /**
     * Removes a specific preference.
     *
     * @param key Preference key to remove
     */
    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
        Log.d(TAG, "Removed preference: $key")
    }

    /**
     * Clears all preferences.
     * WARNING: This will reset all app settings to defaults.
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
        Log.w(TAG, "All preferences cleared")
    }

    /**
     * Gets all preference keys.
     * Useful for debugging or backup/restore scenarios.
     *
     * @return Set of all preference keys
     */
    fun getAllKeys(): Set<String> {
        return sharedPreferences.all.keys
    }

    /**
     * Registers a listener for preference changes.
     *
     * @param listener The listener to register
     */
    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Unregisters a preference change listener.
     *
     * @param listener The listener to unregister
     */
    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
