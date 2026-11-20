package dev.clean.lift.widgets

import dev.clean.lift.ui.BackButton
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class PlateResult(val heading: String?, val lines: List<String>)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PlateCalculatorScreen(model: WidgetCalculatorModel, defaults: WidgetDefaults, onBack: () -> Unit, bottomBar: @Composable () -> Unit) {
    var modeName by rememberSaveable { mutableStateOf(PlateMode.TARGET.name) }
    val mode = PlateMode.valueOf(modeName)
    var unitName by rememberSaveable { mutableStateOf(LoadUnit.LB.name) }
    val unit = LoadUnit.valueOf(unitName)
    val location = defaults.location
    val equipment = defaults.equipment(location, unit)
    var target by rememberSaveable { mutableStateOf("") }
    var max by rememberSaveable { mutableStateOf("") }
    var working by rememberSaveable { mutableStateOf("") }
    var percent by rememberSaveable { mutableStateOf("75") }
    var steps by rememberSaveable { mutableStateOf("40, 60, 80") }
    var equipmentOpen by rememberSaveable { mutableStateOf(false) }
    val base = when (mode) { PlateMode.TARGET -> target; PlateMode.PERCENT -> max; PlateMode.WARMUP -> working }
    var results by remember { mutableStateOf<List<PlateResult>>(emptyList()) }
    var error by remember { mutableStateOf("") }
    var computing by remember { mutableStateOf(false) }

    LaunchedEffect(mode, base, percent, steps, unit, location, equipment) {
        results = emptyList(); error = ""; computing = false
        if (base.isBlank()) return@LaunchedEffect
        computing = true
        delay(150)
        val outcome = withContext(Dispatchers.Default) { runCatching {
            val stock = parseStock(equipment.rows) ?: error("Check the saved equipment.")
            val bar = WidgetCalculations.load(equipment.bar, true) ?: error("Check the saved bar weight.")
            plateTargets(mode, base, percent, steps).map { requested ->
                val options = WidgetCalculations.plates(requested.weight, bar, stock)
                val lines = if (options.exact != null) listOf(sideText(options.exact, unit)) else {
                    listOf("Exact load unavailable.") + listOfNotNull(
                        options.lower?.let { "Lower: ${plateDecimal(it.total)} ${unit.label} • ${sideText(it, unit)}" },
                        options.higher?.let { "Higher: ${plateDecimal(it.total)} ${unit.label} • ${sideText(it, unit)}" })
                }
                PlateResult(requested.caption?.let { "$it • ${plateDecimal(requested.weight)} ${unit.label}" }, lines)
            }
        } }
        results = outcome.getOrNull().orEmpty()
        error = outcome.exceptionOrNull()?.message.orEmpty()
        computing = false
    }
    if (equipmentOpen) {
        EquipmentEditor(model, defaults, location, unit, equipment, onClose = { equipmentOpen = false }, bottomBar = bottomBar)
        return
    }
    BackHandler(onBack = onBack)
    Scaffold(modifier = Modifier.imePadding(), topBar = { TopAppBar(title = { Text("Plate calculator") },
        navigationIcon = { BackButton(onClick = onBack) }) }, bottomBar = bottomBar) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    PlateMode.entries.forEachIndexed { index, value ->
                        SegmentedButton(selected = mode == value, onClick = { modeName = value.name },
                            shape = SegmentedButtonDefaults.itemShape(index, PlateMode.entries.size), icon = {}) { Text(value.label) }
                    }
                }
            }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { WidgetLocation.entries.forEach { value ->
                    FilterChip(selected = location == value, enabled = !defaults.saving,
                        onClick = { model.selectLocation(value) }, label = { Text(value.label) })
                } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { LoadUnit.entries.forEach { value ->
                    FilterChip(selected = unit == value, onClick = { if (unit != value) {
                        unitName = value.name; target = ""; max = ""; working = ""; model.clearMessage()
                    } }, label = { Text(value.label) })
                } }
            } }
            item { WidgetNumberField(base, { value -> when (mode) {
                PlateMode.TARGET -> target = value; PlateMode.PERCENT -> max = value; PlateMode.WARMUP -> working = value
            } }, "${when (mode) { PlateMode.TARGET -> "Target weight"; PlateMode.PERCENT -> "1RM"; PlateMode.WARMUP -> "Working weight" }} (${unit.label})") }
            if (mode == PlateMode.PERCENT) item { WidgetNumberField(percent, { percent = it }, "Percentage of 1RM") }
            if (mode == PlateMode.WARMUP) item {
                OutlinedTextField(value = steps, onValueChange = { steps = it.take(100) }, label = { Text("Warm-up percentages") },
                    supportingText = { Text("Separate with commas, for example 40, 60, 80.") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            // Results immediately follow the active inputs; a direct target is never repeated.
            when {
                computing -> item { Text("Calculating…") }
                error.isNotEmpty() -> item { Text(error, color = MaterialTheme.colorScheme.error) }
                base.isBlank() -> item { Text("Enter ${when (mode) { PlateMode.TARGET -> "a target weight"; PlateMode.PERCENT -> "your 1RM and percentage"; PlateMode.WARMUP -> "your working weight" }} to see plates.") }
                else -> items(results.size) { index ->
                    val result = results[index]
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.heading?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                        result.lines.forEach { Text(it) }
                        if (index < results.lastIndex) HorizontalDivider()
                    }
                }
            }
            item { Text("All weights include the bar. Plates are shown for each side.") }
            if (defaults.message.isNotEmpty()) item { Text(defaults.message) }
            item {
                HorizontalDivider()
                TextButton(onClick = { model.clearMessage(); equipmentOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("Equipment", style = MaterialTheme.typography.titleMedium)
                        Text("${location.label} • ${unit.label} • ${equipment.bar} ${unit.label} bar", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EquipmentEditor(model: WidgetCalculatorModel, defaults: WidgetDefaults, location: WidgetLocation, unit: LoadUnit,
    original: WidgetEquipment, onClose: () -> Unit, bottomBar: @Composable () -> Unit) {
    var bar by rememberSaveable { mutableStateOf(original.bar) }
    var rows by rememberSaveable { mutableStateOf(original.rows) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val changed = bar != original.bar || rows != original.rows
    fun back() { if (!defaults.saving) { if (changed) discard = true else onClose() } }
    BackHandler { back() }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard equipment changes?") },
        text = { Text("Your saved ${location.label} equipment will be kept.") },
        confirmButton = { TextButton(onClick = { discard = false; model.clearMessage(); onClose() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
    Scaffold(modifier = Modifier.imePadding(), topBar = { TopAppBar(title = { Text("${location.label} equipment (${unit.label})") },
        navigationIcon = { BackButton(enabled = !defaults.saving, onClick = { back() }) }) }, bottomBar = bottomBar) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Set this up once for ${location.label}. Counts are individual plates across both sides; unmatched spare plates are unused.") }
            item { WidgetNumberField(bar, { bar = it; model.clearMessage() }, "Bar weight (${unit.label})", enabled = !defaults.saving) }
            items(rows.size) { index ->
                val parts = rows[index].split(':')
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetNumberField(parts[0], { value -> rows = rows.toMutableList().also { it[index] = "$value:${parts.getOrElse(1) { "" }}" }; model.clearMessage() },
                        "Plate ${index + 1} (${unit.label})", Modifier.weight(1f), enabled = !defaults.saving)
                    WidgetNumberField(parts.getOrElse(1) { "" }, { value -> rows = rows.toMutableList().also { it[index] = "${parts[0]}:$value" }; model.clearMessage() },
                        "Plate ${index + 1} count", Modifier.weight(1f), whole = true, enabled = !defaults.saving)
                    TextButton(enabled = !defaults.saving, onClick = { rows = rows.filterIndexed { i, _ -> i != index }; model.clearMessage() }) { Text("Remove") }
                }
            }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = !defaults.saving && rows.size < 16, onClick = { rows = rows + "1:0"; model.clearMessage() }) { Text("Add plate size") }
                Button(enabled = !defaults.saving && parseStock(rows) != null && WidgetCalculations.load(bar, true) != null,
                    onClick = { model.save(location, unit, bar, rows, onSaved = onClose) }) { Text(if (defaults.saving) "Saving…" else "Save equipment") }
            } }
            if (defaults.message.isNotEmpty()) item { Text(defaults.message) }
            if (parseStock(rows) == null || WidgetCalculations.load(bar, true) == null) item {
                Text("Use positive plate sizes, counts from 0–100, and a bar weight from 0–2,000. Use a decimal point for fractional weights.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun sideText(load: PlateLoad, unit: LoadUnit): String = if (load.perSide.isEmpty()) "Bar only" else
    "Each side: " + load.perSide.joinToString(" + ") { "${it.second} × ${plateDecimal(it.first)} ${unit.label}" }
