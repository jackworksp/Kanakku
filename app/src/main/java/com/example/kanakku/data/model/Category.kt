package com.example.kanakku.data.model

import androidx.compose.ui.graphics.Color

/**
 * Domain model representing a transaction category.
 *
 * Categories can be hierarchical (support subcategories via parentId).
 * Categories can be either system-defined defaults or user-created custom categories.
 *
 * @property id Unique identifier (String representation of database ID)
 * @property name Display name of the category
 * @property icon Emoji or icon character for the category
 * @property color Color associated with the category for UI display
 * @property keywords Mutable list of keywords used for smart transaction categorization
 * @property parentId Optional parent category ID for subcategories (null for root categories)
 * @property isSystemCategory True if this is a default system category, false if user-created
 * @property isCustom Convenience flag, opposite of isSystemCategory (true for user-created categories)
 */
data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: Color,
    val keywords: MutableList<String>,
    val parentId: String? = null,
    val isSystemCategory: Boolean = false
) {
    /**
     * Convenience property to check if this is a custom (user-created) category.
     * Opposite of isSystemCategory.
     */
    val isCustom: Boolean
        get() = !isSystemCategory
}

object DefaultCategories {
    val FOOD = Category(
        id = "food",
        name = "Food & Dining",
        icon = "🍔",
        color = Color(0xFFFF9800),
        keywords = mutableListOf("swiggy", "zomato", "restaurant", "cafe", "food", "dining", "pizza", "burger", "biryani", "dominos", "kfc", "mcdonalds", "starbucks", "chaayos"),
        isSystemCategory = true
    )

    val SHOPPING = Category(
        id = "shopping",
        name = "Shopping",
        icon = "🛍️",
        color = Color(0xFFE91E63),
        keywords = mutableListOf("amazon", "flipkart", "myntra", "ajio", "shop", "store", "mall", "mart", "reliance", "dmart", "bigbasket", "grofers", "blinkit", "zepto"),
        isSystemCategory = true
    )

    val TRANSPORT = Category(
        id = "transport",
        name = "Transport",
        icon = "🚗",
        color = Color(0xFF2196F3),
        keywords = mutableListOf("uber", "ola", "rapido", "metro", "fuel", "petrol", "diesel", "iocl", "bpcl", "hpcl", "parking", "fastag", "toll"),
        isSystemCategory = true
    )

    val BILLS = Category(
        id = "bills",
        name = "Bills & Utilities",
        icon = "📄",
        color = Color(0xFF607D8B),
        keywords = mutableListOf("electricity", "water", "gas", "broadband", "mobile", "recharge", "airtel", "jio", "vi", "bsnl", "tata", "adani", "bescom", "bill"),
        isSystemCategory = true
    )

    val ENTERTAINMENT = Category(
        id = "entertainment",
        name = "Entertainment",
        icon = "🎬",
        color = Color(0xFF9C27B0),
        keywords = mutableListOf("netflix", "prime", "spotify", "hotstar", "movie", "game", "pvr", "inox", "bookmyshow", "youtube", "disney"),
        isSystemCategory = true
    )

    val HEALTH = Category(
        id = "health",
        name = "Health",
        icon = "💊",
        color = Color(0xFF4CAF50),
        keywords = mutableListOf("pharmacy", "hospital", "doctor", "medical", "apollo", "medplus", "netmeds", "pharmeasy", "practo", "clinic", "diagnostic"),
        isSystemCategory = true
    )

    val TRANSFER = Category(
        id = "transfer",
        name = "Transfers",
        icon = "💸",
        color = Color(0xFF00BCD4),
        keywords = mutableListOf("transfer", "sent to", "received from", "upi", "imps", "neft", "rtgs"),
        isSystemCategory = true
    )

    val ATM = Category(
        id = "atm",
        name = "ATM & Cash",
        icon = "🏧",
        color = Color(0xFF795548),
        keywords = mutableListOf("atm", "withdrawal", "cash", "withdraw"),
        isSystemCategory = true
    )

    val OTHER = Category(
        id = "other",
        name = "Other",
        icon = "📦",
        color = Color(0xFF9E9E9E),
        keywords = mutableListOf(),
        isSystemCategory = true
    )

    val ALL = listOf(FOOD, SHOPPING, TRANSPORT, BILLS, ENTERTAINMENT, HEALTH, TRANSFER, ATM, OTHER)
}
