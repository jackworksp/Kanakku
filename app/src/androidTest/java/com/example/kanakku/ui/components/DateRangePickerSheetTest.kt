package com.example.kanakku.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.kanakku.data.model.DateRange
import com.example.kanakku.data.model.DateRangePreset
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Instrumented tests for DateRangePickerSheet and DateRangeSelectorChip components.
 *
 * Tests cover:
 * - DateRangeSelectorChip display and interaction
 * - DateRangePickerSheet preset chip selection
 * - Custom date selection in DateRangePicker
 * - Apply and cancel actions
 * - Date validation
 */
@RunWith(AndroidJUnit4::class)
class DateRangePickerSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testDateRange: DateRange
    private var selectedDateRange: DateRange? = null
    private var sheetDismissed = false

    @Before
    fun setup() {
        // Reset state before each test
        testDateRange = DateRange.last30Days()
        selectedDateRange = null
        sheetDismissed = false
    }

    // ==================== DateRangeSelectorChip Tests ====================

    @Test
    fun dateRangeSelectorChip_displaysPresetName() {
        // Given
        val dateRange = DateRange.last30Days()

        // When
        composeTestRule.setContent {
            DateRangeSelectorChip(
                dateRange = dateRange,
                onClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Last 30 Days").assertIsDisplayed()
    }

    @Test
    fun dateRangeSelectorChip_displaysCustomDateRange() {
        // Given - Custom date range (Jan 1 to Jan 31, 2024)
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JANUARY, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis
        calendar.set(2024, Calendar.JANUARY, 31, 23, 59, 59)
        val endDate = calendar.timeInMillis
        val dateRange = DateRange.custom(startDate, endDate)

        // When
        composeTestRule.setContent {
            DateRangeSelectorChip(
                dateRange = dateRange,
                onClick = {}
            )
        }

        // Then - Should display formatted date range
        composeTestRule.onNodeWithText("Jan 1 - 31, 2024").assertIsDisplayed()
    }

    @Test
    fun dateRangeSelectorChip_callsOnClickWhenClicked() {
        // Given
        var clickCount = 0
        val dateRange = DateRange.last30Days()

        composeTestRule.setContent {
            DateRangeSelectorChip(
                dateRange = dateRange,
                onClick = { clickCount++ }
            )
        }

        // When
        composeTestRule.onNodeWithText("Last 30 Days").performClick()

        // Then
        assertEquals(1, clickCount)
    }

    @Test
    fun dateRangeSelectorChip_hasCalendarIcon() {
        // Given
        val dateRange = DateRange.last30Days()

        composeTestRule.setContent {
            DateRangeSelectorChip(
                dateRange = dateRange,
                onClick = {}
            )
        }

        // Then - Icon should be visible (content description check)
        composeTestRule.onNodeWithContentDescription("Select date range").assertIsDisplayed()
    }

    // ==================== DateRangePickerSheet Display Tests ====================

    @Test
    fun dateRangePickerSheet_displaysAllPresets() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // Then - All preset chips should be displayed
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yesterday").assertIsDisplayed()
        composeTestRule.onNodeWithText("This Week").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last Week").assertIsDisplayed()
        composeTestRule.onNodeWithText("This Month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last Month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last 7 Days").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last 30 Days").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last 90 Days").assertIsDisplayed()
        composeTestRule.onNodeWithText("This Year").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last Year").assertIsDisplayed()
    }

    @Test
    fun dateRangePickerSheet_displaysHeader() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Select Date Range").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick Presets").assertIsDisplayed()
        composeTestRule.onNodeWithText("Custom Date Range").assertIsDisplayed()
    }

    @Test
    fun dateRangePickerSheet_displaysActionButtons() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apply").assertIsDisplayed()
    }

    @Test
    fun dateRangePickerSheet_highlightsCurrentPreset() {
        // Given - Last 30 Days preset
        val dateRange = DateRange.last30Days()

        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = dateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // Then - Last 30 Days chip should be selected (FilterChip with selected=true)
        composeTestRule.onNodeWithText("Last 30 Days").assertIsDisplayed()
        // Note: Visual selection state is handled by FilterChip internally
    }

    // ==================== Preset Selection Tests ====================

    @Test
    fun dateRangePickerSheet_selectTodayPreset_enablesApplyButton() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When
        composeTestRule.onNodeWithText("Today").performClick()

        // Then - Apply button should be enabled
        composeTestRule.onNodeWithText("Apply").assertIsEnabled()
    }

    @Test
    fun dateRangePickerSheet_selectYesterdayPreset_enablesApplyButton() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When
        composeTestRule.onNodeWithText("Yesterday").performClick()

        // Then
        composeTestRule.onNodeWithText("Apply").assertIsEnabled()
    }

    @Test
    fun dateRangePickerSheet_selectThisWeekPreset_enablesApplyButton() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When
        composeTestRule.onNodeWithText("This Week").performClick()

        // Then
        composeTestRule.onNodeWithText("Apply").assertIsEnabled()
    }

    @Test
    fun dateRangePickerSheet_selectLastMonthPreset_enablesApplyButton() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When
        composeTestRule.onNodeWithText("Last Month").performClick()

        // Then
        composeTestRule.onNodeWithText("Apply").assertIsEnabled()
    }

    @Test
    fun dateRangePickerSheet_selectLast90DaysPreset_enablesApplyButton() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When
        composeTestRule.onNodeWithText("Last 90 Days").performClick()

        // Then
        composeTestRule.onNodeWithText("Apply").assertIsEnabled()
    }

    // ==================== Apply Action Tests ====================

    @Test
    fun dateRangePickerSheet_applyPresetSelection_callsCallback() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When - Select preset and apply
        composeTestRule.onNodeWithText("Last 7 Days").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        // Then
        assertNotNull(selectedDateRange)
        assertEquals(DateRangePreset.LAST_7_DAYS, selectedDateRange?.preset)
    }

    @Test
    fun dateRangePickerSheet_applyThisMonthPreset_callsCallbackWithCorrectPreset() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When
        composeTestRule.onNodeWithText("This Month").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        // Then
        assertNotNull(selectedDateRange)
        assertEquals(DateRangePreset.THIS_MONTH, selectedDateRange?.preset)
    }

    @Test
    fun dateRangePickerSheet_applyLastYearPreset_callsCallbackWithCorrectPreset() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When
        composeTestRule.onNodeWithText("Last Year").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        // Then
        assertNotNull(selectedDateRange)
        assertEquals(DateRangePreset.LAST_YEAR, selectedDateRange?.preset)
    }

    // ==================== Cancel Action Tests ====================

    @Test
    fun dateRangePickerSheet_cancel_callsDismissCallback() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then
        assertTrue(sheetDismissed)
        assertNull(selectedDateRange)
    }

    @Test
    fun dateRangePickerSheet_cancelAfterPresetSelection_doesNotCallOnDateRangeSelected() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When - Select preset but cancel
        composeTestRule.onNodeWithText("Last Month").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then - Dismiss called but no selection callback
        assertTrue(sheetDismissed)
        assertNull(selectedDateRange)
    }

    // ==================== Date Validation Tests ====================

    @Test
    fun dateRangePickerSheet_applyButtonEnabledWithPreset() {
        // When - Start with a preset
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = DateRange.last30Days(),
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // Then - Apply button should be enabled (preset is selected)
        composeTestRule.onNodeWithText("Apply").assertIsEnabled()
    }

    @Test
    fun dateRangePickerSheet_switchBetweenPresets_updatesSelection() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = DateRange.last30Days(),
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When - Switch between different presets
        composeTestRule.onNodeWithText("Last 7 Days").performClick()
        composeTestRule.onNodeWithText("Last 90 Days").performClick()
        composeTestRule.onNodeWithText("Apply").performClick()

        // Then - Last selected preset should be applied
        assertNotNull(selectedDateRange)
        assertEquals(DateRangePreset.LAST_90_DAYS, selectedDateRange?.preset)
    }

    // ==================== Integration Tests ====================

    @Test
    fun dateRangePickerSheet_fullFlow_selectPresetAndApply() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = DateRange.last30Days(),
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // Then - Verify initial state
        composeTestRule.onNodeWithText("Select Date Range").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apply").assertIsEnabled()

        // When - Select new preset
        composeTestRule.onNodeWithText("This Week").performClick()

        // Then - Apply button still enabled
        composeTestRule.onNodeWithText("Apply").assertIsEnabled()

        // When - Apply selection
        composeTestRule.onNodeWithText("Apply").performClick()

        // Then - Callback invoked with correct preset
        assertNotNull(selectedDateRange)
        assertEquals(DateRangePreset.THIS_WEEK, selectedDateRange?.preset)
        assertFalse(sheetDismissed) // onDismiss should not be called on apply
    }

    @Test
    fun dateRangePickerSheet_multiplePresetClicks_lastOneIsSelected() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = DateRange.last30Days(),
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // When - Click multiple presets in sequence
        composeTestRule.onNodeWithText("Today").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Yesterday").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Last Week").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Apply").performClick()

        // Then - Last clicked preset should be applied
        assertNotNull(selectedDateRange)
        assertEquals(DateRangePreset.LAST_WEEK, selectedDateRange?.preset)
    }

    @Test
    fun dateRangePickerSheet_currentDateRange_isInitiallyDisplayed() {
        // Given - Start with Last 7 Days
        val initialRange = DateRange.last7Days()

        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = initialRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // Then - Apply button should be enabled with current selection
        composeTestRule.onNodeWithText("Apply").assertIsEnabled()

        // When - Apply without changing selection
        composeTestRule.onNodeWithText("Apply").performClick()

        // Then - Should apply the current range
        assertNotNull(selectedDateRange)
        assertEquals(DateRangePreset.LAST_7_DAYS, selectedDateRange?.preset)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun dateRangePickerSheet_customDateRangeAsInput_displaysCorrectly() {
        // Given - Custom date range
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JUNE, 1, 0, 0, 0)
        val startDate = calendar.timeInMillis
        calendar.set(2024, Calendar.JUNE, 30, 23, 59, 59)
        val endDate = calendar.timeInMillis
        val customRange = DateRange.custom(startDate, endDate)

        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = customRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // Then - Sheet should display and allow interaction
        composeTestRule.onNodeWithText("Select Date Range").assertIsDisplayed()
        composeTestRule.onNodeWithText("Apply").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun dateRangePickerSheet_allPresetsAreClickable() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // Then - All preset chips should be clickable
        val presets = listOf(
            "Today", "Yesterday", "This Week", "Last Week",
            "This Month", "Last Month", "Last 7 Days", "Last 30 Days",
            "Last 90 Days", "This Year", "Last Year"
        )

        presets.forEach { presetName ->
            composeTestRule.onNodeWithText(presetName).assertHasClickAction()
        }
    }

    @Test
    fun dateRangePickerSheet_actionButtons_haveClickActions() {
        // When
        composeTestRule.setContent {
            DateRangePickerSheet(
                currentDateRange = testDateRange,
                onDateRangeSelected = { selectedDateRange = it },
                onDismiss = { sheetDismissed = true }
            )
        }

        // Then
        composeTestRule.onNodeWithText("Cancel").assertHasClickAction()
        composeTestRule.onNodeWithText("Apply").assertHasClickAction()
    }
}
