package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.BudgetPeriod
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories

/**
 * Dialog for setting monthly or weekly budget limits for spending categories.
 *
 * This dialog allows users to:
 * - Select a spending category (Food, Shopping, Transport, etc.)
 * - Choose a budget period (Monthly or Weekly)
 * - Set a budget amount with validation
 *
 * The budget will be used to trigger alert notifications when spending
 * reaches 80% or 100% of the configured limit.
 *
 * @param onConfirm Callback invoked when user confirms with (categoryId, period, amount)
 * @param onDismiss Callback invoked when user dismisses the dialog
 * @param initialCategoryId Optional initial category ID to pre-select
 * @param initialPeriod Optional initial budget period to pre-select
 * @param initialAmount Optional initial amount to pre-populate
 */
@Composable
fun BudgetSettingsDialog(
    onConfirm: (categoryId: String, period: BudgetPeriod, amount: Double) -> Unit,
    onDismiss: () -> Unit,
    initialCategoryId: String? = null,
    initialPeriod: BudgetPeriod = BudgetPeriod.MONTHLY,
    initialAmount: Double? = null
) {
    var selectedCategory by remember {
        mutableStateOf(
            DefaultCategories.ALL.find { it.id == initialCategoryId }
                ?: DefaultCategories.FOOD
        )
    }
    var selectedPeriod by remember { mutableStateOf(initialPeriod) }
    var amountText by remember {
        mutableStateOf(
            initialAmount?.toInt()?.toString() ?: ""
        )
    }
    var isError by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            currentCategory = selectedCategory,
            onCategorySelected = { category ->
                selectedCategory = category
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.AttachMoney,
                    contentDescription = "Set Budget",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Set Budget Limit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Set a spending limit for this category. You'll be notified when you reach 80% and 100% of your budget.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Category Selection
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        onClick = { showCategoryPicker = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = selectedCategory.icon,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = selectedCategory.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Period Selection
                    Text(
                        text = "Budget Period",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BudgetPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = period == selectedPeriod,
                                onClick = { selectedPeriod = period },
                                label = { Text(period.displayName) },
                                modifier = Modifier.weight(1f),
                                leadingIcon = if (period == selectedPeriod) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    // Amount Input
                    Text(
                        text = "Budget Amount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { newValue ->
                            amountText = newValue
                            isError = newValue.toDoubleOrNull() == null || newValue.toDoubleOrNull()!! <= 0
                        },
                        label = { Text("Amount (₹)") },
                        placeholder = { Text("10000") },
                        prefix = { Text("₹") },
                        isError = isError,
                        supportingText = if (isError) {
                            { Text("Please enter a valid amount greater than 0") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            onConfirm(selectedCategory.id, selectedPeriod, amount)
                        }
                    },
                    enabled = !isError && amountText.isNotBlank()
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Dialog for selecting a spending category from the available categories.
 *
 * Displays all categories with their icons and names in a scrollable list.
 * The currently selected category is highlighted.
 *
 * @param currentCategory The currently selected category
 * @param onCategorySelected Callback invoked when user selects a category
 * @param onDismiss Callback invoked when user dismisses the dialog
 */
@Composable
private fun CategoryPickerDialog(
    currentCategory: Category,
    onCategorySelected: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = "Select Category",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Choose the category for this budget:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Filter out "Other" and "Transfer" categories as they're not suitable for budgets
                DefaultCategories.ALL
                    .filter { it.id != "other" && it.id != "transfer" }
                    .forEach { category ->
                        Surface(
                            onClick = { onCategorySelected(category) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (category.id == currentCategory.id)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = category.icon,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (category.id == currentCategory.id)
                                        FontWeight.Bold
                                    else FontWeight.Normal,
                                    color = if (category.id == currentCategory.id)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (category.id == currentCategory.id) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
