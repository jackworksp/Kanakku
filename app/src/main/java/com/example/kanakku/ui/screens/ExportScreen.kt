package com.example.kanakku.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.ExportFormat
import com.example.kanakku.data.model.ExportResult
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.ui.components.CategoryFilter
import com.example.kanakku.ui.components.DateRangePicker
import com.example.kanakku.ui.export.ExportUiState
import com.example.kanakku.ui.export.ExportViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Export screen for exporting transactions to CSV or PDF.
 *
 * Features:
 * - Date range filtering with preset options
 * - Category multi-select filtering
 * - Format selection (CSV or PDF)
 * - Preview summary of what will be exported
 * - Export and share functionality
 * - Loading states and error handling
 *
 * @param viewModel ViewModel managing export state and operations
 * @param transactions List of all available transactions for preview
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    transactions: List<ParsedTransaction>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val categoryMap by viewModel.categoryMap.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize ViewModel
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    // Handle error messages with retry option
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { errorMessage ->
            val snackbarResult = snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = "Retry",
                duration = SnackbarDuration.Long
            )
            when (snackbarResult) {
                SnackbarResult.ActionPerformed -> {
                    // User clicked retry
                    viewModel.exportData()
                }
                SnackbarResult.Dismissed -> {
                    viewModel.clearError()
                }
            }
        }
    }

    // Handle successful export with file size and path
    LaunchedEffect(uiState.exportResult) {
        if (uiState.exportResult is ExportResult.Success) {
            val result = uiState.exportResult as ExportResult.Success
            val fileSize = formatFileSize(result.fileSizeBytes)
            snackbarHostState.showSnackbar(
                message = "✓ Exported ${result.fileName} ($fileSize)",
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Screen Title
            Text(
                text = "Export Transactions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Format Selector Section
            Text(
                text = "Export Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                ExportFormat.entries.forEachIndexed { index, format ->
                    SegmentedButton(
                        selected = uiState.exportFormat == format,
                        onClick = { viewModel.setFormat(format) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ExportFormat.entries.size
                        )
                    ) {
                        Text(
                            text = format.displayName,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Date Range Picker Section
            Text(
                text = "Date Range (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            DateRangePicker(
                selectedDateRange = uiState.selectedDateRange,
                onDateRangeSelected = { dateRange ->
                    viewModel.setDateRange(dateRange)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category Filter Section
            Text(
                text = "Categories (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            CategoryFilter(
                availableCategories = uiState.availableCategories,
                selectedCategoryIds = uiState.selectedCategories,
                onCategoryToggled = { categoryId ->
                    viewModel.toggleCategory(categoryId)
                },
                onSelectAll = {
                    viewModel.selectAllCategories()
                },
                onClearAll = {
                    viewModel.clearCategorySelection()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Preview Summary Section
            Text(
                text = "Export Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            PreviewSummary(
                uiState = uiState,
                transactions = transactions,
                categoryMap = categoryMap
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            ExportActionButtons(
                viewModel = viewModel,
                uiState = uiState,
                context = context
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Preview summary card showing what will be exported.
 */
@Composable
private fun PreviewSummary(
    uiState: ExportUiState,
    transactions: List<ParsedTransaction>,
    categoryMap: Map<String, com.example.kanakku.data.model.Category>
) {
    // Filter transactions based on current filters
    val filteredTransactions = remember(transactions, uiState.selectedDateRange, uiState.selectedCategories) {
        var filtered = transactions

        // Apply date range filter
        uiState.selectedDateRange?.let { dateRange ->
            filtered = filtered.filter { tx ->
                tx.date >= dateRange.startDate && tx.date <= dateRange.endDate
            }
        }

        // Apply category filter
        if (uiState.selectedCategories.isNotEmpty()) {
            filtered = filtered.filter { tx ->
                uiState.selectedCategories.contains(tx.categoryId.toString())
            }
        }

        filtered
    }

    val totalAmount = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.amount }
    }

    val transactionCount = filteredTransactions.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Export Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction count
            SummaryRow(
                label = "Transactions",
                value = "$transactionCount"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Total amount
            SummaryRow(
                label = "Total Amount",
                value = "₹${String.format(Locale.getDefault(), "%.2f", totalAmount)}"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Date range
            SummaryRow(
                label = "Date Range",
                value = uiState.selectedDateRange?.let { dateRange ->
                    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    "${dateFormat.format(Date(dateRange.startDate))} - ${dateFormat.format(Date(dateRange.endDate))}"
                } ?: "All dates"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Categories
            SummaryRow(
                label = "Categories",
                value = if (uiState.selectedCategories.isEmpty()) {
                    "All categories"
                } else {
                    "${uiState.selectedCategories.size} selected"
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Format
            SummaryRow(
                label = "Format",
                value = uiState.exportFormat.displayName
            )

            if (transactionCount == 0) {
                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "⚠️ No transactions match the selected filters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Summary row for preview card.
 */
@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Export and share action buttons.
 */
@Composable
private fun ExportActionButtons(
    viewModel: ExportViewModel,
    uiState: ExportUiState,
    context: Context
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Export button
        Button(
            onClick = { viewModel.exportData() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !uiState.isExporting,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Exporting...",
                    style = MaterialTheme.typography.labelLarge
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export ${uiState.exportFormat.displayName}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Share button (only shown after successful export)
        if (uiState.exportResult is ExportResult.Success) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    val shareIntent = viewModel.shareFile(context)
                    shareIntent?.let { context.startActivity(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share Export",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Export result info
        if (uiState.exportResult is ExportResult.Success) {
            Spacer(modifier = Modifier.height(12.dp))

            val result = uiState.exportResult as ExportResult.Success
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "✓ Export successful",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Size: ${formatFileSize(result.fileSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Formats file size in bytes to a human-readable string.
 *
 * @param bytes File size in bytes
 * @return Formatted string (e.g., "1.5 MB", "256 KB")
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
