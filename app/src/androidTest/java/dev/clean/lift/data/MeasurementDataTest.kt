package dev.clean.lift.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MeasurementDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Test fun createCorrectDeleteAndDuplicateProtectionSurviveReopening() = runBlocking {
        val name = "measurements-reopen-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            var repository = MeasurementRepository(db)
            val weight = repository.save(null, BodyMetric.BODY_WEIGHT, "2024-01-01", "185.125", MeasurementUnit.LB)
            val waist = repository.save(null, BodyMetric.WAIST, "2024-01-01", "35", MeasurementUnit.IN)
            repository.save(null, BodyMetric.BODY_WEIGHT, "2024-01-02", "184", MeasurementUnit.LB)
            var duplicate = false
            try { repository.save(null, BodyMetric.BODY_WEIGHT, "2024-01-01", "190", MeasurementUnit.LB) }
            catch (_: SQLiteConstraintException) { duplicate = true }
            assertTrue(duplicate)
            repository.save(weight, BodyMetric.BODY_WEIGHT, "2024-01-01", "185.25", MeasurementUnit.LB)
            val records = db.measurementDao().observe(BodyMetric.BODY_WEIGHT).first()
            assertEquals(listOf(185250L, 184000L), records.map { it.valueMilliUnits })
            db.close()
            db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
            repository = MeasurementRepository(db)
            assertEquals(records, db.measurementDao().observe(BodyMetric.BODY_WEIGHT).first())
            repository.delete(waist)
            assertTrue(db.measurementDao().observe(BodyMetric.WAIST).first().isEmpty())
            assertEquals(records, db.measurementDao().observe(BodyMetric.BODY_WEIGHT).first())
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun wrongMetricUnitAndDuplicateDateEditsCannotOverwriteRecords() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = MeasurementRepository(db)
            val first = repository.save(null, BodyMetric.WAIST, "2024-01-01", "35", MeasurementUnit.IN)
            repository.save(null, BodyMetric.WAIST, "2024-01-02", "34", MeasurementUnit.IN)
            val before = db.measurementDao().observe(BodyMetric.WAIST).first()
            suspend fun reject(action: suspend () -> Unit) {
                var rejected = false
                try { action() } catch (_: IllegalArgumentException) { rejected = true }
                catch (_: IllegalStateException) { rejected = true }
                catch (_: SQLiteConstraintException) { rejected = true }
                assertTrue(rejected)
            }
            reject { repository.save(first, BodyMetric.BODY_WEIGHT, "2024-01-01", "190", MeasurementUnit.LB) }
            reject { repository.save(first, BodyMetric.WAIST, "2024-01-01", "35", MeasurementUnit.KG) }
            reject { repository.save(first, BodyMetric.WAIST, "2024-01-02", "36", MeasurementUnit.IN) }
            reject { repository.save(first, BodyMetric.WAIST, "2999-01-01", "36", MeasurementUnit.IN) }
            assertEquals(before, db.measurementDao().observe(BodyMetric.WAIST).first())
        } finally { db.close() }
    }
    @Test fun populatedVersionThreeMigratesWithoutChangingPlanOrHistory() = runBlocking {
        val name = "measurement-v3-migration-test.db"
        context.deleteDatabase(name)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val schema = instrumentation.context.assets.open("dev.clean.lift.data.WorkoutDatabase/3.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).apply {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indexes = entity.optJSONArray("indices") ?: JSONArray()
                for (j in 0 until indexes.length()) execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) execSQL(setup.getString(i))
            execSQL("INSERT INTO exercises VALUES (42, 'Bench', 'bench')")
            execSQL("INSERT INTO workouts VALUES (51, 1000, NULL, 'Notes', 'Workout A', 2)")
            execSQL("INSERT INTO workout_exercises VALUES (61, 51, 42, 'Old Bench', 0, 3, 1)")
            execSQL("INSERT INTO workout_sets VALUES (71, 61, 0, 185125, 'LB', 5, 85, 'Set note')")
            execSQL("INSERT INTO workout_templates VALUES (81, 'Workout A', 'workout a')")
            execSQL("INSERT INTO template_exercises VALUES (91, 81, 42, 0, 3)")
            execSQL("INSERT INTO app_metadata VALUES ('common_exercises_v1', '1')")
            version = 3; close()
        }
        val db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).addMigrations(WorkoutDatabase.MIGRATION_3_4, WorkoutDatabase.MIGRATION_4_5, WorkoutDatabase.MIGRATION_5_6, WorkoutDatabase.MIGRATION_6_7, WorkoutDatabase.MIGRATION_7_8).build()
        try {
            assertEquals(Workout(51, 1000, null, "Notes", "Workout A", 2), db.dao().workout(51))
            assertEquals(WorkoutExercise(61, 51, 42, "Old Bench", 0, 3, true), db.dao().workoutExercise(61))
            assertEquals(WorkoutSet(71, 61, 0, 185125, WeightUnit.LB, 5, 85, "Set note"), db.dao().set(71))
            assertEquals(TemplateExercise(91, 81, 42, 0, 3), db.dao().template(81)!!.exercises.single())
            assertEquals("1", db.dao().metadata("common_exercises_v1"))
            assertTrue(db.measurementDao().observe(BodyMetric.BODY_WEIGHT).first().isEmpty())
            MeasurementRepository(db).save(null, BodyMetric.BODY_WEIGHT, "2024-01-01", "185.125", MeasurementUnit.LB)
            assertEquals(51L, WorkoutRepository(db).start())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
