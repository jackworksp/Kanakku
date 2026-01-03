package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.DateRange
import java.text.SimpleDateFormat
import java.util.*

/**
 * Date range preset options for quick selection.
 */
enum class DateRangePreset(val displayName: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_3_MONTHS("Last 3 Months"),
    CUSTOM("Custom")
}

/**
 * Reusable date range picker component with preset options and custom date selection.
 *
 * Features:
 * - Quick preset options (This Week, This Month, Last 3 Months)
 * - Custom date range selection using Material3 DateRangePicker dialog
 * - Clear selection option
 * - Formatted date display
 *
 * @param selectedDateRange Currently selected date range (null if no selection)
 * @param onDateRangeSelected Callback when a date range is selected
 * @param modifier Optional modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePicker(
    selectedDateRange: DateRange?,
    onDateRangeSelected: (DateRange?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPreset by remember(selectedDateRange) {
        mutableStateOf(
            if (selectedDateRange == null) null
            else getPresetForDateRange(selectedDateRange)
        )
    }
    var showCustomPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Preset chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateRangePreset.entries.forEach { preset ->
                FilterChip(
                    selected = selectedPreset == preset,
                    onClick = {
                        if (preset == DateRangePreset.CUSTOM) {
                            showCustomPicker = true
                            selectedPreset = preset
                        } else {
                            val dateRange = getDateRangeForPreset(preset)
                            onDateRangeSelected(dateRange)
                            selectedPreset = preset
                        }
                    },
                    label = {
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = if (selectedPreset == preset && preset != DateRangePreset.CUSTOM) {
                        {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }

        // Display selected date range
        if (selectedDateRange != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Selected Range",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatDateRange(selectedDateRange),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = {
                            onDateRangeSelected(null)
                            selectedPreset = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear date range",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // Custom date range picker dialog
    if (showCustomPicker) {
        CustomDateRangePickerDialog(
            initialDateRange = selectedDateRange,
            onDateRangeSelected = { dateRange ->
                onDateRangeSelected(dateRange)
                selectedPreset = if (dateRange != null) DateRangePreset.CUSTOM else null
                showCustomPicker = false
            },
            onDismiss = {
                showCustomPicker = false
                if (selectedDateRange == null) {
                    selectedPreset = null
                }
            }
        )
    }
}

/**
 * Custom date range picker dialog using Material3 DateRangePicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangePickerDialog(
    initialDateRange: DateRange?,
    onDateRangeSelected: (DateRange?) -> Unit,
    onDismiss: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialDateRange?.startDate,
        initialSelectedEndDateMillis = initialDateRange?.endDate
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startDate = dateRangePickerState.selectedStartDateMillis
                    val endDate = dateRangePickerState.selectedEndDateMillis
                    if (startDate != null && endDate != null) {
                        onDateRangeSelected(DateRange(startDate, endDate))
                    } else {
                        onDateRangeSelected(null)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null &&
                        dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ==================== Helper Functions ====================

/**
 * Gets the DateRange for a preset option.
 */
private fun getDateRangeForPreset(preset: DateRangePreset): DateRange? {
    val calendar = Calendar.getInstance()
    val endDate = calendar.timeInMillis

    return when (preset) {
        DateRangePreset.THIS_WEEK -> {
            // Start from beginning of current week (Monday)
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            DateRange(calendar.timeInMillis, endDate)
        }
        DateRangePreset.THIS_MONTH -> {
            // Start from beginning of current month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            DateRange(calendar.timeInMillis, endDate)
        }
        DateRangePreset.LAST_3_MONTHS -> {
            // Start from 3 months ago
            calendar.add(Calendar.MONTH, -3)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            DateRange(calendar.timeInMillis, endDate)
        }
        DateRangePreset.CUSTOM -> null
    }
}

/**
 * Attempts to match a DateRange to a preset option.
 * Returns the preset if it matches closely, null otherwise.
 */
private fun getPresetForDateRange(dateRange: DateRange): DateRangePreset? {
    // Allow 24-hour tolerance for matching presets
    val tolerance = 24 * 60 * 60 * 1000L

    DateRangePreset.entries.forEach { preset ->
        if (preset == DateRangePreset.CUSTOM) return@forEach

        val presetRange = getDateRangeForPreset(preset) ?: return@forEach

        if (Math.abs(dateRange.startDate - presetRange.startDate) < tolerance &&
            Math.abs(dateRange.endDate - presetRange.endDate) < tolerance) {
            return preset
        }
    }

    // If no preset matches, it's a custom range
    return DateRangePreset.CUSTOM
}

/**
 * Formats a DateRange into a human-readable string.
 */
private fun formatDateRange(dateRange: DateRange): String {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val startDateStr = dateFormat.format(Date(dateRange.startDate))
    val endDateStr = dateFormat.format(Date(dateRange.endDate))
    return "$startDateStr - $endDateStr"
}
