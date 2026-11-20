package dev.clean.lift.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.R
import dev.clean.lift.data.*
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, bottomBar: @Composable () -> Unit, model: HistoryViewModel = viewModel(), initialWorkoutId: Long? = null) {
    val state by model.state.collectAsStateWithLifecycle()
    val detail by model.detail.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    val busy = operation == WorkoutOperation.Saving || operation == WorkoutOperation.Saved
    var selectedId by rememberSaveable { mutableStateOf<Long?>(initialWorkoutId) }
    var setId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exerciseName by rememberSaveable { mutableStateOf("") }
    var setNumber by rememberSaveable { mutableIntStateOf(1) }
    var weight by rememberSaveable { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf("") }
    var unitName by rememberSaveable { mutableStateOf(WeightUnit.LB.name) }
    var originalWeight by rememberSaveable { mutableStateOf("") }
    var originalReps by rememberSaveable { mutableStateOf("") }
    var originalUnit by rememberSaveable { mutableStateOf(WeightUnit.LB.name) }
    var discard by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current

    fun closeEditor() { setId = null; discard = false; focus.clearFocus(); model.resetOperation() }
    fun back() {
        if (busy) return
        if (setId != null) {
            if (weight != originalWeight || reps != originalReps || unitName != originalUnit) discard = true
            else closeEditor()
        } else if (selectedId != null) {
            if (initialWorkoutId != null) { onBack(); return }
            selectedId = null
        }
        else onBack()
    }
    fun save() {
        val values = SetInput.parse(weight, reps) ?: return
        val workout = selectedId ?: return
        val set = setId ?: return
        model.correct(workout, set, values, WeightUnit.valueOf(unitName))
    }

    LaunchedEffect(selectedId) { model.select(selectedId) }
    LaunchedEffect(query) { model.search(query.trim()) }
    LaunchedEffect(operation) { if (operation == WorkoutOperation.Saved) closeEditor() }
    BackHandler { back() }
    if (discard) AlertDialog(
        onDismissRequest = { discard = false },
        title = { Text(stringResource(R.string.discard_title)) },
        text = { Text(stringResource(R.string.discard_message)) },
        confirmButton = { TextButton(onClick = { closeEditor() }) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.keep_editing)) } }
    )

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (setId != null) R.string.edit_set else if (selectedId != null) R.string.workout_details else R.string.history)) },
                navigationIcon = { BackButton(onClick = { back() }, enabled = !busy) }
            )
        },
        bottomBar = { if (selectedId == null) bottomBar() }
    ) { padding ->
        val workout = detail.workout
        when {
            selectedId != null && (detail.id != selectedId || detail.loading) -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            selectedId != null && detail.failed -> Column(Modifier.padding(padding).padding(24.dp)) {
                Text(stringResource(R.string.load_workout_failed))
                TextButton(onClick = { model.select(selectedId) }) { Text(stringResource(R.string.retry)) }
            }
            selectedId != null && workout == null -> Text(stringResource(R.string.workout_unavailable), Modifier.padding(padding).padding(24.dp))
            setId != null -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text(stringResource(R.string.exercise_set_title, exerciseName, setNumber), style = MaterialTheme.typography.titleLarge) }
                item { Text(stringResource(R.string.history_edit_guidance)) }
                if (operation == WorkoutOperation.Failed) item { Text(stringResource(R.string.workout_save_failed), color = MaterialTheme.colorScheme.error) }
                item { SetValueFields(weight, reps, unitName, !busy,
                    onWeight = { weight = it; model.resetOperation() }, onReps = { reps = it; model.resetOperation() },
                    onUnit = { unitName = it; model.resetOperation() }, onSave = { save() }) }
                item { Button(onClick = { save() }, enabled = !busy && SetInput.parse(weight, reps) != null) {
                    Text(stringResource(if (busy) R.string.saving else R.string.save_changes))
                } }
            }
            selectedId != null && workout != null -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(formatWorkoutDate(workout.workout.startedAt), style = MaterialTheme.typography.titleLarge) }
                item { Text(stringResource(R.string.workout_finished, formatWorkoutDate(workout.workout.finishedAt!!))) }
                item { Text(stringResource(R.string.workout_saved_summary, workout.exercises.size, workout.setCount)) }
                workout.workout.templateName?.let { name -> item { Text(name, style = MaterialTheme.typography.titleMedium) } }
                if (workout.workout.overrideCount > 0) item { Text(stringResource(R.string.overrides_count, workout.workout.overrideCount), color = MaterialTheme.colorScheme.tertiary) }
                workout.exercises.forEach { entry ->
                    item(key = "exercise-${entry.exercise.id}") {
                        HorizontalDivider()
                        Text(entry.exercise.exerciseName, Modifier.padding(top = 12.dp).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
                        entry.exercise.circuitRound?.let { Text("Ab circuit • round $it of 2", style = MaterialTheme.typography.bodySmall) }
                    }
                    if (entry.sets.isEmpty()) item(key = "empty-${entry.exercise.id}") { Text(stringResource(R.string.skipped_exercise)) }
                    itemsIndexed(entry.sets, key = { _, set -> "set-${set.id}" }) { index, set ->
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.numbered_set, index + 1, setDescription(set))) },
                            modifier = Modifier.clickable(onClickLabel = stringResource(R.string.edit_set)) {
                                model.resetOperation(); setId = set.id; exerciseName = entry.exercise.exerciseName; setNumber = index + 1
                                weight = SetInput.weightText(set.weightMilliUnits); reps = set.reps.toString(); unitName = set.weightUnit.name
                                originalWeight = weight; originalReps = reps; originalUnit = unitName
                            }
                        )
                    }
                }
            }
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            state.failed -> Column(Modifier.padding(padding).padding(24.dp)) {
                Text(stringResource(R.string.load_workout_failed))
                TextButton(onClick = model::load) { Text(stringResource(R.string.retry)) }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
                item { OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(stringResource(R.string.search_history)) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                if (state.workouts.isEmpty()) item {
                    Text(stringResource(if (query.isBlank()) R.string.history_empty_title else R.string.no_history_matches), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(if (query.isBlank()) R.string.history_empty_description else R.string.search_history_guidance), Modifier.padding(top = 8.dp))
                }
                items(state.workouts, key = { it.id }) { summary ->
                    ListItem(
                        headlineContent = { Text(summary.templateName ?: formatWorkoutDate(summary.startedAt)) },
                        supportingContent = { Column {
                            if (summary.templateName != null) Text(formatWorkoutDate(summary.startedAt))
                            Text(stringResource(R.string.workout_saved_summary, summary.exerciseCount, summary.setCount))
                        } },
                        modifier = Modifier.clickable { selectedId = summary.id }
                    )
                    HorizontalDivider()
                }
                if (state.hasMore) item { TextButton(onClick = model::more) { Text(stringResource(R.string.show_more)) } }
            }
        }
    }
}

private fun formatWorkoutDate(value: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(value))
