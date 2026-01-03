package com.example.kanakku.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.ui.MainUiState
import com.example.kanakku.ui.budget.BudgetViewModel
import com.example.kanakku.ui.screens.AnalyticsScreen
import com.example.kanakku.ui.screens.BudgetScreen
import com.example.kanakku.ui.screens.CategoriesScreen
import com.example.kanakku.ui.screens.TransactionsScreen

@Composable
fun KanakkuNavHost(
    uiState: MainUiState,
    categoryMap: Map<Long, Category>,
    onRefresh: () -> Unit,
    onCategoryChange: (Long, Category) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    Scaffold(
        bottomBar = {
            KanakkuBottomBar(navController = navController)
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Transactions.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Transactions.route) {
                TransactionsScreen(
                    uiState = uiState,
                    categoryMap = categoryMap,
                    onRefresh = onRefresh,
                    onCategoryChange = onCategoryChange
                )
            }
            composable(BottomNavItem.Analytics.route) {
                AnalyticsScreen(
                    transactions = uiState.transactions,
                    categoryMap = categoryMap,
                    budgetSummary = uiState.budgetSummary,
                    onNavigateToBudget = {
                        navController.navigate(BottomNavItem.Budget.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(BottomNavItem.Budget.route) {
                val context = LocalContext.current
                val budgetViewModel: BudgetViewModel = viewModel()
                val budgetUiState by budgetViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    budgetViewModel.initialize(context)
                    budgetViewModel.updateCategoryMap(categoryMap)
                    budgetViewModel.loadBudgets()
                }

                BudgetScreen(
                    uiState = budgetUiState,
                    onEditBudget = { budget ->
                        budgetViewModel.startEditBudget(
                            budget = budget,
                            isOverallBudget = budget == null || budget.categoryId == null
                        )
                    },
                    onDeleteBudget = { budget ->
                        budgetViewModel.deleteBudget(budget)
                    },
                    onAddCategoryBudget = {
                        budgetViewModel.startEditBudget(
                            budget = null,
                            isOverallBudget = false
                        )
                    },
                    onMonthChange = { month, year ->
                        budgetViewModel.changeMonth(month, year)
                    },
                    onSaveBudget = { amount, categoryId ->
                        budgetViewModel.saveBudgetFromDialog(amount, categoryId)
                    },
                    onCancelEdit = {
                        budgetViewModel.cancelEdit()
                    }
                )
            }
            composable(BottomNavItem.Categories.route) {
                CategoriesScreen(
                    transactions = uiState.transactions,
                    categoryMap = categoryMap,
                    onCategoryChange = onCategoryChange
                )
            }
        }
    }
}

@Composable
fun KanakkuBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        BottomNavItem.items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
