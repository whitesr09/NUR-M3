package com.nshd.nurm3.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.nshd.nurm3.MainActivity
import com.nshd.nurm3.R
import com.nshd.nurm3.data.*
import com.nshd.nurm3.ui.DailyProgress
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

open class NurWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED
        ) {
            refresh(context)
        }
    }

    companion object {
        const val EXTRA_ROUTE = "com.nshd.nurm3.ROUTE"

        private val providers = listOf(
            NurWidgetProvider::class.java,
            NurPrayerWidgetProvider::class.java,
            NurTaskWidgetProvider::class.java,
            NurQuickWidgetProvider::class.java
        )
        private val widgetDateFormatter = DateTimeFormatter.ofPattern("d MMM")
        private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val refreshLock = Any()
        private var refreshJob: Job? = null

        /**
         * Coalesce bursts of Room updates into a single widget refresh. Rapid task/prayer taps used
         * to start several independent database reads and RemoteViews updates at once.
         */
        fun refresh(context: Context) {
            val app = context.applicationContext
            synchronized(refreshLock) {
                refreshJob?.cancel()
                refreshJob = refreshScope.launch {
                    delay(140L)
                    runCatching { refreshNow(app) }
                }
            }
        }

        suspend fun refreshNow(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val targets = providers.mapNotNull { provider ->
                val ids = manager.getAppWidgetIds(ComponentName(context, provider))
                if (ids.isEmpty()) null else provider to ids
            }

            // Most users have no home-screen widget. Avoid opening/querying Room in that case.
            if (targets.isEmpty()) return

            val database = NurDatabase.get(context)
            val entries = database.dao().getAllEntries().filterNot { it.archived }
            val completions = database.dao().getAllCompletions()
            val today = LocalDate.now()
            val prefs = NurSettings(context).preferences.first()

            for ((provider, ids) in targets) {
                val type = when (provider) {
                    NurPrayerWidgetProvider::class.java -> "prayer"
                    NurTaskWidgetProvider::class.java -> "task"
                    NurQuickWidgetProvider::class.java -> "quick"
                    else -> "daily"
                }
                ids.forEach { updateWidget(context, manager, it, type, entries, completions, today, prefs) }
            }
        }

        private fun shortcut(context: Context, widgetId: Int, action: Int, route: String): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_ROUTE, route)
                data = android.net.Uri.parse("nur-m3://widget/$widgetId/$action")
            }
            return PendingIntent.getActivity(
                context,
                widgetId * 10 + action,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            id: Int,
            type: String,
            entries: List<Entry>,
            completions: List<Completion>,
            today: LocalDate,
            prefs: NurPreferences
        ) {
            val active = entries.filter { EntrySchedule.isActive(it, today) }
            val selected = when (type) {
                "prayer" -> active.filter { it.kind == NurKind.PRAYER }
                "task" -> active.filter { it.kind == NurKind.AMANAH }
                else -> active
            }
            val summary = if (type == "daily" || type == "quick") {
                DailyProgress.lightSummary(entries, completions, today)
            } else {
                DailyProgress.summary(selected, completions, today)
            }
            val title = when (type) {
                "prayer" -> "Prayer"
                "task" -> "Amanah"
                "quick" -> "Quick actions"
                else -> "نُور"
            }
            val subtitle = when (type) {
                "prayer" -> "${summary.completed} of ${summary.total} prayers checked"
                "task" -> "${summary.completed} of ${summary.total} tasks completed"
                "quick" -> "Open your daily tools"
                else -> "Daily Light · ${summary.completed} of ${summary.total} completed"
            }

            val views = RemoteViews(context.packageName, R.layout.nur_daily_widget).apply {
                setTextViewText(R.id.widget_title, title)
                setTextViewText(R.id.widget_date, today.format(widgetDateFormatter))
                setTextViewText(
                    R.id.widget_subtitle,
                    if (prefs.widgetsEnabled) subtitle else "Widgets are disabled in NUR settings"
                )
                setProgressBar(
                    R.id.widget_progress,
                    100,
                    (summary.fraction * 100).toInt().coerceIn(0, 100),
                    false
                )
                setViewVisibility(R.id.widget_actions, if (prefs.widgetsEnabled) View.VISIBLE else View.GONE)
                setOnClickPendingIntent(R.id.widget_root, shortcut(context, id, 0, "journey"))
                setTextViewText(R.id.widget_action_one, if (type == "prayer") "Prayers" else "Add task")
                setOnClickPendingIntent(
                    R.id.widget_action_one,
                    shortcut(context, id, 1, if (type == "prayer") "journey" else "amanah?create=true")
                )
                setOnClickPendingIntent(R.id.widget_action_two, shortcut(context, id, 2, "dhikr"))
                setOnClickPendingIntent(R.id.widget_action_three, shortcut(context, id, 3, "focus"))
                setContentDescription(R.id.widget_root, "$title. $subtitle")
            }
            manager.updateAppWidget(id, views)
        }
    }
}

class NurPrayerWidgetProvider : NurWidgetProvider()
class NurTaskWidgetProvider : NurWidgetProvider()
class NurQuickWidgetProvider : NurWidgetProvider()

/** No private data is accepted through external broadcasts. */
class NurWidgetRefreshReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NurWidgetProvider.refresh(context)
    }
}
