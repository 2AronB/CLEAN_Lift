package dev.clean.lift.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import dev.clean.lift.widgets.WidgetAttempt
import dev.clean.lift.widgets.WidgetProtocol
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.UUID

class BackupDataTest {
    private val base: Context = ApplicationProvider.getApplicationContext()
    // Prefix every preference name: tests must never mutate the real local profile/settings.
    private class TestContext(base: Context) : ContextWrapper(base) {
        private val prefix = "backup-test-${UUID.randomUUID()}-"
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = super.getSharedPreferences(prefix + name, mode)
        fun clean() { BackupPreferences.names.forEach { getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() } }
    }
    private suspend fun seed(context: Context, db: WorkoutDatabase) {
        db.dao().insertExercise(Exercise(42, "Bench café", "bench café"))
        db.dao().insertTemplate(WorkoutTemplate(81, "Workout A", "workout a"))
        db.dao().insertTemplateExercise(TemplateExercise(91, 81, 42, 0, 3, 5, 8, 90))
        db.dao().insertWorkout(Workout(51, 1000, 2000, "Workout notes", "Old plan", 2, "full-a"))
        db.dao().insertWorkout(Workout(52, 3000, notes = "Unfinished"))
        db.dao().insertWorkoutExercise(WorkoutExercise(61, 51, 42, "Historical Bench", 0, 3, true, repMin = 5, repMax = 8))
        db.dao().insertSet(WorkoutSet(71, 61, 0, 185125, WeightUnit.LB, 5, 85, "Set \"notes\"\nnext line"))
        db.dao().insertMetadata(AppMetadata("seed_version", "3"))
        db.measurementDao().insert(BodyMeasurement(101, BodyMetric.BODY_WEIGHT, 19723, 185125, MeasurementUnit.LB))
        db.measurementDao().insert(BodyMeasurement(102, BodyMetric.HEIGHT, 19723, 180125, MeasurementUnit.CM))
        db.healthDao().insert(HealthRecord(111, HealthKind.COOPER, 19723, 44.603123456789, weightKg = 81.125,
            age = 30, gender = "M", distanceMetres = 2500.0, notes = "Original inputs"))
        db.widgetAttemptDao().insert(WidgetAttempt(201, "token-1", "COOPER", WidgetProtocol.COOPER.version, 19723,
            2500.0, true, "Entered", "Original protocol", healthRecordId = 111))
        // Retain the high-water mark for deleted IDs, not just MAX(id).
        db.dao().insertExercise(Exercise(900, "Deleted", "deleted"))
        db.withTransaction { db.openHelper.writableDatabase.execSQL("DELETE FROM exercises WHERE id = 900") }
        context.getSharedPreferences("local-profile", 0).edit().putString("name", "Original person").putString("age", "30")
            .putString("gender", "M").putInt("switch-attempt-0", 4)
            .putString("palette", "FOREST").putString("theme", "DARK").commit()
        context.getSharedPreferences("metrics-page", 0).edit().putBoolean("imperial", false)
            .putInt("rep-count", 5).putLong("big-three-0", 42).putStringSet("benchmarks", setOf("42"))
            .putStringSet("hidden", setOf("health.BMI")).commit()
        context.getSharedPreferences("generator", 0).edit().putBoolean("locations_v1", true)
            .putString("location", "HOME").putStringSet("equipment_HOME", emptySet()).commit()
        context.getSharedPreferences("widgets-calculators", 0).edit().putString("location", "HOME").putString("plates-HOME-KG", "").commit()
        context.getSharedPreferences("widget-audio", 0).edit().putString("HAMR", "content://old-recording").commit()
    }
    private fun payload(bytes: ByteArray) = JSONObject(bytes.toString(Charsets.UTF_8)).getJSONObject("payload")
    private fun edited(bytes: ByteArray, edit: (JSONObject) -> Unit): ByteArray {
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        val data = root.getJSONObject("payload")
        edit(data)
        root.put("sha256", BackupRepository.digest(data))
        return root.toString().toByteArray(Charsets.UTF_8)
    }
    private fun changeCell(data: JSONObject, table: String, column: String, value: Any) {
        val section = data.getJSONObject("tables").getJSONObject(table)
        val columns = section.getJSONArray("columns")
        val index = (0 until columns.length()).single { columns.getString(it) == column }
        section.getJSONArray("rows").getJSONArray(0).put(index, value)
    }

