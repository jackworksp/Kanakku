package com.example.kanakku.ui.savings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

/**
 * Bottom sheet for creating new savings goals
 *
 * @param onCreateGoal Callback when goal is created with name, target amount, deadline timestamp, icon, and color
 * @param onDismiss Callback when sheet is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalSheet(
    onCreateGoal: (name: String, targetAmount: Double, deadline: Long, icon: String, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("🎯") }
    var selectedColor by remember { mutableStateOf("#6200EE") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Create Savings Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("Goal Name") },
                placeholder = { Text("e.g., Vacation Fund") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Please enter a goal name") }
                } else null,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target Amount Input
            OutlinedTextField(
                value = targetAmount,
                onValueChange = {
                    // Allow only numbers and decimal point
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                        targetAmount = it
                        amountError = false
                    }
                },
                label = { Text("Target Amount") },
                placeholder = { Text("e.g., 50000") },
                leadingIcon = { Text("₹", style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier.fillMaxWidth(),
                isError = amountError,
                supportingText = if (amountError) {
                    { Text("Please enter a valid amount") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deadline Picker Button
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (dateError) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    } else {
                        Color.Transparent
                    }
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select deadline",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedDateMillis != null) {
                        "Deadline: ${formatDate(selectedDateMillis!!)}"
                    } else {
                        "Select Deadline"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (dateError) {
                Text(
                    text = "Please select a deadline",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Icon Selection
            Text(
                text = "Choose Icon",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(goalIcons) { icon ->
                    IconOption(
                        icon = icon,
                        isSelected = icon == selectedIcon,
                        onClick = { selectedIcon = icon }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Color Selection
            Text(
                text = "Choose Color",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(100.dp)
            ) {
                items(goalColors) { colorItem ->
                    ColorOption(
                        color = colorItem.color,
                        isSelected = colorItem.hex == selectedColor,
                        onClick = { selectedColor = colorItem.hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            Button(
                onClick = {
                    // Validate inputs
                    var hasError = false

                    if (name.isBlank()) {
                        nameError = true
                        hasError = true
                    }

                    val amount = targetAmount.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = true
                        hasError = true
                    }

                    if (selectedDateMillis == null) {
                        dateError = true
                        hasError = true
                    }

                    if (!hasError && amount != null && selectedDateMillis != null) {
                        onCreateGoal(name, amount, selectedDateMillis!!, selectedIcon, selectedColor)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Goal")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { dateMillis ->
                selectedDateMillis = dateMillis
                dateError = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * Icon option for goal icon selection
 *
 * @param icon The icon emoji
 * @param isSelected Whether this icon is currently selected
 * @param onClick Callback when icon is clicked
 */
@Composable
private fun IconOption(
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .align(Alignment.TopEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

/**
 * Color option for goal color selection
 *
 * @param color The color
 * @param isSelected Whether this color is currently selected
 * @param onClick Callback when color is clicked
 */
@Composable
private fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = color,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Date picker dialog using Material3 DatePicker
 *
 * @param onDateSelected Callback when date is selected with timestamp in milliseconds
 * @param onDismiss Callback when dialog is dismissed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                // Convert to end of day in local timezone
                                val localDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                val endOfDay = localDate.atTime(23, 59, 59)
                                val endOfDayMillis = endOfDay
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                                onDateSelected(endOfDayMillis)
                            }
                            onDismiss()
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

/**
 * Format date timestamp to readable string
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Available goal icons
 */
private val goalIcons = listOf(
    "🎯", "💰", "🏠", "✈️", "🚗", "💍",
    "🎓", "💻", "📱", "🏖️", "🎁", "⭐",
    "❤️", "🌟", "🔥", "💎", "🏆", "🎉",
    "🌈", "☀️", "🌙", "🎸", "🎨", "📚"
)

/**
 * Available goal colors with hex values
 */
private data class ColorItem(val color: Color, val hex: String)

private val goalColors = listOf(
    ColorItem(Color(0xFF6200EE), "#6200EE"),
    ColorItem(Color(0xFF3700B3), "#3700B3"),
    ColorItem(Color(0xFF03DAC6), "#03DAC6"),
    ColorItem(Color(0xFF018786), "#018786"),
    ColorItem(Color(0xFFE91E63), "#E91E63"),
    ColorItem(Color(0xFFF44336), "#F44336"),
    ColorItem(Color(0xFF9C27B0), "#9C27B0"),
    ColorItem(Color(0xFF673AB7), "#673AB7"),
    ColorItem(Color(0xFF2196F3), "#2196F3"),
    ColorItem(Color(0xFF00BCD4), "#00BCD4"),
    ColorItem(Color(0xFF009688), "#009688"),
    ColorItem(Color(0xFF4CAF50), "#4CAF50"),
    ColorItem(Color(0xFF8BC34A), "#8BC34A"),
    ColorItem(Color(0xFFCDDC39), "#CDDC39"),
    ColorItem(Color(0xFFFFEB3B), "#FFEB3B"),
    ColorItem(Color(0xFFFFC107), "#FFC107"),
    ColorItem(Color(0xFFFF9800), "#FF9800"),
    ColorItem(Color(0xFFFF5722), "#FF5722")
)
