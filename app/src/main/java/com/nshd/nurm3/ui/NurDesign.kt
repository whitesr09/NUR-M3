package com.nshd.nurm3.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nshd.nurm3.data.Entry
import com.nshd.nurm3.data.NurKind
import java.time.LocalDate

/** NUR's custom visual vocabulary. Material supplies accessibility, not the visual identity. */
object NurDesign {
    val gold = Color(0xFFD9B96F)
    val darkGold = Color(0xFF75551A)
    val pagePadding = 20.dp
    val touchTarget = 48.dp
}

@Composable
fun NurPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val compact = LocalNurCompact.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(if (compact) 14.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun NurPageHeading(eyebrow: String, title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}

@Composable
fun NurPrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: (@Composable () -> Unit)? = null) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
    ) {
        if (icon != null) { icon(); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

/** The 48dp control is semantic and the visual mark is deliberately smaller. */
@Composable
fun NurCheckMark(checked: Boolean, modifier: Modifier = Modifier, pending: Boolean = false) {
    val reduce = LocalNurReduceMotion.current
    val progress by animateFloatAsState(if (checked) 1f else 0f, animationSpec = if (reduce) snap() else spring(stiffness = 420f), label = "Check mark")
    val foreground = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.outline
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(25.dp)) {
            val size = this.size
            val radius = CornerRadius(7.dp.toPx())
            drawRoundRect(color = foreground.copy(alpha = 0.12f * progress), cornerRadius = radius)
            drawRoundRect(color = lerp(empty, foreground, progress), cornerRadius = radius, style = Stroke(width = 1.6.dp.toPx()))
            if (progress > 0f) {
                val check = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.52f)
                    lineTo(size.width * 0.43f, size.height * 0.70f)
                    lineTo(size.width * 0.76f, size.height * 0.32f)
                }
                drawPath(check, color = foreground, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round), alpha = progress)
            }
        }
        if (pending) CircularProgressIndicator(modifier = Modifier.size(37.dp), strokeWidth = 1.5.dp, color = foreground)
    }
}

/** The stored completion is authoritative. A tap never fabricates a completed state. */
@Composable
fun ChecklistRow(
    entry: Entry,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    date: LocalDate = LocalDate.MIN,
    pending: Boolean = false
) {
    val reduce = LocalNurReduceMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !reduce) 0.985f else 1f, animationSpec = if (reduce) snap() else spring(stiffness = 520f), label = "Row press")
    val highlight = rememberNurProgress(if (checked) 1f else 0f, date, "row-${entry.id}")
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val canToggle = enabled && !pending
    val status = when {
        pending -> "Saving…"
        !enabled -> "Not scheduled today"
        checked -> "Completed"
        else -> "Tap to complete"
    }
    Row(
        Modifier.fillMaxWidth().scale(scale).clip(shape)
            .background(scheme.surfaceVariant.copy(alpha = 0.24f + 0.24f * highlight))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.45f + 0.3f * highlight), shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1f).heightIn(min = 60.dp)
                .toggleable(value = checked, enabled = canToggle, role = Role.Checkbox, interactionSource = interaction, indication = ripple(), onValueChange = onChecked)
                .semantics(mergeDescendants = true) {
                    contentDescription = entry.title
                    stateDescription = status
                }
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NurCheckMark(checked, pending = pending)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = scheme.onSurface)
                Text(status, style = MaterialTheme.typography.labelSmall, color = if (checked) scheme.primary else scheme.onSurfaceVariant)
                if (entry.kind != NurKind.PRAYER && entry.schedule != "daily") Text(entry.schedule.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
        trailing?.let {
            Box(Modifier.padding(end = 4.dp), contentAlignment = Alignment.Center) { it() }
        }
    }
}

@Composable
fun NurMoreAction(label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(NurDesign.touchTarget)) {
        Icon(Icons.Default.MoreHoriz, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The full setting row has one toggle action and one accessibility node. */
@Composable
fun NurSettingRow(title: String, description: String, value: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    NurPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().toggleable(value = value, enabled = enabled, role = Role.Switch, interactionSource = interaction, indication = ripple(), onValueChange = onChange).semantics(mergeDescendants = true) { contentDescription = title }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = value, onCheckedChange = null, enabled = enabled, modifier = Modifier.clearAndSetSemantics { }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary))
        }
    }
}
