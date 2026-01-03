package com.example.kanakku.data.category

import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.repository.TransactionRepository

/**
 * Manages transaction categorization and category overrides.
 *
 * This class handles automatic categorization based on keywords,
 * supports manual category overrides that persist to database,
 * and learns merchant-to-category mappings from user corrections.
 *
 * Categorization priority:
 * 1. Manual per-transaction overrides (highest priority)
 * 2. Learned merchant-to-category mappings
 * 3. Keyword-based automatic categorization
 * 4. Default "Other" category (fallback)
 *
 * @param repository Optional repository for persisting category overrides and merchant mappings
 */
class CategoryManager(
    private var repository: TransactionRepository? = null
) {

    private val manualOverrides = mutableMapOf<Long, Category>()

    /**
     * In-memory cache of merchant-to-category mappings.
     * Maps normalized merchant names to category IDs for fast lookups.
     */
    private val merchantMappings = mutableMapOf<String, String>()

    /**
     * Initializes the CategoryManager by loading category overrides and merchant mappings from database.
     * Should be called after repository is available and before categorizing transactions.
     *
     * @param repo The transaction repository to use for persistence
     */
    suspend fun initialize(repo: TransactionRepository) {
        repository = repo
        loadOverridesFromDatabase()
        loadMerchantMappingsFromDatabase()
    }

    /**
     * Loads all category overrides from database into memory.
     * Maps category IDs to Category objects from DefaultCategories.
     */
    private suspend fun loadOverridesFromDatabase() {
        val repo = repository ?: return

        // Handle Result type from repository
        repo.getAllCategoryOverrides()
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

    /**
     * Loads all merchant-to-category mappings from database into memory cache.
     * Enables fast merchant-based categorization without database lookups.
     */
    private suspend fun loadMerchantMappingsFromDatabase() {
        val repo = repository ?: return

        // Handle Result type from repository
        repo.getAllMerchantMappingsSnapshot()
            .onSuccess { mappings ->
                merchantMappings.clear()
                merchantMappings.putAll(mappings)
            }
            .onFailure {
                // Silently fail - merchant mappings will be empty if database is unavailable
                merchantMappings.clear()
            }
    }

    /**
     * Categorizes a transaction using a multi-tier approach.
     *
     * Priority order:
     * 1. Manual per-transaction override (highest)
     * 2. Learned merchant mapping (if merchant available)
     * 3. Keyword-based categorization
     * 4. Default "Other" category
     *
     * @param transaction The transaction to categorize
     * @return The determined category
     */
    fun categorizeTransaction(transaction: ParsedTransaction): Category {
        // Priority 1: Check for manual override
        manualOverrides[transaction.smsId]?.let { return it }

        // Priority 2: Check learned merchant mapping
        transaction.merchant?.let { merchant ->
            val normalizedMerchant = normalizeMerchantName(merchant)
            merchantMappings[normalizedMerchant]?.let { categoryId ->
                // Find category from DefaultCategories by ID
                val category = DefaultCategories.ALL.find { it.id == categoryId }
                if (category != null) {
                    return category
                }
            }
        }

        // Priority 3: Keyword-based categorization
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

        // Priority 4: Default fallback
        return DefaultCategories.OTHER
    }

    /**
     * Normalizes a merchant name for consistent mapping lookups.
     * Must match the normalization used in TransactionRepository.
     *
     * @param merchantName The raw merchant name
     * @return Normalized merchant name (lowercase, trimmed, alphanumeric + spaces only)
     */
    private fun normalizeMerchantName(merchantName: String): String {
        return merchantName
            .lowercase()
            .trim()
            .replace(Regex("[^a-z0-9\\s]"), "") // Keep only alphanumeric and spaces
            .replace(Regex("\\s+"), " ") // Normalize multiple spaces to single space
    }

    /**
     * Sets a manual category override for a transaction.
     * Persists to database if repository is available.
     * If merchant name is provided, also learns the merchant-to-category mapping.
     *
     * @param smsId The SMS ID of the transaction
     * @param category The category to assign
     * @param merchant Optional merchant name to learn mapping from (null if not available)
     * @return Result<Unit> indicating success or failure
     */
    suspend fun setManualOverride(
        smsId: Long,
        category: Category,
        merchant: String? = null
    ): Result<Unit> {
        // Update in-memory state first
        manualOverrides[smsId] = category

        val repo = repository ?: return Result.success(Unit) // No repository, succeed in-memory only

        // Persist per-transaction override to database
        val overrideResult = repo.setCategoryOverride(smsId, category.id)
        if (overrideResult.isFailure) {
            return overrideResult
        }

        // Learn merchant mapping if merchant is provided and not empty
        if (!merchant.isNullOrBlank()) {
            val normalizedMerchant = normalizeMerchantName(merchant)
            if (normalizedMerchant.isNotBlank()) {
                // Save merchant mapping to database
                repo.setMerchantCategoryMapping(normalizedMerchant, category.id)
                    .onSuccess {
                        // Update in-memory cache
                        merchantMappings[normalizedMerchant] = category.id
                    }
                    // Ignore merchant mapping errors - per-transaction override still succeeded
            }
        }

        return Result.success(Unit)
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
        return if (repository != null) {
            repository!!.removeCategoryOverride(smsId).mapCatching { Unit }
        } else {
            // If no repository, still succeed (in-memory only)
            Result.success(Unit)
        }
    }

    /**
     * Resets all learned merchant-to-category mappings.
     * Clears both in-memory cache and database records.
     *
     * @return Result<Int> containing the number of mappings deleted, or error information
     */
    suspend fun resetAllMerchantMappings(): Result<Int> {
        val repo = repository ?: return Result.success(0) // No repository, nothing to delete

        // Remove all mappings from database
        return repo.removeAllMerchantMappings()
            .onSuccess { count ->
                // Clear in-memory cache on success
                merchantMappings.clear()
            }
            .onFailure {
                // Keep in-memory cache intact on failure
            }
    }

    fun getManualOverride(smsId: Long): Category? = manualOverrides[smsId]

    fun hasManualOverride(smsId: Long): Boolean = manualOverrides.containsKey(smsId)

    fun categorizeAll(transactions: List<ParsedTransaction>): Map<Long, Category> {
        return transactions.associate { it.smsId to categorizeTransaction(it) }
    }

    /**
     * Export category overrides in a format suitable for backup.
     * Converts the internal map of Category objects to a map of category IDs.
     *
     * @return Map of smsId to categoryId for all manual overrides
     */
    fun exportCategoryOverrides(): Map<Long, String> {
        return manualOverrides.mapValues { (_, category) -> category.id }
    }

    /**
     * Import category overrides from backup data.
     * Converts a map of category IDs back to Category objects by looking up
     * categories from DefaultCategories.
     *
     * This method validates that all category IDs exist in DefaultCategories.
     * Invalid category IDs are skipped with a warning.
     *
     * @param overrides Map of smsId to categoryId from backup
     * @return Number of successfully imported overrides
     */
    fun importCategoryOverrides(overrides: Map<Long, String>): Int {
        var importedCount = 0
        overrides.forEach { (smsId, categoryId) ->
            val category = DefaultCategories.ALL.find { it.id == categoryId }
            if (category != null) {
                manualOverrides[smsId] = category
                importedCount++
            } else {
                // Skip invalid category ID - could happen if backup is from
                // a different app version or corrupted
                android.util.Log.w(
                    "CategoryManager",
                    "Skipping override for smsId=$smsId: unknown categoryId=$categoryId"
                )
            }
        }
        return importedCount
    }

    /**
     * Clear all manual category overrides.
     * Useful for restore operations or resetting category assignments.
     */
    fun clearAllOverrides() {
        manualOverrides.clear()
    }

    /**
     * Get count of current manual overrides.
     *
     * @return Number of manual category overrides
     */
    fun getOverrideCount(): Int = manualOverrides.size
}
