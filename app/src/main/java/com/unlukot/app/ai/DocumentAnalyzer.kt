package com.unlukot.app.ai

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import kotlin.math.exp
import kotlin.math.min

/**
 * UnLukot's on-device "paper understanding" model.
 *
 *  1. Component predictor – walks OCR lines and predicts what each one *is*
 *     (title, labelled field, date, amount, phone, ID number, table row, paragraph…).
 *  2. Document-type classifier – a tiny weighted-lexicon model with a softmax output
 *     (English + Filipino cue words) that predicts the kind of paper it is.
 *
 * Everything runs locally in a few milliseconds. No cloud calls.
 */
object DocumentAnalyzer {

    data class Analysis(
        val docType: DocType,
        val confidence: Float,
        val runnerUp: DocType?,
        val components: List<Component>,
        val rawText: String
    )

    // ---------- patterns ----------
    private val EMAIL = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val URL = Regex("(?:https?://|www\\.)[^\\s]+", RegexOption.IGNORE_CASE)
    private const val MONTHS = "(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\.?"
    private val DATE = Regex(
        "\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b|\\b\\d{4}-\\d{2}-\\d{2}\\b|" +
            "\\b$MONTHS\\s+\\d{1,2}(?:st|nd|rd|th)?,?\\s+\\d{2,4}\\b|" +
            "\\b\\d{1,2}\\s+$MONTHS,?\\s+\\d{2,4}\\b",
        RegexOption.IGNORE_CASE
    )
    private val TIME = Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\s?(?:AM|PM|am|pm)?")
    private val PHONE = Regex(
        "(?:\\+\\d{1,3}[\\s-]?\\(?\\d{1,4}\\)?[\\s-]?\\d{3,4}[\\s-]?\\d{3,4})|" +
            "(?:\\b0\\d{2,3}[\\s-]?\\d{3,4}[\\s-]?\\d{4}\\b)|" +
            "(?:\\(\\d{2,4}\\)\\s?\\d{3,4}[\\s-]?\\d{4})"
    )
    private val AMOUNT = Regex(
        "(?:₱|PHP|Php|\\$|USD|€|£)\\s?\\d[\\d,]*(?:\\.\\d{1,2})?|\\b\\d{1,3}(?:,\\d{3})*\\.\\d{2}\\b"
    )
    private val FIELD = Regex("^\\s*([A-Za-z][A-Za-z .'/()-]{1,28})\\s*[:：]\\s*(.+)$")
    private val ID_INLINE = Regex(
        "(?i)\\b(?:id|no\\.?|tin|sss|ref(?:erence)?|serial|invoice|receipt)\\s*(?:no\\.?|number|#)?\\s*[:#]?\\s*([A-Z0-9][A-Z0-9-]{5,})"
    )
    private val ID_LABEL = Regex("(?i)\\b(id|no|number|tin|sss|ref|reference|serial|license|licence|reg|registration)\\b")
    private val TOTAL_WORDS = Regex(
        "(?i)sub-?total|total|amount due|balance|change|cash|vat|tax|discount|kabuuan|sukli"
    )

    // ---------- public API ----------
    fun analyze(text: Text, imgW: Int, imgH: Int): Analysis {
        val blocks = text.textBlocks
        val heights = blocks.flatMap { it.lines }.mapNotNull { it.boundingBox?.height() }.sorted()
        val medianH = if (heights.isEmpty()) 0 else heights[heights.size / 2]

        val comps = mutableListOf<Component>()
        var titles = 0

        for (block in blocks) {
            val loose = mutableListOf<Text.Line>()
            for (line in block.lines) {
                val t = OcrFixes.digits(line.text.trim())
                if (t.length < 2) continue
                val box = line.boundingBox
                val conf = (if (line.confidence > 0f) line.confidence else 0.8f).coerceIn(0f, 1f)

                val parts = classifyLine(t, box, conf)
                if (parts.isNotEmpty()) {
                    comps += parts
                    continue
                }

                val looksTitle = titles < 2 && box != null && medianH > 0 &&
                    box.top < imgH * 0.30f &&
                    (box.height() >= medianH * 1.35f ||
                        (t == t.uppercase() && t.length in 4..40 && t.any { it.isLetter() }))
                if (looksTitle) {
                    titles++
                    comps += Component(Kind.TITLE, t, null, conf, box)
                } else {
                    loose += line
                }
            }
            if (loose.isNotEmpty()) {
                val joined = loose.joinToString(" ") { OcrFixes.digits(it.text.trim()) }
                val union = Rect()
                loose.forEach { l -> l.boundingBox?.let { union.union(it) } }
                val conf = loose.map { if (it.confidence > 0f) it.confidence else 0.8f }.average().toFloat()
                comps += Component(Kind.TEXT, joined, null, conf, if (union.isEmpty) null else union)
            }
        }

        val raw = OcrFixes.digits(text.text.trim())
        val (type, conf, runnerUp) = predictType(raw, comps)
        return Analysis(type, conf, runnerUp, comps, raw)
    }

