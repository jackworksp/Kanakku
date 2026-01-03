package com.example.kanakku.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.RecurringTransaction
import com.example.kanakku.data.model.RecurringType
import com.example.kanakku.ui.recurring.RecurringSummaryHeader
import com.example.kanakku.ui.recurring.RecurringTransactionCard
import com.example.kanakku.ui.recurring.RecurringUiState
import com.example.kanakku.ui.recurring.RecurringViewModel

/**
 * Main screen for displaying recurring transactions.
 * Shows summary header with monthly total, subscription count, and upcoming count.
 * Displays recurring transactions grouped by type in collapsible sections.
 *
 * Features:
 * - Summary header with key metrics
 * - Sectioned display: Subscriptions, EMIs, Salaries, Rent, Utilities, Other
 * - User actions: Confirm or remove recurring patterns
 * - Loading, empty, and error state handling
 *
 * @param viewModel ViewModel managing recurring transactions state
 * @param modifier Optional modifier for custom styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    viewModel: RecurringViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Load data on first composition
    LaunchedEffect(Unit) {
        viewModel.loadRecurringTransactions(context)
    }

    // Show snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.recurringTransactions.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    RecurringContent(
                        uiState = uiState,
                        onConfirm = { id ->
                            viewModel.confirmRecurringTransaction(context, id)
                        },
                        onRemove = { id ->
                            viewModel.removeRecurringTransaction(context, id)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Loading state showing a centered progress indicator.
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Empty state shown when no recurring transactions are detected.
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No recurring transactions detected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "We'll automatically detect recurring patterns like subscriptions, EMIs, and salaries from your transactions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Main content displaying recurring transactions with summary header and sections.
 *
 * @param uiState Current UI state containing recurring transactions and statistics
 * @param onConfirm Callback when user confirms a recurring pattern
 * @param onRemove Callback when user removes a recurring pattern
 */
@Composable
private fun RecurringContent(
    uiState: RecurringUiState,
    onConfirm: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Page title
        Text(
            text = "Recurring Transactions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary header
        RecurringSummaryHeader(
            totalMonthlyRecurring = uiState.totalMonthlyRecurring,
            subscriptionCount = uiState.subscriptionCount,
            upcomingCount = uiState.upcomingCount
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Subscriptions
        if (uiState.subscriptionCount > 0) {
            RecurringSection(
                title = "Subscriptions",
                count = uiState.subscriptionCount,
                transactions = uiState.recurringTransactions.filter {
                    it.type == RecurringType.SUBSCRIPTION
                },
                onConfirm = onConfirm,
                onRemove = onRemove
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: EMIs
        if (uiState.emiCount > 0) {
            RecurringSection(
                title = "EMIs",
                count = uiState.emiCount,
                transactions = uiState.recurringTransactions.filter {
                    it.type == RecurringType.EMI
                },
                onConfirm = onConfirm,
                onRemove = onRemove
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Salaries
        if (uiState.salaryCount > 0) {
            RecurringSection(
                title = "Salaries",
                count = uiState.salaryCount,
                transactions = uiState.recurringTransactions.filter {
                    it.type == RecurringType.SALARY
                },
                onConfirm = onConfirm,
                onRemove = onRemove
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Rent
        if (uiState.rentCount > 0) {
            RecurringSection(
                title = "Rent",
                count = uiState.rentCount,
                transactions = uiState.recurringTransactions.filter {
                    it.type == RecurringType.RENT
                },
                onConfirm = onConfirm,
                onRemove = onRemove
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Utilities
        if (uiState.utilityCount > 0) {
            RecurringSection(
                title = "Utilities",
                count = uiState.utilityCount,
                transactions = uiState.recurringTransactions.filter {
                    it.type == RecurringType.UTILITY
                },
                onConfirm = onConfirm,
                onRemove = onRemove
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Other
        if (uiState.otherCount > 0) {
            RecurringSection(
                title = "Other",
                count = uiState.otherCount,
                transactions = uiState.recurringTransactions.filter {
                    it.type == RecurringType.OTHER
                },
                onConfirm = onConfirm,
                onRemove = onRemove
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Section component displaying a group of recurring transactions by type.
 *
 * @param title Section title (e.g., "Subscriptions", "EMIs")
 * @param count Number of items in this section
 * @param transactions List of recurring transactions for this section
 * @param onConfirm Callback when user confirms a recurring pattern
 * @param onRemove Callback when user removes a recurring pattern
 */
@Composable
private fun RecurringSection(
    title: String,
    count: Int,
    transactions: List<RecurringTransaction>,
    onConfirm: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Column {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction cards
        transactions.forEach { transaction ->
            RecurringTransactionCard(
                recurringTransaction = transaction,
                onConfirm = { onConfirm(transaction.id) },
                onRemove = { onRemove(transaction.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
