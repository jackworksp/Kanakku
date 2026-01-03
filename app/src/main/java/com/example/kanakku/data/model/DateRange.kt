package com.example.kanakku.data.model

import java.util.*

/**
 * Enum representing predefined date range presets.
 * Each preset has a display name and can calculate its corresponding date range.
 */
enum class DateRangePreset(val displayName: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    LAST_WEEK("Last Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    THIS_YEAR("This Year"),
    LAST_YEAR("Last Year"),
    CUSTOM("Custom Range")
}

/**
 * Data class representing a date range for filtering transactions and analytics.
 *
 * @param startDate Start date timestamp in milliseconds
 * @param endDate End date timestamp in milliseconds (inclusive)
 * @param preset Optional preset type if this range was created from a preset
 */
data class DateRange(
    val startDate: Long,
    val endDate: Long,
    val preset: DateRangePreset? = null
) {
    companion object {
        /**
         * Creates a DateRange for today (start of day to current time).
         */
        fun today(): DateRange {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val endOfDay = System.currentTimeMillis()

            return DateRange(
                startDate = startOfDay,
                endDate = endOfDay,
                preset = DateRangePreset.TODAY
            )
        }

        /**
         * Creates a DateRange for yesterday (full day).
         */
        fun yesterday(): DateRange {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis

            return DateRange(
                startDate = startOfDay,
                endDate = endOfDay,
                preset = DateRangePreset.YESTERDAY
            )
        }

        /**
         * Creates a DateRange for this week (Monday to current time).
         */
        fun thisWeek(): DateRange {
            val calendar = Calendar.getInstance()
            calendar.firstDayOfWeek = Calendar.MONDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfWeek = calendar.timeInMillis
            val now = System.currentTimeMillis()

            return DateRange(
                startDate = startOfWeek,
                endDate = now,
                preset = DateRangePreset.THIS_WEEK
            )
        }

        /**
         * Creates a DateRange for last week (Monday to Sunday).
         */
        fun lastWeek(): DateRange {
            val calendar = Calendar.getInstance()
            calendar.firstDayOfWeek = Calendar.MONDAY
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfWeek = calendar.timeInMillis

            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfWeek = calendar.timeInMillis

            return DateRange(
                startDate = startOfWeek,
                endDate = endOfWeek,
                preset = DateRangePreset.LAST_WEEK
            )
        }

        /**
         * Creates a DateRange for this month (1st to current time).
         */
        fun thisMonth(): DateRange {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis
            val now = System.currentTimeMillis()

            return DateRange(
                startDate = startOfMonth,
                endDate = now,
                preset = DateRangePreset.THIS_MONTH
            )
        }

        /**
         * Creates a DateRange for last month (1st to last day).
         */
        fun lastMonth(): DateRange {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -1)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfMonth = calendar.timeInMillis

            return DateRange(
                startDate = startOfMonth,
                endDate = endOfMonth,
                preset = DateRangePreset.LAST_MONTH
            )
        }

        /**
         * Creates a DateRange for the last 7 days (7 days ago to current time).
         */
        fun last7Days(): DateRange {
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)

            return DateRange(
                startDate = sevenDaysAgo,
                endDate = now,
                preset = DateRangePreset.LAST_7_DAYS
            )
        }

        /**
         * Creates a DateRange for the last 30 days (30 days ago to current time).
         */
        fun last30Days(): DateRange {
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000L)

            return DateRange(
                startDate = thirtyDaysAgo,
                endDate = now,
                preset = DateRangePreset.LAST_30_DAYS
            )
        }

        /**
         * Creates a DateRange for the last 90 days (90 days ago to current time).
         */
        fun last90Days(): DateRange {
            val now = System.currentTimeMillis()
            val ninetyDaysAgo = now - (90 * 24 * 60 * 60 * 1000L)

            return DateRange(
                startDate = ninetyDaysAgo,
                endDate = now,
                preset = DateRangePreset.LAST_90_DAYS
            )
        }

        /**
         * Creates a DateRange for this year (Jan 1 to current time).
         */
        fun thisYear(): DateRange {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfYear = calendar.timeInMillis
            val now = System.currentTimeMillis()

            return DateRange(
                startDate = startOfYear,
                endDate = now,
                preset = DateRangePreset.THIS_YEAR
            )
        }

        /**
         * Creates a DateRange for last year (Jan 1 to Dec 31).
         */
        fun lastYear(): DateRange {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, -1)
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfYear = calendar.timeInMillis

            calendar.set(Calendar.MONTH, Calendar.DECEMBER)
            calendar.set(Calendar.DAY_OF_MONTH, 31)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfYear = calendar.timeInMillis

            return DateRange(
                startDate = startOfYear,
                endDate = endOfYear,
                preset = DateRangePreset.LAST_YEAR
            )
        }

        /**
         * Creates a custom DateRange with specified start and end dates.
         *
         * @param startDate Start date timestamp in milliseconds
         * @param endDate End date timestamp in milliseconds
         */
        fun custom(startDate: Long, endDate: Long): DateRange {
            return DateRange(
                startDate = startDate,
                endDate = endDate,
                preset = DateRangePreset.CUSTOM
            )
        }

        /**
         * Creates a DateRange from a preset type.
         *
         * @param preset The preset type to create
         */
        fun fromPreset(preset: DateRangePreset): DateRange {
            return when (preset) {
                DateRangePreset.TODAY -> today()
                DateRangePreset.YESTERDAY -> yesterday()
                DateRangePreset.THIS_WEEK -> thisWeek()
                DateRangePreset.LAST_WEEK -> lastWeek()
                DateRangePreset.THIS_MONTH -> thisMonth()
                DateRangePreset.LAST_MONTH -> lastMonth()
                DateRangePreset.LAST_7_DAYS -> last7Days()
                DateRangePreset.LAST_30_DAYS -> last30Days()
                DateRangePreset.LAST_90_DAYS -> last90Days()
                DateRangePreset.THIS_YEAR -> thisYear()
                DateRangePreset.LAST_YEAR -> lastYear()
                DateRangePreset.CUSTOM -> throw IllegalArgumentException(
                    "Cannot create custom range from preset. Use custom(startDate, endDate) instead."
                )
            }
        }
    }

    /**
     * Returns the display name for this date range.
     * Uses the preset's display name if available, otherwise returns a formatted date range.
     */
    val displayName: String
        get() = preset?.displayName ?: "Custom Range"

    /**
     * Returns the duration of this date range in days.
     */
    val durationInDays: Int
        get() {
            val durationMillis = endDate - startDate
            return (durationMillis / (24 * 60 * 60 * 1000L)).toInt() + 1
        }

    /**
     * Checks if a timestamp falls within this date range (inclusive).
     */
    fun contains(timestamp: Long): Boolean {
        return timestamp in startDate..endDate
    }
}
