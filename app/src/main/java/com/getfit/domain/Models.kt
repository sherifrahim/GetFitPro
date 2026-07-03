package com.getfit.domain

/** A single logged working set. */
data class LoggedSet(val exerciseId: String, val weight: Double, val reps: Int, val dateMs: Long)

/** Derived personal best for an exercise. */
data class Best(
    val bodyweight: Boolean,
    val weight: Double,
    val reps: Int,
    val e1rm: Int,
    val dateMs: Long,
)

/** A completed workout session (history record). */
data class SessionRecord(
    val id: String,
    val dateMs: Long,
    val name: String,
    val durationSec: Int,
    val totalSets: Int,
    val volume: Int,
    val prs: Int,
)
