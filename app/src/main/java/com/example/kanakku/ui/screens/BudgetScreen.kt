package com.example.kanakku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.*
import com.example.kanakku.ui.budget.BudgetUiState
import com.example.kanakku.ui.components.BudgetEditDialog
import com.example.kanakku.ui.components.BudgetProgressBar
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main budget management screen showing overall and category budgets.
 *
 * @param uiState Current UI state from BudgetViewModel
 * @param onEditBudget Callback when user wants to edit a budget (null for new overall budget)
 * @param onDeleteBudget Callback when user wants to delete a budget
 * @param onAddCategoryBudget Callback when user wants to add a new category budget
 * @param onMonthChange Callback when user changes the displayed month
 * @param onSaveBudget Callback when user saves a budget with amount and categoryId
 * @param onCancelEdit Callback when user cancels editing
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    uiState: BudgetUiState,
    onEditBudget: (Budget?) -> Unit,
    onDeleteBudget: (Budget) -> Unit,
    onAddCategoryBudget: () -> Unit,
    onMonthChange: (Int, Int) -> Unit,
    onSaveBudget: (amount: Double, categoryId: String?) -> Unit,
    onCancelEdit: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf<Budget?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCategoryBudget,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Category Budget"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.errorMessage != null -> {
                    ErrorState(errorMessage = uiState.errorMessage)
                }
                else -> {
                    BudgetContent(
                        uiState = uiState,
                        onEditBudget = onEditBudget,
                        onDeleteBudget = { showDeleteConfirmation = it },
                        onMonthChange = onMonthChange
                    )
                }
            }
        }

        // Delete confirmation dialog
        showDeleteConfirmation?.let { budget ->
            DeleteConfirmationDialog(
                budget = budget,
                onConfirm = {
                    onDeleteBudget(budget)
                    showDeleteConfirmation = null
                },
                onDismiss = {
                    showDeleteConfirmation = null
                }
            )
        }

        // Budget edit dialog
        if (uiState.isEditMode) {
            BudgetEditDialog(
                existingBudget = uiState.editingBudget,
                isOverallBudget = uiState.isEditingOverallBudget,
                currentMonth = uiState.currentMonth,
                currentYear = uiState.currentYear,
                onSave = { amount, categoryId ->
                    onSaveBudget(amount, categoryId)
                },
                onDismiss = onCancelEdit
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading budgets...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(errorMessage: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BudgetContent(
    uiState: BudgetUiState,
    onEditBudget: (Budget?) -> Unit,
    onDeleteBudget: (Budget) -> Unit,
    onMonthChange: (Int, Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(0.dp))
        }

        // Header
        item {
            BudgetHeader(
                currentMonth = uiState.currentMonth,
                currentYear = uiState.currentYear,
                onMonthChange = onMonthChange
            )
        }

        // Overall Budget Card
        item {
            OverallBudgetCard(
                budgetProgress = uiState.overallBudgetProgress,
                budget = uiState.budgets.find { it.categoryId == null },
                onEdit = { onEditBudget(null) },
                onDelete = { budget -> onDeleteBudget(budget) }
            )
        }

        // Category Budgets Section Header
        if (uiState.categoryBudgetProgresses.isNotEmpty()) {
            item {
                Text(
                    text = "Category Budgets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Category Budget Cards
        if (uiState.categoryBudgetProgresses.isEmpty() && uiState.budgets.find { it.categoryId == null } != null) {
            item {
                EmptyCategoryBudgetsCard(onAddCategoryBudget = onEditBudget)
            }
        } else {
            items(uiState.categoryBudgetProgresses) { categoryBudgetProgress ->
                CategoryBudgetCard(
                    categoryBudgetProgress = categoryBudgetProgress,
                    onEdit = { onEditBudget(categoryBudgetProgress.budget) },
                    onDelete = { onDeleteBudget(categoryBudgetProgress.budget) }
                )
            }
        }

        // Empty state when no budgets at all
        if (uiState.budgets.isEmpty()) {
            item {
                EmptyBudgetsCard(onSetBudget = { onEditBudget(null) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun BudgetHeader(
    currentMonth: Int,
    currentYear: Int,
    onMonthChange: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Budget",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Month/Year Display
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
            Calendar.getInstance().apply {
                set(Calendar.MONTH, currentMonth - 1)
                set(Calendar.YEAR, currentYear)
            }.time
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun OverallBudgetCard(
    budgetProgress: BudgetProgress?,
    budget: Budget?,
    onEdit: () -> Unit,
    onDelete: (Budget) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💰",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Overall Budget",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (budget != null) {
                        IconButton(onClick = { onDelete(budget) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Budget",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (budgetProgress != null) {
                // Show budget progress
                BudgetProgressContent(budgetProgress = budgetProgress)
            } else {
                // No budget set - show call to action
                Text(
                    text = "No overall budget set for this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onEdit) {
                    Text("Set Budget")
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetCard(
    categoryBudgetProgress: CategoryBudgetProgress,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = categoryBudgetProgress.category.icon,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = categoryBudgetProgress.category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Budget",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            BudgetProgressContent(budgetProgress = categoryBudgetProgress.progress)
        }
    }
}

@Composable
private fun BudgetProgressContent(budgetProgress: BudgetProgress) {
    Column {
        // Amounts row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Spent",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${formatAmount(budgetProgress.spent)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = getBudgetStatusColor(budgetProgress.status)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Budget",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${formatAmount(budgetProgress.limit)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar using reusable component
        BudgetProgressBar(
            budgetProgress = budgetProgress,
            showPercentageText = true,
            showRemainingAmount = true
        )
    }
}

@Composable
private fun EmptyBudgetsCard(onSetBudget: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📊",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "No Budgets Set",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Set a monthly budget to track your spending and make better financial decisions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onSetBudget,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Set Overall Budget")
            }
        }
    }
}

@Composable
private fun EmptyCategoryBudgetsCard(onAddCategoryBudget: (Budget?) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAddCategoryBudget(null) }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add Category Budgets",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Set budgets for specific categories like Food, Shopping, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    budget: Budget,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val budgetName = if (budget.categoryId == null) {
        "overall budget"
    } else {
        DefaultCategories.ALL.find { it.id == budget.categoryId }?.name ?: "this budget"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Delete Budget?")
        },
        text = {
            Text(text = "Are you sure you want to delete the $budgetName for ${getMonthName(budget.month)} ${budget.year}?")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun getMonthName(month: Int): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.MONTH, month - 1)
    return SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
}

private fun getBudgetStatusColor(status: BudgetStatus): Color {
    return when (status) {
        BudgetStatus.UNDER_BUDGET -> Color(0xFF2E7D32)  // Green
        BudgetStatus.APPROACHING -> Color(0xFFF57F17)   // Amber/Yellow
        BudgetStatus.EXCEEDED -> Color(0xFFC62828)      // Red
    }
}

private fun formatAmount(amount: Double): String {
    return when {
        amount >= 100000 -> String.format(Locale.getDefault(), "%.1fL", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1000)
        else -> String.format(Locale.getDefault(), "%.0f", amount)
    }
}
