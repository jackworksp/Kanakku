package com.example.kanakku.ui.savings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Main screen for displaying savings goals.
 *
 * TODO: Full implementation in subtask 6.7
 * This is a placeholder that shows a list of active goals, completed goals section,
 * FAB to add new goal, and savings suggestions.
 */
@Composable
fun SavingsGoalsScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Savings Goals Screen\n(Implementation pending)",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
