package dev.clean.lift.data

import androidx.room.withTransaction

object CommonExercises {
    val names = listOf(
        "Squat", "Front Squat", "Goblet Squat", "Leg Press", "Leg Extension", "Leg Curl",
        "Deadlift", "Romanian Deadlift", "Hip Thrust", "Lunge", "Bulgarian Split Squat", "Calf Raise",
        "Bench Press", "Incline Bench Press", "Dumbbell Bench Press", "Chest Fly", "Push-up",
        "Overhead Press", "Dumbbell Shoulder Press", "Lateral Raise", "Pull-up", "Chin-up",
        "Lat Pulldown", "Barbell Row", "Dumbbell Row", "Seated Cable Row", "Face Pull",
        "Biceps Curl", "Hammer Curl", "Triceps Pushdown", "Dip"
    )

    suspend fun installOnce(db: WorkoutDatabase) = db.withTransaction {
        val dao = db.dao()
        if (dao.metadata("common_exercises_v1") == null) {
            names.forEach { dao.seedExercise(Exercise(name = it, normalizedName = ExerciseNames.key(it))) }
            dao.insertMetadata(AppMetadata("common_exercises_v1", "installed"))
        }
        CatalogRepository.install(db)
    }
}
