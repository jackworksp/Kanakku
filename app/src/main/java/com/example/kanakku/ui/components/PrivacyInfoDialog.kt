package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A dialog that explains the app's offline-first architecture and privacy benefits
 * to first-time users.
 *
 * This dialog informs users that:
 * - All data stays on their device
 * - The app works completely offline
 * - No internet connection is required
 * - Their financial data is private and secure
 *
 * @param onDismiss Callback invoked when the user dismisses the dialog
 */
@Composable
fun PrivacyInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Privacy",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Your Privacy Matters",
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
                    text = "Kanakku is built with your privacy in mind. Here's what makes it special:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PrivacyFeatureRow(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Data Stays on Your Device",
                    description = "All your financial information is stored locally and never leaves your phone."
                )

                PrivacyFeatureRow(
                    icon = Icons.Default.CloudOff,
                    title = "Works Completely Offline",
                    description = "No internet connection required. Use the app anywhere, anytime."
                )

                PrivacyFeatureRow(
                    icon = Icons.Default.Lock,
                    title = "Secure & Private",
                    description = "Your transaction data is yours alone. No cloud storage, no tracking."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Got It!")
            }
        },
        modifier = Modifier.padding(16.dp)
    )
}

/**
 * A row displaying a privacy feature with an icon, title, and description.
 *
 * @param icon The icon representing this feature
 * @param title The feature title
 * @param description A brief description of the feature
 */
@Composable
private fun PrivacyFeatureRow(
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
