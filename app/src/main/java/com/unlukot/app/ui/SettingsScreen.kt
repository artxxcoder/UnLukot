package com.unlukot.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.unlukot.app.ai.FlattenMode
import com.unlukot.app.data.AppSettings
import com.unlukot.app.data.ResultTab
import com.unlukot.app.data.ThemeMode

@Composable
fun SettingsScreen(
    settings: AppSettings,
    scanCount: Int,
    storageBytes: Long,
    padding: PaddingValues,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onClearHistory: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Tune how UnLukot scans, and learn about the app",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item { GroupLabel("Appearance") }
        item {
            SettingsGroup {
                ChoiceSetting(Icons.Filled.Palette, "Theme", null, ThemeMode.entries, settings.theme, { it.label }) { v ->
                    onChange { it.copy(theme = v) }
                }
            }
        }

        item { GroupLabel("Scanning") }
        item {
            SettingsGroup {
                ChoiceSetting(
                    Icons.Filled.AutoFixHigh, "Crease flattening", settings.flatten.description,
                    FlattenMode.entries, settings.flatten, { it.label }
                ) { v -> onChange { it.copy(flatten = v) } }
                Divider()
                SwitchSetting(Icons.Filled.Vibration, "Haptic feedback", "Vibrate when a photo is taken", settings.haptics) { v ->
                    onChange { it.copy(haptics = v) }
                }
            }
        }

        item { GroupLabel("Results") }
        item {
            SettingsGroup {
                SwitchSetting(Icons.Filled.CropFree, "Highlight boxes", "Outline detected parts on the scanned page", settings.showBoxes) { v ->
                    onChange { it.copy(showBoxes = v) }
                }
                Divider()
                ChoiceSetting(Icons.Filled.Tab, "Open results on", null, ResultTab.entries, settings.defaultTab, { it.label }) { v ->
                    onChange { it.copy(defaultTab = v) }
                }
            }
        }

        item { GroupLabel("Storage") }
        item {
            SettingsGroup {
                SwitchSetting(Icons.Filled.History, "Save scans to history", "Keep a private copy of each scan on this phone", settings.saveHistory) { v ->
                    onChange { it.copy(saveHistory = v) }
                }
                Divider()
                InfoRow(Icons.Filled.Storage, "Used space", "$scanCount ${if (scanCount == 1) "scan" else "scans"} · ${formatBytes(storageBytes)}")
                Divider()
                ActionRow(Icons.Filled.DeleteSweep, "Clear all scans", enabled = scanCount > 0, destructive = true) { confirmClear = true }
            }
        }

        item { GroupLabel("About") }
        item { AboutPanel() }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Filled.DeleteSweep, null) },
            title = { Text("Clear all scans?") },
            text = { Text("All $scanCount saved scans and their images will be permanently removed from this phone.") },
            confirmButton = {
                TextButton(onClick = { onClearHistory(); confirmClear = false }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun GroupLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable () -> Unit) {
    AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = 4.dp)) { content() } }
}

@Composable
private fun Divider() {
    HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant)
}
