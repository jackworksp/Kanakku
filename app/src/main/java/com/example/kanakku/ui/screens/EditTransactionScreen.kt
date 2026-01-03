package com.example.kanakku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionType
import com.example.kanakku.ui.components.CategoryPickerSheet
import java.text.SimpleDateFormat
import java.util.*

/**
 * Form state for editing a manual transaction.
 */
data class EditTransactionFormState(
    val amount: String = "",
    val amountError: String? = null,
    val type: TransactionType = TransactionType.DEBIT,
    val category: Category? = null,
    val categoryError: String? = null,
    val merchant: String = "",
    val merchantError: String? = null,
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    transaction: ParsedTransaction?,
    initialCategory: Category?,
    onNavigateBack: () -> Unit,
    onSave: (amount: Double, type: TransactionType, category: Category, merchant: String, date: Long, notes: String) -> Unit,
    onDelete: () -> Unit
) {
    // Initialize form state with transaction data
    var formState by remember(transaction) {
        mutableStateOf(
            if (transaction != null) {
                EditTransactionFormState(
                    amount = transaction.amount.toString(),
                    type = transaction.type,
                    category = initialCategory,
                    merchant = transaction.merchant ?: "",
                    date = transaction.date,
                    notes = transaction.notes ?: ""
                )
            } else {
                EditTransactionFormState()
            }
        )
    }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Show loading or error if transaction is null
    if (transaction == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Transaction") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Transaction not found",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount Input
            OutlinedTextField(
                value = formState.amount,
                onValueChange = { value ->
                    // Allow only numbers and single decimal point
                    if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                        formState = formState.copy(
                            amount = value,
                            amountError = null
                        )
                    }
                },
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                leadingIcon = { Text("₹", style = MaterialTheme.typography.titleMedium) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = formState.amountError != null,
                supportingText = formState.amountError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Transaction Type Toggle
            Column {
                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Debit Button
                    TransactionTypeButton(
                        label = "Debit (Expense)",
                        isSelected = formState.type == TransactionType.DEBIT,
                        onClick = { formState = formState.copy(type = TransactionType.DEBIT) },
                        color = Color(0xFFC62828),
                        backgroundColor = Color(0xFFFFEBEE),
                        modifier = Modifier.weight(1f)
                    )

                    // Credit Button
                    TransactionTypeButton(
                        label = "Credit (Income)",
                        isSelected = formState.type == TransactionType.CREDIT,
                        onClick = { formState = formState.copy(type = TransactionType.CREDIT) },
                        color = Color(0xFF2E7D32),
                        backgroundColor = Color(0xFFE8F5E9),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Category Picker
            Column {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (formState.category != null) {
                            formState.category!!.color.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (formState.category != null) {
                            Text(
                                text = formState.category!!.icon,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = formState.category!!.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "Select a category",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (formState.categoryError != null) {
                    Text(
                        text = formState.categoryError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // Merchant Input
            OutlinedTextField(
                value = formState.merchant,
                onValueChange = { value ->
                    formState = formState.copy(
                        merchant = value,
                        merchantError = null
                    )
                },
                label = { Text("Merchant / Description") },
                placeholder = { Text("e.g., Local Store, Taxi, etc.") },
                isError = formState.merchantError != null,
                supportingText = formState.merchantError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date Picker
            Column {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDate(formState.date),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Notes Input
            OutlinedTextField(
                value = formState.notes,
                onValueChange = { value ->
                    formState = formState.copy(notes = value)
                },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Add any additional notes...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        // Validate form
                        val validationErrors = validateEditForm(formState)

                        if (validationErrors.isEmpty()) {
                            // All valid - save transaction
                            val amount = formState.amount.toDouble()
                            onSave(
                                amount,
                                formState.type,
                                formState.category!!,
                                formState.merchant.trim(),
                                formState.date,
                                formState.notes.trim()
                            )
                        } else {
                            // Update form state with errors
                            formState = formState.copy(
                                amountError = validationErrors["amount"],
                                categoryError = validationErrors["category"],
                                merchantError = validationErrors["merchant"]
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }

    // Category Picker Sheet
    if (showCategoryPicker) {
        CategoryPickerSheet(
            currentCategory = formState.category,
            onCategorySelected = { category ->
                formState = formState.copy(
                    category = category,
                    categoryError = null
                )
                showCategoryPicker = false
            },
            onDismiss = {
                showCategoryPicker = false
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.date
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            formState = formState.copy(date = selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TransactionTypeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) backgroundColor else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, color)
        } else {
            null
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Validates the edit form and returns a map of field names to error messages.
 * Empty map indicates all fields are valid.
 */
private fun validateEditForm(formState: EditTransactionFormState): Map<String, String> {
    val errors = mutableMapOf<String, String>()

    // Validate amount
    if (formState.amount.isBlank()) {
        errors["amount"] = "Amount is required"
    } else {
        val amount = formState.amount.toDoubleOrNull()
        if (amount == null) {
            errors["amount"] = "Invalid amount"
        } else if (amount <= 0) {
            errors["amount"] = "Amount must be greater than zero"
        }
    }

    // Validate category
    if (formState.category == null) {
        errors["category"] = "Please select a category"
    }

    // Validate merchant
    if (formState.merchant.isBlank()) {
        errors["merchant"] = "Merchant/Description is required"
    }

    return errors
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
