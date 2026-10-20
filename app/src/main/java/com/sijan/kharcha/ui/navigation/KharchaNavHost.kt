package com.sijan.kharcha.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sijan.kharcha.ui.calendar.CashFlowCalendarScreen
import com.sijan.kharcha.ui.entries.AddEditEntryScreen
import com.sijan.kharcha.ui.entries.EntriesScreen
import com.sijan.kharcha.ui.home.HomeScreen
import com.sijan.kharcha.ui.reports.SpendingReportsScreen
import com.sijan.kharcha.ui.scan.ConfirmScanScreen
import com.sijan.kharcha.ui.settings.categories.CategoriesScreen
import com.sijan.kharcha.ui.settings.SettingsScreen
import com.sijan.kharcha.ui.summary.SummaryScreen

@Composable
fun KharchaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToEntries: () -> Unit = {
        navController.navigate(Screen.Entries.route) {
            popUpTo(Screen.Home.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    },
    onEntryClick: (Long) -> Unit = { entryId ->
        navController.navigate(Screen.EditEntry.createRoute(entryId))
    }
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToEntries = onNavigateToEntries,
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                onNavigateToUdhar = { navController.navigate(Screen.Udhar.route) },
                onNavigateToSavings = { navController.navigate(Screen.SavingsGoals.route) },
                onEntryClick = onEntryClick
            )
        }
        composable(Screen.Calendar.route) {
            CashFlowCalendarScreen(
                onNavigateBack = { navController.popBackStack() },
                onEntryClick = onEntryClick
            )
        }
        composable(Screen.Entries.route) {
            EntriesScreen(
                onEntryClick = onEntryClick
            )
        }
        composable(Screen.Summary.route) {
            SummaryScreen(
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                }
            )
        }
        composable(Screen.Reports.route) {
            SpendingReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                },
                onNavigateToBills = {
                    navController.navigate(Screen.Bills.route)
                },
                onNavigateToUdhar = {
                    navController.navigate(Screen.Udhar.route)
                },
                onNavigateToSavings = {
                    navController.navigate(Screen.SavingsGoals.route)
                }
            )
        }
        composable(Screen.Bills.route) {
            com.sijan.kharcha.ui.bills.BillsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Udhar.route) {
            com.sijan.kharcha.ui.udhar.UdharScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SavingsGoals.route) {
            com.sijan.kharcha.ui.savings.SavingsGoalsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Categories.route) {
            CategoriesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.AddEntry.route,
            arguments = listOf(
                navArgument("prefillAmountPaisa") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("prefillNote") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("prefillType") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("prefillDate") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("prefillTxnId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("prefillSource") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            AddEditEntryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditEntry.route,
            arguments = listOf(
                navArgument("entryId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) {
            AddEditEntryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ConfirmScan.route,
            arguments = listOf(
                navArgument("imageUri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            ConfirmScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEntry = { entryId ->
                    navController.navigate(Screen.EditEntry.createRoute(entryId))
                }
            )
        }
    }
}
