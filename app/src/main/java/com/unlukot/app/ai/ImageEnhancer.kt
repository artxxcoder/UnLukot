package com.unlukot.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pure on-device image processing that "un-crumples" a photo of a wrinkled page.
 *
 * Crumpled paper breaks OCR mostly because of uneven lighting: every crease casts a
 * shadow or a bright ridge. We estimate the local paper brightness (ignoring ink),
 * divide it out, then apply a gentle contrast curve. No network, no cloud.
 */
object ImageEnhancer {

    /** Scale the image down so its longest side is at most [maxSide]. */
    fun prepare(src: Bitmap, maxSide: Int = 2400): Bitmap {
        val longest = max(src.width, src.height)
        if (longest <= maxSide) return src.copy(Bitmap.Config.ARGB_8888, false)
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(
            src, (src.width * scale).toInt(), (src.height * scale).toInt(), true
        )
    }

    /** 0f (flat paper) .. 1f (heavily crumpled). A fast estimate from shading variation. */
    fun crumpleScore(src: Bitmap): Float {
        val n = 48
        val small = Bitmap.createScaledBitmap(src, n, n, true)
        val px = IntArray(n * n)
        small.getPixels(px, 0, n, 0, 0, n, n)
        val g = FloatArray(n * n) {
            val c = px[it]
            (0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)) / 255f
        }
        var sum = 0f
        var count = 0
        for (y in 0 until n - 1) for (x in 0 until n - 1) {
            val i = y * n + x
            sum += abs(g[i] - g[i + 1]) + abs(g[i] - g[i + n])
            count += 2
        }
        return ((sum / count) * 9f).coerceIn(0f, 1f)
    }

    /**
     * Remove crease shadows / uneven light, sharpen strokes and boost text contrast
     * without clipping faint or thin print.
     */
    fun flatten(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val gray = FloatArray(w * h) {
            val c = px[it]
            0.299f * ((c shr 16) and 0xFF) + 0.587f * ((c shr 8) and 0xFF) + 0.114f * (c and 0xFF)
        }

        // 1) Estimate paper brightness per cell (90th percentile ignores ink).
        val cell = max(16, max(w, h) / 80)
        val gw = (w + cell - 1) / cell
        val gh = (h + cell - 1) / cell
        val raw = FloatArray(gw * gh)
        val buf = FloatArray(cell * cell)
        for (gy in 0 until gh) for (gx in 0 until gw) {
            var n = 0
            val x0 = gx * cell
            val y0 = gy * cell
            for (y in y0 until min(y0 + cell, h)) for (x in x0 until min(x0 + cell, w)) {
                buf[n++] = gray[y * w + x]
            }
            java.util.Arrays.sort(buf, 0, n)
            raw[gy * gw + gx] = max(buf[((n * 0.95f).toInt()).coerceAtMost(n - 1)], 30f)
        }
        // Cells filled by big/bold letters look far darker than their neighbours: patch those only,
        // so real crease shadows (which change gradually cell to cell) are still divided out.
        val bg = patchInkCells(raw, gw, gh)

        // 2) Divide out the background (in place).
        for (y in 0 until h) {
            val fy = ((y + 0.5f) / cell - 0.5f).coerceIn(0f, (gh - 1).toFloat())
            val y0 = fy.toInt()
            val y1 = min(y0 + 1, gh - 1)
            val ty = fy - y0
            for (x in 0 until w) {
                val fx = ((x + 0.5f) / cell - 0.5f).coerceIn(0f, (gw - 1).toFloat())
                val x0 = fx.toInt()
                val x1 = min(x0 + 1, gw - 1)
                val tx = fx - x0
                val top = bg[y0 * gw + x0] * (1 - tx) + bg[y0 * gw + x1] * tx
                val bottom = bg[y1 * gw + x0] * (1 - tx) + bg[y1 * gw + x1] * tx
                val i = y * w + x
                gray[i] = (gray[i] / (top * (1 - ty) + bottom * ty) * 255f).coerceIn(0f, 255f)
            }
        }

        // 3) Unsharp mask (3x3 box blur) + tone curve that keeps faint strokes visible.
        val hBlur = FloatArray(w * h)
        for (y in 0 until h) {
            val r = y * w
            for (x in 0 until w) {
                val l = gray[r + max(x - 1, 0)]
                val c = gray[r + x]
                val rr = gray[r + min(x + 1, w - 1)]
                hBlur[r + x] = (l + c + rr) / 3f
            }
        }
        for (y in 0 until h) {
            val up = max(y - 1, 0) * w
            val mid = y * w
            val dn = min(y + 1, h - 1) * w
            for (x in 0 until w) {
                val blur = (hBlur[up + x] + hBlur[mid + x] + hBlur[dn + x]) / 3f
                val v = gray[mid + x]
                val sharp = (v + SHARPEN * (v - blur)).toInt().coerceIn(0, 255)
                val g = TONE[sharp]
                px[mid + x] = (0xFF shl 24) or (g shl 16) or (g shl 8) or g
            }
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    private const val SHARPEN = 0.8f

    /** Paper (~255) goes white, ink stays dark, mid-greys are darkened rather than clipped. */
    private val TONE = IntArray(256) { v ->
        val s = ((v - 25f) / (228f - 25f)).coerceIn(0f, 1f)
        (Math.pow(s.toDouble(), 1.5) * 255.0).toInt().coerceIn(0, 255)
    }

    private fun patchInkCells(g: FloatArray, gw: Int, gh: Int) = FloatArray(gw * gh) { i ->
        val gx = i % gw
        val gy = i / gw
        val around = ArrayList<Float>(8)
        for (dy in -1..1) for (dx in -1..1) {
            val x = gx + dx
            val y = gy + dy
            if ((dx != 0 || dy != 0) && x in 0 until gw && y in 0 until gh) around += g[y * gw + x]
        }
        around.sort()
        val median = if (around.isEmpty()) g[i] else around[around.size / 2]
        if (g[i] < median * 0.7f) median else g[i]
    }
}

/** Loads gallery images with correct rotation and a sane memory footprint. */
object BitmapLoader {

    fun load(ctx: Context, uri: Uri, maxSide: Int = 2400): Bitmap? {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val rotation = runCatching {
            cr.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)
        return rotate(bmp, rotation)
    }

    fun rotate(bmp: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bmp
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
}
