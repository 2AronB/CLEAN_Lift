package dev.clean.lift.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProgressDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Test fun previousSetListKeepsOrderPrecisionAndRepeatedOccurrencesFromOneSession() = runBlocking {
        val name = "previous-set-list-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val dao = db.dao()
            val lift = dao.insertExercise(Exercise(name = "Row", normalizedName = "row"))
            val other = dao.insertExercise(Exercise(name = "Press", normalizedName = "press"))
            suspend fun entry(workout: Long, position: Int, exercise: Long = lift): Long =
                dao.insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = exercise, exerciseName = "Original", position = position))
            suspend fun set(entry: Long, position: Int, weight: Long, unit: WeightUnit, reps: Int): Long =
                dao.insertSet(WorkoutSet(workoutExerciseId = entry, position = position, weightMilliUnits = weight, weightUnit = unit, reps = reps))
            val older = dao.insertWorkout(Workout(startedAt = 1000, finishedAt = 2000))
            set(entry(older, 0), 0, 999000, WeightUnit.KG, 1)
            // Tied timestamps must select the greater workout ID, not combine both sessions.
            val latest = dao.insertWorkout(Workout(startedAt = 1000, finishedAt = 2100))
            val first = entry(latest, 0)
            val repeat = entry(latest, 2)
            val lastId = set(repeat, 4, 72125, WeightUnit.KG, 8)
            val firstId = set(first, 0, 185125, WeightUnit.LB, 10)
            val middleId = set(first, 3, 180000, WeightUnit.LB, 9) // Position gaps survive deletion.
            set(entry(latest, 1, other), 0, 123000, WeightUnit.KG, 3)
            val active = dao.insertWorkout(Workout(startedAt = 4000))
            set(entry(active, 0), 0, 888000, WeightUnit.KG, 2)
            val empty = dao.insertWorkout(Workout(startedAt = 5000, finishedAt = 6000))
            entry(empty, 0) // An exercise with no sets is not a previous performance.
            dao.renameExercise(lift, "Renamed row", "renamed row")
            val expected = listOf(
                PreviousWorkoutSet(firstId, 0, 0, PreviousWorkoutValues(185125, WeightUnit.LB, 10, 1000)),
                PreviousWorkoutSet(middleId, 0, 3, PreviousWorkoutValues(180000, WeightUnit.LB, 9, 1000)),
                PreviousWorkoutSet(lastId, 2, 4, PreviousWorkoutValues(72125, WeightUnit.KG, 8, 1000))
            )
            assertEquals(expected, db.progressDao().previousSets(lift))
            assertTrue(db.progressDao().previousSets(Long.MAX_VALUE).isEmpty())
            WorkoutRepository(db).correctFinishedSet(latest, middleId, SetValues(175125, 11), WeightUnit.LB)
            val corrected = db.progressDao().previousSets(lift)
            assertEquals(expected[0], corrected[0])
            assertEquals(PreviousWorkoutValues(175125, WeightUnit.LB, 11, 1000), corrected[1].values)
            assertEquals(expected[2], corrected[2])
            db.close(); db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
            assertEquals(corrected, db.progressDao().previousSets(lift))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun previousValuesUseLatestCompletedSessionAndKeepRenamedExerciseIdentity() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val dao = db.dao()
            val exercise = dao.insertExercise(Exercise(name = "Bench", normalizedName = "bench"))
            suspend fun add(start: Long, finish: Long?, weight: Long): Long {
                val workout = dao.insertWorkout(Workout(startedAt = start, finishedAt = finish))
                val entry = dao.insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = exercise, exerciseName = "Bench", position = 0))
                dao.insertSet(WorkoutSet(workoutExerciseId = entry, position = 0, weightMilliUnits = weight, weightUnit = WeightUnit.KG, reps = 5))
                return entry
            }
            add(2000, 2100, 85000)
            val latest = add(3000, 3100, 90000)
            dao.insertSet(WorkoutSet(workoutExerciseId = latest, position = 1, weightMilliUnits = 72125, weightUnit = WeightUnit.KG, reps = 10))
            add(1000, 1100, 50000) // Larger generated ID does not mean a newer workout.
            add(4000, null, 999000)
            dao.renameExercise(exercise, "Renamed bench", "renamed bench")
            assertEquals(PreviousWorkoutValues(72125, WeightUnit.KG, 10, 3000), db.progressDao().previousValues(exercise))
        } finally { db.close() }
    }
    @Test fun progressGroupsRepeatedEntriesConvertsUnitsAndExcludesActiveSessions() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val dao = db.dao()
            val exercise = dao.insertExercise(Exercise(name = "Bench", normalizedName = "bench"))
            suspend fun entry(workout: Long, position: Int) = dao.insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = exercise, exerciseName = "Old bench", position = position))
            val finished = dao.insertWorkout(Workout(startedAt = 1000, finishedAt = 2000, templateName = "Push A"))
            val first = entry(finished, 0)
            val repeated = entry(finished, 1)
            val firstSet = dao.insertSet(WorkoutSet(workoutExerciseId = first, position = 0, weightMilliUnits = 100000, weightUnit = WeightUnit.KG, reps = 5))
            dao.insertSet(WorkoutSet(workoutExerciseId = repeated, position = 0, weightMilliUnits = 220000, weightUnit = WeightUnit.LB, reps = 10))
            dao.insertSet(WorkoutSet(workoutExerciseId = repeated, position = 1, weightMilliUnits = 120000, weightUnit = WeightUnit.KG, reps = 3))
            val active = dao.insertWorkout(Workout(startedAt = 3000))
            dao.insertSet(WorkoutSet(workoutExerciseId = entry(active, 0), position = 0, weightMilliUnits = 999000, weightUnit = WeightUnit.KG, reps = 5))
            val point = db.progressDao().observeExercise(exercise).first().single()
            assertEquals(finished, point.workoutId)
            assertEquals(120.0, point.heaviestKg, 0.00001)
            assertEquals(100.0, point.fiveRepKg!!, 0.00001)
            assertEquals(220 * 0.45359237, point.tenRepKg!!, 0.00001)
            assertEquals(3, db.progressDao().previousValues(exercise)!!.reps)
            WorkoutRepository(db).correctFinishedSet(finished, firstSet, SetValues(110000, 5), WeightUnit.KG)
            assertEquals(110.0, db.progressDao().observeExercise(exercise).first().single().fiveRepKg!!, 0.00001)
            assertEquals("Push A", dao.observeCompleted(50, "push").first().single().templateName)
            assertEquals(finished, dao.observeCompleted(50, "old BENCH").first().single().id)
            assertTrue(dao.observeCompleted(50, "%").first().isEmpty())
        } finally { db.close() }
    }
    @Test fun bodyweightSetsDoNotProduceEstimatesAndPreviousValuesStayExerciseSpecific() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val dao = db.dao()
            val exercise = dao.insertExercise(Exercise(name = "Push-up", normalizedName = "push-up"))
            val other = dao.insertExercise(Exercise(name = "Squat", normalizedName = "squat"))
            val workout = dao.insertWorkout(Workout(startedAt = 1000, finishedAt = 2000))
            val entry = dao.insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = exercise, exerciseName = "Push-up", position = 0))
            dao.insertSet(WorkoutSet(workoutExerciseId = entry, position = 0, weightMilliUnits = 0, weightUnit = WeightUnit.LB, reps = 10))
            val point = db.progressDao().observeExercise(exercise).first().single()
            assertEquals(0.0, point.heaviestKg, 0.0)
            assertNull(StrengthEstimates.estimate(point))
            assertNull(db.progressDao().previousValues(other))
        } finally { db.close() }
    }
}
