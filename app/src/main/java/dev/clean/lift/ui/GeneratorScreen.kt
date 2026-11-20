package dev.clean.lift.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.clean.lift.data.*

private fun encode(row: GeneratedExercise) = "${row.key}:${row.sets}:${row.repMin}:${row.repMax}:${row.restSeconds}:${row.circuitRound ?: ""}"
private fun decode(value: String): GeneratedExercise? {
    val parts = value.split(':')
    if (parts.size !in 5..6 || ExerciseCatalog.find(parts[0]) == null) return null
    return GeneratedExercise(parts[0], parts[1].toIntOrNull() ?: return null, parts[2].toIntOrNull() ?: return null,
        parts[3].toIntOrNull() ?: return null, parts[4].toIntOrNull() ?: return null, "Edited workout draft",
        parts.getOrNull(5)?.takeIf { it.isNotEmpty() }?.toIntOrNull())
}
private fun label(equipment: Equipment): String = when (equipment) {
    Equipment.DUMBBELL_PAIR -> "Dumbbell pair (also permits single dumbbell work)"
    Equipment.FLOOR_SPACE -> "Clear floor space"
    Equipment.SAFETIES -> "Rack / bench safety supports"
    Equipment.LOW_FIXED_BAR -> "Low fixed bar suitable for inverted rows"
    else -> equipment.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeneratorScreen(model: TemplatesViewModel, onBack: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    val duplicate by model.duplicate.collectAsStateWithLifecycle()
    val busy = operation == WorkoutOperation.Saving || operation == WorkoutOperation.Saved
    var styleName by rememberSaveable { mutableStateOf(GeneratorStyle.FULL_BODY.name) }
    val style = GeneratorStyle.valueOf(styleName)
    var sessionOverride by rememberSaveable { mutableStateOf("") }
    val suggested = WorkoutGenerator.nextSession(style, state.generatedHistory)
    val session = sessionOverride.takeIf { it in style.sessions } ?: suggested
    var locationName by rememberSaveable { mutableStateOf(model.savedLocation.name) }
    val location = WorkoutLocation.valueOf(locationName)
    var equipmentNames by rememberSaveable { mutableStateOf(model.savedEquipment(location).map { it.name }) }
    val equipment = equipmentNames.map { Equipment.valueOf(it) }.toSet().let {
        if (Equipment.DUMBBELL_PAIR in it) it + Equipment.DUMBBELL_SINGLE else it
    }
    var length by rememberSaveable { mutableIntStateOf(5) }
    var effort by rememberSaveable { mutableIntStateOf(3) }
    var swings by rememberSaveable { mutableStateOf(false) }
    var technical by rememberSaveable { mutableStateOf(false) }
    var compoundFirst by rememberSaveable { mutableStateOf(true) }
    var abCircuit by rememberSaveable { mutableStateOf(false) }
    var equipmentOpen by rememberSaveable { mutableStateOf(false) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var draftSession by rememberSaveable { mutableStateOf("") }
    var draftRows by rememberSaveable { mutableStateOf(listOf<String>()) }
    var gaps by rememberSaveable { mutableStateOf(listOf<String>()) }
    var reasons by rememberSaveable { mutableStateOf(listOf<String>()) }
    var name by rememberSaveable { mutableStateOf("") }
    var discard by rememberSaveable { mutableStateOf(false) }
    var replaceIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    val request = GeneratorRequest(style, session, equipment, length, effort, swings, technical, compoundFirst, abCircuit)
    val rows = draftRows.map(::decode)
    val valid = ExerciseNames.valid(name) && rows.isNotEmpty() && rows.all { row ->
        row != null && runCatching { WorkoutGenerator.validateRow(row) }.isSuccess
    } && WorkoutGenerator.uniqueRows(rows.filterNotNull())
    fun back() {
        if (busy) return
        when { replaceIndex != null -> replaceIndex = null; equipmentOpen -> equipmentOpen = false; preview -> discard = true; else -> onBack() }
    }
    fun replaceNumber(index: Int, part: Int, value: String) {
        draftRows = draftRows.toMutableList().also { list ->
            val fields = list[index].split(':').toMutableList(); fields[part] = value; list[index] = fields.joinToString(":")
        }; model.reset()
    }
    BackHandler { back() }
    LaunchedEffect(operation) { if (operation == WorkoutOperation.Saved) onBack() }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard generated draft?") },
        text = { Text("The draft has not been saved.") }, confirmButton = { TextButton(onClick = {
            discard = false; preview = false; model.reset()
        }) { Text("Discard") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
    Scaffold(modifier = Modifier.imePadding(), topBar = { TopAppBar(title = { Text(if (preview) "Review generated workout" else "Generate workout") },
        navigationIcon = { BackButton(enabled = !busy, onClick = { back() }) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (operation == WorkoutOperation.Failed) item { Text(if (duplicate) "A template with this name already exists. Choose another name." else "Could not save. Your draft is still here; try again.", color = MaterialTheme.colorScheme.error) }
            when {
                replaceIndex != null -> {
                    item { OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search alternatives") }, modifier = Modifier.fillMaxWidth()) }
                    item { Text("Choose an exercise that fits your equipment. Review its targets after replacing.") }
                    val replacing = rows[replaceIndex!!]
                    val choices = WorkoutGenerator.candidates(request).filter { entry ->
                        entry.name.contains(search.trim(), true) && rows.filterNotNull().none { it.key == entry.key && it.circuitRound == replacing?.circuitRound } &&
                            (replacing?.circuitRound == null || entry.primary.any { it in setOf(Muscle.ABS, Muscle.OBLIQUES) })
                    }
                    if (choices.isEmpty()) item { Text("No compatible alternatives. Change equipment or keep the current exercise.") }
                    items(choices, key = { it.key }) { entry ->
                        OutlinedButton(onClick = {
                            val index = replaceIndex!!
                            draftRows = draftRows.toMutableList().also { list ->
                                val fields = list[index].split(':').toMutableList(); fields[0] = entry.key; list[index] = fields.joinToString(":")
                            }; replaceIndex = null; model.reset()
                        }, modifier = Modifier.fillMaxWidth()) { Text(entry.name) }
                    }
                }
                equipmentOpen -> {
                    item { Text("${location.label} equipment. Check what is actually available. Each location is remembered separately on this device.") }
                    item { TextButton(onClick = { equipmentNames = emptyList(); model.rememberEquipment(location, emptySet()) }) { Text("Clear equipment") } }
                    items(Equipment.entries.filter { it !in setOf(Equipment.BOX, Equipment.CLEAR_PATH, Equipment.PLATFORM) }, key = { it.name }) { item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked = item in equipment, enabled = item != Equipment.DUMBBELL_SINGLE || Equipment.DUMBBELL_PAIR !in equipment, onCheckedChange = { checked ->
                                equipmentNames = if (checked) equipmentNames + item.name else equipmentNames - item.name
                                model.rememberEquipment(location, equipmentNames.map { Equipment.valueOf(it) }.toSet())
                            }); Text(label(item), Modifier.weight(1f))
                        }
                    }
                    item { Button(onClick = { equipmentOpen = false }) { Text("Done") } }
                }
                !preview -> {
                    item { Text("Build an editable workout using staple lifts and your available equipment. No schedule required.") }
                    item { Text("Workout style", style = MaterialTheme.typography.titleMedium) }
                    item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GeneratorStyle.entries.forEach { value -> FilterChip(selected = style == value, onClick = { styleName = value.name; sessionOverride = "" }, label = { Text(value.label) }) }
                    } }
                    item {
                        val previous = state.generatedHistory.firstOrNull { it.generatorSession in style.sessions }
                        Text(if (previous == null) "No completed generated session in this style. Start with ${WorkoutGenerator.sessionLabel(session)} or choose another." else "Last completed: ${WorkoutGenerator.sessionLabel(previous.generatorSession!!)}. Suggested next: ${WorkoutGenerator.sessionLabel(suggested)}.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            style.sessions.forEach { value -> FilterChip(selected = value == session, onClick = { sessionOverride = value }, label = { Text(WorkoutGenerator.sessionLabel(value)) }) }
                        }
                    }
                    item { Text("Location", style = MaterialTheme.typography.titleMedium); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkoutLocation.entries.forEach { value -> FilterChip(selected = value == location, onClick = {
                            locationName = value.name; equipmentNames = model.savedEquipment(value).map { it.name }; model.rememberLocation(value)
                        }, label = { Text(value.label) }) }
                    }; Text("Equipment is saved separately for each location.", style = MaterialTheme.typography.bodySmall) }
                    item { OutlinedButton(onClick = { equipmentOpen = true }) { Text("${location.label} equipment (${equipmentNames.size} selected)") } }
                    item { Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = technical, onCheckedChange = { technical = it })
                        Text("Include practiced lifts (squats, deadlifts, pull-ups, overhead presses). Review every selection for familiarity.", Modifier.weight(1f))
                    } }
                    item { Text("Maximum main exercises: $length", style = MaterialTheme.typography.titleMedium)
                        Slider(value = length.toFloat(), onValueChange = { length = kotlin.math.round(it).toInt() }, valueRange = 4f..10f, steps = 5)
                        Text("Uses fewer when this style or equipment has fewer suitable choices. The ab finisher is additional.", style = MaterialTheme.typography.bodySmall)
                    }
                    item { Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Switch(checked = compoundFirst, onCheckedChange = { compoundFirst = it }); Text("Compound lifts first", Modifier.weight(1f).padding(start = 8.dp))
                    }; Text("Prioritizes squats and hinges, then other compound work, then isolation. Opted-in swings come first. Turn off to use the preset sequence.", style = MaterialTheme.typography.bodySmall) }
                    item { Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Switch(checked = abCircuit, onCheckedChange = { abCircuit = it }); Text("Add ab circuit at the end", Modifier.weight(1f).padding(start = 8.dp))
                    }; Text("Three floor exercises, two rounds, counted by reps. Short rest targets; no countdown timer.", style = MaterialTheme.typography.bodySmall) }
                    item { Text("Effort today", style = MaterialTheme.typography.titleMedium); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Very light", "Light", "Normal", "Challenging", "Very challenging").forEachIndexed { index, label ->
                            FilterChip(selected = effort == index + 1, onClick = { effort = index + 1 }, label = { Text("${index + 1} $label") })
                        }
                    }; Text("Light choices reduce working sets; challenging choices add optional work. Weights stay yours to choose. 5×5 main lifts keep five sets; choose a lighter weight for a lighter day.", style = MaterialTheme.typography.bodySmall) }
                    if (style == GeneratorStyle.KETTLEBELL) item {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Checkbox(checked = swings, onCheckedChange = { swings = it }); Text("Include swings — I am familiar with them", Modifier.weight(1f)) }
                        Text("Straight sets counted by reps. Timed circuits, EMOMs and interval workouts are not included.", style = MaterialTheme.typography.bodySmall)
                    }
                    item { Button(onClick = {
                        val draft = WorkoutGenerator.generate(request)
                        draftSession = draft.session; draftRows = draft.rows.map(::encode); gaps = draft.gaps; reasons = draft.rows.map { it.reason }
                        val baseName = when (style) {
                            GeneratorStyle.FULL_BODY -> "Full body"
                            GeneratorStyle.PPL -> "Workout"
                            GeneratorStyle.BODYWEIGHT -> "Bodyweight"
                            GeneratorStyle.FIVE_BY_FIVE -> "5×5"
                            GeneratorStyle.KETTLEBELL -> "Kettlebell"
                        } + " ${WorkoutGenerator.sessionLabel(session)}"
                        val existingNames = state.templates.map { it.template.normalizedName }.toSet()
                        name = baseName
                        var suffix = 2
                        while (ExerciseNames.key(name) in existingNames) { name = "$baseName ($suffix)"; suffix++ }
                        preview = true; model.reset()
                    }) { Text("Generate draft") } }
                }
                else -> {
                    item { OutlinedTextField(value = name, enabled = !busy, onValueChange = { name = it; model.reset() }, label = { Text("Template name") }, isError = !ExerciseNames.valid(name), modifier = Modifier.fillMaxWidth(), singleLine = true) }
                    item { Text("Review sets, reps and rest. These are editable targets, not logged results. Choose working weights during the workout; no automatic weight increases.") }
                    if (gaps.isNotEmpty()) item { Text("Equipment and coverage notes:\n${gaps.joinToString("\n")}\nYou can save this workout. Review coverage after replacements.") }
                    if (draftRows.isEmpty()) item { Text("No exercises fit. Go Back to change equipment or workout style.") }
                    itemsIndexed(draftRows) { index, encoded ->
                        val parts = encoded.split(':'); val exercise = ExerciseCatalog.find(parts[0])!!
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(); Text("${index + 1}. ${exercise.name}", style = MaterialTheme.typography.titleMedium)
                            rows[index]?.circuitRound?.let { Text("Ab circuit • round $it of 2", style = MaterialTheme.typography.labelLarge) }
                            Text(WorkoutGenerator.loadHint(exercise), style = MaterialTheme.typography.bodySmall)
                            Text("Primary targets: ${exercise.primary.joinToString { it.name.lowercase().replace('_', ' ') }}", style = MaterialTheme.typography.bodySmall)
                            Text(reasons.getOrNull(index) ?: "Manual choice", style = MaterialTheme.typography.bodySmall)
                            val titles = listOf("Working sets", "Minimum reps", "Maximum reps", "Rest seconds")
                            titles.forEachIndexed { fieldIndex, title -> OutlinedTextField(value = parts[fieldIndex + 1], enabled = !busy,
                                onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) replaceNumber(index, fieldIndex + 1, it) }, label = { Text(title) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()) }
                            if (rows[index] == null || runCatching { WorkoutGenerator.validateRow(rows[index]!!) }.isFailure) Text("Use 1–99 sets, valid ordered rep targets, and 15–600 seconds rest.", color = MaterialTheme.colorScheme.error)
                            FlowRow {
                                TextButton(enabled = !busy && index > 0, onClick = {
                                    draftRows = draftRows.toMutableList().also { val row = it.removeAt(index); it.add(index - 1, row) }; reasons = emptyList(); model.reset()
                                }) { Text("Move up") }
                                TextButton(enabled = !busy && index < draftRows.lastIndex, onClick = {
                                    draftRows = draftRows.toMutableList().also { val row = it.removeAt(index); it.add(index + 1, row) }; reasons = emptyList(); model.reset()
                                }) { Text("Move down") }
                                TextButton(enabled = !busy, onClick = { replaceIndex = index; search = "" }) { Text("Replace") }
                                TextButton(enabled = !busy, onClick = { draftRows = draftRows.filterIndexed { i, _ -> i != index }; reasons = emptyList(); model.reset() }) { Text("Remove") }
                            }
                        }
                    }
                    item { Button(enabled = valid && !busy, onClick = { model.saveGenerated(name, draftSession, rows.filterNotNull()) }) { Text(if (busy) "Saving…" else "Save new template") } }
                }
            }
        }
    }
}
