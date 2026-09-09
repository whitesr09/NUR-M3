package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.NurPreferences

/** Keeps every existing Appearance Studio control while exposing font and glass studios. */
@Composable
fun AppearanceStudio14(prefs: NurPreferences, model: NurViewModel, refreshStatus: String, navigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = NurDesign.pagePadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NurFontSettingsLink(prefs) { navigate("fonts") }
            OutlinedButton(
                onClick = { navigate("glass") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.BlurOn, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Glass & depth · glassmorphism / liquid glass")
            }
        }
        Box(Modifier.weight(1f)) {
            AppearanceStudio(prefs, model, refreshStatus)
        }
    }
}
