package com.example.kanakku.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.outlined.Folder
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
import com.example.kanakku.ui.components.BentoCard
import com.example.kanakku.ui.components.BentoChip
import com.example.kanakku.ui.components.BentoMiniStat
import com.example.kanakku.ui.components.BentoSectionHeader
import com.example.kanakku.ui.components.CategoryPickerSheet
import com.example.kanakku.ui.theme.CreditColor
import com.example.kanakku.ui.theme.CreditContainerLight
import com.example.kanakku.ui.theme.DebitColor
import com.example.kanakku.ui.theme.DebitContainerLight
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    transactions: List<ParsedTransaction>,
    categoryMap: Map<Long, Category>,
    onCategoryChange: (Long, Category) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedTransaction by remember { mutableStateOf<ParsedTransaction?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    val categoryTotals = remember(transactions, categoryMap) {
        val debitTransactions = transactions.filter { it.type == TransactionType.DEBIT }
        val totalSpent = debitTransactions.sumOf { it.amount }

        debitTransactions
            .groupBy { categoryMap[it.smsId] ?: DefaultCategories.OTHER }
            .map { (category, txns) ->
                val amount = txns.sumOf { it.amount }
                CategoryTotal(
                    category = category,
                    totalAmount = amount,
                    transactionCount = txns.size,
                    percentage = if (totalSpent > 0) (amount / totalSpent) * 100 else 0.0
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    val totalSpent = remember(transactions) {
        transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
    }

    AnimatedVisibility(
        visible = selectedCategory != null,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut()
    ) {
        if (selectedCategory != null) {
            ExpressiveCategoryDetailScreen(
                category = selectedCategory!!,
                transactions = transactions.filter {
                    categoryMap[it.smsId] == selectedCategory
                },
                onBack = { selectedCategory = null },
                onTransactionClick = { transaction ->
                    selectedTransaction = transaction
                    showCategoryPicker = true
                }
            )
        }
    }

    AnimatedVisibility(
        visible = selectedCategory == null,
        enter = slideInHorizontally { -it } + fadeIn(),
        exit = slideOutHorizontally { -it } + fadeOut()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                CategoriesBentoHeader(
                    totalSpent = totalSpent,
                    categoryCount = categoryTotals.size
                )
            }

            // Section Header
            item {
                BentoSectionHeader(
                    title = "Spending by Category",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Category Cards
            itemsIndexed(
                items = categoryTotals,
                key = { _, item -> item.category.name }
            ) { index, categoryTotal ->
                ExpressiveCategoryCard(
                    categoryTotal = categoryTotal,
                    rank = index + 1,
                    onClick = { selectedCategory = categoryTotal.category }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
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
private fun CategoriesBentoHeader(
    totalSpent: Double,
    categoryCount: Int
) {
    // Animation state for staggered entrance
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationTriggered = true }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Title with subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Organize your expenses",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Hero Summary Card with gradient
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
                    .height(130.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF7B5800),
                                Color(0xFF9C7700),
                                Color(0xFFAD8B00)
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
                // Decorative circles
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .offset(x = (-40).dp, y = (-40).dp)
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

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Spending",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "₹${formatAmount(totalSpent)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$categoryCount",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveCategoryCard(
    categoryTotal: CategoryTotal,
    rank: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with colored background
            Surface(
                shape = CircleShape,
                color = categoryTotal.category.color.copy(alpha = 0.15f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = categoryTotal.category.icon,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryTotal.category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${categoryTotal.transactionCount} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = categoryTotal.category.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", categoryTotal.percentage)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryTotal.category.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Progress bar
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (categoryTotal.percentage / 100).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = categoryTotal.category.color,
                    trackColor = categoryTotal.category.color.copy(alpha = 0.15f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${formatAmount(categoryTotal.totalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = categoryTotal.category.color
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressiveCategoryDetailScreen(
    category: Category,
    transactions: List<ParsedTransaction>,
    onBack: () -> Unit,
    onTransactionClick: (ParsedTransaction) -> Unit
) {
    val total = remember(transactions) { transactions.sumOf { it.amount } }

    Column(modifier = Modifier.fillMaxSize()) {
        // Custom TopAppBar with category color
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = category.color.copy(alpha = 0.1f)
        ) {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = category.icon,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(category.name)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                // Summary Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    category.color.copy(alpha = 0.15f),
                                    category.color.copy(alpha = 0.05f)
                                )
                            ),
                            shape = MaterialTheme.shapes.large
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Spent",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${formatAmount(total)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = category.color
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = category.color.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${transactions.size}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = category.color
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                BentoSectionHeader(title = "Transactions")
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(
                items = transactions.sortedByDescending { it.date },
                key = { it.smsId }
            ) { transaction ->
                ExpressiveCategoryTransactionCard(
                    transaction = transaction,
                    categoryColor = category.color,
                    onClick = { onTransactionClick(transaction) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ExpressiveCategoryTransactionCard(
    transaction: ParsedTransaction,
    categoryColor: Color,
    onClick: () -> Unit
) {
    val isDebit = transaction.type == TransactionType.DEBIT
    val amountColor = if (isDebit) DebitColor else CreditColor
    val amountPrefix = if (isDebit) "-" else "+"
    val typeContainerColor = if (isDebit) DebitContainerLight else CreditContainerLight

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type indicator
            Surface(
                shape = CircleShape,
                color = typeContainerColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDebit) Icons.AutoMirrored.Outlined.TrendingDown else Icons.AutoMirrored.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(20.dp)
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

            Text(
                text = "$amountPrefix₹${formatAmount(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

// Keep for backward compatibility
@Composable
private fun CategoryCard(
    categoryTotal: CategoryTotal,
    onClick: () -> Unit
) {
    ExpressiveCategoryCard(
        categoryTotal = categoryTotal,
        rank = 0,
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
