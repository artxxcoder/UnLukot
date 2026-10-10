package com.unlukot.app.ai

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * On-device OCR. Uses the BUNDLED ML Kit Latin text model, which lives inside the APK,
 * so it runs with airplane mode on and without the INTERNET permission.
 */
class LocalOcr {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): Text = suspendCancellableCoroutine { cont ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    fun close() = recognizer.close()

    companion object {
        /** Rough "how well did we read this" score: confidence weighted by characters read. */
        /** Too few lines or low confidence: likely rotated or badly framed. */
        fun looksUnreadable(text: Text): Boolean {
            val lines = text.textBlocks.flatMap { it.lines }
            if (lines.size < 3) return true
            val confs = lines.map { it.confidence }.filter { it > 0f }
            return confs.isNotEmpty() && confs.average() < 0.55
        }

        fun quality(text: Text): Float =
            text.textBlocks.flatMap { it.lines }.sumOf { line ->
                val conf = if (line.confidence > 0f) line.confidence else 0.8f
                (conf * line.text.length).toDouble()
            }.toFloat()
    }
}
