package com.sijan.kharcha.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.sijan.kharcha.R
import com.sijan.kharcha.ui.navigation.KharchaNavHost
import com.sijan.kharcha.ui.navigation.Screen
import com.sijan.kharcha.ui.scan.ScanSourceBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KharchaApp(
    modifier: Modifier = Modifier,
    initialImageUri: Uri? = null,
    initialSmsPrefill: com.sijan.kharcha.domain.model.SmsPrefillData? = null,
    initialShortcutAction: String? = null,
    onEntryClick: (Long) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val isTopLevelDestination = Screen.bottomNavItems.any { it.route == currentRoute }
    val shouldShowFab = currentRoute == Screen.Home.route || currentRoute == Screen.Entries.route
    var showScanSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            navController.navigate(Screen.ConfirmScan.createRoute(uri.toString()))
        }
    }

    // Document scanner launcher
    val docScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pages = scanResult?.pages
            val imageUri = pages?.firstOrNull()?.imageUri
            if (imageUri != null) {
                navController.navigate(Screen.ConfirmScan.createRoute(imageUri.toString()))
            }
        }
    }

    // Handle initial shared image URI from intent
    LaunchedEffect(initialImageUri) {
        if (initialImageUri != null) {
            navController.navigate(Screen.ConfirmScan.createRoute(initialImageUri.toString()))
        }
    }

    // Handle initial SMS transaction prefill
    LaunchedEffect(initialSmsPrefill) {
        if (initialSmsPrefill != null) {
            navController.navigate(
                Screen.AddEntry.createRoute(
                    amountPaisa = initialSmsPrefill.amountPaisa,
                    note = initialSmsPrefill.note,
                    type = initialSmsPrefill.type,
                    date = initialSmsPrefill.date,
                    txnId = initialSmsPrefill.transactionId,
                    source = initialSmsPrefill.source
                )
            )
        }
    }

    // Handle launcher app shortcuts
    LaunchedEffect(initialShortcutAction) {
        when (initialShortcutAction) {
            "action_scan" -> showScanSheet = true
            "action_add_expense" -> navController.navigate(Screen.AddEntry.createRoute(type = "EXPENSE"))
            "action_add_income" -> navController.navigate(Screen.AddEntry.createRoute(type = "INCOME"))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevelDestination) {
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
        }
    },
        floatingActionButton = {
            if (shouldShowFab) {
                FloatingActionButton(
                    onClick = { showScanSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
            onEntryClick = { entryId ->
                onEntryClick(entryId)
                navController.navigate(Screen.EditEntry.createRoute(entryId))
            }
        )

        if (showScanSheet) {
            ScanSourceBottomSheet(
                sheetState = sheetState,
                onDismissRequest = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showScanSheet = false
                    }
                },
                onPickFromGallery = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onScanWithCamera = {
                    val activity = context as? Activity
                    if (activity != null) {
                        val scannerOptions = GmsDocumentScannerOptions.Builder()
                            .setGalleryImportAllowed(false)
                            .setPageLimit(1)
                            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                            .build()
                        val client = GmsDocumentScanning.getClient(scannerOptions)
                        client.getStartScanIntent(activity)
                            .addOnSuccessListener { intentSender ->
                                docScannerLauncher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                            .addOnFailureListener {
                                // Fallback to photo picker if document scanner unavailable
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    } else {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                onManualEntry = {
                    navController.navigate(Screen.AddEntry.route)
                }
            )
        }
    }
}
