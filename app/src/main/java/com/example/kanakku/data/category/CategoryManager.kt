package com.example.kanakku.data.category

import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.repository.TransactionRepository

/**
 * Manages transaction categorization and category overrides.
 *
 * This class handles automatic categorization based on keywords
 * and supports manual category overrides that persist to database.
 *
 * @param repository Optional repository for persisting category overrides
 */
class CategoryManager(
    private var repository: TransactionRepository? = null
) {

    private val manualOverrides = mutableMapOf<Long, Category>()

    /**
     * Initializes the CategoryManager by loading category overrides from database.
     * Should be called after repository is available and before categorizing transactions.
     *
     * @param repo The transaction repository to use for persistence
     */
    suspend fun initialize(repo: TransactionRepository) {
        repository = repo
        loadOverridesFromDatabase()
    }

    /**
     * Loads all category overrides from database into memory.
     * Maps category IDs to Category objects from DefaultCategories.
     */
    private suspend fun loadOverridesFromDatabase() {
        val repo = repository ?: return

        try {
            val overrides = repo.getAllCategoryOverrides()
            manualOverrides.clear()

            for ((smsId, categoryId) in overrides) {
                // Find matching category from DefaultCategories
                val category = DefaultCategories.ALL.find { it.id == categoryId }
                if (category != null) {
                    manualOverrides[smsId] = category
                }
            }
        } catch (e: Exception) {
            // Silently fail - overrides will be empty if database is unavailable
        }
    }

    fun categorizeTransaction(transaction: ParsedTransaction): Category {
        manualOverrides[transaction.smsId]?.let { return it }

        val searchText = buildString {
            append(transaction.merchant?.lowercase() ?: "")
            append(" ")
            append(transaction.rawSms.lowercase())
        }

        for (category in DefaultCategories.ALL) {
            if (category.keywords.any { keyword -> searchText.contains(keyword.lowercase()) }) {
                return category
            }
        }

        return DefaultCategories.OTHER
    }

    /**
     * Sets a manual category override for a transaction.
     * Persists to database if repository is available.
     *
     * @param smsId The SMS ID of the transaction
     * @param category The category to assign
     */
    suspend fun setManualOverride(smsId: Long, category: Category) {
        manualOverrides[smsId] = category

        // Persist to database
        repository?.setCategoryOverride(smsId, category.id)
    }

    /**
     * Removes a manual category override for a transaction.
     * Removes from database if repository is available.
     *
     * @param smsId The SMS ID of the transaction
     */
    suspend fun removeManualOverride(smsId: Long) {
        manualOverrides.remove(smsId)

        // Remove from database
        repository?.removeCategoryOverride(smsId)
    }

    fun getManualOverride(smsId: Long): Category? = manualOverrides[smsId]

    fun hasManualOverride(smsId: Long): Boolean = manualOverrides.containsKey(smsId)

    fun categorizeAll(transactions: List<ParsedTransaction>): Map<Long, Category> {
        return transactions.associate { it.smsId to categorizeTransaction(it) }
    }
}
