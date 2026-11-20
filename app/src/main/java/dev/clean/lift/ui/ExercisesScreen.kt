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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.R
import dev.clean.lift.data.ExerciseNames
import dev.clean.lift.data.ExerciseCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(onBack: () -> Unit, bottomBar: @Composable () -> Unit, model: ExercisesViewModel = viewModel()) {
    val list by model.list.collectAsStateWithLifecycle()
    val result by model.save.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var original by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val saving = result == SaveResult.Saving

    fun close() { editing = false; confirmDiscard = false; model.resetSave() }
    fun leave() {
        if (saving) return
        if (name != original) confirmDiscard = true else close()
    }
    fun save() { model.save(editingId, name) }

    LaunchedEffect(result) { if (result == SaveResult.Saved) close() }
    BackHandler { if (editing) leave() else onBack() }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_message)) },
            confirmButton = { TextButton(onClick = { close() }) { Text(stringResource(R.string.discard)) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) } }
        )
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (!editing) R.string.exercises else if (editingId == null) R.string.new_exercise else R.string.edit_exercise)) },
                navigationIcon = {
                    if (editing) TextButton(onClick = { leave() }, enabled = !saving) { Text(stringResource(R.string.cancel)) }
                },
                actions = {
                    if (editing) TextButton(onClick = { save() }, enabled = !saving && ExerciseNames.valid(name)) {
                        Text(stringResource(if (saving) R.string.saving else R.string.save))
                    }
                }
            )
        },
        bottomBar = { if (!editing) bottomBar() },
        floatingActionButton = {
            if (!editing && !list.loading && !list.failed) {
                ExtendedFloatingActionButton(onClick = {
                    model.resetSave(); editingId = null; original = ""; name = ""; editing = true
                }) { Text(stringResource(R.string.new_exercise)) }
            }
        }
    ) { padding ->
        when {
            editing -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val error = when (result) {
                    SaveResult.Duplicate -> R.string.duplicate_exercise
                    SaveResult.Failed -> R.string.save_failed
                    SaveResult.Invalid -> R.string.invalid_exercise
                    else -> null
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; if (!saving) model.resetSave() },
                    label = { Text(stringResource(R.string.exercise_name)) },
                    singleLine = true,
                    enabled = !saving,
                    isError = error != null || name.length > ExerciseNames.MAX_LENGTH,
                    supportingText = { Text(stringResource(error ?: R.string.name_guidance)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!saving && ExerciseNames.valid(name)) save() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            list.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            list.failed -> Column(Modifier.padding(padding).padding(24.dp)) {
                Text(stringResource(R.string.load_failed))
                TextButton(onClick = { model.load() }) { Text(stringResource(R.string.retry)) }
            }
            list.exercises.isEmpty() -> Column(Modifier.padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.exercises_empty_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.exercises_empty_description))
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
                item { OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(stringResource(R.string.search_exercises)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp)) }
                val matches = list.exercises.filter { it.name.contains(query.trim(), true) }
                if (matches.isEmpty()) item { Text(stringResource(R.string.no_matching_exercises), Modifier.padding(16.dp)) }
                items(matches, key = { it.id }) { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = { ExerciseCatalog.find(exercise.catalogKey.orEmpty())?.let { entry ->
                            Text(entry.primary.joinToString { it.name.lowercase().replace('_', ' ') } + " • " +
                                entry.equipmentAlternatives.joinToString(" or ") { group -> group.joinToString(" + ") { it.name.lowercase().replace('_', ' ') } })
                        } },
                        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.edit_exercise)) {
                            model.resetSave(); editingId = exercise.id; original = exercise.name; name = exercise.name; editing = true
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
