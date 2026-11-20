package dev.clean.lift.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.clean.lift.R

// Plan-blocked actions stay reachable for long press and accessibility's long-click action.
// Busy/invalid-input actions are genuinely disabled and cannot be overridden.
@Composable
internal fun PlanAction(label: String, allowed: Boolean, enabled: Boolean, overrideAvailable: Boolean, onClick: () -> Unit, onOverride: () -> Unit) {
    val hint = stringResource(R.string.hold_override)
    val actionable = enabled && (allowed || overrideAvailable)
    val blocked = !allowed
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (blocked || !enabled) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (blocked || !enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.heightIn(min = 48.dp)
            .semantics { if (blocked && overrideAvailable) stateDescription = hint }
            .combinedClickable(enabled = actionable, role = Role.Button,
                onClick = { if (allowed) onClick() },
                onLongClickLabel = if (blocked && overrideAvailable) hint else null,
                onLongClick = if (blocked && overrideAvailable) onOverride else null)
    ) { Box(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) { Text(label, style = MaterialTheme.typography.labelLarge) } }
}
