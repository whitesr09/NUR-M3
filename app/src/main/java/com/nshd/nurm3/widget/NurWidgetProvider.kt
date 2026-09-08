package com.nshd.nurm3.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.nshd.nurm3.MainActivity
import com.nshd.nurm3.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NurWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val intent = Intent(context, MainActivity::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)
            val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM"))
            val views = RemoteViews(context.packageName, R.layout.nur_daily_widget).apply {
                setTextViewText(R.id.widget_title, "نُور")
                setTextViewText(R.id.widget_date, today)
                setTextViewText(R.id.widget_subtitle, "Open NUR to update your Daily Journey")
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            }
            manager.updateAppWidget(id, views)
        }
    }
}
