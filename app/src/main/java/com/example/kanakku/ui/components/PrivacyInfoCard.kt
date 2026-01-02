package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Reusable privacy information card component.
 *
 * This card provides a summary of privacy and security features for backup,
 * with a "Learn More" button that opens a detailed dialog explaining:
 * - What data is included in backups
 * - Security features (encryption, key derivation)
 * - Privacy guarantees (user control, no app servers)
 *
 * The card uses a purple color scheme to visually distinguish it as
 * important privacy information.
 *
 * Features:
 * - Summary card with security icon
 * - "Learn More" button for detailed information
 * - Detailed dialog with comprehensive privacy information
 * - Styled with Material3 theme colors
 *
 * @param modifier Optional modifier for the card
 */
@Composable
fun PrivacyInfoCard(
    modifier: Modifier = Modifier
) {
    var showPrivacyInfo by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E5F5)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = Color(0xFF7B1FA2),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Privacy & Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7B1FA2)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your backup is encrypted with AES-256 and secured with your password. Data stays under your control.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { showPrivacyInfo = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Learn More")
            }
        }
    }

    // Privacy Info Dialog
    if (showPrivacyInfo) {
        PrivacyInfoDialog(
            onDismiss = { showPrivacyInfo = false }
        )
    }
}

/**
 * Detailed privacy information dialog.
 *
 * This dialog provides comprehensive information about:
 * - What data is included in backups (transactions, categories, settings)
 * - Security features (AES-256-GCM, PBKDF2, iteration count)
 * - Privacy guarantees (user control, no servers, storage options)
 *
 * @param onDismiss Callback when user dismisses the dialog
 */
@Composable
private fun PrivacyInfoDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = Color(0xFF7B1FA2)
            )
        },
        title = { Text("Privacy & Security") },
        text = {
            Column {
                Text(
                    text = "What's included in your backup:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• All parsed bank transactions", style = MaterialTheme.typography.bodySmall)
                Text("• Category assignments and overrides", style = MaterialTheme.typography.bodySmall)
                Text("• App preferences and settings", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Security features:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• AES-256-GCM encryption", style = MaterialTheme.typography.bodySmall)
                Text("• Password-based key derivation (PBKDF2)", style = MaterialTheme.typography.bodySmall)
                Text("• 100,000 iterations for key strength", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Privacy guarantees:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Data stays under your control", style = MaterialTheme.typography.bodySmall)
                Text("• No app servers involved", style = MaterialTheme.typography.bodySmall)
                Text("• Local storage or your personal Google Drive", style = MaterialTheme.typography.bodySmall)
                Text("• Google Drive uses app-specific folder", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}
