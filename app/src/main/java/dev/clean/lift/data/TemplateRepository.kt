package dev.clean.lift.data

import androidx.room.withTransaction

class TemplateRepository(private val db: WorkoutDatabase) {
    private val dao = db.dao()

    suspend fun save(id: Long?, name: String, rows: List<PlannedExercise>, generatorSession: String? = null): Long = db.withTransaction {
        require(ExerciseNames.valid(name))
        require(rows.isNotEmpty() && rows.size <= 100)
        rows.forEach {
            require(it.sets == null || it.sets in 1..GuidedPlan.MAX_SETS)
            require((it.repMin == null && it.repMax == null) ||
                (it.repMin != null && it.repMax != null && it.repMin in 1..SetInput.MAX_REPS && it.repMax in it.repMin..SetInput.MAX_REPS))
            require(it.restSeconds == null || it.restSeconds in 15..600)
            require(it.circuitRound == null || it.circuitRound in 1..2)
            requireNotNull(dao.exercise(it.exerciseId))
        }
        require(generatorSession == null || generatorSession in WorkoutGenerator.sessionKeys)
        val cleaned = ExerciseNames.clean(name)
        val key = ExerciseNames.key(cleaned)
        val templateId = if (id == null) dao.insertTemplate(WorkoutTemplate(name = cleaned, normalizedName = key))
        else {
            check(dao.renameTemplate(id, cleaned, key) == 1)
            dao.clearTemplateExercises(id)
            id
        }
        rows.forEachIndexed { position, row ->
            dao.insertTemplateExercise(TemplateExercise(templateId = templateId, exerciseId = row.exerciseId, position = position, plannedSets = row.sets,
                repMin = row.repMin, repMax = row.repMax, restSeconds = row.restSeconds, circuitRound = row.circuitRound))
        }
        check(dao.setGeneratorSession(templateId, generatorSession) == 1)
        templateId
    }

    suspend fun delete(id: Long) = db.withTransaction { check(dao.deleteTemplate(id) == 1) }
}
