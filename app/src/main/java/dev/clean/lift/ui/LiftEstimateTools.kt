package dev.clean.lift.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.clean.lift.data.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LiftEstimateTools(exerciseId: Long, samples: List<StrengthSample>, imperial: Boolean,
                               start: Long?, end: Long?, onWorkout: (Long) -> Unit) {
    var expanded by rememberSaveable(exerciseId) { mutableStateOf(false) }
    var repCount by rememberSaveable(exerciseId) { mutableIntStateOf(0) }
    var scenario by rememberSaveable(exerciseId) { mutableStateOf(false) }
    var loss by rememberSaveable(exerciseId) { mutableFloatStateOf(0f) }
    var shown by rememberSaveable(exerciseId) { mutableIntStateOf(20) }
    val points = remember(samples, exerciseId) { StrengthEstimates.progress(samples, exerciseId) }
    val estimates = remember(points, repCount) { points.mapNotNull { point ->
        StrengthEstimates.estimate(point, repCount.takeIf { it != 0 })?.let { point to it }
    } }
    val unit = if (imperial) WeightUnit.LB else WeightUnit.KG
    fun value(kg: Double) = "${displayNumber(StrengthEstimates.inUnit(kg, unit))} ${unit.name.lowercase()}"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide 5/10-rep tools" else "5/10-rep estimates & break scenario") }
        if (expanded) {
            Text("Separate 5/10-rep view", style = MaterialTheme.typography.titleMedium)
            Text("Uses only positive-load sets of exactly 5 or 10 reps. The main estimated-1RM view uses 2–10 reps. These methods stay separate.", style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "5 & 10 reps", 5 to "5 reps", 10 to "10 reps").forEach { (reps, title) ->
                    FilterChip(selected = repCount == reps, onClick = { repCount = reps; shown = 20 }, label = { Text(title) })
                }
            }
            estimates.maxByOrNull { it.second }?.let { best -> Text("All-time estimated best: ${value(best.second)} • ${displayDate(best.first.performedAt)}") }
            val ranged = estimates.filter { (point, _) ->
                val day = MeasurementInput.parseDay(MeasurementInput.today(point.performedAt))
                start != null && end != null && day != null && day in start..end
            }
            Text("Chart and sessions use the date range selected above.", style = MaterialTheme.typography.bodySmall)
            TrendChart(ranged.map { TrendValue(it.first.performedAt, StrengthEstimates.inUnit(it.second, unit)) }, unit.name.lowercase(), "5/10-rep estimated 1RM")
            estimates.lastOrNull()?.let { recent ->
                Text("Latest eligible estimate: ${value(recent.second)} • ${displayDate(recent.first.performedAt)}")
                TextButton(onClick = { scenario = !scenario }) { Text(if (scenario) "Hide break scenario" else "Break scenario • what if?") }
                if (scenario) {
                    Text("Choose an assumed monthly loss. This is a what-if calculation, not a prediction or a recommended training load. No app records does not mean no exercise.")
                    Text("Assumed monthly loss: ${displayNumber(loss.toDouble())}%")
                    Slider(loss, { loss = it }, valueRange = 0f..10f, steps = 19)
                    val days = StrengthEstimates.ageDays(recent.first.performedAt, System.currentTimeMillis())
                    Text("Scenario after $days days: ${value(StrengthEstimates.scenario(recent.second, days, loss.toDouble()))}")
                }
            }
            if (estimates.isEmpty()) Text("No positive-load 5- or 10-rep sets in completed workouts yet.")
            ranged.asReversed().take(shown).forEach { (point, estimate) ->
                TextButton(onClick = { onWorkout(point.workoutId) }, modifier = Modifier.fillMaxWidth()) {
                    Text("${displayDate(point.performedAt)} • estimated ${value(estimate)} • Review workout")
                }
            }
            if (ranged.size > shown) TextButton(onClick = { shown += 20 }) { Text("Show more estimates") }
        }
    }
}
