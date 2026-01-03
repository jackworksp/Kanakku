package com.example.kanakku.data.model

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

/**
 * Unit tests for DateRange data class and DateRangePreset enum.
 *
 * Tests cover:
 * - All DateRangePreset factory methods
 * - DateRange display name formatting
 * - Duration calculation
 * - Contains method for date checking
 * - Edge cases (same day, multi-year ranges, boundary conditions)
 * - Custom range creation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DateRangeTest {

    // ==================== DateRangePreset Tests ====================

    @Test
    fun dateRangePreset_allPresets_haveDisplayNames() {
        // Given/When/Then - All presets should have non-empty display names
        assertEquals("Today", DateRangePreset.TODAY.displayName)
        assertEquals("Yesterday", DateRangePreset.YESTERDAY.displayName)
        assertEquals("This Week", DateRangePreset.THIS_WEEK.displayName)
        assertEquals("Last Week", DateRangePreset.LAST_WEEK.displayName)
        assertEquals("This Month", DateRangePreset.THIS_MONTH.displayName)
        assertEquals("Last Month", DateRangePreset.LAST_MONTH.displayName)
        assertEquals("Last 7 Days", DateRangePreset.LAST_7_DAYS.displayName)
        assertEquals("Last 30 Days", DateRangePreset.LAST_30_DAYS.displayName)
        assertEquals("Last 90 Days", DateRangePreset.LAST_90_DAYS.displayName)
        assertEquals("This Year", DateRangePreset.THIS_YEAR.displayName)
        assertEquals("Last Year", DateRangePreset.LAST_YEAR.displayName)
        assertEquals("Custom Range", DateRangePreset.CUSTOM.displayName)
    }

    // ==================== Factory Method Tests ====================

    @Test
    fun today_createsValidRange() {
        // When
        val range = DateRange.today()

        // Then
        assertNotNull(range)
        assertEquals(DateRangePreset.TODAY, range.preset)
        assertTrue(range.startDate <= range.endDate)

        // Verify start is beginning of today
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = range.startDate
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))

        // Verify end is current time or later than start
        assertTrue(range.endDate >= range.startDate)
    }

    @Test
    fun yesterday_createsFullDayRange() {
        // When
        val range = DateRange.yesterday()

        // Then
        assertNotNull(range)
        assertEquals(DateRangePreset.YESTERDAY, range.preset)
        assertTrue(range.startDate < range.endDate)

        // Verify it's a full day (23:59:59.999)
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = range.startDate
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = range.endDate

        // Start should be 00:00:00.000
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))

        // End should be 23:59:59.999
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))

        // Should be same day
        assertEquals(startCal.get(Calendar.DAY_OF_YEAR), endCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(startCal.get(Calendar.YEAR), endCal.get(Calendar.YEAR))
    }

    @Test
    fun thisWeek_startsOnMonday() {
        // When
        val range = DateRange.thisWeek()

        // Then
        assertNotNull(range)
        assertEquals(DateRangePreset.THIS_WEEK, range.preset)

        // Verify starts on Monday at 00:00
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = range.startDate
        calendar.firstDayOfWeek = Calendar.MONDAY
        assertEquals(Calendar.MONDAY, calendar.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))

        // End should be current time
        assertTrue(range.endDate >= range.startDate)
    }

    @Test
    fun lastWeek_fullWeekMondayToSunday() {
        // When
        val range = DateRange.lastWeek()

        // Then
        assertNotNull(range)
        assertEquals(DateRangePreset.LAST_WEEK, range.preset)

        // Verify starts on Monday
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = range.startDate
        startCal.firstDayOfWeek = Calendar.MONDAY
        assertEquals(Calendar.MONDAY, startCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        // Verify ends on Sunday
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = range.endDate
        endCal.firstDayOfWeek = Calendar.MONDAY
        assertEquals(Calendar.SUNDAY, endCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
    }

    @Test
    fun thisMonth_startsOnFirstDay() {
        // When
        val range = DateRange.thisMonth()

        // Then
        assertNotNull(range)
        assertEquals(DateRangePreset.THIS_MONTH, range.preset)

        // Verify starts on 1st of month at 00:00
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = range.startDate
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))

        // End should be current time
        assertTrue(range.endDate >= range.startDate)
    }

    @Test
    fun lastMonth_fullMonthRange() {
        // When
        val range = DateRange.lastMonth()

        // Then
        assertNotNull(range)
        assertEquals(DateRangePreset.LAST_MONTH, range.preset)

        // Verify starts on 1st of last month
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = range.startDate
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        // Verify ends on last day of last month
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = range.endDate
        val expectedLastDay = endCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        assertEquals(expectedLastDay, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))

        // Verify they are in the same month
        assertEquals(startCal.get(Calendar.MONTH), endCal.get(Calendar.MONTH))
        assertEquals(startCal.get(Calendar.YEAR), endCal.get(Calendar.YEAR))
    }

    @Test
    fun last7Days_covers7DaysInMilliseconds() {
        // When
        val range = DateRange.last7Days()

        // Then
        assertNotNull(range)
        assertEquals(DateRangePreset.LAST_7_DAYS, range.preset)

        // Verify duration is approximately 7 days in milliseconds
        val expectedDuration = 7 * 24 * 60 * 60 * 1000L
        val actualDuration = range.endDate - range.startDate

        // Allow small margin for execution time
        assertTrue(Math.abs(actualDuration - expectedDuration) < 1000)
        assertTrue(range.endDate >= range.startDate)
    }

    @Test
    fun last30Days_covers30DaysInMilliseconds() {
        // When
        val range = DateRange.last30Days()

        // Then
        assertNotNull(range)
        assertEquals(DateRangePreset.LAST_30_DAYS, range.preset)

        // Verify duration is approximately 30 days in milliseconds
        val expectedDuration = 30 * 24 * 60 * 60 * 1000L
        val actualDuration = range.endDate - range.startDate

        // Allow small margin for execution time
        assertTrue(Math.abs(actualDuration - expectedDuration) < 1000)
        assertTrue(range.endDate >= range.startDate)
    }

    @Test
    fun last90Days_covers90DaysInMilliseconds() {
        // When
        val range = DateRange.last90Days()

        // Then
        assertNotNull(range)
        assertEquals(DateRangePreset.LAST_90_DAYS, range.preset)

        // Verify duration is approximately 90 days in milliseconds
        val expectedDuration = 90 * 24 * 60 * 60 * 1000L
        val actualDuration = range.endDate - range.startDate

        // Allow small margin for execution time
        assertTrue(Math.abs(actualDuration - expectedDuration) < 1000)
        assertTrue(range.endDate >= range.startDate)
    }

    @Test
    fun thisYear_startsOnJanuary1st() {
        // When
        val range = DateRange.thisYear()

        // Then
        assertNotNull(range)
        assertEquals(DateRangePreset.THIS_YEAR, range.preset)

        // Verify starts on January 1st at 00:00
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = range.startDate
        assertEquals(1, calendar.get(Calendar.DAY_OF_YEAR))
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))

        // End should be current time
        assertTrue(range.endDate >= range.startDate)
    }

    @Test
    fun lastYear_fullYearRange() {
        // When
        val range = DateRange.lastYear()

        // Then
        assertNotNull(range)
        assertEquals(DateRangePreset.LAST_YEAR, range.preset)

        // Verify starts on January 1st of last year
        val startCal = Calendar.getInstance()
        startCal.timeInMillis = range.startDate
        assertEquals(1, startCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        // Verify ends on December 31st of last year
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = range.endDate
        assertEquals(Calendar.DECEMBER, endCal.get(Calendar.MONTH))
        assertEquals(31, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))

        // Verify they are in the same year
        assertEquals(startCal.get(Calendar.YEAR), endCal.get(Calendar.YEAR))

        // Verify it's last year
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        assertEquals(currentYear - 1, startCal.get(Calendar.YEAR))
    }

    @Test
    fun custom_createsRangeWithSpecifiedDates() {
        // Given
        val start = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L)
        val end = System.currentTimeMillis()

        // When
        val range = DateRange.custom(start, end)

        // Then
        assertNotNull(range)
        assertEquals(start, range.startDate)
        assertEquals(end, range.endDate)
        assertEquals(DateRangePreset.CUSTOM, range.preset)
    }

    @Test
    fun fromPreset_today_createsTodayRange() {
        // When
        val range = DateRange.fromPreset(DateRangePreset.TODAY)

        // Then
        assertEquals(DateRangePreset.TODAY, range.preset)
        assertTrue(range.startDate <= range.endDate)
    }

    @Test
    fun fromPreset_lastMonth_createsLastMonthRange() {
        // When
        val range = DateRange.fromPreset(DateRangePreset.LAST_MONTH)

        // Then
        assertEquals(DateRangePreset.LAST_MONTH, range.preset)
        assertTrue(range.startDate < range.endDate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromPreset_custom_throwsException() {
        // When/Then - Should throw IllegalArgumentException
        DateRange.fromPreset(DateRangePreset.CUSTOM)
    }

    // ==================== Display Name Tests ====================

    @Test
    fun displayName_withPreset_returnsPresetDisplayName() {
        // Given
        val range = DateRange.today()

        // When
        val displayName = range.displayName

        // Then
        assertEquals("Today", displayName)
    }

    @Test
    fun displayName_customRange_returnsCustomRange() {
        // Given
        val start = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L)
        val end = System.currentTimeMillis()
        val range = DateRange.custom(start, end)

        // When
        val displayName = range.displayName

        // Then
        assertEquals("Custom Range", displayName)
    }

    @Test
    fun displayName_noPreset_returnsCustomRange() {
        // Given
        val start = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L)
        val end = System.currentTimeMillis()
        val range = DateRange(start, end, null)

        // When
        val displayName = range.displayName

        // Then
        assertEquals("Custom Range", displayName)
    }

    // ==================== Duration Tests ====================

    @Test
    fun durationInDays_sameDay_returns1() {
        // Given - Same day range
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val end = calendar.timeInMillis

        val range = DateRange(start, end)

        // When
        val duration = range.durationInDays

        // Then
        assertEquals(1, duration)
    }

    @Test
    fun durationInDays_exactlyOneDayApart_returns2() {
        // Given - Exactly 24 hours apart
        val start = System.currentTimeMillis()
        val end = start + (24 * 60 * 60 * 1000L)
        val range = DateRange(start, end)

        // When
        val duration = range.durationInDays

        // Then
        assertEquals(2, duration)
    }

    @Test
    fun durationInDays_7DaysRange_returns8() {
        // Given - Last 7 days range
        val range = DateRange.last7Days()

        // When
        val duration = range.durationInDays

        // Then
        // 7 days * 24 hours + 1 for inclusive counting
        assertTrue(duration >= 7 && duration <= 9)
    }

    @Test
    fun durationInDays_yearRange_returnsCorrectValue() {
        // Given
        val range = DateRange.lastYear()

        // When
        val duration = range.durationInDays

        // Then
        // Should be approximately 365 or 366 days (leap year)
        assertTrue(duration >= 365 && duration <= 367)
    }

    // ==================== Contains Method Tests ====================

    @Test
    fun contains_timestampInRange_returnsTrue() {
        // Given
        val start = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L)
        val end = System.currentTimeMillis()
        val range = DateRange(start, end)
        val timestampInRange = start + (5 * 24 * 60 * 60 * 1000L)

        // When
        val result = range.contains(timestampInRange)

        // Then
        assertTrue(result)
    }

    @Test
    fun contains_timestampAtStart_returnsTrue() {
        // Given
        val start = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L)
        val end = System.currentTimeMillis()
        val range = DateRange(start, end)

        // When
        val result = range.contains(start)

        // Then
        assertTrue(result)
    }

    @Test
    fun contains_timestampAtEnd_returnsTrue() {
        // Given
        val start = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L)
        val end = System.currentTimeMillis()
        val range = DateRange(start, end)

        // When
        val result = range.contains(end)

        // Then
        assertTrue(result)
    }

    @Test
    fun contains_timestampBeforeRange_returnsFalse() {
        // Given
        val start = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L)
        val end = System.currentTimeMillis()
        val range = DateRange(start, end)
        val timestampBefore = start - (1 * 24 * 60 * 60 * 1000L)

        // When
        val result = range.contains(timestampBefore)

        // Then
        assertFalse(result)
    }

    @Test
    fun contains_timestampAfterRange_returnsFalse() {
        // Given
        val start = System.currentTimeMillis() - (10 * 24 * 60 * 60 * 1000L)
        val end = System.currentTimeMillis()
        val range = DateRange(start, end)
        val timestampAfter = end + (1 * 24 * 60 * 60 * 1000L)

        // When
        val result = range.contains(timestampAfter)

        // Then
        assertFalse(result)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun edgeCase_instantaneousRange_sameStartAndEnd() {
        // Given
        val timestamp = System.currentTimeMillis()
        val range = DateRange(timestamp, timestamp)

        // When
        val duration = range.durationInDays
        val contains = range.contains(timestamp)

        // Then
        assertEquals(1, duration)
        assertTrue(contains)
    }

    @Test
    fun edgeCase_multiYearRange_calculatesCorrectly() {
        // Given - 2 year range
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -2)
        val start = calendar.timeInMillis
        val end = System.currentTimeMillis()
        val range = DateRange(start, end)

        // When
        val duration = range.durationInDays

        // Then
        // Should be approximately 730 days (2 years)
        assertTrue(duration >= 729 && duration <= 732)
    }

    @Test
    fun edgeCase_monthTransition_handlesCorrectly() {
        // Given - Range that spans month boundary
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 28)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 5) // Cross month boundary
        val end = calendar.timeInMillis
        val range = DateRange(start, end)

        // When
        val duration = range.durationInDays

        // Then
        assertTrue(duration >= 5 && duration <= 7)
    }

    @Test
    fun edgeCase_yearTransition_handlesCorrectly() {
        // Given - Range that spans year boundary
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 28)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, 10) // Cross year boundary
        val end = calendar.timeInMillis
        val range = DateRange(start, end)

        // When
        val duration = range.durationInDays

        // Then
        assertTrue(duration >= 10 && duration <= 12)
    }

    @Test
    fun edgeCase_leapYearFebruary_handlesCorrectly() {
        // Given - February in a leap year
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.FEBRUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, 29) // Leap year has 29 days
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis
        val range = DateRange(start, end)

        // When
        val duration = range.durationInDays

        // Then
        assertEquals(29, duration)
    }

    // ==================== Data Class Tests ====================

    @Test
    fun dataClass_equality_worksCorrectly() {
        // Given
        val start = System.currentTimeMillis()
        val end = start + 1000
        val range1 = DateRange(start, end, DateRangePreset.TODAY)
        val range2 = DateRange(start, end, DateRangePreset.TODAY)

        // When/Then
        assertEquals(range1, range2)
        assertEquals(range1.hashCode(), range2.hashCode())
    }

    @Test
    fun dataClass_copy_createsNewInstance() {
        // Given
        val range = DateRange.today()

        // When
        val copied = range.copy(preset = DateRangePreset.YESTERDAY)

        // Then
        assertNotEquals(range.preset, copied.preset)
        assertEquals(range.startDate, copied.startDate)
        assertEquals(range.endDate, copied.endDate)
    }

    @Test
    fun dataClass_toString_containsFieldValues() {
        // Given
        val range = DateRange.today()

        // When
        val string = range.toString()

        // Then
        assertTrue(string.contains("startDate"))
        assertTrue(string.contains("endDate"))
        assertTrue(string.contains("preset"))
    }
}
