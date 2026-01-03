package com.example.kanakku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.ui.components.ColorPickerDialog
import com.example.kanakku.ui.components.IconPickerDialog
import com.example.kanakku.ui.components.KeywordChipInput
import com.example.kanakku.ui.viewmodel.CategoryEditorUiState

/**
 * Screen for creating and editing categories.
 *
 * Features:
 * - Name input with validation
 * - Icon picker dialog
 * - Color picker dialog
 * - Parent category selection
 * - Keywords input with chip display
 * - Save and Cancel buttons
 * - Error and success message handling
 * - Loading state during save operation
 *
 * Follows offline-first architecture with comprehensive error handling.
 *
 * @param uiState Current UI state from CategoryEditorViewModel
 * @param onNameChange Callback when category name changes
 * @param onIconChange Callback when category icon changes
 * @param onColorChange Callback when category color changes
 * @param onParentCategoryChange Callback when parent category changes (id, name)
 * @param onClearParentCategory Callback to clear parent category selection
 * @param onAddKeyword Callback to add a keyword
 * @param onRemoveKeyword Callback to remove a keyword
 * @param onSave Callback when Save button is clicked
 * @param onCancel Callback when Cancel or back button is clicked
 * @param onErrorDismiss Callback to dismiss error messages
 * @param onSuccessDismiss Callback to dismiss success messages
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorScreen(
    uiState: CategoryEditorUiState,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (Color) -> Unit,
    onParentCategoryChange: (Long?, String?) -> Unit,
    onClearParentCategory: () -> Unit,
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onErrorDismiss: () -> Unit,
    onSuccessDismiss: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showIconPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showParentPicker by remember { mutableStateOf(false) }

    // Show error message in Snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            onErrorDismiss()
        }
    }

    // Show success message in Snackbar
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            onSuccessDismiss()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Category" else "New Category",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Cancel"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name input field
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text("Category Name *") },
                placeholder = { Text("e.g., Groceries, Entertainment") },
                isError = uiState.nameError != null,
                supportingText = {
                    if (uiState.nameError != null) {
                        Text(
                            text = uiState.nameError,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Required • 1-50 characters",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Icon picker field
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Category Icon *",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.isSaving) { showIconPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = uiState.color.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.icon,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }

                            Column {
                                Text(
                                    text = "Select Icon",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Tap to change",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (uiState.iconError != null) {
                    Text(
                        text = uiState.iconError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Color picker field
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Category Color",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.isSaving) { showColorPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = uiState.color,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                            )

                            Column {
                                Text(
                                    text = "Select Color",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Tap to change",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Parent category picker field
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Parent Category (Optional)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.isSaving) { showParentPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = uiState.parentCategoryName ?: "None (Root Category)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (uiState.parentCategoryName != null) {
                                    FontWeight.Medium
                                } else {
                                    FontWeight.Normal
                                },
                                color = if (uiState.parentCategoryName != null) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = "Tap to select parent category",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.parentCategoryName != null) {
                            IconButton(
                                onClick = onClearParentCategory,
                                enabled = !uiState.isSaving
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear parent category",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Keywords input
            KeywordChipInput(
                keywords = uiState.keywords,
                onKeywordsChange = { newKeywords ->
                    // Handle keywords change by determining what was added or removed
                    val added = newKeywords.filterNot { it in uiState.keywords }
                    val removed = uiState.keywords.filterNot { it in newKeywords }

                    added.forEach { onAddKeyword(it) }
                    removed.forEach { onRemoveKeyword(it) }
                },
                label = "Keywords (Optional)",
                placeholder = "e.g., grocery, supermarket, food",
                maxKeywords = 20,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onSave,
                    enabled = uiState.isValid && !uiState.isSaving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (uiState.isEditMode) "Update" else "Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Icon picker dialog
    if (showIconPicker) {
        IconPickerDialog(
            currentIcon = uiState.icon,
            onIconSelected = { icon ->
                onIconChange(icon)
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }

    // Color picker dialog
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = uiState.color,
            onColorSelected = { color ->
                onColorChange(color)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    // Parent category picker dialog
    if (showParentPicker) {
        ParentCategoryPickerDialog(
            availableCategories = uiState.availableParentCategories,
            currentParentId = uiState.parentCategoryId,
            onCategorySelected = { category ->
                onParentCategoryChange(category.id.toLongOrNull(), category.name)
                showParentPicker = false
            },
            onDismiss = { showParentPicker = false }
        )
    }
}

/**
 * Dialog for selecting a parent category.
 *
 * @param availableCategories List of categories that can be selected as parent
 * @param currentParentId Currently selected parent category ID
 * @param onCategorySelected Callback when a category is selected
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
private fun ParentCategoryPickerDialog(
    availableCategories: List<com.example.kanakku.data.model.Category>,
    currentParentId: Long?,
    onCategorySelected: (com.example.kanakku.data.model.Category) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Parent Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (availableCategories.isEmpty()) {
                    Text(
                        text = "No parent categories available. Create a root category first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Text(
                        text = "Select a category to make this a subcategory:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    availableCategories.forEach { category ->
                        ParentCategoryItem(
                            category = category,
                            isSelected = category.id.toLongOrNull() == currentParentId,
                            onClick = { onCategorySelected(category) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Individual item in the parent category picker dialog.
 *
 * @param category The category to display
 * @param isSelected Whether this category is currently selected
 * @param onClick Callback when the item is clicked
 */
@Composable
private fun ParentCategoryItem(
    category: com.example.kanakku.data.model.Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
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

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
