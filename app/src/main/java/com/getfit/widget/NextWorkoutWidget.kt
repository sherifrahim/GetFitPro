package com.getfit.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.getfit.GetFitApp
import com.getfit.MainActivity
import com.getfit.R
import com.getfit.di.AppContainer
import com.getfit.domain.SessionRecord
import com.getfit.domain.floorDayLocal
import com.getfit.domain.streakCount
import com.getfit.domain.weekAgg
import com.getfit.domain.weekStartLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen widget: the routine up next, its size, the streak and this week's count, and a Start
 * pill that opens the app straight into that routine. Classic AppWidgetProvider + RemoteViews on
 * purpose — no Glance dependency for a single card.
 *
 * Data is read straight from the container (DataStore + Room) because the widget updates with no
 * activity or ViewModel alive. [refresh] is called by the app after anything that changes what the
 * card shows (a saved session, a routine change), plus the 30-minute system update as a backstop.
 */
class NextWorkoutWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val container = (context.applicationContext as? GetFitApp)?.container ?: return
        // goAsync keeps the receiver alive for the short DB read; the result is delivered from a
        // background coroutine and the receiver finished afterwards.
        val pending = goAsync()
        scope.launch {
            try {
                val views = build(context, container)
                ids.forEach { manager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        const val EXTRA_START_ROUTINE = "com.getfit.START_ROUTINE"

        /** Re-render every placed instance. Cheap when none are placed (the id list is empty). */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NextWorkoutWidget::class.java))
            if (ids.isEmpty()) return
            val container = (context.applicationContext as? GetFitApp)?.container ?: return
            scope.launch {
                val views = build(context, container)
                ids.forEach { manager.updateAppWidget(it, views) }
            }
        }

        private suspend fun build(context: Context, container: AppContainer): RemoteViews {
            val routines = container.routinesStore.flow.first()
            val settings = container.settingsStore.flow.first()
            val sessions = container.db.sessionDao().allOnce()
            val current = routines.current
            val now = System.currentTimeMillis()
            val records = sessions.map { SessionRecord(it.id, it.dateMs, it.name, it.durationSec, it.totalSets, it.volume, it.prs) }
            val week = weekAgg(records, weekStartLocal(now, settings.weekStartsMonday))
            val streak = streakCount(sessions.map { it.dateMs }, now, ::floorDayLocal)
            val est = current?.let { r -> Math.round(r.items.sumOf { it.sets * (45 + settings.restDefault) / 60.0 }).toInt() } ?: 0

            val views = RemoteViews(context.packageName, R.layout.widget_next)
            views.setTextViewText(R.id.widget_name, current?.name ?: "No routine yet")
            views.setTextViewText(
                R.id.widget_sub,
                current?.let { "${it.items.size} exercises · ${it.setsTotal} sets · ~$est min" } ?: "Open Forge to set one up",
            )
            views.setTextViewText(
                R.id.widget_streak,
                (if (streak > 0) "$streak-day streak · " else "") + "${week.workouts} of ${settings.weeklyGoal} this week",
            )

            // Whole card opens the app; the pill opens it AND starts the routine.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 0, open, flags))
            val start = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_START_ROUTINE, current?.id.orEmpty())
            views.setOnClickPendingIntent(R.id.widget_start, PendingIntent.getActivity(context, 1, start, flags))
            return views
        }
    }
}
