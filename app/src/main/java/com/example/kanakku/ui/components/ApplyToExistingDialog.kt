package com.example.kanakku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.Category

/**
 * Operation type for category changes that affect existing transactions.
 */
enum class CategoryOperationType {
    /** Renaming a category */
    RENAME,
    /** Deleting a category */
    DELETE
}

/**
 * Action to take on existing transactions when a category is modified.
 */
enum class ExistingTransactionAction {
    /** Apply changes to all existing transactions (rename) or reassign to a new category (delete) */
    APPLY_TO_ALL,
    /** Keep existing transaction assignments unchanged */
    KEEP_EXISTING,
    /** Select specific transactions to update (future enhancement) */
    SELECT_SPECIFIC
}

/**
 * Configuration for what to do with existing transactions affected by a category change.
 *
 * @property action The action to take on existing transactions
 * @property replacementCategory The category to use as replacement (only for DELETE with APPLY_TO_ALL)
 */
data class ExistingTransactionConfig(
    val action: ExistingTransactionAction,
    val replacementCategory: Category? = null
)

/**
 * Confirmation dialog shown when renaming or deleting a category that has existing transactions.
 *
 * This dialog provides options for handling existing transactions:
 * - **For RENAME**: Apply the new name to all transactions or keep them with the old category
 * - **For DELETE**: Reassign transactions to a different category or leave them uncategorized
 *
 * The dialog shows the count of affected transactions and provides clear options for each scenario.
 *
 * @param operationType Whether this is a RENAME or DELETE operation
 * @param category The category being renamed or deleted
 * @param newCategoryName The new name (only for RENAME operations)
 * @param affectedTransactionCount Number of transactions currently using this category
 * @param availableCategories List of categories to choose from as replacement (for DELETE only)
 * @param onConfirm Callback when confirmed, receives the configuration for handling existing transactions
 * @param onDismiss Callback when the dialog is dismissed or cancelled
 */
