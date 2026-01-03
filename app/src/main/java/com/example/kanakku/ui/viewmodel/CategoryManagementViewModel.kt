package com.example.kanakku.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.core.error.toErrorInfo
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for the category management screen.
 *
 * @property isLoading Whether the screen is currently loading data
 * @property categories List of all categories to display
 * @property rootCategories List of root-level categories (no parent)
 * @property selectedCategory Currently selected category for viewing/editing (null if none selected)
 * @property isEditMode Whether the UI is in edit mode (reordering, bulk operations, etc.)
 * @property showDeleteConfirmation Whether to show delete confirmation dialog
 * @property categoryToDelete Category pending deletion (shown in confirmation dialog)
 * @property errorMessage User-friendly error message to display (null if no error)
 * @property successMessage User-friendly success message to display (null if no message)
 * @property searchQuery Current search query for filtering categories
 * @property filterParentId Optional parent ID for filtering subcategories (null shows all)
 */
data class CategoryManagementUiState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val rootCategories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val isEditMode: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val categoryToDelete: Category? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val filterParentId: Long? = null
)

/**
 * ViewModel for the category management screen.
 *
 * This ViewModel manages the state and business logic for viewing, creating,
 * editing, and deleting custom categories. It follows the offline-first
 * architecture pattern with comprehensive error handling.
 *
 * Key responsibilities:
 * - Load and display categories from the database
 * - Handle category CRUD operations (Create, Read, Update, Delete)
 * - Manage UI state for edit mode, selection, and confirmation dialogs
 * - Support category reordering via drag-and-drop
 * - Filter and search categories
 * - Provide user-friendly error and success messages
 *
 * Error Handling Strategy:
 * - All repository operations return Result<T> for explicit error handling
 * - Errors are caught and converted to user-friendly messages via ErrorHandler
 * - Loading states are managed to prevent UI inconsistencies
 * - Success messages are shown for user confirmation of operations
 *
 * @param repository The CategoryRepository for database operations
 */
class CategoryManagementViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState: StateFlow<CategoryManagementUiState> = _uiState.asStateFlow()

    init {
        // Load categories on initialization
        loadCategories()
    }

    // ==================== Category Loading ====================

    /**
     * Loads all categories from the database.
     * Also loads root-level categories separately for hierarchy display.
     *
     * Error Handling:
     * - Database read failures are caught and shown as error messages
     * - Partial results are supported (root categories may load even if all categories fail)
     * - Loading state is managed to prevent UI flickering
     */
    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Load all categories
                val allCategoriesResult = repository.getAllCategoriesSnapshot()
                val allCategories = allCategoriesResult
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load categories: ${errorInfo.userMessage}"
                        )
                        return@launch
                    }
                    .getOrElse { emptyList() }

                // Load root categories separately for hierarchy display
                val rootCategoriesResult = repository.getRootCategoriesSnapshot()
                val rootCategories = rootCategoriesResult
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to load root categories: ${errorInfo.technicalMessage}",
                            "CategoryManagementViewModel.loadCategories"
                        )
                        // Continue with empty list - not critical
                    }
                    .getOrElse { emptyList() }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    categories = allCategories,
                    rootCategories = rootCategories
                )

                ErrorHandler.logInfo(
                    "Loaded ${allCategories.size} categories (${rootCategories.size} root)",
                    "CategoryManagementViewModel.loadCategories"
                )
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "loadCategories")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Loads subcategories for a specific parent category.
     * Updates the categories list to show only subcategories.
     *
     * @param parentId The ID of the parent category
     */
    fun loadSubcategories(parentId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val subcategoriesResult = repository.getSubcategoriesSnapshot(parentId)
                subcategoriesResult
                    .onSuccess { subcategories ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            categories = subcategories,
                            filterParentId = parentId
                        )
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load subcategories: ${errorInfo.userMessage}"
                        )
                    }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "loadSubcategories")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Clears the parent filter and reloads all categories.
     */
    fun clearParentFilter() {
        _uiState.value = _uiState.value.copy(filterParentId = null)
        loadCategories()
    }

    // ==================== Category CRUD Operations ====================

    /**
     * Saves a new category to the database.
     * Shows success message on completion and reloads categories.
     *
     * @param category The category to save
     * @param parentId Optional parent category ID for subcategories
     * @param isSystemCategory Whether this is a system (default) category
     *
     * Error Handling:
     * - Database write failures are caught and shown as error messages
     * - Success message is shown to confirm the operation
     * - Categories are reloaded to reflect the new category
     */
    fun saveCategory(
        category: Category,
        parentId: Long? = null,
        isSystemCategory: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                repository.saveCategory(category, parentId, isSystemCategory)
                    .onSuccess { categoryId ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Category '${category.name}' created successfully"
                        )
                        // Reload categories to reflect the new category
                        loadCategories()

                        ErrorHandler.logInfo(
                            "Saved category '${category.name}' with ID: $categoryId",
                            "CategoryManagementViewModel.saveCategory"
                        )
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to save category: ${errorInfo.userMessage}"
                        )
                    }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "saveCategory")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Updates an existing category in the database.
     * Shows success message on completion and reloads categories.
     *
     * @param categoryId The ID of the category to update
     * @param category The updated category data
     *
     * Error Handling:
     * - Database write failures are caught and shown as error messages
     * - Success message is shown to confirm the operation
     * - Categories are reloaded to reflect the changes
     */
    fun updateCategory(categoryId: Long, category: Category) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                repository.updateCategory(categoryId, category)
                    .onSuccess { updated ->
                        if (updated) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                successMessage = "Category '${category.name}' updated successfully"
                            )
                            // Reload categories to reflect the changes
                            loadCategories()

                            ErrorHandler.logInfo(
                                "Updated category '${category.name}' with ID: $categoryId",
                                "CategoryManagementViewModel.updateCategory"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Category not found or update failed"
                            )
                        }
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to update category: ${errorInfo.userMessage}"
                        )
                    }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "updateCategory")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    /**
     * Initiates the delete confirmation flow for a category.
     * Shows the delete confirmation dialog with the category to delete.
     *
     * @param category The category to delete
     */
    fun requestDeleteCategory(category: Category) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true,
            categoryToDelete = category
        )
    }

    /**
     * Cancels the delete operation and hides the confirmation dialog.
     */
    fun cancelDeleteCategory() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = false,
            categoryToDelete = null
        )
    }

    /**
     * Confirms and executes the delete operation for the pending category.
     * Shows success message on completion and reloads categories.
     *
     * Error Handling:
     * - Database delete failures are caught and shown as error messages
     * - Success message is shown to confirm the operation
     * - Categories are reloaded to reflect the deletion
     * - Cascade delete of subcategories is handled by the database
     */
    fun confirmDeleteCategory() {
        viewModelScope.launch {
            val category = _uiState.value.categoryToDelete
            if (category == null) {
                _uiState.value = _uiState.value.copy(
                    showDeleteConfirmation = false,
                    errorMessage = "No category selected for deletion"
                )
                return@launch
            }

            // Close confirmation dialog immediately
            _uiState.value = _uiState.value.copy(
                showDeleteConfirmation = false,
                categoryToDelete = null,
                isLoading = true,
                errorMessage = null
            )

            try {
                // Parse category ID (stored as String in domain model)
                val categoryId = category.id.toLongOrNull()
                if (categoryId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Invalid category ID"
                    )
                    return@launch
                }

                repository.deleteCategory(categoryId)
                    .onSuccess { deleted ->
                        if (deleted) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                successMessage = "Category '${category.name}' deleted successfully"
                            )
                            // Reload categories to reflect the deletion
                            loadCategories()

                            ErrorHandler.logInfo(
                                "Deleted category '${category.name}' with ID: $categoryId",
                                "CategoryManagementViewModel.confirmDeleteCategory"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "Category not found or delete failed"
                            )
                        }
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to delete category: ${errorInfo.userMessage}"
                        )
                    }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "confirmDeleteCategory")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    // ==================== Category Selection & UI State ====================

    /**
     * Selects a category for viewing or editing.
     *
     * @param category The category to select (null to clear selection)
     */
    fun selectCategory(category: Category?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    /**
     * Toggles edit mode on/off.
     * Edit mode enables features like reordering and bulk operations.
     *
     * @param enabled Whether to enable edit mode
     */
    fun setEditMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isEditMode = enabled)
    }

    /**
     * Updates the search query for filtering categories.
     * Note: Actual filtering should be done in the UI layer or via repository search.
     *
     * @param query The search query
     */
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    // ==================== Category Reordering ====================

    /**
     * Reorders categories based on drag-and-drop operations.
     * This updates the sortOrder field in the database to persist the new order.
     *
     * Note: The actual reordering logic depends on the UI implementation.
     * This method should be called with the new ordered list of categories.
     *
     * @param reorderedCategories The categories in their new order
     *
     * TODO: Implement database update for sortOrder when drag-and-drop UI is ready
     */
    fun reorderCategories(reorderedCategories: List<Category>) {
        viewModelScope.launch {
            try {
                // Update UI state optimistically
                _uiState.value = _uiState.value.copy(categories = reorderedCategories)

                // TODO: Implement batch update of sortOrder in CategoryRepository
                // For now, just update the UI state
                ErrorHandler.logInfo(
                    "Categories reordered (UI only, database update pending)",
                    "CategoryManagementViewModel.reorderCategories"
                )
            } catch (e: Exception) {
                val errorInfo = ErrorHandler.handleError(e, "reorderCategories")
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorInfo.userMessage
                )
                // Reload categories to revert to database order
                loadCategories()
            }
        }
    }

    // ==================== Message Management ====================

    /**
     * Clears any error message from the UI state.
     * Should be called after the user has acknowledged the error.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Clears any success message from the UI state.
     * Should be called after the user has acknowledged the success message.
     */
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
