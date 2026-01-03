package com.example.kanakku.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting learned merchant-to-category mappings.
 * Stores normalized merchant names with their assigned category IDs to enable
 * automatic categorization of future transactions from the same merchant.
 *
 * The merchant name is normalized (lowercase, trimmed) to ensure consistent matching
 * across different transaction sources and formats.
 */
@Entity(tableName = "merchant_category_mappings")
data class MerchantCategoryMappingEntity(
    /**
     * Normalized merchant name (lowercase, trimmed).
     * Serves as the primary key for merchant lookups.
     */
    @PrimaryKey
    val merchantName: String,

    /**
     * Category ID that this merchant should be assigned to.
     * References the TransactionCategory enum value.
     */
    val categoryId: String,

    /**
     * Timestamp (epoch millis) when this mapping was last updated.
     * Used to track when the user last corrected this merchant's category.
     */
    val updatedAt: Long
)
