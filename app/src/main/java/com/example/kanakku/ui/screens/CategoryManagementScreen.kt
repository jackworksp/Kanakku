package com.example.kanakku.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.Category
import com.example.kanakku.ui.components.CategoryListItem
import com.example.kanakku.ui.viewmodel.CategoryManagementUiState

/**
 * Main screen for managing categories.
 *
 * Features:
 * - Search categories by name
 * - Filter categories by parent
 * - Swipe to delete with confirmation
 * - FAB to add new category
 * - Edit existing categories
 * - Display hierarchical categories (root and subcategories)
 *
 * Follows offline-first architecture with comprehensive error handling.
 *
 * @param uiState Current UI state from CategoryManagementViewModel
 * @param onSearchQueryChange Callback when search query changes
 * @param onFilterParentChange Callback when parent filter changes
 * @param onClearFilter Callback to clear parent filter
 * @param onAddCategoryClick Callback when FAB is clicked to add new category
 * @param onEditCategory Callback when edit button is clicked on a category
 * @param onDeleteCategory Callback when delete button is clicked on a category (shows confirmation)
 * @param onConfirmDelete Callback when delete is confirmed in the dialog
 * @param onCancelDelete Callback when delete is cancelled in the dialog
 * @param onCategoryClick Optional callback when a category item is clicked
 * @param onErrorDismiss Callback to dismiss error messages
 * @param onSuccessDismiss Callback to dismiss success messages
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    uiState: CategoryManagementUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterParentChange: (Category) -> Unit,
    onClearFilter: () -> Unit,
    onAddCategoryClick: () -> Unit,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onCategoryClick: ((Category) -> Unit)? = null,
    onErrorDismiss: () -> Unit,
    onSuccessDismiss: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCategoryClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new category"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header with title
            CategoryManagementHeader()

            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter by parent (optional)
            FilterSection(
                filterParentId = uiState.filterParentId,
                rootCategories = uiState.rootCategories,
                onFilterChange = onFilterParentChange,
                onClearFilter = onClearFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Loading indicator
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Categories list
            if (!uiState.isLoading) {
                CategoriesList(
                    categories = getFilteredCategories(uiState),
                    allCategories = uiState.categories,
                    onEditCategory = onEditCategory,
                    onDeleteCategory = onDeleteCategory,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirmation && uiState.categoryToDelete != null) {
        DeleteConfirmationDialog(
            category = uiState.categoryToDelete,
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete
        )
    }
}

/**
 * Header section with title and description.
 */
@Composable
private fun CategoryManagementHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Manage Categories",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Create, edit, and organize your custom categories",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Search bar for filtering categories by name.
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search categories...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * Filter section for filtering categories by parent.
 */
@Composable
private fun FilterSection(
    filterParentId: Long?,
    rootCategories: List<Category>,
    onFilterChange: (Category) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Filter by parent:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (filterParentId != null) {
                Button(
                    onClick = onClearFilter,
                    colors = ButtonDefaults.textButtonColors()
                ) {
                    Text("Clear filter")
                }
            }
        }

        if (filterParentId == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                Button(
                    onClick = { showFilterMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select parent category")
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    rootCategories.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = category.icon)
                                    Text(text = category.name)
                                }
                            },
                            onClick = {
                                onFilterChange(category)
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * List of categories with swipe-to-delete functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesList(
    categories: List<Category>,
    allCategories: List<Category>,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onCategoryClick: ((Category) -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) {
        EmptyState(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = categories,
                key = { it.id }
            ) { category ->
                var showItem by remember { mutableStateOf(true) }

                AnimatedVisibility(
                    visible = showItem,
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut()
                ) {
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        // Swipe left to delete
                                        onDeleteCategory(category)
                                        showItem = false
                                        true
                                    }
                                    else -> false
                                }
                            }
                        ),
                        backgroundContent = {
                            SwipeBackground()
                        }
                    ) {
                        val subcategoryCount = allCategories.count { it.parentId == category.id }
                        CategoryListItem(
                            category = category,
                            subcategoryCount = subcategoryCount,
                            onEditClick = { onEditCategory(category) },
                            onDeleteClick = { onDeleteCategory(category) },
                            onClick = onCategoryClick?.let { { it(category) } }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB
            }
        }
    }
}

/**
 * Background shown when swiping to delete.
 */
@Composable
private fun SwipeBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

/**
 * Empty state when no categories are found.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📦",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "No categories found",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Tap + to create your first custom category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Delete confirmation dialog.
 */
@Composable
private fun DeleteConfirmationDialog(
    category: Category,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Delete Category?")
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete '${category.name}'?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This will also delete all subcategories and cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Filters categories based on current UI state (search query and parent filter).
 */
private fun getFilteredCategories(uiState: CategoryManagementUiState): List<Category> {
    var filtered = uiState.categories

    // Apply parent filter
    if (uiState.filterParentId != null) {
        filtered = filtered.filter { it.parentId == uiState.filterParentId.toString() }
    }

    // Apply search filter
    if (uiState.searchQuery.isNotEmpty()) {
        filtered = filtered.filter { category ->
            category.name.contains(uiState.searchQuery, ignoreCase = true) ||
            category.keywords.any { it.contains(uiState.searchQuery, ignoreCase = true) }
        }
    }

    return filtered
}
