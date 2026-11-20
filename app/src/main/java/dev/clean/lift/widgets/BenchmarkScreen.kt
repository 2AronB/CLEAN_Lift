package dev.clean.lift.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.data.*
import java.util.UUID
import kotlin.math.roundToInt

@Composable
internal fun BenchmarkPage(kind: WidgetProtocol, modifier: Modifier, model: WidgetAttemptModel) {
    val clock: WidgetClockModel = viewModel(key = "widgets.benchmark.${kind.name}")
    val clockState by clock.state.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    var guided by rememberSaveable { mutableStateOf(false) }
    var value by rememberSaveable { mutableStateOf("") }
    var finished by rememberSaveable { mutableStateOf(false) }
    var partial by rememberSaveable { mutableStateOf(false) }
    var date by rememberSaveable { mutableStateOf(MeasurementInput.today()) }
    var notes by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var sex by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var pulse by rememberSaveable { mutableStateOf("") }
    var estimate by rememberSaveable { mutableStateOf(true) }
    var token by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    val guide = WidgetGuides.forProtocol(kind)
    val cap = when (kind) { WidgetProtocol.PUSH_UP -> 120; WidgetProtocol.PLANK -> 220; WidgetProtocol.COOPER -> 720; else -> 7200 }
    val plan = remember(kind) { TimerPlan(listOf(TimerPhase(kind.title, cap * 1000L)), 1) }
    LaunchedEffect(plan) { clock.configure(plan) }
    if (guided) VisibleWidgetClock(clock)
    fun finish() {
        clock.pause()
        val stopped = clock.state.value
        if (kind in listOf(WidgetProtocol.PUSH_UP, WidgetProtocol.PULL_UP)) value = stopped.tally.toString()
        if (kind in listOf(WidgetProtocol.PLANK, WidgetProtocol.ROCKPORT)) value = HealthCalculations.timeText((stopped.elapsed / 1000.0).roundToInt().coerceIn(1, cap))
        partial = kind == WidgetProtocol.COOPER && stopped.elapsed < 720_000
        finished = true
    }
    LaunchedEffect(clockState.elapsed) { if (guided && !finished && clockState.elapsed >= plan.totalMillis) finish() }
    val day = MeasurementInput.parseDay(date)
    val result = if (kind == WidgetProtocol.PLANK && value.trim() == "0:00") 0.0
        else if (kind in listOf(WidgetProtocol.PLANK, WidgetProtocol.ROCKPORT)) HealthCalculations.parseTime(value)?.toDouble()
        else value.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val rawValid = result != null && when (kind) {
        WidgetProtocol.PUSH_UP, WidgetProtocol.PULL_UP -> result <= 1000 && result == result.toInt().toDouble()
        WidgetProtocol.PLANK -> result <= 220
        WidgetProtocol.ROCKPORT -> result <= 7200
        WidgetProtocol.COOPER -> result <= 6000
        else -> false
    }
    val healthDraft = if (day != null && result != null && kind in listOf(WidgetProtocol.COOPER, WidgetProtocol.ROCKPORT)) HealthRecord(
        kind = if (kind == WidgetProtocol.COOPER) HealthKind.COOPER else HealthKind.ROCKPORT, epochDay = day, result = 0.0,
        distanceMetres = if (kind == WidgetProtocol.COOPER) result else 1609.344,
        timeSeconds = if (kind == WidgetProtocol.ROCKPORT) result.toInt() else 720,
        weightKg = weight.toDoubleOrNull(), age = age.toIntOrNull(), gender = sex,
        heartRate = pulse.toIntOrNull(), notes = "Widgets • ${kind.version} • $token\n$notes".take(1000)) else null
    val prediction = if (age.isNotBlank() && age.toIntOrNull() == null) null else healthDraft?.let(HealthCalculations::calculate)
    val deviations = listOfNotNull(if (guided && clockState.interrupted) "Timer interrupted." else null,
        if (partial) "Stopped before 12 minutes; incomplete distance test." else null).joinToString(" ")
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(guide.equipment, style = MaterialTheme.typography.titleMedium); Text(guide.protocol) }
        item { WidgetChoices(listOf("Enter result", "Run benchmark"), if (guided) "Run benchmark" else "Enter result", enabled = !clockState.running && !operation.busy) {
            guided = it == "Run benchmark"; clock.reset(); finished = false; partial = false; value = ""; token = UUID.randomUUID().toString(); model.clear()
        } }
        if (guided) item {
            Text("An observer judges repetitions/form and announces the 15-second tones.")
            if (!finished) WidgetClockControls(clock, plan, elapsedDisplay = kind in listOf(WidgetProtocol.PULL_UP, WidgetProtocol.ROCKPORT), strict = true,
                showTally = kind in listOf(WidgetProtocol.PUSH_UP,WidgetProtocol.PULL_UP,WidgetProtocol.COOPER), showLaps = false, onFinish = ::finish)
            else Text("Finished at ${WidgetTimerPlans.clockText(clockState.elapsed)}")
            if (clockState.interrupted) Text("Interrupted practice; excluded from comparable trends.", color = MaterialTheme.colorScheme.error)
        }
        if(kind==WidgetProtocol.ROCKPORT) item { WidgetNumberField(pulse,{pulse=it},"Immediate finishing pulse (bpm)",whole=true) }
        item {
            if (kind in listOf(WidgetProtocol.PLANK, WidgetProtocol.ROCKPORT)) OutlinedTextField(value, { value = it.take(8) }, label = { Text("Time (m:ss)") }, enabled = !clockState.running, singleLine = true, modifier = Modifier.fillMaxWidth())
            else WidgetNumberField(value, { value = it }, if (kind == WidgetProtocol.COOPER) "Distance covered in 12 minutes (m, 0–6000)" else "Valid repetitions (0–1000)", whole = kind != WidgetProtocol.COOPER, enabled = !clockState.running)
        }
        item { AttemptDateField(date) { date = it } }
        if (kind in listOf(WidgetProtocol.COOPER, WidgetProtocol.ROCKPORT)) {
            item { Row { Checkbox(estimate, { estimate = it }); Text("Also store available VO₂ estimate in Progress") } }
            item { WidgetNumberField(age, { age = it }, if (kind == WidgetProtocol.ROCKPORT) "Age at test (20–69 for estimate)" else "Age at test (optional; adults 18+)", whole = true) }
            item { WidgetChoices(listOf("M", "F", "Not set"), sex.ifEmpty { "Not set" }) { sex = if (it == "Not set") "" else it } }
            if (kind == WidgetProtocol.ROCKPORT) {
                item { WidgetNumberField(weight, { weight = it }, "Weight at test (kg)") }
            }
            item { Text(if (prediction != null) "Estimated VO₂max: %.1f mL/kg/min".format(java.util.Locale.US, prediction) else "Estimate unavailable with these inputs. The measured result can still be saved.") }
        }
        item { OutlinedTextField(notes, { notes = it.take(1000) }, label = { Text("Course / grip / conditions / notes") }, modifier = Modifier.fillMaxWidth()) }
        item {
            Button(onClick = {
                val details = "${kind.version}\n${result} ${kind.unit} • ${if (guided) "guided" else "entered"}\nAge: $age; equation category: $sex; weight: $weight kg; finishing pulse: $pulse bpm.\n$notes"
                model.save(WidgetAttempt(token = token, protocol = kind.name, version = kind.version, epochDay = day!!, result = result,
                    complete = !partial, mode = if (guided) "Guided" else "Entered", details = details, deviations = deviations),
                    if (estimate && prediction != null && deviations.isBlank()) healthDraft else null)
            }, enabled = rawValid && day != null && (!guided || finished) && !clockState.running && !operation.busy && operation.savedToken != token) { Text("Save benchmark to Progress") }
            if (!rawValid || day == null) Text("Enter a valid result and date before saving.")
            if (deviations.isNotBlank()) Text(deviations)
            Text(operation.message)
            TextButton(onClick = { clock.reset(); finished = false; partial = false; value = ""; token = UUID.randomUUID().toString(); model.clear() }, enabled = !clockState.running && !operation.busy) { Text("New attempt") }
        }
        item { WidgetSourceLink(guide.sourceTitle, guide.sourceUrl) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WidgetChoices(options: List<String>, selected: String, enabled: Boolean = true, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.forEach { option ->
        FilterChip(selected = selected == option, onClick = { onSelect(option) }, enabled = enabled, label = { Text(option) })
    } }
}
@Composable
internal fun AttemptDateField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, { onChange(it.take(10)) }, label = { Text("Test date (YYYY-MM-DD)") }, singleLine = true, isError = MeasurementInput.parseDay(value) == null, modifier = Modifier.fillMaxWidth())
}
@Composable
internal fun WidgetSourceLink(title: String, url: String) {
    val uri = LocalUriHandler.current
    var failed by remember { mutableStateOf(false) }
    TextButton(onClick = { try { uri.openUri(url); failed = false } catch (_: Exception) { failed = true } }) { Text(title) }
    if (failed) Text("No browser could open the source.", color = MaterialTheme.colorScheme.error)
}
