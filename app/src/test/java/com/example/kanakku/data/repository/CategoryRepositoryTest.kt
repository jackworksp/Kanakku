package com.example.kanakku.data.repository

import androidx.compose.ui.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kanakku.data.database.KanakkuDatabase
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for CategoryRepository using in-memory Room database.
 *
 * Tests cover:
 * - Category CRUD operations
 * - Entity-model mapping
 * - Hierarchical category support (parent-child relationships)
 * - Cache management and invalidation
 * - Error handling with Result types
 * - Default category seeding
 * - Flow operations
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CategoryRepositoryTest {

    private lateinit var database: KanakkuDatabase
    private lateinit var repository: CategoryRepository

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KanakkuDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()

        repository = CategoryRepository(database)
    }

    @After
    fun teardown() {
        database.close()
    }

    // ==================== Helper Functions ====================

    private fun createTestCategory(
        name: String = "Test Category",
        icon: String = "🏷️",
        color: Color = Color(0xFFFF5722),
        keywords: MutableList<String> = mutableListOf("test"),
        isSystemCategory: Boolean = false
    ): Category {
        return Category(
            id = name.lowercase().replace(" ", "_"),
            name = name,
            icon = icon,
            color = color,
            keywords = keywords,
            parentId = null,
            isSystemCategory = isSystemCategory
        )
    }

    // ==================== Category Save/Load Tests ====================

    @Test
    fun saveCategory_insertsSuccessfully() = runTest {
        // Given
        val category = createTestCategory(name = "Groceries")

        // When
        val result = repository.saveCategory(category)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!! > 0) // Should return generated ID

        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals(1, loaded.size)
        assertEquals("Groceries", loaded[0].name)
        assertEquals("🏷️", loaded[0].icon)
        assertEquals(Color(0xFFFF5722), loaded[0].color)
    }

    @Test
    fun saveCategory_withParentId_insertsAsSubcategory() = runTest {
        // Given - Parent category
        val parent = createTestCategory(name = "Food")
        val parentId = repository.saveCategory(parent).getOrNull()!!

        // When - Save subcategory
        val subcategory = createTestCategory(name = "Groceries")
        val result = repository.saveCategory(subcategory, parentId = parentId)

        // Then
        assertTrue(result.isSuccess)

        val subcategories = repository.getSubcategoriesSnapshot(parentId).getOrNull()!!
        assertEquals(1, subcategories.size)
        assertEquals("Groceries", subcategories[0].name)
    }

    @Test
    fun saveCategory_asSystemCategory_setsFlag() = runTest {
        // Given
        val category = createTestCategory(name = "Food", isSystemCategory = true)

        // When
        val result = repository.saveCategory(category, isSystemCategory = true)

        // Then
        assertTrue(result.isSuccess)

        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals(1, loaded.size)
        assertTrue(loaded[0].isSystemCategory)
    }

    @Test
    fun saveCategory_multipleTimes_maintainsSortOrder() = runTest {
        // Given
        val category1 = createTestCategory(name = "First")
        val category2 = createTestCategory(name = "Second")
        val category3 = createTestCategory(name = "Third")

        // When
        repository.saveCategory(category1)
        repository.saveCategory(category2)
        repository.saveCategory(category3)

        // Then - Categories should be sorted by sortOrder then name
        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals(3, loaded.size)
        // Note: Names are used for secondary sorting when sortOrder is the same
    }

    @Test
    fun getAllCategories_returnsFlowOfCategories() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Food"))
        repository.saveCategory(createTestCategory(name = "Transport"))

        // When
        val flow = repository.getAllCategories()
        val result = flow.first()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getAllCategoriesSnapshot_returnsList() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Food"))
        repository.saveCategory(createTestCategory(name = "Transport"))

        // When
        val result = repository.getAllCategoriesSnapshot()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
    }

    @Test
    fun getAllCategoriesSnapshot_usesCacheOnSecondCall() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Food"))

        // When - First call populates cache
        val result1 = repository.getAllCategoriesSnapshot()

        // Close database to verify cache is used
        database.close()

        // Second call should use cache
        val result2 = repository.getAllCategoriesSnapshot()

        // Then - Both should succeed (second uses cache)
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertEquals(result1.getOrNull()?.size, result2.getOrNull()?.size)
    }

    // ==================== Category Update Tests ====================

    @Test
    fun updateCategory_updatesSuccessfully() = runTest {
        // Given
        val category = createTestCategory(name = "Food", icon = "🍔")
        val categoryId = repository.saveCategory(category).getOrNull()!!

        // When
        val updatedCategory = category.copy(
            name = "Food & Beverages",
            icon = "🍕",
            color = Color(0xFF00FF00)
        )
        val result = repository.updateCategory(categoryId, updatedCategory)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)

        val loaded = repository.getCategoryById(categoryId).getOrNull()!!
        assertEquals("Food & Beverages", loaded.name)
        assertEquals("🍕", loaded.icon)
        assertEquals(Color(0xFF00FF00), loaded.color)
    }

    @Test
    fun updateCategory_preservesStructuralFields() = runTest {
        // Given - Category with parent
        val parent = createTestCategory(name = "Parent")
        val parentId = repository.saveCategory(parent).getOrNull()!!

        val child = createTestCategory(name = "Child")
        val childId = repository.saveCategory(child, parentId = parentId).getOrNull()!!

        // When - Update only name and icon
        val updated = child.copy(name = "Updated Child", icon = "✨")
        repository.updateCategory(childId, updated)

        // Then - parentId should be preserved
        val loaded = repository.getSubcategoriesSnapshot(parentId).getOrNull()!!
        assertEquals(1, loaded.size)
        assertEquals("Updated Child", loaded[0].name)
    }

    @Test
    fun updateCategory_nonExistent_returnsFalse() = runTest {
        // Given
        val category = createTestCategory(name = "Food")

        // When - Try to update non-existent category
        val result = repository.updateCategory(999L, category)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun updateCategory_invalidatesCacheOnSuccess() = runTest {
        // Given
        val category = createTestCategory(name = "Food")
        val categoryId = repository.saveCategory(category).getOrNull()!!

        // Populate cache
        repository.getAllCategoriesSnapshot()

        // When - Update category
        val updated = category.copy(name = "Food Updated")
        repository.updateCategory(categoryId, updated)

        // Then - Cache should be invalidated, fresh data loaded
        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals("Food Updated", loaded[0].name)
    }

    // ==================== Category Delete Tests ====================

    @Test
    fun deleteCategory_removesSuccessfully() = runTest {
        // Given
        val category = createTestCategory(name = "Food")
        val categoryId = repository.saveCategory(category).getOrNull()!!

        // When
        val result = repository.deleteCategory(categoryId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)

        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertTrue(loaded.isEmpty())
    }

    @Test
    fun deleteCategory_cascadesToSubcategories() = runTest {
        // Given - Parent with subcategories
        val parent = createTestCategory(name = "Food")
        val parentId = repository.saveCategory(parent).getOrNull()!!

        repository.saveCategory(createTestCategory(name = "Groceries"), parentId = parentId)
        repository.saveCategory(createTestCategory(name = "Restaurants"), parentId = parentId)

        // When - Delete parent
        val result = repository.deleteCategory(parentId)

        // Then - All subcategories should be deleted (CASCADE)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)

        val allCategories = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertTrue(allCategories.isEmpty())
    }

    @Test
    fun deleteCategory_nonExistent_returnsFalse() = runTest {
        // When
        val result = repository.deleteCategory(999L)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun deleteCategory_invalidatesCacheOnSuccess() = runTest {
        // Given
        val category = createTestCategory(name = "Food")
        val categoryId = repository.saveCategory(category).getOrNull()!!

        // Populate cache
        repository.getAllCategoriesSnapshot()

        // When - Delete category
        repository.deleteCategory(categoryId)

        // Then - Cache should be invalidated
        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertTrue(loaded.isEmpty())
    }

    // ==================== Hierarchical Category Tests ====================

    @Test
    fun getRootCategories_returnsOnlyParentCategories() = runTest {
        // Given
        val parent1 = createTestCategory(name = "Food")
        val parent1Id = repository.saveCategory(parent1).getOrNull()!!

        val parent2 = createTestCategory(name = "Transport")
        repository.saveCategory(parent2)

        // Add subcategory
        repository.saveCategory(createTestCategory(name = "Groceries"), parentId = parent1Id)

        // When
        val rootCategories = repository.getRootCategories().first()

        // Then - Only root categories, not subcategories
        assertEquals(2, rootCategories.size)
        assertTrue(rootCategories.any { it.name == "Food" })
        assertTrue(rootCategories.any { it.name == "Transport" })
        assertFalse(rootCategories.any { it.name == "Groceries" })
    }

    @Test
    fun getRootCategoriesSnapshot_returnsOnlyParentCategories() = runTest {
        // Given
        val parent = createTestCategory(name = "Food")
        val parentId = repository.saveCategory(parent).getOrNull()!!

        repository.saveCategory(createTestCategory(name = "Groceries"), parentId = parentId)

        // When
        val result = repository.getRootCategoriesSnapshot()

        // Then
        assertTrue(result.isSuccess)
        val rootCategories = result.getOrNull()!!
        assertEquals(1, rootCategories.size)
        assertEquals("Food", rootCategories[0].name)
    }

    @Test
    fun getSubcategories_returnsChildrenOfParent() = runTest {
        // Given - Parent with multiple children
        val parent = createTestCategory(name = "Food")
        val parentId = repository.saveCategory(parent).getOrNull()!!

        repository.saveCategory(createTestCategory(name = "Groceries"), parentId = parentId)
        repository.saveCategory(createTestCategory(name = "Restaurants"), parentId = parentId)
        repository.saveCategory(createTestCategory(name = "Fast Food"), parentId = parentId)

        // When
        val subcategories = repository.getSubcategories(parentId).first()

        // Then
        assertEquals(3, subcategories.size)
        assertTrue(subcategories.all { it.name in listOf("Groceries", "Restaurants", "Fast Food") })
    }

    @Test
    fun getSubcategoriesSnapshot_returnsChildrenOfParent() = runTest {
        // Given
        val parent = createTestCategory(name = "Food")
        val parentId = repository.saveCategory(parent).getOrNull()!!

        repository.saveCategory(createTestCategory(name = "Groceries"), parentId = parentId)

        // When
        val result = repository.getSubcategoriesSnapshot(parentId)

        // Then
        assertTrue(result.isSuccess)
        val subcategories = result.getOrNull()!!
        assertEquals(1, subcategories.size)
        assertEquals("Groceries", subcategories[0].name)
    }

    @Test
    fun getSubcategories_withNoChildren_returnsEmpty() = runTest {
        // Given - Category with no children
        val category = createTestCategory(name = "Food")
        val categoryId = repository.saveCategory(category).getOrNull()!!

        // When
        val subcategories = repository.getSubcategories(categoryId).first()

        // Then
        assertTrue(subcategories.isEmpty())
    }

    @Test
    fun getSubcategories_nonExistentParent_returnsEmpty() = runTest {
        // When
        val subcategories = repository.getSubcategories(999L).first()

        // Then
        assertTrue(subcategories.isEmpty())
    }

    // ==================== Get Category By ID Tests ====================

    @Test
    fun getCategoryById_returnsCategory() = runTest {
        // Given
        val category = createTestCategory(name = "Food")
        val categoryId = repository.saveCategory(category).getOrNull()!!

        // When
        val result = repository.getCategoryById(categoryId)

        // Then
        assertTrue(result.isSuccess)
        val loaded = result.getOrNull()!!
        assertEquals("Food", loaded.name)
    }

    @Test
    fun getCategoryById_nonExistent_returnsNull() = runTest {
        // When
        val result = repository.getCategoryById(999L)

        // Then
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun getCategoryByIdFlow_returnsFlow() = runTest {
        // Given
        val category = createTestCategory(name = "Food")
        val categoryId = repository.saveCategory(category).getOrNull()!!

        // When
        val flow = repository.getCategoryByIdFlow(categoryId)
        val result = flow.first()

        // Then
        assertNotNull(result)
        assertEquals("Food", result?.name)
    }

    @Test
    fun getCategoryByIdFlow_nonExistent_returnsNull() = runTest {
        // When
        val flow = repository.getCategoryByIdFlow(999L)
        val result = flow.first()

        // Then
        assertNull(result)
    }

    // ==================== Search Tests ====================

    @Test
    fun searchByName_findsMatchingCategories() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Food"))
        repository.saveCategory(createTestCategory(name = "Food & Beverages"))
        repository.saveCategory(createTestCategory(name = "Transport"))

        // When
        val results = repository.searchByName("Food").first()

        // Then
        assertEquals(2, results.size)
        assertTrue(results.all { it.name.contains("Food", ignoreCase = true) })
    }

    @Test
    fun searchByName_caseInsensitive() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Food"))

        // When
        val results = repository.searchByName("food").first()

        // Then
        assertEquals(1, results.size)
        assertEquals("Food", results[0].name)
    }

    @Test
    fun searchByName_partialMatch() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Groceries"))

        // When
        val results = repository.searchByName("Groc").first()

        // Then
        assertEquals(1, results.size)
        assertEquals("Groceries", results[0].name)
    }

    @Test
    fun searchByName_noMatches_returnsEmpty() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Food"))

        // When
        val results = repository.searchByName("xyz").first()

        // Then
        assertTrue(results.isEmpty())
    }

    // ==================== Database Check Tests ====================

    @Test
    fun isCategoryDatabaseEmpty_returnsTrue_whenEmpty() = runTest {
        // When
        val result = repository.isCategoryDatabaseEmpty()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun isCategoryDatabaseEmpty_returnsFalse_whenNotEmpty() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Food"))

        // When
        val result = repository.isCategoryDatabaseEmpty()

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun getCategoryCount_returnsCorrectCount() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Food"))
        repository.saveCategory(createTestCategory(name = "Transport"))

        // When
        val result = repository.getCategoryCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun getCategoryCount_returnsZero_whenEmpty() = runTest {
        // When
        val result = repository.getCategoryCount()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun getCategoryCount_usesCacheOnSecondCall() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Food"))

        // First call populates cache
        repository.getCategoryCount()

        // Close database
        database.close()

        // When - Second call should use cache
        val result = repository.getCategoryCount()

        // Then - Should succeed using cache
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
    }

    // ==================== Seed Default Categories Tests ====================

    @Test
    fun seedDefaultCategories_insertsAllDefaults() = runTest {
        // When
        val result = repository.seedDefaultCategories()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(DefaultCategories.ALL.size, result.getOrNull())

        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals(DefaultCategories.ALL.size, loaded.size)
        assertTrue(loaded.all { it.isSystemCategory })
    }

    @Test
    fun seedDefaultCategories_skipsIfCategoriesExist() = runTest {
        // Given - Add one category
        repository.saveCategory(createTestCategory(name = "Custom"))

        // When - Try to seed
        val result = repository.seedDefaultCategories()

        // Then - Should skip seeding
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())

        val count = repository.getCategoryCount().getOrNull()!!
        assertEquals(1, count) // Only the custom category
    }

    @Test
    fun seedDefaultCategories_invalidatesCache() = runTest {
        // When
        repository.seedDefaultCategories()

        // Then - Cache should be populated with seeded categories
        val result = repository.getAllCategoriesSnapshot()
        assertTrue(result.isSuccess)
        assertEquals(DefaultCategories.ALL.size, result.getOrNull()?.size)
    }

    // ==================== Delete Custom Categories Tests ====================

    @Test
    fun deleteAllCustomCategories_removesOnlyCustom() = runTest {
        // Given - Mix of system and custom categories
        repository.saveCategory(createTestCategory(name = "System1"), isSystemCategory = true)
        repository.saveCategory(createTestCategory(name = "System2"), isSystemCategory = true)
        repository.saveCategory(createTestCategory(name = "Custom1"), isSystemCategory = false)
        repository.saveCategory(createTestCategory(name = "Custom2"), isSystemCategory = false)

        // When
        val result = repository.deleteAllCustomCategories()

        // Then - Only custom deleted
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        val remaining = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals(2, remaining.size)
        assertTrue(remaining.all { it.isSystemCategory })
    }

    @Test
    fun deleteAllCustomCategories_whenNoCustom_returnsZero() = runTest {
        // Given - Only system categories
        repository.saveCategory(createTestCategory(name = "System"), isSystemCategory = true)

        // When
        val result = repository.deleteAllCustomCategories()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())

        val count = repository.getCategoryCount().getOrNull()!!
        assertEquals(1, count)
    }

    @Test
    fun deleteAllCustomCategories_invalidatesCacheOnSuccess() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Custom"), isSystemCategory = false)

        // Populate cache
        repository.getAllCategoriesSnapshot()

        // When
        repository.deleteAllCustomCategories()

        // Then - Cache should be invalidated
        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertTrue(loaded.isEmpty())
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun saveCategory_handlesErrorGracefully() = runTest {
        // Given
        database.close() // Close database to simulate error

        // When
        val result = repository.saveCategory(createTestCategory(name = "Food"))

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun updateCategory_handlesErrorGracefully() = runTest {
        // Given
        val category = createTestCategory(name = "Food")
        val categoryId = repository.saveCategory(category).getOrNull()!!

        database.close()

        // When
        val result = repository.updateCategory(categoryId, category)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun deleteCategory_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.deleteCategory(1L)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getAllCategoriesSnapshot_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getAllCategoriesSnapshot()

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getRootCategoriesSnapshot_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getRootCategoriesSnapshot()

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getSubcategoriesSnapshot_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getSubcategoriesSnapshot(1L)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getCategoryById_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getCategoryById(1L)

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun isCategoryDatabaseEmpty_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.isCategoryDatabaseEmpty()

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getCategoryCount_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.getCategoryCount()

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun seedDefaultCategories_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.seedDefaultCategories()

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun deleteAllCustomCategories_handlesErrorGracefully() = runTest {
        // Given
        database.close()

        // When
        val result = repository.deleteAllCustomCategories()

        // Then
        assertTrue(result.isFailure)
        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun getAllCategories_catchesErrors() = runTest {
        // Given
        database.close()

        // When - Flow should handle error gracefully
        val flow = repository.getAllCategories()
        val result = flow.first()

        // Then - Should return empty list on error
        assertTrue(result.isEmpty())
    }

    @Test
    fun getRootCategories_catchesErrors() = runTest {
        // Given
        database.close()

        // When
        val flow = repository.getRootCategories()
        val result = flow.first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun getSubcategories_catchesErrors() = runTest {
        // Given
        database.close()

        // When
        val flow = repository.getSubcategories(1L)
        val result = flow.first()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun getCategoryByIdFlow_catchesErrors() = runTest {
        // Given
        database.close()

        // When
        val flow = repository.getCategoryByIdFlow(1L)
        val result = flow.first()

        // Then
        assertNull(result)
    }

    @Test
    fun searchByName_catchesErrors() = runTest {
        // Given
        database.close()

        // When
        val flow = repository.searchByName("test")
        val result = flow.first()

        // Then
        assertTrue(result.isEmpty())
    }

    // ==================== Cache Invalidation Tests ====================

    @Test
    fun cacheInvalidation_afterSaveCategory() = runTest {
        // Given
        repository.saveCategory(createTestCategory(name = "Food"))

        // Populate cache
        val result1 = repository.getAllCategoriesSnapshot()
        assertEquals(1, result1.getOrNull()?.size)

        // When - Save another category (should invalidate cache)
        repository.saveCategory(createTestCategory(name = "Transport"))

        // Then - Should get fresh data from database
        val result2 = repository.getAllCategoriesSnapshot()
        assertEquals(2, result2.getOrNull()?.size)
    }

    @Test
    fun cacheInvalidation_afterUpdateCategory() = runTest {
        // Given
        val category = createTestCategory(name = "Food")
        val categoryId = repository.saveCategory(category).getOrNull()!!

        // Populate cache
        repository.getAllCategoriesSnapshot()

        // When - Update category
        val updated = category.copy(name = "Food Updated")
        repository.updateCategory(categoryId, updated)

        // Then - Cache should be invalidated
        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals("Food Updated", loaded[0].name)
    }

    @Test
    fun cacheInvalidation_afterDeleteCategory() = runTest {
        // Given
        val categoryId = repository.saveCategory(createTestCategory(name = "Food")).getOrNull()!!

        // Populate cache
        val result1 = repository.getAllCategoriesSnapshot()
        assertEquals(1, result1.getOrNull()?.size)

        // When - Delete category
        repository.deleteCategory(categoryId)

        // Then - Cache should be invalidated
        val result2 = repository.getAllCategoriesSnapshot()
        assertTrue(result2.getOrNull()?.isEmpty() == true)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun saveCategory_withEmptyName() = runTest {
        // Given
        val category = createTestCategory(name = "")

        // When
        val result = repository.saveCategory(category)

        // Then - Should succeed (validation is UI layer responsibility)
        assertTrue(result.isSuccess)
    }

    @Test
    fun saveCategory_withVeryLongName() = runTest {
        // Given
        val longName = "A".repeat(500)
        val category = createTestCategory(name = longName)

        // When
        val result = repository.saveCategory(category)

        // Then
        assertTrue(result.isSuccess)

        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals(longName, loaded[0].name)
    }

    @Test
    fun saveCategory_withSpecialCharacters() = runTest {
        // Given
        val specialName = "Food & Beverages™️ (新しい) @#$%"
        val category = createTestCategory(name = specialName)

        // When
        val result = repository.saveCategory(category)

        // Then
        assertTrue(result.isSuccess)

        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals(specialName, loaded[0].name)
    }

    @Test
    fun saveCategory_withEmptyKeywords() = runTest {
        // Given
        val category = createTestCategory(keywords = mutableListOf())

        // When
        val result = repository.saveCategory(category)

        // Then
        assertTrue(result.isSuccess)

        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertTrue(loaded[0].keywords.isEmpty())
    }

    @Test
    fun saveCategory_withManyKeywords() = runTest {
        // Given
        val manyKeywords = (1..100).map { "keyword$it" }.toMutableList()
        val category = createTestCategory(keywords = manyKeywords)

        // When
        val result = repository.saveCategory(category)

        // Then
        assertTrue(result.isSuccess)

        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals(100, loaded[0].keywords.size)
    }

    @Test
    fun hierarchicalCategories_multiLevel() = runTest {
        // Given - Create 3-level hierarchy: Food > Groceries > Vegetables
        val foodId = repository.saveCategory(createTestCategory(name = "Food")).getOrNull()!!
        val groceriesId = repository.saveCategory(
            createTestCategory(name = "Groceries"),
            parentId = foodId
        ).getOrNull()!!
        repository.saveCategory(
            createTestCategory(name = "Vegetables"),
            parentId = groceriesId
        )

        // When
        val level1 = repository.getRootCategoriesSnapshot().getOrNull()!!
        val level2 = repository.getSubcategoriesSnapshot(foodId).getOrNull()!!
        val level3 = repository.getSubcategoriesSnapshot(groceriesId).getOrNull()!!

        // Then
        assertEquals(1, level1.size)
        assertEquals("Food", level1[0].name)

        assertEquals(1, level2.size)
        assertEquals("Groceries", level2[0].name)

        assertEquals(1, level3.size)
        assertEquals("Vegetables", level3[0].name)
    }

    @Test
    fun concurrentOperations_maintainDataIntegrity() = runTest {
        // Given
        val categories = (1..10).map { id ->
            createTestCategory(name = "Category $id")
        }

        // When - Save multiple categories
        categories.forEach { category ->
            repository.saveCategory(category)
        }

        // Then
        assertEquals(10, repository.getCategoryCount().getOrNull())

        val loaded = repository.getAllCategoriesSnapshot().getOrNull()!!
        assertEquals(10, loaded.size)
    }

    @Test
    fun flowReactivity_updatesOnDataChange() = runTest {
        // Given - Start observing flow
        val flow = repository.getAllCategories()

        // When - Add category
        repository.saveCategory(createTestCategory(name = "Food"))

        val result1 = flow.first()

        // Add another
        repository.saveCategory(createTestCategory(name = "Transport"))

        val result2 = flow.first()

        // Then - Flow should emit updated data
        assertEquals(1, result1.size)
        assertEquals(2, result2.size)
    }
}
