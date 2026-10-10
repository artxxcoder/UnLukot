package com.unlukot.app.ai

/** Repairs common OCR letter/digit confusions inside number-like tokens (amounts, dates, IDs). */
object OcrFixes {

    private val CONFUSABLE = mapOf(
        'O' to '0', 'o' to '0', 'D' to '0', 'Q' to '0',
        'l' to '1', 'I' to '1', '|' to '1', 'i' to '1',
        'S' to '5', 's' to '5', 'B' to '8', 'Z' to '2', 'z' to '2'
    )
    private const val SEPARATORS = ".,:/-"

    fun digits(line: String): String {
        val out = StringBuilder(line.length)
        var i = 0
        while (i < line.length) {
            if (line[i].isWhitespace()) {
                out.append(line[i++])
                continue
            }
            var j = i
            while (j < line.length && !line[j].isWhitespace()) j++
            out.append(fixToken(line.substring(i, j)))
            i = j
        }
        return out.toString()
    }

    private fun fixToken(t: String): String {
        val digitCount = t.count { it.isDigit() }
        if (digitCount < 2) return t
        val letters = t.count { it.isLetter() }
        if (letters == 0 || letters > 2 || digitCount < letters * 2) return t
        val hasDecimal = t.any { it == '.' || it == ',' }
        val out = StringBuilder(t)
        for (i in t.indices) {
            val fix = CONFUSABLE[t[i]] ?: continue
            val before = t.getOrNull(i - 1)
            val after = t.getOrNull(i + 1)
            val numBefore = before != null && (before.isDigit() || before in SEPARATORS)
            val numAfter = after != null && (after.isDigit() || after in SEPARATORS)
            val trailingDecimal = after == null && hasDecimal && before?.isDigit() == true
            if ((numBefore && numAfter) || trailingDecimal) out.setCharAt(i, fix)
        }
        return out.toString()
    }
}
