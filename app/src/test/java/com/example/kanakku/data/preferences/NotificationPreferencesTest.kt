package com.example.kanakku.data.preferences

import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.model.BudgetAlertSettings
import com.example.kanakku.data.model.DayOfWeek
import com.example.kanakku.data.model.LargeTransactionSettings
import com.example.kanakku.data.model.NotificationSettings
import com.example.kanakku.data.model.WeeklySummarySettings
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for AppPreferences notification-related methods.
 *
 * Tests cover:
 * - Budget alert settings (get/set, enabled flags, threshold toggles)
 * - Large transaction settings (get/set, enabled flag, threshold amount)
 * - Weekly summary settings (get/set, enabled flag, day/time configuration)
 * - Complete notification settings (get/set all at once)
 * - Default values for all notification preferences
 * - Edge cases (boundary values, invalid inputs, state preservation)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationPreferencesTest {

    private lateinit var appPreferences: AppPreferences

    @Before
    fun setup() {
        // Reset singleton before each test
        AppPreferences.resetInstance()

        // Create fresh instance for testing
        appPreferences = AppPreferences.getInstance(ApplicationProvider.getApplicationContext())

        // Clear all preferences to start with clean slate
        appPreferences.clearAll()
    }

    @After
    fun teardown() {
        // Clean up after each test
        appPreferences.clearAll()
        AppPreferences.resetInstance()
    }

    // ==================== Budget Alert Settings Tests ====================

    @Test
    fun getBudgetAlertSettings_returnsDefaultValues() {
        // When
        val settings = appPreferences.getBudgetAlertSettings()

        // Then - Verify defaults: enabled=true, 80%=true, 100%=true
        assertTrue(settings.enabled)
        assertTrue(settings.notifyAt80Percent)
        assertTrue(settings.notifyAt100Percent)
    }

    @Test
    fun setBudgetAlertSettings_storesAndRetrievesCorrectly() {
        // Given
        val settings = BudgetAlertSettings(
            enabled = false,
            notifyAt80Percent = false,
            notifyAt100Percent = true
        )

        // When
        appPreferences.setBudgetAlertSettings(settings)
        val retrieved = appPreferences.getBudgetAlertSettings()

        // Then
        assertEquals(settings.enabled, retrieved.enabled)
        assertEquals(settings.notifyAt80Percent, retrieved.notifyAt80Percent)
        assertEquals(settings.notifyAt100Percent, retrieved.notifyAt100Percent)
    }

    @Test
    fun setBudgetAlertSettings_withAllEnabled_storesCorrectly() {
        // Given
        val settings = BudgetAlertSettings(
            enabled = true,
            notifyAt80Percent = true,
            notifyAt100Percent = true
        )

        // When
        appPreferences.setBudgetAlertSettings(settings)
        val retrieved = appPreferences.getBudgetAlertSettings()

        // Then
        assertTrue(retrieved.enabled)
        assertTrue(retrieved.notifyAt80Percent)
        assertTrue(retrieved.notifyAt100Percent)
    }

    @Test
    fun setBudgetAlertSettings_withAllDisabled_storesCorrectly() {
        // Given
        val settings = BudgetAlertSettings(
            enabled = false,
            notifyAt80Percent = false,
            notifyAt100Percent = false
        )

        // When
        appPreferences.setBudgetAlertSettings(settings)
        val retrieved = appPreferences.getBudgetAlertSettings()

        // Then
        assertFalse(retrieved.enabled)
        assertFalse(retrieved.notifyAt80Percent)
        assertFalse(retrieved.notifyAt100Percent)
    }

    @Test
    fun isBudgetAlertsEnabled_returnsDefaultTrue() {
        // When
        val enabled = appPreferences.isBudgetAlertsEnabled()

        // Then
        assertTrue(enabled)
    }

    @Test
    fun setBudgetAlertsEnabled_updatesCorrectly() {
        // When
        appPreferences.setBudgetAlertsEnabled(false)

        // Then
        assertFalse(appPreferences.isBudgetAlertsEnabled())

        // When
        appPreferences.setBudgetAlertsEnabled(true)

        // Then
        assertTrue(appPreferences.isBudgetAlertsEnabled())
    }

    @Test
    fun isBudget80PercentAlertEnabled_returnsDefaultTrue() {
        // When
        val enabled = appPreferences.isBudget80PercentAlertEnabled()

        // Then
        assertTrue(enabled)
    }

    @Test
    fun setBudget80PercentAlertEnabled_updatesCorrectly() {
        // When
        appPreferences.setBudget80PercentAlertEnabled(false)

        // Then
        assertFalse(appPreferences.isBudget80PercentAlertEnabled())

        // When
        appPreferences.setBudget80PercentAlertEnabled(true)

        // Then
        assertTrue(appPreferences.isBudget80PercentAlertEnabled())
    }

    @Test
    fun isBudget100PercentAlertEnabled_returnsDefaultTrue() {
        // When
        val enabled = appPreferences.isBudget100PercentAlertEnabled()

        // Then
        assertTrue(enabled)
    }

    @Test
    fun setBudget100PercentAlertEnabled_updatesCorrectly() {
        // When
        appPreferences.setBudget100PercentAlertEnabled(false)

        // Then
        assertFalse(appPreferences.isBudget100PercentAlertEnabled())

        // When
        appPreferences.setBudget100PercentAlertEnabled(true)

        // Then
        assertTrue(appPreferences.isBudget100PercentAlertEnabled())
    }

    @Test
    fun budgetAlertSettings_individualSettersMatchBatchSetter() {
        // Given
        val batchSettings = BudgetAlertSettings(
            enabled = false,
            notifyAt80Percent = true,
            notifyAt100Percent = false
        )

        // When - Set using batch setter
        appPreferences.setBudgetAlertSettings(batchSettings)
        val retrieved1 = appPreferences.getBudgetAlertSettings()

        // Clear and set using individual setters
        appPreferences.clearAll()
        appPreferences.setBudgetAlertsEnabled(false)
        appPreferences.setBudget80PercentAlertEnabled(true)
        appPreferences.setBudget100PercentAlertEnabled(false)
        val retrieved2 = appPreferences.getBudgetAlertSettings()

        // Then - Both approaches should produce identical results
        assertEquals(retrieved1.enabled, retrieved2.enabled)
        assertEquals(retrieved1.notifyAt80Percent, retrieved2.notifyAt80Percent)
        assertEquals(retrieved1.notifyAt100Percent, retrieved2.notifyAt100Percent)
    }

    // ==================== Large Transaction Settings Tests ====================

    @Test
    fun getLargeTransactionSettings_returnsDefaultValues() {
        // When
        val settings = appPreferences.getLargeTransactionSettings()

        // Then - Verify defaults: enabled=true, threshold=5000.0
        assertTrue(settings.enabled)
        assertEquals(5000.0, settings.threshold, 0.001)
    }

    @Test
    fun setLargeTransactionSettings_storesAndRetrievesCorrectly() {
        // Given
        val settings = LargeTransactionSettings(
            enabled = false,
            threshold = 10000.0
        )

        // When
        appPreferences.setLargeTransactionSettings(settings)
        val retrieved = appPreferences.getLargeTransactionSettings()

        // Then
        assertEquals(settings.enabled, retrieved.enabled)
        assertEquals(settings.threshold, retrieved.threshold, 0.001)
    }

    @Test
    fun setLargeTransactionSettings_withCustomThreshold_storesCorrectly() {
        // Given
        val settings = LargeTransactionSettings(
            enabled = true,
            threshold = 25000.0
        )

        // When
        appPreferences.setLargeTransactionSettings(settings)
        val retrieved = appPreferences.getLargeTransactionSettings()

        // Then
        assertTrue(retrieved.enabled)
        assertEquals(25000.0, retrieved.threshold, 0.001)
    }

    @Test
    fun isLargeTransactionAlertsEnabled_returnsDefaultTrue() {
        // When
        val enabled = appPreferences.isLargeTransactionAlertsEnabled()

        // Then
        assertTrue(enabled)
    }

    @Test
    fun setLargeTransactionAlertsEnabled_updatesCorrectly() {
        // When
        appPreferences.setLargeTransactionAlertsEnabled(false)

        // Then
        assertFalse(appPreferences.isLargeTransactionAlertsEnabled())

        // When
        appPreferences.setLargeTransactionAlertsEnabled(true)

        // Then
        assertTrue(appPreferences.isLargeTransactionAlertsEnabled())
    }

    @Test
    fun getLargeTransactionThreshold_returnsDefault5000() {
        // When
        val threshold = appPreferences.getLargeTransactionThreshold()

        // Then
        assertEquals(5000.0, threshold, 0.001)
    }

    @Test
    fun setLargeTransactionThreshold_updatesCorrectly() {
        // When
        appPreferences.setLargeTransactionThreshold(15000.0)

        // Then
        assertEquals(15000.0, appPreferences.getLargeTransactionThreshold(), 0.001)
    }

    @Test
    fun setLargeTransactionThreshold_withSmallAmount() {
        // When
        appPreferences.setLargeTransactionThreshold(100.0)

        // Then
        assertEquals(100.0, appPreferences.getLargeTransactionThreshold(), 0.001)
    }

    @Test
    fun setLargeTransactionThreshold_withLargeAmount() {
        // When
        appPreferences.setLargeTransactionThreshold(1000000.0)

        // Then
        assertEquals(1000000.0, appPreferences.getLargeTransactionThreshold(), 0.001)
    }

    @Test
    fun setLargeTransactionThreshold_withDecimalAmount() {
        // When
        appPreferences.setLargeTransactionThreshold(7499.99)

        // Then
        assertEquals(7499.99, appPreferences.getLargeTransactionThreshold(), 0.001)
    }

    @Test
    fun setLargeTransactionThreshold_withZero() {
        // When
        appPreferences.setLargeTransactionThreshold(0.0)

        // Then
        assertEquals(0.0, appPreferences.getLargeTransactionThreshold(), 0.001)
    }

    @Test
    fun largeTransactionSettings_individualSettersMatchBatchSetter() {
        // Given
        val batchSettings = LargeTransactionSettings(
            enabled = false,
            threshold = 8000.0
        )

        // When - Set using batch setter
        appPreferences.setLargeTransactionSettings(batchSettings)
        val retrieved1 = appPreferences.getLargeTransactionSettings()

        // Clear and set using individual setters
        appPreferences.clearAll()
        appPreferences.setLargeTransactionAlertsEnabled(false)
        appPreferences.setLargeTransactionThreshold(8000.0)
        val retrieved2 = appPreferences.getLargeTransactionSettings()

        // Then - Both approaches should produce identical results
        assertEquals(retrieved1.enabled, retrieved2.enabled)
        assertEquals(retrieved1.threshold, retrieved2.threshold, 0.001)
    }

    // ==================== Weekly Summary Settings Tests ====================

    @Test
    fun getWeeklySummarySettings_returnsDefaultValues() {
        // When
        val settings = appPreferences.getWeeklySummarySettings()

        // Then - Verify defaults: enabled=false, day=MONDAY, hour=9
        assertFalse(settings.enabled)
        assertEquals(DayOfWeek.MONDAY, settings.dayOfWeek)
        assertEquals(9, settings.hourOfDay)
    }

    @Test
    fun setWeeklySummarySettings_storesAndRetrievesCorrectly() {
        // Given
        val settings = WeeklySummarySettings(
            enabled = true,
            dayOfWeek = DayOfWeek.FRIDAY,
            hourOfDay = 18
        )

        // When
        appPreferences.setWeeklySummarySettings(settings)
        val retrieved = appPreferences.getWeeklySummarySettings()

        // Then
        assertEquals(settings.enabled, retrieved.enabled)
        assertEquals(settings.dayOfWeek, retrieved.dayOfWeek)
        assertEquals(settings.hourOfDay, retrieved.hourOfDay)
    }

    @Test
    fun setWeeklySummarySettings_withDifferentDays() {
        // Test all days of the week
        DayOfWeek.entries.forEach { day ->
            // Given
            val settings = WeeklySummarySettings(
                enabled = true,
                dayOfWeek = day,
                hourOfDay = 12
            )

            // When
            appPreferences.setWeeklySummarySettings(settings)
            val retrieved = appPreferences.getWeeklySummarySettings()

            // Then
            assertEquals(day, retrieved.dayOfWeek)
        }
    }

    @Test
    fun isWeeklySummaryEnabled_returnsDefaultFalse() {
        // When
        val enabled = appPreferences.isWeeklySummaryEnabled()

        // Then
        assertFalse(enabled)
    }

    @Test
    fun setWeeklySummaryEnabled_updatesCorrectly() {
        // When
        appPreferences.setWeeklySummaryEnabled(true)

        // Then
        assertTrue(appPreferences.isWeeklySummaryEnabled())

        // When
        appPreferences.setWeeklySummaryEnabled(false)

        // Then
        assertFalse(appPreferences.isWeeklySummaryEnabled())
    }

    @Test
    fun getWeeklySummaryDay_returnsDefaultMonday() {
        // When
        val day = appPreferences.getWeeklySummaryDay()

        // Then
        assertEquals(DayOfWeek.MONDAY, day)
    }

    @Test
    fun setWeeklySummaryDay_updatesCorrectly() {
        // When
        appPreferences.setWeeklySummaryDay(DayOfWeek.SATURDAY)

        // Then
        assertEquals(DayOfWeek.SATURDAY, appPreferences.getWeeklySummaryDay())
    }

    @Test
    fun setWeeklySummaryDay_testAllDays() {
        // Test setting each day of the week
        val days = listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )

        days.forEach { expectedDay ->
            // When
            appPreferences.setWeeklySummaryDay(expectedDay)

            // Then
            assertEquals(expectedDay, appPreferences.getWeeklySummaryDay())
        }
    }

    @Test
    fun getWeeklySummaryHour_returnsDefault9() {
        // When
        val hour = appPreferences.getWeeklySummaryHour()

        // Then
        assertEquals(9, hour)
    }

    @Test
    fun setWeeklySummaryHour_updatesCorrectly() {
        // When
        appPreferences.setWeeklySummaryHour(15)

        // Then
        assertEquals(15, appPreferences.getWeeklySummaryHour())
    }

    @Test
    fun setWeeklySummaryHour_withMinimumValue() {
        // When
        appPreferences.setWeeklySummaryHour(0)

        // Then
        assertEquals(0, appPreferences.getWeeklySummaryHour())
    }

    @Test
    fun setWeeklySummaryHour_withMaximumValue() {
        // When
        appPreferences.setWeeklySummaryHour(23)

        // Then
        assertEquals(23, appPreferences.getWeeklySummaryHour())
    }

    @Test
    fun setWeeklySummaryHour_clampsNegativeValue() {
        // When - Set negative value
        appPreferences.setWeeklySummaryHour(-5)

        // Then - Should clamp to 0
        assertEquals(0, appPreferences.getWeeklySummaryHour())
    }

    @Test
    fun setWeeklySummaryHour_clampsValueAbove23() {
        // When - Set value above 23
        appPreferences.setWeeklySummaryHour(25)

        // Then - Should clamp to 23
        assertEquals(23, appPreferences.getWeeklySummaryHour())
    }

    @Test
    fun setWeeklySummaryHour_testAllValidHours() {
        // Test all valid hours (0-23)
        for (hour in 0..23) {
            // When
            appPreferences.setWeeklySummaryHour(hour)

            // Then
            assertEquals(hour, appPreferences.getWeeklySummaryHour())
        }
    }

    @Test
    fun weeklySummarySettings_individualSettersMatchBatchSetter() {
        // Given
        val batchSettings = WeeklySummarySettings(
            enabled = true,
            dayOfWeek = DayOfWeek.WEDNESDAY,
            hourOfDay = 14
        )

        // When - Set using batch setter
        appPreferences.setWeeklySummarySettings(batchSettings)
        val retrieved1 = appPreferences.getWeeklySummarySettings()

        // Clear and set using individual setters
        appPreferences.clearAll()
        appPreferences.setWeeklySummaryEnabled(true)
        appPreferences.setWeeklySummaryDay(DayOfWeek.WEDNESDAY)
        appPreferences.setWeeklySummaryHour(14)
        val retrieved2 = appPreferences.getWeeklySummarySettings()

        // Then - Both approaches should produce identical results
        assertEquals(retrieved1.enabled, retrieved2.enabled)
        assertEquals(retrieved1.dayOfWeek, retrieved2.dayOfWeek)
        assertEquals(retrieved1.hourOfDay, retrieved2.hourOfDay)
    }

    // ==================== Complete Notification Settings Tests ====================

    @Test
    fun getNotificationSettings_returnsDefaultValues() {
        // When
        val settings = appPreferences.getNotificationSettings()

        // Then - Verify all defaults
        assertTrue(settings.budgetAlerts.enabled)
        assertTrue(settings.budgetAlerts.notifyAt80Percent)
        assertTrue(settings.budgetAlerts.notifyAt100Percent)
        assertTrue(settings.largeTransactions.enabled)
        assertEquals(5000.0, settings.largeTransactions.threshold, 0.001)
        assertFalse(settings.weeklySummary.enabled)
        assertEquals(DayOfWeek.MONDAY, settings.weeklySummary.dayOfWeek)
        assertEquals(9, settings.weeklySummary.hourOfDay)
    }

    @Test
    fun setNotificationSettings_storesAndRetrievesCorrectly() {
        // Given
        val settings = NotificationSettings(
            budgetAlerts = BudgetAlertSettings(
                enabled = false,
                notifyAt80Percent = true,
                notifyAt100Percent = false
            ),
            largeTransactions = LargeTransactionSettings(
                enabled = true,
                threshold = 12000.0
            ),
            weeklySummary = WeeklySummarySettings(
                enabled = true,
                dayOfWeek = DayOfWeek.SUNDAY,
                hourOfDay = 20
            )
        )

        // When
        appPreferences.setNotificationSettings(settings)
        val retrieved = appPreferences.getNotificationSettings()

        // Then - Verify budget alerts
        assertEquals(settings.budgetAlerts.enabled, retrieved.budgetAlerts.enabled)
        assertEquals(settings.budgetAlerts.notifyAt80Percent, retrieved.budgetAlerts.notifyAt80Percent)
        assertEquals(settings.budgetAlerts.notifyAt100Percent, retrieved.budgetAlerts.notifyAt100Percent)

        // Verify large transactions
        assertEquals(settings.largeTransactions.enabled, retrieved.largeTransactions.enabled)
        assertEquals(settings.largeTransactions.threshold, retrieved.largeTransactions.threshold, 0.001)

        // Verify weekly summary
        assertEquals(settings.weeklySummary.enabled, retrieved.weeklySummary.enabled)
        assertEquals(settings.weeklySummary.dayOfWeek, retrieved.weeklySummary.dayOfWeek)
        assertEquals(settings.weeklySummary.hourOfDay, retrieved.weeklySummary.hourOfDay)
    }

    @Test
    fun setNotificationSettings_withAllEnabled() {
        // Given
        val settings = NotificationSettings(
            budgetAlerts = BudgetAlertSettings(
                enabled = true,
                notifyAt80Percent = true,
                notifyAt100Percent = true
            ),
            largeTransactions = LargeTransactionSettings(
                enabled = true,
                threshold = 5000.0
            ),
            weeklySummary = WeeklySummarySettings(
                enabled = true,
                dayOfWeek = DayOfWeek.MONDAY,
                hourOfDay = 9
            )
        )

        // When
        appPreferences.setNotificationSettings(settings)
        val retrieved = appPreferences.getNotificationSettings()

        // Then - All should be enabled
        assertTrue(retrieved.budgetAlerts.enabled)
        assertTrue(retrieved.largeTransactions.enabled)
        assertTrue(retrieved.weeklySummary.enabled)
    }

    @Test
    fun setNotificationSettings_withAllDisabled() {
        // Given
        val settings = NotificationSettings(
            budgetAlerts = BudgetAlertSettings(
                enabled = false,
                notifyAt80Percent = false,
                notifyAt100Percent = false
            ),
            largeTransactions = LargeTransactionSettings(
                enabled = false,
                threshold = 5000.0
            ),
            weeklySummary = WeeklySummarySettings(
                enabled = false,
                dayOfWeek = DayOfWeek.MONDAY,
                hourOfDay = 9
            )
        )

        // When
        appPreferences.setNotificationSettings(settings)
        val retrieved = appPreferences.getNotificationSettings()

        // Then - All should be disabled
        assertFalse(retrieved.budgetAlerts.enabled)
        assertFalse(retrieved.largeTransactions.enabled)
        assertFalse(retrieved.weeklySummary.enabled)
    }

    // ==================== State Preservation Tests ====================

    @Test
    fun notificationSettings_persistAcrossInstanceRecreation() {
        // Given
        val originalSettings = NotificationSettings(
            budgetAlerts = BudgetAlertSettings(
                enabled = false,
                notifyAt80Percent = true,
                notifyAt100Percent = false
            ),
            largeTransactions = LargeTransactionSettings(
                enabled = true,
                threshold = 7500.0
            ),
            weeklySummary = WeeklySummarySettings(
                enabled = true,
                dayOfWeek = DayOfWeek.THURSDAY,
                hourOfDay = 16
            )
        )

        // When - Save settings
        appPreferences.setNotificationSettings(originalSettings)

        // Recreate instance (simulating app restart)
        AppPreferences.resetInstance()
        val newInstance = AppPreferences.getInstance(ApplicationProvider.getApplicationContext())

        // Then - Settings should be preserved
        val retrieved = newInstance.getNotificationSettings()
        assertEquals(originalSettings.budgetAlerts.enabled, retrieved.budgetAlerts.enabled)
        assertEquals(originalSettings.budgetAlerts.notifyAt80Percent, retrieved.budgetAlerts.notifyAt80Percent)
        assertEquals(originalSettings.budgetAlerts.notifyAt100Percent, retrieved.budgetAlerts.notifyAt100Percent)
        assertEquals(originalSettings.largeTransactions.enabled, retrieved.largeTransactions.enabled)
        assertEquals(originalSettings.largeTransactions.threshold, retrieved.largeTransactions.threshold, 0.001)
        assertEquals(originalSettings.weeklySummary.enabled, retrieved.weeklySummary.enabled)
        assertEquals(originalSettings.weeklySummary.dayOfWeek, retrieved.weeklySummary.dayOfWeek)
        assertEquals(originalSettings.weeklySummary.hourOfDay, retrieved.weeklySummary.hourOfDay)
    }

    @Test
    fun budgetAlertSettings_persistAcrossInstanceRecreation() {
        // Given
        appPreferences.setBudgetAlertsEnabled(false)
        appPreferences.setBudget80PercentAlertEnabled(false)
        appPreferences.setBudget100PercentAlertEnabled(true)

        // When - Recreate instance
        AppPreferences.resetInstance()
        val newInstance = AppPreferences.getInstance(ApplicationProvider.getApplicationContext())

        // Then
        assertFalse(newInstance.isBudgetAlertsEnabled())
        assertFalse(newInstance.isBudget80PercentAlertEnabled())
        assertTrue(newInstance.isBudget100PercentAlertEnabled())
    }

    @Test
    fun largeTransactionSettings_persistAcrossInstanceRecreation() {
        // Given
        appPreferences.setLargeTransactionAlertsEnabled(false)
        appPreferences.setLargeTransactionThreshold(20000.0)

        // When - Recreate instance
        AppPreferences.resetInstance()
        val newInstance = AppPreferences.getInstance(ApplicationProvider.getApplicationContext())

        // Then
        assertFalse(newInstance.isLargeTransactionAlertsEnabled())
        assertEquals(20000.0, newInstance.getLargeTransactionThreshold(), 0.001)
    }

    @Test
    fun weeklySummarySettings_persistAcrossInstanceRecreation() {
        // Given
        appPreferences.setWeeklySummaryEnabled(true)
        appPreferences.setWeeklySummaryDay(DayOfWeek.SATURDAY)
        appPreferences.setWeeklySummaryHour(21)

        // When - Recreate instance
        AppPreferences.resetInstance()
        val newInstance = AppPreferences.getInstance(ApplicationProvider.getApplicationContext())

        // Then
        assertTrue(newInstance.isWeeklySummaryEnabled())
        assertEquals(DayOfWeek.SATURDAY, newInstance.getWeeklySummaryDay())
        assertEquals(21, newInstance.getWeeklySummaryHour())
    }

    // ==================== Edge Cases and Integration Tests ====================

    @Test
    fun multipleUpdates_onlyLastValuePersists() {
        // When - Rapid updates to same setting
        appPreferences.setLargeTransactionThreshold(1000.0)
        appPreferences.setLargeTransactionThreshold(2000.0)
        appPreferences.setLargeTransactionThreshold(3000.0)
        appPreferences.setLargeTransactionThreshold(4500.0)

        // Then - Last value should persist
        assertEquals(4500.0, appPreferences.getLargeTransactionThreshold(), 0.001)
    }

    @Test
    fun batchSetterDoesNotAffectOtherSettings() {
        // Given - Set some initial values
        appPreferences.setBudgetAlertsEnabled(true)
        appPreferences.setLargeTransactionThreshold(8000.0)
        appPreferences.setWeeklySummaryEnabled(true)

        // When - Update only large transaction settings using batch setter
        appPreferences.setLargeTransactionSettings(
            LargeTransactionSettings(enabled = false, threshold = 15000.0)
        )

        // Then - Other settings should remain unchanged
        assertTrue(appPreferences.isBudgetAlertsEnabled())
        assertTrue(appPreferences.isWeeklySummaryEnabled())
        // Only large transaction settings should change
        assertFalse(appPreferences.isLargeTransactionAlertsEnabled())
        assertEquals(15000.0, appPreferences.getLargeTransactionThreshold(), 0.001)
    }

    @Test
    fun clearAll_resetsToDefaults() {
        // Given - Set custom values
        appPreferences.setBudgetAlertsEnabled(false)
        appPreferences.setLargeTransactionThreshold(25000.0)
        appPreferences.setWeeklySummaryEnabled(true)

        // When - Clear all preferences
        appPreferences.clearAll()

        // Then - Should revert to defaults
        assertTrue(appPreferences.isBudgetAlertsEnabled())
        assertEquals(5000.0, appPreferences.getLargeTransactionThreshold(), 0.001)
        assertFalse(appPreferences.isWeeklySummaryEnabled())
    }

    @Test
    fun extremeThresholdValues_storeCorrectly() {
        // Test very small value
        appPreferences.setLargeTransactionThreshold(0.01)
        assertEquals(0.01, appPreferences.getLargeTransactionThreshold(), 0.001)

        // Test very large value
        appPreferences.setLargeTransactionThreshold(999999999.99)
        assertEquals(999999999.99, appPreferences.getLargeTransactionThreshold(), 0.001)

        // Test maximum double value
        appPreferences.setLargeTransactionThreshold(Double.MAX_VALUE)
        assertEquals(Double.MAX_VALUE, appPreferences.getLargeTransactionThreshold(), 0.001)
    }

    @Test
    fun allDaysOfWeek_storeAndRetrieveCorrectly() {
        // Test storing and retrieving each day of the week
        val allDays = listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )

        allDays.forEach { day ->
            appPreferences.setWeeklySummaryDay(day)
            assertEquals(day, appPreferences.getWeeklySummaryDay())

            // Verify it persists after instance recreation
            AppPreferences.resetInstance()
            val newInstance = AppPreferences.getInstance(ApplicationProvider.getApplicationContext())
            assertEquals(day, newInstance.getWeeklySummaryDay())
        }
    }

    @Test
    fun boundaryHourValues_handleCorrectly() {
        // Test boundary values for hour
        val testCases = listOf(
            -100 to 0,  // Negative value should clamp to 0
            -1 to 0,    // -1 should clamp to 0
            0 to 0,     // 0 is valid
            1 to 1,     // 1 is valid
            12 to 12,   // 12 is valid (noon)
            23 to 23,   // 23 is valid (11 PM)
            24 to 23,   // 24 should clamp to 23
            25 to 23,   // 25 should clamp to 23
            100 to 23   // 100 should clamp to 23
        )

        testCases.forEach { (input, expected) ->
            appPreferences.setWeeklySummaryHour(input)
            assertEquals("Input $input should result in $expected", expected, appPreferences.getWeeklySummaryHour())
        }
    }

    @Test
    fun concurrentSetting_independentValues() {
        // When - Set all notification settings independently
        appPreferences.setBudgetAlertsEnabled(true)
        appPreferences.setBudget80PercentAlertEnabled(false)
        appPreferences.setBudget100PercentAlertEnabled(true)
        appPreferences.setLargeTransactionAlertsEnabled(false)
        appPreferences.setLargeTransactionThreshold(18000.0)
        appPreferences.setWeeklySummaryEnabled(true)
        appPreferences.setWeeklySummaryDay(DayOfWeek.FRIDAY)
        appPreferences.setWeeklySummaryHour(17)

        // Then - All values should be stored correctly
        assertTrue(appPreferences.isBudgetAlertsEnabled())
        assertFalse(appPreferences.isBudget80PercentAlertEnabled())
        assertTrue(appPreferences.isBudget100PercentAlertEnabled())
        assertFalse(appPreferences.isLargeTransactionAlertsEnabled())
        assertEquals(18000.0, appPreferences.getLargeTransactionThreshold(), 0.001)
        assertTrue(appPreferences.isWeeklySummaryEnabled())
        assertEquals(DayOfWeek.FRIDAY, appPreferences.getWeeklySummaryDay())
        assertEquals(17, appPreferences.getWeeklySummaryHour())
    }

    @Test
    fun decimalThresholdPrecision_maintainedCorrectly() {
        // Test that decimal precision is maintained
        val testValues = listOf(
            1234.56,
            9999.99,
            5000.00,
            7777.77,
            123.45
        )

        testValues.forEach { value ->
            appPreferences.setLargeTransactionThreshold(value)
            assertEquals(value, appPreferences.getLargeTransactionThreshold(), 0.001)
        }
    }

    @Test
    fun defaultsMatchModelClasses() {
        // When - Get settings without setting anything
        val budgetAlerts = appPreferences.getBudgetAlertSettings()
        val largeTransactions = appPreferences.getLargeTransactionSettings()
        val weeklySummary = appPreferences.getWeeklySummarySettings()

        // Then - Should match default values from model classes
        val defaultBudgetAlerts = BudgetAlertSettings()
        val defaultLargeTransactions = LargeTransactionSettings()
        val defaultWeeklySummary = WeeklySummarySettings()

        assertEquals(defaultBudgetAlerts.enabled, budgetAlerts.enabled)
        assertEquals(defaultBudgetAlerts.notifyAt80Percent, budgetAlerts.notifyAt80Percent)
        assertEquals(defaultBudgetAlerts.notifyAt100Percent, budgetAlerts.notifyAt100Percent)
        assertEquals(defaultLargeTransactions.enabled, largeTransactions.enabled)
        assertEquals(defaultLargeTransactions.threshold, largeTransactions.threshold, 0.001)
        assertEquals(defaultWeeklySummary.enabled, weeklySummary.enabled)
        assertEquals(defaultWeeklySummary.dayOfWeek, weeklySummary.dayOfWeek)
        assertEquals(defaultWeeklySummary.hourOfDay, weeklySummary.hourOfDay)
    }

    @Test
    fun notificationSettings_completeWorkflow() {
        // Simulate a complete user workflow

        // 1. User enables budget alerts with custom thresholds
        appPreferences.setBudgetAlertsEnabled(true)
        appPreferences.setBudget80PercentAlertEnabled(true)
        appPreferences.setBudget100PercentAlertEnabled(false)

        // 2. User sets large transaction threshold
        appPreferences.setLargeTransactionAlertsEnabled(true)
        appPreferences.setLargeTransactionThreshold(10000.0)

        // 3. User configures weekly summary
        appPreferences.setWeeklySummaryEnabled(true)
        appPreferences.setWeeklySummaryDay(DayOfWeek.SUNDAY)
        appPreferences.setWeeklySummaryHour(20)

        // 4. Verify everything is set correctly
        val settings = appPreferences.getNotificationSettings()
        assertTrue(settings.budgetAlerts.enabled)
        assertTrue(settings.budgetAlerts.notifyAt80Percent)
        assertFalse(settings.budgetAlerts.notifyAt100Percent)
        assertTrue(settings.largeTransactions.enabled)
        assertEquals(10000.0, settings.largeTransactions.threshold, 0.001)
        assertTrue(settings.weeklySummary.enabled)
        assertEquals(DayOfWeek.SUNDAY, settings.weeklySummary.dayOfWeek)
        assertEquals(20, settings.weeklySummary.hourOfDay)

        // 5. User disables all notifications
        appPreferences.setBudgetAlertsEnabled(false)
        appPreferences.setLargeTransactionAlertsEnabled(false)
        appPreferences.setWeeklySummaryEnabled(false)

        // 6. Verify all disabled
        val disabledSettings = appPreferences.getNotificationSettings()
        assertFalse(disabledSettings.budgetAlerts.enabled)
        assertFalse(disabledSettings.largeTransactions.enabled)
        assertFalse(disabledSettings.weeklySummary.enabled)
    }
}
