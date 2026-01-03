package com.example.kanakku.ui.savings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kanakku.data.model.GoalContribution
import com.example.kanakku.data.model.SavingsGoal
import com.example.kanakku.ui.savings.components.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * Main screen for displaying savings goals.
 *
 * This screen shows:
 * - A list of active savings goals with progress indicators
 * - A completed goals section
 * - Savings suggestions based on spending patterns
 * - FAB to create new goals
 * - Celebration animations when goals are completed
 *
 * @param viewModel The ViewModel for managing savings goals state
 * @param modifier Modifier for this composable
 * @param onNavigateToGoalDetail Callback when a goal is clicked to navigate to detail screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: SavingsGoalsViewModel,
    modifier: Modifier = Modifier,
    onNavigateToGoalDetail: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Track which goal to add contribution to
    var contributionGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateGoalSheet() }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new goal"
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                SavingsGoalsHeader(
                    activeGoalsCount = uiState.activeGoals.size,
                    completedGoalsCount = uiState.completedGoals.size,
                    totalSaved = uiState.aggregateStats?.totalAmountSaved ?: 0.0
                )

                // Loading State
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Main Content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Savings Suggestion Card
                        item {
                            if (uiState.savingsSuggestion != null) {
                                SavingsSuggestionCard(suggestion = uiState.savingsSuggestion)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        // Active Goals Section
                        if (uiState.activeGoals.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Active Goals",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(uiState.activeGoals) { goal ->
                                GoalCard(
                                    goal = goal,
                                    onClick = { onNavigateToGoalDetail(goal.id) },
                                    onAddContribution = {
                                        contributionGoal = goal
                                        viewModel.showAddContributionDialog()
                                    }
                                )
                            }
                        }

                        // Completed Goals Section
                        if (uiState.completedGoals.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Completed Goals",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(uiState.completedGoals) { goal ->
                                GoalCard(
                                    goal = goal,
                                    onClick = { onNavigateToGoalDetail(goal.id) }
                                )
                            }
                        }

                        // Empty State
                        if (uiState.activeGoals.isEmpty() && uiState.completedGoals.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "🎯",
                                            style = MaterialTheme.typography.displayLarge
                                        )
                                        Text(
                                            text = "No savings goals yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Create a goal to start saving",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom spacer for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
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

    // Add Goal Sheet
    if (uiState.showCreateGoalSheet) {
        AddGoalSheet(
            onCreateGoal = { name, targetAmount, deadline, icon, color ->
                coroutineScope.launch {
                    val goal = SavingsGoal(
                        name = name,
                        targetAmount = targetAmount,
                        currentAmount = 0.0,
                        deadline = deadline,
                        createdAt = System.currentTimeMillis(),
                        isCompleted = false,
                        completedAt = null,
                        icon = icon,
                        color = color
                    )
                    viewModel.createGoal(goal)
                }
            },
            onDismiss = { viewModel.hideCreateGoalSheet() }
        )
    }

    // Add Contribution Dialog
    if (uiState.showAddContributionDialog && contributionGoal != null) {
        AddContributionDialog(
            goalName = contributionGoal!!.name,
            onAddContribution = { amount, note ->
                coroutineScope.launch {
                    val contribution = GoalContribution(
                        goalId = contributionGoal!!.id,
                        amount = amount,
                        date = System.currentTimeMillis(),
                        note = note
                    )
                    viewModel.addContribution(contribution)
                    contributionGoal = null
                }
            },
            onDismiss = {
                viewModel.hideAddContributionDialog()
                contributionGoal = null
            }
        )
    }
}

/**
 * Header section showing summary statistics
 *
 * @param activeGoalsCount Number of active goals
 * @param completedGoalsCount Number of completed goals
 * @param totalSaved Total amount saved across all goals
 */
@Composable
private fun SavingsGoalsHeader(
    activeGoalsCount: Int,
    completedGoalsCount: Int,
    totalSaved: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Savings Goals",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active Goals Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = activeGoalsCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Completed Goals Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = completedGoalsCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Total Saved Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "₹${formatAmount(totalSaved)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
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
