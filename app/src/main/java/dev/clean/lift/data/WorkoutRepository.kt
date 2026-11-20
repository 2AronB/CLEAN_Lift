package dev.clean.lift.data

import androidx.room.withTransaction

class WorkoutRepository(private val db: WorkoutDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    private val dao = db.dao()
    private suspend fun requireActive(id: Long): Workout = requireNotNull(dao.workout(id))
        .also { check(it.finishedAt == null) { "Workout is already finished" } }

    suspend fun start(): Long = db.withTransaction {
        dao.activeWorkout()?.id ?: dao.insertWorkout(Workout(startedAt = clock()))
    }

    suspend fun startTemplate(templateId: Long): Long = db.withTransaction {
        check(dao.activeWorkout() == null) { "Finish or cancel the active workout first" }
        val template = requireNotNull(dao.template(templateId))
        require(template.exercises.isNotEmpty())
        val id = dao.insertWorkout(Workout(startedAt = clock(), templateName = template.template.name, generatorSession = template.template.generatorSession))
        template.exercises.sortedBy { it.position }.forEachIndexed { position, row ->
            val exercise = requireNotNull(dao.exercise(row.exerciseId))
            dao.insertWorkoutExercise(WorkoutExercise(workoutId = id, exerciseId = exercise.id,
                exerciseName = exercise.name, position = position, plannedSets = row.plannedSets,
                catalogKey = exercise.catalogKey, repMin = row.repMin, repMax = row.repMax, restSeconds = row.restSeconds, circuitRound = row.circuitRound))
        }
        id
    }

    private suspend fun recordOverride(workoutId: Long) { check(dao.overridePlan(workoutId) == 1) }

    suspend fun advance(entryId: Long, override: Boolean = false): WorkoutExercise? = db.withTransaction {
        val entry = requireNotNull(dao.workoutExercise(entryId))
        val workout = requireActive(entry.workoutId)
        check(workout.templateName != null)
        val details = requireNotNull(dao.workoutDetails(workout.id)).ordered()
        val current = details.exercises.first { it.exercise.id == entryId }
        if (!GuidedPlan.canAdvance(current)) {
            check(override) { "Planned sets are not complete" }
            recordOverride(workout.id)
        }
        check(dao.markCompleted(entryId, true) == 1)
        GuidedPlan.next(requireNotNull(dao.workoutDetails(workout.id)))?.exercise
    }

    suspend fun addExercise(workoutId: Long, exerciseId: Long, override: Boolean = false): Long = db.withTransaction {
        val workout = requireActive(workoutId)
        if (workout.templateName != null) { check(override); recordOverride(workoutId) }
        val exercise = requireNotNull(dao.exercise(exerciseId))
        val next = (dao.workoutExercises(workoutId).maxOfOrNull { it.position } ?: -1) + 1
        dao.insertWorkoutExercise(WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, exerciseName = exercise.name, position = next))
    }

    suspend fun saveSet(entryId: Long, setId: Long?, values: SetValues, unit: WeightUnit, override: Boolean = false): Long = db.withTransaction {
        require(values.weightMilliUnits in 0..SetInput.MAX_WEIGHT_MILLI && values.reps in 1..SetInput.MAX_REPS)
        val entry = requireNotNull(dao.workoutExercise(entryId))
        val workout = requireActive(entry.workoutId)
        if (setId == null) {
            if (workout.templateName != null && !GuidedPlan.canAppend(requireNotNull(dao.workoutDetails(workout.id)), ExerciseWithSets(entry, dao.sets(entryId)))) {
                check(override) { "Planned set count reached" }
                recordOverride(workout.id)
            }
            check(dao.markCompleted(entryId, false) == 1)
            val next = (dao.sets(entryId).maxOfOrNull { it.position } ?: -1) + 1
            dao.insertSet(WorkoutSet(workoutExerciseId = entryId, position = next,
                weightMilliUnits = values.weightMilliUnits, weightUnit = unit, reps = values.reps))
        } else {
            check(requireNotNull(dao.set(setId)).workoutExerciseId == entryId)
            check(dao.updateSet(setId, values.weightMilliUnits, unit, values.reps) == 1)
            setId
        }
    }

    suspend fun removeSet(setId: Long) = db.withTransaction {
        val set = requireNotNull(dao.set(setId))
        requireActive(requireNotNull(dao.workoutExercise(set.workoutExerciseId)).workoutId)
        check(dao.deleteSet(setId) == 1)
        check(dao.markCompleted(set.workoutExerciseId, false) == 1)
    }

    // History corrections change only numeric set values, preserving names, order, notes and timestamps.
    suspend fun correctFinishedSet(workoutId: Long, setId: Long, values: SetValues, unit: WeightUnit) = db.withTransaction {
        require(values.weightMilliUnits in 0..SetInput.MAX_WEIGHT_MILLI && values.reps in 1..SetInput.MAX_REPS)
        check(requireNotNull(dao.workout(workoutId)).finishedAt != null)
        val set = requireNotNull(dao.set(setId))
        check(requireNotNull(dao.workoutExercise(set.workoutExerciseId)).workoutId == workoutId)
        check(dao.updateSet(setId, values.weightMilliUnits, unit, values.reps) == 1)
    }

    suspend fun removeExercise(entryId: Long, override: Boolean = false) = db.withTransaction {
        val entry = requireNotNull(dao.workoutExercise(entryId))
        val workout = requireActive(entry.workoutId)
        if (workout.templateName != null) { check(override); recordOverride(workout.id) }
        check(dao.deleteWorkoutExercise(entryId) == 1)
    }

    suspend fun finish(workoutId: Long, override: Boolean = false) = db.withTransaction {
        val workout = requireActive(workoutId)
        val details = requireNotNull(dao.workoutDetails(workoutId))
        require(details.setCount > 0) { "Save a set before finishing" }
        if (!GuidedPlan.canFinish(details)) {
            if (workout.templateName == null) throw IllegalArgumentException("Each exercise needs a saved set")
            check(override) { "Plan is incomplete" }
            recordOverride(workoutId)
        }
        check(dao.finishWorkout(workoutId, maxOf(clock(), workout.startedAt)) == 1)
    }

    suspend fun cancel(workoutId: Long) = db.withTransaction {
        requireActive(workoutId)
        check(dao.cancelWorkout(workoutId) == 1)
    }
}
