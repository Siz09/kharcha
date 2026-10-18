package com.sijan.kharcha.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.sijan.kharcha.R

sealed class Screen(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        labelRes = R.string.nav_home,
        icon = Icons.Default.Home
    )

    data object Entries : Screen(
        route = "entries",
        labelRes = R.string.nav_entries,
        icon = Icons.AutoMirrored.Filled.ListAlt
    )

    data object Summary : Screen(
        route = "summary",
        labelRes = R.string.nav_summary,
        icon = Icons.Default.PieChart
    )

    data object Settings : Screen(
        route = "settings",
        labelRes = R.string.nav_settings,
        icon = Icons.Default.Settings
    )

    companion object {
        val bottomNavItems = listOf(Home, Entries, Summary, Settings)
    }
}
