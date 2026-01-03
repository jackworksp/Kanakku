package com.example.kanakku.ui.theme

import androidx.compose.ui.graphics.Color

// Material 3 base colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Transaction colors - Dark theme (lighter for readability on dark backgrounds)
val IncomeGreen80 = Color(0xFF81C784)  // Lighter green for dark mode
val ExpenseRed80 = Color(0xFFE57373)   // Lighter red for dark mode
val IncomeBackgroundGreen80 = Color(0xFF1B5E20).copy(alpha = 0.2f)  // Dark green tint
val ExpenseBackgroundRed80 = Color(0xFFB71C1C).copy(alpha = 0.2f)   // Dark red tint

// Transaction colors - Light theme (darker for readability on light backgrounds)
val IncomeGreen40 = Color(0xFF2E7D32)  // Dark green for light mode
val ExpenseRed40 = Color(0xFFC62828)   // Dark red for light mode
val IncomeBackgroundGreen40 = Color(0xFFE8F5E9)  // Light green background
val ExpenseBackgroundRed40 = Color(0xFFFFEBEE)   // Light red background

// Chart colors - Dark theme
val ChartBlue80 = Color(0xFF64B5F6)    // Lighter blue for dark mode
val ChartPurple80 = Color(0xFFBA68C8) // Lighter purple for dark mode

// Chart colors - Light theme
val ChartBlue40 = Color(0xFF1565C0)    // Dark blue for light mode
val ChartPurple40 = Color(0xFF6A1B9A) // Dark purple for light mode

// Accent colors for rankings/medals (work well in both themes)
val Gold = Color(0xFFFFD700)
val GoldDark = Color(0xFFB8860B)
val Silver = Color(0xFFC0C0C0)
val SilverDark = Color(0xFF808080)
val Bronze = Color(0xFFCD7F32)
val BronzeDark = Color(0xFF8B4513)

// Offline badge colors
val OfflineGreen = Color(0xFF4CAF50)
val OfflineGreenDark = Color(0xFF2E7D32)