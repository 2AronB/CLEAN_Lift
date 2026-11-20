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
class TemplateWorkoutTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
    private suspend fun reject(action: suspend () -> Unit) {
        var rejected = false
        try { action() } catch (_: IllegalArgumentException) { rejected = true }
        catch (_: IllegalStateException) { rejected = true }
        assertTrue("Plan deviation must be rejected without its own override", rejected)
    }

    @Test fun orderedTemplatesAndSnapshotsSurviveReopenEditAndDeletion() = runBlocking {
        val name = "template-snapshot-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val bench = db.dao().insertExercise(Exercise(name = "Bench", normalizedName = "bench"))
            val squat = db.dao().insertExercise(Exercise(name = "Squat", normalizedName = "squat"))
            val template = TemplateRepository(db).save(null, "  Workout A  ", listOf(PlannedExercise(squat, 2), PlannedExercise(bench, null)))
            val workout = WorkoutRepository(db) { 1000 }.startTemplate(template)
            val firstEntry = db.dao().workoutExercises(workout).first().id
            WorkoutRepository(db).saveSet(firstEntry, null, SetValues(72125, 8), WeightUnit.KG)
            WorkoutRepository(db).advance(firstEntry, override = true)
            val snapshot = db.dao().workoutDetails(workout)!!.ordered()
            assertEquals("Workout A", snapshot.workout.templateName)
            assertEquals(listOf("Squat", "Bench"), snapshot.exercises.map { it.exercise.exerciseName })
            assertEquals(listOf(2, null), snapshot.exercises.map { it.exercise.plannedSets })
            TemplateRepository(db).save(template, "Changed", listOf(PlannedExercise(bench, 5)))
            assertEquals(snapshot, db.dao().workoutDetails(workout)!!.ordered())
            db.dao().renameExercise(squat, "Renamed", "renamed")
            db.close()
            db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
            assertEquals("Changed", db.dao().template(template)!!.template.name)
            assertEquals(snapshot, db.dao().workoutDetails(workout)!!.ordered())
            assertEquals(1, db.dao().workout(workout)!!.overrideCount)
            assertEquals("Bench", GuidedPlan.next(db.dao().workoutDetails(workout)!!)!!.exercise.exerciseName)
            TemplateRepository(db).delete(template)
            assertTrue(db.dao().observeTemplates().first().isEmpty())
            assertEquals(snapshot, db.dao().workoutDetails(workout)!!.ordered())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun fixedSetsAndUntilDoneGuideInOrderAndFinishWithoutOverrides() = runBlocking {
        val db = memory()
        try {
            val exercise = db.dao().insertExercise(Exercise(name = "Bench", normalizedName = "bench"))
            val template = TemplateRepository(db).save(null, "A", listOf(PlannedExercise(exercise, 2), PlannedExercise(exercise, null)))
            val repository = WorkoutRepository(db) { 1000 }
            val workout = repository.startTemplate(template)
            val entries = db.dao().workoutDetails(workout)!!.ordered().exercises
            val first = entries[0].exercise.id
            val second = entries[1].exercise.id
            reject { repository.startTemplate(template) }
            repository.saveSet(first, null, SetValues(185000, 8), WeightUnit.LB)
            reject { repository.advance(first) }
            reject { repository.saveSet(second, null, SetValues(95000, 8), WeightUnit.LB) }
            repository.saveSet(first, null, SetValues(185000, 7), WeightUnit.LB)
            reject { repository.saveSet(first, null, SetValues(185000, 6), WeightUnit.LB) }
            assertEquals(second, repository.advance(first)!!.id)
            repeat(3) { repository.saveSet(second, null, SetValues(95000, 8), WeightUnit.LB) }
            reject { repository.finish(workout) }
            assertNull(repository.advance(second))
            repository.finish(workout)
            assertEquals(5, db.dao().workoutDetails(workout)!!.setCount)
            assertEquals(0, db.dao().workout(workout)!!.overrideCount)
        } finally { db.close() }
    }

    @Test fun extraSetOverridesApplyOnlyToOneWriteAndEditsDoNotConsumeOverrides() = runBlocking {
        val db = memory()
        try {
            val exercise = db.dao().insertExercise(Exercise(name = "Bench", normalizedName = "bench"))
            val template = TemplateRepository(db).save(null, "A", listOf(PlannedExercise(exercise, 1)))
            val repository = WorkoutRepository(db)
            val workout = repository.startTemplate(template)
            val entry = db.dao().workoutExercises(workout).single().id
            val set = repository.saveSet(entry, null, SetValues(185000, 8), WeightUnit.LB)
            repository.saveSet(entry, null, SetValues(185000, 7), WeightUnit.LB, override = true)
            reject { repository.saveSet(entry, null, SetValues(185000, 6), WeightUnit.LB) }
            repository.saveSet(entry, set, SetValues(190000, 8), WeightUnit.LB)
            assertEquals(1, db.dao().workout(workout)!!.overrideCount)
            assertEquals(2, db.dao().sets(entry).size)
            repository.saveSet(entry, null, SetValues(175000, 8), WeightUnit.LB, override = true)
            assertEquals(2, db.dao().workout(workout)!!.overrideCount)
            repository.finish(workout)
            repository.correctFinishedSet(workout, set, SetValues(180000, 8), WeightUnit.LB)
            assertEquals(2, db.dao().workout(workout)!!.overrideCount)
        } finally { db.close() }
    }

    @Test fun skippingAndEarlyFinishNeedSeparateOverridesAndCannotFinishAnEmptyWorkout() = runBlocking {
        val db = memory()
        try {
            val exercise = db.dao().insertExercise(Exercise(name = "Bench", normalizedName = "bench"))
            val template = TemplateRepository(db).save(null, "A", listOf(PlannedExercise(exercise, 3), PlannedExercise(exercise, 2)))
            val repository = WorkoutRepository(db)
            val workout = repository.startTemplate(template)
            val entries = db.dao().workoutExercises(workout)
            reject { repository.finish(workout, true) }
            assertEquals(0, db.dao().workout(workout)!!.overrideCount)
            repository.saveSet(entries[0].id, null, SetValues(185000, 8), WeightUnit.LB)
            assertEquals(entries[1].id, repository.advance(entries[0].id, true)!!.id)
            assertEquals(1, db.dao().workout(workout)!!.overrideCount)
            reject { repository.advance(entries[1].id) }
            reject { repository.finish(workout) }
            repository.finish(workout, true)
            assertEquals(2, db.dao().workout(workout)!!.overrideCount)
            assertEquals(1, db.dao().workoutDetails(workout)!!.setCount)
        } finally { db.close() }
    }

    @Test fun removingSetsReopensGuidanceAndAddingRemovingExercisesNeedsOwnOverride() = runBlocking {
        val db = memory()
        try {
            val exercise = db.dao().insertExercise(Exercise(name = "Bench", normalizedName = "bench"))
            val template = TemplateRepository(db).save(null, "A", listOf(PlannedExercise(exercise, 1)))
            val repository = WorkoutRepository(db)
            val workout = repository.startTemplate(template)
            val entry = db.dao().workoutExercises(workout).single().id
            val set = repository.saveSet(entry, null, SetValues(185000, 8), WeightUnit.LB)
            repository.advance(entry)
            repository.removeSet(set)
            assertFalse(db.dao().workoutExercise(entry)!!.completed)
            repository.saveSet(entry, null, SetValues(185000, 8), WeightUnit.LB)
            reject { repository.addExercise(workout, exercise) }
            val extra = repository.addExercise(workout, exercise, true)
            reject { repository.removeExercise(extra) }
            repository.removeExercise(extra, true)
            assertEquals(2, db.dao().workout(workout)!!.overrideCount)
            assertEquals(1, db.dao().workoutDetails(workout)!!.setCount)
        } finally { db.close() }
    }

    @Test fun templateValidationAndFailedReplacementLeaveExistingPlanIntact() = runBlocking {
        val db = memory()
        try {
            val exercise = db.dao().insertExercise(Exercise(name = "Bench", normalizedName = "bench"))
            val repository = TemplateRepository(db)
            val template = repository.save(null, "A", listOf(PlannedExercise(exercise, 2)))
            val before = db.dao().template(template)
            reject { repository.save(template, "", listOf(PlannedExercise(exercise, 2))) }
            reject { repository.save(template, "A", emptyList()) }
            reject { repository.save(template, "A", listOf(PlannedExercise(exercise, 0))) }
            reject { repository.save(template, "A", listOf(PlannedExercise(exercise, 100))) }
            reject { repository.save(template, "A", listOf(PlannedExercise(Long.MAX_VALUE, 2))) }
            val other = repository.save(null, "B", listOf(PlannedExercise(exercise, null)))
            var collision = false
            try { repository.save(template, " b ", listOf(PlannedExercise(exercise, 4))) }
            catch (_: android.database.sqlite.SQLiteConstraintException) { collision = true }
            assertTrue(collision)
            assertEquals(before, db.dao().template(template))
            assertNotNull(db.dao().template(other))
        } finally { db.close() }
    }
}
