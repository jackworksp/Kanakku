package com.example.kanakku.ui.savings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.domain.savings.SavingsLevel
import com.example.kanakku.domain.savings.SavingsSuggestion
import java.util.*

/**
 * Card component displaying suggested monthly/weekly savings amount based on spending analysis
 *
 * @param suggestion The savings suggestion data to display
 * @param modifier Modifier for this composable
 */
@Composable
fun SavingsSuggestionCard(
    suggestion: SavingsSuggestion?,
    modifier: Modifier = Modifier
) {
    if (suggestion == null) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Savings suggestion",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Savings Suggestion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Analysis Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = suggestion.analysis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Savings Levels
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SavingsLevelOption(
                    level = "Conservative",
                    amount = suggestion.conservative,
                    period = suggestion.period.displayName,
                    isRecommended = suggestion.recommendedLevel == SavingsLevel.CONSERVATIVE,
                    description = "Safe starting point"
                )

                SavingsLevelOption(
                    level = "Moderate",
                    amount = suggestion.moderate,
                    period = suggestion.period.displayName,
                    isRecommended = suggestion.recommendedLevel == SavingsLevel.MODERATE,
                    description = "Balanced approach"
                )

                SavingsLevelOption(
                    level = "Aggressive",
                    amount = suggestion.aggressive,
                    period = suggestion.period.displayName,
                    isRecommended = suggestion.recommendedLevel == SavingsLevel.AGGRESSIVE,
                    description = "Maximum savings"
                )
            }
        }
    }
}

/**
 * Option showing a single savings level with amount
 *
 * @param level The savings level name
 * @param amount The suggested amount for this level
 * @param period The time period (e.g., "Monthly", "Weekly")
 * @param isRecommended Whether this is the recommended level
 * @param description Brief description of this level
 */
@Composable
private fun SavingsLevelOption(
    level: String,
    amount: Double,
    period: String,
    isRecommended: Boolean,
    description: String
) {
    val backgroundColor = if (isRecommended) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isRecommended) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isRecommended) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = level,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isRecommended) FontWeight.Bold else FontWeight.Medium,
                        color = if (isRecommended) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Recommended",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${formatAmount(amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isRecommended) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "per ${period.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Format amount with compact notation for large numbers
 */
private fun formatAmount(amount: Double): String {
    return when {
        amount >= 100000 -> String.format(Locale.getDefault(), "%.1fL", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1000)
        amount <= 0 -> "0"
        else -> String.format(Locale.getDefault(), "%.0f", amount)
    }
}