    // ---------- component prediction ----------
    private fun classifyLine(t: String, box: Rect?, conf: Float): List<Component> {
        // "Label: value" lines
        FIELD.matchEntire(t)?.let { m ->
            val label = m.groupValues[1].trim()
            val value = m.groupValues[2].trim()
            if (value.isNotEmpty()) {
                return listOf(Component(valueKind(label, value), value, label, conf, box))
            }
        }

        // Receipt-style rows: "Milk 2x ..... 45.00"
        val amounts = AMOUNT.findAll(t).toList()
        if (amounts.isNotEmpty()) {
            val before = t.substring(0, amounts.first().range.first)
            if (before.count { it.isLetter() } >= 3) {
                val key = TOTAL_WORDS.find(before)?.value
                return if (key != null) {
                    listOf(
                        Component(
                            Kind.AMOUNT, amounts.last().value.trim(),
                            key.replaceFirstChar { it.uppercase() }, conf, box
                        )
                    )
                } else {
                    listOf(Component(Kind.TABLE_ROW, t, null, conf, box))
                }
            }
        }

        // Standalone entities in the line
        val out = mutableListOf<Component>()
        val used = mutableListOf<IntRange>()
        fun add(kind: Kind, re: Regex) {
            re.findAll(t).forEach { m ->
                if (used.none { it.first <= m.range.last && m.range.first <= it.last }) {
                    used += m.range
                    out += Component(kind, m.value.trim(), null, conf, box)
                }
            }
        }
        add(Kind.EMAIL, EMAIL)
        add(Kind.URL, URL)
        add(Kind.DATE, DATE)
        add(Kind.TIME, TIME)
        add(Kind.PHONE, PHONE)
        add(Kind.AMOUNT, AMOUNT)
        ID_INLINE.find(t)?.let { m ->
            val id = m.groupValues[1]
            if (id.any { it.isDigit() } && used.none { it.first <= m.range.last && m.range.first <= it.last }) {
                out += Component(Kind.ID_NUMBER, id, null, conf, box)
            }
        }
        return out
    }

    private fun valueKind(label: String, value: String): Kind = when {
        ID_LABEL.containsMatchIn(label) && value.length >= 5 && value.any { it.isDigit() } &&
            !DATE.containsMatchIn(value) -> Kind.ID_NUMBER
        EMAIL.containsMatchIn(value) -> Kind.EMAIL
        URL.containsMatchIn(value) -> Kind.URL
        DATE.containsMatchIn(value) -> Kind.DATE
        TIME.containsMatchIn(value) -> Kind.TIME
        AMOUNT.containsMatchIn(value) -> Kind.AMOUNT
        PHONE.containsMatchIn(value) -> Kind.PHONE
        else -> Kind.FIELD
    }

