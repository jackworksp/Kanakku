package com.example.kanakku.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * A dialog that requests notification permission (Android 13+ / API 33+) with a clear rationale
 * explaining the benefits of enabling notifications.
 *
 * This dialog explains that notifications help users:
 * - Stay informed about budget thresholds (80%, 100%)
 * - Get alerted about large transactions for fraud awareness
 * - Receive weekly spending summaries without opening the app
 *
 * On Android versions below 13 (API < 33), notifications are granted by default and this dialog
 * should not be shown.
 *
 * @param onPermissionResult Callback invoked with the permission result (granted: true/false)
 * @param onDismiss Callback invoked when the user dismisses the dialog without action
 */
@Composable
fun NotificationPermissionDialog(
    onPermissionResult: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Permission launcher for POST_NOTIFICATIONS (Android 13+ only)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Stay Informed About Your Spending",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enable notifications to receive timely alerts about your finances:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                NotificationFeatureRow(
                    icon = Icons.Default.Warning,
                    title = "Budget Alerts",
                    description = "Get notified when you reach 80% and 100% of your budget to help control spending."
                )

                NotificationFeatureRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "Large Transaction Alerts",
                    description = "Stay aware of unusually large transactions for fraud detection and peace of mind."
                )

                NotificationFeatureRow(
                    icon = Icons.Default.Schedule,
                    title = "Weekly Summaries",
                    description = "Receive a weekly spending overview without opening the app."
                )

                Text(
                    text = "You can customize or disable these notifications anytime in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Only request permission on Android 13+ (API 33+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // On older versions, notifications are granted by default
                        onPermissionResult(true)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Notifications")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Maybe Later")
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}

/**
 * A row displaying a notification feature with an icon, title, and description.
 *
 * @param icon The icon representing this notification feature
 * @param title The feature title
 * @param description A brief description of the notification feature
 */
@Composable
private fun NotificationFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Checks if notification permission is granted.
 *
 * On Android 13+ (API 33+), checks POST_NOTIFICATIONS permission.
 * On older versions, returns true since notifications are granted by default.
 *
 * @return true if notifications are allowed, false otherwise
 */
fun hasNotificationPermission(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        // Notifications are granted by default on Android < 13
        true
    }
}
