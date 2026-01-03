package com.example.kanakku.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kanakku.data.model.Category
import com.example.kanakku.data.model.DateRange
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.data.model.TransactionFilter
import com.example.kanakku.ui.MainUiState
import com.example.kanakku.ui.backup.BackupViewModel
import com.example.kanakku.ui.screens.AnalyticsScreen
import com.example.kanakku.ui.screens.BackupSettingsScreen
import com.example.kanakku.ui.screens.CategoriesScreen
import com.example.kanakku.ui.screens.ExportScreen
import com.example.kanakku.ui.screens.TransactionsScreen

@Composable
fun KanakkuNavHost(
    uiState: MainUiState,
    categoryMap: Map<Long, Category>,
    searchFilterState: SearchFilterState,
    onRefresh: () -> Unit,
    onCategoryChange: (Long, Category) -> Unit,
    onResetLearnedMappings: () -> Unit,
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
                    searchFilterState = searchFilterState,
                    onRefresh = onRefresh,
                    onCategoryChange = onCategoryChange,
                    onSearchQueryChange = onSearchQueryChange,
                    onFilterChange = onFilterChange,
                    onClearFilters = onClearFilters
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
                    merchantMappingCount = uiState.merchantMappingCount,
                    onCategoryChange = onCategoryChange,
                    onResetLearnedMappings = onResetLearnedMappings
                )
            }
            composable(BottomNavItem.Settings.route) {
                val context = LocalContext.current
                val backupViewModel: BackupViewModel = viewModel()
                val backupUiState by backupViewModel.uiState.collectAsState()

                // Initialize BackupViewModel with required dependencies
                // Note: CategoryManager will be properly wired in P5-S3
                LaunchedEffect(Unit) {
                    // TODO: Initialize with actual CategoryManager in P5-S3
                    // backupViewModel.initialize(context, categoryManager)
                }

                // Activity result launcher for creating backup file
                val createBackupLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
                ) { uri ->
                    if (uri != null) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                // TODO: Get actual transactions and category overrides from MainViewModel in P5-S3
                                val transactions = emptyList<ParsedTransaction>()
                                val categoryOverrides = emptyMap<Long, String>()
                                backupViewModel.createBackup(
                                    transactions = transactions,
                                    categoryOverrides = categoryOverrides,
                                    outputStream = outputStream
                                )
                            }
                        } catch (e: Exception) {
                            // Error will be handled by BackupViewModel and displayed in UI
                            android.util.Log.e("BackupSettings", "Failed to create backup file", e)
                        }
                    }
                }

                // Activity result launcher for opening backup file to restore
                val restoreBackupLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                backupViewModel.restoreBackup(
                                    inputStream = inputStream,
                                    onRestoreComplete = { transactions, categoryOverrides ->
                                        // TODO: Apply restored data to MainViewModel in P5-S3
                                        android.util.Log.d("BackupSettings", "Restore successful: ${transactions.size} transactions")
                                    }
                                )
                            }
                        } catch (e: Exception) {
                            // Error will be handled by BackupViewModel and displayed in UI
                            android.util.Log.e("BackupSettings", "Failed to open backup file", e)
                        }
                    }
                }

                // Activity result launcher for Google Drive sign-in
                val driveSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == android.app.Activity.RESULT_OK) {
                        backupViewModel.handleDriveSignIn(context)
                    } else {
                        android.util.Log.d("BackupSettings", "Google Sign-In cancelled or failed")
                    }
                }

                BackupSettingsScreen(
                    viewModel = backupViewModel,
                    uiState = backupUiState,
                    onCreateBackup = {
                        // Launch SAF file picker for creating backup
                        createBackupLauncher.launch("kanakku_backup_${System.currentTimeMillis()}.kbak")
                    },
                    onRestoreBackup = {
                        // Launch SAF file picker for opening backup file
                        restoreBackupLauncher.launch(arrayOf("*/*"))
                    },
                    onDriveSignIn = {
                        // Launch Google Sign-In intent
                        val signInIntent = backupViewModel.getGoogleSignInIntent()
                        if (signInIntent != null) {
                            driveSignInLauncher.launch(signInIntent)
                        } else {
                            android.util.Log.e("BackupSettings", "Failed to initialize Google Sign-In")
                        }
                    }
                )
            }
            composable(BottomNavItem.Export.route) {
                val exportViewModel: ExportViewModel = viewModel()
                ExportScreen(
                    viewModel = exportViewModel,
                    transactions = uiState.transactions
                )
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen()
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
