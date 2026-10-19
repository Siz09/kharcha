package com.sijan.kharcha.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
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

    data object AddEntry : Screen(
        route = "add_entry",
        labelRes = R.string.add_entry_title,
        icon = Icons.Default.Add
    )

    data object EditEntry : Screen(
        route = "edit_entry/{entryId}",
        labelRes = R.string.edit_entry_title,
        icon = Icons.Default.Add
    ) {
        fun createRoute(entryId: Long): String = "edit_entry/$entryId"
    }

    data object ConfirmScan : Screen(
        route = "confirm_scan?imageUri={imageUri}",
        labelRes = R.string.confirm_scan_title,
        icon = Icons.Default.Add
    ) {
        fun createRoute(imageUri: String): String = "confirm_scan?imageUri=${android.net.Uri.encode(imageUri)}"
    }

    data object Categories : Screen(
        route = "categories",
        labelRes = R.string.categories_title,
        icon = Icons.Default.Category
    )

    companion object {
        val bottomNavItems = listOf(Home, Entries, Summary, Settings)
    }
}

