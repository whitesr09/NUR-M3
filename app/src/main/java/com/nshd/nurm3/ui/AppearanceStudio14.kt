package com.nshd.nurm3.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.NurViewModel
import com.nshd.nurm3.data.NurPreferences

/** Keeps every existing Appearance Studio control while adding the font library. */
@Composable
fun AppearanceStudio14(prefs: NurPreferences, model: NurViewModel, refreshStatus: String, navigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = NurDesign.pagePadding, vertical = 8.dp)) {
            NurFontSettingsLink(prefs) { navigate("fonts") }
        }
        Box(Modifier.weight(1f)) {
            AppearanceStudio(prefs, model, refreshStatus)
        }
    }
}
