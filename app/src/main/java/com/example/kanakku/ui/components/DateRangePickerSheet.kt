package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.DateRange
import com.example.kanakku.data.model.DateRangePreset

/**
 * A ModalBottomSheet that displays quick date range presets and a Material3 DateRangePicker
 * for custom date selection.
 *
 * The sheet contains:
 * - Quick preset chips at the top (Today, Last Week, Last Month, etc.)
 * - Material3 DateRangePicker for custom date selection
 * - Apply and Cancel buttons
 *
 * @param currentDateRange The currently selected date range
 * @param onDateRangeSelected Callback invoked when a date range is selected and confirmed
 * @param onDismiss Callback invoked when the sheet is dismissed without selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerSheet(
    currentDateRange: DateRange,
    onDateRangeSelected: (DateRange) -> Unit,
    onDismiss: () -> Unit
) {
    // State for the selected date range (working copy before apply)
    var selectedRange by remember { mutableStateOf(currentDateRange) }

    // State for the date range picker
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = currentDateRange.startDate,
        initialSelectedEndDateMillis = currentDateRange.endDate
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "Select Date Range",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Presets Section
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(getQuickPresets()) { preset ->
                    PresetChip(
                        preset = preset,
                        isSelected = selectedRange.preset == preset,
                        onClick = {
                            selectedRange = DateRange.fromPreset(preset)
                            // Update the date picker state to match the preset
                            val presetRange = DateRange.fromPreset(preset)
                            dateRangePickerState.setSelection(
                                startDateMillis = presetRange.startDate,
                                endDateMillis = presetRange.endDate
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Custom Date Range Section
            Text(
                text = "Custom Date Range",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Material3 DateRangePicker
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.fillMaxWidth(),
                title = null,
                headline = {
                    DateRangePickerDefaults.DateRangePickerHeadline(
                        selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                        selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                        displayMode = dateRangePickerState.displayMode,
                        dateFormatter = DatePickerDefaults.dateFormatter(),
                        modifier = Modifier.padding(start = 16.dp, end = 12.dp, bottom = 12.dp)
                    )
                },
                showModeToggle = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                // Apply Button
                Button(
                    onClick = {
                        val startDate = dateRangePickerState.selectedStartDateMillis
                        val endDate = dateRangePickerState.selectedEndDateMillis

                        if (startDate != null && endDate != null && startDate <= endDate) {
                            // Create custom date range if both dates are selected
                            val newRange = DateRange.custom(startDate, endDate)
                            onDateRangeSelected(newRange)
                        } else if (selectedRange.preset != null) {
                            // Use the selected preset if custom dates are not valid
                            onDateRangeSelected(selectedRange)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isValidSelection(dateRangePickerState, selectedRange)
                ) {
                    Text("Apply")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * A chip component for quick date range preset selection.
 *
 * @param preset The preset to display
 * @param isSelected Whether this preset is currently selected
 * @param onClick Callback invoked when the chip is clicked
 */
@Composable
private fun PresetChip(
    preset: DateRangePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = preset.displayName,
                style = MaterialTheme.typography.labelMedium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * Returns the list of quick presets to display in the sheet.
 * Excludes CUSTOM as it's handled by the date picker.
 */
private fun getQuickPresets(): List<DateRangePreset> {
    return listOf(
        DateRangePreset.TODAY,
        DateRangePreset.YESTERDAY,
        DateRangePreset.THIS_WEEK,
        DateRangePreset.LAST_WEEK,
        DateRangePreset.THIS_MONTH,
        DateRangePreset.LAST_MONTH,
        DateRangePreset.LAST_7_DAYS,
        DateRangePreset.LAST_30_DAYS,
        DateRangePreset.LAST_90_DAYS,
        DateRangePreset.THIS_YEAR,
        DateRangePreset.LAST_YEAR
    )
}

/**
 * Validates the current selection in the date range picker.
 * Returns true if either:
 * - Both start and end dates are selected and start <= end
 * - A preset is selected
 *
 * @param state The DateRangePickerState to validate
 * @param selectedRange The currently selected preset range
 * @return true if the selection is valid, false otherwise
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun isValidSelection(
    state: DateRangePickerState,
    selectedRange: DateRange
): Boolean {
    val startDate = state.selectedStartDateMillis
    val endDate = state.selectedEndDateMillis

    // Valid if a preset is selected
    if (selectedRange.preset != null && selectedRange.preset != DateRangePreset.CUSTOM) {
        return true
    }

    // Valid if both dates are selected and start <= end
    if (startDate != null && endDate != null) {
        return startDate <= endDate
    }

    return false
}
