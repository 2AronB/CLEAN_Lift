package dev.clean.lift.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.R
import dev.clean.lift.data.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TemplatesScreen(onBack: () -> Unit, onStart: (Long) -> Unit, bottomBar: @Composable () -> Unit, onSettings: () -> Unit = {}, model: TemplatesViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    val duplicate by model.duplicate.collectAsStateWithLifecycle()
    val busy = operation == WorkoutOperation.Saving || operation == WorkoutOperation.Saved
    var page by rememberSaveable { mutableStateOf("list") }
    if (page == "exercises") {
        ExercisesScreen(onBack = { page = "list" }, bottomBar = {})
        return
    }
    var id by rememberSaveable { mutableStateOf<Long?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    // Primitive saved-state rows retain builder drafts across rotation/process recreation.
    val rowsSaver = remember { listSaver<List<String>, String>(save = { it }, restore = { it.toList() }) }
    var rows by rememberSaveable(stateSaver = rowsSaver) { mutableStateOf(emptyList<String>()) }
    var originalName by rememberSaveable { mutableStateOf("") }
    var originalRows by rememberSaveable(stateSaver = rowsSaver) { mutableStateOf(emptyList<String>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    if (page == "generator") {
        GeneratorScreen(model, onBack = { page = "list"; model.reset() })
        return
    }
    fun close() { page = "list"; dialog = null; model.reset() }
    fun back() {
        if (busy) return
        when (page) {
            "picker" -> page = "edit"
            "edit" -> if (name != originalName || rows != originalRows) dialog = "discard" else close()
            else -> onBack()
        }
    }
    fun edit(template: TemplateWithExercises?) {
        id = template?.template?.id; name = template?.template?.name.orEmpty()
        rows = ArrayList(template?.exercises?.sortedBy { it.position }?.map { "${it.exerciseId}:${it.plannedSets ?: ""}:${it.repMin ?: ""}:${it.repMax ?: ""}:${it.restSeconds ?: ""}:${it.circuitRound ?: ""}" }.orEmpty())
        originalName = name; originalRows = ArrayList(rows); page = "edit"; model.reset()
    }
    fun replace(index: Int, value: String) {
        val targets = rows[index].split(':').drop(2)
        rows = ArrayList(rows).also { it[index] = (value.split(':').take(2) + targets).joinToString(":") }; model.reset()
    }
    fun move(index: Int, offset: Int) {
        rows = ArrayList(rows).also { val row = it.removeAt(index); it.add(index + offset, row) }; model.reset()
    }
    fun replaceTarget(index: Int, field: Int, value: String) {
        rows = ArrayList(rows).also { list ->
            val parts = list[index].split(':').toMutableList()
            while (parts.size < 5) parts.add("")
            parts[field] = value; list[index] = parts.joinToString(":")
        }; model.reset()
    }
    val parsed = rows.map { row ->
        val parts = row.split(':')
        PlannedExercise(parts[0].toLong(), if (parts[1].isEmpty()) null else parts[1].toIntOrNull() ?: 0,
            parts.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: 0 },
            parts.getOrNull(3)?.takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: 0 },
            parts.getOrNull(4)?.takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: 0 },
            parts.getOrNull(5)?.takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: 0 })
    }
    val valid = ExerciseNames.valid(name) && parsed.isNotEmpty() && parsed.all {
        (it.sets == null || it.sets in 1..GuidedPlan.MAX_SETS) &&
            ((it.repMin == null && it.repMax == null) || (it.repMin != null && it.repMax != null && it.repMin in 1..SetInput.MAX_REPS && it.repMax in it.repMin..SetInput.MAX_REPS)) &&
            (it.restSeconds == null || it.restSeconds in 15..600) && (it.circuitRound == null || it.circuitRound in 1..2)
    }
    val copyName = stringResource(R.string.copy_workout_name, ExerciseNames.clean(name).take(90))
    fun save() { if (valid && !busy) model.save(id, name, parsed) }
    BackHandler { back() }
    LaunchedEffect(operation) { if (operation == WorkoutOperation.Saved) close() }
    if (dialog != null) AlertDialog(
        onDismissRequest = { if (!busy) dialog = null },
        title = { Text(stringResource(if (dialog == "delete") R.string.delete_template_title else R.string.discard_title)) },
        text = { Text(stringResource(if (dialog == "delete") R.string.delete_template_message else R.string.discard_message)) },
        confirmButton = { TextButton(enabled = !busy, onClick = { if (dialog == "delete") id?.let(model::delete) else close() }) {
            Text(stringResource(if (dialog == "delete") R.string.remove else R.string.discard))
        } },
        dismissButton = { TextButton(enabled = !busy, onClick = { dialog = null }) { Text(stringResource(R.string.keep_editing)) } }
    )
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = { TopAppBar(
            title = { Text(stringResource(if (page == "picker") R.string.add_exercise else if (page == "edit") R.string.workout_builder else R.string.templates)) },
            navigationIcon = { if (page != "list") BackButton(enabled = !busy, onClick = { back() }) },
            actions = { if (page == "list") SettingsButton(onClick = onSettings, enabled = !busy)
                if (page == "edit") TextButton(enabled = !busy && valid, onClick = { save() }) { Text(stringResource(if (busy) R.string.saving else R.string.save)) } }
        ) },
        bottomBar = { if (page == "list" && !busy) bottomBar() }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            state.failed -> Column(Modifier.padding(padding).padding(24.dp)) {
                Text(stringResource(R.string.load_failed))
                TextButton(onClick = model::load) { Text(stringResource(R.string.retry)) }
            }
            page == "picker" -> Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                    label = { Text(stringResource(R.string.search_exercises)) }, modifier = Modifier.fillMaxWidth().padding(16.dp))
                val matches = state.exercises.filter { it.name.contains(query.trim(), ignoreCase = true) }
                if (matches.isEmpty()) Text(stringResource(R.string.no_matching_exercises), Modifier.padding(16.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(matches, key = { it.id }) { exercise ->
                        ListItem(headlineContent = { Text(exercise.name) }, modifier = Modifier.clickable {
                            rows = ArrayList(rows).also { it.add("${exercise.id}:3") }; page = "edit"; model.reset()
                        })
                        HorizontalDivider()
                    }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (operation == WorkoutOperation.Failed) item { Text(stringResource(if (duplicate) R.string.duplicate_template else R.string.save_failed), color = MaterialTheme.colorScheme.error) }
                if (page == "list") {
                    item { OutlinedButton(onClick = { page = "exercises" }) { Text("Exercise library") } }
                    item { Button(enabled = !busy, onClick = { edit(null) }) { Text(stringResource(R.string.new_template)) } }
                    item { OutlinedButton(enabled = !busy, onClick = { model.reset(); page = "generator" }) { Text("Generate workout") } }
                    if (state.active) item { Text(stringResource(R.string.active_before_template)) }
                    if (state.templates.isEmpty()) item { Text(stringResource(R.string.templates_guidance)) }
                    items(state.templates, key = { it.template.id }) { template ->
                        ListItem(
                            headlineContent = { Text(template.template.name) },
                            supportingContent = { Text(stringResource(R.string.template_summary, template.exercises.size)) },
                            trailingContent = { TextButton(enabled = !state.active && !busy, onClick = { onStart(template.template.id) }) { Text(stringResource(R.string.start)) } },
                            modifier = Modifier.clickable(enabled = !busy, onClickLabel = stringResource(R.string.edit_template)) { edit(template) }
                        )
                        HorizontalDivider()
                    }
                } else {
                    item { OutlinedTextField(value = name, onValueChange = { name = it; model.reset() }, enabled = !busy, singleLine = true,
                        label = { Text(stringResource(R.string.template_name)) }, supportingText = { Text(stringResource(R.string.name_guidance)) },
                        isError = name.isNotEmpty() && !ExerciseNames.valid(name), modifier = Modifier.fillMaxWidth()) }
                    item { Text(stringResource(R.string.builder_guidance)) }
                    if (id != null && state.templates.firstOrNull { it.template.id == id }?.template?.generatorSession != null)
                        item { Text("Saving edits in this builder makes this a manual template. Completed workouts already saved keep their generated session label.", style = MaterialTheme.typography.bodySmall) }
                    itemsIndexed(rows) { index, encoded ->
                        val row = parsed[index]
                        val suffix = encoded.split(':')[1]
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider()
                            Text("${index + 1}. ${state.exercises.firstOrNull { it.id == row.exerciseId }?.name.orEmpty()}", style = MaterialTheme.typography.titleMedium)
                            if (row.repMin != null) Text("Target: ${row.repMin}–${row.repMax} reps • rest ${row.restSeconds ?: 120}s", style = MaterialTheme.typography.bodySmall)
                            row.circuitRound?.let { Text("Ab circuit • round $it of 2", style = MaterialTheme.typography.labelLarge) }
                            if (encoded.split(':').drop(2).any { it.isNotEmpty() }) {
                                listOf("Minimum reps", "Maximum reps", "Rest seconds").forEachIndexed { field, title ->
                                    OutlinedTextField(value = encoded.split(':').getOrNull(field + 2).orEmpty(), enabled = !busy,
                                        onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) replaceTarget(index, field + 2, it) },
                                        label = { Text(title) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                                Text("Rep targets must be ordered; rest is 15–600 seconds. Targets do not limit what you can log.", style = MaterialTheme.typography.bodySmall)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(selected = row.sets != null, enabled = !busy, onClick = { if (row.sets == null) replace(index, "${row.exerciseId}:3") }, label = { Text(stringResource(R.string.fixed_sets)) })
                                FilterChip(selected = row.sets == null, enabled = !busy, onClick = { replace(index, "${row.exerciseId}:") }, label = { Text(stringResource(R.string.until_done)) })
                            }
                            if (row.sets != null) OutlinedTextField(value = if (suffix == "?") "" else suffix, onValueChange = { replace(index, "${row.exerciseId}:${it.ifEmpty { "?" }}") }, enabled = !busy,
                                label = { Text(stringResource(R.string.planned_sets)) }, singleLine = true,
                                isError = row.sets !in 1..GuidedPlan.MAX_SETS, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(), supportingText = { Text(stringResource(R.string.set_count_guidance)) })
                            FlowRow {
                                TextButton(enabled = !busy && index > 0, onClick = { move(index, -1) }) { Text(stringResource(R.string.move_up)) }
                                TextButton(enabled = !busy && index < rows.lastIndex, onClick = { move(index, 1) }) { Text(stringResource(R.string.move_down)) }
                                TextButton(enabled = !busy, onClick = { rows = ArrayList(rows).also { it.removeAt(index) }; model.reset() }) { Text(stringResource(R.string.remove)) }
                            }
                        }
                    }
                    item { OutlinedButton(enabled = !busy && rows.size < 100, onClick = { query = ""; page = "picker" }) { Text(stringResource(R.string.add_exercise)) } }
                    item { Button(enabled = !busy && valid, onClick = { save() }) { Text(stringResource(if (busy) R.string.saving else R.string.save_template)) } }
                    if (id != null) item { OutlinedButton(enabled = !busy, onClick = {
                        id = null; name = copyName
                        originalName = ""; originalRows = emptyList(); model.reset()
                    }) { Text(stringResource(R.string.duplicate_workout)) } }
                    if (id != null) item { TextButton(enabled = !busy, onClick = { dialog = "delete" }) { Text(stringResource(R.string.delete_template)) } }
                }
            }
        }
    }
}
