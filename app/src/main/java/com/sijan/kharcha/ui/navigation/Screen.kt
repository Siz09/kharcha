package com.sijan.kharcha.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
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
        route = "add_entry?prefillAmountPaisa={prefillAmountPaisa}&prefillNote={prefillNote}&prefillType={prefillType}&prefillDate={prefillDate}&prefillTxnId={prefillTxnId}&prefillSource={prefillSource}",
        labelRes = R.string.add_entry_title,
        icon = Icons.Default.Add
    ) {
        fun createRoute(
            amountPaisa: Long? = null,
            note: String? = null,
            type: String? = null,
            date: Long? = null,
            txnId: String? = null,
            source: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (amountPaisa != null && amountPaisa > 0) params.add("prefillAmountPaisa=$amountPaisa")
            if (!note.isNullOrBlank()) params.add("prefillNote=${android.net.Uri.encode(note)}")
            if (!type.isNullOrBlank()) params.add("prefillType=${android.net.Uri.encode(type)}")
            if (date != null && date > 0) params.add("prefillDate=$date")
            if (!txnId.isNullOrBlank()) params.add("prefillTxnId=${android.net.Uri.encode(txnId)}")
            if (!source.isNullOrBlank()) params.add("prefillSource=${android.net.Uri.encode(source)}")
            return if (params.isEmpty()) "add_entry" else "add_entry?${params.joinToString("&")}"
        }
    }

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

    data object Calendar : Screen(
        route = "calendar",
        labelRes = R.string.calendar_title,
        icon = Icons.Default.CalendarToday
    )

    data object Bills : Screen(
        route = "bills",
        labelRes = R.string.bills_title,
        icon = Icons.Default.Receipt
    )

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Home, Entries, Summary, Settings)
    }
}

