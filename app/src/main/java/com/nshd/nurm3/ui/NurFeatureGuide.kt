package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class GuidePage(val title: String, val description: String)
private val guidePages = listOf(
    GuidePage("Your Daily Journey", "Check prayers and complete real tasks. Your saved entries remain after midnight; only the daily completion state changes."),
    GuidePage("Make NUR yours", "Choose a theme, optional glass styling, progress shapes and a custom font. Appearance does not change your records."),
    GuidePage("Focus & Routine", "Use work and rest presets. A Focus completion is recorded only after the actual interval finishes."),
    GuidePage("NUR AI", "AI is optional. Add your own Gemini key and explicitly enable network access. Verify religious sources and do not treat AI as a qualified scholar."),
    GuidePage("Your data stays yours", "Review backups before restoring. Use encrypted exports for private records. Keep your passphrase safe and test recovery before uninstalling."),
    GuidePage("Accessible by design", "Adjust text size, contrast, language and motion from Accessibility. Reopen this guide whenever you need it.")
)

@Composable
fun NurFeatureGuide(onDismiss: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(guidePages[page].title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(guidePages[page].description, style = MaterialTheme.typography.bodyLarge)
                LinearProgressIndicator(progress = { (page + 1).toFloat() / guidePages.size }, modifier = Modifier.fillMaxWidth())
                Text("${page + 1} / ${guidePages.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { if (page == guidePages.lastIndex) onDismiss() else page++ }) { Text(if (page == guidePages.lastIndex) "Finish" else "Next") } },
        dismissButton = { TextButton(onClick = { if (page == 0) onDismiss() else page-- }) { Text(if (page == 0) "Skip" else "Back") } }
    )
}
