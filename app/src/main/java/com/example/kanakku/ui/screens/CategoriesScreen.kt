package com.example.kanakku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.*
import com.example.kanakku.ui.components.CategoryPickerSheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    transactions: List<ParsedTransaction>,
    categoryMap: Map<Long, Category>,
    merchantMappingCount: Int,
    onCategoryChange: (Long, Category) -> Unit,
    onResetLearnedMappings: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedTransaction by remember { mutableStateOf<ParsedTransaction?>(null) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }

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

    if (selectedCategory != null) {
        CategoryDetailScreen(
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
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show reset learned preferences card if mappings exist
                if (merchantMappingCount > 0) {
                    item {
                        LearnedPreferencesCard(
                            mappingCount = merchantMappingCount,
                            onResetClick = { showResetConfirmation = true }
                        )
                    }
                }

                items(categoryTotals) { categoryTotal ->
                    CategoryCard(
                        categoryTotal = categoryTotal,
                        onClick = { selectedCategory = categoryTotal.category }
                    )
                }
            }
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

    if (showResetConfirmation) {
        ResetConfirmationDialog(
            mappingCount = merchantMappingCount,
            onConfirm = {
                onResetLearnedMappings()
                showResetConfirmation = false
            },
            onDismiss = {
                showResetConfirmation = false
            }
        )
    }
}

@Composable
private fun CategoryCard(
    categoryTotal: CategoryTotal,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = categoryTotal.category.color.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryTotal.category.icon,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryTotal.category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${categoryTotal.transactionCount} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%,.0f", categoryTotal.totalAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", categoryTotal.percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDetailScreen(
    category: Category,
    transactions: List<ParsedTransaction>,
    onBack: () -> Unit,
    onTransactionClick: (ParsedTransaction) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(category.name) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = category.color.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val total = transactions.sumOf { it.amount }
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.0f", total)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = category.color
                        )
                        Text(
                            text = "${transactions.size} transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(transactions.sortedByDescending { it.date }) { transaction ->
                CategoryTransactionCard(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CategoryTransactionCard(
    transaction: ParsedTransaction,
    onClick: () -> Unit
) {
    val isDebit = transaction.type == TransactionType.DEBIT
    val amountColor = if (isDebit) Color(0xFFC62828) else Color(0xFF2E7D32)
    val amountPrefix = if (isDebit) "-" else "+"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                text = "$amountPrefix₹${String.format(Locale.getDefault(), "%,.0f", transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
private fun LearnedPreferencesCard(
    mappingCount: Int,
    onResetClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Learned Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$mappingCount merchant ${if (mappingCount == 1) "mapping" else "mappings"} learned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onResetClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Reset Learned Preferences")
            }
        }
    }
}

@Composable
private fun ResetConfirmationDialog(
    mappingCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Reset Learned Preferences?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "This will remove $mappingCount learned merchant-to-category ${if (mappingCount == 1) "mapping" else "mappings"}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Future transactions will be categorized using only the automatic rules until you manually categorize them again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
