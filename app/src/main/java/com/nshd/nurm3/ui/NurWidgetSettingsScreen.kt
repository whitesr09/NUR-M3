package com.nshd.nurm3.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.NurPreferences
import com.nshd.nurm3.widget.*

@Composable
fun NurWidgetSettingsScreen(prefs: NurPreferences, model: NurViewModel) {
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    var message by remember { mutableStateOf("") }
    val choices = listOf(
        Triple("Daily Light", "A quiet overview of today's recorded progress.", NurWidgetProvider::class.java),
        Triple("Prayer checklist", "The number of prayers you have actually checked today.", NurPrayerWidgetProvider::class.java),
        Triple("Amanah tasks", "Today's saved tasks and completion count.", NurTaskWidgetProvider::class.java),
        Triple("Quick actions", "Open task creation, Dhikr or the Focus timer.", NurQuickWidgetProvider::class.java)
    )
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { NurPageHeading("Outside the app", "Widgets & quick actions", "Keep useful parts of NUR close to your daily routine.") }
        item { NurSettingRow("Enable widgets", "Allow your home screen to show current NUR progress.", prefs.widgetsEnabled) { model.setting("widgets", it); NurWidgetProvider.refresh(context) } }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("Add a widget", style = MaterialTheme.typography.titleMedium)
                Text("Long-press an empty area of your Android home screen, choose Widgets, find NUR, and drag your preferred widget into place. Your launcher controls the available sizes and placement.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { NurWidgetProvider.refresh(context); message = "Widget refresh requested. Your launcher controls when the update appears." }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Refresh widgets") }
                if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        choices.forEach { (title, subtitle, provider) ->
            item {
                NurPanel(Modifier.fillMaxWidth()) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (Build.VERSION.SDK_INT >= 26 && manager.isRequestPinAppWidgetSupported) {
                        OutlinedButton(onClick = {
                            val accepted = manager.requestPinAppWidget(ComponentName(context, provider), null, null)
                            message = if (accepted) "Check your launcher to confirm the widget." else "Your launcher did not accept the pin request. Add the widget manually."
                        }, modifier = Modifier.fillMaxWidth(), enabled = prefs.widgetsEnabled) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add to home screen") }
                    }
                }
            }
        }
        item { Text("Widgets display aggregate progress by default. They do not show private journal or AI conversations, and they never create completion records. Exact refresh timing and appearance depend on your Android launcher.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
