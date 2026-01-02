package com.example.kanakku.data.category

import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories
import com.example.kanakku.data.model.ParsedTransaction

class CategoryManager {

    private val manualOverrides = mutableMapOf<Long, Category>()

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

    fun setManualOverride(smsId: Long, category: Category) {
        manualOverrides[smsId] = category
    }

    fun removeManualOverride(smsId: Long) {
        manualOverrides.remove(smsId)
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
