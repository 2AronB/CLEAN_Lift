package dev.clean.lift.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutLoggingTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()

    @Test fun starterExercisesPreserveCustomRecordsAndInstallOnlyOnce() = runBlocking {
        val db = memory()
        try {
            val dao = db.dao()
            val customId = dao.insertExercise(Exercise(name = "BENCH PRESS", normalizedName = "bench press"))
            dao.insertExercise(Exercise(name = "Custom lift", normalizedName = "custom lift"))
            CommonExercises.installOnce(db)
            assertEquals(CommonExercises.names.size + CatalogRepository.loggable.size + 1, dao.exercises().size)
            assertEquals("BENCH PRESS", dao.exercise(customId)!!.name)
            dao.renameExercise(customId, "My bench", "my bench")
            CommonExercises.installOnce(db)
            assertEquals(CommonExercises.names.size + CatalogRepository.loggable.size + 1, dao.exercises().size)
            assertFalse(dao.exercises().any { it.normalizedName == "bench press" })
        } finally { db.close() }
    }

    @Test fun fullWorkoutAndUnfinishedWorkoutSurviveDatabaseReopen() = runBlocking {
        val filename = "logging-reopen-test.db"
        context.deleteDatabase(filename)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, filename).build()
        try {
            var repository = WorkoutRepository(db) { 1000 }
            val exercise = db.dao().insertExercise(Exercise(name = "Bench Press", normalizedName = "bench press"))
            val workout = repository.start()
            val entry = repository.addExercise(workout, exercise)
            val first = repository.saveSet(entry, null, SetValues(185125, 8), WeightUnit.LB)
            repository.saveSet(entry, null, SetValues(85000, 5), WeightUnit.KG)
            db.close()
            db = Room.databaseBuilder(context, WorkoutDatabase::class.java, filename).build()
            repository = WorkoutRepository(db) { 2000 }
            assertEquals(workout, repository.start())
            repository.saveSet(entry, first, SetValues(190125, 7), WeightUnit.LB)
            repository.finish(workout)
            db.close()
            db = Room.databaseBuilder(context, WorkoutDatabase::class.java, filename).build()
            val saved = db.dao().workoutDetails(workout)!!.ordered()
            assertEquals(2000L, saved.workout.finishedAt)
            assertEquals(2, saved.setCount)
            assertEquals(listOf(190125L, 85000L), saved.exercises.single().sets.map { it.weightMilliUnits })
            assertEquals(listOf(7, 5), saved.exercises.single().sets.map { it.reps })
            assertEquals(listOf(WeightUnit.LB, WeightUnit.KG), saved.exercises.single().sets.map { it.weightUnit })
        } finally { db.close(); context.deleteDatabase(filename) }
    }

    @Test fun concurrentStartsReuseOneSessionAndFinishedSessionsRejectChanges() = runBlocking {
        val db = memory()
        try {
            val repository = WorkoutRepository(db) { 1000 }
            val ids = List(4) { async { repository.start() } }.awaitAll()
            assertEquals(1, ids.toSet().size)
            val id = ids.first()
            val exercise = db.dao().insertExercise(Exercise(name = "Squat", normalizedName = "squat"))
            val entry = repository.addExercise(id, exercise)
            val set = repository.saveSet(entry, null, SetValues(100000, 5), WeightUnit.KG)
            repository.finish(id)
            suspend fun reject(action: suspend () -> Unit) {
                try { action(); fail("Finished workout must be immutable") } catch (_: IllegalStateException) { }
            }
            reject { repository.saveSet(entry, set, SetValues(110000, 5), WeightUnit.KG) }
            reject { repository.removeSet(set) }
            reject { repository.removeExercise(entry) }
            reject { repository.cancel(id) }
            reject { repository.addExercise(id, exercise) }
            assertEquals(100000L, db.dao().sets(entry).single().weightMilliUnits)
            assertNotEquals(id, repository.start())
        } finally { db.close() }
    }

    @Test fun unfinishedExercisesBlockFinishAndRemovalDoesNotOverwriteOtherSets() = runBlocking {
        val db = memory()
        try {
            val repository = WorkoutRepository(db) { 1000 }
            val workout = repository.start()
            suspend fun finishMustFail() {
                try { repository.finish(workout); fail("Incomplete session must not finish") }
                catch (_: IllegalArgumentException) { }
            }
            finishMustFail()
            val exercise = db.dao().insertExercise(Exercise(name = "Squat", normalizedName = "squat"))
            val entry = repository.addExercise(workout, exercise)
            finishMustFail()
            val first = repository.saveSet(entry, null, SetValues(80000, 5), WeightUnit.KG)
            val middle = repository.saveSet(entry, null, SetValues(90000, 5), WeightUnit.KG)
            val last = repository.saveSet(entry, null, SetValues(100000, 5), WeightUnit.KG)
            repository.removeSet(middle)
            val added = repository.saveSet(entry, null, SetValues(110000, 5), WeightUnit.KG)
            assertEquals(listOf(first, last, added), db.dao().sets(entry).map { it.id })
            val unused = repository.addExercise(workout, exercise)
            finishMustFail()
            repository.removeExercise(unused)
            repository.finish(workout)
            assertEquals(3, db.dao().workoutDetails(workout)!!.setCount)
        } finally { db.close() }
    }

    @Test fun cancellationDeletesOnlyTheActiveSession() = runBlocking {
        val db = memory()
        try {
            val repository = WorkoutRepository(db) { 1000 }
            val exercise = db.dao().insertExercise(Exercise(name = "Squat", normalizedName = "squat"))
            val finished = repository.start()
            val entry = repository.addExercise(finished, exercise)
            repository.saveSet(entry, null, SetValues(100000, 5), WeightUnit.KG)
            repository.finish(finished)
            val active = repository.start()
            val activeEntry = repository.addExercise(active, exercise)
            repository.saveSet(activeEntry, null, SetValues(120000, 3), WeightUnit.KG)
            repository.cancel(active)
            assertNull(db.dao().workout(active))
            assertTrue(db.dao().sets(activeEntry).isEmpty())
            assertEquals(1, db.dao().workoutDetails(finished)!!.setCount)
            assertEquals(1, db.dao().exercises().size)
        } finally { db.close() }
    }
}
