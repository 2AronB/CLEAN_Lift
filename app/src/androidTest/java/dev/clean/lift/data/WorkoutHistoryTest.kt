package dev.clean.lift.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutHistoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun historyExcludesActiveAndReturnsNewestSummariesWithAccurateCounts() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val dao = db.dao()
            val exercise = dao.insertExercise(Exercise(name = "Bench Press", normalizedName = "bench press"))
            suspend fun insert(finished: Long?, entries: Int): Long {
                val workout = dao.insertWorkout(Workout(startedAt = 100, finishedAt = finished))
                repeat(entries) { position ->
                    val entry = dao.insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = exercise, exerciseName = "Bench Press", position = position))
                    repeat(position + 1) { setPosition ->
                        dao.insertSet(WorkoutSet(workoutExerciseId = entry, position = setPosition, weightMilliUnits = 185000, weightUnit = WeightUnit.LB, reps = 8))
                    }
                }
                return workout
            }
            val old = insert(200, 1)
            val newer = insert(300, 2)
            val tied = insert(300, 1)
            insert(null, 3)
            assertEquals(listOf(tied, newer), dao.observeCompleted(2).first().map { it.id })
            val all = dao.observeCompleted(50).first()
            assertEquals(listOf(tied, newer, old), all.map { it.id })
            val summary = all.first { it.id == newer }
            assertEquals(2, summary.exerciseCount)
            assertEquals(3, summary.setCount)
            assertEquals(100L, summary.startedAt)
            assertEquals(300L, summary.finishedAt)
        } finally { db.close() }
    }

    @Test fun correctionSurvivesReopenAndPreservesEveryOtherFieldAndWorkout() = runBlocking {
        val filename = "history-correction-test.db"
        context.deleteDatabase(filename)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, filename).build()
        try {
            val dao = db.dao()
            val exercise = dao.insertExercise(Exercise(name = "Bench Press", normalizedName = "bench press"))
            val workout = dao.insertWorkout(Workout(startedAt = 100, finishedAt = 200, notes = "Workout note"))
            val entry = dao.insertWorkoutExercise(WorkoutExercise(workoutId = workout, exerciseId = exercise, exerciseName = "Original bench", position = 4))
            val set = dao.insertSet(WorkoutSet(workoutExerciseId = entry, position = 7, weightMilliUnits = 185000, weightUnit = WeightUnit.LB, reps = 8, rpeTenths = 85, notes = "Set note"))
            val untouched = dao.insertSet(WorkoutSet(workoutExerciseId = entry, position = 9, weightMilliUnits = 190000, weightUnit = WeightUnit.LB, reps = 6))
            val otherWorkout = dao.insertWorkout(Workout(startedAt = 300, finishedAt = 400))
            val otherEntry = dao.insertWorkoutExercise(WorkoutExercise(workoutId = otherWorkout, exerciseId = exercise, exerciseName = "Bench Press", position = 0))
            dao.insertSet(WorkoutSet(workoutExerciseId = otherEntry, position = 0, weightMilliUnits = 200000, weightUnit = WeightUnit.LB, reps = 4))
            val before = dao.observeWorkout(workout).first()!!.ordered()
            val otherBefore = dao.workoutDetails(otherWorkout)
            WorkoutRepository(db).correctFinishedSet(workout, set, SetValues(85125, 7), WeightUnit.KG)
            db.close()
            db = Room.databaseBuilder(context, WorkoutDatabase::class.java, filename).build()
            val after = db.dao().observeWorkout(workout).first()!!.ordered()
            assertEquals(before.workout, after.workout)
            assertEquals(before.exercises.single().exercise, after.exercises.single().exercise)
            assertEquals(before.exercises.single().sets.first().copy(weightMilliUnits = 85125, weightUnit = WeightUnit.KG, reps = 7), after.exercises.single().sets.first())
            assertEquals(before.exercises.single().sets.last(), db.dao().set(untouched))
            assertEquals(otherBefore, db.dao().workoutDetails(otherWorkout))
        } finally { db.close(); context.deleteDatabase(filename) }
    }

    @Test fun correctionRejectsWrongWorkoutActiveSessionsAndInvalidValues() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val dao = db.dao()
            val repository = WorkoutRepository(db) { 1000 }
            val exercise = dao.insertExercise(Exercise(name = "Squat", normalizedName = "squat"))
            val finished = repository.start()
            val entry = repository.addExercise(finished, exercise)
            val set = repository.saveSet(entry, null, SetValues(100000, 5), WeightUnit.KG)
            repository.finish(finished)
            val active = repository.start()
            val activeEntry = repository.addExercise(active, exercise)
            val activeSet = repository.saveSet(activeEntry, null, SetValues(120000, 3), WeightUnit.KG)
            val before = dao.workoutDetails(finished)
            val activeBefore = dao.workoutDetails(active)
            suspend fun reject(action: suspend () -> Unit) {
                var rejected = false
                try { action() } catch (_: IllegalArgumentException) { rejected = true }
                catch (_: IllegalStateException) { rejected = true }
                assertTrue("Unsafe correction must be rejected", rejected)
            }
            reject { repository.correctFinishedSet(finished, activeSet, SetValues(90000, 8), WeightUnit.KG) }
            reject { repository.correctFinishedSet(active, activeSet, SetValues(90000, 8), WeightUnit.KG) }
            reject { repository.correctFinishedSet(finished, set, SetValues(-1, 8), WeightUnit.KG) }
            reject { repository.correctFinishedSet(finished, set, SetValues(90000, 0), WeightUnit.KG) }
            reject { repository.correctFinishedSet(finished, set, SetValues(SetInput.MAX_WEIGHT_MILLI + 1, 8), WeightUnit.KG) }
            reject { repository.correctFinishedSet(finished, set, SetValues(90000, SetInput.MAX_REPS + 1), WeightUnit.KG) }
            assertEquals(before, dao.workoutDetails(finished))
            assertEquals(activeBefore, dao.workoutDetails(active))
        } finally { db.close() }
    }
}
