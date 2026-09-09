package com.nshd.nurm3.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.R
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NurAccessibilityScreen(prefs: NurPreferences, model: NurViewModel) {
    val context = LocalContext.current
    val store = remember(context) { NurAccessibilityStore.get(context) }
    val config by store.settings.collectAsState()
    var languagePicker by remember { mutableStateOf(false) }
    var firstRun by remember { mutableStateOf(false) }
    val languages = listOf("system" to R.string.accessibility_system, "en" to R.string.accessibility_english, "ml" to R.string.accessibility_malayalam, "ar" to R.string.accessibility_arabic)
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { NurPageHeading("NUR", stringResource(R.string.accessibility_title), stringResource(R.string.accessibility_subtitle)) }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.accessibility_language), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { languagePicker = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Language, null); Spacer(Modifier.width(8.dp)); Text(stringResource(languages.first { it.first == config.language }.second)) }
                Text(stringResource(R.string.accessibility_review_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.accessibility_text_size), style = MaterialTheme.typography.titleMedium)
                Text("${(config.textScale * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                Slider(config.textScale, onValueChange = { store.update(config.copy(textScale = it)) }, valueRange = 1f..1.6f, steps = 5)
                Text(stringResource(R.string.accessibility_preview), style = MaterialTheme.typography.titleSmall)
                Text("نُور · A meaningful day, one step at a time.", style = MaterialTheme.typography.bodyLarge)
                Text("Text follows your system font scale as well as this additional NUR preference.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { NurSettingRow(stringResource(R.string.accessibility_contrast), "Use stronger text and surface contrast.", config.highContrast) { store.update(config.copy(highContrast = it)) } }
        item { NurSettingRow(stringResource(R.string.accessibility_motion), "Reduce decorative animation and transitions.", config.reduceMotion) { store.update(config.copy(reduceMotion = it)) } }
        item { NurSettingRow(stringResource(R.string.accessibility_hints), "Keep short explanations visible in new companion tools.", config.showHints) { store.update(config.copy(showHints = it)) } }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("Guided setup", style = MaterialTheme.typography.titleMedium)
                Text("Learn where to find your daily checklist, appearance options, private backups and new companion tools.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { firstRun = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Explore, null); Spacer(Modifier.width(8.dp)); Text("Open feature guide") }
            }
        }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("Accessibility review", style = MaterialTheme.typography.titleMedium)
                Text("Controls use semantic labels and accessible touch targets. Android TalkBack, font scaling and system settings remain supported. Full translation and physical-device accessibility review are still required before release.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (languagePicker) AlertDialog(onDismissRequest = { languagePicker = false }, title = { Text(stringResource(R.string.accessibility_language)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            languages.forEach { (id, label) ->
                TextButton(onClick = {
                    languagePicker = false
                    if (config.language != id) { store.update(config.copy(language = id)); (context as? Activity)?.recreate() }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(label)); if (config.language == id) { Spacer(Modifier.width(8.dp)); Icon(Icons.Default.Check, null) } }
            }
        }
    }, confirmButton = { TextButton(onClick = { languagePicker = false }) { Text("Cancel") } })
    if (firstRun) NurFeatureGuide(onDismiss = { firstRun = false })
}
