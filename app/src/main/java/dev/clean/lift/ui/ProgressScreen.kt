package dev.clean.lift.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.R
import dev.clean.lift.data.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgressScreen(onBack: () -> Unit, onWorkout: (Long) -> Unit, model: ProgressViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var unitName by rememberSaveable { mutableStateOf(WeightUnit.LB.name) }
    var metric by rememberSaveable { mutableStateOf("weight") }
    var repFilter by rememberSaveable { mutableIntStateOf(0) }
    var period by rememberSaveable { mutableIntStateOf(0) }
    var showScenario by rememberSaveable { mutableStateOf(false) }
    var monthlyLoss by rememberSaveable { mutableFloatStateOf(0f) }
    var shown by rememberSaveable { mutableIntStateOf(50) }
    val now = remember(selectedId) { System.currentTimeMillis() }
    val unit = WeightUnit.valueOf(unitName)
    val unitLabel = stringResource(if (unit == WeightUnit.KG) R.string.kg else R.string.lb)
    val reps = repFilter.takeIf { it != 0 }
    val estimates = state.points.mapNotNull { point -> StrengthEstimates.estimate(point, reps)?.let { point to it } }
    val best = estimates.maxByOrNull { it.second }
    val latest = estimates.lastOrNull()
    val visible = state.points.filter { period == 0 || it.performedAt >= now - period * StrengthEstimates.DAY_MILLIS }
    val values = visible.mapNotNull { point ->
        val value = if (metric == "weight") point.heaviestKg else StrengthEstimates.estimate(point, reps)
        value?.let { TrendValue(point.performedAt, StrengthEstimates.inUnit(it, unit)) }
    }
    fun back() { if (selectedId != null) selectedId = null else onBack() }
    BackHandler { back() }
    LaunchedEffect(selectedId) { model.select(selectedId) }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.exercise_progress)) },
        navigationIcon = { BackButton(onClick = { back() }) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                state.loading -> item { CircularProgressIndicator() }
                state.failed -> item {
                    Text(stringResource(R.string.load_workout_failed))
                    TextButton(onClick = { model.load(); model.select(selectedId) }) { Text(stringResource(R.string.retry)) }
                }
                selectedId == null -> {
                    item { OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(stringResource(R.string.search_exercises)) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                    val matches = state.exercises.filter { it.name.contains(query.trim(), ignoreCase = true) }
                    if (matches.isEmpty()) item { Text(stringResource(R.string.no_matching_exercises)) }
                    items(matches, key = { it.id }) { exercise -> ListItem(headlineContent = { Text(exercise.name) }, modifier = Modifier.clickable { selectedId = exercise.id; shown = 50 }) }
                }
                else -> {
                    item { Text(state.exercises.firstOrNull { it.id == selectedId }?.name.orEmpty(), style = MaterialTheme.typography.titleLarge) }
                    item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        WeightUnit.entries.forEach { choice -> FilterChip(selected = unit == choice, onClick = { unitName = choice.name }, label = { Text(stringResource(if (choice == WeightUnit.KG) R.string.kg else R.string.lb)) }) }
                    } }
                    if (state.points.isEmpty()) item { Text(stringResource(R.string.progress_empty)) }
                    else {
                        item { Text(stringResource(R.string.heaviest_record, displayNumber(StrengthEstimates.inUnit(state.points.maxOf { it.heaviestKg }, unit)), unitLabel), style = MaterialTheme.typography.titleMedium) }
                        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0, 5, 10).forEach { count -> FilterChip(selected = repFilter == count, onClick = { repFilter = count }, label = { Text(stringResource(if (count == 0) R.string.five_ten_reps else if (count == 5) R.string.five_reps else R.string.ten_reps)) }) }
                        } }
                        if (best != null) item { Text(stringResource(R.string.estimated_record, displayNumber(StrengthEstimates.inUnit(best.second, unit)), unitLabel, displayDate(best.first.performedAt))) }
                        else item { Text(stringResource(R.string.no_estimates)) }
                        item { Text(stringResource(R.string.estimate_guidance), style = MaterialTheme.typography.bodySmall) }
                        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(selected = metric == "weight", onClick = { metric = "weight" }, label = { Text(stringResource(R.string.heaviest_weight)) })
                            FilterChip(selected = metric == "estimate", onClick = { metric = "estimate" }, label = { Text(stringResource(R.string.estimated_one_rm)) })
                        } }
                        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0, 90, 365).forEach { days -> FilterChip(selected = period == days, onClick = { period = days; shown = 50 }, label = { Text(stringResource(if (days == 0) R.string.all_time else if (days == 90) R.string.last_90_days else R.string.last_year)) }) }
                        } }
                        item { TrendChart(values, unitLabel, stringResource(if (metric == "weight") R.string.heaviest_weight else R.string.estimated_one_rm)) }
                        item { Text(stringResource(R.string.progress_chart_guidance), style = MaterialTheme.typography.bodySmall) }
                        latest?.let { recent ->
                            item {
                                Text(stringResource(R.string.latest_estimate, displayNumber(StrengthEstimates.inUnit(recent.second, unit)), unitLabel, displayDate(recent.first.performedAt)))
                                TextButton(onClick = { showScenario = !showScenario }) { Text(stringResource(R.string.break_scenario)) }
                            }
                            if (showScenario) item {
                                Text(stringResource(R.string.scenario_guidance))
                                Text(stringResource(R.string.monthly_loss, displayNumber(monthlyLoss.toDouble())))
                                Slider(value = monthlyLoss, onValueChange = { monthlyLoss = it }, valueRange = 0f..10f, steps = 19)
                                val days = StrengthEstimates.ageDays(recent.first.performedAt, now)
                                val adjusted = StrengthEstimates.scenario(recent.second, days, monthlyLoss.toDouble())
                                Text(stringResource(R.string.scenario_result, days, displayNumber(StrengthEstimates.inUnit(adjusted, unit)), unitLabel))
                            }
                        }
                        item { Text(stringResource(R.string.logged_sessions), style = MaterialTheme.typography.titleMedium) }
                        items(visible.asReversed().take(shown), key = { it.workoutId }) { point ->
                            ListItem(headlineContent = { Text(displayDate(point.performedAt)) },
                                supportingContent = { Column {
                                    Text("${displayNumber(StrengthEstimates.inUnit(point.heaviestKg, unit))} $unitLabel")
                                    if (metric == "estimate") StrengthEstimates.estimate(point, reps)?.let { estimate ->
                                        Text(stringResource(R.string.session_estimate, displayNumber(StrengthEstimates.inUnit(estimate, unit)), unitLabel))
                                    }
                                } },
                                modifier = Modifier.clickable(onClickLabel = stringResource(R.string.workout_details)) { onWorkout(point.workoutId) })
                        }
                        if (visible.size > shown) item { TextButton(onClick = { shown += 50 }) { Text(stringResource(R.string.show_more)) } }
                    }
                }
            }
        }
    }
}
