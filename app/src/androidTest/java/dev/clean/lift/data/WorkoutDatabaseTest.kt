package dev.clean.lift.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()

    @Test fun completeWorkoutSurvivesReopeningAndRenamePreservesHistory() = runBlocking {
        val filename = "database-reopen-test.db"
        context.deleteDatabase(filename)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, filename).build()
        try {
            val dao = db.dao()
            val exerciseId = dao.insertExercise(Exercise(name = "Bench Press", normalizedName = "bench press"))
            val workoutId = dao.insertWorkout(Workout(startedAt = 1000, finishedAt = 2000, notes = "Session note"))
            val entryId = dao.insertWorkoutExercise(WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, exerciseName = "Bench Press", position = 0))
            val recorded = WorkoutSet(workoutExerciseId = entryId, position = 0, weightMilliUnits = 185125, weightUnit = WeightUnit.LB, reps = 8, rpeTenths = 85, notes = "Set note")
            val setId = dao.insertSet(recorded)
            dao.renameExercise(exerciseId, "Barbell Bench Press", "barbell bench press")
            db.close()
            db = Room.databaseBuilder(context, WorkoutDatabase::class.java, filename).build()
            assertEquals("Barbell Bench Press", db.dao().exercises().single().name)
            assertEquals("Bench Press", db.dao().workoutExercises(workoutId).single().exerciseName)
            assertEquals(recorded.copy(id = setId), db.dao().sets(entryId).single())
            assertEquals(Workout(workoutId, 1000, 2000, "Session note"), db.dao().workout(workoutId))
        } finally { db.close(); context.deleteDatabase(filename) }
    }

    @Test fun duplicatesCannotReplaceAnExistingExercise() = runBlocking {
        val db = memory()
        try {
            val dao = db.dao()
            val id = dao.insertExercise(Exercise(name = "Bench Press", normalizedName = "bench press"))
            try {
                dao.insertExercise(Exercise(name = "BENCH PRESS", normalizedName = "bench press"))
                fail("Duplicate must be rejected")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            val other = dao.insertExercise(Exercise(name = "Squat", normalizedName = "squat"))
            try {
                dao.renameExercise(other, "Bench Press", "bench press")
                fail("Rename collision must be rejected")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertEquals(listOf(id, other), dao.exercises().map { it.id })
            assertEquals("Squat", dao.exercises().last().name)
        } finally { db.close() }
    }

    @Test fun orphanRowsFailAndTransactionsRollback() = runBlocking {
        val db = memory()
        try {
            try {
                db.withTransaction {
                    db.dao().insertExercise(Exercise(name = "Squat", normalizedName = "squat"))
                    db.dao().insertWorkoutExercise(WorkoutExercise(workoutId = 999, exerciseId = 999, exerciseName = "Missing", position = 0))
                }
                fail("Foreign keys must reject orphan rows")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertTrue(db.dao().exercises().isEmpty())
        } finally { db.close() }
    }

    @Test fun setsAreOrderedAndExerciseDeletionCannotEraseHistory() = runBlocking {
        val db = memory()
        try {
            val dao = db.dao()
            val exerciseId = dao.insertExercise(Exercise(name = "Squat", normalizedName = "squat"))
            val workoutId = dao.insertWorkout(Workout(startedAt = 1000))
            val entryId = dao.insertWorkoutExercise(WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, exerciseName = "Squat", position = 0))
            dao.insertSet(WorkoutSet(workoutExerciseId = entryId, position = 1, weightMilliUnits = 100000, weightUnit = WeightUnit.KG, reps = 5))
            dao.insertSet(WorkoutSet(workoutExerciseId = entryId, position = 0, weightMilliUnits = 90000, weightUnit = WeightUnit.KG, reps = 8))
            assertEquals(listOf(0, 1), dao.sets(entryId).map { it.position })
            try {
                db.openHelper.writableDatabase.execSQL("DELETE FROM exercises WHERE id = $exerciseId")
                fail("Referenced exercise must be protected")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertEquals(2, dao.sets(entryId).size)
            db.openHelper.writableDatabase.execSQL("DELETE FROM workouts WHERE id = $workoutId")
            assertTrue(dao.workoutExercises(workoutId).isEmpty())
            assertTrue(dao.sets(entryId).isEmpty())
            assertEquals(1, dao.exercises().size)
        } finally { db.close() }
    }
}
