package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.ui.backup.OperationType

/**
 * Reusable progress card component for backup/restore operations.
 *
 * This card displays a circular progress indicator along with the operation type
 * and optional progress status text. It uses a primary container background to
 * make the progress visually distinct from other UI elements.
 *
 * Features:
 * - Circular progress indicator
 * - Operation-specific title text
 * - Optional progress status text
 * - Styled with Material3 theme colors
 *
 * @param operationType The type of operation currently in progress
 * @param progress Optional status text describing current progress (e.g., "Encrypting backup...")
 * @param modifier Optional modifier for the card
 */
@Composable
fun BackupProgressCard(
    operationType: OperationType,
    progress: String = "",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = when (operationType) {
                        OperationType.CREATING_BACKUP -> "Creating Backup"
                        OperationType.RESTORING_BACKUP -> "Restoring Backup"
                        OperationType.VALIDATING_PASSWORD -> "Validating Password"
                        OperationType.LOADING_BACKUPS -> "Loading Backups"
                        OperationType.DELETING_BACKUP -> "Deleting Backup"
                        OperationType.IDLE -> "Processing"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (progress.isNotEmpty()) {
                    Text(
                        text = progress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
