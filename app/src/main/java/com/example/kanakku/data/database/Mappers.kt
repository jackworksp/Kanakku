package com.example.kanakku.data.database

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.kanakku.core.error.ErrorHandler
import com.example.kanakku.data.database.entity.CustomCategoryEntity
import com.example.kanakku.data.database.entity.TransactionEntity
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction

/**
 * Extension functions for mapping between database entities and domain models.
 *
 * These mappers provide bidirectional conversion between:
 * - TransactionEntity (database layer) <-> ParsedTransaction (domain layer)
 * - CustomCategoryEntity (database layer) <-> Category (domain layer)
 *
 * The mapping handles data type conversions (e.g., Long ID to String, hex color to Color object)
 * while preserving all semantic information.
 */

// ==================== Transaction Mappers ====================

/**
 * Converts a ParsedTransaction domain model to a TransactionEntity for database storage.
 *
 * @return TransactionEntity with all fields mapped from the domain model
 */
fun ParsedTransaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        smsId = this.smsId,
        amount = this.amount,
        type = this.type,
        merchant = this.merchant,
        accountNumber = this.accountNumber,
        referenceNumber = this.referenceNumber,
        date = this.date,
        rawSms = this.rawSms,
        senderAddress = this.senderAddress,
        balanceAfter = this.balanceAfter,
        location = this.location
    )
}

/**
 * Converts a TransactionEntity from the database to a ParsedTransaction domain model.
 *
 * @return ParsedTransaction with all fields mapped from the database entity
 */
fun TransactionEntity.toDomain(): ParsedTransaction {
    return ParsedTransaction(
        smsId = this.smsId,
        amount = this.amount,
        type = this.type,
        merchant = this.merchant,
        accountNumber = this.accountNumber,
        referenceNumber = this.referenceNumber,
        date = this.date,
        rawSms = this.rawSms,
        senderAddress = this.senderAddress,
        balanceAfter = this.balanceAfter,
        location = this.location
    )
}

// ==================== Category Mappers ====================

/**
 * Converts a Category domain model to a CustomCategoryEntity for database storage.
 *
 * Note: This conversion requires additional context (sortOrder, timestamps) that must be
 * provided by the caller. For saving new categories, use CategoryRepository.saveCategory()
 * which handles these fields automatically.
 *
 * @param parentId Optional parent category ID for subcategories (null for root categories)
 * @param sortOrder Display order within parent or root level
 * @param createdAt Creation timestamp (Unix milliseconds)
 * @param updatedAt Last update timestamp (Unix milliseconds)
 * @return CustomCategoryEntity with all fields mapped from the domain model
 */
fun Category.toEntity(
    parentId: Long? = null,
    sortOrder: Int = 0,
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis()
): CustomCategoryEntity {
    return CustomCategoryEntity(
        id = this.id.toLongOrNull() ?: 0L, // Convert String ID back to Long, use 0 for auto-generation
        name = this.name,
        icon = this.icon,
        colorHex = this.color.toHexString(),
        keywords = this.keywords.joinToString(","),
        parentId = parentId ?: this.parentId?.toLongOrNull(),
        isSystemCategory = this.isSystemCategory,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Converts a CustomCategoryEntity from the database to a Category domain model.
 *
 * @return Category with all fields mapped from the database entity
 */
fun CustomCategoryEntity.toDomain(): Category {
    return Category(
        id = this.id.toString(), // Convert Long ID to String for domain model
        name = this.name,
        icon = this.icon,
        color = parseColorFromHex(this.colorHex),
        keywords = parseKeywords(this.keywords),
        parentId = this.parentId?.toString(), // Convert Long to String
        isSystemCategory = this.isSystemCategory
    )
}

// ==================== Helper Functions ====================

/**
 * Converts a Color to a hex string (e.g., #RRGGBB).
 *
 * @return Hex color string
 */
private fun Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

/**
 * Parses a hex color string to a Color object.
 * Handles formats: #RGB, #RRGGBB, #AARRGGBB
 *
 * @param hex The hex color string
 * @return Color object, defaults to gray if parsing fails
 */
private fun parseColorFromHex(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val argb = when (cleanHex.length) {
            3 -> {
                // #RGB -> #RRGGBB
                val r = cleanHex[0].toString().repeat(2)
                val g = cleanHex[1].toString().repeat(2)
                val b = cleanHex[2].toString().repeat(2)
                0xFF000000.toInt() or (r.toInt(16) shl 16) or (g.toInt(16) shl 8) or b.toInt(16)
            }
            6 -> {
                // #RRGGBB
                0xFF000000.toInt() or cleanHex.toInt(16)
            }
            8 -> {
                // #AARRGGBB
                cleanHex.toLong(16).toInt()
            }
            else -> 0xFF9E9E9E.toInt() // Default gray
        }
        Color(argb)
    } catch (e: Exception) {
        ErrorHandler.handleError(e, "Parse color from hex: $hex")
        Color(0xFF9E9E9E) // Default gray on parse failure
    }
}

/**
 * Parses a comma-separated keywords string to a mutable list.
 *
 * @param keywords Comma-separated keywords string
 * @return Mutable list of keywords, empty list if input is blank
 */
private fun parseKeywords(keywords: String): MutableList<String> {
    return if (keywords.isBlank()) {
        mutableListOf()
    } else {
        keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    }
}
