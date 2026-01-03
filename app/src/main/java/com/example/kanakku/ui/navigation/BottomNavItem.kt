package com.example.kanakku.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Transactions : BottomNavItem(
        route = "transactions",
        title = "Transactions",
        icon = Icons.AutoMirrored.Filled.List
    )

    data object Analytics : BottomNavItem(
        route = "analytics",
        title = "Analytics",
        icon = Icons.Default.Analytics
    )

    data object Budget : BottomNavItem(
        route = "budget",
        title = "Budget",
        icon = Icons.Default.AccountBalance
    )

    data object Categories : BottomNavItem(
        route = "categories",
        title = "Categories",
        icon = Icons.Default.Category
    )

    data object Settings : BottomNavItem(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings
    )

    companion object {
        val items = listOf(Transactions, Analytics, Categories, Settings)
    }
}
