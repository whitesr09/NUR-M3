package com.nshd.nurm3.ui

import android.content.ActivityNotFoundException
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.QuranReflection
import com.nshd.nurm3.data.ReflectionLibrary
import kotlinx.coroutines.launch

/** Offline curated reading. Editorial summaries are never presented as literal translations. */
@Composable
fun ReflectionScreen(initialVerseId: String? = null) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable(initialVerseId) { mutableStateOf(initialVerseId?.takeIf { ReflectionLibrary.find(it) != null }) }
    val results = remember(query) { ReflectionLibrary.search(query) }
    val listState = rememberLazyListState()
    val reduceMotion = LocalNurReduceMotion.current
    LaunchedEffect(initialVerseId, reduceMotion) {
        val index = ReflectionLibrary.items.indexOfFirst { it.id == initialVerseId }
        if (index >= 0) {
            query = ""
            selected = initialVerseId
            // Heading and search are two preceding lazy-list items.
            if (reduceMotion) listState.scrollToItem(index + 2) else listState.animateScrollToItem(index + 2)
        }
    }
    LazyColumn(state = listState, contentPadding = PaddingValues(NurDesign.pagePadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { NurPageHeading("Read and reflect", "Quran Reflections", "A quiet reading collection, available offline.") }
        item {
            NurPanel(Modifier.fillMaxWidth()) {
                Text("Explore the collection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search by reference or meaning") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) ({ IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = "Clear search") } }) else null,
                    shape = MaterialTheme.shapes.medium
                )
                Text("${results.size} passages • Offline", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (results.isEmpty()) item {
            NurEmptyState(Icons.Default.SearchOff, "No matching passages", "Try a different word or reference. The offline collection is unchanged.", "Clear search") { query = "" }
        }
        items(results, key = { it.id }) { reflection ->
            ReflectionCard(reflection, selected == reflection.id) {
                selected = if (selected == reflection.id) null else reflection.id
            }
        }
        item {
            Text("English meanings are brief editorial summaries, not literal translations or religious rulings. Open the source for the full verse and context.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReflectionCard(reflection: QuranReflection, expanded: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val reduceMotion = LocalNurReduceMotion.current
    val snackbar = LocalNurSnackbarHost.current
    val scope = rememberCoroutineScope()
    val openSource: () -> Unit = {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(reflection.sourceUrl))) }
        catch (_: ActivityNotFoundException) { scope.launch { snackbar?.showSnackbar("No browser is available to open this source.") } }
        catch (_: SecurityException) { scope.launch { snackbar?.showSnackbar("The source could not be opened.") } }
    }
    NurPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(reflection.reference.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(reflection.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(48.dp)) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (expanded) "Collapse ${reflection.reference}" else "Read ${reflection.reference}")
            }
        }
        Text(reflection.meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (reduceMotion) {
            if (expanded) ReflectionDetails(reflection, openSource)
        } else {
            AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                ReflectionDetails(reflection, openSource)
            }
        }
    }
}

@Composable
private fun ReflectionDetails(reflection: QuranReflection, onSource: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text("ARABIC TEXT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text(
            reflection.arabic,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = reflection.arabic },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Right,
            lineHeight = MaterialTheme.typography.headlineSmall.lineHeight * 1.65f
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Meaning summary", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(reflection.meaning, style = MaterialTheme.typography.bodyLarge)
        }
        OutlinedButton(onClick = onSource, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) {
            Icon(Icons.Default.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Read full verse and context")
        }
    }
}
