package dev.clean.lift.data

import androidx.room.withTransaction

class GeneratorRepository(private val db: WorkoutDatabase) {
    suspend fun save(name: String, session: String, rows: List<GeneratedExercise>): Long = db.withTransaction {
        require(session in WorkoutGenerator.sessionKeys && rows.isNotEmpty() && rows.size <= 100)
        require(WorkoutGenerator.uniqueRows(rows))
        rows.forEach(WorkoutGenerator::validateRow)
        val planned = rows.map { row ->
            val exerciseId = CatalogRepository.ensureExercise(db, row.key)
            PlannedExercise(exerciseId, row.sets, row.repMin, row.repMax, row.restSeconds, row.circuitRound)
        }
        TemplateRepository(db).save(null, name, planned, session)
    }
}
