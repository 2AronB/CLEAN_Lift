package dev.clean.lift.data

import androidx.room.Room
import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import org.json.JSONArray

class WorkoutMigrationTest {
    @Test fun versionTwoActiveAndFinishedRecordsSurviveTemplateMigration() = runBlocking {
        val name = "workout-v2-migration-test.db"
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        context.deleteDatabase(name)
        val oldSchema = instrumentation.context.assets.open("dev.clean.lift.data.WorkoutDatabase/2.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).apply {
            val entities = oldSchema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indexes = entity.optJSONArray("indices") ?: JSONArray()
                for (j in 0 until indexes.length()) execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = oldSchema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) execSQL(setup.getString(i))
            execSQL("INSERT INTO exercises VALUES (42, 'Bench Press', 'bench press')")
            execSQL("INSERT INTO workouts VALUES (51, 1000, 2000, 'Finished note')")
            execSQL("INSERT INTO workouts VALUES (52, 3000, NULL, 'Active note')")
            execSQL("INSERT INTO workout_exercises VALUES (61, 51, 42, 'Historical Bench', 0)")
            execSQL("INSERT INTO workout_exercises VALUES (62, 52, 42, 'Bench Press', 0)")
            execSQL("INSERT INTO workout_sets VALUES (71, 61, 0, 185125, 'LB', 8, 85, 'Set note')")
            execSQL("INSERT INTO workout_sets VALUES (72, 62, 0, 90000, 'KG', 5, NULL, 'Active set')")
            execSQL("INSERT INTO app_metadata VALUES ('common_exercises_v1', '1')")
            version = 2
            close()
        }
        val db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(WorkoutDatabase.MIGRATION_2_3, WorkoutDatabase.MIGRATION_3_4, WorkoutDatabase.MIGRATION_4_5, WorkoutDatabase.MIGRATION_5_6, WorkoutDatabase.MIGRATION_6_7, WorkoutDatabase.MIGRATION_7_8).build()
        try {
            assertEquals(Workout(51, 1000, 2000, "Finished note"), db.dao().workout(51))
            assertEquals(Workout(52, 3000, null, "Active note"), db.dao().workout(52))
            assertEquals(WorkoutExercise(61, 51, 42, "Historical Bench", 0), db.dao().workoutExercise(61))
            assertEquals(WorkoutSet(71, 61, 0, 185125, WeightUnit.LB, 8, 85, "Set note"), db.dao().set(71))
            assertEquals(WorkoutSet(72, 62, 0, 90000, WeightUnit.KG, 5, null, "Active set"), db.dao().set(72))
            assertEquals("1", db.dao().metadata("common_exercises_v1"))
            assertEquals(52L, WorkoutRepository(db).start())
            assertTrue(db.dao().observeTemplates().first().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun versionOneRecordsSurviveMigrationAndSeeding() = runBlocking {
        val name = "workout-migration-test.db"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(name)
        // Build the old database from its exported schema, not the current entity definitions.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val oldSchema = instrumentation.context.assets.open("dev.clean.lift.data.WorkoutDatabase/1.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).apply {
            val entities = oldSchema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indexes = entity.optJSONArray("indices") ?: JSONArray()
                for (j in 0 until indexes.length()) {
                    execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = oldSchema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) execSQL(setup.getString(i))
            execSQL("INSERT INTO exercises VALUES (42, 'Bench Press', 'bench press')")
            execSQL("INSERT INTO workouts VALUES (51, 1000, 2000, 'Workout note')")
            execSQL("INSERT INTO workout_exercises VALUES (61, 51, 42, 'Bench Press', 0)")
            execSQL("INSERT INTO workout_sets VALUES (71, 61, 0, 185125, 'LB', 8, 85, 'Set note')")
            version = 1
            close()
        }
        val db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
            .addMigrations(WorkoutDatabase.MIGRATION_1_2, WorkoutDatabase.MIGRATION_2_3, WorkoutDatabase.MIGRATION_3_4, WorkoutDatabase.MIGRATION_4_5, WorkoutDatabase.MIGRATION_5_6, WorkoutDatabase.MIGRATION_6_7, WorkoutDatabase.MIGRATION_7_8).build()
        try {
            CommonExercises.installOnce(db)
            assertEquals(42L, db.dao().exercises().single { it.normalizedName == "bench press" }.id)
            val workout = db.dao().workoutDetails(51)!!
            assertEquals("Workout note", workout.workout.notes)
            assertEquals(WorkoutSet(71, 61, 0, 185125, WeightUnit.LB, 8, 85, "Set note"), workout.exercises.single().sets.single())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
