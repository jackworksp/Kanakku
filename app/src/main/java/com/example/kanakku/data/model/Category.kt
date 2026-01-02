package com.example.kanakku.data.model

import androidx.compose.ui.graphics.Color

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: Color,
    val keywords: List<String>
)

object DefaultCategories {
    val FOOD = Category(
        id = "food",
        name = "Food & Dining",
        icon = "🍔",
        color = Color(0xFFFF9800),
        keywords = listOf("swiggy", "zomato", "restaurant", "cafe", "food", "dining", "pizza", "burger", "biryani", "dominos", "kfc", "mcdonalds", "starbucks", "chaayos")
    )

    val SHOPPING = Category(
        id = "shopping",
        name = "Shopping",
        icon = "🛍️",
        color = Color(0xFFE91E63),
        keywords = listOf("amazon", "flipkart", "myntra", "ajio", "shop", "store", "mall", "mart", "reliance", "dmart", "bigbasket", "grofers", "blinkit", "zepto")
    )

    val TRANSPORT = Category(
        id = "transport",
        name = "Transport",
        icon = "🚗",
        color = Color(0xFF2196F3),
        keywords = listOf("uber", "ola", "rapido", "metro", "fuel", "petrol", "diesel", "iocl", "bpcl", "hpcl", "parking", "fastag", "toll")
    )

    val BILLS = Category(
        id = "bills",
        name = "Bills & Utilities",
        icon = "📄",
        color = Color(0xFF607D8B),
        keywords = listOf("electricity", "water", "gas", "broadband", "mobile", "recharge", "airtel", "jio", "vi", "bsnl", "tata", "adani", "bescom", "bill")
    )

    val ENTERTAINMENT = Category(
        id = "entertainment",
        name = "Entertainment",
        icon = "🎬",
        color = Color(0xFF9C27B0),
        keywords = listOf("netflix", "prime", "spotify", "hotstar", "movie", "game", "pvr", "inox", "bookmyshow", "youtube", "disney")
    )

    val HEALTH = Category(
        id = "health",
        name = "Health",
        icon = "💊",
        color = Color(0xFF4CAF50),
        keywords = listOf("pharmacy", "hospital", "doctor", "medical", "apollo", "medplus", "netmeds", "pharmeasy", "practo", "clinic", "diagnostic")
    )

    val TRANSFER = Category(
        id = "transfer",
        name = "Transfers",
        icon = "💸",
        color = Color(0xFF00BCD4),
        keywords = listOf("transfer", "sent to", "received from", "upi", "imps", "neft", "rtgs")
    )

    val ATM = Category(
        id = "atm",
        name = "ATM & Cash",
        icon = "🏧",
        color = Color(0xFF795548),
        keywords = listOf("atm", "withdrawal", "cash", "withdraw")
    )

    val OTHER = Category(
        id = "other",
        name = "Other",
        icon = "📦",
        color = Color(0xFF9E9E9E),
        keywords = emptyList()
    )

    val ALL = listOf(FOOD, SHOPPING, TRANSPORT, BILLS, ENTERTAINMENT, HEALTH, TRANSFER, ATM, OTHER)
}
