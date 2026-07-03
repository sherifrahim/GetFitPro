package com.getfit.core

/**
 * MEDIA CONFIG — the exercise demo GIFs are NOT in the dataset (only a media_id).
 * ExerciseDB's CDN blocks hotlinking and ownership is disputed, so hosting is deferred.
 * Point MEDIA_BASE at wherever the GIFs are self-hosted; each demo then loads {media_id}.gif.
 * Leave "" to fall back to the animated-icon placeholder. Flipping this needs no other code change.
 */
object Constants {
    // Confirmed gifUrl format from the free ExerciseDB v1 API (oss.exercisedb.dev/api/v1):
    // each exercise's gifUrl is "https://static.exercisedb.dev/media/{id}.gif" and that {id} is the
    // same code as our dataset's media_id. So this base + media_id resolves the right GIF.
    //
    // NOTE: as of 2026-07, static.exercisedb.dev is NXDOMAIN (their CDN is down), so demos fall back
    // to the animated-icon placeholder. When the host returns — or to point at a self-hosted mirror —
    // no other code changes: Coil loads {MEDIA_BASE}{media_id}.gif and shows shimmer/fallback otherwise.
    const val MEDIA_BASE = "https://static.exercisedb.dev/media/"

    fun gifUrl(mediaId: String?): String =
        if (MEDIA_BASE.isNotBlank() && !mediaId.isNullOrBlank()) "$MEDIA_BASE$mediaId.gif" else ""
}
