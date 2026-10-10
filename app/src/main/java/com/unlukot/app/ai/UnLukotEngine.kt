package com.unlukot.app.ai

import android.graphics.Bitmap
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** How the crease-flattening step is applied before reading the page. */
enum class FlattenMode(val label: String, val description: String) {
    AUTO("Auto", "Reads both versions and keeps the clearer one"),
    ALWAYS("Always", "Always reads the flattened page"),
    OFF("Off", "Reads the original photo only (fastest)")
}

/** The full scan pipeline: flatten → read → understand. All on the phone. */
class UnLukotEngine {

    data class Output(val result: ScanResult, val image: Bitmap)

    private val ocr = LocalOcr()

    suspend fun analyze(
        source: Bitmap,
        mode: FlattenMode = FlattenMode.AUTO,
        onStage: (step: Int, label: String) -> Unit
    ): Output =
        withContext(Dispatchers.Default) {
            val t0 = SystemClock.elapsedRealtime()

            onStage(1, "Smoothing creases & shadows…")
            val base = ImageEnhancer.prepare(source)
            val crumple = ImageEnhancer.crumpleScore(base)
            val flat = if (mode == FlattenMode.OFF) null else ImageEnhancer.flatten(base)

            onStage(2, "Reading the text…")
            val tOcr = SystemClock.elapsedRealtime()
            val readBase = if (mode == FlattenMode.ALWAYS) null else ocr.recognize(base)
            val readFlat = flat?.let { ocr.recognize(it) }
            val useFlat = when {
                readFlat == null -> false
                readBase == null -> true
                else -> LocalOcr.quality(readFlat) >= LocalOcr.quality(readBase) * 0.98f
            }
            var text = if (useFlat) readFlat!! else readBase!!
            var image = if (useFlat) flat!! else base

            // Phones pointed straight down often report the wrong orientation; retry sideways/upside-down.
            if (LocalOcr.looksUnreadable(text)) {
                onStage(2, "Trying another angle…")
                for (deg in intArrayOf(90, 270, 180)) {
                    val turned = BitmapLoader.rotate(image, deg)
                    val read = ocr.recognize(turned)
                    if (LocalOcr.quality(read) > LocalOcr.quality(text) * 1.3f) {
                        text = read
                        image = turned
                    }
                }
            }
            val ocrMs = SystemClock.elapsedRealtime() - tOcr

            onStage(3, "Finding the parts of the page…")
            val analysis = DocumentAnalyzer.analyze(text, image.width, image.height)

            val now = System.currentTimeMillis()
            Output(
                ScanResult(
                    id = now,
                    createdAt = now,
                    rawText = analysis.rawText,
                    docType = analysis.docType,
                    typeConfidence = analysis.confidence,
                    runnerUp = analysis.runnerUp,
                    components = analysis.components,
                    crumple = crumple,
                    ocrMs = ocrMs,
                    totalMs = SystemClock.elapsedRealtime() - t0,
                    imgW = image.width,
                    imgH = image.height,
                    usedFlattened = useFlat
                ),
                image
            )
        }

    fun close() = ocr.close()
}
