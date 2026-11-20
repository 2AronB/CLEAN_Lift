package dev.clean.lift.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class BodyMetric { BODY_WEIGHT, WAIST, CHEST, HIPS, NECK, LEFT_BICEP, RIGHT_BICEP, LEFT_THIGH, RIGHT_THIGH, LEFT_CALF, RIGHT_CALF, HEIGHT }
enum class MeasurementUnit { KG, LB, CM, IN }

@Entity(tableName = "body_measurements", indices = [Index(value = ["metric", "epochDay"], unique = true)])
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val metric: BodyMetric,
    val epochDay: Long,
    val valueMilliUnits: Long,
    val unit: MeasurementUnit
)

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM body_measurements ORDER BY epochDay, id")
    fun observeAll(): Flow<List<BodyMeasurement>>
    @Query("SELECT * FROM body_measurements WHERE metric = :metric ORDER BY epochDay, id")
    fun observe(metric: BodyMetric): Flow<List<BodyMeasurement>>
    @Query("SELECT * FROM body_measurements WHERE id = :id") suspend fun get(id: Long): BodyMeasurement?
    @Insert suspend fun insert(value: BodyMeasurement): Long
    @Update suspend fun update(value: BodyMeasurement): Int
    @Query("DELETE FROM body_measurements WHERE id = :id") suspend fun delete(id: Long): Int
}

object MeasurementInput {
    fun units(metric: BodyMetric): List<MeasurementUnit> = if (metric == BodyMetric.BODY_WEIGHT)
        listOf(MeasurementUnit.LB, MeasurementUnit.KG) else listOf(MeasurementUnit.IN, MeasurementUnit.CM)
    fun parseValue(value: String): Long? = SetInput.parseWeight(value)?.takeIf { it in 1..1_000_000 }
    fun today(now: Long = System.currentTimeMillis()): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now))
    fun dayText(day: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(day * StrengthEstimates.DAY_MILLIS))
    private fun uncheckedDay(value: String): Long? {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return null
        val position = ParsePosition(0)
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false; timeZone = TimeZone.getTimeZone("UTC") }.parse(value, position)
        return date?.takeIf { position.index == value.length }?.time?.div(StrengthEstimates.DAY_MILLIS)
    }
    fun parseDay(value: String, now: Long = System.currentTimeMillis()): Long? {
        val day = uncheckedDay(value.trim()) ?: return null
        val maximum = uncheckedDay(today(now)) ?: return null
        return day.takeIf { it in 0..maximum }
    }
    fun convert(value: BodyMeasurement, target: MeasurementUnit): Double {
        require(target in units(value.metric) && value.unit in units(value.metric))
        val number = value.valueMilliUnits / 1000.0
        val base = when (value.unit) { MeasurementUnit.LB -> number * StrengthEstimates.LB_TO_KG; MeasurementUnit.IN -> number * 2.54; else -> number }
        return when (target) { MeasurementUnit.LB -> base / StrengthEstimates.LB_TO_KG; MeasurementUnit.IN -> base / 2.54; else -> base }
    }
}

class MeasurementRepository(private val db: WorkoutDatabase, private val clock: () -> Long = System::currentTimeMillis) {
    // New entries only: a conflicting metric/day aborts the entire batch without replacing anything.
    suspend fun saveDay(date: String, entries: List<DailyMeasurement>): List<Long> = db.withTransaction {
        val day = requireNotNull(MeasurementInput.parseDay(date, clock()))
        val records = DailyMeasurementInput.records(day, entries)
        records.map { db.measurementDao().insert(it) }
    }
    suspend fun save(id: Long?, metric: BodyMetric, date: String, value: String, unit: MeasurementUnit): Long = db.withTransaction {
        val day = requireNotNull(MeasurementInput.parseDay(date, clock()))
        val milli = requireNotNull(MeasurementInput.parseValue(value))
        require(unit in MeasurementInput.units(metric))
        val dao = db.measurementDao()
        if (id == null) dao.insert(BodyMeasurement(metric = metric, epochDay = day, valueMilliUnits = milli, unit = unit))
        else {
            val previous = requireNotNull(dao.get(id))
            check(previous.metric == metric)
            check(dao.update(previous.copy(epochDay = day, valueMilliUnits = milli, unit = unit)) == 1)
            id
        }
    }
    suspend fun delete(id: Long) = db.withTransaction { check(db.measurementDao().delete(id) == 1) }
}
