package com.getfit.core

/**
 * MEDIA CONFIG — the exercise demo GIFs are NOT in the dataset (only a media_id).
 * ExerciseDB's CDN blocks hotlinking and ownership is disputed, so hosting is deferred.
 * Point MEDIA_BASE at wherever the GIFs are self-hosted; each demo then loads {media_id}.gif.
 * Leave "" to fall back to the animated-icon placeholder. Flipping this needs no other code change.
 */
object Constants {
    // Exercise demo GIFs. Our dataset's media_id is the ExerciseDB v1 media code (e.g. squat=qXTaZnJ).
    // The official host (static.exercisedb.dev) is dead (NXDOMAIN), so we serve the same GIFs from a
    // community mirror (andresmonc/LogWell) via the free jsdelivr CDN — no key, no server. Pinned to a
    // commit SHA so it's immutable and cached permanently by jsdelivr (survives repo changes).
    //
    // Loads {MEDIA_BASE}{media_id}.gif via Coil (coil-gif); shimmer while loading, animated-icon
    // fallback when a media_id is missing from the mirror. To self-host later, just change this base.
    // TODO(prod): self-host these GIFs (own bucket/CDN) instead of depending on a third-party repo.
    const val MEDIA_BASE = "https://cdn.jsdelivr.net/gh/andresmonc/LogWell@0ba87a3bd5006b1b08fe03e69782fb858def6445/assets/exercise-gif/"

    fun gifUrl(mediaId: String?): String =
        if (MEDIA_BASE.isNotBlank() && !mediaId.isNullOrBlank()) "$MEDIA_BASE$mediaId.gif" else ""
}
