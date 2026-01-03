package com.example.kanakku.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.example.kanakku.data.model.ParsedTransaction
import com.example.kanakku.ui.MainUiState
import com.example.kanakku.ui.screens.AnalyticsScreen
import com.example.kanakku.ui.screens.CategoriesScreen
import com.example.kanakku.ui.screens.CategoryEditorScreen
import com.example.kanakku.ui.screens.CategoryManagementScreen
import com.example.kanakku.ui.screens.TransactionsScreen
import com.example.kanakku.ui.viewmodel.CategoryEditorViewModel
import com.example.kanakku.ui.viewmodel.CategoryManagementViewModel

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

            // Category Management Screen
            composable(NavRoutes.CATEGORY_MANAGEMENT) {
                val viewModel: CategoryManagementViewModel = viewModel()
                val categoryUiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.loadCategories()
                }

                CategoryManagementScreen(
                    uiState = categoryUiState,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onFilterParentChange = { category ->
                        viewModel.loadSubcategories(category.id)
                    },
                    onClearFilter = viewModel::loadCategories,
                    onAddCategoryClick = {
                        navController.navigate(NavRoutes.categoryEditorRoute())
                    },
                    onEditCategory = { category ->
                        navController.navigate(NavRoutes.categoryEditorRoute(category.id))
                    },
                    onDeleteCategory = viewModel::requestDeleteCategory,
                    onConfirmDelete = viewModel::confirmDeleteCategory,
                    onCancelDelete = viewModel::cancelDeleteCategory,
                    onErrorDismiss = viewModel::clearError,
                    onSuccessDismiss = viewModel::clearSuccessMessage
                )
            }

            // Category Editor Screen
            composable(
                route = NavRoutes.CATEGORY_EDITOR_ROUTE,
                arguments = listOf(
                    navArgument(NavRoutes.ARG_CATEGORY_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val viewModel: CategoryEditorViewModel = viewModel()
                val editorUiState by viewModel.uiState.collectAsState()
                val categoryId = backStackEntry.arguments?.getString(NavRoutes.ARG_CATEGORY_ID)

                // Load category for editing if categoryId is provided
                LaunchedEffect(categoryId) {
                    viewModel.loadAvailableParentCategories()
                    categoryId?.let {
                        viewModel.loadCategoryForEditing(it)
                    }
                }

                CategoryEditorScreen(
                    uiState = editorUiState,
                    onNameChange = viewModel::setName,
                    onIconChange = viewModel::setIcon,
                    onColorChange = viewModel::setColor,
                    onParentCategoryChange = viewModel::setParentCategory,
                    onClearParentCategory = {
                        viewModel.setParentCategory(null, null)
                    },
                    onAddKeyword = viewModel::addKeyword,
                    onRemoveKeyword = viewModel::removeKeyword,
                    onSave = {
                        viewModel.validateAndSave(
                            onSuccess = {
                                navController.popBackStack()
                            }
                        )
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                    onErrorDismiss = viewModel::clearError,
                    onSuccessDismiss = viewModel::clearSuccessMessage
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
