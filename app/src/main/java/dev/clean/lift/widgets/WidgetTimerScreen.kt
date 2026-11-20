package dev.clean.lift.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WidgetTimerPage(tempo: Boolean, modifier: Modifier) {
    val clock: WidgetClockModel = viewModel(key = if (tempo) "widgets.tempo" else "widgets.general")
    val state by clock.state.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(if (tempo) "Tempo" else "Countdown") }
    var seconds by rememberSaveable { mutableStateOf("60") }
    var rest by rememberSaveable { mutableStateOf("30") }
    var rounds by rememberSaveable { mutableStateOf("10") }
    var phases by rememberSaveable { mutableStateOf(listOf("3", "0", "1", "0")) }
    var continuous by rememberSaveable { mutableStateOf(false) }
    var bpm by rememberSaveable { mutableStateOf("60") }
    var sound by rememberSaveable { mutableStateOf(true) }
    val outcome = remember(mode, seconds, rest, rounds, phases, continuous, bpm) { runCatching {
        fun number(s: String, min: Int = 1, max: Int = 86400) = requireNotNull(s.toIntOrNull()?.takeIf { it in min..max }) { "Enter valid whole numbers within the field limits." }
        if (tempo) {
            if (mode == "Metronome") WidgetTimerPlans.metronome(number(bpm, 30, 300), if (continuous) 40_000 else number(rounds, 1, 10_000))
            else WidgetTimerPlans.tempo(phases.map { number(it, 0, 30) }, number(rounds, 1, 10_000), continuous)
        } else WidgetTimerPlans.general(mode, number(seconds), number(rest), number(rounds, 1, 10_000))
    } }
    val plan = outcome.getOrNull()
    LaunchedEffect(plan?.signature) { if (plan != null) clock.configure(plan) else clock.reset() }
    VisibleWidgetClock(clock, sound, pollMillis = if(tempo) 20 else 100)
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(if (tempo) "Cue each lifting phase or keep a steady beat. Reps are cued, not detected." else "Timing only; use the tally for completed rounds or reps.") }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (if (tempo) listOf("Tempo", "Metronome") else listOf("Countdown", "Stopwatch", "Intervals", "EMOM", "AMRAP")).forEach { value ->
                FilterChip(selected = mode == value, enabled = !state.running, onClick = { mode = value }, label = { Text(value) })
            }
        } }
        if (tempo) {
            if (mode == "Tempo") phases.forEachIndexed { i, value -> item { WidgetNumberField(value, { phases = phases.toMutableList().also { list -> list[i] = it } },
                listOf("Lower", "Bottom pause", "Lift", "Top pause")[i] + " (0–30 seconds)", whole = true, enabled = !state.running) } }
            else item { WidgetNumberField(bpm, { bpm = it }, "Beats per minute (30–300)", whole = true, enabled = !state.running) }
            item { Row { Checkbox(continuous, { continuous = it }, enabled = !state.running); Text("Continuous (up to 24 hours)") } }
        } else {
            if (mode in listOf("Countdown", "Intervals", "AMRAP")) item { WidgetNumberField(seconds, { seconds = it }, "${if (mode == "Intervals") "Work" else "Duration"} (seconds, 1–86400)", whole = true, enabled = !state.running) }
            if (mode == "Intervals") item { WidgetNumberField(rest, { rest = it }, "Rest (seconds, 1–86400)", whole = true, enabled = !state.running) }
        }
        if ((!tempo && mode in listOf("Intervals", "EMOM")) || tempo && !continuous) item { WidgetNumberField(rounds, { rounds = it }, if (tempo) "Reps / beats (1–10000)" else "Rounds (1–10000)", whole = true, enabled = !state.running) }
        item { Row { Switch(sound, { sound = it }); Text("Sound cues • uses media volume") } }
        if (plan == null) item { Text(outcome.exceptionOrNull()?.message.orEmpty(), color = MaterialTheme.colorScheme.error) }
        else item { WidgetClockControls(clock, plan, elapsedDisplay = mode == "Stopwatch") }
        item { Text("Keep this screen visible. Leaving, locking the phone or a delayed cue pauses the clock and marks an interruption. Changing settings resets a paused timer.", style = MaterialTheme.typography.bodySmall) }
        if (state.laps.isNotEmpty()) items(state.laps.size) { i -> Text("Lap ${i + 1}: ${WidgetTimerPlans.clockText(state.laps[i])} elapsed • ${WidgetTimerPlans.clockText(state.laps[i] - (state.laps.getOrNull(i - 1) ?: 0))} split") }
    }
}

@Composable
internal fun VisibleWidgetClock(clock: WidgetClockModel, sound: Boolean = true, pollMillis: Long = 100) {
    val state by clock.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    SideEffect { clock.sound = sound }
    DisposableEffect(view, owner) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) clock.pause(interrupted = true) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); view.keepScreenOn = previous; clock.pause(interrupted = true) }
    }
    LaunchedEffect(state.running,pollMillis) { while (state.running) { delay(pollMillis); clock.tick() } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WidgetClockControls(clock: WidgetClockModel, plan: TimerPlan, elapsedDisplay: Boolean = false, strict: Boolean = false,
    showTally: Boolean = true, showLaps: Boolean = elapsedDisplay, onFinish: (() -> Unit)? = null) {
    val state by clock.state.collectAsStateWithLifecycle()
    val position = plan.at(state.elapsed)
    Text(if (position.complete) "Finished" else "${plan.phases[position.phase].label} • ${position.round}/${plan.rounds}", style = MaterialTheme.typography.titleMedium)
    Text(WidgetTimerPlans.clockText(if (elapsedDisplay) state.elapsed else position.remaining + 999), style = MaterialTheme.typography.displayMedium)
    if (state.interrupted) Text("Interrupted • resumed timing is practice only.", color = MaterialTheme.colorScheme.error)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { if (state.running) clock.pause(interrupted = strict) else clock.start() }, enabled = !position.complete) { Text(if (state.running) "Pause" else if (state.elapsed == 0L) "Start" else "Resume") }
        OutlinedButton(onClick = { clock.reset() }, enabled = !state.running) { Text("Reset") }
        if (showLaps) OutlinedButton(onClick = clock::lap, enabled = state.running) { Text("Lap") }
        if (onFinish != null) OutlinedButton(onClick = { clock.pause(); onFinish() }, enabled = state.elapsed > 0) { Text("Finish event") }
    }
    if(showTally) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tally: ${state.tally}")
        TextButton(onClick = { clock.tally(1) }) { Text("+1") }
        TextButton(onClick = { clock.tally(-1) }, enabled = state.tally > 0) { Text("−1") }
    }
}
