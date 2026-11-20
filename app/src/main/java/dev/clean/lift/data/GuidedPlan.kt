package dev.clean.lift.data

object GuidedPlan {
    const val MAX_SETS = 99
    fun canAppend(entry: ExerciseWithSets): Boolean = !entry.exercise.completed &&
        (entry.exercise.plannedSets == null || entry.sets.size < entry.exercise.plannedSets)
    fun canAppend(workout: WorkoutWithExercises, entry: ExerciseWithSets): Boolean =
        canAppend(entry) && next(workout)?.exercise?.id == entry.exercise.id
    fun canAdvance(entry: ExerciseWithSets): Boolean = entry.sets.isNotEmpty() &&
        (entry.exercise.plannedSets == null || entry.sets.size >= entry.exercise.plannedSets)
    fun canFinish(workout: WorkoutWithExercises): Boolean {
        if (workout.setCount == 0 || workout.exercises.isEmpty()) return false
        if (workout.workout.templateName == null) return workout.exercises.all { it.sets.isNotEmpty() }
        return workout.exercises.all {
            it.exercise.completed || (it.exercise.plannedSets != null && canAdvance(it))
        }
    }
    fun next(workout: WorkoutWithExercises): ExerciseWithSets? = workout.exercises
        .sortedBy { it.exercise.position }.firstOrNull { !it.exercise.completed }
}
