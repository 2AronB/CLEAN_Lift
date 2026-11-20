package dev.clean.lift.data

data class MetricPoint(val day: Long, val value: Double, val recordId: Long)
data class MetricSeries(val key: String, val title: String, val unit: String, val group: String,
                        val points: List<MetricPoint>, val kind: HealthKind? = null, val bodyMetric: BodyMetric? = null,
                        val exerciseId: Long? = null, val strengthMetric: StrengthMetric? = null, val widgetProtocol: String? = null)

fun bodyMetricTitle(metric: BodyMetric): String = when (metric) {
    BodyMetric.BODY_WEIGHT -> "Bodyweight"; BodyMetric.HEIGHT -> "Height"; BodyMetric.WAIST -> "Waist"
    BodyMetric.CHEST -> "Chest"; BodyMetric.HIPS -> "Hips"; BodyMetric.NECK -> "Neck"
    BodyMetric.LEFT_BICEP -> "Left bicep"; BodyMetric.RIGHT_BICEP -> "Right bicep"
    BodyMetric.LEFT_THIGH -> "Left thigh"; BodyMetric.RIGHT_THIGH -> "Right thigh"
    BodyMetric.LEFT_CALF -> "Left calf"; BodyMetric.RIGHT_CALF -> "Right calf"
}

object MetricComparison {
    // Saved keys may refer to metrics that have since been hidden.
    fun select(visible: List<MetricSeries>, firstKey: String, secondKey: String): Pair<MetricSeries?, MetricSeries?> {
        val first = visible.firstOrNull { it.key == firstKey } ?: visible.firstOrNull()
        val alternatives = visible.filter { it.key != first?.key }
        val second = alternatives.firstOrNull { it.key == secondKey } ?: alternatives.firstOrNull()
        return first to second
    }
}

object MetricSeriesBuilder {
    fun build(body: List<BodyMeasurement>, health: List<HealthRecord>, imperial: Boolean): List<MetricSeries> = buildList {
        BodyMetric.entries.forEach { metric ->
            val unit = if (metric == BodyMetric.BODY_WEIGHT) { if (imperial) MeasurementUnit.LB else MeasurementUnit.KG }
                else if (imperial) MeasurementUnit.IN else MeasurementUnit.CM
            add(MetricSeries("body.${metric.name}", bodyMetricTitle(metric), unit.name.lowercase(), "Body measurements",
                body.filter { it.metric == metric }.map { MetricPoint(it.epochDay, MeasurementInput.convert(it, unit), it.id) }, bodyMetric = metric))
        }
        HealthKind.entries.forEach { kind ->
            val records = health.filter { it.kind == kind }
            val group = when { kind.cardio -> "Cardio"; kind.bodyFat -> "Body composition"; else -> "Body indices" }
            add(MetricSeries("health.${kind.name}", kind.title, kind.unit, group,
                records.map { MetricPoint(it.epochDay, it.result, it.id) }, kind))
            if (kind.bodyFat) {
                val unit = if (imperial) WeightUnit.LB else WeightUnit.KG
                listOf(false, true).forEach { free ->
                    add(MetricSeries("${if (free) "ffmass" else "fatmass"}.${kind.name}", "${if (free) "Fat-free" else "Fat"} mass • ${kind.title.substringAfter("• ")}", unit.name.lowercase(), group,
                        records.mapNotNull { r -> (if (free) HealthCalculations.fatFreeMass(r) else HealthCalculations.fatMass(r))?.let { MetricPoint(r.epochDay, StrengthEstimates.inUnit(it, unit), r.id) } }, kind))
                }
            }
            if (kind == HealthKind.ROCKPORT || kind == HealthKind.COOPER) add(MetricSeries("raw.${kind.name}",
                if (kind == HealthKind.ROCKPORT) "One-mile walk time" else "12-minute run distance",
                if (kind == HealthKind.ROCKPORT) "minutes" else "m", group,
                records.mapNotNull { r -> (if (kind == HealthKind.ROCKPORT) r.timeSeconds?.div(60.0) else r.distanceMetres)?.let { MetricPoint(r.epochDay, it, r.id) } }, kind))
        }
    }
    fun weightAverage(points: List<MetricPoint>): List<MetricPoint> = points.map { point ->
        val window = points.filter { it.day in point.day - 6..point.day }
        point.copy(value = window.map { it.value }.average())
    }
    fun normalized(points: List<MetricPoint>): List<MetricPoint> {
        val first = points.firstOrNull()?.value ?: return emptyList()
        if (first == 0.0) return emptyList()
        return points.map { it.copy(value = (it.value / first - 1) * 100) }
    }
}
