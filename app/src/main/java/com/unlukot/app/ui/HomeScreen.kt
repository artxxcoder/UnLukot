package com.unlukot.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.unlukot.app.R
import com.unlukot.app.ai.ScanResult
import java.util.Calendar

@Composable
fun HomeScreen(
    history: List<ScanResult>,
    padding: PaddingValues,
    onScan: () -> Unit,
    onImport: () -> Unit,
    onOpen: (ScanResult) -> Unit,
    onSeeAll: () -> Unit,
    loadThumbnail: suspend (Long) -> Bitmap?
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HomeHeader() }
        item { HeroCard(onScan, onImport) }
        if (history.isNotEmpty()) item { StatsRow(history) }

        item { SectionHeader("How it works") }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    StepRow(Icons.Filled.AutoFixHigh, "Flatten", "Smooths out crease shadows and uneven light")
                    StepRow(Icons.Filled.TextFields, "Read", "Recognizes every line of printed text")
                    StepRow(Icons.Filled.Category, "Understand", "Labels titles, dates, amounts, IDs and more")
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                SectionHeader("Recent scans") {
                    TextButton(onClick = onSeeAll) {
                        Text("See all")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
                    }
                }
            }
            items(history.take(3), key = { it.id }) { r ->
                ScanRow(r, loadThumbnail, onClick = { onOpen(r) })
            }
        } else {
            item { TipCard() }
        }
    }
}

@Composable
fun AppLogo(size: androidx.compose.ui.unit.Dp = 44.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f))
            .background(Brush.linearGradient(listOf(GreenMid, Green, GreenDark))),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(size * 1.5f))
    }
}

@Composable
private fun HomeHeader() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppLogo()
        Spacer(Modifier.width(12.dp))
        Column {
            Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("UnLukot", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun HeroCard(onScan: () -> Unit, onImport: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(listOf(Green, GreenDark, GreenDeep)))
    ) {
        PaperDecoration(Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 18.dp).size(96.dp, 116.dp))
        Column(Modifier.padding(22.dp)) {
            Text(
                "Un-crumple\nany paper.",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan a wrinkled page and get clean, searchable text with every part labelled.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.fillMaxWidth(0.78f)
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScan,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = GreenDark),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Filled.CameraAlt, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scan paper")
                }
                OutlinedButton(
                    onClick = onImport,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import")
                }
            }
        }
    }
}

/** A tilted, slightly crumpled sheet of paper drawn behind the hero text. */
@Composable
private fun PaperDecoration(modifier: Modifier) {
    Canvas(modifier) {
        rotate(10f) {
            val w = size.width
            val h = size.height
            val sheet = Path().apply {
                moveTo(w * 0.05f, h * 0.06f)
                lineTo(w * 0.52f, 0f)
                lineTo(w * 0.95f, h * 0.05f)
                lineTo(w, h * 0.52f)
                lineTo(w * 0.94f, h * 0.97f)
                lineTo(w * 0.48f, h)
                lineTo(w * 0.04f, h * 0.95f)
                lineTo(0f, h * 0.48f)
                close()
            }
            drawPath(sheet, Color.White.copy(alpha = 0.16f))
            val crease = Color.White.copy(alpha = 0.18f)
            drawLine(crease, Offset(w * 0.52f, 0f), Offset(w * 0.48f, h), 1.5f)
            drawLine(crease, Offset(0f, h * 0.48f), Offset(w, h * 0.52f), 1.5f)
            val ink = Color.White.copy(alpha = 0.45f)
            listOf(0.2f to 0.7f, 0.32f to 0.55f, 0.66f to 0.72f, 0.78f to 0.5f).forEach { (y, len) ->
                drawLine(ink, Offset(w * 0.16f, h * y), Offset(w * (0.16f + len * 0.7f), h * y), 5f, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun StatsRow(history: List<ScanResult>) {
    val components = history.sumOf { it.components.size }
    val topType = history.groupingBy { it.docType }.eachCount().maxByOrNull { it.value }?.key
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("${history.size}", if (history.size == 1) "Scan" else "Scans", Modifier.weight(1f))
        StatTile("$components", "Parts found", Modifier.weight(1f))
        StatTile(topType?.label?.substringBefore(" /") ?: "—", "Most scanned", Modifier.weight(1f))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StepRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TipCard() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Lightbulb, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Tip for best results", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "Lay the page on a darker surface in even light. Creases are fine — that's what UnLukot is for.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}
