package com.nshd.nurm3.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.NurPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val fontOptions = listOf(
    Triple("default", "NUR default", "The balanced system typography"),
    Triple("sans", "Clean sans", "A clear, modern everyday style"),
    Triple("serif", "Editorial serif", "A more traditional reading style"),
    Triple("mono", "Monospace", "A precise, technical appearance"),
    Triple("rounded", "Expressive script", "A decorative cursive style")
)

@Composable
fun NurFontSettingsLink(prefs: NurPreferences, onClick: () -> Unit) {
    val label = if (prefs.fontStyle == "custom") prefs.customFontName.ifBlank { "Custom font" }
        else fontOptions.firstOrNull { it.first == prefs.fontStyle }?.second ?: "NUR default"
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.TextFields, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Font style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NurFontSettingsScreen(prefs: NurPreferences, model: NurViewModel) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null && !busy) scope.launch {
            busy = true
            try {
                val name = model.importFont(uri)
                message = "$name imported and selected."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message = error.message ?: "The font could not be imported." }
            finally { busy = false }
        }
    }
    LazyColumn(contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { NurPageHeading("Appearance", "Font library", "Choose a built-in style or import your own TTF or OTF font.") }
        item {
            NurPanel {
                Text("LIVE TYPOGRAPHY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("Make every day meaningful.", style = MaterialTheme.typography.headlineMedium)
                Text("The quick brown fox jumps over the lazy dog. 0123456789", style = MaterialTheme.typography.bodyMedium)
                Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", style = MaterialTheme.typography.titleMedium)
                Text("Arabic and other missing glyphs use Android font fallback. NUR's dedicated calligraphy remains separately styled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Text("Built-in styles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        fontOptions.forEach { (id, title, subtitle) ->
            item {
                Surface(onClick = { if (!busy) model.choice("font_style", id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = if (prefs.fontStyle == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (prefs.fontStyle == id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (prefs.fontStyle == id) Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            NurPanel {
                Text("Your own font", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Import a font from your phone. It is copied into NUR's private storage, so the original file can be moved afterward. Maximum size: 8 MB.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (prefs.customFontId.isNotBlank()) {
                    Text(prefs.customFontName.ifBlank { "Imported font" }, style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(onClick = { model.choice("font_style", "custom") }, enabled = !busy && prefs.fontStyle != "custom", modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (prefs.fontStyle == "custom") "Selected" else "Use imported font")
                    }
                }
                NurPrimaryAction(if (busy) "Importing…" else "Choose TTF / OTF file", onClick = {
                    launcher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream", "*/*"))
                }, modifier = Modifier.fillMaxWidth(), enabled = !busy, icon = { Icon(Icons.Default.UploadFile, null) })
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Text("An imported font changes the interface only. It is not included in JSON backups and is never uploaded to a server. Resetting appearance returns to the default font without deleting the imported file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
