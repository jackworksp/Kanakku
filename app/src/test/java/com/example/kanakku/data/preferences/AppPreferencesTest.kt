package com.example.kanakku.data.preferences

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for AppPreferences.
 *
 * Tests cover:
 * - Notification preference getter/setter
 * - Default analytics period preference getter/setter
 * - Currency symbol preference getter/setter
 * - Default values are correct
 * - Value persistence across get/set operations
 * - Edge cases and special characters
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppPreferencesTest {

    private lateinit var appPreferences: AppPreferences

    @Before
    fun setup() {
        // Reset AppPreferences singleton before each test
        AppPreferences.resetInstance()

        // Get fresh instance
        appPreferences = AppPreferences.getInstance(ApplicationProvider.getApplicationContext())
    }

    @After
    fun teardown() {
        // Clean up
        appPreferences.clearAll()
        AppPreferences.resetInstance()
    }

    // ==================== Notification Preference Tests ====================

    @Test
    fun isNotificationsEnabled_defaultValue_returnsTrue() {
        // Given: Fresh AppPreferences instance

        // When
        val result = appPreferences.isNotificationsEnabled()

        // Then
        assertTrue("Default notification preference should be true", result)
    }

    @Test
    fun setNotificationsEnabled_true_storesAndRetrievesTrue() {
        // Given
        val expectedValue = true

        // When
        appPreferences.setNotificationsEnabled(expectedValue)
        val result = appPreferences.isNotificationsEnabled()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun setNotificationsEnabled_false_storesAndRetrievesFalse() {
        // Given
        val expectedValue = false

        // When
        appPreferences.setNotificationsEnabled(expectedValue)
        val result = appPreferences.isNotificationsEnabled()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun setNotificationsEnabled_multipleUpdates_persistsLatestValue() {
        // Given
        appPreferences.setNotificationsEnabled(true)
        appPreferences.setNotificationsEnabled(false)

        // When
        appPreferences.setNotificationsEnabled(true)
        val result = appPreferences.isNotificationsEnabled()

        // Then
        assertTrue("Latest value should be persisted", result)
    }

    // ==================== Default Analytics Period Preference Tests ====================

    @Test
    fun getDefaultAnalyticsPeriod_defaultValue_returnsMonth() {
        // Given: Fresh AppPreferences instance

        // When
        val result = appPreferences.getDefaultAnalyticsPeriod()

        // Then
        assertEquals("Default analytics period should be MONTH", "MONTH", result)
    }

    @Test
    fun setDefaultAnalyticsPeriod_day_storesAndRetrievesDay() {
        // Given
        val expectedValue = "DAY"

        // When
        appPreferences.setDefaultAnalyticsPeriod(expectedValue)
        val result = appPreferences.getDefaultAnalyticsPeriod()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun setDefaultAnalyticsPeriod_week_storesAndRetrievesWeek() {
        // Given
        val expectedValue = "WEEK"

        // When
        appPreferences.setDefaultAnalyticsPeriod(expectedValue)
        val result = appPreferences.getDefaultAnalyticsPeriod()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun setDefaultAnalyticsPeriod_month_storesAndRetrievesMonth() {
        // Given
        val expectedValue = "MONTH"

        // When
        appPreferences.setDefaultAnalyticsPeriod(expectedValue)
        val result = appPreferences.getDefaultAnalyticsPeriod()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun setDefaultAnalyticsPeriod_year_storesAndRetrievesYear() {
        // Given
        val expectedValue = "YEAR"

        // When
        appPreferences.setDefaultAnalyticsPeriod(expectedValue)
        val result = appPreferences.getDefaultAnalyticsPeriod()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun setDefaultAnalyticsPeriod_multipleUpdates_persistsLatestValue() {
        // Given
        appPreferences.setDefaultAnalyticsPeriod("DAY")
        appPreferences.setDefaultAnalyticsPeriod("WEEK")

        // When
        appPreferences.setDefaultAnalyticsPeriod("YEAR")
        val result = appPreferences.getDefaultAnalyticsPeriod()

        // Then
        assertEquals("Latest value should be persisted", "YEAR", result)
    }

    @Test
    fun setDefaultAnalyticsPeriod_customValue_storesAndRetrievesCustomValue() {
        // Given
        val customValue = "CUSTOM_PERIOD"

        // When
        appPreferences.setDefaultAnalyticsPeriod(customValue)
        val result = appPreferences.getDefaultAnalyticsPeriod()

        // Then
        assertEquals("Custom values should be stored", customValue, result)
    }

    // ==================== Currency Symbol Preference Tests ====================

    @Test
    fun getCurrencySymbol_defaultValue_returnsRupee() {
        // Given: Fresh AppPreferences instance

        // When
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals("Default currency symbol should be ₹", "₹", result)
    }

    @Test
    fun setCurrencySymbol_rupee_storesAndRetrievesRupee() {
        // Given
        val expectedValue = "₹"

        // When
        appPreferences.setCurrencySymbol(expectedValue)
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun setCurrencySymbol_dollar_storesAndRetrievesDollar() {
        // Given
        val expectedValue = "$"

        // When
        appPreferences.setCurrencySymbol(expectedValue)
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun setCurrencySymbol_euro_storesAndRetrievesEuro() {
        // Given
        val expectedValue = "€"

        // When
        appPreferences.setCurrencySymbol(expectedValue)
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun setCurrencySymbol_pound_storesAndRetrievesPound() {
        // Given
        val expectedValue = "£"

        // When
        appPreferences.setCurrencySymbol(expectedValue)
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun setCurrencySymbol_yen_storesAndRetrievesYen() {
        // Given
        val expectedValue = "¥"

        // When
        appPreferences.setCurrencySymbol(expectedValue)
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun setCurrencySymbol_multipleUpdates_persistsLatestValue() {
        // Given
        appPreferences.setCurrencySymbol("$")
        appPreferences.setCurrencySymbol("€")

        // When
        appPreferences.setCurrencySymbol("£")
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals("Latest value should be persisted", "£", result)
    }

    @Test
    fun setCurrencySymbol_emptyString_storesAndRetrievesEmptyString() {
        // Given
        val expectedValue = ""

        // When
        appPreferences.setCurrencySymbol(expectedValue)
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals("Empty string should be stored", expectedValue, result)
    }

    @Test
    fun setCurrencySymbol_multiCharacterSymbol_storesAndRetrievesCorrectly() {
        // Given
        val expectedValue = "USD"

        // When
        appPreferences.setCurrencySymbol(expectedValue)
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals("Multi-character symbols should be stored", expectedValue, result)
    }

    @Test
    fun setCurrencySymbol_specialCharacters_storesAndRetrievesCorrectly() {
        // Given
        val expectedValue = "Rs."

        // When
        appPreferences.setCurrencySymbol(expectedValue)
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals("Special characters should be stored", expectedValue, result)
    }

    // ==================== Encryption Status Test ====================

    @Test
    fun isUsingEncryption_returnsNonNull() {
        // Given: AppPreferences instance

        // When
        val result = appPreferences.isUsingEncryption()

        // Then
        assertNotNull("Encryption status should not be null", result)
    }

    // ==================== Integration Tests ====================

    @Test
    fun allNewPreferences_setAndGet_persistCorrectly() {
        // Given
        val notificationEnabled = false
        val analyticsPeriod = "WEEK"
        val currencySymbol = "€"

        // When
        appPreferences.setNotificationsEnabled(notificationEnabled)
        appPreferences.setDefaultAnalyticsPeriod(analyticsPeriod)
        appPreferences.setCurrencySymbol(currencySymbol)

        // Then
        assertEquals(notificationEnabled, appPreferences.isNotificationsEnabled())
        assertEquals(analyticsPeriod, appPreferences.getDefaultAnalyticsPeriod())
        assertEquals(currencySymbol, appPreferences.getCurrencySymbol())
    }

    @Test
    fun allNewPreferences_afterClearAll_returnToDefaults() {
        // Given
        appPreferences.setNotificationsEnabled(false)
        appPreferences.setDefaultAnalyticsPeriod("DAY")
        appPreferences.setCurrencySymbol("$")

        // When
        appPreferences.clearAll()

        // Then
        assertTrue("Notification should return to default (true)", appPreferences.isNotificationsEnabled())
        assertEquals("Analytics period should return to default (MONTH)", "MONTH", appPreferences.getDefaultAnalyticsPeriod())
        assertEquals("Currency should return to default (₹)", "₹", appPreferences.getCurrencySymbol())
    }

    @Test
    fun preferences_persistAcrossInstanceRecreation() {
        // Given
        val notificationEnabled = false
        val analyticsPeriod = "YEAR"
        val currencySymbol = "£"

        appPreferences.setNotificationsEnabled(notificationEnabled)
        appPreferences.setDefaultAnalyticsPeriod(analyticsPeriod)
        appPreferences.setCurrencySymbol(currencySymbol)

        // When: Reset instance and get new one
        AppPreferences.resetInstance()
        val newInstance = AppPreferences.getInstance(ApplicationProvider.getApplicationContext())

        // Then: Values should persist
        assertEquals(notificationEnabled, newInstance.isNotificationsEnabled())
        assertEquals(analyticsPeriod, newInstance.getDefaultAnalyticsPeriod())
        assertEquals(currencySymbol, newInstance.getCurrencySymbol())
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun setDefaultAnalyticsPeriod_emptyString_storesAndRetrievesEmptyString() {
        // Given
        val expectedValue = ""

        // When
        appPreferences.setDefaultAnalyticsPeriod(expectedValue)
        val result = appPreferences.getDefaultAnalyticsPeriod()

        // Then
        assertEquals("Empty string should be stored", expectedValue, result)
    }

    @Test
    fun setDefaultAnalyticsPeriod_lowercase_storesAndRetrievesLowercase() {
        // Given
        val expectedValue = "day"

        // When
        appPreferences.setDefaultAnalyticsPeriod(expectedValue)
        val result = appPreferences.getDefaultAnalyticsPeriod()

        // Then
        assertEquals("Lowercase value should be stored as-is", expectedValue, result)
    }

    @Test
    fun setCurrencySymbol_unicodeCharacter_storesAndRetrievesCorrectly() {
        // Given
        val expectedValue = "₦" // Nigerian Naira

        // When
        appPreferences.setCurrencySymbol(expectedValue)
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals("Unicode currency symbols should be stored", expectedValue, result)
    }

    @Test
    fun setCurrencySymbol_withSpaces_storesAndRetrievesCorrectly() {
        // Given
        val expectedValue = "Rs. "

        // When
        appPreferences.setCurrencySymbol(expectedValue)
        val result = appPreferences.getCurrencySymbol()

        // Then
        assertEquals("Symbols with spaces should be stored", expectedValue, result)
    }
}
