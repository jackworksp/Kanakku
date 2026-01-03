package com.example.kanakku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A dialog for selecting category colors from a predefined palette or custom color picker.
 *
 * The dialog displays a grid of Material Design colors commonly used for categories,
 * plus an option to select a custom color.
 *
 * @param currentColor The currently selected color, or null if none selected
 * @param onColorSelected Callback invoked when a color is selected, receives the selected Color
 * @param onDismiss Callback invoked when the dialog is dismissed
 */
@Composable
fun ColorPickerDialog(
    currentColor: Color?,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val predefinedColors = remember { getPredefinedColors() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Color",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Choose a color for your category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Color grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(predefinedColors) { color ->
                        ColorPickerItem(
                            color = color,
                            isSelected = color == currentColor,
                            onClick = { onColorSelected(color) }
                        )
                    }

                    // Custom color option
                    item {
                        CustomColorPickerItem(
                            isSelected = currentColor != null && !predefinedColors.contains(currentColor),
                            currentColor = currentColor,
                            onClick = { /* TODO: Implement custom color picker in future */ }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Custom colors coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

/**
 * A single color item in the picker grid.
 *
 * @param color The color to display
 * @param isSelected Whether this color is currently selected
 * @param onClick Callback invoked when this color is clicked
 */
@Composable
private fun ColorPickerItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(color = color, shape = CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Show check mark on selected color
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (isColorLight(color)) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * A custom color picker item that allows users to choose any color.
 *
 * @param isSelected Whether a custom color is currently selected
 * @param currentColor The currently selected custom color, if any
 * @param onClick Callback invoked when this item is clicked
 */
@Composable
private fun CustomColorPickerItem(
    isSelected: Boolean,
    currentColor: Color?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isSelected && currentColor != null) {
                    currentColor
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = CircleShape
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (!isSelected || currentColor == null) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Custom Color",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (isColorLight(currentColor)) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Returns a list of predefined Material Design colors suitable for categories.
 *
 * Colors are selected to provide good contrast and visual distinction between categories.
 * Includes colors used by default categories plus additional Material Design palette colors.
 *
 * @return List of predefined colors
 */
private fun getPredefinedColors(): List<Color> {
    return listOf(
        // Default category colors (from existing categories)
        Color(0xFFFF9800), // Food - Orange
        Color(0xFFE91E63), // Shopping - Pink
        Color(0xFF2196F3), // Transport - Blue
        Color(0xFF607D8B), // Bills - Blue Grey
        Color(0xFF9C27B0), // Entertainment - Purple
        Color(0xFF4CAF50), // Health - Green
        Color(0xFF00BCD4), // Transfer - Cyan
        Color(0xFF795548), // ATM - Brown
        Color(0xFF9E9E9E), // Other - Grey

        // Additional Material Design colors
        Color(0xFFF44336), // Red
        Color(0xFFE91E63), // Pink (duplicate, but good color)
        Color(0xFF9C27B0), // Purple (duplicate, but good color)
        Color(0xFF673AB7), // Deep Purple
        Color(0xFF3F51B5), // Indigo
        Color(0xFF2196F3), // Blue (duplicate, but good color)
        Color(0xFF03A9F4), // Light Blue
        Color(0xFF00BCD4), // Cyan (duplicate, but good color)
        Color(0xFF009688), // Teal
        Color(0xFF4CAF50), // Green (duplicate, but good color)
        Color(0xFF8BC34A), // Light Green
        Color(0xFFCDDC39), // Lime
        Color(0xFFFFEB3B), // Yellow
        Color(0xFFFFC107), // Amber
        Color(0xFFFF9800), // Orange (duplicate, but good color)
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF795548), // Brown (duplicate, but good color)
        Color(0xFF607D8B), // Blue Grey (duplicate, but good color)
        Color(0xFF9E9E9E), // Grey (duplicate, but good color)
    ).distinctBy { it.value } // Remove duplicates
}

/**
 * Determines if a color is light or dark based on its luminance.
 *
 * This is used to choose appropriate icon colors (black for light backgrounds,
 * white for dark backgrounds) for better visibility.
 *
 * @param color The color to check
 * @return True if the color is light, false if dark
 */
private fun isColorLight(color: Color): Boolean {
    // Calculate relative luminance
    val red = color.red
    val green = color.green
    val blue = color.blue

    // Using the formula for relative luminance
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue

    return luminance > 0.5
}
