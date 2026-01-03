package com.example.kanakku.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for persisting custom user-defined categories.
 * Supports hierarchical categories through parentId relationship.
 * System categories (defaults) are marked with isSystemCategory flag.
 */
@Entity(
    tableName = "custom_categories",
    indices = [
        Index(value = ["name"]),
        Index(value = ["parentId"]),
        Index(value = ["isSystemCategory"]),
        Index(value = ["sortOrder"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CustomCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val keywords: String, // Comma-separated keywords for smart categorization
    val parentId: Long?, // Null for root categories, references parent category for subcategories
    val isSystemCategory: Boolean, // True for default categories, false for user-created
    val sortOrder: Int, // Display order within parent or root level
    val createdAt: Long, // Unix timestamp
    val updatedAt: Long // Unix timestamp
)
