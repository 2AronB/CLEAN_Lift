package dev.clean.lift.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.data.*
import dev.clean.lift.widgets.WidgetAttemptRules

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MetricsHome(onBack: () -> Unit, bottomBar: @Composable () -> Unit, onSettings: () -> Unit = {}, model: MetricsViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val operation by model.operation.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("overview") }
    var key by rememberSaveable { mutableStateOf("health.BMI") }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var period by rememberSaveable { mutableIntStateOf(0) }
    var from by rememberSaveable { mutableStateOf("") }
    var until by rememberSaveable { mutableStateOf(MeasurementInput.today()) }
    var compareA by rememberSaveable { mutableStateOf("body.BODY_WEIGHT") }
    var compareB by rememberSaveable { mutableStateOf("body.WAIST") }
    var normalized by rememberSaveable { mutableStateOf(false) }
    var selectedRecord by rememberSaveable { mutableStateOf<Long?>(null) }
    var shown by rememberSaveable { mutableIntStateOf(50) }
    var historyId by rememberSaveable { mutableStateOf<Long?>(null) }
    var historyOrigin by rememberSaveable { mutableStateOf("overview") }
    var strengthOrigin by rememberSaveable { mutableStateOf("overview") }
    var category by rememberSaveable { mutableStateOf("All") }
    var recordedOnly by rememberSaveable { mutableStateOf(false) }
    val child = rememberSaveableStateHolder()
    val all = remember(state.body, state.health, state.widgetAttempts, state.imperial, state.strength, state.exercises, state.benchmarks, state.bigThree, state.repCount) {
        WidgetAttemptRules.integrate(MetricSeriesBuilder.build(state.body, state.health, state.imperial), state.widgetAttempts) +
            StrengthMetrics.series(state.exercises, state.strength, state.benchmarks + state.bigThree.filterNotNull(), state.body, state.imperial, state.repCount) +
            StrengthMetrics.bigThree(state.strength, state.bigThree, state.imperial)
    }
    val ordered = all.sortedBy { state.order.indexOf(it.key).takeIf { index -> index >= 0 } ?: (state.order.size + all.indexOf(it)) }
    val visible = ordered.filter { it.key !in state.hidden && it.group !in state.hiddenGroups }
    val overview = visible.filter { (category == "All" || it.group == category) && (!recordedOnly || it.points.isNotEmpty()) }
    val series = all.firstOrNull { it.key == key } ?: all.first()
    val today = MeasurementInput.parseDay(MeasurementInput.today())!!
    val start = if (period == -1) MeasurementInput.parseDay(from) else if (period == 0) 0L else today - period + 1
    val end = if (period == -1) MeasurementInput.parseDay(until) else today
    val validRange = start != null && end != null && start <= end
    fun filtered(points: List<MetricPoint>) = if (validRange) points.filter { it.day in start!!..end!! } else emptyList()
    fun back() { if (page == "overview") onBack() else page = "overview" }
    BackHandler { back() }
    when (page) {
        "history" -> { child.SaveableStateProvider("history-${historyId ?: "list"}") { HistoryScreen(onBack = { page = historyOrigin }, bottomBar = {}, initialWorkoutId = historyId) }; return }
        "strength-settings" -> { StrengthPreferencesScreen(state, model, onBack = { page = strengthOrigin }); return }
        "daily" -> { DailyMeasurementsScreen(state.body, state.imperial, onBack = { page = "overview" }); return }
        "about" -> { MetricGuideScreen(series, state, onBack = { page = "detail" }); return }
        "body" -> {
            val metric = series.bodyMetric ?: BodyMetric.BODY_WEIGHT
            val measurementModel: MeasurementsViewModel = viewModel()
            val measurementOperation by measurementModel.operation.collectAsStateWithLifecycle()
            if (state.loading) { CircularProgressIndicator(); return }
            val record = editId?.let { id -> state.body.firstOrNull { it.id == id && it.metric == metric } }
            if (editId != null && record == null && measurementOperation !in listOf(WorkoutOperation.Saving, WorkoutOperation.Saved)) {
                LaunchedEffect(editId) { measurementModel.reset(); page = "detail" }; return
            }
            MeasurementsScreen(onBack = { page = "detail" }, model = measurementModel, initialMetric = metric, initialRecord = record, startInEditor = true,
                initialUnit = if (metric == BodyMetric.BODY_WEIGHT) { if (state.imperial) MeasurementUnit.LB else MeasurementUnit.KG }
                    else if (state.imperial) MeasurementUnit.IN else MeasurementUnit.CM)
            return
        }
        "entry" -> {
            val kind = series.kind ?: HealthKind.BMI
            val record = editId?.let { id -> state.health.firstOrNull { it.id == id } }
            if (state.loading) { CircularProgressIndicator(); return }
            if (editId != null && record == null && operation != WorkoutOperation.Saved) {
                // A concurrently deleted record must never be recreated as a new one.
                LaunchedEffect(editId) { page = "detail"; model.reset() }; return
            }
            HealthEntryScreen(kind, record, state, operation, model::save, model::delete, model::reset, onBack = { page = "detail" })
            return
        }
    }
    Scaffold(modifier = Modifier.imePadding(), topBar = { TopAppBar(title = { Text(when (page) { "settings" -> "Customize Progress"; "detail" -> series.title; "compare" -> "Compare metrics"; else -> "Progress" }) },
        navigationIcon = { if (page != "overview") BackButton(onClick = { back() }) },
        actions = { if (page == "overview") { TextButton(onClick = { page = "settings" }) { Text("Customize") }; SettingsButton(onClick = onSettings) } }) },
        bottomBar = { if (page == "overview") bottomBar() }) { padding ->
        child.SaveableStateProvider("content-$page-${if (page == "detail") series.key else "page"}") {
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                state.loading -> item { CircularProgressIndicator() }
                state.failed -> item { Text("Could not load metrics."); TextButton(onClick = model::load) { Text("Retry") } }
                page == "settings" -> {
                    item { Text("Hide metrics without deleting data. Group visibility preserves your individual choices and also applies to newly selected lifts. All options remain available here.") }
                    item { OutlinedButton(onClick = { strengthOrigin = "settings"; page = "strength-settings" }) { Text("Choose strength benchmarks & Big 3") } }
                    if (state.preferencesFailed) item { Text("Could not save settings. Try again.", color = MaterialTheme.colorScheme.error) }
                    item { SettingsSwitch("Use lb / inches", state.imperial, !state.preferencesSaving) { model.configure(imperial = it) } }
                    item { SettingsSwitch("Show sourced reference context and chart markers", state.references, !state.preferencesSaving) { model.configure(references = it) } }
                    item { SettingsSwitch("Show 7-day recorded-weight average", state.smoothing, !state.preferencesSaving) { model.configure(smoothing = it) } }
                    item { Text("The average uses measurements actually recorded in the preceding seven calendar days. Raw points stay visible.", style = MaterialTheme.typography.bodySmall) }
                    item { TextButton(enabled = !state.preferencesSaving, onClick = { model.configure(hidden = emptySet(), hiddenGroups = emptySet(), order = emptyList()) }) { Text("Show all metrics & reset order") } }
                    ordered.map { it.group }.distinct().forEach { group -> item {
                        SettingsSwitch("Show $group", group !in state.hiddenGroups, !state.preferencesSaving) { show ->
                            model.configure(hiddenGroups = if (show) state.hiddenGroups - group else state.hiddenGroups + group)
                        }
                    } }
                    items(ordered, key = { "setting-${it.key}" }) { metric ->
                        Column {
                            SettingsSwitch(metric.title, metric.key !in state.hidden, !state.preferencesSaving && metric.group !in state.hiddenGroups) { show ->
                                model.configure(hidden = if (show) state.hidden - metric.key else state.hidden + metric.key)
                            }
                            val index = ordered.indexOf(metric)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(enabled = index > 0 && !state.preferencesSaving, onClick = {
                                    val order = ordered.map { it.key }.toMutableList(); order.add(index - 1, order.removeAt(index)); model.configure(order = order)
                                }) { Text("Move up") }
                                TextButton(enabled = index < ordered.lastIndex && !state.preferencesSaving, onClick = {
                                    val order = ordered.map { it.key }.toMutableList(); order.add(index + 1, order.removeAt(index)); model.configure(order = order)
                                }) { Text("Move down") }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                page == "overview" -> {
                    item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { historyId = null; historyOrigin = "overview"; page = "history" }) { Text("Workout history") }
                        OutlinedButton(onClick = { page = "compare" }) { Text("Compare") }
                        OutlinedButton(onClick = { strengthOrigin = "overview"; page = "strength-settings" }) { Text("Choose lifts") }
                        OutlinedButton(onClick = { page = "daily" }) { Text("Daily measurements") }
                    } }
                    item { Text("Workouts are logged in Workout. Review them in Workout history; their strength trends update automatically. Choose a body metric to record a measurement or result.", style = MaterialTheme.typography.bodySmall) }
                    item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf("All") + visible.map { it.group }.distinct()).forEach { group ->
                            FilterChip(selected = category == group, onClick = { category = group }, label = { Text(group) })
                        }
                    } }
                    item { SettingsSwitch("Only metrics with recorded data", recordedOnly, true) { recordedOnly = it } }
                    if (visible.isEmpty()) item { Text("All metrics are hidden. Use Customize to show them again.") }
                    else if (overview.isEmpty()) item {
                        Text("No metrics match these filters.")
                        TextButton(onClick = { category = "All"; recordedOnly = false }) { Text("Show all visible metrics") }
                    }
                    items(overview, key = { it.key }) { metric ->
                        val latest = metric.points.lastOrNull()
                        ListItem(headlineContent = { Text(metric.title) },
                            supportingContent = { Text(if (latest == null) "${metric.group} • No data yet" else "${displayNumber(latest.value)} ${metric.unit} • ${MeasurementInput.dayText(latest.day)}") },
                            trailingContent = { if (metric.points.size > 1) CompactMetricTrend(metric.points, metric.title) },
                            modifier = Modifier.clickable { key = metric.key; page = "detail"; shown = 50; selectedRecord = null })
                        if (metric.points.size > 1) Text("Change from first: ${displayNumber(metric.points.last().value - metric.points.first().value)} ${metric.unit}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp))
                        HorizontalDivider()
                    }
                }
                page == "compare" -> {
                    val (a, b) = MetricComparison.select(visible, compareA, compareB)
                    item { Text("Aligned charts share the same dates. Comparison does not establish cause and effect.") }
                    item { MetricPicker("First metric", a?.title ?: "No visible metrics", visible) { compareA = it.key } }
                    item { MetricPicker("Second metric", b?.title ?: "Choose another visible metric", visible.filter { it.key != a?.key }) { compareB = it.key } }
                    item { SettingsSwitch("Compare percentage change from first visible point", normalized, true) { normalized = it } }
                    item { PeriodControls(period, { period = it }, from, { from = it }, until, { until = it }) }
                    if (!validRange) item { Text("Enter valid dates, with start on or before end.", color = MaterialTheme.colorScheme.error) }
                    val times = (filtered(a?.points.orEmpty()) + filtered(b?.points.orEmpty())).map { it.day }
                    val window = if (times.isEmpty()) null else times.min() * StrengthEstimates.DAY_MILLIS..times.max() * StrengthEstimates.DAY_MILLIS
                    if (visible.isEmpty()) item { Text("All metrics are hidden. Show some in Customize to compare them.") }
                    else if (b == null) item { Text("Show at least two metrics in Customize to compare them.") }
                    listOfNotNull(a, b).forEach { metric -> item {
                        val raw = filtered(metric.points)
                        val points = if (normalized) MetricSeriesBuilder.normalized(raw) else raw
                        Text(metric.title, style = MaterialTheme.typography.titleMedium)
                        if (normalized && raw.firstOrNull()?.value == 0.0) Text("Percentage change is unavailable because the first value is zero. Turn off percentage change to see recorded values.")
                        TrendChart(points.map { TrendValue(it.day * StrengthEstimates.DAY_MILLIS, it.value) }, if (normalized) "% change" else metric.unit,
                            metric.title, window = window, dateLabel = { MeasurementInput.dayText(it / StrengthEstimates.DAY_MILLIS) })
                    } }
                    if (normalized) item { Text("Percentage-change baselines may have different dates. Each chart starts from its own first recorded point in this range.", style = MaterialTheme.typography.bodySmall) }
                }
                else -> {
                    item { OutlinedButton(onClick = { page = "about" }) { Text("About & research") } }
                    if (series.key in listOf("raw.COOPER","raw.ROCKPORT")) item { Text("Includes measured results from Tools and existing Progress entries. Widget attempts retain their original inputs; correct those in Tools > Saved attempts.") }
                    if (series.widgetProtocol != null) item { Text("Record or remove attempts in Tools. Interrupted/incomplete attempts remain there and are excluded from this trend.") }
                    else item { Button(onClick = {
                        when { series.bodyMetric != null -> { editId = null; page = "body" }
                            series.kind != null -> { editId = null; model.reset(); page = "entry" }
                            else -> { historyId = null; historyOrigin = "detail"; page = "history" } }
                    }) { Text(if (series.bodyMetric != null) "Add measurement" else if (series.kind != null) "Add result" else "Review workout history") } }
                    if (series.key.startsWith("big3.")) item { OutlinedButton(onClick = { strengthOrigin = "detail"; page = "strength-settings" }) { Text("Choose Big 3 lifts") } }
                    item { PeriodControls(period, { period = it; shown = 50 }, from, { from = it }, until, { until = it }) }
                    if (!validRange) item { Text("Enter valid dates, with start on or before end.", color = MaterialTheme.colorScheme.error) }
                    val points = filtered(series.points)
                    val latest = series.points.lastOrNull()
                    if (latest != null) item { Text("Latest: ${displayNumber(latest.value)} ${series.unit} • ${MeasurementInput.dayText(latest.day)}", style = MaterialTheme.typography.titleMedium) }
                    if (series.group in listOf("Strength", "Big 3")) series.points.maxByOrNull { it.value }?.let { best -> item {
                        Text("Highest in saved history: ${displayNumber(best.value)} ${series.unit} • ${MeasurementInput.dayText(best.day)}", style = MaterialTheme.typography.titleMedium)
                        Text("All-time record; the selected date range only filters the chart and list.", style = MaterialTheme.typography.bodySmall)
                    } }
                    if (points.isNotEmpty()) item { Text("Change in range: ${displayNumber(points.last().value - points.first().value)} ${series.unit}") }
                    val latestHealth = latest?.let { point -> state.health.firstOrNull { it.id == point.recordId && it.kind == series.kind } }
                    val marks = if (!state.references || !series.key.startsWith("health.")) emptyList() else when (series.kind) {
                        HealthKind.BMI -> if (latestHealth?.age != null && latestHealth.age >= 20) listOf(18.5,25.0,30.0,35.0,40.0) else emptyList()
                        HealthKind.WAIST_HEIGHT -> if (latestHealth?.age != null && latestHealth.age >= 18 && latestHealth.weightKg != null && latestHealth.heightCm != null && latestHealth.weightKg / (latestHealth.heightCm / 100).let { it * it } < 35) listOf(0.4,0.5,0.6) else emptyList()
                        HealthKind.WAIST_HIP -> if (latestHealth?.age != null && latestHealth.age >= 18 && latestHealth.gender.isNotEmpty()) listOf(if (latestHealth.gender == "M") 0.90 else 0.85) else emptyList()
                        HealthKind.RFM_FAT -> if (latestHealth?.age != null && latestHealth.age >= 20) listOf(if (latestHealth.gender == "M") 30.0 else 40.0) else emptyList()
                        else -> emptyList()
                    }
                    item { TrendChart(points.map { TrendValue(it.day * StrengthEstimates.DAY_MILLIS, it.value, it.recordId) }, series.unit, series.title,
                        referenceMarks = marks, onPoint = { tap -> selectedRecord = tap.recordId },
                        dateLabel = { MeasurementInput.dayText(it / StrengthEstimates.DAY_MILLIS) }) }
                    if (marks.isNotEmpty()) item { Text("Dashed reference markers: ${marks.joinToString { displayNumber(it) }} ${series.unit}. Based on the latest result's reference context; older results can have different age/gender/scope.", style = MaterialTheme.typography.bodySmall) }
                    if (state.smoothing && series.bodyMetric == BodyMetric.BODY_WEIGHT) item {
                        Text("7-day recorded-weight average", style = MaterialTheme.typography.titleMedium)
                        TrendChart(filtered(MetricSeriesBuilder.weightAverage(series.points)).map { TrendValue(it.day * StrengthEstimates.DAY_MILLIS, it.value) }, series.unit, "7-day recorded-weight average",
                            dateLabel = { MeasurementInput.dayText(it / StrengthEstimates.DAY_MILLIS) })
                    }
                    if (series.key.startsWith("ffmass.")) item { Text("Fat-free mass includes water, bone, organs and muscle. It is not muscle mass.") }
                    if (state.references && latestHealth != null && series.key.startsWith("health.")) item { Text(HealthCalculations.reference(latestHealth)) }
                    selectedRecord?.let { id -> points.firstOrNull { it.recordId == id }?.let { point -> item {
                        Text("Selected: ${MeasurementInput.dayText(point.day)} • ${displayNumber(point.value)} ${series.unit}", style = MaterialTheme.typography.titleMedium)
                    } } }
                    series.exerciseId?.let { exerciseId -> item(key = "lift-estimate-tools") {
                        LiftEstimateTools(exerciseId, state.strength, state.imperial, start, end, onWorkout = {
                            historyId = it; historyOrigin = "detail"; page = "history"
                        })
                    } }
                    item { Text(if (series.widgetProtocol != null) "Recorded attempts • tap for details; manage in Tools" else "Recorded values • tap a row to inspect/edit", style = MaterialTheme.typography.titleMedium) }
                    items(points.asReversed().take(shown), key = { "point-${it.recordId}" }) { point ->
                        val record = state.health.firstOrNull { it.id == point.recordId && it.kind == series.kind }
                        ListItem(headlineContent = { Text("${displayNumber(point.value)} ${series.unit}") },
                            supportingContent = { Column {
                                Text(MeasurementInput.dayText(point.day))
                                if (series.widgetProtocol != null || point.recordId < 0) state.widgetAttempts.firstOrNull { it.id == if(point.recordId < 0) -point.recordId else point.recordId }?.let { attempt ->
                                    Text("${attempt.mode} • ${attempt.version}", style = MaterialTheme.typography.bodySmall)
                                    if (selectedRecord == point.recordId) Text(attempt.details, style = MaterialTheme.typography.bodySmall)
                                }
                                record?.let {
                                    Text(healthInputSummary(it), style = MaterialTheme.typography.bodySmall)
                                    if (state.references && series.key.startsWith("health.")) Text(HealthCalculations.reference(it), style = MaterialTheme.typography.bodySmall)
                                }
                                if (series.strengthMetric in listOf(StrengthMetric.RELATIVE_SINGLE, StrengthMetric.RELATIVE_ESTIMATE)) {
                                    val weight = state.body.firstOrNull { it.metric == BodyMetric.BODY_WEIGHT && it.epochDay == point.day }
                                    weight?.let { Text("Bodyweight: ${displayNumber(MeasurementInput.convert(it, if (state.imperial) MeasurementUnit.LB else MeasurementUnit.KG))} ${if (state.imperial) "lb" else "kg"} • ${MeasurementInput.dayText(it.epochDay)}", style = MaterialTheme.typography.bodySmall) }
                                }
                                if (series.key.startsWith("big3.")) {
                                    val components = StrengthMetrics.bigThreeComponents(state.strength, state.bigThree, point.recordId,
                                        series.key.contains("ESTIMATED"), series.key.endsWith("BEST"))
                                    components.forEach { sample ->
                                        val value = if (series.key.contains("ESTIMATED")) StrengthMetrics.estimate(sample)!! else sample.weightKg
                                        Text("${state.exercises.firstOrNull { it.id == sample.exerciseId }?.name.orEmpty()}: ${displayNumber(StrengthEstimates.inUnit(value, if (state.imperial) WeightUnit.LB else WeightUnit.KG))} ${series.unit} • ${displayDate(sample.performedAt)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            } }, modifier = Modifier.clickable {
                                when { series.widgetProtocol != null || point.recordId < 0 -> selectedRecord = point.recordId
                                    series.bodyMetric != null -> { editId = point.recordId; page = "body" }
                                    series.kind != null -> { editId = point.recordId; model.reset(); page = "entry" }
                                    else -> { historyId = point.recordId; historyOrigin = "detail"; page = "history" } }
                            })
                        HorizontalDivider()
                    }
                    if (points.size > shown) item { TextButton(onClick = { shown += 50 }) { Text("Show more") } }

                }
            }
        }
        }
    }
}

