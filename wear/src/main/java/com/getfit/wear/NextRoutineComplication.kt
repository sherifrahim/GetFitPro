package com.getfit.wear

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.getfit.wear.data.WatchStore

/**
 * Watch-face complication: "Up next · Push Day", tapping it opens the app and starts that routine
 * (on the phone if it answers, else on the wrist — the same path as the Idle screen's Start chip).
 *
 * Data comes from the routine cache the phone last sent (WatchStore), so the face shows the right
 * routine even with the phone away. There is no polling: ForgeListenerService pokes [refresh]
 * whenever a snapshot arrives, so a rotation advance on the phone updates the face within a second.
 */
class NextRoutineComplication : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, "Push Day", "r_push", preview = true)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val cache = WatchStore(this).routines()
        val current = cache.routines.firstOrNull { it.id == cache.currentRoutineId } ?: cache.routines.firstOrNull()
        return build(request.complicationType, current?.name ?: "Forge", current?.id.orEmpty(), preview = false)
    }

    private fun build(type: ComplicationType, name: String, routineId: String, preview: Boolean): ComplicationData? {
        val tap = if (preview) null else PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_START_ROUTINE, routineId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title: ComplicationText = PlainComplicationText.Builder("Up next").build()
        val desc: ComplicationText = PlainComplicationText.Builder("Start $name in Forge").build()
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(PlainComplicationText.Builder(name.take(7)).build(), desc)
                .setTitle(PlainComplicationText.Builder("Next").build())
                .setTapAction(tap)
                .build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(PlainComplicationText.Builder(name).build(), desc)
                .setTitle(title)
                .setTapAction(tap)
                .build()
            else -> null
        }
    }

    companion object {
        /** Ask every watch face showing this complication to re-request data. */
        fun refresh(context: Context) {
            runCatching {
                ComplicationDataSourceUpdateRequester
                    .create(context, ComponentName(context, NextRoutineComplication::class.java))
                    .requestUpdateAll()
            }
        }
    }
}
