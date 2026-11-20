package dev.clean.lift.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dev.clean.lift.widgets.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import androidx.lifecycle.SavedStateHandle
import android.app.Application

class WidgetAttemptDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun attempt(token: String = "test") = WidgetAttempt(token=token,protocol=WidgetProtocol.COOPER.name,version=WidgetProtocol.COOPER.version,
        epochDay=19000,result=2500.0,complete=true,mode="Guided",details="Measured track; exactly 12 minutes")
    private fun health() = HealthRecord(kind=HealthKind.COOPER,epochDay=19000,result=0.0,distanceMetres=2500.0,timeSeconds=720)
    @Test fun rawAndHealthResultsCommitOnceAndSurviveReopenAndDeletion() = runBlocking {
        val name="widget-attempt-test.db"; context.deleteDatabase(name)
        var db=Room.databaseBuilder(context,WorkoutDatabase::class.java,name).build()
        try {
            val unrelated=HealthRepository(db).save(HealthRecord(kind=HealthKind.BMI,epochDay=19000,result=0.0,heightCm=180.0,weightKg=81.0))
            val id=WidgetAttemptRepository(db).save(attempt(),health())
            assertEquals(id,WidgetAttemptRepository(db).save(attempt(),health()))
            assertEquals(2,db.healthDao().observeAll().first().size)
            val snapshot=db.widgetAttemptDao().get(id)!!
            assertNotNull(snapshot.healthRecordId)
            db.close(); db=Room.databaseBuilder(context,WorkoutDatabase::class.java,name).build()
            assertEquals(snapshot,db.widgetAttemptDao().get(id))
            WidgetAttemptRepository(db).delete(id)
            assertTrue(db.widgetAttemptDao().observeAll().first().isEmpty())
            assertEquals(listOf(unrelated),db.healthDao().observeAll().first().map { it.id })
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun invalidEstimateRollsBackAndHealthDeletionKeepsRawPerformance() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,WorkoutDatabase::class.java).build()
        try {
            assertTrue(runCatching { WidgetAttemptRepository(db).save(attempt(),health().copy(distanceMetres=500.0)) }.isFailure)
            assertTrue(db.widgetAttemptDao().observeAll().first().isEmpty()); assertTrue(db.healthDao().observeAll().first().isEmpty())
            assertTrue(runCatching { WidgetAttemptRepository(db).save(attempt().copy(deviations="Interrupted"),health()) }.isFailure)
            val id=WidgetAttemptRepository(db).save(attempt(),health())
            val linked=db.widgetAttemptDao().get(id)!!.healthRecordId!!
            HealthRepository(db).delete(linked)
            assertNull(db.widgetAttemptDao().get(id)!!.healthRecordId)
            assertEquals(2500.0,db.widgetAttemptDao().get(id)!!.result!!,0.0)
        } finally { db.close() }
    }
    @Test fun completedBodyMeasurementCanLinkFromAnIncompletePhysicalPractice() = runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,WorkoutDatabase::class.java).build()
        try {
            val p=WidgetProtocol.AIR_FORCE
            val a=WidgetAttempt(token="partial-af",protocol=p.name,version=p.version,epochDay=19000,result=null,complete=false,mode="Guided",details="Body complete; strength interrupted",deviations="Incomplete")
            val h=HealthRecord(kind=HealthKind.WAIST_HEIGHT,epochDay=19000,result=0.0,heightCm=72*2.54,waistCm=39.5*2.54)
            val id=WidgetAttemptRepository(db).save(a,h)
            assertTrue(WidgetAttemptRules.series(db.widgetAttemptDao().observeAll().first()).first {it.widgetProtocol==p.name}.points.isEmpty())
            val measurement=db.healthDao().get(db.widgetAttemptDao().get(id)!!.healthRecordId!!)!!
            assertEquals(39.5/72,measurement.result,1e-9)
        } finally {db.close()}
    }
    @Test fun restoredClockRetainsElapsedAndPausesInsteadOfReplayingCues() {
        val plan=WidgetTimerPlans.general("Countdown",60,1,1)
        val saved=SavedStateHandle(mapOf("elapsed" to 5000L,"wasRunning" to true,"plan" to plan.signature))
        val clock=WidgetClockModel(saved)
        clock.sound=false;clock.configure(plan)
        assertEquals(5000L,clock.state.value.elapsed);assertFalse(clock.state.value.running);assertTrue(clock.state.value.interrupted)
        clock.configure(WidgetTimerPlans.general("Countdown",30,1,1))
        assertEquals(0L,clock.state.value.elapsed);assertFalse(clock.state.value.interrupted)
        val paused=WidgetClockModel(SavedStateHandle(mapOf("elapsed" to 5000L,"wasRunning" to false,"interrupted" to true,"plan" to plan.signature)))
        paused.configure(plan)
        assertTrue(paused.state.value.interrupted);assertFalse(paused.state.value.running)
    }
    @Test fun restoredRecordingIsInterruptedUntilNewAttempt() {
        val kind="__isolated_restore_test__"
        val audio=ShuttleAudioModel(context.applicationContext as Application,SavedStateHandle(mapOf("protocol" to kind,"playing" to true,"position" to 4000)))
        assertTrue(audio.state.value.interrupted);assertFalse(audio.state.value.playing)
        audio.load(kind)
        assertTrue(audio.state.value.interrupted);assertEquals(4000,audio.state.value.position)
        audio.restart(kind)
        assertFalse(audio.state.value.interrupted);assertEquals(0,audio.state.value.position)
    }
    @Test fun populatedSevenMigrationPreservesHealthWorkoutBodyAndTemplates() = runBlocking {
        val name="widget-v7-test.db"; context.deleteDatabase(name)
        val schema=InstrumentationRegistry.getInstrumentation().context.assets.open("dev.clean.lift.data.WorkoutDatabase/7.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val sql=SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name),null)
        try {
            val entities=schema.getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val e=entities.getJSONObject(i); val t=e.getString("tableName")
                sql.execSQL(e.getString("createSql").replace("${'$'}{TABLE_NAME}",t))
                val indices=e.optJSONArray("indices")?:JSONArray()
                for(j in 0 until indices.length()) sql.execSQL(indices.getJSONObject(j).getString("createSql").replace("${'$'}{TABLE_NAME}",t))
            }
            sql.execSQL("INSERT INTO exercises(id,name,normalizedName) VALUES(1,'Bench','bench')")
            sql.execSQL("INSERT INTO workouts(id,startedAt,finishedAt,notes,overrideCount) VALUES(1,100,200,'keep',0)")
            sql.execSQL("INSERT INTO workout_exercises(id,workoutId,exerciseId,exerciseName,position,completed) VALUES(1,1,1,'Bench',0,1)")
            sql.execSQL("INSERT INTO workout_sets(id,workoutExerciseId,position,weightMilliUnits,weightUnit,reps,notes) VALUES(1,1,0,80000,'KG',5,'keep')")
            sql.execSQL("INSERT INTO body_measurements(id,metric,epochDay,valueMilliUnits,unit) VALUES(1,'WAIST',19000,90000,'CM')")
            sql.execSQL("INSERT INTO workout_templates(id,name,normalizedName) VALUES(1,'Plan','plan')")
            sql.execSQL("INSERT INTO health_records(id,kind,epochDay,result,formulaVersion,gender,notes) VALUES(1,'COOPER',19000,44.603,1,'M','original')")
            sql.version=7
        } finally { sql.close() }
        val db=Room.databaseBuilder(context,WorkoutDatabase::class.java,name).addMigrations(WorkoutDatabase.MIGRATION_7_8).build()
        try {
            assertEquals("keep",db.dao().set(1)!!.notes); assertEquals("keep",db.dao().workout(1)!!.notes)
            assertEquals("Plan",db.dao().template(1)!!.template.name)
            assertEquals(90000L,db.measurementDao().get(1)!!.valueMilliUnits)
            assertEquals("original",db.healthDao().get(1)!!.notes)
            assertTrue(db.widgetAttemptDao().observeAll().first().isEmpty())
            WidgetAttemptRepository(db).save(attempt(),health())
            assertEquals(2,db.healthDao().observeAll().first().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
