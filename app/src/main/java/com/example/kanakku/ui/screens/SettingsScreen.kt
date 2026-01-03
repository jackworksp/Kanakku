package com.example.kanakku.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

        // Display Settings Section
        SettingsSection(title = "Display") {
            // Dark Mode Toggle (System/Light/Dark options)
            SettingsDarkModeItem(
                isDarkMode = uiState.isDarkMode,
                onDarkModeChange = { viewModel.updateDarkMode(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Dynamic Colors Toggle
            SettingsToggleItem(
                title = "Dynamic Colors",
                description = "Use colors from your wallpaper",
                checked = uiState.isDynamicColors,
                onCheckedChange = { viewModel.updateDynamicColors(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Compact View Toggle
            SettingsToggleItem(
                title = "Compact View",
                description = "Show more transactions on screen",
                checked = uiState.isCompactView,
                onCheckedChange = { viewModel.updateCompactView(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Show Offline Badge Toggle
            SettingsToggleItem(
                title = "Show Offline Badge",
                description = "Display 'Local Data' indicator",
                checked = uiState.showOfflineBadge,
                onCheckedChange = { viewModel.updateShowOfflineBadge(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Notifications Section
        SettingsSection(title = "Notifications") {
            SettingsToggleItem(
                title = "Enable Notifications",
                description = "Get notified about new transactions",
                checked = uiState.isNotificationsEnabled,
                onCheckedChange = { viewModel.updateNotificationsEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Placeholder for remaining sections
        // - Analytics Preferences (4.4)
        // - Data Management (4.5)
        // - About (4.6)
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

/**
 * Settings section composable that groups related settings under a title.
 *
 * @param title The section title to display
 * @param content The settings items to display in this section
 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Settings toggle item with title, description, and switch.
 *
 * @param title The main title for the setting
 * @param description Optional description text explaining the setting
 * @param checked Current toggle state
 * @param onCheckedChange Callback when toggle state changes
 */
@Composable
private fun SettingsToggleItem(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Dark mode settings item with System/Light/Dark options.
 *
 * @param isDarkMode Current dark mode setting: true = dark, false = light, null = system
 * @param onDarkModeChange Callback when dark mode setting changes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDarkModeItem(
    isDarkMode: Boolean?,
    onDarkModeChange: (Boolean?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Dark Mode",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose your preferred theme",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented button for System/Light/Dark
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            val options = listOf(
                Triple("System", null as Boolean?, 0),
                Triple("Light", false, 1),
                Triple("Dark", true, 2)
            )

            options.forEach { (label, value, index) ->
                SegmentedButton(
                    selected = isDarkMode == value,
                    onClick = { onDarkModeChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    )
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
