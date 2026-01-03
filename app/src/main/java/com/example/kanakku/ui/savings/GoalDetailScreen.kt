package com.example.kanakku.ui.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kanakku.data.model.GoalContribution
import com.example.kanakku.data.model.GoalStatus
import com.example.kanakku.data.model.SavingsGoal
import com.example.kanakku.domain.savings.ProgressMetrics
import com.example.kanakku.ui.savings.components.AddContributionDialog
import com.example.kanakku.ui.savings.components.CelebrationAnimation
import com.example.kanakku.ui.savings.components.CircularGoalProgressIndicator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Detail screen for an individual savings goal.
 *
 * This screen shows:
 * - Large progress indicator with goal icon
 * - Goal details (name, amounts, deadline)
 * - Progress metrics (days remaining, required savings, projected completion)
 * - Contribution history with dates and amounts
 * - Edit/delete actions
 * - Add contribution FAB
 * - Celebration animation when goal is completed
 *
 * @param goalId The ID of the goal to display
 * @param viewModel The ViewModel for managing savings goals state
 * @param onNavigateBack Callback to navigate back
 * @param modifier Modifier for this composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: String,
    viewModel: SavingsGoalsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load goal details
    LaunchedEffect(goalId) {
        viewModel.loadGoalDetails(goalId.toLongOrNull() ?: 0)
    }

    val goal = uiState.selectedGoal
    val contributions = uiState.selectedGoalContributions
    val progressMetrics = uiState.selectedGoalProgress

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Goal Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Edit button (only for active goals)
                    if (goal != null && !goal.isCompleted) {
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit goal"
                            )
                        }
                    }
                    // Delete button
                    if (goal != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete goal"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Add contribution FAB (only for active goals)
            if (goal != null && !goal.isCompleted) {
                FloatingActionButton(
                    onClick = { viewModel.showAddContributionDialog() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add contribution"
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (goal == null) {
                // Loading or error state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Goal not found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Button(onClick = onNavigateBack) {
                                Text("Go Back")
                            }
                        }
                    }
                }
            } else {
                // Main content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Large progress indicator with icon
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        GoalProgressHeader(goal = goal, progressMetrics = progressMetrics)
                    }

                    // Goal details
                    item {
                        GoalDetailsCard(goal = goal)
                    }

                    // Progress metrics
                    if (progressMetrics != null) {
                        item {
                            ProgressMetricsCard(
                                goal = goal,
                                metrics = progressMetrics
                            )
                        }
                    }

                    // Contribution history
                    item {
                        Text(
                            text = "Contribution History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (contributions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No contributions yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(contributions.sortedByDescending { it.date }) { contribution ->
                            ContributionCard(
                                contribution = contribution,
                                onDelete = {
                                    coroutineScope.launch {
                                        viewModel.deleteContribution(contribution.id)
                                    }
                                }
                            )
                        }
                    }

                    // Bottom spacer for FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            // Error Snackbar
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // Celebration Animation
            if (uiState.celebratingGoal != null) {
                CelebrationAnimation(
                    show = true,
                    message = "Goal Completed! 🎉\n${uiState.celebratingGoal?.name}",
                    onAnimationComplete = { viewModel.clearCelebration() }
                )
            }
        }
    }

    // Add Contribution Dialog
    if (uiState.showAddContributionDialog && goal != null) {
        AddContributionDialog(
            goalName = goal.name,
            onAddContribution = { amount, note ->
                coroutineScope.launch {
                    val contribution = GoalContribution(
                        goalId = goal.id,
                        amount = amount,
                        date = System.currentTimeMillis(),
                        note = note
                    )
                    viewModel.addContribution(contribution)
                }
            },
            onDismiss = { viewModel.hideAddContributionDialog() }
        )
    }

    // Edit Goal Sheet
    if (showEditSheet && goal != null) {
        EditGoalSheet(
            goal = goal,
            onUpdateGoal = { name, targetAmount, deadline, icon, color ->
                coroutineScope.launch {
                    val updatedGoal = goal.copy(
                        name = name,
                        targetAmount = targetAmount,
                        deadline = deadline,
                        icon = icon,
                        color = color
                    )
                    viewModel.updateGoal(updatedGoal)
                    showEditSheet = false
                }
            },
            onDismiss = { showEditSheet = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && goal != null) {
        DeleteGoalDialog(
            goalName = goal.name,
            onConfirm = {
                coroutineScope.launch {
                    val success = viewModel.deleteGoal(goal.id)
                    if (success) {
                        onNavigateBack()
                    }
                }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

/**
 * Large progress indicator header with goal icon
 */
@Composable
private fun GoalProgressHeader(
    goal: SavingsGoal,
    progressMetrics: ProgressMetrics?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with progress circle
        Box(
            contentAlignment = Alignment.Center
        ) {
            CircularGoalProgressIndicator(
                progress = (progressMetrics?.percentageComplete?.div(100.0) ?: 0.0).toFloat(),
                size = 200.dp,
                strokeWidth = 16.dp,
                color = parseColor(goal.color),
                showPercentage = false
            )

            // Goal icon in center
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(parseColor(goal.color).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = goal.icon ?: "🎯",
                    fontSize = 56.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Goal name
        Text(
            text = goal.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Percentage
        Text(
            text = "${(progressMetrics?.percentageComplete ?: 0.0).toInt()}%",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = parseColor(goal.color)
        )

        // Status badge
        GoalStatusBadge(status = goal.status)
    }
}

/**
 * Card showing goal details
 */
@Composable
private fun GoalDetailsCard(goal: SavingsGoal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Current amount
            DetailRow(
                label = "Current Amount",
                value = "₹${formatAmount(goal.currentAmount)}",
                valueColor = MaterialTheme.colorScheme.primary,
                valueFontWeight = FontWeight.Bold
            )

            Divider()

            // Target amount
            DetailRow(
                label = "Target Amount",
                value = "₹${formatAmount(goal.targetAmount)}"
            )

            Divider()

            // Remaining amount
            DetailRow(
                label = "Remaining Amount",
                value = "₹${formatAmount(goal.remainingAmount)}",
                valueColor = MaterialTheme.colorScheme.error
            )

            Divider()

            // Deadline
            DetailRow(
                label = "Deadline",
                value = formatDate(goal.deadline)
            )

            // Completed date (if completed)
            if (goal.isCompleted && goal.completedAt != null) {
                Divider()
                DetailRow(
                    label = "Completed On",
                    value = formatDate(goal.completedAt),
                    valueColor = MaterialTheme.colorScheme.tertiary,
                    valueFontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Card showing progress metrics
 */
@Composable
private fun ProgressMetricsCard(
    goal: SavingsGoal,
    metrics: ProgressMetrics
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Progress Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Days remaining
            if (!goal.isCompleted) {
                MetricRow(
                    icon = "⏱️",
                    label = "Days Remaining",
                    value = "${metrics.daysRemaining} days"
                )

                // Required savings (only if not overdue)
                if (goal.status != GoalStatus.OVERDUE && metrics.daysRemaining > 0) {
                    MetricRow(
                        icon = "💰",
                        label = "Required Daily Savings",
                        value = "₹${formatAmount(metrics.requiredDailySavings)}"
                    )

                    MetricRow(
                        icon = "💵",
                        label = "Required Monthly Savings",
                        value = "₹${formatAmount(metrics.requiredMonthlySavings)}"
                    )
                }

                // Projected completion
                metrics.projectedCompletionDate?.let { projectedDate ->
                    MetricRow(
                        icon = "📅",
                        label = "Projected Completion",
                        value = formatDate(projectedDate),
                        valueColor = if (projectedDate <= goal.deadline) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            // Contribution count
            MetricRow(
                icon = "📊",
                label = "Total Contributions",
                value = "${metrics.contributionCount} contributions"
            )
        }
    }
}

/**
 * Card for individual contribution
 */
@Composable
private fun ContributionCard(
    contribution: GoalContribution,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "₹${formatAmount(contribution.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatDate(contribution.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Note (if present)
                contribution.note?.let { note ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete contribution",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Status badge component
 */
@Composable
private fun GoalStatusBadge(status: GoalStatus) {
    val (text, color) = when (status) {
        GoalStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.tertiary
        GoalStatus.ACTIVE -> "Active" to MaterialTheme.colorScheme.primary
        GoalStatus.OVERDUE -> "Overdue" to MaterialTheme.colorScheme.error
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

/**
 * Detail row for key-value pairs
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueFontWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = valueFontWeight
        )
    }
}

/**
 * Metric row with icon
 */
@Composable
private fun MetricRow(
    icon: String,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}

/**
 * Edit goal bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditGoalSheet(
    goal: SavingsGoal,
    onUpdateGoal: (name: String, targetAmount: Double, deadline: Long, icon: String, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(goal.name) }
    var targetAmount by remember { mutableStateOf(goal.targetAmount.toString()) }
    var selectedIcon by remember { mutableStateOf(goal.icon ?: "🎯") }
    var selectedColor by remember { mutableStateOf(goal.color ?: "#6200EE") }
    var selectedDateMillis by remember { mutableStateOf(goal.deadline) }
    var showDatePicker by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Edit Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("Goal Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Please enter a goal name") }
                } else null,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target Amount Input
            OutlinedTextField(
                value = targetAmount,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                        targetAmount = it
                        amountError = false
                    }
                },
                label = { Text("Target Amount") },
                leadingIcon = { Text("₹", style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier.fillMaxWidth(),
                isError = amountError,
                supportingText = if (amountError) {
                    { Text("Please enter a valid amount") }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deadline Picker
            OutlinedTextField(
                value = formatDate(selectedDateMillis),
                onValueChange = {},
                label = { Text("Deadline") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select date"
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Icon and Color selection (simplified - just show current values)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Icon: $selectedIcon",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.bodyLarge
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(parseColor(selectedColor))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        // Validate
                        val amountValue = targetAmount.toDoubleOrNull()
                        when {
                            name.isBlank() -> nameError = true
                            amountValue == null || amountValue <= 0 -> amountError = true
                            else -> {
                                onUpdateGoal(
                                    name.trim(),
                                    amountValue,
                                    selectedDateMillis,
                                    selectedIcon,
                                    selectedColor
                                )
                            }
                        }
                    }
                ) {
                    Text("Update")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDateMillis = millis
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
}

/**
 * Delete confirmation dialog
 */
@Composable
private fun DeleteGoalDialog(
    goalName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Goal?") },
        text = {
            Text("Are you sure you want to delete \"$goalName\"? This will also delete all contributions and cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
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

/**
 * Format amount with compact notation for large numbers
 */
private fun formatAmount(amount: Double): String {
    return when {
        amount >= 100000 -> String.format(Locale.getDefault(), "%.1fL", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "%.1fK", amount / 1000)
        else -> String.format(Locale.getDefault(), "%.0f", amount)
    }
}

/**
 * Format date to readable string
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Parse color from hex string
 */
private fun parseColor(colorHex: String?): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex ?: "#6200EE"))
    } catch (e: Exception) {
        Color(0xFF6200EE)
    }
}
