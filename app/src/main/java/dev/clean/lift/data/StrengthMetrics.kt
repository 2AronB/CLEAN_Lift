package dev.clean.lift.data

data class StrengthSample(val setId: Long, val workoutId: Long, val performedAt: Long, val exerciseId: Long,
                          val reps: Int, val weightKg: Double)

enum class StrengthMetric(val label: String) {
    HEAVIEST("Heaviest logged load"), SINGLE("Logged single"), REP_RECORD("Rep-specific load"),
    ESTIMATE("Estimated 1RM • 2–10 reps"), RELATIVE_SINGLE("Single / bodyweight"), RELATIVE_ESTIMATE("Estimated 1RM / bodyweight")
}

object StrengthMetrics {
    fun bigThreeComponents(samples: List<StrengthSample>, ids: List<Long?>, workoutId: Long, estimated: Boolean, personalBests: Boolean): List<StrengthSample> {
        val session = samples.firstOrNull { it.workoutId == workoutId } ?: return emptyList()
        val eligible = samples.filter { if (personalBests) it.performedAt < session.performedAt || it.performedAt == session.performedAt && it.workoutId <= workoutId else it.workoutId == workoutId }
            .sortedWith(compareBy({ it.performedAt }, { it.workoutId }, { it.setId }))
        return ids.mapNotNull { id -> eligible.filter { it.exerciseId == id && (if (estimated) estimate(it) != null else it.reps == 1 && it.weightKg > 0) }
            .maxByOrNull { if (estimated) estimate(it)!! else it.weightKg } }
    }
    // Separate from the existing 5/10-only estimates: this view explicitly offers 2–10 reps.
    fun estimate(sample: StrengthSample): Double? = sample.weightKg.takeIf { it.isFinite() && it > 0 && sample.reps in 2..10 }
        ?.times(1 + sample.reps / 30.0)
    fun kg(samples: List<StrengthSample>, metric: StrengthMetric, reps: Int): Double? = when (metric) {
        StrengthMetric.HEAVIEST -> samples.maxOfOrNull { it.weightKg }
        StrengthMetric.SINGLE, StrengthMetric.RELATIVE_SINGLE -> samples.filter { it.reps == 1 && it.weightKg > 0 }.maxOfOrNull { it.weightKg }
        StrengthMetric.REP_RECORD -> samples.filter { it.reps == reps }.maxOfOrNull { it.weightKg }
        StrengthMetric.ESTIMATE, StrengthMetric.RELATIVE_ESTIMATE -> samples.mapNotNull(::estimate).maxOrNull()
    }
    fun sameDayBodyweight(samples: List<StrengthSample>, body: List<BodyMeasurement>): BodyMeasurement? = samples.firstOrNull()?.let { sample ->
        val day = MeasurementInput.parseDay(MeasurementInput.today(sample.performedAt)) ?: return null
        body.firstOrNull { it.metric == BodyMetric.BODY_WEIGHT && it.epochDay == day }
    }
    fun series(exercises: List<Exercise>, samples: List<StrengthSample>, selected: Set<Long>, body: List<BodyMeasurement>,
               imperial: Boolean, reps: Int): List<MetricSeries> = buildList {
        val unit = if (imperial) WeightUnit.LB else WeightUnit.KG
        exercises.filter { it.id in selected }.forEach { exercise ->
            val sessions = samples.filter { it.exerciseId == exercise.id }.sortedWith(compareBy({ it.performedAt }, { it.workoutId })).groupBy { it.workoutId }
            StrengthMetric.entries.forEach { metric ->
                val relative = metric in listOf(StrengthMetric.RELATIVE_SINGLE, StrengthMetric.RELATIVE_ESTIMATE)
                val points = sessions.values.mapNotNull { sets ->
                    val value = kg(sets, metric, reps) ?: return@mapNotNull null
                    val display = if (relative) value / (sameDayBodyweight(sets, body)?.let { MeasurementInput.convert(it, MeasurementUnit.KG) } ?: return@mapNotNull null)
                        else StrengthEstimates.inUnit(value, unit)
                    MetricPoint(MeasurementInput.parseDay(MeasurementInput.today(sets.first().performedAt)) ?: return@mapNotNull null, display, sets.first().workoutId)
                }
                add(MetricSeries("strength.${exercise.id}.${metric.name}", "${exercise.name} • ${if (metric == StrengthMetric.REP_RECORD) "Best $reps-rep load" else metric.label}",
                    if (relative) "× bodyweight" else unit.name.lowercase(), "Strength", points, exerciseId = exercise.id, strengthMetric = metric))
            }
        }
    }

    fun bigThree(samples: List<StrengthSample>, ids: List<Long?>, imperial: Boolean): List<MetricSeries> = buildList {
        val unit = if (imperial) WeightUnit.LB else WeightUnit.KG
        val valid = ids.size == 3 && ids.all { it != null } && ids.distinct().size == 3
        val sessions = if (valid) samples.filter { it.exerciseId in ids }.sortedWith(compareBy({ it.performedAt }, { it.workoutId })).groupBy { it.workoutId } else emptyMap()
        listOf(false, true).forEach { estimated ->
            fun value(sets: List<StrengthSample>) = if (estimated) sets.mapNotNull(::estimate).maxOrNull() else kg(sets, StrengthMetric.SINGLE, 1)
            val sessionPoints = sessions.values.mapNotNull { sets ->
                val values = ids.map { id -> value(sets.filter { it.exerciseId == id }) ?: return@mapNotNull null }
                MetricPoint(MeasurementInput.parseDay(MeasurementInput.today(sets.first().performedAt)) ?: return@mapNotNull null,
                    StrengthEstimates.inUnit(values.sum(), unit), sets.first().workoutId)
            }
            val running = mutableMapOf<Long, Double>()
            val bestPoints = sessions.values.mapNotNull { sets ->
                ids.forEach { id -> value(sets.filter { it.exerciseId == id })?.let { running[id!!] = maxOf(running[id] ?: 0.0, it) } }
                if (running.size != 3) return@mapNotNull null
                MetricPoint(MeasurementInput.parseDay(MeasurementInput.today(sets.first().performedAt)) ?: return@mapNotNull null,
                    StrengthEstimates.inUnit(running.values.sum(), unit), sets.first().workoutId)
            }
            add(MetricSeries("big3.${if (estimated) "ESTIMATED" else "SINGLE"}.SESSION", "Big 3 • ${if (estimated) "estimated" else "logged singles"} session total",
                unit.name.lowercase(), "Big 3", sessionPoints))
            add(MetricSeries("big3.${if (estimated) "ESTIMATED" else "SINGLE"}.BEST", "Big 3 • sum of ${if (estimated) "estimated" else "single"} personal bests",
                unit.name.lowercase(), "Big 3", bestPoints))
        }
    }
}
