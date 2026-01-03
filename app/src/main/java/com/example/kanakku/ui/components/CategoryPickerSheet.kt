package com.example.kanakku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories

/**
 * Bottom sheet for selecting a category from a hierarchical list.
 *
 * Displays categories grouped by parent-child relationships with collapsible sections.
 * Subcategories are shown in indented, expandable groups under their parent categories.
 * Includes an optional "Create New Category" button at the bottom.
 *
 * @param currentCategory The currently selected category (highlighted in UI)
 * @param allCategories List of all available categories (both root and subcategories)
 * @param onCategorySelected Callback invoked when a category is selected
 * @param onDismiss Callback invoked when the sheet is dismissed
 * @param onCreateNewCategory Optional callback invoked when "Create New Category" is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    currentCategory: Category?,
    allCategories: List<Category> = DefaultCategories.ALL,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit,
    onCreateNewCategory: (() -> Unit)? = null
) {
    // Track expanded state for each parent category
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    // Group categories by parent-child relationship
    val rootCategories = remember(allCategories) {
        allCategories.filter { it.parentId == null }
    }

    val subcategoriesMap = remember(allCategories) {
        allCategories
            .filter { it.parentId != null }
            .groupBy { it.parentId!! }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Display root categories with their subcategories
                items(rootCategories) { category ->
                    val subcategories = subcategoriesMap[category.id] ?: emptyList()
                    val hasSubcategories = subcategories.isNotEmpty()
                    val isExpanded = expandedCategories[category.id] ?: false

                    // Root category item
                    CategoryPickerItem(
                        category = category,
                        isSelected = category.id == currentCategory?.id,
                        hasSubcategories = hasSubcategories,
                        isExpanded = isExpanded,
                        onClick = {
                            if (hasSubcategories) {
                                // Toggle expansion
                                expandedCategories[category.id] = !isExpanded
                            } else {
                                // Select category
                                onCategorySelected(category)
                            }
                        }
                    )

                    // Show subcategories if expanded
                    if (hasSubcategories && isExpanded) {
                        subcategories.forEach { subcategory ->
                            SubcategoryPickerItem(
                                category = subcategory,
                                isSelected = subcategory.id == currentCategory?.id,
                                onClick = { onCategorySelected(subcategory) }
                            )
                        }
                    }
                }

                // "Create New Category" option
                if (onCreateNewCategory != null) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CreateNewCategoryItem(onClick = onCreateNewCategory)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Composable for displaying a root category item in the picker.
 *
 * @param category The category to display
 * @param isSelected Whether this category is currently selected
 * @param hasSubcategories Whether this category has subcategories
 * @param isExpanded Whether subcategories are currently visible (only relevant if hasSubcategories is true)
 * @param onClick Callback invoked when the item is clicked
 */
@Composable
private fun CategoryPickerItem(
    category: Category,
    isSelected: Boolean,
    hasSubcategories: Boolean = false,
    isExpanded: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                category.color.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expansion icon (if has subcategories)
            if (hasSubcategories) {
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.KeyboardArrowDown
                    } else {
                        Icons.Default.KeyboardArrowRight
                    },
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Category icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = category.color.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.icon,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Category name
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            // Selected indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = category.color
                )
            }
        }
    }
}

/**
 * Composable for displaying a subcategory item in the picker.
 *
 * Subcategories are visually indented to show their hierarchical relationship.
 *
 * @param category The subcategory to display
 * @param isSelected Whether this subcategory is currently selected
 * @param onClick Callback invoked when the item is clicked
 */
@Composable
private fun SubcategoryPickerItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp) // Indent to show hierarchy
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                category.color.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subcategory icon (smaller size)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = category.color.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.icon,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Subcategory name
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            // Selected indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = category.color,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Composable for displaying the "Create New Category" option.
 *
 * @param onClick Callback invoked when the item is clicked
 */
@Composable
private fun CreateNewCategoryItem(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text
            Text(
                text = "Create New Category",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
