package dev.clean.lift.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.clean.lift.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StrengthPreferencesScreen(state: MetricsState, model: MetricsViewModel, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var reps by rememberSaveable { mutableStateOf(state.repCount.toString()) }
    BackHandler(onBack = onBack)
    Scaffold(topBar = { TopAppBar(title = { Text("Strength benchmarks") }, navigationIcon = { BackButton(onClick = onBack) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Choose the exercises you want in Progress. Big 3 mappings also add their individual charts. Select distinct squat, bench and deadlift variants explicitly; the app never guesses from an exercise name.") }
            if (state.preferencesFailed) item { Text("Could not save this selection. Choose distinct exercises and try again.", color = MaterialTheme.colorScheme.error) }
            listOf("Squat", "Bench press", "Deadlift").forEachIndexed { index, title -> item {
                var expanded by remember { mutableStateOf(false) }
                val selected = state.exercises.firstOrNull { it.id == state.bigThree[index] }
                Box {
                    OutlinedButton(enabled = !state.preferencesSaving, onClick = { expanded = true }) { Text("$title: ${selected?.name ?: "Not set"}") }
                    DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Not set") }, onClick = { model.configure(bigThree = state.bigThree.toMutableList().also { it[index] = null }); expanded = false })
                        state.exercises.forEach { exercise -> DropdownMenuItem(text = { Text(exercise.name) },
                            enabled = exercise.id !in state.bigThree.filterIndexed { other, _ -> other != index },
                            onClick = { model.configure(bigThree = state.bigThree.toMutableList().also { it[index] = exercise.id }); expanded = false }) }
                    }
                }
            } }
            item { Text("Session total requires all three selected lifts in one completed workout. Personal-best sum can combine different workouts; it is labeled separately. Logged single and estimated totals remain separate.", style = MaterialTheme.typography.bodySmall) }
            item { OutlinedTextField(reps, { reps = it }, label = { Text("Rep-specific record • 1–30 reps") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                TextButton(enabled = !state.preferencesSaving && reps.toIntOrNull() in 1..30, onClick = { model.configure(repCount = reps.toInt()) }) { Text("Use this rep count (currently ${state.repCount})") } }
            item { Text("Estimated 1RM uses Epley on positive external loads for 2–10 reps. A logged single is shown directly. These estimates require near-maximal sets, consistent technique and load convention. They do not model bodyweight, assisted movements or machine differences.") }
            item { OutlinedTextField(query, { query = it }, label = { Text("Search exercises") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            items(state.exercises.filter { it.name.contains(query.trim(), ignoreCase = true) }, key = { it.id }) { exercise ->
                val forced = exercise.id in state.bigThree
                SettingsSwitch(exercise.name + if (forced) " • Big 3" else "", exercise.id in state.benchmarks || forced, !state.preferencesSaving && !forced) { checked ->
                    model.configure(benchmarks = if (checked) state.benchmarks + exercise.id else state.benchmarks - exercise.id)
                }
                HorizontalDivider()
            }
        }
    }
}