    // ---------- document-type classifier ----------
    private val LEXICON: Map<DocType, Map<String, Float>> = mapOf(
        DocType.RECEIPT to mapOf(
            "official receipt" to 3f, "receipt" to 2f, "cashier" to 2f, "change" to 1f, "cash" to 1f,
            "vat" to 1.5f, "total" to 1.2f, "subtotal" to 1.5f, "thank you" to 1.5f, "qty" to 1f,
            "tendered" to 2f, "terminal" to 1f, "branch" to .8f, "tin" to 1f, "salamat" to 1f,
            "kabuuan" to 1.5f, "sukli" to 1.5f, "resibo" to 2f
        ),
        DocType.INVOICE to mapOf(
            "invoice" to 3f, "bill to" to 3f, "due date" to 2.5f, "amount due" to 2.5f,
            "statement of account" to 2.5f, "billing" to 2f, "balance" to 1f, "payment" to 1f,
            "account no" to 1.5f, "purchase order" to 2f, "unit price" to 1.5f, "billed" to 1.5f
        ),
        DocType.ID_CARD to mapOf(
            "republic of the philippines" to 3f, "id no" to 2.5f, "date of birth" to 2.5f,
            "birth" to 1.5f, "valid until" to 2f, "expiry" to 2f, "certificate" to 2.5f,
            "this is to certify" to 3f, "issued" to 1.5f, "license" to 2f, "nationality" to 2f,
            "blood type" to 2f, "student no" to 2f, "holder" to 1.5f, "awarded" to 1.5f
        ),
        DocType.FORM to mapOf(
            "applicant" to 2f, "application" to 2f, "please fill" to 2.5f, "date signed" to 2f,
            "check one" to 2f, "form" to 1.5f, "civil status" to 2f, "birthdate" to 1.5f,
            "contact no" to 1.5f, "signature" to 1f, "address" to .8f, "name" to .6f,
            "pangalan" to 1.5f, "tirahan" to 1.5f, "lagda" to 1.5f, "kindly" to .8f
        ),
        DocType.LETTER to mapOf(
            "dear" to 3f, "sincerely" to 3f, "regards" to 2.5f, "respectfully" to 3f,
            "yours truly" to 3f, "subject" to 1.5f, "memorandum" to 3f, "memo" to 2f,
            "re:" to 1.2f, "attached" to 1f, "hereby" to 1.5f, "cc:" to 1.5f,
            "we are pleased" to 2f, "mahal na" to 2f, "gumagalang" to 2.5f
        ),
        DocType.SCHEDULE to mapOf(
            "schedule" to 3f, "agenda" to 3f, "calendar" to 2.5f, "monday" to 1.2f,
            "tuesday" to 1.2f, "wednesday" to 1.2f, "thursday" to 1.2f, "friday" to 1.2f,
            "room" to 1.2f, "section" to 1f, "class" to 1f, "week" to 1f, "time" to .8f
        ),
        DocType.NOTE to mapOf(
            "notes" to 2f, "note" to 1f, "todo" to 2f, "to do" to 2f, "reminder" to 2f,
            "ideas" to 1.5f, "remember" to 1.5f, "buy" to 1.2f
        )
    )

    private fun predictType(raw: String, comps: List<Component>): Triple<DocType, Float, DocType?> {
        if (raw.isBlank()) return Triple(DocType.NOTE, 0.2f, null)
        val low = raw.lowercase()
        val scores = DocType.entries.associateWith { 0.0 }.toMutableMap()

        for ((type, words) in LEXICON) {
            for ((w, weight) in words) if (low.contains(w)) scores[type] = scores.getValue(type) + weight
        }

        val amounts = comps.count { it.kind == Kind.AMOUNT }
        val rows = comps.count { it.kind == Kind.TABLE_ROW }
        val fields = comps.count { it.kind == Kind.FIELD }
        val times = comps.count { it.kind == Kind.TIME }

        scores[DocType.RECEIPT] = scores.getValue(DocType.RECEIPT) + min(rows, 6) * 0.5 + min(amounts, 4) * 0.4
        scores[DocType.INVOICE] = scores.getValue(DocType.INVOICE) + min(amounts, 4) * 0.2
        scores[DocType.FORM] = scores.getValue(DocType.FORM) + min(fields, 6) * 0.45
        if (comps.any { it.kind == Kind.ID_NUMBER }) {
            scores[DocType.ID_CARD] = scores.getValue(DocType.ID_CARD) + 1.0
        }
        scores[DocType.SCHEDULE] = scores.getValue(DocType.SCHEDULE) + min(times, 5) * 0.35
        scores[DocType.LETTER] = scores.getValue(DocType.LETTER) +
            if (comps.count { it.kind == Kind.TEXT } >= 2 && rows == 0) 0.8 else 0.0
        scores[DocType.NOTE] = scores.getValue(DocType.NOTE) + 0.8 // prior

        val best = scores.maxByOrNull { it.value }!!
        if (best.value < 1.5) return Triple(DocType.NOTE, 0.35f, null)

        // softmax over scores → confidence
        val temp = 1.4
        val exps = scores.mapValues { exp((it.value - best.value) / temp) }
        val sum = exps.values.sum()
        val ranked = exps.entries.sortedByDescending { it.value }
        val conf = (ranked[0].value / sum).toFloat().coerceIn(0.3f, 0.99f)
        val runner = ranked.getOrNull(1)?.takeIf { it.value / sum > 0.12 }?.key
        return Triple(best.key, conf, runner)
    }
}
