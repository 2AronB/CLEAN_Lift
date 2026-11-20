package dev.clean.lift.data

import androidx.room.withTransaction

object CatalogRepository {
    val loggable get() = ExerciseCatalog.exercises.filter { it.recording in setOf(RecordingMode.LOAD_REPS, RecordingMode.REPS_ONLY) }
    suspend fun ensureExercise(db: WorkoutDatabase, key: String): Long {
        db.dao().catalogExercise(key)?.let { return it.id }
        val catalog = requireNotNull(ExerciseCatalog.find(key))
        val names = db.dao().exercises().map { it.normalizedName }.toSet()
        var candidate = catalog.name
        var suffix = 1
        while (ExerciseNames.key(candidate) in names) { candidate = "${catalog.name.take(80)} (variant $suffix)"; suffix++ }
        return db.dao().insertExercise(Exercise(name = candidate, normalizedName = ExerciseNames.key(candidate), catalogKey = key))
    }
    suspend fun install(db: WorkoutDatabase) = db.withTransaction {
        val marker = "structured_catalog_v${ExerciseCatalog.VERSION}"
        if (db.dao().metadata(marker) == null) {
            loggable.forEach { ensureExercise(db, it.key) }
            db.dao().insertMetadata(AppMetadata(marker, "installed"))
        }
    }
}
