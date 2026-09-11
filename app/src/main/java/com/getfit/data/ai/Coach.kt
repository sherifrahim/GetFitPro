package com.getfit.data.ai

/**
 * The AI coach: three focused asks on top of the same workout brief the review uses.
 *
 *  - LAST_WORKOUT: critique one session against its routine and the previous time each lift was done.
 *  - NEXT: what to train next (given muscle recovery and the routine rotation) and the next PR
 *    attempt per main lift.
 *  - GOALS: review the active targets — realistic? too easy? — and propose replacements.
 *
 * NEXT and GOALS end with a machine-readable TARGETS block so the app can offer "Set as target"
 * for what the coach proposes, instead of the user retyping numbers into the goal sheet.
 */
enum class CoachMode(val label: String, val blurb: String) {
    LAST_WORKOUT("Last workout", "An honest critique of your most recent session"),
    NEXT("What's next", "Which routine to do next and the PR attempts to go for"),
    GOALS("My goals", "Are the targets right? What should they be?"),
}

data class SuggestedTarget(val exerciseId: String, val value: Double, val weeks: Int)

const val COACH_MAX_TOKENS = 5000

private const val TARGETS_HEADER = "TARGETS:"

fun coachSystemPrompt(mode: CoachMode, units: String): String {
    val base =
        "You are the lifter's strength coach inside their Forge app: direct, specific, a little demanding, " +
            "never generic. Plain text only — no markdown, no headings, no bullet symbols other than '-', " +
            "no disclaimers, no praise for its own sake. Use the numbers you are given and say what to do. " +
            "Weights in the brief are in $units.\n\n"
    val task = when (mode) {
        CoachMode.LAST_WORKOUT ->
            "Critique the session marked 'Session under review'. Compare each exercise's top set with the " +
                "previous time it was done (the 'vs last' notes are pre-computed facts), judge whether the " +
                "reps and load matched the routine's targets, comment on pacing (duration, sets) and, if heart " +
                "rate is present, on effort. Say what was good in one or two lines, then what to change next " +
                "time this routine comes round, with exact weights or reps. Keep it under 250 words."
        CoachMode.NEXT ->
            "Decide what the lifter should train next. The 'Recovery' section is a pre-computed readiness " +
                "estimate per muscle group and the 'Routines' section lists what they can pick from; the " +
                "rotation currently proposes one of them — agree or override it and say why in a sentence. " +
                "Then give the next PR attempt for up to three main lifts: the exact weight x reps to go for " +
                "in the next session where that lift appears, grounded in the current bests and trends. " +
                "Keep it under 250 words, then finish with the TARGETS block."
        CoachMode.GOALS ->
            "Review the active targets against the bests and trends: which are on track, which are " +
                "unrealistic for their deadline, which are too easy. If there are no targets, say what " +
                "the first two should be. Propose up to three concrete targets. Keep it under 220 words, then " +
                "finish with the TARGETS block."
    }
    val block = if (mode == CoachMode.LAST_WORKOUT) "" else
        "\n\nThe TARGETS block must be the very last thing in your answer, formatted exactly:\n" +
            "$TARGETS_HEADER\n- <exercise_id> | <target> | <weeks>\n" +
            "One line per target, at most three. exercise_id must be one of the ids listed under " +
            "'Targetable exercises'. <target> is a number: weight in $units for weighted lifts, reps for " +
            "bodyweight ones. <weeks> is an integer 2-16. Do not write anything after the block."
    return base + task + block
}

/** The user message: the shared workout summary plus the section(s) the mode needs. */
fun buildCoachBrief(mode: CoachMode, summary: String, sessionSection: String?, recoverySection: String?, targetable: String): String {
    val sb = StringBuilder()
    when (mode) {
        CoachMode.LAST_WORKOUT -> sessionSection?.let { sb.appendLine(it).appendLine() }
        CoachMode.NEXT -> recoverySection?.let { sb.appendLine(it).appendLine() }
        CoachMode.GOALS -> {}
    }
    sb.appendLine(summary)
    if (mode != CoachMode.LAST_WORKOUT) {
        sb.appendLine()
        sb.appendLine("Targetable exercises (id: name):")
        sb.append(targetable)
    }
    return sb.toString()
}

/** Splits the TARGETS block off the answer. The prose is what gets shown; the targets become chips. */
fun parseCoachAnswer(text: String, allowedIds: Set<String>): Pair<String, List<SuggestedTarget>> {
    val idx = text.lastIndexOf(TARGETS_HEADER)
    if (idx < 0) return text.trim() to emptyList()
    val prose = text.substring(0, idx).trim()
    val targets = text.substring(idx + TARGETS_HEADER.length).lines().mapNotNull { line ->
        val m = Regex("""^\s*-\s*([A-Za-z0-9_\-]+)\s*\|\s*([0-9]+(?:\.[0-9]+)?)\s*(?:[a-zA-Z]+)?\s*\|\s*([0-9]+)""").find(line) ?: return@mapNotNull null
        val id = m.groupValues[1]
        if (id !in allowedIds) return@mapNotNull null
        val value = m.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
        val weeks = m.groupValues[3].toIntOrNull()?.coerceIn(2, 16) ?: return@mapNotNull null
        if (value <= 0) return@mapNotNull null
        SuggestedTarget(id, value, weeks)
    }.distinctBy { it.exerciseId }.take(3)
    return prose to targets
}
