package dev.clean.lift.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyMeasurementsScreen(body: List<BodyMeasurement>, imperial: Boolean, onBack: () -> Unit,
                                     model: DailyMeasurementsViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    var date by rememberSaveable { mutableStateOf(MeasurementInput.today()) }
    var values by rememberSaveable { mutableStateOf(List(BodyMetric.entries.size) { "" }) }
    // Unit choices label the entered numbers. Retain them independently of later preference changes.
    val initialImperial = rememberSaveable { imperial }
    var discard by rememberSaveable { mutableStateOf(false) }
    val day = MeasurementInput.parseDay(date)
    val entries = BodyMetric.entries.mapIndexed { index, metric ->
        val unit = if (metric == BodyMetric.BODY_WEIGHT) { if (initialImperial) MeasurementUnit.LB else MeasurementUnit.KG }
            else if (initialImperial) MeasurementUnit.IN else MeasurementUnit.CM
        DailyMeasurement(metric, values[index], unit)
    }
    val conflicts = entries.filter { it.value.isNotBlank() && body.any { saved -> saved.metric == it.metric && saved.epochDay == day } }.map { it.metric }.toSet()
    val valid = day != null && conflicts.isEmpty() && runCatching { DailyMeasurementInput.records(day, entries) }.isSuccess
    val busy = state.saving || state.saved
    fun back() { if (!busy) { if (values.any { it.isNotBlank() }) discard = true else { model.reset(); onBack() } } }
    BackHandler { back() }
    LaunchedEffect(state.saved) { if (state.saved) { model.reset(); onBack() } }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard measurements?") },
        text = { Text("Your unsaved measurements will be discarded.") },
        confirmButton = { TextButton(onClick = { model.reset(); onBack() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
    Scaffold(modifier = Modifier.imePadding(), topBar = { TopAppBar(title = { Text("Daily measurements") },
        navigationIcon = { BackButton(enabled = !busy, onClick = { back() }) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Record any measurements taken on the same day. Leave the rest blank. Existing entries are kept; correct those from their charts.") }
            item { MeasurementDateField(date, { date = it; model.reset() }, enabled = !busy) }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            items(entries, key = { it.metric.name }) { entry ->
                val index = entry.metric.ordinal
                val conflict = entry.metric in conflicts
                OutlinedTextField(values[index], { next -> values = values.toMutableList().also { it[index] = next }; model.reset() },
                    label = { Text("${bodyMetricTitle(entry.metric)} • ${entry.unit.name.lowercase()}") },
                    enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    isError = conflict || (entry.value.isNotBlank() && MeasurementInput.parseValue(entry.value) == null),
                    supportingText = { if (conflict) Text("Already recorded for this date. Clear this field to save the others.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
            item { Text("All entered measurements save together. No calculated health results are created automatically.", style = MaterialTheme.typography.bodySmall) }
            item { Button(enabled = !busy && valid, onClick = { model.save(date, entries) }) { Text(if (busy) "Saving…" else "Save measurements") } }
        }
    }
}
