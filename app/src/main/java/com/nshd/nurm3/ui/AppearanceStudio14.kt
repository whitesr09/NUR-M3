package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.NurPreferences

/** Keeps every existing Appearance Studio control while adding advanced surfaces and fonts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceStudio14(prefs: NurPreferences, model: NurViewModel, refreshStatus: String, navigate: (String) -> Unit) {
    var advancedOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = NurDesign.pagePadding, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NurFontSettingsLink(prefs) { navigate("fonts") }
            OutlinedButton(onClick = { advancedOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Liquid Glass & advanced progress")
            }
        }
        Box(Modifier.weight(1f)) { AppearanceStudio(prefs, model, refreshStatus) }
    }
    if (advancedOpen) ModalBottomSheet(onDismissRequest = { advancedOpen = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) { AdvancedAppearanceScreen(model) }
    }
}
