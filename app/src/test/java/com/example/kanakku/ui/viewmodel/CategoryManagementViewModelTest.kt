package com.example.kanakku.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive unit tests for CategoryManagementViewModel.
 *
 * Tests cover:
 * - State management (initial state, state updates)
 * - Category CRUD operations (save, update, delete)
 * - Error handling and error states
 * - Loading states during async operations
 * - Success and error message management
 * - Category selection and UI state management
 * - Delete confirmation flow
 * - Search and filter operations
 * - Category loading (all categories, subcategories, root categories)
 * - Cascade delete handling
 * - Edge cases and error recovery
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CategoryManagementViewModelTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var repository: CategoryRepository
    private lateinit var viewModel: CategoryManagementViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // Set main dispatcher for testing
        Dispatchers.setMain(testDispatcher)

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        repository = CategoryRepository(database)
        viewModel = CategoryManagementViewModel(repository)
    }

    @After
    fun teardown() {
        database.close()
        testDispatcher.cancel()
    }

    // ==================== Helper Functions ====================

    private fun createTestCategory(
        id: String = "test_category",
        name: String = "Test Category",
        icon: String = "🏷️",
        color: Color = Color(0xFFFF5722),
        keywords: MutableList<String> = mutableListOf("test"),
        parentId: String? = null,
        isSystemCategory: Boolean = false
    ): Category {
        return Category(
            id = id,
            name = name,
            icon = icon,
            color = color,
            keywords = keywords,
            parentId = parentId,
            isSystemCategory = isSystemCategory
        )
    }

    private suspend fun seedTestCategories(): List<Long> {
        val foodCategory = createTestCategory(
            id = "food",
            name = "Food",
            keywords = mutableListOf("food", "restaurant")
        )
        val shoppingCategory = createTestCategory(
            id = "shopping",
            name = "Shopping",
            keywords = mutableListOf("shop", "store")
        )
        val transportCategory = createTestCategory(
            id = "transport",
            name = "Transport",
            keywords = mutableListOf("taxi", "fuel")
        )

        val foodId = repository.saveCategory(foodCategory).getOrNull()!!
        val shoppingId = repository.saveCategory(shoppingCategory).getOrNull()!!
        val transportId = repository.saveCategory(transportCategory).getOrNull()!!

        return listOf(foodId, shoppingId, transportId)
    }

    // ==================== Initial State Tests ====================

    @Test
    fun initialState_hasDefaultValues() = runTest {
        // Given - ViewModel just created

        // When
        advanceUntilIdle() // Wait for init block to complete

        val state = viewModel.uiState.value

        // Then - Should have loaded categories from empty database
        assertFalse(state.isLoading)
        assertTrue(state.categories.isEmpty())
        assertTrue(state.rootCategories.isEmpty())
        assertNull(state.selectedCategory)
        assertFalse(state.isEditMode)
        assertFalse(state.showDeleteConfirmation)
        assertNull(state.categoryToDelete)
        assertEquals("", state.searchQuery)
        assertNull(state.filterParentId)
    }

    @Test
    fun initialState_loadsCategoriesAutomatically() = runTest {
        // Given - Categories in database
        seedTestCategories()

        // When - Create new ViewModel
        val newViewModel = CategoryManagementViewModel(repository)
        advanceUntilIdle()

        val state = newViewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertEquals(3, state.categories.size)
        assertEquals(3, state.rootCategories.size) // All are root categories
    }

    // ==================== Category Loading Tests ====================

    @Test
    fun loadCategories_loadsAllCategories() = runTest {
        // Given
        seedTestCategories()

        // When
        viewModel.loadCategories()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertEquals(3, state.categories.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun loadCategories_loadsRootCategories() = runTest {
        // Given
        val categoryIds = seedTestCategories()

        // Add a subcategory
        val subcategory = createTestCategory(
            id = "groceries",
            name = "Groceries",
            parentId = categoryIds[0].toString()
        )
        repository.saveCategory(subcategory, parentId = categoryIds[0])

        // When
        viewModel.loadCategories()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertEquals(4, state.categories.size) // 3 root + 1 subcategory
        assertEquals(3, state.rootCategories.size) // Only root categories
    }

    @Test
    fun loadCategories_setsLoadingState() = runTest {
        // Given
        seedTestCategories()

        // When
        viewModel.loadCategories()

        // Then - Loading state should be true initially
        val stateBeforeCompletion = viewModel.uiState.value
        assertTrue(stateBeforeCompletion.isLoading)

        advanceUntilIdle()

        // Then - Loading state should be false after completion
        val stateAfterCompletion = viewModel.uiState.value
        assertFalse(stateAfterCompletion.isLoading)
    }

    @Test
    fun loadCategories_clearsErrorMessage() = runTest {
        // Given - ViewModel with error message
        seedTestCategories()
        viewModel.loadCategories()
        advanceUntilIdle()

        // Trigger an error by closing database
        database.close()
        viewModel.loadCategories()
        advanceUntilIdle()

        // Verify error exists
        assertNotNull(viewModel.uiState.value.errorMessage)

        // Reopen database
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = CategoryRepository(database)
        viewModel = CategoryManagementViewModel(repository)
        seedTestCategories()

        // When
        viewModel.loadCategories()
        advanceUntilIdle()

        // Then - Error should be cleared
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loadCategories_handlesEmptyDatabase() = runTest {
        // Given - Empty database

        // When
        viewModel.loadCategories()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertTrue(state.categories.isEmpty())
        assertTrue(state.rootCategories.isEmpty())
        assertNull(state.errorMessage)
    }

    @Test
    fun loadCategories_handlesDatabaseError() = runTest {
        // Given - Closed database to simulate error
        database.close()

        // When
        viewModel.loadCategories()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Failed to load categories"))
    }

    // ==================== Subcategory Loading Tests ====================

    @Test
    fun loadSubcategories_loadsSubcategoriesForParent() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        val foodId = categoryIds[0]

        // Add subcategories to Food
        val groceries = createTestCategory(id = "groceries", name = "Groceries")
        val restaurants = createTestCategory(id = "restaurants", name = "Restaurants")
        repository.saveCategory(groceries, parentId = foodId)
        repository.saveCategory(restaurants, parentId = foodId)

        // When
        viewModel.loadSubcategories(foodId)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertEquals(2, state.categories.size)
        assertEquals(foodId, state.filterParentId)
        assertNull(state.errorMessage)
    }

    @Test
    fun loadSubcategories_setsFilterParentId() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        val foodId = categoryIds[0]

        // When
        viewModel.loadSubcategories(foodId)
        advanceUntilIdle()

        // Then
        assertEquals(foodId, viewModel.uiState.value.filterParentId)
    }

    @Test
    fun loadSubcategories_handlesNoSubcategories() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        val foodId = categoryIds[0]
        // No subcategories added

        // When
        viewModel.loadSubcategories(foodId)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertTrue(state.categories.isEmpty())
        assertEquals(foodId, state.filterParentId)
        assertNull(state.errorMessage)
    }

    @Test
    fun loadSubcategories_handlesDatabaseError() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        database.close()

        // When
        viewModel.loadSubcategories(categoryIds[0])
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Failed to load subcategories"))
    }

    @Test
    fun clearParentFilter_resetsFilterAndReloadsCategories() = runTest {
        // Given - Filter set
        val categoryIds = seedTestCategories()
        viewModel.loadSubcategories(categoryIds[0])
        advanceUntilIdle()

        assertEquals(categoryIds[0], viewModel.uiState.value.filterParentId)

        // When
        viewModel.clearParentFilter()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertNull(state.filterParentId)
        assertEquals(3, state.categories.size) // All categories loaded
    }

    // ==================== Category Save Tests ====================

    @Test
    fun saveCategory_savesSuccessfully() = runTest {
        // Given
        val newCategory = createTestCategory(name = "New Category")

        // When
        viewModel.saveCategory(newCategory)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("created successfully"))
        assertEquals(1, state.categories.size)
    }

    @Test
    fun saveCategory_withParentId_savesAsSubcategory() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        val foodId = categoryIds[0]
        val newSubcategory = createTestCategory(name = "Groceries")

        // When
        viewModel.saveCategory(newSubcategory, parentId = foodId)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("created successfully"))

        // Verify subcategory was saved
        val subcategories = repository.getSubcategoriesSnapshot(foodId).getOrNull()!!
        assertEquals(1, subcategories.size)
        assertEquals("Groceries", subcategories[0].name)
    }

    @Test
    fun saveCategory_asSystemCategory_savesWithFlag() = runTest {
        // Given
        val systemCategory = createTestCategory(name = "System Category", isSystemCategory = true)

        // When
        viewModel.saveCategory(systemCategory, isSystemCategory = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertNotNull(state.successMessage)

        // Verify system flag was set
        val allCategories = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals(1, allCategories.size)
        assertTrue(allCategories[0].isSystemCategory)
    }

    @Test
    fun saveCategory_setsLoadingState() = runTest {
        // Given
        val newCategory = createTestCategory(name = "New Category")

        // When
        viewModel.saveCategory(newCategory)

        // Then - Should be loading initially
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        // Then - Should not be loading after completion
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun saveCategory_reloadsCategories() = runTest {
        // Given
        seedTestCategories()
        viewModel.loadCategories()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.categories.size)

        val newCategory = createTestCategory(name = "New Category")

        // When
        viewModel.saveCategory(newCategory)
        advanceUntilIdle()

        // Then - Categories should be reloaded
        assertEquals(4, viewModel.uiState.value.categories.size)
    }

    @Test
    fun saveCategory_handlesDatabaseError() = runTest {
        // Given
        database.close()
        val newCategory = createTestCategory(name = "New Category")

        // When
        viewModel.saveCategory(newCategory)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Failed to save category"))
        assertNull(state.successMessage)
    }

    // ==================== Category Update Tests ====================

    @Test
    fun updateCategory_updatesSuccessfully() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        val foodId = categoryIds[0]
        val updatedCategory = createTestCategory(
            id = "food",
            name = "Updated Food Name",
            keywords = mutableListOf("updated", "keywords")
        )

        // When
        viewModel.updateCategory(foodId, updatedCategory)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("updated successfully"))

        // Verify update
        val category = repository.getCategoryById(foodId).first()
        assertEquals("Updated Food Name", category?.name)
    }

    @Test
    fun updateCategory_setsLoadingState() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        val updatedCategory = createTestCategory(name = "Updated Name")

        // When
        viewModel.updateCategory(categoryIds[0], updatedCategory)

        // Then - Should be loading initially
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        // Then - Should not be loading after completion
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun updateCategory_reloadsCategories() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        viewModel.loadCategories()
        advanceUntilIdle()

        val originalName = viewModel.uiState.value.categories[0].name
        val updatedCategory = createTestCategory(name = "Updated Name")

        // When
        viewModel.updateCategory(categoryIds[0], updatedCategory)
        advanceUntilIdle()

        // Then - Categories should be reloaded with updated data
        val newName = viewModel.uiState.value.categories.find {
            it.id == categoryIds[0].toString()
        }?.name
        assertEquals("Updated Name", newName)
        assertNotEquals(originalName, newName)
    }

    @Test
    fun updateCategory_handlesNonExistentCategory() = runTest {
        // Given
        val nonExistentId = 999L
        val updatedCategory = createTestCategory(name = "Updated Name")

        // When
        viewModel.updateCategory(nonExistentId, updatedCategory)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("not found") ||
                   state.errorMessage!!.contains("update failed"))
        assertNull(state.successMessage)
    }

    @Test
    fun updateCategory_handlesDatabaseError() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        database.close()
        val updatedCategory = createTestCategory(name = "Updated Name")

        // When
        viewModel.updateCategory(categoryIds[0], updatedCategory)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Failed to update category"))
        assertNull(state.successMessage)
    }

    // ==================== Delete Confirmation Flow Tests ====================

    @Test
    fun requestDeleteCategory_showsConfirmationDialog() = runTest {
        // Given
        val category = createTestCategory(name = "Test Category")

        // When
        viewModel.requestDeleteCategory(category)

        val state = viewModel.uiState.value

        // Then
        assertTrue(state.showDeleteConfirmation)
        assertEquals(category, state.categoryToDelete)
    }

    @Test
    fun cancelDeleteCategory_hidesConfirmationDialog() = runTest {
        // Given - Confirmation dialog shown
        val category = createTestCategory(name = "Test Category")
        viewModel.requestDeleteCategory(category)
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        // When
        viewModel.cancelDeleteCategory()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showDeleteConfirmation)
        assertNull(state.categoryToDelete)
    }

    @Test
    fun confirmDeleteCategory_deletesSuccessfully() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        val foodId = categoryIds[0]
        val category = createTestCategory(id = foodId.toString(), name = "Food")

        viewModel.loadCategories()
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.categories.size)

        // When
        viewModel.requestDeleteCategory(category)
        viewModel.confirmDeleteCategory()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showDeleteConfirmation)
        assertNull(state.categoryToDelete)
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("deleted successfully"))
        assertEquals(2, state.categories.size) // One category deleted
    }

    @Test
    fun confirmDeleteCategory_closesDialogImmediately() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        val category = createTestCategory(id = categoryIds[0].toString(), name = "Food")
        viewModel.requestDeleteCategory(category)

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        // When
        viewModel.confirmDeleteCategory()
        // Don't advance - check state before async completion

        // Then - Dialog should close immediately, before delete completes
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.categoryToDelete)
    }

    @Test
    fun confirmDeleteCategory_setsLoadingState() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        val category = createTestCategory(id = categoryIds[0].toString(), name = "Food")
        viewModel.requestDeleteCategory(category)

        // When
        viewModel.confirmDeleteCategory()

        // Then - Should be loading during delete
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        // Then - Should not be loading after completion
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun confirmDeleteCategory_withoutCategoryToDelete_showsError() = runTest {
        // Given - No category to delete
        assertNull(viewModel.uiState.value.categoryToDelete)

        // When
        viewModel.confirmDeleteCategory()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.showDeleteConfirmation)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("No category selected"))
    }

    @Test
    fun confirmDeleteCategory_withInvalidCategoryId_showsError() = runTest {
        // Given
        val category = createTestCategory(id = "invalid_id", name = "Test")
        viewModel.requestDeleteCategory(category)

        // When
        viewModel.confirmDeleteCategory()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Invalid category ID"))
    }

    @Test
    fun confirmDeleteCategory_handlesDatabaseError() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        val category = createTestCategory(id = categoryIds[0].toString(), name = "Food")
        viewModel.requestDeleteCategory(category)

        database.close()

        // When
        viewModel.confirmDeleteCategory()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Failed to delete category"))
        assertNull(state.successMessage)
    }

    @Test
    fun confirmDeleteCategory_cascadeDeletesSubcategories() = runTest {
        // Given - Parent with subcategories
        val categoryIds = seedTestCategories()
        val foodId = categoryIds[0]

        // Add subcategories
        val groceries = createTestCategory(id = "groceries", name = "Groceries")
        val restaurants = createTestCategory(id = "restaurants", name = "Restaurants")
        repository.saveCategory(groceries, parentId = foodId)
        repository.saveCategory(restaurants, parentId = foodId)

        viewModel.loadCategories()
        advanceUntilIdle()
        assertEquals(5, viewModel.uiState.value.categories.size) // 3 root + 2 subcategories

        val category = createTestCategory(id = foodId.toString(), name = "Food")

        // When
        viewModel.requestDeleteCategory(category)
        viewModel.confirmDeleteCategory()
        advanceUntilIdle()

        // Then - Parent and subcategories should be deleted
        assertEquals(2, viewModel.uiState.value.categories.size)

        // Verify subcategories are also deleted
        val remainingSubcategories = repository.getSubcategoriesSnapshot(foodId).getOrNull()!!
        assertTrue(remainingSubcategories.isEmpty())
    }

    // ==================== UI State Management Tests ====================

    @Test
    fun selectCategory_setsSelectedCategory() = runTest {
        // Given
        val category = createTestCategory(name = "Test Category")

        // When
        viewModel.selectCategory(category)

        // Then
        assertEquals(category, viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun selectCategory_withNull_clearsSelection() = runTest {
        // Given - Category selected
        val category = createTestCategory(name = "Test Category")
        viewModel.selectCategory(category)
        assertEquals(category, viewModel.uiState.value.selectedCategory)

        // When
        viewModel.selectCategory(null)

        // Then
        assertNull(viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun setEditMode_enablesEditMode() = runTest {
        // Given
        assertFalse(viewModel.uiState.value.isEditMode)

        // When
        viewModel.setEditMode(true)

        // Then
        assertTrue(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun setEditMode_disablesEditMode() = runTest {
        // Given - Edit mode enabled
        viewModel.setEditMode(true)
        assertTrue(viewModel.uiState.value.isEditMode)

        // When
        viewModel.setEditMode(false)

        // Then
        assertFalse(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun setSearchQuery_updatesSearchQuery() = runTest {
        // Given
        assertEquals("", viewModel.uiState.value.searchQuery)

        // When
        viewModel.setSearchQuery("food")

        // Then
        assertEquals("food", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun setSearchQuery_handlesEmptyString() = runTest {
        // Given
        viewModel.setSearchQuery("food")
        assertEquals("food", viewModel.uiState.value.searchQuery)

        // When
        viewModel.setSearchQuery("")

        // Then
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    // ==================== Message Management Tests ====================

    @Test
    fun clearError_removesErrorMessage() = runTest {
        // Given - Trigger an error
        database.close()
        viewModel.loadCategories()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun clearSuccessMessage_removesSuccessMessage() = runTest {
        // Given - Trigger success message
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = CategoryRepository(database)
        viewModel = CategoryManagementViewModel(repository)
        advanceUntilIdle()

        val newCategory = createTestCategory(name = "New Category")
        viewModel.saveCategory(newCategory)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.successMessage)

        // When
        viewModel.clearSuccessMessage()

        // Then
        assertNull(viewModel.uiState.value.successMessage)
    }

    // ==================== Reorder Categories Tests ====================

    @Test
    fun reorderCategories_updatesUIState() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        viewModel.loadCategories()
        advanceUntilIdle()

        val originalCategories = viewModel.uiState.value.categories
        assertEquals(3, originalCategories.size)

        // Reverse the order
        val reorderedCategories = originalCategories.reversed()

        // When
        viewModel.reorderCategories(reorderedCategories)
        advanceUntilIdle()

        // Then - UI should reflect new order
        val newCategories = viewModel.uiState.value.categories
        assertEquals(3, newCategories.size)
        assertEquals(reorderedCategories[0].id, newCategories[0].id)
        assertEquals(reorderedCategories[1].id, newCategories[1].id)
        assertEquals(reorderedCategories[2].id, newCategories[2].id)
    }

    @Test
    fun reorderCategories_handlesEmptyList() = runTest {
        // Given
        seedTestCategories()
        viewModel.loadCategories()
        advanceUntilIdle()

        // When
        viewModel.reorderCategories(emptyList())
        advanceUntilIdle()

        // Then - Should handle gracefully
        val state = viewModel.uiState.value
        assertTrue(state.categories.isEmpty())
    }

    // ==================== Edge Cases and Error Recovery ====================

    @Test
    fun multipleOperations_inSequence_workCorrectly() = runTest {
        // Given
        val categoryIds = seedTestCategories()
        advanceUntilIdle()

        // When - Perform multiple operations
        viewModel.loadCategories()
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.categories.size)

        val newCategory = createTestCategory(name = "New Category")
        viewModel.saveCategory(newCategory)
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.categories.size)

        val updatedCategory = createTestCategory(name = "Updated Name")
        viewModel.updateCategory(categoryIds[0], updatedCategory)
        advanceUntilIdle()
        assertEquals("Updated Name", viewModel.uiState.value.categories.find {
            it.id == categoryIds[0].toString()
        }?.name)

        val category = createTestCategory(id = categoryIds[1].toString(), name = "Shopping")
        viewModel.requestDeleteCategory(category)
        viewModel.confirmDeleteCategory()
        advanceUntilIdle()

        // Then - All operations should succeed
        assertEquals(3, viewModel.uiState.value.categories.size)
        assertNotNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun errorRecovery_loadingAfterError_clearsError() = runTest {
        // Given - Trigger an error
        database.close()
        viewModel.loadCategories()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        // When - Reopen database and reload
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = CategoryRepository(database)
        viewModel = CategoryManagementViewModel(repository)
        seedTestCategories()

        viewModel.loadCategories()
        advanceUntilIdle()

        // Then - Error should be cleared
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(3, viewModel.uiState.value.categories.size)
    }

    @Test
    fun stateFlow_emitsUpdates() = runTest {
        // Given
        val states = mutableListOf<CategoryManagementUiState>()

        // Collect states
        val job = kotlinx.coroutines.launch {
            viewModel.uiState.collect { states.add(it) }
        }

        // When
        seedTestCategories()
        viewModel.loadCategories()
        advanceUntilIdle()

        job.cancel()

        // Then - Should have emitted multiple states
        assertTrue(states.size > 1)

        // Verify state progression
        val loadingStates = states.filter { it.isLoading }
        val completedStates = states.filter { !it.isLoading && it.categories.isNotEmpty() }

        assertTrue(loadingStates.isNotEmpty())
        assertTrue(completedStates.isNotEmpty())
    }

    @Test
    fun concurrentOperations_handleGracefully() = runTest {
        // Given
        seedTestCategories()

        // When - Launch multiple operations concurrently
        viewModel.loadCategories()
        viewModel.setSearchQuery("food")
        viewModel.setEditMode(true)

        advanceUntilIdle()

        // Then - All state updates should be applied
        val state = viewModel.uiState.value
        assertEquals(3, state.categories.size)
        assertEquals("food", state.searchQuery)
        assertTrue(state.isEditMode)
    }
}
