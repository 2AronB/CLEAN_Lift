package dev.clean.lift.widgets

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.data.MeasurementInput
import kotlinx.coroutines.delay
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShuttlePage(modifier: Modifier, model: WidgetAttemptModel) {
    var hamr by rememberSaveable { mutableStateOf(true) }
    var score by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(MeasurementInput.today()) }
    var token by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var notes by rememberSaveable { mutableStateOf("") }
    val kind = if (hamr) WidgetProtocol.HAMR else WidgetProtocol.PACER
    val operation by model.operation.collectAsStateWithLifecycle()
    var progress by rememberSaveable { mutableStateOf(listOf(0,0,0,0,0,0)) }
    fun current() = ShuttleProgress(progress[0],progress[1],progress[2],progress[3],progress[4],progress[5] == 1)
    val audio: ShuttleAudioModel = viewModel(key = "widgets.shuttle")
    val state by audio.state.collectAsStateWithLifecycle()
    LaunchedEffect(kind) { audio.load(kind.name) }
    fun judge(reached: Boolean) {
        val next = current().judge(hamr, reached)
        progress = listOf(next.attempts,next.successful,next.lastSuccess,next.misses,next.consecutive,if (next.ended) 1 else 0)
        if (hamr) score = next.lastSuccess.toString()
        if (next.ended) audio.stop()
    }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { WidgetChoices(listOf("HAMR", "PACER"), if (hamr) "HAMR" else "PACER", enabled = !state.playing) {
            hamr = it == "HAMR"; progress = listOf(0,0,0,0,0,0); score = ""; token = UUID.randomUUID().toString(); model.clear()
        } }
        item { Text(if (hamr) "20 m lines. Touch the far line before the cue; wait there if early. Mark each shuttle after the recording's arrival cue. Three consecutive warnings end the test; a success resets warnings. Skipping a shuttle or turning before reaching the far line ends the test. Score is the last successful shuttle number."
            else "20 m FitnessGram recording only. Touch the far line by each beep. The second miss ends this version of the test, even if separated by successes. An observer must enter the final score from the official score sheet. This tool does not infer laps from audio or assign youth fitness grades.") }
        item { LocalShuttleAudio(audio, kind.name, onRecordingChanged = {
            progress = listOf(0,0,0,0,0,0); score = ""; token = UUID.randomUUID().toString(); model.clear()
        }) }
        item { Text("Attempted: ${current().attempts} • successful: ${current().successful} • last successful number: ${current().lastSuccess}")
            Text("Warnings: ${if (hamr) current().consecutive else current().misses}/${if (hamr) 3 else 2}")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { judge(true) }, enabled = state.playing && !current().ended && current().attempts < 1000) { Text("Reached line") }
                OutlinedButton(onClick = { judge(false) }, enabled = state.playing && !current().ended && current().attempts < 1000) { Text("Miss / early departure") }
            }
        }
        item { WidgetNumberField(score, { score = it }, "Final recorder score (shuttles, 0–1000)", whole = true, enabled = !state.playing) }
        item { AttemptDateField(date) { date = it } }
        item { OutlinedTextField(notes, { notes = it.take(1000) }, label = { Text("Course / recording version / notes") }, modifier = Modifier.fillMaxWidth()) }
        item {
            val value = score.toIntOrNull()?.takeIf { it in 0..1000 }
            val day = MeasurementInput.parseDay(date)
            Button(onClick = { model.save(WidgetAttempt(token = token, protocol = kind.name, version = kind.version, epochDay = day!!,
                result = value!!.toDouble(), complete = true, mode = if (state.ended) "Guided" else "Entered",
                details = "${kind.version}\n20 m; observer-entered score. Attempted ${current().attempts}, successful ${current().successful}, last success ${current().lastSuccess}, misses ${current().misses}.\n$notes",
                deviations = if (state.interrupted) "Recording interrupted; repeat for a comparable result." else "")) },
                enabled = value != null && day != null && !state.playing && !operation.busy && operation.savedToken != token) { Text("Save attempt to Progress") }
            Text(operation.message)
            TextButton(onClick = { audio.restart(kind.name); progress = listOf(0,0,0,0,0,0); score = ""; token = UUID.randomUUID().toString(); model.clear() }, enabled = !state.playing && !operation.busy) { Text("New attempt") }
        }
        item { WidgetSourceLink(if (hamr) "Air Force manual • Attachment 2, HAMR" else "Cooper Institute • PACER instructions", if (hamr) AirForceScoring.MANUAL_URL else "https://resources.finalsite.net/images/v1710865382/hebisdedu/mswuhakyitgev6btcucc/1_PACER_and_Mile_Run.pdf") }
    }
}

@Composable
internal fun LocalShuttleAudio(audio: ShuttleAudioModel, kind: String, keepAwake: Boolean = true, onRecordingChanged: () -> Unit = {}) {
    val state by audio.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = LocalView.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onRecordingChanged()
            audio.load(kind, uri)
        }
    }
    DisposableEffect(owner, view, audio, keepAwake) {
        val previous = view.keepScreenOn; if (keepAwake) view.keepScreenOn = true
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) audio.stop(interrupted = true) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); if (keepAwake) view.keepScreenOn = previous; audio.stop(interrupted = true) }
    }
    LaunchedEffect(state.playing) { while (state.playing) { delay(100); audio.tick() } }
    Text(state.message)
    Text("Recording elapsed: ${WidgetTimerPlans.clockText(state.position.toLong())}")
    OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }, enabled = !state.playing) { Text("Choose local official recording") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = audio::start, enabled = state.ready && !state.playing && !state.ended) { Text("Play from beginning") }
        OutlinedButton(onClick = { audio.stop() }, enabled = state.playing) { Text("End test") }
    }
    if (state.interrupted) Text("Audio interrupted. Saved practice remains in Tools but is excluded from comparable Progress trends.", color = MaterialTheme.colorScheme.error)
    Text("Keep this screen visible. Cues come from the selected recording; verify it is $kind 20 m. An observer counts/judges each arrival. Media volume must be audible.", style = MaterialTheme.typography.bodySmall)
}
