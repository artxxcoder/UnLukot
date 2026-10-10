package com.unlukot.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import android.view.MotionEvent
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.unlukot.app.ai.BitmapLoader
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    haptics: Boolean,
    onCaptured: (Bitmap) -> Unit,
    onPickGallery: () -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }
    BackHandler(onBack = onClose)

    val imageCapture = remember {
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var capturing by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    var focusAt by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(focusAt) {
        if (focusAt != null) {
            delay(900)
            focusAt = null
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    val previewView = PreviewView(c).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                    previewView.setOnTouchListener { v, e ->
                        if (e.action == MotionEvent.ACTION_UP) {
                            val point = previewView.meteringPointFactory.createPoint(e.x, e.y)
                            camera?.cameraControl?.startFocusAndMetering(
                                FocusMeteringAction.Builder(point)
                                    .setAutoCancelDuration(5, TimeUnit.SECONDS)
                                    .build()
                            )
                            focusAt = Offset(e.x, e.y)
                            v.performClick()
                        }
                        true
                    }
                    val future = ProcessCameraProvider.getInstance(c)
                    future.addListener({
                        val provider = future.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        runCatching {
                            provider.unbindAll()
                            camera = provider.bindToLifecycle(
                                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                            )
                        }
                    }, ContextCompat.getMainExecutor(c))
                    previewView
                }
            )
            // Green corner guide so the paper is framed well.
            Canvas(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 120.dp)) {
                val len = 54.dp.toPx()
                val sw = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                val g = GreenMid
                fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                    drawLine(g, Offset(x, y), Offset(x + dx * len, y), sw.width, StrokeCap.Round)
                    drawLine(g, Offset(x, y), Offset(x, y + dy * len), sw.width, StrokeCap.Round)
                }
                corner(0f, 0f, 1f, 1f)
                corner(size.width, 0f, -1f, 1f)
                corner(0f, size.height, 1f, -1f)
                corner(size.width, size.height, -1f, -1f)
            }
            focusAt?.let { p ->
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Color.White, radius = 34.dp.toPx(), center = p, style = Stroke(2.dp.toPx()))
                    drawCircle(GreenMid, radius = 4.dp.toPx(), center = p)
                }
            }
        } else {
            Text(
                "UnLukot needs the camera to scan paper.\nYour photos never leave this phone.",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp)
            )
        }

        // Top bar
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Close, "Close", tint = Color.White) }
            Text(
                "Scan paper",
                color = Color.White, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            )
            val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
            Box(
                Modifier.size(42.dp).clip(CircleShape)
                    .background(if (torchOn) Color.White else Color.Black.copy(alpha = 0.45f))
                    .clickable(enabled = hasFlash) {
                        torchOn = !torchOn
                        camera?.cameraControl?.enableTorch(torchOn)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    if (torchOn) "Turn flash off" else "Turn flash on",
                    tint = when {
                        torchOn -> GreenDark
                        hasFlash -> Color.White
                        else -> Color.White.copy(alpha = 0.35f)
                    }
                )
            }
        }

        // Bottom controls
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Fill the frame with the page · tap to focus",
                color = Color.White, fontSize = 13.sp,
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 14.dp, vertical = 6.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp, start = 32.dp, end = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable(onClick = onPickGallery),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.PhotoLibrary, "Gallery", tint = Color.White) }

                Box(
                    Modifier.size(78.dp).border(4.dp, Color.White, CircleShape).padding(7.dp)
                        .clip(CircleShape).background(if (capturing) GreenSoft else Green)
                        .clickable(enabled = hasPermission && !capturing) {
                            capturing = true
                            if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val rotation = image.imageInfo.rotationDegrees
                                    val bmp = runCatching { BitmapLoader.rotate(image.toBitmap(), rotation) }.getOrNull()
                                    image.close()
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        capturing = false
                                        if (bmp != null) onCaptured(bmp)
                                        else Toast.makeText(ctx, "Couldn't read the photo", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        capturing = false
                                        Toast.makeText(ctx, "Capture failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            })
                        }
                )
                // spacer to balance the layout
                Box(Modifier.size(52.dp))
            }
        }
    }
}
