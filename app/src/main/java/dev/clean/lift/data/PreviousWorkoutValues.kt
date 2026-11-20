package dev.clean.lift.data

import androidx.room.Embedded

data class PreviousWorkoutValues(val weightMilliUnits: Long, val weightUnit: WeightUnit, val reps: Int, val performedAt: Long)

data class PreviousWorkoutSet(
    val setId: Long,
    val entryPosition: Int,
    val setPosition: Int,
    @Embedded val values: PreviousWorkoutValues
)
