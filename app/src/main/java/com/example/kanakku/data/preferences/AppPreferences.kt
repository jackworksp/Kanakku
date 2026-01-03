package com.example.kanakku.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.kanakku.core.error.ErrorHandler
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
 * // Get/set preferences
 * appPrefs.setDarkModeEnabled(true)
 * val isDarkMode = appPrefs.isDarkModeEnabled()
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
        private const val KEY_USE_DYNAMIC_COLORS = "use_dynamic_colors"
        private const val KEY_COMPACT_VIEW = "compact_view"
        private const val KEY_SHOW_OFFLINE_BADGE = "show_offline_badge"
        private const val KEY_AUTO_CATEGORIZE = "auto_categorize"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DEFAULT_ANALYTICS_PERIOD = "default_analytics_period"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_LAST_APP_VERSION = "last_app_version"

        // Sync Metadata Keys
        private const val KEY_INITIAL_SYNC_COMPLETE = "initial_sync_complete"
        private const val KEY_SYNC_TOTAL_COUNT = "sync_total_count"
        private const val KEY_SYNC_PROCESSED_COUNT = "sync_processed_count"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"

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
     * Checks if dark mode is enabled.
     *
     * @return true if dark mode is enabled, false for light mode, null for system default
     */
    fun isDarkModeEnabled(): Boolean? {
        return if (sharedPreferences.contains(KEY_DARK_MODE_ENABLED)) {
            sharedPreferences.getBoolean(KEY_DARK_MODE_ENABLED, false)
        } else {
            null // System default
        }
    }

    /**
     * Sets dark mode preference.
     *
     * @param enabled true for dark mode, false for light mode, null for system default
     */
    fun setDarkModeEnabled(enabled: Boolean?) {
        if (enabled == null) {
            sharedPreferences.edit().remove(KEY_DARK_MODE_ENABLED).apply()
        } else {
            sharedPreferences.edit().putBoolean(KEY_DARK_MODE_ENABLED, enabled).apply()
        }
        Log.d(TAG, "Dark mode set to: $enabled")
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

    /**
     * Checks if SMS notifications are enabled.
     *
     * @return true if notifications are enabled, false otherwise (default: true)
     */
    fun isNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    /**
     * Sets SMS notification preference.
     *
     * @param enabled true to enable notifications, false to disable
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        Log.d(TAG, "Notifications enabled set to: $enabled")
    }

    /**
     * Gets the default analytics period selection.
     *
     * @return Default time period (DAY, WEEK, MONTH, or YEAR) (default: MONTH)
     */
    fun getDefaultAnalyticsPeriod(): String {
        return sharedPreferences.getString(KEY_DEFAULT_ANALYTICS_PERIOD, "MONTH") ?: "MONTH"
    }

    /**
     * Sets the default analytics period selection.
     *
     * @param period Default time period (DAY, WEEK, MONTH, or YEAR)
     */
    fun setDefaultAnalyticsPeriod(period: String) {
        sharedPreferences.edit().putString(KEY_DEFAULT_ANALYTICS_PERIOD, period).apply()
        Log.d(TAG, "Default analytics period set to: $period")
    }

    /**
     * Gets the user's currency symbol preference for display.
     *
     * @return Currency symbol (default: ₹)
     */
    fun getCurrencySymbol(): String {
        return sharedPreferences.getString(KEY_CURRENCY_SYMBOL, "₹") ?: "₹"
    }

    /**
     * Sets the currency symbol preference for display.
     *
     * @param symbol Currency symbol to use in the app
     */
    fun setCurrencySymbol(symbol: String) {
        sharedPreferences.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
        Log.d(TAG, "Currency symbol set to: $symbol")
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
    // Sync Metadata
    // ============================================

    /**
     * Checks if the initial full history sync has been completed.
     * The initial sync reads all available SMS messages on first app launch
     * and processes them into the transaction database.
     *
     * @return true if initial sync is complete, false if it hasn't run yet
     */
    fun isInitialSyncComplete(): Boolean {
        return sharedPreferences.getBoolean(KEY_INITIAL_SYNC_COMPLETE, false)
    }

    /**
     * Marks the initial full history sync as complete.
     * This should be called after the first complete scan of all SMS messages
     * has been successfully processed and stored in the database.
     */
    fun setInitialSyncComplete() {
        sharedPreferences.edit().putBoolean(KEY_INITIAL_SYNC_COMPLETE, true).apply()
        Log.d(TAG, "Initial sync marked as complete")
    }

    /**
     * Resets the initial sync status.
     * Useful for testing or when user explicitly requests a full re-sync.
     * WARNING: This will cause the app to re-scan all SMS messages on next launch.
     */
    fun resetInitialSync() {
        sharedPreferences.edit()
            .putBoolean(KEY_INITIAL_SYNC_COMPLETE, false)
            .remove(KEY_SYNC_TOTAL_COUNT)
            .remove(KEY_SYNC_PROCESSED_COUNT)
            .remove(KEY_LAST_SYNC_TIMESTAMP)
            .apply()
        Log.d(TAG, "Initial sync status reset")
    }

    /**
     * Gets the total number of SMS messages to be processed in the current sync.
     *
     * @return Total SMS count for current sync, or 0 if no sync in progress
     */
    fun getSyncTotalCount(): Int {
        return sharedPreferences.getInt(KEY_SYNC_TOTAL_COUNT, 0)
    }

    /**
     * Sets the total number of SMS messages to be processed in the current sync.
     * This should be set at the start of a sync operation to enable progress tracking.
     *
     * @param count Total number of SMS messages to process
     */
    fun setSyncTotalCount(count: Int) {
        sharedPreferences.edit().putInt(KEY_SYNC_TOTAL_COUNT, count).apply()
        Log.d(TAG, "Sync total count set to: $count")
    }

    /**
     * Gets the number of SMS messages processed so far in the current sync.
     *
     * @return Number of processed SMS messages, or 0 if no sync in progress
     */
    fun getSyncProcessedCount(): Int {
        return sharedPreferences.getInt(KEY_SYNC_PROCESSED_COUNT, 0)
    }

    /**
     * Sets the number of SMS messages processed so far in the current sync.
     * This should be updated periodically during sync to track progress.
     *
     * @param count Number of SMS messages processed
     */
    fun setSyncProcessedCount(count: Int) {
        sharedPreferences.edit().putInt(KEY_SYNC_PROCESSED_COUNT, count).apply()
        Log.d(TAG, "Sync processed count set to: $count")
    }

    /**
     * Increments the sync processed count by one.
     * Convenience method for updating progress after processing each SMS.
     *
     * @return The new processed count after incrementing
     */
    fun incrementSyncProcessedCount(): Int {
        val newCount = getSyncProcessedCount() + 1
        setSyncProcessedCount(newCount)
        return newCount
    }

    /**
     * Gets the timestamp of the last successful sync operation.
     *
     * @return Timestamp in milliseconds, or 0 if never synced
     */
    fun getLastSyncTimestamp(): Long {
        return sharedPreferences.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
    }

    /**
     * Sets the timestamp of the last successful sync operation.
     * Should be called when a sync completes successfully.
     *
     * @param timestamp Timestamp in milliseconds (typically System.currentTimeMillis())
     */
    fun setLastSyncTimestamp(timestamp: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
        Log.d(TAG, "Last sync timestamp set to: $timestamp")
    }

    /**
     * Clears all sync progress tracking.
     * Useful for resetting sync state after completion or cancellation.
     * Does NOT reset the initial sync complete flag.
     */
    fun clearSyncProgress() {
        sharedPreferences.edit()
            .remove(KEY_SYNC_TOTAL_COUNT)
            .remove(KEY_SYNC_PROCESSED_COUNT)
            .apply()
        Log.d(TAG, "Sync progress cleared")
    }

    /**
     * Gets the current sync progress as a percentage.
     *
     * @return Progress percentage (0-100), or 0 if no sync data available
     */
    fun getSyncProgressPercentage(): Int {
        val total = getSyncTotalCount()
        if (total == 0) return 0

        val processed = getSyncProcessedCount()
        return ((processed.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100)
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
