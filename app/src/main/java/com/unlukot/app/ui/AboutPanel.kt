package com.unlukot.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.unlukot.app.ai.DocType
import com.unlukot.app.ai.Kind

@Composable
fun AboutPanel() {
    val ctx = LocalContext.current
    val version = remember {
        runCatching {
            val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            "Version ${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
        }.getOrDefault("Version —")
    }

    AppCard(Modifier.fillMaxWidth()) {
        Column {
            Column(
                Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 18.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppLogo(size = 72.dp)
                Spacer(Modifier.height(12.dp))
                Text("UnLukot", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(version, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    "\u201cLukot\u201d is Filipino for crumpled.\nUnLukot un-crumples it.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ExpandableRow(Icons.Filled.Info, "What UnLukot does", initiallyOpen = true) {
                Text(
                    "UnLukot turns photos of wrinkled, folded or crumpled paper into clean digital text. " +
                        "It smooths out crease shadows, reads every line, then labels each part of the page " +
                        "so you can copy, share or export it as text or JSON.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant)
            ExpandableRow(Icons.Filled.Category, "What it recognizes") {
                Text("Paper types", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Tags(DocType.entries.map { it.label })
                Spacer(Modifier.height(12.dp))
                Text("Page parts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                Tags(Kind.entries.map { it.label })
            }
            HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant)
            ExpandableRow(Icons.Filled.Shield, "Privacy") {
                Text(
                    "Everything happens on your phone. UnLukot has no internet permission, works in airplane mode, " +
                        "and keeps saved scans in its private storage. Uninstalling the app removes them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(Modifier.padding(start = 70.dp), color = MaterialTheme.colorScheme.outlineVariant)
            ExpandableRow(Icons.Filled.Code, "Built with") {
                Credit("Jetpack Compose", "Material 3 user interface")
                Credit("CameraX", "Camera preview and capture")
                Credit("ML Kit Text Recognition", "Bundled Latin-script text model")
            }
        }
    }
}

@Composable
private fun ExpandableRow(
    icon: ImageVector,
    title: String,
    initiallyOpen: Boolean = false,
    content: @Composable () -> Unit
) {
    var open by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon, size = 40.dp)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ExpandMore, if (open) "Collapse" else "Expand", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(rotation))
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 68.dp, end = 18.dp, bottom = 14.dp)) { content() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(labels: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun Credit(name: String, role: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
