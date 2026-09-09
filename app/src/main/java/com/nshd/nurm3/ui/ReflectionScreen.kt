package com.nshd.nurm3.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.QuranReflection
import com.nshd.nurm3.data.ReflectionLibrary

/** Read-only curated content. No generated quotations or remote content are displayed. */
@Composable
fun ReflectionScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val results = remember(query) { ReflectionLibrary.search(query) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Quran Reflections", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("A small curated collection for quiet reading. Available offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search reflections") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("${results.size} reflections", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
        if (results.isEmpty()) item { Text("No matching reflections. Try another word or reference.") }
        items(results, key = { it.id }) { reflection ->
            ReflectionCard(reflection, selected == reflection.id) { selected = if (selected == reflection.id) null else reflection.id }
        }
        item { Text("English meanings are brief editorial summaries, not literal translations or religious rulings. Open the source for the full verse and context.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun ReflectionCard(reflection: QuranReflection, expanded: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val reduceMotion = LocalNurReduceMotion.current
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(reflection.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(reflection.reference, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onToggle) { Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Collapse ${reflection.reference}" else "Read ${reflection.reference}") }
            }
            Text(reflection.meaning, style = MaterialTheme.typography.bodyMedium)
            if (reduceMotion) {
                if (expanded) ReflectionDetails(reflection, context)
            } else {
                AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    ReflectionDetails(reflection, context)
                }
            }
        }
    }
}

@Composable
private fun ReflectionDetails(reflection: QuranReflection, context: android.content.Context) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider()
        Text(reflection.arabic, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Right, lineHeight = MaterialTheme.typography.headlineSmall.lineHeight * 1.5f)
        Text("Meaning summary", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(reflection.meaning, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reflection.sourceUrl))
            runCatching { context.startActivity(intent) }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Read full verse and context")
        }
    }
}