@Composable
private fun CompactMetricTrend(points: List<MetricPoint>, label: String) {
    val low = points.minOf { it.value }; val high = points.maxOf { it.value }
    val range = (high - low).takeIf { it > 0 } ?: maxOf(1.0, high * 0.1)
    val first = points.first().day; val last = points.last().day
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.width(64.dp).height(28.dp).semantics { contentDescription = "$label trend, ${points.size} recorded values. Open for dates and exact values." }) {
        val inset = 3.dp.toPx(); val width = size.width - inset * 2; val height = size.height - inset * 2
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = inset + width * (if (last == first) 0.5 else (point.day - first).toDouble() / (last - first)).toFloat()
            val y = inset + height * (1 - (point.value - low) / range).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color, 1.5.dp.toPx(), Offset(x, y))
        }
        drawPath(path, color, style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
internal fun SettingsSwitch(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, onChange, enabled = enabled, modifier = Modifier.semantics { contentDescription = label })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PeriodControls(period: Int, onPeriod: (Int) -> Unit, from: String, onFrom: (String) -> Unit, until: String, onUntil: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(30 to "1 month", 90 to "3 months", 365 to "1 year", 0 to "All", -1 to "Custom").forEach { (days, title) ->
            FilterChip(selected = period == days, onClick = { onPeriod(days) }, label = { Text(title) })
        }
    }
    if (period == -1) {
        MeasurementDateField(from, onFrom, label = "From")
        MeasurementDateField(until, onUntil, label = "Through")
    }
}

