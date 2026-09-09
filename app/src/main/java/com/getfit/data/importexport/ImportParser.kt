package com.getfit.data.importexport

import java.text.SimpleDateFormat
import java.util.Locale

/** Which app's export shape was detected — informational (shown to the user), not required for
 *  correctness: parsing itself matches column NAMES rather than fixed positions, since this session
 *  has no way to verify real sample exports from these apps and hardcoded column order is the
 *  fragile approach. */
enum class SourceFormat(val label: String) {
    HEVY("Hevy"), STRONG("Strong"), FITNOTES("FitNotes"), FORGE("Forge"), GENERIC("a workout log"),
}

/** One flattened (session, exercise, set) row parsed from an imported CSV, before exercise matching. */
data class ParsedSetRow(
    val sessionKey: String,
    val sessionName: String,
    val dateMs: Long,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
)

data class ParsedImport(val format: SourceFormat, val rows: List<ParsedSetRow>, val skippedRows: Int)

private val DATE_PATTERNS = listOf(
    "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
    "dd MMM yyyy, HH:mm", "dd MMM yyyy HH:mm", "dd MMM yyyy",
    "MMM dd, yyyy, h:mm a", "MMM dd, yyyy h:mm a", "MMM dd, yyyy",
    "MM/dd/yyyy HH:mm", "MM/dd/yyyy", "dd/MM/yyyy HH:mm", "dd/MM/yyyy",
)

/** Tries a battery of common export date formats; falls back to [fallback] (never crashes/rejects
 *  a whole file over one unparseable date). */
fun parseFlexibleDate(raw: String, fallback: Long): Long {
    val s = raw.trim()
    if (s.isEmpty()) return fallback
    for (p in DATE_PATTERNS) {
        try {
            val fmt = SimpleDateFormat(p, Locale.US)
            fmt.isLenient = false
            val parsed = fmt.parse(s) ?: continue
            return parsed.time
        } catch (_: Exception) { /* try next pattern */ }
    }
    return fallback
}

private fun findCol(header: List<String>, vararg keywords: String): Int {
    val lower = header.map { it.trim().lowercase() }
    for (kw in keywords) { val i = lower.indexOf(kw); if (i >= 0) return i }
    for (kw in keywords) { val i = lower.indexOfFirst { it.contains(kw) }; if (i >= 0) return i }
    return -1
}

private fun detectFormat(header: List<String>): SourceFormat {
    val lower = header.map { it.trim().lowercase() }
    return when {
        lower.any { it == "exercise_title" } || (lower.contains("start_time") && lower.any { it == "weight_kg" }) -> SourceFormat.HEVY
        lower.any { it.contains("set order") || it == "set_order" } -> SourceFormat.STRONG
        lower.any { it == "category" } && lower.any { it.contains("weight unit") } && lower.none { it.contains("workout") } -> SourceFormat.FITNOTES
        lower.any { it.contains("workout name") } && lower.any { it.contains("exercise name") } && lower.any { it.contains("weight unit") } -> SourceFormat.FORGE
        else -> SourceFormat.GENERIC
    }
}

/**
 * Parses an exported workout CSV (Hevy, Strong, FitNotes, Forge-native, or a close-enough generic
 * layout) into flat set rows. Column NAMES are matched (case-insensitively, by keyword), not fixed
 * positions — real exports vary in column order and add extra columns over time, and this is the
 * defensible approach given no verified sample files. A row is kept only if it has a recognizable
 * exercise name and a positive rep count; everything else is counted as skipped rather than
 * crashing the whole import.
 */
fun parseImportCsv(text: String): ParsedImport {
    val table = Csv.parse(text)
    if (table.size < 2) return ParsedImport(SourceFormat.GENERIC, emptyList(), 0)

    val header = table[0]
    val format = detectFormat(header)

    val dateCol = findCol(header, "start_time", "start time", "date", "timestamp")
    val nameCol = findCol(header, "workout name", "title", "workout_title", "routine")
    val exCol = findCol(header, "exercise_title", "exercise name", "exercise")
    val weightCol = findCol(header, "weight_kg", "weight")
    val unitCol = findCol(header, "weight_unit", "weight unit")
    val repsCol = findCol(header, "reps", "rep")

    if (exCol < 0 || repsCol < 0) return ParsedImport(format, emptyList(), 0)

    val fallbackNow = System.currentTimeMillis()
    val rows = mutableListOf<ParsedSetRow>()
    var skipped = 0

    for (r in table.drop(1)) {
        if (r.size <= exCol) { skipped++; continue }
        val exName = r.getOrNull(exCol)?.trim().orEmpty()
        if (exName.isEmpty()) { skipped++; continue }

        val repsRaw = r.getOrNull(repsCol)?.trim().orEmpty()
        val reps = repsRaw.toDoubleOrNull()?.let { Math.round(it).toInt() }
        if (reps == null || reps <= 0) { skipped++; continue }

        val weightRaw = weightCol.takeIf { it >= 0 }?.let { r.getOrNull(it)?.trim() }.orEmpty()
        val weightVal = weightRaw.toDoubleOrNull() ?: 0.0
        val unitRaw = unitCol.takeIf { it >= 0 }?.let { r.getOrNull(it)?.trim()?.lowercase() }.orEmpty()
        val looksKgColumn = weightCol >= 0 && header[weightCol].trim().lowercase().contains("kg")
        val weightKg = when {
            unitRaw.contains("lb") -> weightVal * 0.45359237
            unitRaw.contains("kg") -> weightVal
            looksKgColumn -> weightVal
            else -> weightVal // best effort: assume kg when no unit hint is present anywhere
        }

        val dateRaw = dateCol.takeIf { it >= 0 }?.let { r.getOrNull(it)?.trim() }.orEmpty()
        val dateMs = parseFlexibleDate(dateRaw, fallbackNow)

        val sessionName = nameCol.takeIf { it >= 0 }?.let { r.getOrNull(it)?.trim() }?.takeIf { it.isNotEmpty() }
            ?: "Imported workout"

        // Rows sharing (workout name, raw date) — or just the raw date when there's no name column,
        // as with FitNotes — belong to the same session.
        val sessionKey = when {
            nameCol >= 0 && dateCol >= 0 -> "$sessionName|${r.getOrNull(dateCol).orEmpty()}"
            dateCol >= 0 -> r.getOrNull(dateCol).orEmpty().ifEmpty { "single" }
            else -> "single"
        }

        rows.add(ParsedSetRow(sessionKey, sessionName, dateMs, exName, weightKg, reps))
    }

    return ParsedImport(format, rows, skipped)
}
