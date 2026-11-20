package dev.clean.lift.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class GeneratorDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val rows = listOf(GeneratedExercise("bench-barbell", 5, 5, 5, 180, ""))
    @Test fun generatedTargetsAndIdentitySurviveReopenAndTemplateEdits() = runBlocking {
        val name = "generator-reopen-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val original = db.dao().insertExercise(Exercise(name = "Flat Barbell Bench Press", normalizedName = "flat barbell bench press"))
            val templateId = GeneratorRepository(db).save("Generated A", "five:a", rows)
            val linked = db.dao().catalogExercise("bench-barbell")!!
            assertNotEquals(original, linked.id)
            assertNull(db.dao().exercise(original)!!.catalogKey)
            val workout = WorkoutRepository(db) { 1000 }.startTemplate(templateId)
            val entry = db.dao().workoutExercises(workout).single()
            assertEquals("bench-barbell", entry.catalogKey)
            assertEquals(5, entry.repMin)
            assertEquals(5, entry.repMax)
            assertEquals(180, entry.restSeconds)
            assertEquals("five:a", db.dao().workout(workout)!!.generatorSession)
            val repository = WorkoutRepository(db) { 2000 }
            repeat(5) { repository.saveSet(entry.id, null, SetValues(50000, 4), WeightUnit.KG) }
            // Actual reps below target remain valid user data; targets do not prevent finishing.
            repository.finish(workout)
            val before = db.dao().workoutDetails(workout)
            TemplateRepository(db).save(templateId, "Edited", listOf(PlannedExercise(linked.id, 2, 8, 10, 60)))
            assertNull(db.dao().template(templateId)!!.template.generatorSession)
            assertEquals(before, db.dao().workoutDetails(workout))
            db.close()
            db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
            assertEquals(before, db.dao().workoutDetails(workout))
            assertEquals("five:b", WorkoutGenerator.nextSession(GeneratorStyle.FIVE_BY_FIVE, db.dao().observeGeneratedHistory().first()))
            assertEquals(8, db.dao().template(templateId)!!.exercises.single().repMin)
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun duplicateTemplateFailureRollsBackNewExercisesAndPreviewWritesNothing() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repo = GeneratorRepository(db)
            repo.save("A", "five:a", rows)
            val before = db.dao().exercises()
            WorkoutGenerator.generate(GeneratorRequest(GeneratorStyle.KETTLEBELL, "kb:a", setOf(Equipment.FLOOR_SPACE, Equipment.KETTLEBELL)))
            assertEquals(before, db.dao().exercises())
            assertTrue(db.dao().observeGeneratedHistory().first().isEmpty())
            try {
                repo.save("A", "kb:a", listOf(GeneratedExercise("row-kettlebell", 3, 6, 10, 120, "")))
                fail("Duplicate template must fail")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertEquals(before, db.dao().exercises())
            assertNull(db.dao().catalogExercise("row-kettlebell"))
            assertNull(db.dao().activeWorkout())
        } finally { db.close() }
    }
    @Test fun populatedVersionFourMigrationPreservesHistoryTemplatesAndUnknownSemantics() = runBlocking {
        val name = "generator-v4-migration-test.db"
        context.deleteDatabase(name)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val schema = instrumentation.context.assets.open("dev.clean.lift.data.WorkoutDatabase/4.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).apply {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: JSONArray()
                for (j in 0 until indices.length()) execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) execSQL(setup.getString(i))
            execSQL("INSERT INTO exercises VALUES (1, 'Bench Press', 'bench press')")
            execSQL("INSERT INTO workouts VALUES (2, 1000, 2000, 'Keep', 'Old template', 1)")
            execSQL("INSERT INTO workout_exercises VALUES (3, 2, 1, 'Old Bench', 0, 2, 1)")
            execSQL("INSERT INTO workout_sets VALUES (4, 3, 0, 185125, 'LB', 8, 85, 'Keep set')")
            execSQL("INSERT INTO workout_templates VALUES (5, 'Old', 'old')")
            execSQL("INSERT INTO template_exercises VALUES (6, 5, 1, 0, 2)")
            version = 4; close()
        }
        val db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).addMigrations(WorkoutDatabase.MIGRATION_4_5, WorkoutDatabase.MIGRATION_5_6, WorkoutDatabase.MIGRATION_6_7, WorkoutDatabase.MIGRATION_7_8).build()
        try {
            assertNull(db.dao().exercise(1)!!.catalogKey)
            val old = db.dao().workoutDetails(2)!!
            assertEquals("Keep", old.workout.notes)
            assertEquals(1, old.workout.overrideCount)
            assertNull(old.workout.generatorSession)
            assertNull(old.exercises.single().exercise.repMin)
            assertEquals(WorkoutSet(4, 3, 0, 185125, WeightUnit.LB, 8, 85, "Keep set"), db.dao().set(4))
            assertNull(db.dao().template(5)!!.exercises.single().repMin)
            assertNull(db.dao().template(5)!!.template.generatorSession)
            assertEquals(2, db.dao().template(5)!!.exercises.single().plannedSets)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
