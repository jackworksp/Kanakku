package com.example.kanakku.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Room entity for persisting user's manual category assignments.
 * Allows users to override automatically detected transaction categories.
 */
@Entity(
    tableName = "category_overrides",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["smsId"],
            childColumns = ["smsId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CategoryOverrideEntity(
    @PrimaryKey
    val smsId: Long,
    val categoryId: String
)
