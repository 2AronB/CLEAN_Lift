package dev.clean.lift.data

import kotlin.math.pow

object StrengthEstimates {
    const val LB_TO_KG = 0.45359237
    const val DAY_MILLIS = 86_400_000L
    // Reuse Metrics' completed-set stream while preserving the legacy 5/10-only method.
    fun progress(samples: List<StrengthSample>, exerciseId: Long): List<ExerciseProgressPoint> = samples
        .filter { it.exerciseId == exerciseId }
        .sortedWith(compareBy({ it.performedAt }, { it.workoutId }, { it.setId }))
        .groupBy { it.workoutId }.map { (id, sets) ->
            ExerciseProgressPoint(id, sets.first().performedAt, sets.maxOf { it.weightKg },
                sets.filter { it.reps == 5 && it.weightKg > 0 }.maxOfOrNull { it.weightKg },
                sets.filter { it.reps == 10 && it.weightKg > 0 }.maxOfOrNull { it.weightKg })
        }
    fun inUnit(kg: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) kg else kg / LB_TO_KG
    // Only the requested 5/10-rep attempts; zero external load is not a total-body estimate.
    fun epley(weight: Double, reps: Int): Double? =
        if (weight.isFinite() && weight > 0 && reps in listOf(5, 10)) weight * (1 + reps / 30.0) else null
    fun estimate(point: ExerciseProgressPoint, reps: Int? = null): Double? = listOfNotNull(
        if (reps == null || reps == 5) point.fiveRepKg?.let { epley(it, 5) } else null,
        if (reps == null || reps == 10) point.tenRepKg?.let { epley(it, 10) } else null
    ).maxOrNull()
    fun ageDays(performedAt: Long, now: Long): Long = ((now - performedAt).coerceAtLeast(0) / DAY_MILLIS)
    // User-chosen what-if assumption, deliberately not a physiological prediction.
    fun scenario(base: Double, elapsedDays: Long, monthlyLossPercent: Double): Double {
        require(base.isFinite() && base >= 0 && elapsedDays >= 0)
        require(monthlyLossPercent.isFinite() && monthlyLossPercent in 0.0..10.0)
        return base * (1 - monthlyLossPercent / 100).pow(elapsedDays / 30.0)
    }
}
