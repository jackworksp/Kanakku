package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.Category

/**
 * Category filter component that displays all categories as toggleable chips.
 *
 * Features:
 * - Multi-select category filtering with toggleable chips
 * - Visual category representation with icon and name
 * - Selection state indicators
 * - 'Select All' / 'Clear All' quick action
 * - Responsive FlowRow layout for chip wrapping
 *
 * @param availableCategories List of all available categories to display
 * @param selectedCategoryIds Set of currently selected category IDs
 * @param onCategoryToggled Callback when a category is toggled
 * @param onSelectAll Callback when 'Select All' is clicked
 * @param onClearAll Callback when 'Clear All' is clicked
 * @param modifier Optional modifier for the component
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryFilter(
    availableCategories: List<Category>,
    selectedCategoryIds: List<String>,
    onCategoryToggled: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSelected = selectedCategoryIds.size == availableCategories.size
    val noneSelected = selectedCategoryIds.isEmpty()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Select All / Clear All action chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = allSelected,
                onClick = {
                    if (allSelected || !noneSelected) {
                        onClearAll()
                    } else {
                        onSelectAll()
                    }
                },
                label = {
                    Text(
                        text = if (allSelected || !noneSelected) "Clear All" else "Select All",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (allSelected || !noneSelected) Icons.Default.Close else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            // Show selection count when some (but not all) are selected
            if (!noneSelected && !allSelected) {
                Text(
                    text = "${selectedCategoryIds.size} of ${availableCategories.size} selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }

        // Category chips in a flow layout
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableCategories.forEach { category ->
                CategoryFilterChip(
                    category = category,
                    isSelected = selectedCategoryIds.contains(category.id),
                    onToggle = { onCategoryToggled(category.id) }
                )
            }
        }

        // Display selected categories summary
        if (!noneSelected) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                            text = "Filtered Categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (allSelected) {
                                "All categories selected"
                            } else {
                                availableCategories
                                    .filter { selectedCategoryIds.contains(it.id) }
                                    .joinToString(", ") { it.name }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    IconButton(
                        onClick = onClearAll
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear category filter",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual category filter chip showing icon, name, and selection state.
 *
 * @param category The category to display
 * @param isSelected Whether this category is currently selected
 * @param onToggle Callback when the chip is clicked
 */
@Composable
private fun CategoryFilterChip(
    category: Category,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        label = {
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            // Category icon (emoji)
            Text(
                text = category.icon,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = category.color.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSurface
        ),
        border = if (isSelected) {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = true,
                borderColor = category.color,
                selectedBorderColor = category.color,
                borderWidth = 2.dp,
                selectedBorderWidth = 2.dp
            )
        } else {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false
            )
        }
    )
}
