package com.example.kanakku.ui.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility object for formatting dates and date ranges for display.
 * Provides locale-aware, smart formatting based on the context of the dates.
 */
object DateFormatUtils {

    /**
     * Formats a date range for display with smart formatting based on date relationships.
     *
     * Examples:
     * - Same day: "Jan 15, 2024"
     * - Same month, same year: "Jan 1 - 31, 2024"
     * - Different months, same year (current year): "Jan 1 - Feb 28"
     * - Different months, same year (past year): "Jan 1 - Feb 28, 2023"
     * - Different years: "Dec 25, 2023 - Jan 5, 2024"
     *
     * @param startDate Start date timestamp in milliseconds
     * @param endDate End date timestamp in milliseconds
     * @return Formatted date range string
     */
    fun formatDateRange(startDate: Long, endDate: Long): String {
        val calendar = Calendar.getInstance()

        calendar.timeInMillis = startDate
        val startYear = calendar.get(Calendar.YEAR)
        val startMonth = calendar.get(Calendar.MONTH)
        val startDay = calendar.get(Calendar.DAY_OF_MONTH)

        calendar.timeInMillis = endDate
        val endYear = calendar.get(Calendar.YEAR)
        val endMonth = calendar.get(Calendar.MONTH)
        val endDay = calendar.get(Calendar.DAY_OF_MONTH)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return when {
            // Same day: "Jan 15, 2024"
            startYear == endYear && startMonth == endMonth && startDay == endDay -> {
                formatShortDate(startDate)
            }

            // Same month, same year: "Jan 1 - 31, 2024"
            startYear == endYear && startMonth == endMonth -> {
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val yearSuffix = if (startYear == currentYear) "" else ", $startYear"
                "${monthFormat.format(Date(startDate))} $startDay - $endDay$yearSuffix"
            }

            // Same year: "Jan 1 - Feb 28" or "Jan 1 - Feb 28, 2023"
            startYear == endYear -> {
                val startFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                val endFormat = if (endYear == currentYear) {
                    SimpleDateFormat("MMM d", Locale.getDefault())
                } else {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                }
                "${startFormat.format(Date(startDate))} - ${endFormat.format(Date(endDate))}"
            }

            // Different years: "Dec 25, 2023 - Jan 5, 2024"
            else -> {
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}"
            }
        }
    }

    /**
     * Formats a single date for display.
     *
     * Examples:
     * - Current year: "Jan 15"
     * - Past/future year: "Jan 15, 2023"
     *
     * @param date Date timestamp in milliseconds
     * @return Formatted date string
     */
    fun formatShortDate(date: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        val year = calendar.get(Calendar.YEAR)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val format = if (year == currentYear) {
            SimpleDateFormat("MMM d", Locale.getDefault())
        } else {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        }

        return format.format(Date(date))
    }

    /**
     * Formats a date with the full date format including year.
     *
     * Example: "Jan 15, 2024"
     *
     * @param date Date timestamp in milliseconds
     * @return Formatted date string with year
     */
    fun formatFullDate(date: Long): String {
        val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return format.format(Date(date))
    }

    /**
     * Formats a date range for compact display (e.g., in chips or small UI elements).
     * This is optimized for brevity while maintaining clarity.
     *
     * Examples:
     * - Same year: "Jan 1 - Jan 31"
     * - Different years: "Dec 25, 2023 - Jan 5, 2024"
     *
     * @param startDate Start date timestamp in milliseconds
     * @param endDate End date timestamp in milliseconds
     * @return Compact formatted date range string
     */
    fun formatCompactDateRange(startDate: Long, endDate: Long): String {
        val calendar = Calendar.getInstance()

        calendar.timeInMillis = startDate
        val startYear = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = endDate
        val endYear = calendar.get(Calendar.YEAR)

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return if (startYear == endYear) {
            // Same year: "Jan 1 - Jan 31" or "Jan 1 - Jan 31, 2023" for past years
            val startFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val endFormat = if (endYear == currentYear) {
                SimpleDateFormat("MMM d", Locale.getDefault())
            } else {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            }
            "${startFormat.format(Date(startDate))} - ${endFormat.format(Date(endDate))}"
        } else {
            // Different years: "Dec 25, 2023 - Jan 5, 2024"
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}"
        }
    }

    /**
     * Formats a date for display in lists or detailed views.
     *
     * Example: "Monday, Jan 15, 2024"
     *
     * @param date Date timestamp in milliseconds
     * @return Formatted date string with day of week
     */
    fun formatLongDate(date: Long): String {
        val format = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        return format.format(Date(date))
    }

    /**
     * Formats a date for display with month and year only.
     *
     * Example: "January 2024"
     *
     * @param date Date timestamp in milliseconds
     * @return Formatted month and year string
     */
    fun formatMonthYear(date: Long): String {
        val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return format.format(Date(date))
    }

    /**
     * Formats a date for display with abbreviated month and year.
     *
     * Example: "Jan 2024"
     *
     * @param date Date timestamp in milliseconds
     * @return Formatted abbreviated month and year string
     */
    fun formatShortMonthYear(date: Long): String {
        val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return format.format(Date(date))
    }

    /**
     * Calculates the duration between two dates in days.
     *
     * @param startDate Start date timestamp in milliseconds
     * @param endDate End date timestamp in milliseconds
     * @return Number of days between the dates (inclusive)
     */
    fun getDurationInDays(startDate: Long, endDate: Long): Int {
        val durationMillis = endDate - startDate
        return (durationMillis / (24 * 60 * 60 * 1000L)).toInt() + 1
    }

    /**
     * Checks if two dates fall on the same day.
     *
     * @param date1 First date timestamp in milliseconds
     * @param date2 Second date timestamp in milliseconds
     * @return true if both dates are on the same calendar day
     */
    fun isSameDay(date1: Long, date2: Long): Boolean {
        val calendar1 = Calendar.getInstance()
        calendar1.timeInMillis = date1

        val calendar2 = Calendar.getInstance()
        calendar2.timeInMillis = date2

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
               calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Checks if two dates are in the same month and year.
     *
     * @param date1 First date timestamp in milliseconds
     * @param date2 Second date timestamp in milliseconds
     * @return true if both dates are in the same month and year
     */
    fun isSameMonth(date1: Long, date2: Long): Boolean {
        val calendar1 = Calendar.getInstance()
        calendar1.timeInMillis = date1

        val calendar2 = Calendar.getInstance()
        calendar2.timeInMillis = date2

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
               calendar1.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH)
    }

    /**
     * Checks if two dates are in the same year.
     *
     * @param date1 First date timestamp in milliseconds
     * @param date2 Second date timestamp in milliseconds
     * @return true if both dates are in the same year
     */
    fun isSameYear(date1: Long, date2: Long): Boolean {
        val calendar1 = Calendar.getInstance()
        calendar1.timeInMillis = date1

        val calendar2 = Calendar.getInstance()
        calendar2.timeInMillis = date2

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
    }
}
