package com.unlukot.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val Steps = listOf("Smoothing creases", "Reading the text", "Finding the parts of the page")

@Composable
fun ProcessingScreen(step: Int, stage: String) {
    val transition = rememberInfiniteTransition(label = "scan")
    val sweep by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweep"
    )
    val progress by animateFloatAsState(step / 3f, tween(400), label = "progress")

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Green, GreenDark, GreenDeep)))
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(width = 190.dp, height = 240.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawRoundRect(Color.White, cornerRadius = CornerRadius(18.dp.toPx()))
                val crease = GreenSoft.copy(alpha = 1f - progress * 0.9f)
                drawLine(crease, Offset(size.width * 0.15f, size.height * 0.2f), Offset(size.width * 0.9f, size.height * 0.32f), 2.dp.toPx())
                drawLine(crease, Offset(size.width * 0.35f, 0f), Offset(size.width * 0.25f, size.height), 2.dp.toPx())
                drawLine(crease, Offset(0f, size.height * 0.7f), Offset(size.width, size.height * 0.62f), 2.dp.toPx())
                val ink = Green.copy(alpha = 0.55f)
                for (i in 0 until 7) {
                    val y = size.height * (0.14f + i * 0.1f)
                    val w = size.width * (0.45f + ((i * 37) % 40) / 100f)
                    drawLine(ink, Offset(size.width * 0.12f, y), Offset(size.width * 0.12f + w, y), 5.dp.toPx(), StrokeCap.Round)
                }
                val y = size.height * sweep
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, GreenMid.copy(alpha = 0.5f), Color.Transparent),
                        startY = y - 40.dp.toPx(), endY = y + 40.dp.toPx()
                    ),
                    topLeft = Offset(0f, y - 40.dp.toPx()), size = Size(size.width, 80.dp.toPx())
                )
                drawLine(GreenMid, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
            }
        }

        Spacer(Modifier.height(36.dp))
        Text(stage, style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .padding(vertical = 8.dp)
        ) {
            Steps.forEachIndexed { i, label ->
                val n = i + 1
                StepLine(label, done = step > n, active = step == n)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "This usually takes a second or two.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun StepLine(label: String, done: Boolean, active: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            when {
                done -> Box(
                    Modifier.size(26.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Check, null, tint = GreenDark, modifier = Modifier.size(16.dp)) }
                active -> CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                else -> Box(Modifier.size(22.dp).border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = if (done || active) 1f else 0.55f)
        )
    }
}
