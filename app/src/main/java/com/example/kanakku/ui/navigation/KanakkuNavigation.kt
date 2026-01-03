package com.example.kanakku.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.kanakku.ui.SettingsViewModel
import com.example.kanakku.ui.screens.AnalyticsScreen
import com.example.kanakku.ui.screens.CategoriesScreen
import com.example.kanakku.ui.screens.SettingsScreen
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
                    categoryMap = categoryMap
                )
            }
            composable(BottomNavItem.Categories.route) {
                CategoriesScreen(
                    transactions = uiState.transactions,
                    categoryMap = categoryMap,
                    onCategoryChange = onCategoryChange
                )
            }
            composable(BottomNavItem.Settings.route) {
                val context = LocalContext.current
                val settingsViewModel: SettingsViewModel = viewModel()
                val settingsUiState by settingsViewModel.uiState.collectAsState()

                // Initialize ViewModel with context
                LaunchedEffect(Unit) {
                    settingsViewModel.initialize(context)
                }

                SettingsScreen(
                    budgetAlertSettings = settingsUiState.budgetAlertSettings,
                    largeTransactionSettings = settingsUiState.largeTransactionSettings,
                    weeklySummarySettings = settingsUiState.weeklySummarySettings,
                    onBudgetAlertsEnabledChange = { enabled ->
                        settingsViewModel.updateBudgetAlertsEnabled(enabled)
                    },
                    onBudgetAlert80PercentChange = { enabled ->
                        settingsViewModel.updateBudgetAlert80Percent(enabled)
                    },
                    onBudgetAlert100PercentChange = { enabled ->
                        settingsViewModel.updateBudgetAlert100Percent(enabled)
                    },
                    onLargeTransactionEnabledChange = { enabled ->
                        settingsViewModel.updateLargeTransactionEnabled(enabled)
                    },
                    onLargeTransactionThresholdChange = { threshold ->
                        settingsViewModel.updateLargeTransactionThreshold(threshold)
                    },
                    onWeeklySummaryEnabledChange = { enabled ->
                        settingsViewModel.updateWeeklySummaryEnabled(enabled)
                    },
                    onWeeklySummaryDayChange = { day ->
                        settingsViewModel.updateWeeklySummaryDay(day)
                    },
                    onWeeklySummaryHourChange = { hour ->
                        settingsViewModel.updateWeeklySummaryHour(hour)
                    }
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
