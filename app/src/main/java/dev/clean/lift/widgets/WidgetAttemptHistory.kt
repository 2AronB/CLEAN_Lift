package dev.clean.lift.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.clean.lift.data.MeasurementInput

@Composable
internal fun WidgetAttemptHistory(modifier: Modifier, model: WidgetAttemptModel) {
    val attempts by model.attempts.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("All") }
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var shown by rememberSaveable { mutableIntStateOf(50) }
    val visible = attempts.filter { filter == "All" || it.protocol == filter }.asReversed()
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Original inputs and versions stay fixed. To correct an attempt, delete it and enter a replacement. Deleting also removes its linked health estimate/measurement, if still present.") }
        item { WidgetChoices(listOf("All") + WidgetProtocol.entries.map { it.title }, WidgetProtocol.entries.firstOrNull { it.name == filter }?.title ?: "All") { label ->
            filter = WidgetProtocol.entries.firstOrNull { it.title == label }?.name ?: "All"; shown = 50
        } }
        item { Text(operation.message) }
        if (visible.isEmpty()) item { Text("No saved attempts in this group.") }
        items(visible.take(shown), key = { it.id }) { attempt ->
            Text(WidgetProtocol.entries.firstOrNull { it.name == attempt.protocol }?.title ?: attempt.protocol, style = MaterialTheme.typography.titleMedium)
            Text("${MeasurementInput.dayText(attempt.epochDay)} • ${attempt.result ?: "Incomplete"} • ${attempt.mode}")
            Text(attempt.version)
            Text(attempt.details)
            if (attempt.deviations.isNotBlank()) Text(attempt.deviations, color = MaterialTheme.colorScheme.error)
            if (!attempt.complete) Text("Incomplete; no composite/progress result.")
            if (attempt.healthRecordId != null) Text("Linked health result saved in Progress.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { deleteId = attempt.id }, enabled = !operation.busy) { Text("Delete attempt") }
            HorizontalDivider()
        }
        if (shown < visible.size) item { TextButton(onClick = { shown += 50 }) { Text("Show more") } }
    }
    if (deleteId != null) AlertDialog(onDismissRequest = { if (!operation.busy) deleteId = null }, title = { Text("Delete this attempt?") },
        text = { Text("Its linked health result will also be removed. Other records are preserved.") },
        confirmButton = { TextButton(onClick = { model.delete(deleteId!!); deleteId = null }, enabled = !operation.busy) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } })
}
