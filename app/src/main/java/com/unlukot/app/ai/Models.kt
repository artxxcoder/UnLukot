package com.unlukot.app.ai

import android.graphics.Rect
import org.json.JSONArray
import org.json.JSONObject

/** What kind of paper the on-device model thinks it is looking at. */
enum class DocType(val label: String, val emoji: String) {
    RECEIPT("Receipt", "🧾"),
    INVOICE("Invoice / Bill", "📄"),
    ID_CARD("ID / Certificate", "🪪"),
    FORM("Form", "📝"),
    LETTER("Letter / Memo", "✉️"),
    SCHEDULE("Schedule / Table", "🗓️"),
    NOTE("Note / Other", "🗒️")
}

/** The kinds of components UnLukot can pull out of a page. */
enum class Kind(val label: String) {
    TITLE("Title"),
    FIELD("Field"),
    DATE("Date"),
    TIME("Time"),
    AMOUNT("Amount"),
    EMAIL("Email"),
    PHONE("Phone"),
    URL("Link"),
    ID_NUMBER("ID number"),
    TABLE_ROW("Table row"),
    TEXT("Paragraph")
}

data class Component(
    val kind: Kind,
    val text: String,
    val label: String?,
    val confidence: Float,
    val box: Rect?
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("kind", kind.name)
        put("text", text)
        put("label", label ?: JSONObject.NULL)
        put("confidence", confidence.toDouble())
        box?.let { put("box", JSONArray(listOf(it.left, it.top, it.right, it.bottom))) }
    }

    companion object {
        fun fromJson(o: JSONObject): Component {
            val b = o.optJSONArray("box")
            return Component(
                kind = runCatching { Kind.valueOf(o.getString("kind")) }.getOrDefault(Kind.TEXT),
                text = o.getString("text"),
                label = if (o.isNull("label")) null else o.getString("label"),
                confidence = o.optDouble("confidence", 0.8).toFloat(),
                box = b?.let { Rect(it.getInt(0), it.getInt(1), it.getInt(2), it.getInt(3)) }
            )
        }
    }
}

data class ScanResult(
    val id: Long,
    val createdAt: Long,
    val rawText: String,
    val docType: DocType,
    val typeConfidence: Float,
    val runnerUp: DocType?,
    val components: List<Component>,
    val crumple: Float,
    val ocrMs: Long,
    val totalMs: Long,
    val imgW: Int,
    val imgH: Int,
    val usedFlattened: Boolean
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("app", "UnLukot")
        put("engine", "on-device (ML Kit bundled OCR + UnLukot component predictor)")
        put("id", id)
        put("createdAt", createdAt)
        put("docType", docType.name)
        put("docTypeLabel", docType.label)
        put("typeConfidence", typeConfidence.toDouble())
        put("runnerUp", runnerUp?.name ?: JSONObject.NULL)
        put("crumpleLevel", crumple.toDouble())
        put("ocrMs", ocrMs)
        put("totalMs", totalMs)
        put("imgW", imgW)
        put("imgH", imgH)
        put("usedFlattened", usedFlattened)
        put("rawText", rawText)
        put("components", JSONArray().also { arr -> components.forEach { arr.put(it.toJson()) } })
    }

    fun toPlainText(): String = buildString {
        appendLine("UnLukot scan — ${docType.label} (${(typeConfidence * 100).toInt()}% sure)")
        appendLine("Processed on this phone by UnLukot.")
        appendLine()
        components.forEach { c ->
            val name = c.label?.let { "${c.kind.label} · $it" } ?: c.kind.label
            appendLine("[$name] ${c.text}")
        }
        appendLine()
        appendLine("— Full text —")
        append(rawText)
    }

    companion object {
        fun fromJson(o: JSONObject): ScanResult {
            val arr = o.optJSONArray("components") ?: JSONArray()
            return ScanResult(
                id = o.getLong("id"),
                createdAt = o.getLong("createdAt"),
                rawText = o.optString("rawText", ""),
                docType = runCatching { DocType.valueOf(o.getString("docType")) }.getOrDefault(DocType.NOTE),
                typeConfidence = o.optDouble("typeConfidence", 0.5).toFloat(),
                runnerUp = if (o.isNull("runnerUp")) null
                else runCatching { DocType.valueOf(o.getString("runnerUp")) }.getOrNull(),
                components = (0 until arr.length()).map { Component.fromJson(arr.getJSONObject(it)) },
                crumple = o.optDouble("crumpleLevel", 0.0).toFloat(),
                ocrMs = o.optLong("ocrMs"),
                totalMs = o.optLong("totalMs"),
                imgW = o.optInt("imgW", 1),
                imgH = o.optInt("imgH", 1),
                usedFlattened = o.optBoolean("usedFlattened", false)
            )
        }
    }
}
