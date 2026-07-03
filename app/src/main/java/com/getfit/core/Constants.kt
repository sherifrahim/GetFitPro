package com.getfit.core

/**
 * MEDIA CONFIG — the exercise demo GIFs are NOT in the dataset (only a media_id).
 * ExerciseDB's CDN blocks hotlinking and ownership is disputed, so hosting is deferred.
 * Point MEDIA_BASE at wherever the GIFs are self-hosted; each demo then loads {media_id}.gif.
 * Leave "" to fall back to the animated-icon placeholder. Flipping this needs no other code change.
 */
object Constants {
    const val MEDIA_BASE = "" // e.g. "https://your-cdn.com/exercises/"

    fun gifUrl(mediaId: String?): String =
        if (MEDIA_BASE.isNotBlank() && !mediaId.isNullOrBlank()) "$MEDIA_BASE$mediaId.gif" else ""
}