@Composable
private fun MetricPicker(label: String, selected: String, choices: List<MetricSeries>, onSelect: (MetricSeries) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { OutlinedButton(enabled = choices.isNotEmpty(), onClick = { expanded = true }) { Text("$label: $selected") }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) { choices.forEach { metric ->
            DropdownMenuItem(text = { Text(metric.title) }, onClick = { onSelect(metric); expanded = false })
        } }
    }
}

private fun healthInputSummary(r: HealthRecord): String = buildList {
    r.heightCm?.let { add("height ${displayNumber(it)} cm") }; r.weightKg?.let { add("weight ${displayNumber(it)} kg") }
    r.waistCm?.let { add("waist ${displayNumber(it)} cm") }; r.abdomenCm?.let { add("abdomen ${displayNumber(it)} cm") }
    r.neckCm?.let { add("neck ${displayNumber(it)} cm") }; r.hipsCm?.let { add("hips ${displayNumber(it)} cm") }
    r.timeSeconds?.let { add("time ${HealthCalculations.timeText(it)}") }; r.distanceMetres?.let { add("distance ${displayNumber(it)} m") }
    r.heartRate?.let { add("pulse $it bpm") }; r.age?.let { add("age $it") }; if (r.gender.isNotEmpty()) add("profile ${r.gender}")
    add("formula v${r.formulaVersion}"); if (r.notes.isNotEmpty()) add(r.notes)
}.joinToString(" • ")
