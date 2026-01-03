package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A composable badge that indicates the app operates in offline-first mode
 * with all data stored locally on the device.
 *
 * This badge serves to:
 * - Reassure users that the app works without internet connectivity
 * - Highlight the privacy benefit of local-only data storage
 * - Provide a clear visual indicator of the app's offline-first architecture
 *
 * @param modifier Optional modifier for customizing the badge appearance and position
 * @param showIcon Whether to display the cloud-off icon (default: true)
 * @param compact Whether to use compact mode (icon only, no text) for space-constrained layouts
 */
@Composable
fun OfflineBadge(
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    compact: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 12.dp,
                vertical = 6.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline Mode",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!compact) {
                Text(
                    text = "Local Data",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * An alternative offline badge variant with custom color scheme
 * for use in different contexts (e.g., dark backgrounds).
 *
 * @param modifier Optional modifier for customizing the badge appearance and position
 * @param backgroundColor The background color of the badge
 * @param contentColor The color for text and icons
 */
@Composable
fun OfflineBadgeColored(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF4CAF50).copy(alpha = 0.15f),
    contentColor: Color = Color(0xFF2E7D32)
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Offline Mode",
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )

            Text(
                text = "Offline Mode",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

/**
 * A privacy-focused badge variant that emphasizes data security
 * and on-device storage.
 *
 * @param modifier Optional modifier for customizing the badge appearance and position
 */
@Composable
fun PrivacyBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔒",
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = "Data stays on device",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
