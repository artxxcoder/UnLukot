package com.unlukot.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.unlukot.app.ai.ScanResult
import org.json.JSONObject
import java.io.File

/** Saves scans in the app's private storage. Nothing leaves the device. */
class ScanStore(context: Context) {
    private val dir = File(context.filesDir, "scans").apply { mkdirs() }

    fun save(result: ScanResult, image: Bitmap) {
        File(dir, "${result.id}.json").writeText(result.toJson().toString())
        File(dir, "${result.id}.jpg").outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    }

    fun list(): List<ScanResult> =
        dir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { runCatching { ScanResult.fromJson(JSONObject(it.readText())) }.getOrNull() }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()

    fun image(id: Long): Bitmap? = BitmapFactory.decodeFile(File(dir, "$id.jpg").path)

    fun thumbnail(id: Long, targetPx: Int = 160): Bitmap? {
        val path = File(dir, "$id.jpg").path
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= targetPx) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun sizeBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    fun delete(id: Long) {
        File(dir, "$id.json").delete()
        File(dir, "$id.jpg").delete()
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }
}
