package com.unlukot.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.unlukot.app.ScanViewModel
import com.unlukot.app.ScanViewModel.UiState

enum class HomeTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Filled.Home),
    History("History", Icons.Outlined.History, Icons.Filled.History),
    Settings("Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun UnLukotApp(vm: ScanViewModel, darkTheme: Boolean) {
    val state by vm.state.collectAsState()
    val history by vm.history.collectAsState()
    val settings by vm.settings.collectAsState()
    val storageBytes by vm.storageBytes.collectAsState()
    var tab by rememberSaveable { mutableStateOf(HomeTab.Home) }
    var showCamera by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            showCamera = false
            vm.processUri(uri)
        }
    }
    val pickImage = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    val fullBleedDark = state is UiState.Processing || (showCamera && state !is UiState.Done)
    SystemBarIcons(darkIcons = !darkTheme && !fullBleedDark)

    when (val s = state) {
        is UiState.Processing -> ProcessingScreen(s.step, s.stage)
        is UiState.Done -> ResultScreen(
            result = s.result,
            image = s.image,
            showBoxes = settings.showBoxes,
            initialTab = settings.defaultTab.ordinal,
            onBack = vm::reset
        )
        else -> {
            if (showCamera) {
                CameraScreen(
                    haptics = settings.haptics,
                    onCaptured = { bmp ->
                        showCamera = false
                        vm.processBitmap(bmp)
                    },
                    onPickGallery = { pickImage() },
                    onClose = { showCamera = false }
                )
            } else {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 0.dp
                        ) {
                            HomeTab.entries.forEach { t ->
                                val selected = tab == t
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { tab = t },
                                    icon = { Icon(if (selected) t.selectedIcon else t.icon, contentDescription = t.label) },
                                    label = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                ) { padding ->
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                        label = "tab"
                    ) { current ->
                        when (current) {
                            HomeTab.Home -> HomeScreen(
                                history = history,
                                padding = padding,
                                onScan = { showCamera = true },
                                onImport = { pickImage() },
                                onOpen = vm::open,
                                onSeeAll = { tab = HomeTab.History },
                                loadThumbnail = vm::thumbnail
                            )
                            HomeTab.History -> HistoryScreen(
                                history = history,
                                padding = padding,
                                onOpen = vm::open,
                                onDelete = vm::delete,
                                onScan = { showCamera = true },
                                loadThumbnail = vm::thumbnail
                            )
                            HomeTab.Settings -> SettingsScreen(
                                settings = settings,
                                scanCount = history.size,
                                storageBytes = storageBytes,
                                padding = padding,
                                onChange = vm::updateSettings,
                                onClearHistory = vm::clearHistory
                            )
                        }
                    }
                }
            }

            if (s is UiState.Failed) {
                AlertDialog(
                    onDismissRequest = vm::reset,
                    icon = { Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
                    confirmButton = { TextButton(onClick = vm::reset) { Text("OK") } },
                    title = { Text("Scan problem") },
                    text = { Text(s.message) }
                )
            }
        }
    }
}
