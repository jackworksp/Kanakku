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
 * @param repository Repository for persisting category overrides
 */
class CategoryManager(
    private val repository: TransactionRepository
) {

    private val manualOverrides = mutableMapOf<Long, Category>()

    /**
     * Initializes the CategoryManager by loading category overrides from database.
     * Should be called before categorizing transactions to load existing overrides.
     */
    suspend fun initialize() {
        loadOverridesFromDatabase()
    }

    /**
     * Loads all category overrides from database into memory.
     * Maps category IDs to Category objects from DefaultCategories.
     */
    private suspend fun loadOverridesFromDatabase() {
        // Handle Result type from repository
        repository.getAllCategoryOverrides()
            .onSuccess { overrides ->
                manualOverrides.clear()
                for ((smsId, categoryId) in overrides) {
                    // Find matching category from DefaultCategories
                    val category = DefaultCategories.ALL.find { it.id == categoryId }
                    if (category != null) {
                        manualOverrides[smsId] = category
                    }
                }
            }
            .onFailure {
                // Silently fail - overrides will be empty if database is unavailable
                manualOverrides.clear()
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
     * Persists to database and updates in-memory cache.
     *
     * @param smsId The SMS ID of the transaction
     * @param category The category to assign
     * @return Result<Unit> indicating success or failure
     */
    suspend fun setManualOverride(smsId: Long, category: Category): Result<Unit> {
        // Update in-memory state first
        manualOverrides[smsId] = category

        // Persist to database
        return repository.setCategoryOverride(smsId, category.id)
    }

    /**
     * Removes a manual category override for a transaction.
     * Removes from database and updates in-memory cache.
     *
     * @param smsId The SMS ID of the transaction
     * @return Result<Unit> indicating success or failure
     */
    suspend fun removeManualOverride(smsId: Long): Result<Unit> {
        // Remove from in-memory state first
        manualOverrides.remove(smsId)

        // Remove from database
        return repository.removeCategoryOverride(smsId).mapCatching { Unit }
    }

    fun getManualOverride(smsId: Long): Category? = manualOverrides[smsId]

    fun hasManualOverride(smsId: Long): Boolean = manualOverrides.containsKey(smsId)

    fun categorizeAll(transactions: List<ParsedTransaction>): Map<Long, Category> {
        return transactions.associate { it.smsId to categorizeTransaction(it) }
    }
}
