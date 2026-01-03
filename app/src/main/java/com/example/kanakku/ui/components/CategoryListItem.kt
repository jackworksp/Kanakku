package com.example.kanakku.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.Category

/**
 * Reusable composable for displaying a category in a list.
 *
 * Displays category information including icon, name, color indicator, subcategory count,
 * and action buttons for editing and deleting. Follows Material Design 3 guidelines.
 *
 * @param category The category to display
 * @param subcategoryCount Number of subcategories this category has
 * @param onEditClick Callback invoked when the edit button is clicked
 * @param onDeleteClick Callback invoked when the delete button is clicked
 * @param onClick Optional callback invoked when the list item itself is clicked
 * @param modifier Optional modifier for the list item
 */
@Composable
fun CategoryListItem(
    category: Category,
    subcategoryCount: Int,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon with color indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = category.color.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.icon,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Category name and subcategory count
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                if (subcategoryCount > 0) {
                    Text(
                        text = "$subcategoryCount subcategor${if (subcategoryCount == 1) "y" else "ies"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Edit button
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit category",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete category",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
