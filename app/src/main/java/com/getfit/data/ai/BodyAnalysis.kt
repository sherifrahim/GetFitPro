package com.getfit.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * Body check: the user's own progress photos, sent with their workout patterns, for a physique
 * assessment and concrete training changes.
 *
 * Privacy stance, enforced by construction rather than policy:
 *   * Photos are read from the system photo picker's URIs only when the user taps Analyze, encoded
 *     in memory, sent once, and dropped. Nothing is written to disk, Room, DataStore or the backup.
 *   * Only base64 of a downscaled JPEG leaves the device — never the original file, never EXIF
 *     (GPS, device model, timestamps are stripped by re-encoding).
 *   * The screen requires an explicit consent tap per session and states where the photos go.
 */

/** Which view a photo shows. Sent to the model as a label; ordering in the UI follows this enum. */
enum class PhotoView(val label: String) {
    FRONT("Front"), BACK("Back"), LEFT("Left side"), RIGHT("Right side"), DETAIL("Specific area")
}

enum class BodyGoal(val label: String, val prompt: String) {
    BUILD_MUSCLE("Build muscle", "build muscle and improve proportions"),
    LOSE_FAT("Lose fat", "lose body fat while keeping muscle"),
    GET_STRONGER("Get stronger", "get stronger on the main lifts"),
    GENERAL("General fitness", "improve general fitness, posture and balance"),
}

data class EncodedImage(val base64: String, val width: Int, val height: Int, val bytes: Int)

private const val SYSTEM_PROMPT =
    "You are an experienced strength and conditioning coach doing a physique check-in. The client " +
        "has shared their OWN photos, taken for this purpose, to plan their training. Be direct, " +
        "specific and respectful: describe what you see, never judge it.\n\n" +
        "Write in plain text with short paragraphs under these headings, in capitals:\n" +
        "WHAT I SEE — overall build; muscle development by region (shoulders, chest, upper back, " +
        "lats, arms, core, glutes, quads, hamstrings, calves) rated roughly as underdeveloped / " +
        "developing / well developed; any left-right or upper-lower imbalance; posture cues " +
        "(rounded shoulders, forward head, anterior pelvic tilt, uneven hips); an approximate " +
        "body-fat range ONLY if reasonably estimable, always as a range and clearly an estimate.\n" +
        "WHAT MATTERS FOR YOUR GOAL — given the stated goal and the training patterns, the 2-3 " +
        "things that would move the needle most.\n" +
        "CHANGES TO YOUR TRAINING — which muscle groups need more weekly sets and roughly how many; " +
        "4-8 specific exercises to add or emphasise and why; what to reduce; how to adjust the " +
        "weekly split given the days they actually train.\n" +
        "RECOVERY AND NUTRITION — one or two sentences of direction, not a plan.\n" +
        "LIMITS — one sentence on what the photos can't show (lighting, pose, clothing, no " +
        "measurements), so the client knows how much weight to put on this.\n\n" +
        "If a specific area is called out, address it first within each heading. If a photo is " +
        "unusable or isn't of a person, say so plainly and work with what remains. Reference the " +
        "training pattern facts you're given as facts. No medical diagnoses, no markdown, no filler."

/**
 * Assembles the user turn: labelled images first, then the request text and the workout context.
 * Images go before text, which is the layout the API handles best for multi-image prompts.
 */
fun buildBodyAnalysisContent(
    images: List<Pair<PhotoView, EncodedImage>>,
    goal: BodyGoal,
    focusArea: String,
    workoutSummary: String,
): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    images.forEachIndexed { i, (view, img) ->
        blocks += textBlock("Photo ${i + 1} of ${images.size}: ${view.label} view.")
        blocks += imageBlock(img.base64)
    }
    val request = buildString {
        appendLine("Goal: ${goal.prompt}.")
        if (focusArea.isNotBlank()) appendLine("Specific area to look at: ${focusArea.trim()}.")
        appendLine()
        appendLine("Their current training, from the app's log:")
        appendLine(workoutSummary)
    }
    blocks += textBlock(request)
    return blocks
}

fun bodyAnalysisSystemPrompt(): String = SYSTEM_PROMPT

/**
 * Reads a picker URI and produces a downscaled, EXIF-corrected, EXIF-stripped JPEG as base64.
 *
 * [maxEdge] of 1280 keeps each image around 1-1.5 MP, which is the sweet spot the API documents
 * for vision: larger images are downscaled server-side anyway and only cost more tokens and
 * upload time. Orientation is applied from EXIF because BitmapFactory ignores it — a portrait
 * phone photo would otherwise arrive sideways and the assessment would be garbage.
 */
object ImageEncoder {
    private const val TAG = "ImageEncoder"

    /** Failure carries the reason, so the UI can say *why* rather than a bare "couldn't read". */
    fun encode(context: Context, uri: Uri, maxEdge: Int = 1280): Result<EncodedImage> = runCatching {
        val resolver = context.contentResolver

        // Pass 1: bounds only, to pick a sample size that lands near maxEdge without a full decode.
        // NB: with inJustDecodeBounds the decode call returns null BY DESIGN — it only fills
        // outWidth/outHeight — so success is judged on those, never on the return value.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val probe = resolver.openInputStream(uri) ?: error("The photo picker didn't give access to that file.")
        probe.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("That file isn't a readable image.")
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2

        // Pass 2: real decode.
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("Couldn't decode that image.")

        // EXIF orientation, best-effort. It is read from a fresh stream (the decode consumed the
        // first), and it must never be allowed to fail the encode: the framework ExifInterface throws
        // on formats it doesn't parse (PNG on some API levels), and a missing rotation is a far
        // better outcome than no photo at all.
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrNull() ?: 0f

        // Final exact scale to maxEdge, plus rotation, in one matrix.
        val longest = maxOf(bmp.width, bmp.height).toFloat()
        val scale = if (longest > maxEdge) maxEdge / longest else 1f
        if (scale != 1f || rotation != 0f) {
            val m = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
            val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (out !== bmp) bmp.recycle()
            bmp = out
        }

        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val bytes = baos.toByteArray()
        val w = bmp.width; val h = bmp.height
        bmp.recycle()
        EncodedImage(Base64.encodeToString(bytes, Base64.NO_WRAP), w, h, bytes.size)
    }.onFailure { Log.w(TAG, "encode failed for $uri", it) }
}
