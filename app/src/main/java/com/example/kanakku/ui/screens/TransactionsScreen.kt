package com.example.kanakku.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.*
import com.example.kanakku.ui.MainUiState
import com.example.kanakku.ui.components.BentoCard
import com.example.kanakku.ui.components.BentoChip
import com.example.kanakku.ui.components.BentoEmptyState
import com.example.kanakku.ui.components.BentoFeatureCard
import com.example.kanakku.ui.components.BentoGlassCard
import com.example.kanakku.ui.components.BentoMiniStat
import com.example.kanakku.ui.components.BentoSectionHeader
import com.example.kanakku.ui.components.BentoTransactionSummaryCard
import com.example.kanakku.ui.components.CategoryPickerSheet
import com.example.kanakku.ui.theme.BentoGradientEnd
import com.example.kanakku.ui.theme.BentoGradientStart
import com.example.kanakku.ui.theme.CreditColor
import com.example.kanakku.ui.theme.CreditContainerLight
import com.example.kanakku.ui.theme.DebitColor
import com.example.kanakku.ui.theme.DebitColorLight
import com.example.kanakku.ui.theme.DebitContainerLight
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: MainUiState,
    categoryMap: Map<Long, Category>,
    onRefresh: () -> Unit,
    onCategoryChange: (Long, Category) -> Unit
) {
    var selectedTransaction by remember { mutableStateOf<ParsedTransaction?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header with Bento Grid
        item {
            TransactionsBentoHeader(
                uiState = uiState,
                onRefresh = onRefresh
            )
        }

        // Transactions Section
        item {
            BentoSectionHeader(
                title = "Recent Transactions",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (uiState.transactions.isEmpty()) {
            item {
                BentoEmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No Transactions",
                    message = "No bank transactions found in the last 30 days"
                )
            }
        } else {
            itemsIndexed(
                items = uiState.transactions,
                key = { _, transaction -> transaction.smsId }
            ) { index, transaction ->
                ExpressiveTransactionCard(
                    transaction = transaction,
                    category = categoryMap[transaction.smsId],
                    onClick = {
                        selectedTransaction = transaction
                        showCategoryPicker = true
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showCategoryPicker && selectedTransaction != null) {
        CategoryPickerSheet(
            currentCategory = categoryMap[selectedTransaction!!.smsId],
            onCategorySelected = { category ->
                onCategoryChange(selectedTransaction!!.smsId, category)
                showCategoryPicker = false
                selectedTransaction = null
            },
            onDismiss = {
                showCategoryPicker = false
                selectedTransaction = null
            }
        )
    }
}

@Composable
private fun TransactionsBentoHeader(
    uiState: MainUiState,
    onRefresh: () -> Unit
) {
    val totalDebit = uiState.transactions
        .filter { it.type == TransactionType.DEBIT }
        .sumOf { it.amount }

    val totalCredit = uiState.transactions
        .filter { it.type == TransactionType.CREDIT }
        .sumOf { it.amount }

    val netFlow = totalCredit - totalDebit
    val transactionCount = uiState.transactions.size

    // Animation state for staggered entrance
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationTriggered = true }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Title Row with Refresh - Expressive style
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                uiState.lastSyncTimestamp?.let { timestamp ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CreditColor,
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Text(
                            text = "Updated ${formatRelativeTime(timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            FilledTonalIconButton(
                onClick = onRefresh,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Hero Card - Net Flow with enhanced gradient
        AnimatedVisibility(
            visible = animationTriggered,
            enter = fadeIn(tween(500)) + slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                BentoGradientStart,
                                BentoGradientEnd,
                                Color(0xFF00897B)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.extraLarge
                    )
            ) {
                // Decorative elements
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .offset(x = (-50).dp, y = (-50).dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 30.dp, y = 30.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Net Flow",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "This Month",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (netFlow >= 0) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "${if (netFlow >= 0) "+" else "-"}₹${formatAmount(kotlin.math.abs(netFlow))}",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$transactionCount transactions",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        if (uiState.duplicatesRemoved > 0) {
                            Text(
                                text = "${uiState.duplicatesRemoved} duplicates removed",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Summary Cards Row - Enhanced Bento Style
        AnimatedVisibility(
            visible = animationTriggered,
            enter = fadeIn(tween(500, delayMillis = 150)) + slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Spent Card
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp),
                    backgroundColor = DebitColorLight.copy(alpha = 0.5f)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Spent",
                                style = MaterialTheme.typography.labelLarge,
                                color = DebitColor.copy(alpha = 0.8f)
                            )
                            Surface(
                                shape = CircleShape,
                                color = DebitColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.TrendingDown,
                                        contentDescription = null,
                                        tint = DebitColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "₹${formatAmount(totalDebit)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = DebitColor
                        )
                    }
                }

                // Received Card
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp),
                    backgroundColor = CreditContainerLight.copy(alpha = 0.5f)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Received",
                                style = MaterialTheme.typography.labelLarge,
                                color = CreditColor.copy(alpha = 0.8f)
                            )
                            Surface(
                                shape = CircleShape,
                                color = CreditColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                                        contentDescription = null,
                                        tint = CreditColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "₹${formatAmount(totalCredit)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = CreditColor
                        )
                    }
                }
            }
        }

        // Quick Stats Pills
        AnimatedVisibility(
            visible = animationTriggered,
            enter = fadeIn(tween(500, delayMillis = 300))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val avgTransaction = if (transactionCount > 0) totalDebit / transactionCount else 0.0
                BentoMiniStat(
                    label = "Avg",
                    value = "₹${formatAmount(avgTransaction)}",
                    color = MaterialTheme.colorScheme.primary
                )

                val debitCount = uiState.transactions.count { it.type == TransactionType.DEBIT }
                BentoMiniStat(
                    label = "Debits",
                    value = "$debitCount",
                    color = DebitColor
                )

                val creditCount = uiState.transactions.count { it.type == TransactionType.CREDIT }
                BentoMiniStat(
                    label = "Credits",
                    value = "$creditCount",
                    color = CreditColor
                )
            }
        }
    }
}

