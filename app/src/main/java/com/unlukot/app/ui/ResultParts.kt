package com.unlukot.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unlukot.app.ai.Component
import com.unlukot.app.ai.ScanResult

@Composable
fun SummaryCard(r: ScanResult) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primaryContainer).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(r.docType.icon, size = 52.dp, container = MaterialTheme.colorScheme.primary, tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("PAPER TYPE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text(r.docType.label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { r.typeConfidence },
                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                drawStopIndicator = {}
            )
            Spacer(Modifier.width(12.dp))
            Text("${(r.typeConfidence * 100).toInt()}% sure", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        r.runnerUp?.let {
            Spacer(Modifier.height(6.dp))
            Text("Could also be: ${it.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("Crumple", crumpleLabel(r.crumple), Modifier.weight(1f))
            Stat("Read in", "${r.ocrMs} ms", Modifier.weight(1f))
            Stat("Parts", "${r.components.size}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            (if (r.usedFlattened) "Creases flattened · " else "") + "Done in ${r.totalMs} ms on this phone",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

private fun crumpleLabel(v: Float) = when {
    v < 0.25f -> "Light"
    v < 0.55f -> "Moderate"
    else -> "Heavy"
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ScannedPage(
    bitmap: Bitmap,
    components: List<Component>,
    selected: Int?,
    showBoxes: Boolean,
    onToggleBoxes: () -> Unit,
    onSelect: (Int?) -> Unit
) {
    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
    Box(
        Modifier.fillMaxWidth().aspectRatio(ratio).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Image(bitmap.asImageBitmap(), "Scanned page", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        if (showBoxes) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(components) {
                    detectTapGestures { tap ->
                        val s = size.width.toFloat() / bitmap.width
                        val x = (tap.x / s).toInt()
                        val y = (tap.y / s).toInt()
                        val hit = components.withIndex()
                            .filter { it.value.box?.contains(x, y) == true }
                            .minByOrNull { (it.value.box!!.width() * it.value.box!!.height()) }
                        onSelect(hit?.index)
                    }
                }
            ) {
                val s = size.width / bitmap.width
                components.forEachIndexed { i, c ->
                    val b = c.box ?: return@forEachIndexed
                    val sel = i == selected
                    val dim = selected != null && !sel
                    val color = kindColor(c.kind)
                    val tl = Offset(b.left * s, b.top * s)
                    val sz = Size(b.width() * s, b.height() * s)
                    val r = CornerRadius(4.dp.toPx())
                    drawRoundRect(color.copy(alpha = if (sel) 0.32f else if (dim) 0.05f else 0.14f), tl, sz, r)
                    drawRoundRect(
                        color.copy(alpha = if (dim) 0.35f else 1f), tl, sz, r,
                        style = Stroke(width = if (sel) 3.dp.toPx() else 1.5.dp.toPx())
                    )
                }
            }
        }
        Surface(
            onClick = onToggleBoxes,
            shape = RoundedCornerShape(50),
            color = Color.Black.copy(alpha = 0.55f),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CropFree, null, Modifier.height(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (showBoxes) "Hide boxes" else "Show boxes", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun ComponentRow(c: Component, selected: Boolean, onClick: () -> Unit) {
    val color = kindColor(c.kind)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) color.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) color else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.height(IntrinsicSize.Min).padding(14.dp)) {
            Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        c.kind.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                    c.label?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${(c.confidence * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(6.dp))
                Text(c.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