    @Test fun fullRoundTripPreservesEveryTableSequenceAndSnapshotAcrossReopening() = runBlocking {
        val context = TestContext(base)
        val name = "backup-roundtrip-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            seed(context, db)
            var repository = BackupRepository(context, db)
            val bytes = repository.export()
            val original = payload(bytes)
            val preview = repository.preview(bytes)
            assertEquals(2, preview.counts["workouts"])
            assertEquals("Original person", preview.profileName)
            db.dao().renameExercise(42, "Changed", "changed")
            db.dao().insertExercise(Exercise(name = "After backup", normalizedName = "after backup"))
            context.getSharedPreferences("local-profile", 0).edit().putString("name", "Changed").commit()
            repository.restore(bytes)
            assertFalse(repository.hasPending())
            assertTrue(repository.generation().isNotEmpty())
            db.close(); db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
            repository = BackupRepository(context, db)
            assertFalse(repository.recoverPending())
            val restored = payload(repository.export())
            assertEquals(original.getJSONObject("tables").toString(), restored.getJSONObject("tables").toString())
            assertEquals(original.getJSONObject("sequences").toString(), restored.getJSONObject("sequences").toString())
            for (file in BackupPreferences.names.filter { it != "widget-audio" }) {
                assertEquals(original.getJSONObject("preferences").getJSONObject(file).toString(), restored.getJSONObject("preferences").getJSONObject(file).toString())
            }
            assertTrue(context.getSharedPreferences("widget-audio", 0).all.isEmpty())
            assertEquals("FOREST", context.getSharedPreferences("local-profile", 0).getString("palette", null))
            assertEquals("DARK", context.getSharedPreferences("local-profile", 0).getString("theme", null))
            assertEquals(185125L, db.dao().set(71)!!.weightMilliUnits)
            assertEquals(WeightUnit.LB, db.dao().set(71)!!.weightUnit)
            assertEquals("Historical Bench", db.dao().workoutExercise(61)!!.exerciseName)
            assertEquals(111L, db.widgetAttemptDao().get(201)!!.healthRecordId)
            assertEquals(44.603123456789, db.healthDao().get(111)!!.result, 0.0)
            assertEquals(52L, db.dao().activeWorkout()!!.id)
            assertTrue(db.dao().insertExercise(Exercise(name = "Next", normalizedName = "next")) > 900)
        } finally { db.close(); context.deleteDatabase(name); context.clean() }
    }

    @Test fun invalidFilesAndInvalidRelationalDataNeverChangeCurrentDataOrSettings() = runBlocking {
        val context = TestContext(base)
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            seed(context, db)
            val repository = BackupRepository(context, db)
            val bytes = repository.export()
            val invalid = listOf(bytes.copyOf(bytes.size / 2), bytes + "trailing".toByteArray(),
                edited(bytes) { it.put("databaseVersion", 9) }, edited(bytes) { it.put("formatVersion", 2) },
                edited(bytes) { it.getJSONObject("tables").remove("body_measurements") },
                edited(bytes) { changeCell(it, "workout_exercises", "exerciseId", 99999) },
                edited(bytes) { changeCell(it, "body_measurements", "unit", "CM") },
                edited(bytes) { changeCell(it, "health_records", "kind", "UNKNOWN") },
                edited(bytes) { changeCell(it, "workout_sets", "weightMilliUnits", "185125") },
                edited(bytes) { changeCell(it, "template_exercises", "position", Long.MAX_VALUE) },
                edited(bytes) { changeCell(it, "body_measurements", "epochDay", Long.MAX_VALUE) },
                edited(bytes) { changeCell(it, "workouts", "finishedAt", JSONObject.NULL) },
                edited(bytes) { it.getJSONObject("sequences").put("exercises", 1) },
                edited(bytes) { it.getJSONObject("preferences").getJSONObject("metrics-page").getJSONObject("imperial").put("value", "false") },
                edited(bytes) { it.getJSONObject("preferences").getJSONObject("local-profile").getJSONObject("palette").put("value", "NEON") },
                edited(bytes) { it.getJSONObject("preferences").getJSONObject("local-profile").getJSONObject("theme").put("value", 1) })
            val before = payload(bytes).getJSONObject("tables").toString()
            for ((index, corrupt) in invalid.withIndex()) {
                assertTrue("preview $index", runCatching { repository.preview(corrupt) }.isFailure)
                assertTrue("restore $index", runCatching { repository.restore(corrupt) }.isFailure)
                assertFalse(repository.hasPending())
                assertEquals(before, payload(repository.export()).getJSONObject("tables").toString())
                assertEquals("Original person", context.getSharedPreferences("local-profile", 0).getString("name", null))
            }
            val badChecksum = JSONObject(bytes.toString(Charsets.UTF_8)).put("sha256", "bad").toString().toByteArray()
            assertTrue(runCatching { repository.preview(badChecksum) }.isFailure)
        } finally { db.close(); context.clean() }
    }

    @Test fun interruptedTransactionRollsBackRowsSequencesAndPendingSettingsTogether() = runBlocking {
        val context = TestContext(base)
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            seed(context, db)
            val normal = BackupRepository(context, db)
            val old = normal.export()
            db.dao().renameExercise(42, "Newer", "newer")
            context.getSharedPreferences("local-profile", 0).edit().putString("name", "Newer person").commit()
            val before = payload(normal.export()).getJSONObject("tables").toString()
            val failing = BackupRepository(context, db) { if (it == "beforeCommit") error("simulated interruption") }
            assertTrue(runCatching { failing.restore(old) }.isFailure)
            assertEquals(before, payload(normal.export()).getJSONObject("tables").toString())
            assertEquals("Newer person", context.getSharedPreferences("local-profile", 0).getString("name", null))
            assertFalse(normal.hasPending())
            assertEquals("", normal.generation())
        } finally { db.close(); context.clean() }
    }

    @Test fun committedRowsRecoverPendingPreferencesAfterDatabaseReopen() = runBlocking {
        val context = TestContext(base)
        val name = "backup-pending-reopen-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            seed(context, db)
            val bytes = BackupRepository(context, db).export()
            db.dao().renameExercise(42, "Newer", "newer")
            context.getSharedPreferences("local-profile", 0).edit().putString("name", "Newer").commit()
            val failing = BackupRepository(context, db) { if (it == "afterCommit") error("simulated process loss") }
            assertTrue(runCatching { failing.restore(bytes) }.isFailure)
            assertTrue(failing.hasPending())
            assertEquals("Bench café", db.dao().exercise(42)!!.name)
            assertEquals("Newer", context.getSharedPreferences("local-profile", 0).getString("name", null))
            assertTrue(runCatching { failing.export() }.isFailure)
            db.close(); db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
            val recovered = BackupRepository(context, db)
            assertTrue(recovered.recoverPending())
            assertEquals("Original person", context.getSharedPreferences("local-profile", 0).getString("name", null))
            assertFalse(recovered.hasPending())
            assertFalse(recovered.recoverPending())
            assertEquals(payload(bytes).getJSONObject("tables").toString(), payload(recovered.export()).getJSONObject("tables").toString())
        } finally { db.close(); context.deleteDatabase(name); context.clean() }
    }

    @Test fun partialPreferenceApplicationIsIdempotentlyFinishedBeforeAppCanResume() = runBlocking {
        val context = TestContext(base)
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            seed(context, db)
            val bytes = BackupRepository(context, db).export()
            context.getSharedPreferences("local-profile", 0).edit().putString("name", "Newer").commit()
            context.getSharedPreferences("metrics-page", 0).edit().putBoolean("imperial", true).commit()
            val failing = BackupRepository(context, db) { if (it == "preference:local-profile") error("simulated settings failure") }
            assertTrue(runCatching { failing.restore(bytes) }.isFailure)
            assertTrue(failing.hasPending())
            assertEquals("Original person", context.getSharedPreferences("local-profile", 0).getString("name", null))
            assertTrue(context.getSharedPreferences("metrics-page", 0).getBoolean("imperial", false))
            assertTrue(BackupRepository(context, db).recoverPending())
            assertFalse(context.getSharedPreferences("metrics-page", 0).getBoolean("imperial", true))
            assertFalse(failing.hasPending())
        } finally { db.close(); context.clean() }
    }

    @Test fun boundedReadsAndEmptyRoundTripAreSupported() = runBlocking {
        assertTrue(runCatching { BackupRepository.readLimited(ByteArrayInputStream(ByteArray(BackupRepository.MAX_BYTES + 1))) }.isFailure)
        assertArrayEquals(byteArrayOf(1, 2, 3), BackupRepository.readLimited(ByteArrayInputStream(byteArrayOf(1, 2, 3))))
        val context = TestContext(base)
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = BackupRepository(context, db)
            val bytes = repository.export()
            assertTrue(repository.preview(bytes).counts.values.all { it == 0 })
            repository.restore(bytes)
            assertFalse(repository.hasPending())
            assertTrue(db.dao().exercises().isEmpty())
        } finally { db.close(); context.clean() }
    }
}
