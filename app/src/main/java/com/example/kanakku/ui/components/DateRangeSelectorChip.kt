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
import com.example.kanakku.ui.utils.DateFormatUtils

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

    // For custom ranges, use the utility function for compact formatting
    return DateFormatUtils.formatCompactDateRange(dateRange.startDate, dateRange.endDate)
}
