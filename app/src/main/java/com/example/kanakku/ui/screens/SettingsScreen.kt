package com.example.kanakku.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.BudgetAlertSettings
import com.example.kanakku.data.model.DayOfWeek
import com.example.kanakku.data.model.LargeTransactionSettings
import com.example.kanakku.data.model.WeeklySummarySettings
import com.example.kanakku.ui.components.NotificationPermissionDialog
import com.example.kanakku.ui.components.hasNotificationPermission
import java.util.Locale

/**
 * Settings screen for configuring notification preferences and spending thresholds.
 *
 * This screen provides controls for:
 * - Budget alert notifications (80%, 100% thresholds)
 * - Large transaction alerts with configurable threshold
 * - Weekly spending summary scheduling
 *
 * All settings are persisted via AppPreferences and apply immediately.
 *
 * @param budgetAlertSettings Current budget alert settings
 * @param largeTransactionSettings Current large transaction alert settings
 * @param weeklySummarySettings Current weekly summary settings
 * @param onBudgetAlertsEnabledChange Callback when budget alerts enabled state changes
 * @param onBudgetAlert80PercentChange Callback when 80% threshold setting changes
 * @param onBudgetAlert100PercentChange Callback when 100% threshold setting changes
 * @param onLargeTransactionEnabledChange Callback when large transaction alerts enabled state changes
 * @param onLargeTransactionThresholdChange Callback when threshold amount changes
 * @param onWeeklySummaryEnabledChange Callback when weekly summary enabled state changes
 * @param onWeeklySummaryDayChange Callback when summary day changes
 * @param onWeeklySummaryHourChange Callback when summary hour changes
 */
@Composable
fun SettingsScreen(
    budgetAlertSettings: BudgetAlertSettings,
    largeTransactionSettings: LargeTransactionSettings,
    weeklySummarySettings: WeeklySummarySettings,
    onBudgetAlertsEnabledChange: (Boolean) -> Unit,
    onBudgetAlert80PercentChange: (Boolean) -> Unit,
    onBudgetAlert100PercentChange: (Boolean) -> Unit,
    onLargeTransactionEnabledChange: (Boolean) -> Unit,
    onLargeTransactionThresholdChange: (Double) -> Unit,
    onWeeklySummaryEnabledChange: (Boolean) -> Unit,
    onWeeklySummaryDayChange: (DayOfWeek) -> Unit,
    onWeeklySummaryHourChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showDayPickerDialog by remember { mutableStateOf(false) }
    var showHourPickerDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(0.dp))
            SettingsHeader()
        }

        // Budget Alerts Section
        item {
            BudgetAlertsSection(
                settings = budgetAlertSettings,
                onEnabledChange = { enabled ->
                    if (enabled && !hasNotificationPermission(context)) {
                        showPermissionDialog = true
                    } else {
                        onBudgetAlertsEnabledChange(enabled)
                    }
                },
                onNotifyAt80PercentChange = onBudgetAlert80PercentChange,
                onNotifyAt100PercentChange = onBudgetAlert100PercentChange
            )
        }

        // Large Transaction Alerts Section
        item {
            LargeTransactionAlertsSection(
                settings = largeTransactionSettings,
                onEnabledChange = { enabled ->
                    if (enabled && !hasNotificationPermission(context)) {
                        showPermissionDialog = true
                    } else {
                        onLargeTransactionEnabledChange(enabled)
                    }
                },
                onThresholdClick = { showThresholdDialog = true }
            )
        }

        // Weekly Summary Section
        item {
            WeeklySummarySection(
                settings = weeklySummarySettings,
                onEnabledChange = { enabled ->
                    if (enabled && !hasNotificationPermission(context)) {
                        showPermissionDialog = true
                    } else {
                        onWeeklySummaryEnabledChange(enabled)
                    }
                },
                onDayClick = { showDayPickerDialog = true },
                onHourClick = { showHourPickerDialog = true }
            )
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Permission Dialog
    if (showPermissionDialog) {
        NotificationPermissionDialog(
            onPermissionResult = { granted ->
                showPermissionDialog = false
                // Permission granted, user can now enable settings in the UI
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }

    // Threshold Amount Dialog
    if (showThresholdDialog) {
        ThresholdAmountDialog(
            currentThreshold = largeTransactionSettings.threshold,
            onConfirm = { newThreshold ->
                onLargeTransactionThresholdChange(newThreshold)
                showThresholdDialog = false
            },
            onDismiss = { showThresholdDialog = false }
        )
    }

    // Day Picker Dialog
    if (showDayPickerDialog) {
        DayPickerDialog(
            currentDay = weeklySummarySettings.dayOfWeek,
            onDaySelected = { day ->
                onWeeklySummaryDayChange(day)
                showDayPickerDialog = false
            },
            onDismiss = { showDayPickerDialog = false }
        )
    }

    // Hour Picker Dialog
    if (showHourPickerDialog) {
        HourPickerDialog(
            currentHour = weeklySummarySettings.hourOfDay,
            onHourSelected = { hour ->
                onWeeklySummaryHourChange(hour)
                showHourPickerDialog = false
            },
            onDismiss = { showHourPickerDialog = false }
        )
    }
}

/**
 * Header section displaying the settings screen title.
 */
@Composable
private fun SettingsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Configure notifications and spending alerts",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Budget alerts settings section.
 */
@Composable
private fun BudgetAlertsSection(
    settings: BudgetAlertSettings,
    onEnabledChange: (Boolean) -> Unit,
    onNotifyAt80PercentChange: (Boolean) -> Unit,
    onNotifyAt100PercentChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Budget Alerts",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Budget Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Get notified when approaching budget limits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Enable Budget Alerts Toggle
            SettingRow(
                title = "Enable Budget Alerts",
                description = "Receive notifications for budget thresholds",
                checked = settings.enabled,
                onCheckedChange = onEnabledChange
            )

            // 80% Threshold Toggle
            SettingRow(
                title = "80% Warning",
                description = "Alert when reaching 80% of budget",
                checked = settings.notifyAt80Percent,
                onCheckedChange = onNotifyAt80PercentChange,
                enabled = settings.enabled
            )

            // 100% Threshold Toggle
            SettingRow(
                title = "100% Limit Alert",
                description = "Alert when budget limit is reached",
                checked = settings.notifyAt100Percent,
                onCheckedChange = onNotifyAt100PercentChange,
                enabled = settings.enabled
            )
        }
    }
}