@Composable
fun ExpressiveTransactionCard(
    transaction: ParsedTransaction,
    category: Category?,
    onClick: () -> Unit
) {
    val isDebit = transaction.type == TransactionType.DEBIT
    val amountColor = if (isDebit) DebitColor else CreditColor
    val amountPrefix = if (isDebit) "-" else "+"
    val typeContainerColor = if (isDebit) DebitContainerLight else CreditContainerLight

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Transaction type indicator
                    Surface(
                        shape = CircleShape,
                        color = typeContainerColor,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDebit) Icons.AutoMirrored.Outlined.TrendingDown else Icons.AutoMirrored.Outlined.TrendingUp,
                                contentDescription = null,
                                tint = amountColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.merchant ?: transaction.senderAddress,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = formatDate(transaction.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$amountPrefix₹${formatAmount(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeContainerColor
                    ) {
                        Text(
                            text = if (isDebit) "DEBIT" else "CREDIT",
                            style = MaterialTheme.typography.labelSmall,
                            color = amountColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Category and Reference Row
            if (category != null || transaction.referenceNumber != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    category?.let { cat ->
                        BentoChip(
                            text = cat.name,
                            color = cat.color
                        )
                    }

                    transaction.referenceNumber?.let { ref ->
                        Text(
                            text = "Ref: $ref",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// Keep old TransactionCard for compatibility but mark as internal
@Composable
internal fun TransactionCard(
    transaction: ParsedTransaction,
    category: Category?,
    onClick: () -> Unit
) {
    ExpressiveTransactionCard(
        transaction = transaction,
        category = category,
        onClick = onClick
    )
}

private fun formatAmount(amount: Double): String {
    return when {
        amount >= 10000000 -> String.format(Locale.getDefault(), "%.2fCr", amount / 10000000)
        amount >= 100000 -> String.format(Locale.getDefault(), "%.2fL", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1000)
        else -> String.format(Locale.getDefault(), "%.0f", amount)
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> {
            val minutes = (diff / 60_000).toInt()
            if (minutes == 1) "1 min ago" else "$minutes mins ago"
        }
        diff < 86400_000 -> {
            val hours = (diff / 3600_000).toInt()
            if (hours == 1) "1 hour ago" else "$hours hours ago"
        }
        else -> {
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
