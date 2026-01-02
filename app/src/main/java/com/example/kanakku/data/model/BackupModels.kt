package com.example.kanakku.data.model

/**
 * Metadata about a backup file
 */
data class BackupMetadata(
    val version: Int,                 // Backup format version for migration support
    val timestamp: Long,              // When the backup was created
    val deviceName: String?,          // Device name for reference
    val appVersion: String,           // App version that created the backup
    val transactionCount: Int,        // Number of transactions in backup
    val categoryOverrideCount: Int    // Number of manual category overrides
)

/**
 * Complete backup data container
 */
data class BackupData(
    val metadata: BackupMetadata,
    val transactions: List<SerializableTransaction>,
    val categoryOverrides: List<CategoryOverride>,
    val customCategories: List<SerializableCategory> = emptyList()  // For future use
)

/**
 * Serializable version of ParsedTransaction
 * Uses primitive types and strings for JSON compatibility
 */
data class SerializableTransaction(
    val smsId: Long,
    val amount: Double,
    val type: String,                 // DEBIT, CREDIT, UNKNOWN
    val merchant: String?,
    val accountNumber: String?,
    val referenceNumber: String?,
    val date: Long,
    val rawSms: String,
    val senderAddress: String,
    val balanceAfter: Double?,
    val location: String?
) {
    companion object {
        /**
         * Convert ParsedTransaction to serializable format
         */
        fun from(transaction: ParsedTransaction): SerializableTransaction {
            return SerializableTransaction(
                smsId = transaction.smsId,
                amount = transaction.amount,
                type = transaction.type.name,
                merchant = transaction.merchant,
                accountNumber = transaction.accountNumber,
                referenceNumber = transaction.referenceNumber,
                date = transaction.date,
                rawSms = transaction.rawSms,
                senderAddress = transaction.senderAddress,
                balanceAfter = transaction.balanceAfter,
                location = transaction.location
            )
        }
    }

    /**
     * Convert back to ParsedTransaction
     */
    fun toTransaction(): ParsedTransaction {
        return ParsedTransaction(
            smsId = smsId,
            amount = amount,
            type = when (type) {
                "DEBIT" -> TransactionType.DEBIT
                "CREDIT" -> TransactionType.CREDIT
                else -> TransactionType.UNKNOWN
            },
            merchant = merchant,
            accountNumber = accountNumber,
            referenceNumber = referenceNumber,
            date = date,
            rawSms = rawSms,
            senderAddress = senderAddress,
            balanceAfter = balanceAfter,
            location = location
        )
    }
}

/**
 * Serializable version of Category
 * Converts Color to hex string for JSON compatibility
 */
data class SerializableCategory(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,             // Color as hex string (e.g., "#FF9800")
    val keywords: List<String>
) {
    companion object {
        /**
         * Convert Category to serializable format
         */
        fun from(category: Category): SerializableCategory {
            return SerializableCategory(
                id = category.id,
                name = category.name,
                icon = category.icon,
                colorHex = category.color.toHexString(),
                keywords = category.keywords
            )
        }

        /**
         * Convert Color to hex string
         */
        private fun androidx.compose.ui.graphics.Color.toHexString(): String {
            val argb = (this.alpha * 255).toInt() shl 24 or
                    ((this.red * 255).toInt() shl 16) or
                    ((this.green * 255).toInt() shl 8) or
                    (this.blue * 255).toInt()
            return "#%08X".format(argb)
        }
    }

    /**
     * Convert back to Category
     */
    fun toCategory(): Category {
        return Category(
            id = id,
            name = name,
            icon = icon,
            color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(colorHex)),
            keywords = keywords
        )
    }
}

/**
 * Represents a manual category override
 * Maps transaction (by smsId) to a category (by categoryId)
 */
data class CategoryOverride(
    val smsId: Long,
    val categoryId: String
)
