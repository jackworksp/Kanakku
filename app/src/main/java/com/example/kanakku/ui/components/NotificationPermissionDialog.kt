package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A dialog that explains notification benefits and prompts the user to grant
 * POST_NOTIFICATIONS permission on Android 13+ devices.
 *
 * This dialog informs users that:
 * - They will receive instant alerts for new transactions
 * - Real-time spending awareness helps avoid overdrafts
 * - Notifications work even when the app is closed
 * - They can customize notification settings later
 *
 * @param onDismiss Callback invoked when the user dismisses the dialog (doesn't grant permission)
 * @param onGrantPermission Callback invoked when the user chooses to grant permission
 */
@Composable
fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onGrantPermission: () -> Unit
) {
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
                text = "Enable Transaction Alerts?",
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
                    text = "Get notified instantly when money enters or leaves your account:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                NotificationFeatureRow(
                    icon = Icons.Default.Speed,
                    title = "Instant Awareness",
                    description = "Know immediately when a transaction happens, even when the app is closed."
                )

                NotificationFeatureRow(
                    icon = Icons.Default.Warning,
                    title = "Avoid Overdrafts",
                    description = "Real-time alerts help you track spending and prevent unexpected charges."
                )

                NotificationFeatureRow(
                    icon = Icons.Default.Notifications,
                    title = "Customizable Settings",
                    description = "Control what information appears in notifications from the app settings."
                )

                Text(
                    text = "You can change this preference anytime in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGrantPermission,
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
                Text("Not Now")
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}

/**
 * A row displaying a notification feature with an icon, title, and description.
 *
 * @param icon The icon representing this feature
 * @param title The feature title
 * @param description A brief description of the feature
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
