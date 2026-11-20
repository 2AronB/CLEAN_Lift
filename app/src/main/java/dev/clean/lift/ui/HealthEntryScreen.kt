package dev.clean.lift.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.clean.lift.data.*
import java.util.Locale

private val lengths = setOf("height", "waist", "abdomen", "neck", "hips")
private fun numberText(value: Double?): String = value?.let { String.format(Locale.ROOT, "%.3f", it).trimEnd('0').trimEnd('.') }.orEmpty()
private fun parseNumber(text: String): Double? = if (Regex("[0-9]+(?:\\.[0-9]{1,3})?").matches(text.trim())) text.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } else null

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun HealthEntryScreen(kind: HealthKind, record: HealthRecord?, state: MetricsState,
                               operation: WorkoutOperation, onSave: (HealthRecord) -> Unit,
                               onDelete: (Long) -> Unit, onReset: () -> Unit, onBack: () -> Unit) {
    val saver = remember { listSaver<Map<String, String>, String>(save = { map -> map.flatMap { listOf(it.key, it.value) } },
        restore = { list -> list.chunked(2).associate { it[0] to it[1] } }) }
    var imperial by rememberSaveable { mutableStateOf(state.imperial) }
    var date by rememberSaveable { mutableStateOf(record?.let { MeasurementInput.dayText(it.epochDay) } ?: MeasurementInput.today()) }
    var gender by rememberSaveable { mutableStateOf(record?.gender ?: state.gender) }
    val initial = remember {
        val day = record?.epochDay ?: MeasurementInput.parseDay(MeasurementInput.today())!!
        fun body(metric: BodyMetric): Double? = state.body.lastOrNull { it.metric == metric && (if (metric == BodyMetric.HEIGHT) it.epochDay <= day else it.epochDay == day) }
            ?.let { MeasurementInput.convert(it, if (metric == BodyMetric.BODY_WEIGHT) MeasurementUnit.KG else MeasurementUnit.CM) }
        val input = record ?: HealthRecord(kind = kind, epochDay = day, result = 0.0,
            heightCm = body(BodyMetric.HEIGHT), weightKg = body(BodyMetric.BODY_WEIGHT),
            waistCm = if (kind == HealthKind.WAIST_HEIGHT || kind == HealthKind.WAIST_HIP) body(BodyMetric.WAIST) else null,
            neckCm = body(BodyMetric.NECK), hipsCm = body(BodyMetric.HIPS), age = state.age, gender = state.gender)
        mapOf("height" to numberText(input.heightCm?.div(if (imperial) 2.54 else 1.0)),
            "weight" to numberText(input.weightKg?.div(if (imperial) StrengthEstimates.LB_TO_KG else 1.0)),
            "waist" to numberText(input.waistCm?.div(if (imperial) 2.54 else 1.0)),
            "abdomen" to numberText(input.abdomenCm?.div(if (imperial) 2.54 else 1.0)),
            "neck" to numberText(input.neckCm?.div(if (imperial) 2.54 else 1.0)),
            "hips" to numberText(input.hipsCm?.div(if (imperial) 2.54 else 1.0)),
            "age" to input.age?.toString().orEmpty(), "time" to input.timeSeconds?.let(HealthCalculations::timeText).orEmpty(),
            "distance" to numberText(input.distanceMetres), "pulse" to input.heartRate?.toString().orEmpty(),
            "result" to if (kind.manual && record != null) numberText(record.result) else "", "notes" to input.notes)
    }
    var fields by rememberSaveable(stateSaver = saver) { mutableStateOf(initial) }
    var original by rememberSaveable(stateSaver = saver) { mutableStateOf(initial) }
    var originalDate by rememberSaveable { mutableStateOf(date) }
    var originalGender by rememberSaveable { mutableStateOf(gender) }
    var originalImperial by rememberSaveable { mutableStateOf(imperial) }
    var confirmed by rememberSaveable { mutableStateOf(record != null || kind.manual || kind == HealthKind.BMI) }
    var prefilledDate by rememberSaveable { mutableStateOf(date) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    val busy = operation in listOf(WorkoutOperation.Saving, WorkoutOperation.Saved)
    val guide = HealthGuides.forKind(kind, gender)
    fun set(key: String, value: String) { fields = fields + (key to value); onReset() }
    fun back() {
        if (busy) return
        if (fields != original || date != originalDate || gender != originalGender || imperial != originalImperial) dialog = "discard" else onBack()
    }
    BackHandler { back() }
    fun length(key: String) = parseNumber(fields[key].orEmpty())?.times(if (imperial) 2.54 else 1.0)
    val used = when (kind) {
        HealthKind.BMI -> listOf("height", "weight")
        HealthKind.WAIST_HEIGHT -> listOf("height", "waist", "weight")
        HealthKind.WAIST_HIP -> listOf("waist", "hips")
        HealthKind.NAVY_FAT -> listOf("height", "neck") + (if (gender == "F") listOf("waist", "hips") else listOf("abdomen")) + "weight"
        HealthKind.RFM_FAT -> listOf("height", "waist", "weight")
        HealthKind.ROCKPORT -> listOf("weight", "time", "pulse")
        HealthKind.COOPER -> listOf("distance")
        else -> listOf("result") + if (kind.bodyFat) listOf("weight") else emptyList()
    }
    val draft = HealthRecord(id = record?.id ?: 0L, kind = kind, epochDay = MeasurementInput.parseDay(date) ?: -1,
        result = parseNumber(fields["result"].orEmpty()) ?: 0.0,
        heightCm = if ("height" in used) length("height") else null,
        weightKg = if ("weight" in used) parseNumber(fields["weight"].orEmpty())?.times(if (imperial) StrengthEstimates.LB_TO_KG else 1.0) else null,
        waistCm = if ("waist" in used) length("waist") else null, abdomenCm = if ("abdomen" in used) length("abdomen") else null,
        neckCm = if ("neck" in used) length("neck") else null, hipsCm = if ("hips" in used) length("hips") else null,
        age = fields["age"]?.trim()?.toIntOrNull(), gender = gender,
        timeSeconds = if ("time" in used) HealthCalculations.parseTime(fields["time"].orEmpty()) else null,
        distanceMetres = if ("distance" in used) parseNumber(fields["distance"].orEmpty()) else null,
        heartRate = if ("pulse" in used) fields["pulse"]?.toIntOrNull() else null, notes = fields["notes"].orEmpty())
    val invalidOptional = used.any { key -> fields[key].orEmpty().isNotBlank() && when (key) {
        "time" -> draft.timeSeconds == null; "pulse" -> draft.heartRate == null; else -> parseNumber(fields[key].orEmpty()) == null
    } } || (fields["age"].orEmpty().isNotBlank() && draft.age !in 1..120)
    val result = if (invalidOptional) null else HealthCalculations.calculate(draft)
    LaunchedEffect(operation) { if (operation == WorkoutOperation.Saved) { onReset(); onBack() } }
    if (dialog != null) AlertDialog(onDismissRequest = { if (!busy) dialog = null },
        title = { Text(if (dialog == "delete") "Delete this result?" else "Discard changes?") },
        text = { Text(if (operation == WorkoutOperation.Failed) "The change could not be saved. Your existing data is unchanged." else if (dialog == "delete") "Only this metric result will be deleted." else "Your unsaved changes will be discarded.") },
        confirmButton = { TextButton(enabled = !busy, onClick = { if (dialog == "delete") record?.let { onDelete(it.id) } else onBack() }) { Text(if (dialog == "delete") "Delete" else "Discard") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { dialog = null }) { Text("Keep editing") } })
    Scaffold(modifier = Modifier.imePadding(), topBar = { TopAppBar(title = { Text(if (record == null) "Add result" else "Edit result") },
        navigationIcon = { BackButton(enabled = !busy, onClick = { back() }) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(kind.title, style = MaterialTheme.typography.titleLarge) }
            item { Text("Required equipment & data", style = MaterialTheme.typography.titleMedium); Text(guide.requirements) }
            item { Text(guide.protocol) }
            if (operation == WorkoutOperation.Failed) item { Text("Could not save. Check the inputs and try again.", color = MaterialTheme.colorScheme.error) }
            item { MeasurementDateField(date, { date = it; onReset() }, enabled = !busy) }
            if (record == null && used.any { it in lengths || it == "weight" }) item {
                TextButton(enabled = !busy && draft.epochDay >= 0, onClick = {
                    fun prefill(metric: BodyMetric, field: String) {
                        val measurement = state.body.lastOrNull { it.metric == metric && (if (metric == BodyMetric.HEIGHT) it.epochDay <= draft.epochDay else it.epochDay == draft.epochDay) }
                        val unit = if (metric == BodyMetric.BODY_WEIGHT) { if (imperial) MeasurementUnit.LB else MeasurementUnit.KG } else if (imperial) MeasurementUnit.IN else MeasurementUnit.CM
                        set(field, numberText(measurement?.let { MeasurementInput.convert(it, unit) }))
                    }
                    if ("height" in used) prefill(BodyMetric.HEIGHT, "height")
                    if ("weight" in used) prefill(BodyMetric.BODY_WEIGHT, "weight")
                    if (kind in listOf(HealthKind.WAIST_HEIGHT, HealthKind.WAIST_HIP)) prefill(BodyMetric.WAIST, "waist")
                    if ("neck" in used) prefill(BodyMetric.NECK, "neck")
                    if ("hips" in used) prefill(BodyMetric.HIPS, "hips")
                    prefilledDate = date; confirmed = kind == HealthKind.BMI || kind.manual
                }) { Text("Load measurements for selected date") }
                Text("Last prefill date: $prefilledDate. Changing the date does not replace your draft; use this button to load its measurements.", style = MaterialTheme.typography.bodySmall)
            }
            item {
                Text("Profile gender used: ${gender.ifEmpty { "Not set" }}. Saved with this result.")
                if (kind.needsGender && gender.isEmpty()) Text("Set M or F in Settings > Edit profile to calculate this estimate.", color = MaterialTheme.colorScheme.error)
                if (record != null && gender != state.gender) TextButton(enabled = !busy, onClick = { gender = state.gender; onReset() }) { Text("Use current profile gender (${state.gender.ifEmpty { "Not set" }})") }
            }
            item { OutlinedTextField(fields["age"].orEmpty(), { set("age", it) }, label = { Text("Age at measurement${if (kind == HealthKind.ROCKPORT) " • required 20–69" else " • optional"}") },
                supportingText = { Text("Prefilled from profile. Correct the age when entering an older result.") }, singleLine = true, enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
            if (used.any { it in lengths || it == "weight" }) item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(false, true).forEach { choice -> FilterChip(selected = imperial == choice, enabled = !busy,
                        onClick = {
                            if (imperial != choice) {
                                fields = fields.mapValues { (key, value) ->
                                    val parsed = parseNumber(value)
                                    when { parsed == null -> value
                                        key in lengths -> numberText(parsed * if (choice) 1 / 2.54 else 2.54)
                                        key == "weight" -> numberText(parsed * if (choice) 1 / StrengthEstimates.LB_TO_KG else StrengthEstimates.LB_TO_KG)
                                        else -> value }
                                }; imperial = choice; onReset()
                            }
                        }, label = { Text(if (choice) "lb / in" else "kg / cm") }) }
                }
            }
            used.forEach { key -> item {
                val optional = key == "weight" && kind != HealthKind.BMI && kind != HealthKind.ROCKPORT
                val label = when (key) {
                    "height" -> "Height"; "weight" -> "Bodyweight"; "waist" -> if (kind == HealthKind.NAVY_FAT) "Natural waist" else if (kind == HealthKind.RFM_FAT) "Waist at hip crest" else "Midpoint waist"
                    "abdomen" -> "Abdomen at navel"; "neck" -> "Neck"; "hips" -> "Hips"
                    "time" -> "One-mile walk time • minutes:seconds"; "pulse" -> "Finishing pulse • bpm"
                    "distance" -> "Distance in exactly 12 minutes • metres"; else -> "Reported result • ${kind.unit}"
                } + if (key in lengths) " • ${if (imperial) "in" else "cm"}" else if (key == "weight") " • ${if (imperial) "lb" else "kg"}" else ""
                OutlinedTextField(fields[key].orEmpty(), { set(key, it) }, label = { Text(label + if (optional) " • optional" else "") },
                    enabled = !busy, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = if (key == "time") KeyboardType.Text else KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
            } }
            item { Text("Prefills use same-day measurements; height may come from an earlier date. Navy/RFM waist sites need their own values. Verify every value and landmark. Saving this result does not change the original measurement entries.", style = MaterialTheme.typography.bodySmall) }
            item { OutlinedTextField(fields["notes"].orEmpty(), { set("notes", it) }, label = { Text("Notes • device, conditions or protocol") },
                enabled = !busy, isError = fields["notes"].orEmpty().length > 1000, supportingText = { Text("Up to 1,000 characters") }, modifier = Modifier.fillMaxWidth()) }
            if (!kind.manual && kind != HealthKind.BMI) item { Row {
                Checkbox(confirmed, { confirmed = it }, enabled = !busy)
                Text("I used the measurement sites or test protocol described above.", Modifier.weight(1f).padding(top = 12.dp))
            } }
            if (result != null) item { Text("${if (kind.estimated) "Estimated" else "Result"}: ${displayNumber(result)} ${kind.unit}", style = MaterialTheme.typography.titleMedium) }
            else item { Text("Enter valid required data to calculate. Length/mass inputs must be positive and ≤1,000 in cm/kg; finishing pulse 30–250 bpm. Estimates outside 0–100% body fat or 0–120 mL/kg/min cannot be saved.", style = MaterialTheme.typography.bodySmall) }
            item { Button(enabled = !busy && confirmed && result != null && draft.epochDay >= 0, onClick = { onSave(draft.copy(result = result!!)) }) { Text(if (busy) "Saving…" else "Save result") } }
            if (record != null) item { TextButton(enabled = !busy, onClick = { dialog = "delete" }) { Text("Delete result") } }
        }
    }
}
