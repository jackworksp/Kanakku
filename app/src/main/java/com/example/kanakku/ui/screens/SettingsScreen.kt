package com.example.kanakku.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kanakku.ui.settings.SettingsViewModel

/**
 * Settings screen where users can configure app behavior and preferences.
 *
 * This screen provides a centralized location for users to:
 * - Configure display preferences (dark mode, dynamic colors, compact view)
 * - Manage notification settings
 * - Set default analytics preferences
 * - Control data management options
 * - View app information
 *
 * The screen uses a scrollable column layout with distinct sections for each
 * category of settings, following Material3 design guidelines.
 *
 * @param viewModel SettingsViewModel for state management and business logic
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Initialize ViewModel on first composition
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Screen Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Placeholder for settings sections
        // Sections will be added in subsequent subtasks:
        // - Display Settings (4.2)
        // - Notifications (4.3)
        // - Analytics Preferences (4.4)
        // - Data Management (4.5)
        // - About (4.6)

        Text(
            text = "Settings sections coming soon...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Error Snackbar
    uiState.errorMessage?.let { error ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(error)
        }
    }
}
