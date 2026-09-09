package com.getfit.data.importexport

/**
 * Minimal RFC4180-ish CSV reader/writer — handles quoted fields with embedded commas, quotes and
 * newlines. Deliberately hand-rolled instead of a naive `.split(",")`: real workout-app exports
 * (Strong, Hevy) quote exercise/workout names that can contain commas.
 */
object Csv {

    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var touched = false
        var i = 0
        val n = text.length

        fun endField() { row.add(field.toString()); field.setLength(0) }
        fun endRow() { endField(); rows.add(row); row = mutableListOf(); touched = false }

        while (i < n) {
            val c = text[i]
            if (inQuotes) {
                touched = true
                if (c == '"') {
                    if (i + 1 < n && text[i + 1] == '"') { field.append('"'); i++ } else inQuotes = false
                } else field.append(c)
            } else {
                when (c) {
                    '"' -> { inQuotes = true; touched = true }
                    ',' -> { endField(); touched = true }
                    '\r' -> {}
                    '\n' -> endRow()
                    else -> { field.append(c); touched = true }
                }
            }
            i++
        }
        if (touched || field.isNotEmpty()) endRow()

        return rows.filter { r -> r.any { it.isNotBlank() } }
    }

    fun writeField(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    fun writeRow(fields: List<String>): String = fields.joinToString(",") { writeField(it) }
}
