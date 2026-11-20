package dev.clean.lift.widgets

import androidx.room.*
import dev.clean.lift.data.*
import kotlinx.coroutines.flow.Flow

enum class WidgetProtocol(val title: String, val unit: String, val version: String) {
    PACER("PACER • 20 m", "shuttles", "FitnessGram 2010 • 20 m"),
    HAMR("HAMR • 20 m", "shuttles", "AFMAN 36-2905 • 24 Mar 2026"),
    PUSH_UP("Push-ups • 2 minutes", "reps", "Navy Guide 5A • Dec 2025"),
    PULL_UP("Strict pull-ups", "reps", "USMC dead-hang • MCO 6100.13A"),
    PLANK("Forearm plank", "seconds", "AFMAN 36-2905 • 2026 form • 3:40 cap"),
    COOPER("Cooper • 12-minute distance", "m", "Cooper 12-minute measured run • v1"),
    ROCKPORT("Rockport • one-mile walk time", "seconds", "Kline 1987 • track walk • v1"),
    AIR_FORCE("Air Force • 2026 practice score", "points", "PFRA tables • 17 Mar 2026 / manual • 24 Mar 2026")
}

@Entity(tableName = "widget_attempts", indices = [Index(value = ["token"], unique = true), Index(value = ["protocol", "epochDay"]), Index(value = ["healthRecordId"])],
    foreignKeys = [ForeignKey(entity = HealthRecord::class, parentColumns = ["id"], childColumns = ["healthRecordId"], onDelete = ForeignKey.SET_NULL)])
data class WidgetAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val token: String, val protocol: String, val version: String, val epochDay: Long,
    val result: Double?, val complete: Boolean, val mode: String, val details: String,
    val deviations: String = "", val healthRecordId: Long? = null
)

@Dao
interface WidgetAttemptDao {
    @Query("SELECT * FROM widget_attempts ORDER BY epochDay, id") fun observeAll(): Flow<List<WidgetAttempt>>
    @Query("SELECT * FROM widget_attempts WHERE token = :token") suspend fun byToken(token: String): WidgetAttempt?
    @Query("SELECT * FROM widget_attempts WHERE id = :id") suspend fun get(id: Long): WidgetAttempt?
    @Insert suspend fun insert(record: WidgetAttempt): Long
    @Query("DELETE FROM widget_attempts WHERE id = :id") suspend fun delete(id: Long): Int
}

object WidgetAttemptRules {
    fun validate(r: WidgetAttempt) {
        val protocol = WidgetProtocol.entries.firstOrNull { it.name == r.protocol } ?: error("Unknown protocol")
        require(r.id == 0L && r.token.length in 1..80 && r.version == protocol.version)
        require(MeasurementInput.parseDay(MeasurementInput.dayText(r.epochDay)) == r.epochDay)
        require(r.mode in listOf("Entered", "Guided") && r.details.length <= 6000 && r.deviations.length <= 1000)
        require(r.healthRecordId == null)
        if (r.complete) require(r.result != null)
        r.result?.let { value ->
            require(value.isFinite() && value >= 0)
            when (protocol) {
                WidgetProtocol.PACER, WidgetProtocol.HAMR, WidgetProtocol.PUSH_UP, WidgetProtocol.PULL_UP -> require(value <= 1000 && value == value.toInt().toDouble())
                WidgetProtocol.PLANK -> require(value in 0.0..220.0 && value == value.toInt().toDouble())
                WidgetProtocol.COOPER -> require(value <= 6000)
                WidgetProtocol.ROCKPORT -> require(value in 1.0..7200.0 && value == value.toInt().toDouble())
                WidgetProtocol.AIR_FORCE -> require(value <= 100)
            }
        }
    }
    fun series(records: List<WidgetAttempt>): List<MetricSeries> = WidgetProtocol.entries.filter { it !in listOf(WidgetProtocol.COOPER,WidgetProtocol.ROCKPORT) }.map { protocol ->
        MetricSeries("widget.${protocol.name}", protocol.title, protocol.unit, "Test benchmarks",
            records.filter { it.protocol == protocol.name && it.version == protocol.version && it.complete && it.deviations.isBlank() && it.result != null }
                .map { MetricPoint(it.epochDay, it.result!!, it.id) }, widgetProtocol = protocol.name)
    }
    // Reuse the existing raw cardio charts. Negative point IDs distinguish widget attempts from health IDs.
    // A linked estimate must not produce a second copy of the same measured performance.
    fun integrate(existing: List<MetricSeries>, records: List<WidgetAttempt>): List<MetricSeries> = existing.map { series ->
        val protocol = when(series.key) { "raw.COOPER" -> WidgetProtocol.COOPER; "raw.ROCKPORT" -> WidgetProtocol.ROCKPORT; else -> null }
        if(protocol == null) series else {
            val valid = records.filter { it.protocol==protocol.name && it.version==protocol.version && it.complete && it.deviations.isBlank() && it.result!=null }
            val linked = valid.mapNotNull { it.healthRecordId }.toSet()
            val points = series.points.filter { it.recordId !in linked } + valid.map {
                MetricPoint(it.epochDay, if(protocol==WidgetProtocol.ROCKPORT) it.result!!/60 else it.result!!, -it.id)
            }
            series.copy(points = points.sortedWith(compareBy<MetricPoint> {it.day}.thenBy { kotlin.math.abs(it.recordId) }))
        }
    } + series(records)
}

class WidgetAttemptRepository(private val db: WorkoutDatabase) {
    // Token makes Save/retry idempotent. Metric + raw attempt commit together or neither commits.
    suspend fun save(r: WidgetAttempt, health: HealthRecord? = null): Long = db.withTransaction {
        WidgetAttemptRules.validate(r)
        db.widgetAttemptDao().byToken(r.token)?.let { return@withTransaction it.id }
        health?.let {
            require(it.id == 0L && it.epochDay == r.epochDay)
            // AF's measured body component remains valid if a later physical component is interrupted.
            if (r.protocol != WidgetProtocol.AIR_FORCE.name) require(r.complete && r.deviations.isBlank())
            require(it.kind == when (WidgetProtocol.valueOf(r.protocol)) {
                WidgetProtocol.COOPER -> HealthKind.COOPER
                WidgetProtocol.ROCKPORT -> HealthKind.ROCKPORT
                WidgetProtocol.AIR_FORCE -> HealthKind.WAIST_HEIGHT
                else -> error("This protocol has no health estimate")
            })
            if (it.kind == HealthKind.COOPER) require(it.distanceMetres == r.result)
            if (it.kind == HealthKind.ROCKPORT) require(it.timeSeconds?.toDouble() == r.result)
        }
        val linked = health?.let { HealthRepository(db).save(it) }
        db.widgetAttemptDao().insert(r.copy(healthRecordId = linked))
    }
    suspend fun delete(id: Long) = db.withTransaction {
        val record = requireNotNull(db.widgetAttemptDao().get(id))
        check(db.widgetAttemptDao().delete(id) == 1)
        record.healthRecordId?.let { db.healthDao().delete(it) }
    }
}
