package dev.clean.lift.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DailyMeasurementDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Test fun conflictRollsBackEarlierInsertsAndLeavesOtherDataUntouched() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val repository = MeasurementRepository(db)
            val original = repository.save(null, BodyMetric.WAIST, "2024-01-01", "35.125", MeasurementUnit.IN)
            val exercise = db.dao().insertExercise(Exercise(name = "Test", normalizedName = "test"))
            val failure = runCatching { repository.saveDay("2024-01-01", listOf(
                DailyMeasurement(BodyMetric.BODY_WEIGHT, "185", MeasurementUnit.LB),
                DailyMeasurement(BodyMetric.WAIST, "34", MeasurementUnit.IN))) }
            assertTrue(failure.isFailure)
            assertTrue(db.measurementDao().observe(BodyMetric.BODY_WEIGHT).first().isEmpty())
            assertEquals(35125L, db.measurementDao().get(original)!!.valueMilliUnits)
            assertNotNull(db.dao().exercise(exercise))
            assertTrue(runCatching { repository.saveDay("2999-01-01", listOf(DailyMeasurement(BodyMetric.CHEST, "40", MeasurementUnit.IN))) }.isFailure)
        } finally { db.close() }
    }
    @Test fun completeBatchReopensWithStableIdsAndExactUnits() = runBlocking {
        val name = "daily-measurement-reopen-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
        try {
            val ids = MeasurementRepository(db).saveDay("2024-01-01", listOf(
                DailyMeasurement(BodyMetric.BODY_WEIGHT, "185.125", MeasurementUnit.LB),
                DailyMeasurement(BodyMetric.HEIGHT, "180.125", MeasurementUnit.CM)))
            val before = db.measurementDao().observeAll().first()
            assertEquals(ids, before.map { it.id })
            db.close(); db = Room.databaseBuilder(context, WorkoutDatabase::class.java, name).build()
            assertEquals(before, db.measurementDao().observeAll().first())
            assertTrue(runCatching { MeasurementRepository(db).saveDay("2024-01-02", listOf(
                DailyMeasurement(BodyMetric.BODY_WEIGHT, "80", MeasurementUnit.KG),
                DailyMeasurement(BodyMetric.WAIST, "bad", MeasurementUnit.CM))) }.isFailure)
            assertEquals(before, db.measurementDao().observeAll().first())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
