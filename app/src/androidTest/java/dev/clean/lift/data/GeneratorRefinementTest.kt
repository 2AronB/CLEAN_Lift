package dev.clean.lift.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GeneratorRefinementTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun locationsPreserveLegacyEquipmentAndRemainIndependentAcrossReopen() {
        val prefs = context.getSharedPreferences("generator-locations-test", Context.MODE_PRIVATE)
        prefs.edit().clear().putStringSet("equipment", setOf("KETTLEBELL", "FLOOR_SPACE", "obsolete-name")).commit()
        try {
            val store = GeneratorLocations(prefs)
            val home = setOf(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE)
            assertEquals(home, store.equipment(WorkoutLocation.HOME))
            assertEquals(setOf(Equipment.FLOOR_SPACE), store.equipment(WorkoutLocation.GYM))
            val gym = setOf(Equipment.BARBELL, Equipment.PLATES, Equipment.RACK, Equipment.SAFETIES, Equipment.FLOOR_SPACE)
            store.remember(WorkoutLocation.GYM, gym)
            store.select(WorkoutLocation.GYM)
            val reopened = GeneratorLocations(context.getSharedPreferences("generator-locations-test", Context.MODE_PRIVATE))
            assertEquals(WorkoutLocation.GYM, reopened.selected)
            assertEquals(home, reopened.equipment(WorkoutLocation.HOME))
            assertEquals(gym, reopened.equipment(WorkoutLocation.GYM))
            reopened.remember(WorkoutLocation.HOME, emptySet())
            assertTrue(reopened.equipment(WorkoutLocation.HOME).isEmpty())
            assertEquals(gym, reopened.equipment(WorkoutLocation.GYM))
            assertTrue(GeneratorLocations(prefs).equipment(WorkoutLocation.HOME).isEmpty())
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun entireLoggableCatalogInstallsOnceWithoutReinterpretingCustomHistory() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val oldId = db.dao().insertExercise(Exercise(name = "Reverse Crunch", normalizedName = "reverse crunch"))
            val repo = WorkoutRepository(db) { 1000 }
            val workout = repo.start()
            val entry = repo.addExercise(workout, oldId)
            repo.saveSet(entry, null, SetValues(0, 8), WeightUnit.KG)
            repo.finish(workout)
            val before = db.dao().workoutDetails(workout)
            CommonExercises.installOnce(db)
            assertEquals(CatalogRepository.loggable.map { it.key }.toSet(), db.dao().exercises().mapNotNull { it.catalogKey }.toSet())
            assertNull(db.dao().exercise(oldId)!!.catalogKey)
            val catalog = db.dao().catalogExercise("reverse-crunch")!!
            assertNotEquals(oldId, catalog.id)
            assertTrue(catalog.name.contains("variant"))
            db.dao().renameExercise(catalog.id, "My reverse crunch", "my reverse crunch")
            val installed = db.dao().exercises()
            CommonExercises.installOnce(db)
            assertEquals(installed, db.dao().exercises())
            assertEquals(before, db.dao().workoutDetails(workout))
        } finally { db.close() }
    }

    @Test fun circuitRoundsGuideInOrderAndSurviveTemplateEditsAndReopen() = runBlocking {
        val name = "generator-circuit-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val draft = WorkoutGenerator.generate(GeneratorRequest(GeneratorStyle.BODYWEIGHT, "body:a", setOf(Equipment.FLOOR_SPACE), abCircuit = true))
            val template = GeneratorRepository(db).save("Circuit test", draft.session, draft.rows)
            val originalRows = db.dao().template(template)!!.exercises.sortedBy { it.position }
            val workout = WorkoutRepository(db) { 1000 }.startTemplate(template)
            db.close()
            db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
            val repo = WorkoutRepository(db) { 2000 }
            val entries = db.dao().workoutExercises(workout)
            assertEquals(listOf(1, 1, 1, 2, 2, 2), entries.takeLast(6).map { it.circuitRound })
            assertEquals(6, entries.takeLast(6).map { it.id }.distinct().size)
            assertEquals(3, entries.takeLast(6).map { it.exerciseId }.distinct().size)
            entries.forEach { row ->
                assertEquals(row.id, GuidedPlan.next(db.dao().workoutDetails(workout)!!)!!.exercise.id)
                repeat(row.plannedSets!!) { repo.saveSet(row.id, null, SetValues(0, 10), WeightUnit.KG) }
                repo.advance(row.id)
            }
            repo.finish(workout)
            val completed = db.dao().workoutDetails(workout)
            // The ordinary builder also preserves round identity when editing or duplicating.
            TemplateRepository(db).save(template, "Edited circuit", originalRows.map {
                PlannedExercise(it.exerciseId, it.plannedSets, it.repMin, it.repMax, it.restSeconds, it.circuitRound)
            })
            assertEquals(completed, db.dao().workoutDetails(workout))
            assertEquals(entries.map { it.circuitRound }, db.dao().template(template)!!.exercises.sortedBy { it.position }.map { it.circuitRound })
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun populatedVersionFivePreservesTargetsHistoryAndCatalogIdentity() = runBlocking {
        val name = "generator-v5-migration-test.db"
        context.deleteDatabase(name)
        val schema = InstrumentationRegistry.getInstrumentation().context.assets.open("dev.clean.lift.data.WorkoutDatabase/5.json")
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
            execSQL("INSERT INTO exercises VALUES (1, 'Bench', 'bench', 'bench-barbell')")
            execSQL("INSERT INTO workouts VALUES (2, 1000, 2000, 'Keep', 'Old template', 1, 'five:a')")
            execSQL("INSERT INTO workout_exercises VALUES (3, 2, 1, 'Old bench', 0, 5, 1, 'bench-barbell', 5, 5, 180)")
            execSQL("INSERT INTO workout_sets VALUES (4, 3, 0, 185125, 'LB', 8, 85, 'Keep set')")
            execSQL("INSERT INTO workout_templates VALUES (5, 'Old', 'old', 'five:a')")
            execSQL("INSERT INTO template_exercises VALUES (6, 5, 1, 0, 5, 5, 5, 180)")
            version = 5; close()
        }
        val db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).addMigrations(WorkoutDatabase.MIGRATION_5_6, WorkoutDatabase.MIGRATION_6_7, WorkoutDatabase.MIGRATION_7_8).build()
        try {
            val before = db.dao().workoutDetails(2)!!
            val template = db.dao().template(5)!!
            assertNull(before.exercises.single().exercise.circuitRound)
            assertNull(template.exercises.single().circuitRound)
            assertEquals(5, before.exercises.single().exercise.repMin)
            assertEquals(180, before.exercises.single().exercise.restSeconds)
            assertEquals("five:a", before.workout.generatorSession)
            assertEquals("Keep", before.workout.notes)
            assertEquals(WorkoutSet(4, 3, 0, 185125, WeightUnit.LB, 8, 85, "Keep set"), db.dao().set(4))
            CommonExercises.installOnce(db)
            assertEquals(1L, db.dao().catalogExercise("bench-barbell")!!.id)
            assertEquals(before, db.dao().workoutDetails(2))
            assertEquals(template, db.dao().template(5))
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
