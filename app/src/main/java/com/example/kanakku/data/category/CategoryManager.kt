package com.example.kanakku.data.category

import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.repository.CategoryRepository
import com.example.kanakku.data.repository.TransactionRepository

/**
 * Manages transaction categorization and category overrides.
 *
 * This class handles automatic categorization based on keywords from database-loaded categories
 * (both system defaults and user-created custom categories) and supports manual category overrides
 * that persist to database.
 *
 * Key Changes from Original:
 * - Loads categories from database instead of hardcoded DefaultCategories
 * - Supports hierarchical categorization with subcategories
 * - Caches categories in memory for fast lookups
 * - Automatically seeds default categories if database is empty
 *
 * @param categoryRepository Repository for loading categories from database
 * @param transactionRepository Optional repository for persisting category overrides
 */
class CategoryManager(
    private val categoryRepository: CategoryRepository? = null,
    private var transactionRepository: TransactionRepository? = null
) {

    // In-memory cache of all categories loaded from database
    private var allCategories: List<Category> = emptyList()

    // Manual category overrides (smsId -> Category)
    private val manualOverrides = mutableMapOf<Long, Category>()

    /**
     * Initializes the CategoryManager by:
     * 1. Loading all categories from database into memory
     * 2. Seeding default categories if database is empty
     * 3. Loading category overrides from transaction repository
     *
     * Should be called after repositories are available and before categorizing transactions.
     *
     * @param transactionRepo The transaction repository to use for persistence
     */
    suspend fun initialize(transactionRepo: TransactionRepository) {
        transactionRepository = transactionRepo
        loadCategoriesFromDatabase()
        loadOverridesFromDatabase()
    }

    /**
     * Loads all categories from database into memory cache.
     * If database is empty (first launch), seeds default categories.
     * Handles subcategories and hierarchical structure.
     */
    private suspend fun loadCategoriesFromDatabase() {
        val categoryRepo = categoryRepository ?: return

        // Check if database is empty and seed default categories if needed
        categoryRepo.isCategoryDatabaseEmpty()
            .onSuccess { isEmpty ->
                if (isEmpty) {
                    ErrorHandler.logDebug(
                        "Category database is empty, seeding default categories",
                        "CategoryManager"
                    )
                    categoryRepo.seedDefaultCategories()
                        .onFailure { throwable ->
                            ErrorHandler.logWarning(
                                "Failed to seed default categories: ${throwable.message}",
                                "CategoryManager"
                            )
                        }
                }
            }
            .onFailure { throwable ->
                ErrorHandler.logWarning(
                    "Failed to check if category database is empty: ${throwable.message}",
                    "CategoryManager"
                )
            }

        // Load all categories from database
        categoryRepo.getAllCategoriesSnapshot()
            .onSuccess { categories ->
                allCategories = categories
                ErrorHandler.logDebug(
                    "Loaded ${categories.size} categories from database (${categories.count { it.isSystemCategory }} system, ${categories.count { !it.isSystemCategory }} custom)",
                    "CategoryManager"
                )
            }
            .onFailure { throwable ->
                ErrorHandler.logWarning(
                    "Failed to load categories from database, using default fallback: ${throwable.message}",
                    "CategoryManager"
                )
                // Fallback to hardcoded defaults if database load fails
                allCategories = DefaultCategories.ALL
            }
    }

    /**
     * Loads all category overrides from database into memory.
     * Maps category IDs to Category objects from loaded categories.
     */
    private suspend fun loadOverridesFromDatabase() {
        val repo = transactionRepository ?: return

        // Handle Result type from repository
        repo.getAllCategoryOverrides()
            .onSuccess { overrides ->
                manualOverrides.clear()
                for ((smsId, categoryId) in overrides) {
                    // Find matching category from loaded categories
                    val category = findCategoryById(categoryId)
                    if (category != null) {
                        manualOverrides[smsId] = category
                    }
                }
                ErrorHandler.logDebug(
                    "Loaded ${manualOverrides.size} category overrides from database",
                    "CategoryManager"
                )
            }
            .onFailure { throwable ->
                ErrorHandler.logWarning(
                    "Failed to load category overrides: ${throwable.message}",
                    "CategoryManager"
                )
                // Silently fail - overrides will be empty if database is unavailable
                manualOverrides.clear()
            }
    }

    /**
     * Categorizes a transaction using the following priority:
     * 1. Manual override (if exists)
     * 2. Keyword matching against loaded categories (prioritizing subcategories)
     * 3. Default "Other" category
     *
     * Supports hierarchical categorization - subcategories are checked alongside root categories.
     *
     * @param transaction The transaction to categorize
     * @return The matched category
     */
    fun categorizeTransaction(transaction: ParsedTransaction): Category {
        // Check manual override first
        manualOverrides[transaction.smsId]?.let { return it }

        // Build search text from transaction
        val searchText = buildString {
            append(transaction.merchant?.lowercase() ?: "")
            append(" ")
            append(transaction.rawSms.lowercase())
        }

        // Check all categories (including subcategories) for keyword matches
        // Prioritize subcategories over parent categories for more specific categorization
        val matchedCategory = findMatchingCategory(searchText)
        if (matchedCategory != null) {
            return matchedCategory
        }

        // Default to "Other" category if no match found
        return findOtherCategory() ?: DefaultCategories.OTHER
    }

    /**
     * Finds a category that matches the search text based on keywords.
     * Prioritizes subcategories over parent categories for more specific matching.
     *
     * @param searchText The text to search for keywords (lowercase)
     * @return The first matching category, or null if no match
     */
    private fun findMatchingCategory(searchText: String): Category? {
        // Separate categories into subcategories and root categories
        val subcategories = allCategories.filter { it.parentId != null }
        val rootCategories = allCategories.filter { it.parentId == null }

        // Check subcategories first for more specific matches
        for (category in subcategories) {
            if (categoryMatchesKeywords(category, searchText)) {
                return category
            }
        }

        // Check root categories next
        for (category in rootCategories) {
            if (categoryMatchesKeywords(category, searchText)) {
                return category
            }
        }

        return null
    }

    /**
     * Checks if a category's keywords match the search text.
     *
     * @param category The category to check
     * @param searchText The text to search (lowercase)
     * @return True if any keyword matches, false otherwise
     */
    private fun categoryMatchesKeywords(category: Category, searchText: String): Boolean {
        return category.keywords.any { keyword ->
            searchText.contains(keyword.lowercase())
        }
    }

    /**
     * Finds a category by its ID from the loaded categories.
     *
     * @param categoryId The ID to search for
     * @return The category if found, null otherwise
     */
    private fun findCategoryById(categoryId: String): Category? {
        return allCategories.find { it.id == categoryId }
    }

    /**
     * Finds the "Other" category from loaded categories.
     * Falls back to DefaultCategories.OTHER if not found in database.
     *
     * @return The "Other" category
     */
    private fun findOtherCategory(): Category? {
        // Try to find "other" category from loaded categories
        return allCategories.find { it.id == "other" || it.name.equals("Other", ignoreCase = true) }
    }

    /**
     * Sets a manual category override for a transaction.
     * Persists to database if repository is available.
     *
     * @param smsId The SMS ID of the transaction
     * @param category The category to assign
     * @return Result<Unit> indicating success or failure
     */
    suspend fun setManualOverride(smsId: Long, category: Category): Result<Unit> {
        // Update in-memory state first
        manualOverrides[smsId] = category

        // Persist to database
        return if (transactionRepository != null) {
            transactionRepository!!.setCategoryOverride(smsId, category.id)
        } else {
            // If no repository, still succeed (in-memory only)
            Result.success(Unit)
        }
    }

    /**
     * Removes a manual category override for a transaction.
     * Removes from database if repository is available.
     *
     * @param smsId The SMS ID of the transaction
     * @return Result<Unit> indicating success or failure
     */
    suspend fun removeManualOverride(smsId: Long): Result<Unit> {
        // Remove from in-memory state first
        manualOverrides.remove(smsId)

        // Remove from database
        return if (transactionRepository != null) {
            transactionRepository!!.removeCategoryOverride(smsId).mapCatching { Unit }
        } else {
            // If no repository, still succeed (in-memory only)
            Result.success(Unit)
        }
    }

    /**
     * Gets the manual override for a transaction, if it exists.
     *
     * @param smsId The SMS ID of the transaction
     * @return The overridden category, or null if no override exists
     */
    fun getManualOverride(smsId: Long): Category? = manualOverrides[smsId]

    /**
     * Checks if a transaction has a manual category override.
     *
     * @param smsId The SMS ID of the transaction
     * @return True if manual override exists, false otherwise
     */
    fun hasManualOverride(smsId: Long): Boolean = manualOverrides.containsKey(smsId)

    /**
     * Categorizes a list of transactions.
     * Useful for bulk categorization operations.
     *
     * @param transactions The list of transactions to categorize
     * @return Map of SMS ID to Category
     */
    fun categorizeAll(transactions: List<ParsedTransaction>): Map<Long, Category> {
        return transactions.associate { it.smsId to categorizeTransaction(it) }
    }

    /**
     * Reloads categories from database.
     * Useful after categories are added/updated/deleted to refresh the cache.
     *
     * @return Result<Int> containing number of categories loaded or error information
     */
    suspend fun reloadCategories(): Result<Int> {
        return try {
            loadCategoriesFromDatabase()
            Result.success(allCategories.size)
        } catch (e: Exception) {
            val errorInfo = ErrorHandler.handleError(e, "Reload categories")
            Result.failure(Exception(errorInfo.userMessage))
        }
    }

    /**
     * Gets all loaded categories.
     * Useful for displaying available categories in UI.
     *
     * @return List of all categories (system + custom)
     */
    fun getAllCategories(): List<Category> = allCategories

    /**
     * Gets all root-level categories (categories without a parent).
     *
     * @return List of root categories
     */
    fun getRootCategories(): List<Category> {
        return allCategories.filter { it.parentId == null }
    }

    /**
     * Gets all subcategories for a given parent category.
     *
     * @param parentId The ID of the parent category
     * @return List of subcategories
     */
    fun getSubcategories(parentId: String): List<Category> {
        return allCategories.filter { it.parentId == parentId }
    }
}
