package com.sijan.kharcha.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sijan.kharcha.ui.entries.AddEditEntryScreen
import com.sijan.kharcha.ui.entries.EntriesScreen
import com.sijan.kharcha.ui.home.HomeScreen
import com.sijan.kharcha.ui.scan.ConfirmScanScreen
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
                onEntryClick = onEntryClick
            )
        }
        composable(Screen.Entries.route) {
            EntriesScreen(
                onEntryClick = onEntryClick
            )
        }
        composable(Screen.Summary.route) {
            SummaryScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(Screen.AddEntry.route) {
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
