package com.sijan.kharcha.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sijan.kharcha.R
import com.sijan.kharcha.ui.navigation.KharchaNavHost
import com.sijan.kharcha.ui.navigation.Screen

@Composable
fun KharchaApp(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit = {},
    onEntryClick: (Long) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val shouldShowFab = currentRoute == Screen.Home.route || currentRoute == Screen.Entries.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Screen.bottomNavItems.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = stringResource(screen.labelRes)
                            )
                        },
                        label = {
                            Text(text = stringResource(screen.labelRes))
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (shouldShowFab) {
                FloatingActionButton(
                    onClick = onAddClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_add_entry)
                    )
                }
            }
        }
    ) { innerPadding ->
        KharchaNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            onEntryClick = onEntryClick
        )
    }
}
