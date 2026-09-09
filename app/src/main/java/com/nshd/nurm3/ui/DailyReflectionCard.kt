package com.nshd.nurm3.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nshd.nurm3.data.QuranReflection
import com.nshd.nurm3.data.ReflectionLibrary
import java.time.LocalDate
import kotlinx.coroutines.delay

/** A dated, offline reflection carousel. Reading never changes completion records. */
@Composable
fun DailyReflectionCard(date: LocalDate, onRead: (String) -> Unit) {
    val reduceMotion = LocalNurReduceMotion.current
    val owner = LocalLifecycleOwner.current
    var offset by rememberSaveable(date.toString()) { mutableIntStateOf(0) }
    var automatic by rememberSaveable { mutableStateOf(false) }
    val reflection = ReflectionLibrary.at(date, offset)
    LaunchedEffect(date, automatic, owner) {
        if (automatic) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(10_000)
                offset = (offset + 1) % ReflectionLibrary.items.size
            }
        }
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(if (LocalNurCompact.current) 12.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("A moment of reflection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Quran • Offline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { automatic = !automatic }) {
                    Icon(if (automatic) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (automatic) "Pause automatic reflections" else "Play reflections every 10 seconds")
                }
            }
            if (reduceMotion) ReflectionPreview(reflection) else Crossfade(targetState = reflection, animationSpec = tween(240), label = "Reflection change") { ReflectionPreview(it) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { offset = Math.floorMod(offset - 1, ReflectionLibrary.items.size) }) { Icon(Icons.Default.ChevronLeft, "Previous reflection") }
                Text("${Math.floorMod(date.toEpochDay() + offset, ReflectionLibrary.items.size.toLong()) + 1} / ${ReflectionLibrary.items.size}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { offset = (offset + 1) % ReflectionLibrary.items.size }) { Icon(Icons.Default.ChevronRight, "Next reflection") }
                TextButton(onClick = { onRead(reflection.id) }) { Text("Read") }
            }
        }
    }
}

@Composable
private fun ReflectionPreview(reflection: QuranReflection) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(reflection.title, style = MaterialTheme.typography.titleSmall)
        Text(reflection.meaning, style = MaterialTheme.typography.bodyMedium)
        Text(reflection.reference + " • Meaning summary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
