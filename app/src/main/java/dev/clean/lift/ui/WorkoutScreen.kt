package dev.clean.lift.ui

import android.os.SystemClock
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(bottomBar: @Composable () -> Unit, model: WorkoutViewModel = viewModel(), onSettings: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    val previous by model.previous.collectAsStateWithLifecycle()
    val busy = operation == WorkoutOperation.Saving || operation == WorkoutOperation.Saved
    var page by rememberSaveable { mutableStateOf("main") }
    var entryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showPreviousSets by rememberSaveable(entryId) { mutableStateOf(false) }
    var referenceSetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var setId by rememberSaveable { mutableStateOf<Long?>(null) }
    var weight by rememberSaveable { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf("") }
    var unitName by rememberSaveable { mutableStateOf(WeightUnit.LB.name) }
    var originalWeight by rememberSaveable { mutableStateOf("") }
    var originalReps by rememberSaveable { mutableStateOf("") }
    var originalUnit by rememberSaveable { mutableStateOf(WeightUnit.LB.name) }
    var entryName by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var removalId by rememberSaveable { mutableStateOf<Long?>(null) }
    var discardTarget by rememberSaveable { mutableStateOf("main") }
    var addingOverride by rememberSaveable { mutableStateOf(false) }
    var removingOverride by rememberSaveable { mutableStateOf(false) }
    // Rest countdown end in elapsedRealtime millis; 0 = no rest showing. Survives rotation, not stored.
    var restEndsAt by rememberSaveable { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(restEndsAt) {
        while (restEndsAt > 0) {
            now = SystemClock.elapsedRealtime()
            if (RestTimer.expired(restEndsAt, now)) restEndsAt = 0L else delay(250)
        }
    }
    val focus = LocalFocusManager.current
    val editorEntry = state.active?.exercises?.firstOrNull { it.exercise.id == entryId }
    val editorSets = editorEntry?.sets.orEmpty()
    val setNumber = setId?.let { id -> editorSets.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.plus(1) } ?: (editorSets.size + 1)
    val activeWorkout = state.active
    val guided = activeWorkout?.workout?.templateName != null
    val canAppend = !guided || setId != null || (editorEntry != null && GuidedPlan.canAppend(activeWorkout!!, editorEntry))
    val canNext = !guided || editorEntry?.let(GuidedPlan::canAdvance) == true
    val canFinish = activeWorkout?.let(GuidedPlan::canFinish) == true

    fun closeEditor() { page = "main"; dialog = null; focus.clearFocus(); model.resetOperation() }
    fun leaveEditor(target: String) {
        dialog = null; focus.clearFocus(); model.resetOperation()
        addingOverride = false
        if (target == "next" || target == "nextOverride") {
            if (guided) entryId?.let { model.advance(it, target == "nextOverride") }
            else { page = "picker"; query = "" }
            return
        }
        page = target
        if (target == "picker") query = ""
    }
    fun back(target: String = "main") {
        if (!busy) {
            discardTarget = target
            if (page == "set" && (weight != originalWeight || reps != originalReps || unitName != originalUnit)) dialog = "discard"
            else leaveEditor(target)
        }
    }
    fun openSet(entry: WorkoutExercise, set: WorkoutSet? = null) {
        model.resetOperation()
        entryId = entry.id; entryName = entry.exerciseName; setId = set?.id
        val previous = set ?: state.active?.exercises?.firstOrNull { it.exercise.id == entry.id }?.sets?.lastOrNull()
        weight = previous?.let { SetInput.weightText(it.weightMilliUnits) }
            ?: if (entry.catalogKey?.let(ExerciseCatalog::find)?.load == LoadConvention.BODYWEIGHT_ONLY) "0" else ""
        reps = previous?.reps?.toString() ?: entry.repMin?.toString().orEmpty()
        if (previous != null) unitName = previous.weightUnit.name
        originalWeight = weight; originalReps = reps; originalUnit = unitName
        page = "set"
    }
    fun saveSet(override: Boolean = false) {
        val values = SetInput.parse(weight, reps) ?: return
        val id = entryId ?: return
        if (canAppend || override) model.saveSet(id, setId, values, WeightUnit.valueOf(unitName), override)
    }
    fun copyPrevious() {
        if (previous.entryId != entryId) return
        val values = if (referenceSetId == null) previous.values
            else previous.sets.firstOrNull { it.setId == referenceSetId }?.values
        if (values == null) return
        weight = SetInput.weightText(values.weightMilliUnits); reps = values.reps.toString(); unitName = values.weightUnit.name
        dialog = null; model.resetOperation()
        // Keep the original baseline: copied values are an unsaved draft until Save set.
    }
    fun requestPrevious(id: Long? = null) {
        referenceSetId = id
        if (weight != originalWeight || reps != originalReps || unitName != originalUnit) {
            discardTarget = "reference"; dialog = "discard"
        } else copyPrevious()
    }
    LaunchedEffect(entryId, page) { if (page == "set") entryId?.let(model::loadPrevious) }

    LaunchedEffect(operation) {
        if (operation == WorkoutOperation.Saved) {
            when (model.lastChange) {
                WorkoutChange.SaveSet -> {
                    // A newly saved set starts rest; correcting an earlier set does not.
                    if (setId == null) {
                        now = SystemClock.elapsedRealtime()
                        restEndsAt = now + RestTimer.seconds(editorEntry?.exercise?.restSeconds) * 1000L
                    }
                    // Keep entered values ready for the next set; edits return to append mode.
                    setId = null; originalWeight = weight; originalReps = reps; originalUnit = unitName
                    dialog = null; focus.clearFocus(); model.resetOperation()
                }
                WorkoutChange.AddExercise -> {
                    addingOverride = false
                    model.lastAddedExercise?.let { openSet(it) } ?: closeEditor()
                }
                WorkoutChange.StartGuide -> model.lastAddedExercise?.let { openSet(it) } ?: closeEditor()
                WorkoutChange.GuidedNext -> {
                    if (model.lastAddedExercise != null) openSet(model.lastAddedExercise!!)
                    else leaveEditor("picker")
                }
                WorkoutChange.RemoveSet -> { dialog = null; model.resetOperation() }
                WorkoutChange.Other -> closeEditor()
            }
        }
    }
    BackHandler(enabled = page != "main" || busy) { back() }

    if (dialog != null) {
        val title = when (dialog) {
            "discard" -> R.string.discard_title
            "cancel" -> R.string.cancel_workout_title
            "exercise" -> R.string.remove_exercise_title
            else -> R.string.remove_set_title
        }
        val description = when (dialog) {
            "discard" -> R.string.discard_message
            "cancel" -> R.string.cancel_workout_message
            "exercise" -> R.string.remove_exercise_message
            else -> R.string.remove_set_message
        }
        AlertDialog(
            onDismissRequest = { if (!busy) dialog = null },
            title = { Text(stringResource(title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(description))
                    if (operation == WorkoutOperation.Failed) Text(stringResource(R.string.workout_save_failed), color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                when (dialog) {
                    "discard" -> {
                        if (discardTarget == "edit") {
                            editorSets.firstOrNull { it.id == removalId }?.let { set -> editorEntry?.let { openSet(it.exercise, set) } }
                            dialog = null
                        } else if (discardTarget == "reference") copyPrevious()
                        else leaveEditor(discardTarget)
                    }
                    "cancel" -> state.active?.let { model.cancel(it.workout.id) }
                    "exercise" -> removalId?.let { model.removeExercise(it, removingOverride) }
                    "set" -> removalId?.let(model::removeSet)
                }
            }) { Text(stringResource(if (dialog == "discard" || dialog == "cancel") R.string.discard else R.string.remove)) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { dialog = null }) { Text(stringResource(R.string.keep)) } }
        )
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(when (page) { "picker" -> R.string.add_exercise; "set" -> if (setId == null) R.string.add_set else R.string.edit_set; else -> R.string.workout })) },
                navigationIcon = { if (page != "main") BackButton(onClick = { back() }, enabled = !busy) },
                actions = {
                    if (page == "main") SettingsButton(onClick = onSettings, enabled = !busy)
                    if (page == "set") PlanAction(
                        stringResource(R.string.next_exercise), allowed = canNext, enabled = !busy && activeWorkout != null,
                        overrideAvailable = guided, onClick = { back("next") }, onOverride = { back("nextOverride") }
                    )
                    if (page == "picker") {
                        if (guided) PlanAction(stringResource(R.string.finish_workout), canFinish, !busy && (activeWorkout?.setCount ?: 0) > 0, true,
                            onClick = { activeWorkout?.let { model.finish(it.workout.id) } }, onOverride = { activeWorkout?.let { model.finish(it.workout.id, true) } })
                        else TextButton(enabled = !busy && canFinish, onClick = { activeWorkout?.let { model.finish(it.workout.id) } }) { Text(stringResource(R.string.finish_workout)) }
                    }
                }
            )
        },
        bottomBar = { if (page == "main" && !busy) bottomBar() }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            state.failed -> Column(Modifier.padding(padding).padding(24.dp)) {
                Text(stringResource(R.string.load_workout_failed))
                TextButton(onClick = model::load) { Text(stringResource(R.string.retry)) }
            }
            page == "set" -> LazyColumn(
                Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text(stringResource(R.string.exercise_set_title, entryName, setNumber), style = MaterialTheme.typography.titleLarge) }
                editorEntry?.exercise?.catalogKey?.let(ExerciseCatalog::find)?.let { catalog ->
                    item { Text(WorkoutGenerator.loadHint(catalog), style = MaterialTheme.typography.bodyMedium) }
                }
                if (guided) item {
                    Text(planDescription(editorEntry), style = MaterialTheme.typography.bodyMedium)
                    if (activeWorkout!!.workout.overrideCount > 0) Text(stringResource(R.string.overrides_count, activeWorkout.workout.overrideCount), color = MaterialTheme.colorScheme.tertiary)
                }
                if (setId == null && previous.entryId == entryId) {
                    if (previous.failed) item { TextButton(enabled = !busy, onClick = { entryId?.let(model::loadPrevious) }) { Text(stringResource(R.string.retry_previous)) } }
                    previous.values?.let { reference -> item {
                        Text(stringResource(R.string.previous_workout_values, SetInput.weightText(reference.weightMilliUnits), stringResource(if (reference.weightUnit == WeightUnit.LB) R.string.lb else R.string.kg), reference.reps, displayDate(reference.performedAt)), style = MaterialTheme.typography.bodyMedium)
                        TextButton(enabled = !busy, onClick = { requestPrevious() }) { Text(stringResource(R.string.use_previous)) }
                    } }
                }
                if (restEndsAt > 0) item(key = "rest") { RestCard(RestTimer.remaining(restEndsAt, now),
                    onExtend = { now = SystemClock.elapsedRealtime(); restEndsAt = RestTimer.extend(restEndsAt, now) }, onSkip = { restEndsAt = 0L }) }
                if (operation == WorkoutOperation.Failed) item { Text(stringResource(R.string.workout_save_failed), color = MaterialTheme.colorScheme.error) }
                item {
                    SetValueFields(
                        weight, reps, unitName, !busy,
                        onWeight = { weight = it; model.resetOperation() },
                        onReps = { reps = it; model.resetOperation() },
                        onUnit = { unitName = it; model.resetOperation() }, onSave = { if (state.active != null) saveSet() }
                    )
                }
                item {
                    PlanAction(stringResource(if (busy) R.string.saving else if (setId != null) R.string.save_changes else if (!canAppend && editorEntry?.exercise?.plannedSets?.let { editorSets.size >= it } == true) R.string.save_extra_set else R.string.save_set),
                        allowed = canAppend, enabled = !busy && SetInput.parse(weight, reps) != null && activeWorkout != null,
                        overrideAvailable = guided && setId == null, onClick = { saveSet() }, onOverride = { saveSet(true) })
                }
                if (previous.entryId == entryId) {
                    if (previous.loading) item { Text(stringResource(R.string.loading_previous_sets)) }
                    else if (!previous.failed && previous.sets.isEmpty()) item { Text(stringResource(R.string.no_previous_sets)) }
                    if (previous.sets.isNotEmpty()) {
                        item {
                            TextButton(onClick = { showPreviousSets = !showPreviousSets }) {
                                Text(stringResource(if (showPreviousSets) R.string.hide_previous_sets else R.string.show_previous_sets))
                            }
                        }
                        if (showPreviousSets) {
                            item { Text(stringResource(R.string.previous_sets_date, displayDate(previous.sets.first().values.performedAt)), style = MaterialTheme.typography.titleMedium) }
                            itemsIndexed(previous.sets, key = { _, set -> "previous-${set.setId}" }) { index, set ->
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.numbered_set, index + 1,
                                        stringResource(R.string.previous_set_values, SetInput.weightText(set.values.weightMilliUnits),
                                            stringResource(if (set.values.weightUnit == WeightUnit.LB) R.string.lb else R.string.kg), set.values.reps))) },
                                    supportingContent = { Text(stringResource(R.string.previous_set_position, set.entryPosition + 1, set.setPosition + 1)) },
                                    trailingContent = { if (setId == null) TextButton(enabled = !busy, onClick = { requestPrevious(set.setId) }) {
                                        Text(stringResource(R.string.copy_previous_set, index + 1))
                                    } }
                                )
                            }
                        }
                    }
                }
                if (editorSets.isNotEmpty()) item { Text(stringResource(R.string.saved_sets), style = MaterialTheme.typography.titleMedium) }
                itemsIndexed(editorSets.asReversed(), key = { _, set -> set.id }) { index, set ->
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.numbered_set, editorSets.size - index, setDescription(set))) },
                                                modifier = Modifier.clickable(enabled = !busy, onClickLabel = stringResource(R.string.edit_set)) {
                            // Do not replace changed input silently when selecting a saved set.
                            if (weight == originalWeight && reps == originalReps && unitName == originalUnit) editorEntry?.let { openSet(it.exercise, set) }
                            else { removalId = set.id; discardTarget = "edit"; dialog = "discard" }
                        }
                    )
                }
            }
            page == "picker" -> Column(Modifier.fillMaxSize().padding(padding)) {
                if (!canFinish) Text(stringResource(if (guided) R.string.guided_finish_guidance else R.string.finish_guidance), Modifier.padding(16.dp))
                if (guided) {
                    if (activeWorkout!!.workout.overrideCount > 0) Text(stringResource(R.string.overrides_count, activeWorkout.workout.overrideCount), Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.tertiary)
                    GuidedPlan.next(activeWorkout)?.let { next -> TextButton(enabled = !busy, onClick = { openSet(next.exercise) }) { Text(stringResource(R.string.resume_plan)) } }
                }
                if (operation == WorkoutOperation.Failed) Text(stringResource(R.string.workout_save_failed), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                OutlinedTextField(value = query, onValueChange = { query = it }, enabled = !busy,
                    label = { Text(stringResource(R.string.search_exercises)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp))
                val matches = state.catalog.filter { it.name.contains(query.trim(), ignoreCase = true) }
                if (matches.isEmpty()) Text(stringResource(R.string.no_matching_exercises), Modifier.padding(16.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(matches, key = { it.id }) { exercise ->
                        if (guided) PlanAction(exercise.name, addingOverride, !busy, true,
                            onClick = { activeWorkout?.let { model.addExercise(it.workout.id, exercise.id, true) } },
                            onOverride = { activeWorkout?.let { model.addExercise(it.workout.id, exercise.id, true) } })
                        else ListItem(headlineContent = { Text(exercise.name) }, modifier = Modifier.clickable(enabled = !busy) {
                            state.active?.let { model.addExercise(it.workout.id, exercise.id) }
                        })
                        HorizontalDivider()
                    }
                }
            }
            else -> {
                val active = state.active
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (operation == WorkoutOperation.Failed) item { Text(stringResource(R.string.workout_save_failed), color = MaterialTheme.colorScheme.error) }
                    if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    if (active == null) {
                        item {
                            Button(onClick = model::start, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                Text(stringResource(R.string.start_workout), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        state.latest?.let { latest ->
                            item { Text(stringResource(R.string.latest_workout), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
                            item { Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(latest.workout.finishedAt!!))) }
                            item { Text(stringResource(R.string.workout_saved_summary, latest.exercises.size, latest.setCount)) }
                            latest.exercises.forEach { entry ->
                                item(key = "finished-${entry.exercise.id}") { Text(entry.exercise.exerciseName, style = MaterialTheme.typography.titleMedium) }
                                items(entry.sets, key = { "finished-set-${it.id}" }) { set -> Text(setDescription(set)) }
                            }
                        } ?: item { Text(stringResource(R.string.start_workout_guidance)) }
                    } else {
                        item { Text(stringResource(R.string.active_workout), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
                        item { Text(stringResource(R.string.workout_started, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(active.workout.startedAt)))) }
                        if (restEndsAt > 0) item(key = "rest") { RestCard(RestTimer.remaining(restEndsAt, now),
                            onExtend = { now = SystemClock.elapsedRealtime(); restEndsAt = RestTimer.extend(restEndsAt, now) }, onSkip = { restEndsAt = 0L }) }

                        if (guided) {
                            item {
                                Text(active.workout.templateName!!, style = MaterialTheme.typography.titleMedium)
                                if (active.workout.overrideCount > 0) Text(stringResource(R.string.overrides_count, active.workout.overrideCount), color = MaterialTheme.colorScheme.tertiary)
                                Text(stringResource(R.string.hold_override), style = MaterialTheme.typography.bodySmall)
                            }
                            GuidedPlan.next(active)?.let { next -> item { Button(enabled = !busy, onClick = { openSet(next.exercise) }) { Text(stringResource(R.string.resume_plan)) } } }
                        }
                        if (active.exercises.isEmpty()) item { Text(stringResource(R.string.add_first_exercise)) }
                        item { PlanAction(stringResource(R.string.add_exercise), !guided, !busy, guided,
                            onClick = { addingOverride = false; query = ""; model.resetOperation(); page = "picker" },
                            onOverride = { addingOverride = true; query = ""; model.resetOperation(); page = "picker" }) }
                        active.exercises.asReversed().forEach { entry ->
                            item(key = "entry-${entry.exercise.id}") {
                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text(entry.exercise.exerciseName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                                    OutlinedButton(onClick = { openSet(entry.exercise) }, enabled = !busy) { Text(stringResource(R.string.add_set)) }
                                }
                                if (guided) Text(planDescription(entry), style = MaterialTheme.typography.bodySmall)
                                PlanAction(stringResource(R.string.remove_exercise), !guided, !busy, guided,
                                    onClick = { removingOverride = false; removalId = entry.exercise.id; dialog = "exercise" },
                                    onOverride = { removingOverride = true; removalId = entry.exercise.id; dialog = "exercise" })
                            }
                            itemsIndexed(entry.sets.asReversed(), key = { _, set -> "set-${set.id}" }) { index, set ->
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.numbered_set, entry.sets.size - index, setDescription(set))) },
                                                                        trailingContent = { TextButton(onClick = { removalId = set.id; dialog = "set" }, enabled = !busy) { Text(stringResource(R.string.remove)) } },
                                    modifier = Modifier.clickable(enabled = !busy, onClickLabel = stringResource(R.string.edit_set)) { openSet(entry.exercise, set) }
                                )
                            }
                        }
                        item {
                            PlanAction(stringResource(R.string.finish_workout), canFinish, !busy && active.setCount > 0,
                                overrideAvailable = guided, onClick = { model.finish(active.workout.id) }, onOverride = { model.finish(active.workout.id, true) })
                            if (!canFinish) Text(stringResource(if (guided) R.string.guided_finish_guidance else R.string.finish_guidance), style = MaterialTheme.typography.bodyMedium)
                        }
                        item { TextButton(onClick = { dialog = "cancel" }, enabled = !busy) { Text(stringResource(R.string.cancel_workout)) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun planDescription(entry: ExerciseWithSets?): String = (when {
    entry == null -> ""
    entry.exercise.completed -> stringResource(R.string.exercise_done)
    entry.exercise.plannedSets == null -> stringResource(R.string.until_done_guidance)
    else -> stringResource(R.string.fixed_sets_progress, entry.sets.size, entry.exercise.plannedSets)
}) + (entry?.exercise?.repMin?.let { minimum ->
    " • target $minimum–${entry.exercise.repMax} reps • rest ${entry.exercise.restSeconds ?: 120}s"
}.orEmpty()) + (entry?.exercise?.circuitRound?.let { " • ab circuit round $it of 2" }.orEmpty())
