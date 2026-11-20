package dev.clean.lift.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.clean.lift.data.*
import dev.clean.lift.widgets.WidgetGuides
import dev.clean.lift.widgets.WidgetProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MetricGuideScreen(series: MetricSeries, state: MetricsState, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val uri = LocalUriHandler.current
    var failed by remember { mutableStateOf(false) }
    fun link(url: String) { try { uri.openUri(url); failed = false } catch (_: Exception) { failed = true } }
    val latest = series.points.lastOrNull()?.let { point -> state.health.firstOrNull { it.id == point.recordId && series.kind != null } }
    Scaffold(topBar = { TopAppBar(title = { Text("About & research") }, navigationIcon = { BackButton(onClick = onBack) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(series.title, style = MaterialTheme.typography.titleLarge) }
            if (series.widgetProtocol != null) {
                val guide = WidgetGuides.forProtocol(WidgetProtocol.valueOf(series.widgetProtocol))
                item { Text("Equipment", style = MaterialTheme.typography.titleMedium); Text(guide.equipment) }
                item { Text(guide.protocol) }
                item { Text("These are recorded test results. Protocol versions stay separate. Interrupted and incomplete attempts are retained in Tools; only complete comparable attempts appear in these trends. Corrections: delete the attempt in Tools and enter a replacement. Cooper/Rockport estimates and raw performance remain separate.") }
                item { TextButton(onClick = { link(guide.sourceUrl) }) { Text(guide.sourceTitle) } }
            } else if (series.kind != null) {
                val guide = HealthGuides.forKind(series.kind, latest?.gender ?: state.gender)
                item { Text("Required equipment & data", style = MaterialTheme.typography.titleMedium); Text(guide.requirements) }
                item { Text("Measurement / test protocol", style = MaterialTheme.typography.titleMedium); Text(guide.protocol) }
                item { Text("Calculation", style = MaterialTheme.typography.titleMedium); Text(guide.formula) }
                item { Text("Interpretation", style = MaterialTheme.typography.titleMedium); Text(guide.interpretation) }
                if (state.references && latest != null && series.key.startsWith("health.")) item { Text(HealthCalculations.reference(latest)) }
                if (series.key.startsWith("ffmass.")) item { Text("Fat-free mass includes water, bone, organs and muscle. It is not a muscle-mass measurement.") }
                item { Text("Research • opens your browser", style = MaterialTheme.typography.titleMedium) }
                items(guide.sources) { source -> TextButton(onClick = { link(source.url) }) { Text(source.title) } }
            } else if (series.exerciseId != null || series.key.startsWith("big3.")) {
                item { Text("Required equipment & data", style = MaterialTheme.typography.titleMedium)
                    Text("The equipment used for the selected lift and completed workout logs with weight, units and repetitions. Relative views also require bodyweight on the workout's local calendar day.") }
                item { Text("Keep technique, range of motion, equipment and weight-recording convention consistent. A logged single is not necessarily a tested maximum. Machine displays and per-hand dumbbell values are not total external barbell loads.") }
                item { Text("Epley estimated 1RM = load × (1 + reps/30), using positive 2–10-rep sets. Near-maximal effort is assumed. Bodyweight and assistance are not included in this equation. Each chosen lift also offers separate 5/10-rep estimate tools and a break scenario above its recorded values.") }
                if (series.key.startsWith("big3.")) item { Text("Session totals require all three selected lifts in one completed workout. Personal-best sums can combine different dates; their component dates are shown in the chart list. Estimated and logged-single totals remain separate. These are training summaries, not judged competition results.") }
                if (state.references) item { Text("A cohort study in men linked combined machine bench/leg-press strength with lower all-cause and cancer mortality. That result does not provide healthy/athletic thresholds for an arbitrary exercise or Big 3 total.") }
                listOf(
                    ResearchLink("BMJ • strength and mortality in men", "https://www.bmj.com/content/337/bmj.a439"),
                    ResearchLink("Review • 1RM measurement reliability", "https://pubmed.ncbi.nlm.nih.gov/32681399/"),
                    ResearchLink("Research • uncertainty in 1RM predictions", "https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0288649")
                ).forEach { source -> item { TextButton(onClick = { link(source.url) }) { Text(source.title) } } }
            } else {
                item { Text("Required equipment & data", style = MaterialTheme.typography.titleMedium)
                    Text(if (series.bodyMetric == BodyMetric.BODY_WEIGHT) "Bathroom scale. Measure under similar conditions to compare trends." else if (series.bodyMetric == BodyMetric.HEIGHT) "Height measurement without shoes, standing upright against a suitable height measure." else "Non-stretch tape. Use the same site, posture and tape tension each time; measure without compressing skin.") }
                item { Text("Older entries do not record a measurement technique. Waist landmarks differ between clinical midpoint, hip-crest, natural-waist and navel protocols. Check the calculated metric's instructions before reusing a value.") }
                if (series.bodyMetric == BodyMetric.WAIST) {
                    if (state.references) item { Text("NHLBI uses waist above 40 in for men and 35 in for women as increased-risk context for heart disease and diabetes, with measurement just above the hip bones. Thresholds and applicability vary by population.") }
                    item { TextButton(onClick = { link("https://www.nhlbi.nih.gov/health/heart-healthy-living/healthy-weight") }) { Text("NHLBI • measurement and health outcomes") } }
                }
            }
            item { Text("Instructions and calculations work offline. Papers and government pages open externally and may require an Internet connection in your browser.", style = MaterialTheme.typography.bodySmall) }
            if (failed) item { Text("No browser could open the link.", color = MaterialTheme.colorScheme.error) }
        }
    }
}
