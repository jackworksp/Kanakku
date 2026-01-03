package com.example.kanakku.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kanakku.ui.settings.SettingsViewModel

/**
 * Settings screen where users can configure app behavior and preferences.
 *
 * This screen provides a centralized location for users to:
 * - Configure display preferences (dark mode, dynamic colors, compact view, offline badge)
 * - Manage notification settings
 * - Set default analytics preferences (time period, auto-categorize)
 * - Control data management options (clear all data)
 * - View app information (version, privacy policy)
 *
 * The screen uses a scrollable column layout with distinct sections for each
 * category of settings, following Material3 design guidelines.
 *
 * Features:
 * - Display Settings: Theme and visual customization
 * - Notifications: SMS notification preferences
 * - Analytics: Default time period and auto-categorization
 * - Data Management: Clear all data with confirmation
 * - About: App version and privacy policy
 *
 * @param viewModel SettingsViewModel for state management and business logic
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Dialog state
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    // Initialize ViewModel on first composition
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    // Track when data clearing completes successfully
    LaunchedEffect(uiState.isClearing) {
        if (!uiState.isClearing && showClearDataDialog && uiState.errorMessage == null) {
            // Data cleared successfully
            showClearDataDialog = false
            showSuccessMessage = true
        }
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

        // Analytics Preferences Section
        SettingsSection(title = "Analytics") {
            // Default Time Period Selector
            SettingsTimePeriodItem(
                currentPeriod = uiState.defaultAnalyticsPeriod,
                onPeriodChange = { viewModel.updateDefaultAnalyticsPeriod(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Auto-Categorize Toggle
            SettingsToggleItem(
                title = "Auto-Categorize",
                description = "Automatically categorize transactions based on patterns",
                checked = uiState.isAutoCategorize,
                onCheckedChange = { viewModel.updateAutoCategorize(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Data Management Section
        SettingsSection(title = "Data Management") {
            SettingsActionItem(
                title = "Clear All Data",
                description = "Delete all transactions and reset app data",
                icon = Icons.Default.DeleteForever,
                iconTint = MaterialTheme.colorScheme.error,
                isLoading = uiState.isClearing,
                onClick = { showClearDataDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About Section
        SettingsSection(title = "About") {
            // App Version
            SettingsInfoItem(
                title = "App Version",
                value = uiState.appVersion
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Privacy Policy
            SettingsActionItem(
                title = "Privacy Policy",
                description = "Learn about our offline-first approach",
                icon = Icons.Default.Info,
                onClick = { showPrivacyPolicyDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Branding Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kanakku",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your Financial Companion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        ClearDataConfirmationDialog(
            isClearing = uiState.isClearing,
            onConfirm = { viewModel.clearAllData() },
            onDismiss = {
                if (!uiState.isClearing) {
                    showClearDataDialog = false
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }

    // Success Snackbar
    if (showSuccessMessage) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showSuccessMessage = false }) {
                    Text("OK")
                }
            }
        ) {
            Text("All data cleared successfully")
        }
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

/**
 * Time period settings item with DAY/WEEK/MONTH/YEAR options.
 *
 * @param currentPeriod Current selected time period (DAY, WEEK, MONTH, or YEAR)
 * @param onPeriodChange Callback when time period selection changes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTimePeriodItem(
    currentPeriod: String,
    onPeriodChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Default Time Period",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Default view for analytics screen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented button for DAY/WEEK/MONTH/YEAR
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            val options = listOf(
                Pair("Day", "DAY"),
                Pair("Week", "WEEK"),
                Pair("Month", "MONTH"),
                Pair("Year", "YEAR")
            )

            options.forEachIndexed { index, (label, value) ->
                SegmentedButton(
                    selected = currentPeriod == value,
                    onClick = { onPeriodChange(value) },
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

/**
 * Settings action item for clickable actions (not toggles).
 *
 * @param title The main title for the action
 * @param description Optional description text explaining the action
 * @param icon Optional icon to display at the start
 * @param iconTint Optional tint color for the icon
 * @param isLoading Whether the action is currently loading
 * @param onClick Callback when the item is clicked
 */
@Composable
private fun SettingsActionItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color? = null,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon (if provided)
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = iconTint ?: MaterialTheme.colorScheme.onSurface
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

        // Loading indicator or chevron
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Settings info item for displaying read-only information.
 *
 * @param title The title label for the info
 * @param value The value to display
 */
@Composable
private fun SettingsInfoItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Confirmation dialog for clearing all app data.
 *
 * Displays a warning message and requires explicit user confirmation before
 * proceeding with the destructive data clearing operation.
 *
 * @param isClearing Whether the clearing operation is in progress
 * @param onConfirm Callback when user confirms the clear action
 * @param onDismiss Callback when user dismisses the dialog
 */
@Composable
private fun ClearDataConfirmationDialog(
    isClearing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isClearing) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Clear All Data?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "This will permanently delete:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• All transactions\n• Category overrides\n• Sync metadata",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your theme preferences will be preserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isClearing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isClearing) "Clearing..." else "Clear Data")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isClearing
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Privacy Policy dialog explaining Kanakku's offline-first approach.
 *
 * Displays comprehensive information about the app's privacy-focused design,
 * including data storage, permissions, and offline-first architecture.
 *
 * @param onDismiss Callback when user dismisses the dialog
 */
@Composable
private fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Offline-First Approach
                Text(
                    text = "Offline-First & Privacy-Focused",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Kanakku is designed with your privacy in mind. All your financial data stays on your device and is never transmitted to any external servers.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Data Storage
                Text(
                    text = "Data Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• All transactions are stored locally in an encrypted database\n" +
                            "• Your preferences are securely encrypted (AES256_GCM)\n" +
                            "• No cloud sync or remote backups\n" +
                            "• No analytics or tracking services",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Permissions
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• SMS Read: To detect and categorize financial transactions from bank SMS messages\n" +
                            "• Storage: For local database and backup files\n" +
                            "• No internet permission required",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Your Control
                Text(
                    text = "Your Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• You can clear all data at any time\n" +
                            "• Uninstalling the app removes all data\n" +
                            "• No account required\n" +
                            "• No data collection or sharing",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your financial data belongs to you. We believe in complete transparency and user control.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}
