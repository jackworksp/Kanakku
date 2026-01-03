package com.example.kanakku.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.Budget
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DefaultCategories
import java.util.*

/**
 * Dialog for creating or editing a budget amount with validation.
 *
 * This dialog provides:
 * - Amount input with number keyboard and validation
 * - Category selector for category-specific budgets
 * - Clear indication of whether creating or editing
 * - Save and Cancel actions
 * - Input validation for positive amounts
 *
 * @param existingBudget The budget being edited, or null to create a new budget
 * @param isOverallBudget Whether this is for the overall monthly budget (true) or category budget (false)
 * @param currentMonth Current month for the budget
 * @param currentYear Current year for the budget
 * @param onSave Callback when user saves the budget with validated amount and category
 * @param onDismiss Callback when user cancels or dismisses the dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditDialog(
    existingBudget: Budget?,
    isOverallBudget: Boolean,
    currentMonth: Int,
    currentYear: Int,
    onSave: (amount: Double, categoryId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    // State management
    var amountText by remember {
        mutableStateOf(
            if (existingBudget != null) {
                existingBudget.amount.toInt().toString()
            } else {
                ""
            }
        )
    }

    var selectedCategory by remember {
        mutableStateOf(
            if (existingBudget?.categoryId != null) {
                DefaultCategories.ALL.find { it.id == existingBudget.categoryId }
            } else {
                null
            }
        )
    }

    var showCategoryDropdown by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }

    // Determine dialog title
    val dialogTitle = when {
        existingBudget != null && isOverallBudget -> "Edit Overall Budget"
        existingBudget != null -> "Edit Category Budget"
        isOverallBudget -> "Set Overall Budget"
        else -> "Set Category Budget"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AttachMoney,
                contentDescription = "Budget",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Description text
                Text(
                    text = if (isOverallBudget) {
                        "Set your total spending limit for ${getMonthName(currentMonth)} $currentYear"
                    } else {
                        "Set spending limit for a specific category"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Category selector (only for category budgets)
                if (!isOverallBudget) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        ExposedDropdownMenuBox(
                            expanded = showCategoryDropdown,
                            onExpandedChange = { showCategoryDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory?.let { "${it.icon} ${it.name}" } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                placeholder = { Text("Select a category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                isError = categoryError != null
                            )

                            ExposedDropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                // Filter out Transfer, ATM, and Other categories as they're not suitable for budgets
                                DefaultCategories.ALL
                                    .filter { it.id !in listOf("transfer", "atm", "other") }
                                    .forEach { category ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = category.icon,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(
                                                        text = category.name,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedCategory = category
                                                categoryError = null
                                                showCategoryDropdown = false
                                            }
                                        )
                                    }
                            }
                        }

                        if (categoryError != null) {
                            Text(
                                text = categoryError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Amount input
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Budget Amount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { newValue ->
                            // Only allow digits
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                amountText = newValue
                                amountError = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter amount") },
                        prefix = { Text("₹ ") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        isError = amountError != null,
                        supportingText = if (amountError != null) {
                            { Text(amountError!!) }
                        } else null
                    )
                }

                // Helpful hint
                if (selectedCategory != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "This will limit spending in the ${selectedCategory?.name} category",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate amount
                    val amount = amountText.toDoubleOrNull()
                    var hasError = false

                    if (amount == null || amount <= 0) {
                        amountError = "Please enter a valid amount greater than zero"
                        hasError = true
                    }

                    // Validate category selection for category budgets
                    if (!isOverallBudget && selectedCategory == null) {
                        categoryError = "Please select a category"
                        hasError = true
                    }

                    if (!hasError && amount != null) {
                        val categoryId = if (isOverallBudget) null else selectedCategory?.id
                        onSave(amount, categoryId)
                    }
                }
            ) {
                Text(if (existingBudget != null) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Helper function to get month name from month number.
 *
 * @param month Month number (1-12)
 * @return Month name (e.g., "January")
 */
private fun getMonthName(month: Int): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.MONTH, month - 1)
    return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
}
