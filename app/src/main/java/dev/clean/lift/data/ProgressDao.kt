package dev.clean.lift.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ExerciseProgressPoint(
    val workoutId: Long,
    val performedAt: Long,
    val heaviestKg: Double,
    val fiveRepKg: Double?,
    val tenRepKg: Double?
)

@Dao
interface ProgressDao {
    @Query("""
        SELECT s.id AS setId, w.id AS workoutId, w.startedAt AS performedAt, e.exerciseId,
            s.reps, s.weightMilliUnits / 1000.0 * CASE WHEN s.weightUnit = 'LB' THEN 0.45359237 ELSE 1.0 END AS weightKg
        FROM workout_sets s JOIN workout_exercises e ON s.workoutExerciseId = e.id
        JOIN workouts w ON e.workoutId = w.id WHERE w.finishedAt IS NOT NULL
        ORDER BY w.startedAt, w.id, e.position, s.position
    """)
    fun observeStrengthSets(): Flow<List<StrengthSample>>
    @Query("""
        SELECT s.weightMilliUnits, s.weightUnit, s.reps, w.startedAt AS performedAt
        FROM workout_sets s JOIN workout_exercises e ON s.workoutExerciseId = e.id
        JOIN workouts w ON e.workoutId = w.id
        WHERE e.exerciseId = :exerciseId AND w.finishedAt IS NOT NULL
        ORDER BY w.startedAt DESC, w.id DESC, e.position DESC, s.position DESC LIMIT 1
    """)
    suspend fun previousValues(exerciseId: Long): PreviousWorkoutValues?

    // One statement gives a consistent snapshot, including separate repeated exercise entries.
    // Empty completed sessions cannot hide the latest session with recorded sets.
    @Query("""
        SELECT s.id AS setId, e.position AS entryPosition, s.position AS setPosition,
            s.weightMilliUnits, s.weightUnit, s.reps, w.startedAt AS performedAt
        FROM workout_sets s JOIN workout_exercises e ON s.workoutExerciseId = e.id
        JOIN workouts w ON e.workoutId = w.id
        WHERE e.exerciseId = :exerciseId AND w.id = (
            SELECT w2.id FROM workouts w2
            JOIN workout_exercises e2 ON e2.workoutId = w2.id
            JOIN workout_sets s2 ON s2.workoutExerciseId = e2.id
            WHERE e2.exerciseId = :exerciseId AND w2.finishedAt IS NOT NULL
            ORDER BY w2.startedAt DESC, w2.id DESC LIMIT 1
        )
        ORDER BY e.position, s.position
    """)
    suspend fun previousSets(exerciseId: Long): List<PreviousWorkoutSet>
    // Compact one-row-per-workout projection, including repeated occurrences of a lift.
    @Query("""
        SELECT w.id AS workoutId, w.startedAt AS performedAt,
            MAX(s.weightMilliUnits / 1000.0 * CASE WHEN s.weightUnit = 'LB' THEN 0.45359237 ELSE 1.0 END) AS heaviestKg,
            MAX(CASE WHEN s.reps = 5 AND s.weightMilliUnits > 0 THEN s.weightMilliUnits / 1000.0 * CASE WHEN s.weightUnit = 'LB' THEN 0.45359237 ELSE 1.0 END END) AS fiveRepKg,
            MAX(CASE WHEN s.reps = 10 AND s.weightMilliUnits > 0 THEN s.weightMilliUnits / 1000.0 * CASE WHEN s.weightUnit = 'LB' THEN 0.45359237 ELSE 1.0 END END) AS tenRepKg
        FROM workouts w JOIN workout_exercises e ON e.workoutId = w.id
        JOIN workout_sets s ON s.workoutExerciseId = e.id
        WHERE w.finishedAt IS NOT NULL AND e.exerciseId = :exerciseId
        GROUP BY w.id ORDER BY w.startedAt, w.id
    """)
    fun observeExercise(exerciseId: Long): Flow<List<ExerciseProgressPoint>>

}
