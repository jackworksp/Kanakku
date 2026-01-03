package com.example.kanakku.ui.viewmodel

import androidx.compose.ui.graphics.Color
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
 * UI State for the category editor dialog/screen.
 *
 * @property name Category name input
 * @property nameError Error message for name field (null if valid)
 * @property icon Category icon (emoji)
 * @property iconError Error message for icon field (null if valid)
 * @property color Category color
 * @property keywords List of keywords for smart categorization
 * @property parentCategoryId Optional parent category ID for subcategories
 * @property parentCategoryName Display name of parent category (for UI display)
 * @property availableParentCategories List of categories that can be selected as parent
 * @property isEditMode True if editing existing category, false if creating new
 * @property editingCategoryId ID of category being edited (null for new category)
 * @property isSaving Whether save operation is in progress
 * @property errorMessage General error message to display
 * @property successMessage Success message after save operation
 * @property isValid Whether all fields pass validation
 */
data class CategoryEditorUiState(
    val name: String = "",
    val nameError: String? = null,
    val icon: String = "📦",
    val iconError: String? = null,
    val color: Color = Color.Gray,
    val keywords: List<String> = emptyList(),
    val parentCategoryId: Long? = null,
    val parentCategoryName: String? = null,
    val availableParentCategories: List<Category> = emptyList(),
    val isEditMode: Boolean = false,
    val editingCategoryId: Long? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isValid: Boolean = false
)

/**
 * ViewModel for the category editor dialog/screen.
 *
 * This ViewModel manages the state and validation logic for creating and editing
 * custom categories. It follows the offline-first architecture pattern with
 * comprehensive error handling and field validation.
 *
 * Key responsibilities:
 * - Manage category editor form state (name, icon, color, keywords, parent)
 * - Validate required fields with user-friendly error messages
 * - Load existing category data for editing
 * - Save new categories or update existing ones
 * - Provide list of available parent categories for hierarchy selection
 *
 * Validation Rules:
 * - Name: Required, non-blank, 1-50 characters
 * - Icon: Required, non-blank
 * - Color: Always valid (has default)
 * - Keywords: Optional
 * - Parent Category: Optional
 *
 * Error Handling Strategy:
 * - All repository operations return Result<T> for explicit error handling
 * - Errors are caught and converted to user-friendly messages via ErrorHandler
 * - Field-level validation errors are shown inline
 * - General errors are shown as error messages
 * - Success messages confirm save operations
 *
 * @param repository The CategoryRepository for database operations
 */
class CategoryEditorViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryEditorUiState())
    val uiState: StateFlow<CategoryEditorUiState> = _uiState.asStateFlow()

    init {
        // Load available parent categories on initialization
        loadAvailableParentCategories()
    }

    // ==================== Field Updates ====================

    /**
     * Updates the category name and validates it.
     * Clears name error if input becomes valid.
     *
     * @param name The new category name
     */
    fun setName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = validateName(name)
        )
        updateValidationState()
    }

    /**
     * Updates the category icon and validates it.
     * Clears icon error if input becomes valid.
     *
     * @param icon The new category icon (emoji)
     */
    fun setIcon(icon: String) {
        _uiState.value = _uiState.value.copy(
            icon = icon,
            iconError = validateIcon(icon)
        )
        updateValidationState()
    }

    /**
     * Updates the category color.
     * Color is always valid, so no validation needed.
     *
     * @param color The new category color
     */
    fun setColor(color: Color) {
        _uiState.value = _uiState.value.copy(color = color)
    }

    /**
     * Adds a keyword to the category's keyword list.
     * Validates and trims the keyword before adding.
     * Prevents duplicate keywords.
     *
     * @param keyword The keyword to add
     */
    fun addKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) {
            return
        }

        val currentKeywords = _uiState.value.keywords
        if (currentKeywords.contains(trimmed)) {
            // Keyword already exists, don't add duplicate
            return
        }

        _uiState.value = _uiState.value.copy(
            keywords = currentKeywords + trimmed
        )
    }

    /**
     * Removes a keyword from the category's keyword list.
     *
     * @param keyword The keyword to remove
     */
    fun removeKeyword(keyword: String) {
        _uiState.value = _uiState.value.copy(
            keywords = _uiState.value.keywords.filter { it != keyword }
        )
    }

    /**
     * Sets or clears the parent category.
     * Pass null for both parameters to create a root-level category.
     *
     * @param categoryId The parent category ID (null for root category)
     * @param categoryName The parent category name for display (null for root category)
     */
    fun setParentCategory(categoryId: Long?, categoryName: String?) {
        _uiState.value = _uiState.value.copy(
            parentCategoryId = categoryId,
            parentCategoryName = categoryName
        )
    }

    // ==================== Validation ====================

    /**
     * Validates the category name.
     *
     * Rules:
     * - Must not be blank
     * - Must be between 1 and 50 characters
     *
     * @param name The name to validate
     * @return Error message if invalid, null if valid
     */
    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Category name is required"
            name.length > 50 -> "Category name must be 50 characters or less"
            else -> null
        }
    }

    /**
     * Validates the category icon.
     *
     * Rules:
     * - Must not be blank
     *
     * @param icon The icon to validate
     * @return Error message if invalid, null if valid
     */
    private fun validateIcon(icon: String): String? {
        return when {
            icon.isBlank() -> "Category icon is required"
            else -> null
        }
    }

    /**
     * Updates the overall validation state based on individual field validations.
     * Sets isValid to true only if all required fields are valid.
     */
    private fun updateValidationState() {
        val state = _uiState.value
        val isValid = state.nameError == null &&
                state.iconError == null &&
                state.name.isNotBlank() &&
                state.icon.isNotBlank()

        _uiState.value = state.copy(isValid = isValid)
    }

    /**
     * Validates all fields and updates error states.
     * Should be called before attempting to save.
     *
     * @return True if all fields are valid, false otherwise
     */
    private fun validateAllFields(): Boolean {
        val nameError = validateName(_uiState.value.name)
        val iconError = validateIcon(_uiState.value.icon)

        _uiState.value = _uiState.value.copy(
            nameError = nameError,
            iconError = iconError
        )

        updateValidationState()

        return nameError == null && iconError == null
    }

    // ==================== Category Loading ====================

    /**
     * Loads available parent categories for the parent category picker.
     * Excludes the current category being edited to prevent circular references.
     * Only loads root-level categories that can be parents.
     *
     * Error Handling:
     * - Database read failures are logged but don't prevent editor usage
     * - Falls back to empty list if loading fails
     */
    private fun loadAvailableParentCategories() {
        viewModelScope.launch {
            try {
                val result = repository.getRootCategoriesSnapshot()
                result
                    .onSuccess { categories ->
                        // Exclude the current category being edited (if any) to prevent circular references
                        val editingId = _uiState.value.editingCategoryId
                        val filtered = if (editingId != null) {
                            categories.filter { it.id.toLongOrNull() != editingId }
                        } else {
                            categories
                        }

                        _uiState.value = _uiState.value.copy(
                            availableParentCategories = filtered
                        )

                        ErrorHandler.logDebug(
                            "Loaded ${filtered.size} available parent categories",
                            "CategoryEditorViewModel.loadAvailableParentCategories"
                        )
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        ErrorHandler.logWarning(
                            "Failed to load parent categories: ${errorInfo.technicalMessage}",
                            "CategoryEditorViewModel.loadAvailableParentCategories"
                        )
                        // Continue with empty list - not critical for editor functionality
                    }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "loadAvailableParentCategories")
                ErrorHandler.logWarning(
                    "Unexpected error loading parent categories: ${errorInfo.technicalMessage}",
                    "CategoryEditorViewModel"
                )
            }
        }
    }

    /**
     * Loads an existing category for editing.
     * Populates all fields with the category's current values.
     *
     * @param categoryId The ID of the category to edit
     *
     * Error Handling:
     * - Category not found errors are shown to the user
     * - Database read failures are shown as error messages
     */
    fun loadCategoryForEditing(categoryId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null
            )

            try {
                repository.getCategoryById(categoryId)
                    .onSuccess { category ->
                        if (category == null) {
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                errorMessage = "Category not found"
                            )
                            return@onSuccess
                        }

                        // Load parent category name if this is a subcategory
                        var parentName: String? = null
                        if (category.parentId != null) {
                            val parentIdLong = category.parentId.toLongOrNull()
                            if (parentIdLong != null) {
                                repository.getCategoryById(parentIdLong)
                                    .onSuccess { parent ->
                                        parentName = parent?.name
                                    }
                            }
                        }

                        _uiState.value = _uiState.value.copy(
                            name = category.name,
                            icon = category.icon,
                            color = category.color,
                            keywords = category.keywords.toList(), // Copy list
                            parentCategoryId = category.parentId?.toLongOrNull(),
                            parentCategoryName = parentName,
                            isEditMode = true,
                            editingCategoryId = categoryId,
                            isSaving = false,
                            nameError = null,
                            iconError = null
                        )

                        // Re-validate after loading
                        updateValidationState()

                        // Reload available parents to exclude this category
                        loadAvailableParentCategories()

                        ErrorHandler.logInfo(
                            "Loaded category '${category.name}' for editing",
                            "CategoryEditorViewModel.loadCategoryForEditing"
                        )
                    }
                    .onFailure { throwable ->
                        val errorInfo = throwable.toErrorInfo()
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = "Failed to load category: ${errorInfo.userMessage}"
                        )
                    }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "loadCategoryForEditing")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    // ==================== Save Operation ====================

    /**
     * Validates all fields and saves the category (create or update).
     * Shows validation errors if fields are invalid.
     * Shows success message on completion.
     *
     * This method should be called when the user clicks the Save button.
     * It will validate all fields first, then either create a new category
     * or update the existing one based on isEditMode.
     *
     * @param onSuccess Callback invoked after successful save with the category ID
     *
     * Error Handling:
     * - Validation errors are shown as field-level errors
     * - Database write failures are shown as error messages
     * - Success message is shown to confirm the operation
     */
    fun validateAndSave(onSuccess: (Long) -> Unit = {}) {
        // Validate all fields first
        if (!validateAllFields()) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                successMessage = null
            )

            try {
                val state = _uiState.value

                // Create Category object from form data
                val category = Category(
                    id = state.editingCategoryId?.toString() ?: "0", // Temporary ID for new categories
                    name = state.name.trim(),
                    icon = state.icon,
                    color = state.color,
                    keywords = state.keywords.toMutableList(),
                    parentId = state.parentCategoryId?.toString(),
                    isSystemCategory = false // User-created categories are never system categories
                )

                if (state.isEditMode && state.editingCategoryId != null) {
                    // Update existing category
                    repository.updateCategory(state.editingCategoryId, category)
                        .onSuccess { updated ->
                            if (updated) {
                                _uiState.value = _uiState.value.copy(
                                    isSaving = false,
                                    successMessage = "Category '${category.name}' updated successfully"
                                )
                                onSuccess(state.editingCategoryId)

                                ErrorHandler.logInfo(
                                    "Updated category '${category.name}' with ID: ${state.editingCategoryId}",
                                    "CategoryEditorViewModel.validateAndSave"
                                )
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isSaving = false,
                                    errorMessage = "Category not found or update failed"
                                )
                            }
                        }
                        .onFailure { throwable ->
                            val errorInfo = throwable.toErrorInfo()
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                errorMessage = "Failed to update category: ${errorInfo.userMessage}"
                            )
                        }
                } else {
                    // Create new category
                    repository.saveCategory(
                        category = category,
                        parentId = state.parentCategoryId,
                        isSystemCategory = false
                    )
                        .onSuccess { categoryId ->
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                successMessage = "Category '${category.name}' created successfully"
                            )
                            onSuccess(categoryId)

                            ErrorHandler.logInfo(
                                "Created category '${category.name}' with ID: $categoryId",
                                "CategoryEditorViewModel.validateAndSave"
                            )
                        }
                        .onFailure { throwable ->
                            val errorInfo = throwable.toErrorInfo()
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                errorMessage = "Failed to save category: ${errorInfo.userMessage}"
                            )
                        }
                }
            } catch (e: Exception) {
                // Catch-all for unexpected errors
                val errorInfo = ErrorHandler.handleError(e, "validateAndSave")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = errorInfo.userMessage
                )
            }
        }
    }

    // ==================== State Management ====================

    /**
     * Resets the editor state to default values.
     * Should be called when creating a new category or after successful save.
     */
    fun reset() {
        _uiState.value = CategoryEditorUiState(
            availableParentCategories = _uiState.value.availableParentCategories // Preserve loaded parent categories
        )
    }

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