@Composable
fun ApplyToExistingDialog(
    operationType: CategoryOperationType,
    category: Category,
    newCategoryName: String? = null,
    affectedTransactionCount: Int,
    availableCategories: List<Category> = emptyList(),
    onConfirm: (ExistingTransactionConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAction by remember { mutableStateOf(ExistingTransactionAction.APPLY_TO_ALL) }
    var selectedReplacementCategory by remember { mutableStateOf<Category?>(null) }
    var showReplacementPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = category.color.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.icon,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        },
        title = {
            Text(
                text = when (operationType) {
                    CategoryOperationType.RENAME -> "Rename Category"
                    CategoryOperationType.DELETE -> "Delete Category"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Operation description
                Text(
                    text = when (operationType) {
                        CategoryOperationType.RENAME ->
                            "You're renaming \"${category.name}\" to \"$newCategoryName\""
                        CategoryOperationType.DELETE ->
                            "You're about to delete \"${category.name}\""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                // Affected transactions warning
                if (affectedTransactionCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⚠️ $affectedTransactionCount transaction${if (affectedTransactionCount != 1) "s are" else " is"} currently using this category",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Text(
                        text = "What would you like to do with these transactions?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Options for handling existing transactions
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 1: Apply changes to all
                        OptionCard(
                            isSelected = selectedAction == ExistingTransactionAction.APPLY_TO_ALL,
                            onClick = { selectedAction = ExistingTransactionAction.APPLY_TO_ALL },
                            title = when (operationType) {
                                CategoryOperationType.RENAME -> "Update all transactions"
                                CategoryOperationType.DELETE -> "Reassign to another category"
                            },
                            description = when (operationType) {
                                CategoryOperationType.RENAME ->
                                    "All $affectedTransactionCount transaction${if (affectedTransactionCount != 1) "s" else ""} will use the new name"
                                CategoryOperationType.DELETE ->
                                    "Choose a replacement category for these transactions"
                            }
                        )

                        // Replacement category picker (for DELETE only)
                        if (operationType == CategoryOperationType.DELETE &&
                            selectedAction == ExistingTransactionAction.APPLY_TO_ALL) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showReplacementPicker = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Replacement Category",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = selectedReplacementCategory?.name ?: "Select a category",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (selectedReplacementCategory != null)
                                                FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selectedReplacementCategory != null)
                                                MaterialTheme.colorScheme.onSurface
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    selectedReplacementCategory?.let { replacement ->
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    color = replacement.color.copy(alpha = 0.2f),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = replacement.icon,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Option 2: Keep existing
                        OptionCard(
                            isSelected = selectedAction == ExistingTransactionAction.KEEP_EXISTING,
                            onClick = { selectedAction = ExistingTransactionAction.KEEP_EXISTING },
                            title = when (operationType) {
                                CategoryOperationType.RENAME -> "Keep existing assignments"
                                CategoryOperationType.DELETE -> "Leave uncategorized"
                            },
                            description = when (operationType) {
                                CategoryOperationType.RENAME ->
                                    "Existing transactions will keep the old category name"
                                CategoryOperationType.DELETE ->
                                    "Transactions will be marked as uncategorized"
                            }
                        )

                        // Option 3: Select specific (future enhancement - disabled)
                        OptionCard(
                            isSelected = selectedAction == ExistingTransactionAction.SELECT_SPECIFIC,
                            onClick = { /* TODO: Implement in future */ },
                            title = "Select specific transactions",
                            description = "Choose which transactions to update (coming soon)",
                            isEnabled = false
                        )
                    }
                } else {
                    // No affected transactions
                    Text(
                        text = when (operationType) {
                            CategoryOperationType.RENAME ->
                                "No transactions are currently using this category. The rename will only affect future categorizations."
                            CategoryOperationType.DELETE ->
                                "No transactions are currently using this category. It's safe to delete."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val config = ExistingTransactionConfig(
                        action = if (affectedTransactionCount > 0) selectedAction else ExistingTransactionAction.KEEP_EXISTING,
                        replacementCategory = if (operationType == CategoryOperationType.DELETE &&
                            selectedAction == ExistingTransactionAction.APPLY_TO_ALL)
                            selectedReplacementCategory
                        else null
                    )
                    onConfirm(config)
                },
                enabled = if (operationType == CategoryOperationType.DELETE &&
                    selectedAction == ExistingTransactionAction.APPLY_TO_ALL &&
                    affectedTransactionCount > 0) {
                    selectedReplacementCategory != null
                } else {
                    true
                }
            ) {
                Text(
                    text = when (operationType) {
                        CategoryOperationType.RENAME -> "Rename"
                        CategoryOperationType.DELETE -> "Delete"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )

    // Replacement category picker dialog
    if (showReplacementPicker) {
        ReplacementCategoryPickerDialog(
            categories = availableCategories.filter { it.id != category.id },
            selectedCategory = selectedReplacementCategory,
            onCategorySelected = { selected ->
                selectedReplacementCategory = selected
                showReplacementPicker = false
            },
            onDismiss = { showReplacementPicker = false }
        )
    }
}

/**
 * A clickable card representing an option for handling existing transactions.
 *
 * @param isSelected Whether this option is currently selected
 * @param onClick Callback when the option is clicked
 * @param title The main title of the option
 * @param description Additional details about what this option does
 * @param isEnabled Whether this option is clickable (default true)
 */
@Composable
private fun OptionCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String,
    isEnabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else null,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection indicator
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = when {
                    !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )

            // Option details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = when {
                        !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Dialog for selecting a replacement category when deleting a category with existing transactions.
 *
 * @param categories List of available categories to choose from
 * @param selectedCategory Currently selected replacement category
 * @param onCategorySelected Callback when a category is selected
 * @param onDismiss Callback to dismiss the dialog
 */
@Composable
private fun ReplacementCategoryPickerDialog(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Replacement Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (categories.isEmpty()) {
                Text(
                    text = "No other categories available. Please create a category first or choose to leave transactions uncategorized.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(categories) { category ->
                        ReplacementCategoryItem(
                            category = category,
                            isSelected = category.id == selectedCategory?.id,
                            onClick = { onCategorySelected(category) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
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
 * Individual category item in the replacement category picker.
 *
 * @param category The category to display
 * @param isSelected Whether this category is currently selected
 * @param onClick Callback when this category is clicked
 */
@Composable
private fun ReplacementCategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            // Category name
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
