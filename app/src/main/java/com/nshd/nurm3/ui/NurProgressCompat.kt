package com.nshd.nurm3.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Compatibility bridge for the existing determinate rings. Indeterminate indicators remain Material3. */
@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    NurCircularProgress(
        target = progress(), date = LocalDate.MIN, label = "Progress",
        modifier = modifier, strokeWidth = strokeWidth, color = color, trackColor = trackColor
    )
}
