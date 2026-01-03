package com.example.kanakku.ui.utils

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

/**
 * Unit tests for DateFormatUtils.
 * Tests all date formatting functions with various scenarios including edge cases.
 */
@RunWith(RobolectricTestRunner::class)
class DateFormatUtilsTest {

    private fun createDate(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // ============================================
    // formatDateRange Tests
    // ============================================

    @Test
    fun `formatDateRange returns single date format for same day`() {
        // Given
        val date = createDate(2024, Calendar.JANUARY, 15)

        // When
        val result = DateFormatUtils.formatDateRange(date, date)

        // Then
        assertTrue(result.contains("Jan") && result.contains("15"))
    }

    @Test
    fun `formatDateRange handles same month and year`() {
        // Given
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val startDate = createDate(currentYear, Calendar.JANUARY, 1)
        val endDate = createDate(currentYear, Calendar.JANUARY, 31)

        // When
        val result = DateFormatUtils.formatDateRange(startDate, endDate)

        // Then
        assertTrue(result.contains("Jan 1 - 31"))
    }

    @Test
    fun `formatDateRange handles same month and year for past year`() {
        // Given
        val startDate = createDate(2023, Calendar.JANUARY, 1)
        val endDate = createDate(2023, Calendar.JANUARY, 31)

        // When
        val result = DateFormatUtils.formatDateRange(startDate, endDate)

        // Then
        assertTrue(result.contains("Jan 1 - 31, 2023"))
    }

    @Test
    fun `formatDateRange handles different months in current year`() {
        // Given
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val startDate = createDate(currentYear, Calendar.JANUARY, 1)
        val endDate = createDate(currentYear, Calendar.FEBRUARY, 28)

        // When
        val result = DateFormatUtils.formatDateRange(startDate, endDate)

        // Then
        assertTrue(result.contains("Jan 1") && result.contains("Feb 28"))
        assertFalse(result.contains(currentYear.toString()))
    }

    @Test
    fun `formatDateRange handles different months in past year`() {
        // Given
        val startDate = createDate(2023, Calendar.JANUARY, 1)
        val endDate = createDate(2023, Calendar.FEBRUARY, 28)

        // When
        val result = DateFormatUtils.formatDateRange(startDate, endDate)

        // Then
        assertTrue(result.contains("Jan 1") && result.contains("Feb 28, 2023"))
    }

    @Test
    fun `formatDateRange handles different years`() {
        // Given
        val startDate = createDate(2023, Calendar.DECEMBER, 25)
        val endDate = createDate(2024, Calendar.JANUARY, 5)

        // When
        val result = DateFormatUtils.formatDateRange(startDate, endDate)

        // Then
        assertTrue(result.contains("Dec 25, 2023") && result.contains("Jan 5, 2024"))
    }

    // ============================================
    // formatShortDate Tests
    // ============================================

    @Test
    fun `formatShortDate formats current year without year`() {
        // Given
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val date = createDate(currentYear, Calendar.MARCH, 15)

        // When
        val result = DateFormatUtils.formatShortDate(date)

        // Then
        assertTrue(result.contains("Mar 15"))
        assertFalse(result.contains(currentYear.toString()))
    }

    @Test
    fun `formatShortDate formats past year with year`() {
        // Given
        val date = createDate(2023, Calendar.MARCH, 15)

        // When
        val result = DateFormatUtils.formatShortDate(date)

        // Then
        assertTrue(result.contains("Mar 15, 2023"))
    }

    @Test
    fun `formatShortDate formats future year with year`() {
        // Given
        val nextYear = Calendar.getInstance().get(Calendar.YEAR) + 1
        val date = createDate(nextYear, Calendar.MARCH, 15)

        // When
        val result = DateFormatUtils.formatShortDate(date)

        // Then
        assertTrue(result.contains("Mar 15"))
        assertTrue(result.contains(nextYear.toString()))
    }

    // ============================================
    // formatFullDate Tests
    // ============================================

    @Test
    fun `formatFullDate always includes year`() {
        // Given
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val date = createDate(currentYear, Calendar.APRIL, 20)

        // When
        val result = DateFormatUtils.formatFullDate(date)

        // Then
        assertTrue(result.contains("Apr 20"))
        assertTrue(result.contains(currentYear.toString()))
    }

    @Test
    fun `formatFullDate handles past dates`() {
        // Given
        val date = createDate(2022, Calendar.DECEMBER, 31)

        // When
        val result = DateFormatUtils.formatFullDate(date)

        // Then
        assertTrue(result.contains("Dec 31, 2022"))
    }

    // ============================================
    // formatCompactDateRange Tests
    // ============================================

    @Test
    fun `formatCompactDateRange handles same year in current year`() {
        // Given
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val startDate = createDate(currentYear, Calendar.JANUARY, 1)
        val endDate = createDate(currentYear, Calendar.JANUARY, 31)

        // When
        val result = DateFormatUtils.formatCompactDateRange(startDate, endDate)

        // Then
        assertTrue(result.contains("Jan 1") && result.contains("Jan 31"))
        assertFalse(result.contains(currentYear.toString()))
    }

    @Test
    fun `formatCompactDateRange handles same year in past year`() {
        // Given
        val startDate = createDate(2023, Calendar.JANUARY, 1)
        val endDate = createDate(2023, Calendar.JANUARY, 31)

        // When
        val result = DateFormatUtils.formatCompactDateRange(startDate, endDate)

        // Then
        assertTrue(result.contains("Jan 1") && result.contains("Jan 31, 2023"))
    }

    @Test
    fun `formatCompactDateRange handles different years`() {
        // Given
        val startDate = createDate(2023, Calendar.DECEMBER, 25)
        val endDate = createDate(2024, Calendar.JANUARY, 5)

        // When
        val result = DateFormatUtils.formatCompactDateRange(startDate, endDate)

        // Then
        assertTrue(result.contains("Dec 25, 2023") && result.contains("Jan 5, 2024"))
    }

    // ============================================
    // formatLongDate Tests
    // ============================================

    @Test
    fun `formatLongDate includes day of week`() {
        // Given - January 1, 2024 is a Monday
        val date = createDate(2024, Calendar.JANUARY, 1)

        // When
        val result = DateFormatUtils.formatLongDate(date)

        // Then
        assertTrue(result.contains("Jan 1, 2024"))
        // Day of week should be included
        assertTrue(result.length > "Jan 1, 2024".length)
    }

    // ============================================
    // formatMonthYear Tests
    // ============================================

    @Test
    fun `formatMonthYear shows full month and year`() {
        // Given
        val date = createDate(2024, Calendar.MARCH, 15)

        // When
        val result = DateFormatUtils.formatMonthYear(date)

        // Then
        assertTrue(result.contains("2024"))
        assertTrue(result.contains("March") || result.contains("march"))
    }

    // ============================================
    // formatShortMonthYear Tests
    // ============================================

    @Test
    fun `formatShortMonthYear shows abbreviated month and year`() {
        // Given
        val date = createDate(2024, Calendar.MARCH, 15)

        // When
        val result = DateFormatUtils.formatShortMonthYear(date)

        // Then
        assertTrue(result.contains("Mar 2024"))
    }

    // ============================================
    // getDurationInDays Tests
    // ============================================

    @Test
    fun `getDurationInDays returns 1 for same day`() {
        // Given
        val date = createDate(2024, Calendar.JANUARY, 15)

        // When
        val result = DateFormatUtils.getDurationInDays(date, date)

        // Then
        assertEquals(1, result)
    }

    @Test
    fun `getDurationInDays calculates correct duration for 7 days`() {
        // Given
        val startDate = createDate(2024, Calendar.JANUARY, 1)
        val endDate = createDate(2024, Calendar.JANUARY, 7)

        // When
        val result = DateFormatUtils.getDurationInDays(startDate, endDate)

        // Then
        assertEquals(7, result)
    }

    @Test
    fun `getDurationInDays calculates correct duration for 30 days`() {
        // Given
        val startDate = createDate(2024, Calendar.JANUARY, 1)
        val endDate = createDate(2024, Calendar.JANUARY, 30)

        // When
        val result = DateFormatUtils.getDurationInDays(startDate, endDate)

        // Then
        assertEquals(30, result)
    }

    @Test
    fun `getDurationInDays handles month boundary`() {
        // Given
        val startDate = createDate(2024, Calendar.JANUARY, 25)
        val endDate = createDate(2024, Calendar.FEBRUARY, 5)

        // When
        val result = DateFormatUtils.getDurationInDays(startDate, endDate)

        // Then
        assertEquals(12, result) // Jan 25-31 (7 days) + Feb 1-5 (5 days) = 12 days
    }

    @Test
    fun `getDurationInDays handles year boundary`() {
        // Given
        val startDate = createDate(2023, Calendar.DECEMBER, 25)
        val endDate = createDate(2024, Calendar.JANUARY, 5)

        // When
        val result = DateFormatUtils.getDurationInDays(startDate, endDate)

        // Then
        assertEquals(12, result) // Dec 25-31 (7 days) + Jan 1-5 (5 days) = 12 days
    }

    // ============================================
    // isSameDay Tests
    // ============================================

    @Test
    fun `isSameDay returns true for same day`() {
        // Given
        val date1 = createDate(2024, Calendar.JANUARY, 15)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date1
        calendar.set(Calendar.HOUR_OF_DAY, 18) // Different time
        val date2 = calendar.timeInMillis

        // When
        val result = DateFormatUtils.isSameDay(date1, date2)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isSameDay returns false for different days`() {
        // Given
        val date1 = createDate(2024, Calendar.JANUARY, 15)
        val date2 = createDate(2024, Calendar.JANUARY, 16)

        // When
        val result = DateFormatUtils.isSameDay(date1, date2)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isSameDay returns false for same day in different years`() {
        // Given
        val date1 = createDate(2023, Calendar.JANUARY, 15)
        val date2 = createDate(2024, Calendar.JANUARY, 15)

        // When
        val result = DateFormatUtils.isSameDay(date1, date2)

        // Then
        assertFalse(result)
    }

    // ============================================
    // isSameMonth Tests
    // ============================================

    @Test
    fun `isSameMonth returns true for same month and year`() {
        // Given
        val date1 = createDate(2024, Calendar.JANUARY, 1)
        val date2 = createDate(2024, Calendar.JANUARY, 31)

        // When
        val result = DateFormatUtils.isSameMonth(date1, date2)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isSameMonth returns false for different months`() {
        // Given
        val date1 = createDate(2024, Calendar.JANUARY, 15)
        val date2 = createDate(2024, Calendar.FEBRUARY, 15)

        // When
        val result = DateFormatUtils.isSameMonth(date1, date2)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isSameMonth returns false for same month in different years`() {
        // Given
        val date1 = createDate(2023, Calendar.JANUARY, 15)
        val date2 = createDate(2024, Calendar.JANUARY, 15)

        // When
        val result = DateFormatUtils.isSameMonth(date1, date2)

        // Then
        assertFalse(result)
    }

    // ============================================
    // isSameYear Tests
    // ============================================

    @Test
    fun `isSameYear returns true for same year`() {
        // Given
        val date1 = createDate(2024, Calendar.JANUARY, 1)
        val date2 = createDate(2024, Calendar.DECEMBER, 31)

        // When
        val result = DateFormatUtils.isSameYear(date1, date2)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isSameYear returns false for different years`() {
        // Given
        val date1 = createDate(2023, Calendar.DECEMBER, 31)
        val date2 = createDate(2024, Calendar.JANUARY, 1)

        // When
        val result = DateFormatUtils.isSameYear(date1, date2)

        // Then
        assertFalse(result)
    }

    // ============================================
    // Edge Cases and Integration Tests
    // ============================================

    @Test
    fun `formatDateRange handles leap year date`() {
        // Given
        val date = createDate(2024, Calendar.FEBRUARY, 29)

        // When
        val result = DateFormatUtils.formatShortDate(date)

        // Then
        assertTrue(result.contains("Feb 29"))
    }

    @Test
    fun `all formats handle beginning of year`() {
        // Given
        val date = createDate(2024, Calendar.JANUARY, 1)

        // When & Then - All should handle without errors
        assertNotNull(DateFormatUtils.formatShortDate(date))
        assertNotNull(DateFormatUtils.formatFullDate(date))
        assertNotNull(DateFormatUtils.formatLongDate(date))
        assertNotNull(DateFormatUtils.formatMonthYear(date))
        assertNotNull(DateFormatUtils.formatShortMonthYear(date))
    }

    @Test
    fun `all formats handle end of year`() {
        // Given
        val date = createDate(2024, Calendar.DECEMBER, 31)

        // When & Then - All should handle without errors
        assertNotNull(DateFormatUtils.formatShortDate(date))
        assertNotNull(DateFormatUtils.formatFullDate(date))
        assertNotNull(DateFormatUtils.formatLongDate(date))
        assertNotNull(DateFormatUtils.formatMonthYear(date))
        assertNotNull(DateFormatUtils.formatShortMonthYear(date))
    }

    @Test
    fun `formatDateRange handles very long range spanning multiple years`() {
        // Given
        val startDate = createDate(2020, Calendar.JANUARY, 1)
        val endDate = createDate(2024, Calendar.DECEMBER, 31)

        // When
        val result = DateFormatUtils.formatDateRange(startDate, endDate)

        // Then
        assertTrue(result.contains("2020"))
        assertTrue(result.contains("2024"))
    }

    @Test
    fun `getDurationInDays handles very long range`() {
        // Given - Full year 2024 (leap year = 366 days)
        val startDate = createDate(2024, Calendar.JANUARY, 1)
        val endDate = createDate(2024, Calendar.DECEMBER, 31)

        // When
        val result = DateFormatUtils.getDurationInDays(startDate, endDate)

        // Then
        assertEquals(366, result) // 2024 is a leap year
    }
}
