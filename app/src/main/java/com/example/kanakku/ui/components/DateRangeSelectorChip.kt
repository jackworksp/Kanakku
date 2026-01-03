package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.DateRange
import java.text.SimpleDateFormat
import java.util.*

/**
 * A compact chip/button component that displays the current date range
 * and opens the date range picker when clicked.
 *
 * The chip shows:
 * - Preset name (e.g., "Last 30 Days") for predefined date ranges
 * - Formatted date range (e.g., "Jan 1 - Jan 31") for custom ranges
 *
 * @param dateRange The currently selected date range to display
 * @param onClick Callback invoked when the chip is clicked to open the picker
 * @param modifier Optional modifier for customizing the chip appearance and position
 */
@Composable
fun DateRangeSelectorChip(
    dateRange: DateRange,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = getDateRangeDisplayText(dateRange),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select date range",
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = AssistChipDefaults.assistChipBorder(
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            borderWidth = 1.dp
        )
    )
}

/**
 * Returns the display text for a date range.
 * Shows the preset name if available, otherwise formats the custom date range.
 *
 * @param dateRange The date range to format
 * @return A human-readable string representing the date range
 */
private fun getDateRangeDisplayText(dateRange: DateRange): String {
    // If it's a preset, use the preset's display name
    dateRange.preset?.let {
        return it.displayName
    }

    // For custom ranges, format as "MMM d - MMM d" or "MMM d, yyyy - MMM d, yyyy"
    return formatCustomDateRange(dateRange.startDate, dateRange.endDate)
}

/**
 * Formats a custom date range for display.
 * Uses smart formatting based on whether dates are in the same year.
 *
 * Examples:
 * - Same year: "Jan 1 - Jan 31"
 * - Different years: "Dec 25, 2023 - Jan 5, 2024"
 *
 * @param startDate Start date timestamp in milliseconds
 * @param endDate End date timestamp in milliseconds
 * @return Formatted date range string
 */
private fun formatCustomDateRange(startDate: Long, endDate: Long): String {
    val calendar = Calendar.getInstance()

    calendar.timeInMillis = startDate
    val startYear = calendar.get(Calendar.YEAR)

    calendar.timeInMillis = endDate
    val endYear = calendar.get(Calendar.YEAR)

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    return if (startYear == endYear) {
        // Same year: "Jan 1 - Jan 31" or "Jan 1 - Jan 31, 2023" if not current year
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