/**
 * Large transaction alerts settings section.
 */
@Composable
private fun LargeTransactionAlertsSection(
    settings: LargeTransactionSettings,
    onEnabledChange: (Boolean) -> Unit,
    onThresholdClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Large Transaction Alerts",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Large Transaction Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Alert for unusually large spending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Enable Large Transaction Alerts Toggle
            SettingRow(
                title = "Enable Large Transaction Alerts",
                description = "Get notified about large spending",
                checked = settings.enabled,
                onCheckedChange = onEnabledChange
            )

            // Threshold Setting (clickable)
            SettingRowClickable(
                title = "Alert Threshold",
                description = "₹${formatAmount(settings.threshold)}",
                onClick = onThresholdClick,
                enabled = settings.enabled,
                icon = Icons.Default.Edit
            )
        }
    }
}

/**
 * Weekly summary settings section.
 */
@Composable
private fun WeeklySummarySection(
    settings: WeeklySummarySettings,
    onEnabledChange: (Boolean) -> Unit,
    onDayClick: () -> Unit,
    onHourClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Weekly Summary",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weekly Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Receive weekly spending overview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Enable Weekly Summary Toggle
            SettingRow(
                title = "Enable Weekly Summary",
                description = "Get a weekly spending report",
                checked = settings.enabled,
                onCheckedChange = onEnabledChange
            )

            // Day of Week Setting
            SettingRowClickable(
                title = "Day of Week",
                description = settings.dayOfWeek.displayName,
                onClick = onDayClick,
                enabled = settings.enabled,
                icon = Icons.Default.CalendarToday
            )

            // Time of Day Setting
            SettingRowClickable(
                title = "Time",
                description = formatHour(settings.hourOfDay),
                onClick = onHourClick,
                enabled = settings.enabled,
                icon = Icons.Default.AccessTime
            )
        }
    }
}

/**
 * A settings row with a switch toggle.
 */
@Composable
private fun SettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

/**
 * A clickable settings row with an icon (for opening dialogs).
 */
@Composable
private fun SettingRowClickable(
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Dialog for setting the large transaction threshold amount.
 */
@Composable
private fun ThresholdAmountDialog(
    currentThreshold: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var thresholdText by remember { mutableStateOf(currentThreshold.toInt().toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Threshold",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Set Alert Threshold",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "You'll be notified when a transaction exceeds this amount.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { newValue ->
                        thresholdText = newValue
                        isError = newValue.toDoubleOrNull() == null || newValue.toDoubleOrNull()!! <= 0
                    },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("5000") },
                    prefix = { Text("₹") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Please enter a valid amount greater than 0") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val threshold = thresholdText.toDoubleOrNull()
                    if (threshold != null && threshold > 0) {
                        onConfirm(threshold)
                    }
                },
                enabled = !isError && thresholdText.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for selecting the day of week for weekly summary.
 */
@Composable
private fun DayPickerDialog(
    currentDay: DayOfWeek,
    onDaySelected: (DayOfWeek) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select Day",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Select Day",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Choose which day to receive your weekly summary:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                DayOfWeek.entries.forEach { day ->
                    Surface(
                        onClick = {
                            onDaySelected(day)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (day == currentDay)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = day.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (day == currentDay)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (day == currentDay) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog for selecting the hour of day for weekly summary.
 */
@Composable
private fun HourPickerDialog(
    currentHour: Int,
    onHourSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Select Time",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Select Time",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Choose what time to receive your weekly summary:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Common hours (morning, afternoon, evening)
                val commonHours = listOf(9, 12, 15, 18, 21)
                commonHours.forEach { hour ->
                    Surface(
                        onClick = {
                            onHourSelected(hour)
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (hour == currentHour)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatHour(hour),
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (hour == currentHour)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (hour == currentHour) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Formats a double amount to a string with proper formatting.
 */
private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%,.0f", amount)
}

/**
 * Formats an hour (0-23) to a readable time string (e.g., "9:00 AM").
 */
private fun formatHour(hour: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:00 $period"
}
