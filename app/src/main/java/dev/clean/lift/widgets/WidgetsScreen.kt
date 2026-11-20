package dev.clean.lift.widgets

import androidx.compose.foundation.clickable
import dev.clean.lift.ui.BackButton
import dev.clean.lift.ui.SettingsButton
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

private enum class Calculator(val title: String, val detail: String, val group: String = "Calculators") {
    GENERAL("General timer", "Countdown, stopwatch/laps, intervals, EMOM and AMRAP", "Timers"),
    TEMPO("Tempo timer", "Four lifting phases, fixed reps, continuous or metronome", "Timers"),
    SHUTTLE("Shuttle timer", "PACER / HAMR with local official audio and observer scoring", "Timers"),
    AIR_FORCE("Air Force 2026 PFRA", "Quick score, prescribed practice timing and target explorer", "Assessments"),
    PLATES("Plate calculator", "Target weight, %Max and warm-up loads using your equipment"),
    PACE("Pace & speed", "Convert a distance and finish time into pace and speed"),
    SPLITS("Target splits", "Even-paced lap and cumulative times"),
    PUSH_UP("Two-minute push-ups", "Navy form • valid repetitions", "Benchmarks"),
    PULL_UP("Strict pull-ups", "USMC dead-hang form • valid repetitions", "Benchmarks"),
    PLANK("Forearm plank", "AF form • personal benchmark capped at 3:40", "Benchmarks"),
    COOPER("Cooper 12-minute run", "Measured distance and optional VO₂ estimate", "Benchmarks"),
    ROCKPORT("Rockport one-mile walk", "Measured time, finishing pulse and optional VO₂ estimate", "Benchmarks"),
    ATTEMPTS("Saved attempts", "Original inputs, protocol versions and corrections", "History")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(onBack: () -> Unit, bottomBar: @Composable () -> Unit, onSettings: () -> Unit = {}) {
    var route by rememberSaveable { mutableStateOf("") }
    val pages = rememberSaveableStateHolder()
    val model: WidgetCalculatorModel = viewModel()
    val attempts: WidgetAttemptModel = viewModel()
    val defaults by model.state.collectAsStateWithLifecycle()
    val tool = Calculator.entries.firstOrNull { it.name == route }
    val back = { if (tool == null) onBack() else { route = ""; model.clearMessage(); attempts.clear() } }
    // Plate calculator owns its equipment sub-menu and draft-aware Back behavior.
    if (tool != Calculator.PLATES) BackHandler(onBack = back)
    pages.SaveableStateProvider(route) {
        if (tool == Calculator.PLATES && !defaults.loading) {
            PlateCalculatorScreen(model, defaults, onBack = back, bottomBar = bottomBar)
        } else Scaffold(modifier = Modifier.imePadding(), topBar = { TopAppBar(title = { Text(tool?.title ?: "Tools") },
            navigationIcon = { if (tool != null) BackButton(onClick = back) },
            actions = { if (tool == null) SettingsButton(onClick = onSettings) }) }, bottomBar = bottomBar) { padding ->
            if (defaults.loading) {
                Box(Modifier.padding(padding).padding(24.dp)) { CircularProgressIndicator() }
            } else if (tool == null) LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(Calculator.entries.size) { index ->
                    val calculator = Calculator.entries[index]
                    if (index == 0 || Calculator.entries[index-1].group != calculator.group) Text(calculator.group, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp))
                    ListItem(headlineContent = { Text(calculator.title) },
                        supportingContent = { Text(calculator.detail) },
                        modifier = Modifier.clickable { route = calculator.name })
                    HorizontalDivider()
                }
            } else if (tool in listOf(Calculator.GENERAL, Calculator.TEMPO)) WidgetTimerPage(tool == Calculator.TEMPO, Modifier.fillMaxSize().padding(padding))
            else if (tool == Calculator.SHUTTLE) ShuttlePage(Modifier.fillMaxSize().padding(padding), attempts)
            else if (tool == Calculator.AIR_FORCE) AirForcePage(Modifier.fillMaxSize().padding(padding), attempts)
            else if (tool == Calculator.ATTEMPTS) WidgetAttemptHistory(Modifier.fillMaxSize().padding(padding), attempts)
            else if (tool.name in listOf("PUSH_UP", "PULL_UP", "PLANK", "COOPER", "ROCKPORT")) BenchmarkPage(WidgetProtocol.valueOf(tool.name), Modifier.fillMaxSize().padding(padding), attempts)
            else PaceCalculatorPage(tool, Modifier.fillMaxSize().padding(padding))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaceCalculatorPage(tool: Calculator, modifier: Modifier) {
    var distance by rememberSaveable { mutableStateOf("") }
    var unitName by rememberSaveable { mutableStateOf(DistanceUnit.KM.name) }
    val unit = DistanceUnit.valueOf(unitName)
    var finish by rememberSaveable { mutableStateOf("") }
    var segment by rememberSaveable { mutableStateOf("400") }
    val outcome = remember(tool, distance, unit, finish, segment) { runCatching {
        val metres = (WidgetCalculations.positive(distance) ?: error("Enter a positive distance.")) * unit.metres
        require(metres in 1.0..1_000_000.0) { "Distance must be between 1 m and 1,000 km." }
        val seconds = WidgetCalculations.duration(finish) ?: error("Enter time as m:ss or h:mm:ss, from 0:01 to 24:00:00.")
        if (tool == Calculator.PACE) {
            val pace = WidgetCalculations.pace(metres, seconds)
            listOf("${WidgetCalculations.time(pace.secondsPerKm)} / km", "${WidgetCalculations.time(pace.secondsPerMile)} / mile",
                "${widgetNumber(pace.kmPerHour)} km/h", "${widgetNumber(pace.milesPerHour)} mph")
        } else {
            val split = WidgetCalculations.positive(segment) ?: error("Enter a positive split length in metres.")
            require(split in 1.0..metres) { "Split length must be between 1 m and the full distance." }
            WidgetCalculations.splits(metres, seconds, split).mapIndexed { index, value ->
                "${index + 1}. ${widgetNumber(value.distanceMetres)} m • ${WidgetCalculations.time(value.segmentSeconds)} segment • ${WidgetCalculations.time(value.cumulativeSeconds)} elapsed"
            }
        }
    } }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(tool.detail) }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { DistanceUnit.entries.forEach { value ->
            FilterChip(selected = unit == value, onClick = { if (unit != value) { unitName = value.name; distance = "" } }, label = { Text(value.label) })
        } } }
        item { WidgetNumberField(distance, { distance = it }, "Distance (${unit.label})") }
        item { OutlinedTextField(value = finish, onValueChange = { finish = it.take(12) }, label = { Text("Finish time (m:ss or h:mm:ss)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = true, modifier = Modifier.fillMaxWidth()) }
        if (tool == Calculator.SPLITS) item { WidgetNumberField(segment, { segment = it }, "Split length (m)") }
        item { HorizontalDivider(); Text("Result", style = MaterialTheme.typography.titleMedium) }
        val result = outcome.getOrNull()
        if (result == null) item { Text(outcome.exceptionOrNull()?.message.orEmpty(), color = MaterialTheme.colorScheme.error) }
        else items(result.size) { index -> Text(result[index]) }
        if (tool == Calculator.SPLITS) item { Text("Even pacing. Each displayed time is rounded independently to the nearest second; the exact total is retained.") }
    }
}

@Composable
internal fun WidgetNumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth(), whole: Boolean = false, enabled: Boolean = true) {
    OutlinedTextField(value = value, onValueChange = { onChange(it.replace(':', ' ').take(16)) }, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (whole) KeyboardType.Number else KeyboardType.Decimal), modifier = modifier, enabled = enabled)
}
private fun widgetNumber(value: Double): String = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
