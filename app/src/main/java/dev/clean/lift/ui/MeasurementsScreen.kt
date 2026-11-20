package dev.clean.lift.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.R
import dev.clean.lift.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementsScreen(onBack: () -> Unit, model: MeasurementsViewModel = viewModel(), initialMetric: BodyMetric = BodyMetric.BODY_WEIGHT,
                       initialRecord: BodyMeasurement? = null, startInEditor: Boolean = false,
                       initialUnit: MeasurementUnit = MeasurementInput.units(initialMetric).first()) {
    val state by model.state.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    val duplicate by model.duplicate.collectAsStateWithLifecycle()
    val busy = operation == WorkoutOperation.Saving || operation == WorkoutOperation.Saved
    var metricName by rememberSaveable { mutableStateOf(initialMetric.name) }
    var unitName by rememberSaveable { mutableStateOf((initialRecord?.unit ?: initialUnit).name) }
    var editing by rememberSaveable { mutableStateOf(startInEditor) }
    var id by rememberSaveable { mutableStateOf(initialRecord?.id) }
    var date by rememberSaveable { mutableStateOf(initialRecord?.let { MeasurementInput.dayText(it.epochDay) } ?: MeasurementInput.today()) }
    var value by rememberSaveable { mutableStateOf(initialRecord?.let { SetInput.weightText(it.valueMilliUnits) }.orEmpty()) }
    var originalDate by rememberSaveable { mutableStateOf(date) }
    var originalValue by rememberSaveable { mutableStateOf(value) }
    var originalUnit by rememberSaveable { mutableStateOf(unitName) }
    var expanded by remember { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var shown by rememberSaveable { mutableIntStateOf(50) }
    val metric = BodyMetric.valueOf(metricName)
    val unit = MeasurementUnit.valueOf(unitName)
    val valid = MeasurementInput.parseDay(date) != null && MeasurementInput.parseValue(value) != null && unit in MeasurementInput.units(metric)
    fun close() { editing = false; dialog = null; model.reset(); if (startInEditor) onBack() }
    fun back() {
        if (busy) return
        if (!editing) onBack()
        else if (date != originalDate || value != originalValue || unitName != originalUnit) dialog = "discard"
        else close()
    }
    fun open(record: BodyMeasurement?) {
        id = record?.id; date = record?.let { MeasurementInput.dayText(it.epochDay) } ?: MeasurementInput.today()
        value = record?.let { SetInput.weightText(it.valueMilliUnits) }.orEmpty()
        if (record != null) unitName = record.unit.name
        originalDate = date; originalValue = value; originalUnit = unitName
        editing = true; model.reset()
    }
    fun save() { if (valid && !busy) model.save(id, metric, date, value, unit) }
    LaunchedEffect(metric) { model.select(metric) }
    LaunchedEffect(operation) { if (operation == WorkoutOperation.Saved) close() }
    BackHandler { back() }
    if (dialog != null) AlertDialog(
        onDismissRequest = { if (!busy) dialog = null },
        title = { Text(stringResource(if (dialog == "delete") R.string.delete_measurement_title else R.string.discard_title)) },
        text = { Column {
            Text(stringResource(if (dialog == "delete") R.string.delete_measurement_message else R.string.discard_message))
            if (operation == WorkoutOperation.Failed) Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
        } },
        confirmButton = { TextButton(enabled = !busy, onClick = { if (dialog == "delete") id?.let(model::delete) else close() }) { Text(stringResource(if (dialog == "delete") R.string.remove else R.string.discard)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { dialog = null }) { Text(stringResource(R.string.keep_editing)) } }
    )
    Scaffold(modifier = Modifier.imePadding(), topBar = { TopAppBar(
        title = { Text(if (startInEditor) "${if (id == null) "Add" else "Edit"} measurement" else stringResource(R.string.body_measurements)) },
        navigationIcon = { BackButton(enabled = !busy, onClick = { back() }) }
    ) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (editing) {
                item { Text(stringResource(metricLabel(metric)), style = MaterialTheme.typography.titleLarge) }
                if (operation == WorkoutOperation.Failed) item { Text(stringResource(if (duplicate) R.string.duplicate_measurement else R.string.save_failed), color = MaterialTheme.colorScheme.error) }
                item { MeasurementDateField(date, { date = it; model.reset() }, enabled = !busy) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MeasurementInput.units(metric).forEach { choice ->
                    FilterChip(selected = unit == choice, enabled = !busy, onClick = { unitName = choice.name; model.reset() }, label = { Text(stringResource(unitLabel(choice))) })
                } } }
                item { OutlinedTextField(value = value, onValueChange = { value = it; model.reset() }, enabled = !busy, singleLine = true,
                    label = { Text(stringResource(R.string.measurement_value, stringResource(unitLabel(unit)))) },
                    supportingText = { Text(stringResource(R.string.measurement_value_guidance)) }, isError = value.isNotEmpty() && MeasurementInput.parseValue(value) == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }), modifier = Modifier.fillMaxWidth()) }
                item { Text(stringResource(R.string.measurement_entry_units), style = MaterialTheme.typography.bodySmall) }
                item { Button(enabled = !busy && valid, onClick = { save() }) { Text(stringResource(if (busy) R.string.saving else R.string.save)) } }
                if (id != null) item { TextButton(enabled = !busy, onClick = { dialog = "delete" }) { Text(stringResource(R.string.delete_measurement)) } }
            } else {
                item { Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(stringResource(metricLabel(metric))) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        BodyMetric.entries.forEach { choice -> DropdownMenuItem(text = { Text(stringResource(metricLabel(choice))) }, onClick = {
                            metricName = choice.name; unitName = MeasurementInput.units(choice).first().name; shown = 50; expanded = false
                        }) }
                    }
                } }
                item { Button(enabled = !busy, onClick = { open(null) }) { Text(stringResource(R.string.add_measurement)) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MeasurementInput.units(metric).forEach { choice ->
                    FilterChip(selected = unit == choice, onClick = { unitName = choice.name }, label = { Text(stringResource(unitLabel(choice))) })
                } } }
                when {
                    state.loading -> item { CircularProgressIndicator() }
                    state.failed -> item { Text(stringResource(R.string.load_failed)); TextButton(onClick = { model.select(metric) }) { Text(stringResource(R.string.retry)) } }
                    state.values.isEmpty() -> item { Text(stringResource(R.string.measurements_empty)) }
                    else -> {
                        val points = state.values.map { TrendValue(it.epochDay * StrengthEstimates.DAY_MILLIS, MeasurementInput.convert(it, unit)) }
                        val first = points.first().value
                        val latest = points.last().value
                        item { Text(stringResource(R.string.measurement_latest, displayNumber(latest), stringResource(unitLabel(unit)))) }
                        item { Text(stringResource(R.string.measurement_change, displayNumber(latest - first), stringResource(unitLabel(unit)))) }
                        item { TrendChart(points, stringResource(unitLabel(unit)), stringResource(metricLabel(metric))) { MeasurementInput.dayText(it / StrengthEstimates.DAY_MILLIS) } }
                        item { Text(stringResource(R.string.measurement_chart_guidance), style = MaterialTheme.typography.bodySmall) }
                        items(state.values.asReversed().take(shown), key = { it.id }) { record -> ListItem(
                            headlineContent = { Text(MeasurementInput.dayText(record.epochDay)) },
                            supportingContent = { Text("${displayNumber(MeasurementInput.convert(record, unit))} ${stringResource(unitLabel(unit))}") },
                            modifier = Modifier.clickable(onClickLabel = stringResource(R.string.edit_measurement)) { open(record) }
                        ) }
                        if (state.values.size > shown) item { TextButton(onClick = { shown += 50 }) { Text(stringResource(R.string.show_more)) } }
                    }
                }
            }
        }
    }
}

private fun unitLabel(unit: MeasurementUnit): Int = when (unit) { MeasurementUnit.KG -> R.string.kg; MeasurementUnit.LB -> R.string.lb; MeasurementUnit.CM -> R.string.cm; MeasurementUnit.IN -> R.string.inches }
private fun metricLabel(metric: BodyMetric): Int = when (metric) {
    BodyMetric.BODY_WEIGHT -> R.string.body_weight; BodyMetric.WAIST -> R.string.waist; BodyMetric.CHEST -> R.string.chest
    BodyMetric.HIPS -> R.string.hips; BodyMetric.NECK -> R.string.neck; BodyMetric.LEFT_BICEP -> R.string.left_bicep
    BodyMetric.RIGHT_BICEP -> R.string.right_bicep; BodyMetric.LEFT_THIGH -> R.string.left_thigh; BodyMetric.RIGHT_THIGH -> R.string.right_thigh
    BodyMetric.LEFT_CALF -> R.string.left_calf; BodyMetric.RIGHT_CALF -> R.string.right_calf
    BodyMetric.HEIGHT -> R.string.height
}
