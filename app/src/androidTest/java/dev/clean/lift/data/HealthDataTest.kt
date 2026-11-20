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

class HealthDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Test fun strengthProjectionUsesStableIdsCompletedWorkoutsAndCorrections() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val exercise = db.dao().insertExercise(Exercise(name = "Bench", normalizedName = "bench"))
            val finished = db.dao().insertWorkout(Workout(startedAt = 1000, finishedAt = 2000))
            val active = db.dao().insertWorkout(Workout(startedAt = 3000))
            val row = db.dao().insertWorkoutExercise(WorkoutExercise(workoutId = finished, exerciseId = exercise, exerciseName = "Old bench", position = 0))
            val activeRow = db.dao().insertWorkoutExercise(WorkoutExercise(workoutId = active, exerciseId = exercise, exerciseName = "Bench", position = 0))
            val set = db.dao().insertSet(WorkoutSet(workoutExerciseId = row, position = 0, weightMilliUnits = 185000, weightUnit = WeightUnit.LB, reps = 5))
            db.dao().insertSet(WorkoutSet(workoutExerciseId = activeRow, position = 0, weightMilliUnits = 999000, weightUnit = WeightUnit.KG, reps = 5))
            val before = db.progressDao().observeStrengthSets().first().single()
            assertEquals(exercise, before.exerciseId)
            assertEquals(185 * StrengthEstimates.LB_TO_KG, before.weightKg, 1e-9)
            db.dao().renameExercise(exercise, "Renamed bench", "renamed bench")
            assertEquals(before, db.progressDao().observeStrengthSets().first().single())
            db.dao().updateSet(set, 80000, WeightUnit.KG, 8)
            val corrected = db.progressDao().observeStrengthSets().first().single()
            assertEquals(80.0, corrected.weightKg, 1e-9)
            assertEquals(8, corrected.reps)
            assertEquals(before.workoutId, corrected.workoutId)
        } finally { db.close() }
    }
    @Test fun snapshotsCorrectionsAndDeletionSurviveReopen() = runBlocking {
        val name = "health-snapshot-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val base = HealthRecord(kind = HealthKind.ROCKPORT, epochDay = 19000, result = 999.0, gender = "M", age = 30,
                weightKg = 70.0, timeSeconds = 900, heartRate = 120, notes = "Track")
            val id = HealthRepository(db).save(base)
            val manual = HealthRepository(db).save(HealthRecord(kind = HealthKind.FAT_DXA, epochDay = 19000, result = 22.0))
            val before = db.healthDao().get(id)!!
            assertNotEquals(999.0, before.result)
            db.close(); db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
            assertEquals(before, db.healthDao().get(id))
            HealthRepository(db).save(before.copy(heartRate = 130))
            assertTrue(db.healthDao().get(id)!!.result < before.result)
            assertEquals("M", db.healthDao().get(id)!!.gender)
            assertEquals(30, db.healthDao().get(id)!!.age)
            var rejected = false
            try { HealthRepository(db).save(before.copy(kind = HealthKind.COOPER, distanceMetres = 2500.0)) } catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
            HealthRepository(db).delete(manual)
            assertEquals(listOf(id), db.healthDao().observeAll().first().map { it.id })
            val weight = MeasurementRepository(db).save(null, BodyMetric.BODY_WEIGHT, "2022-01-01", "80", MeasurementUnit.KG)
            assertNotNull(db.measurementDao().get(weight))
            HealthRepository(db).delete(id)
            assertNotNull(db.measurementDao().get(weight))
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun populatedSixMigrationPreservesAllOriginalTables() = runBlocking {
        val name = "health-v6-migration-test.db"
        context.deleteDatabase(name)
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("dev.clean.lift.data.WorkoutDatabase/6.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val sql = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        try {
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index); val table = entity.getString("tableName")
                sql.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: JSONArray()
                for (j in 0 until indices.length()) sql.execSQL(indices.getJSONObject(j).getString("createSql").replace("${'$'}{TABLE_NAME}", table))
            }
            sql.execSQL("INSERT INTO exercises(id,name,normalizedName,catalogKey) VALUES(1,'Bench','bench','barbell-bench-press')")
            sql.execSQL("INSERT INTO workouts(id,startedAt,finishedAt,notes,overrideCount) VALUES(1,100,200,'keep',0)")
            sql.execSQL("INSERT INTO workout_exercises(id,workoutId,exerciseId,exerciseName,position,completed,circuitRound) VALUES(1,1,1,'Bench',0,1,2)")
            sql.execSQL("INSERT INTO workout_sets(id,workoutExerciseId,position,weightMilliUnits,weightUnit,reps,notes) VALUES(1,1,0,80000,'KG',5,'preserve')")
            sql.execSQL("INSERT INTO body_measurements(id,metric,epochDay,valueMilliUnits,unit) VALUES(1,'WAIST',19000,90000,'CM')")
            sql.execSQL("INSERT INTO workout_templates(id,name,normalizedName) VALUES(1,'Plan','plan')")
            sql.execSQL("INSERT INTO template_exercises(id,templateId,exerciseId,position,plannedSets,circuitRound) VALUES(1,1,1,0,3,2)")
            sql.execSQL("INSERT INTO app_metadata(`key`,value) VALUES('keep','yes')")
            sql.version = 6
        } finally { sql.close() }
        val db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).addMigrations(WorkoutDatabase.MIGRATION_6_7, WorkoutDatabase.MIGRATION_7_8).build()
        try {
            assertEquals("preserve", db.dao().set(1)!!.notes)
            assertEquals(2, db.dao().workoutExercise(1)!!.circuitRound)
            assertEquals(2, db.dao().template(1)!!.exercises.single().circuitRound)
            assertEquals("keep", db.dao().workout(1)!!.notes)
            assertEquals("yes", db.dao().metadata("keep"))
            assertEquals(90000L, db.measurementDao().get(1)!!.valueMilliUnits)
            assertTrue(db.healthDao().observeAll().first().isEmpty())
            HealthRepository(db).save(HealthRecord(kind = HealthKind.BMI, epochDay = 19000, result = 0.0, heightCm = 180.0, weightKg = 81.0))
            assertEquals(25.0, db.healthDao().observeAll().first().single().result, 1e-9)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
