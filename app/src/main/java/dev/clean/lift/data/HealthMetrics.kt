package dev.clean.lift.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlin.math.log10

enum class HealthKind(val title: String, val unit: String, val estimated: Boolean = false) {
    BMI("BMI", "kg/m²"), WAIST_HEIGHT("Waist-to-height ratio", "ratio"),
    WAIST_HIP("Waist-to-hip ratio", "ratio"),
    NAVY_FAT("Body fat • Navy tape", "%", true), RFM_FAT("Body fat • relative fat mass", "%", true),
    FAT_DXA("Body fat • DXA", "%"), FAT_BIA("Body fat • impedance", "%", true),
    FAT_SKINFOLD("Body fat • skinfold", "%", true), FAT_OTHER("Body fat • other method", "%"),
    ROCKPORT("VO₂max • Rockport walk", "mL/kg/min", true),
    COOPER("VO₂max • Cooper run", "mL/kg/min", true),
    VO2_LAB("VO₂ • laboratory treadmill", "mL/kg/min"),
    VO2_DEVICE("VO₂max • device / other", "mL/kg/min", true);

    val bodyFat: Boolean get() = this in listOf(NAVY_FAT, RFM_FAT, FAT_DXA, FAT_BIA, FAT_SKINFOLD, FAT_OTHER)
    val cardio: Boolean get() = this in listOf(ROCKPORT, COOPER, VO2_LAB, VO2_DEVICE)
    val needsGender: Boolean get() = this in listOf(NAVY_FAT, RFM_FAT, ROCKPORT)
    val manual: Boolean get() = this in listOf(FAT_DXA, FAT_BIA, FAT_SKINFOLD, FAT_OTHER, VO2_LAB, VO2_DEVICE)
}

// Each result keeps its original inputs. Profile changes never recalculate saved history.
@Entity(tableName = "health_records", indices = [Index(value = ["kind", "epochDay"])])
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: HealthKind, val epochDay: Long, val result: Double,
    val formulaVersion: Int = 1,
    val heightCm: Double? = null, val weightKg: Double? = null,
    val waistCm: Double? = null, val abdomenCm: Double? = null,
    val neckCm: Double? = null, val hipsCm: Double? = null,
    val age: Int? = null, val gender: String = "",
    val timeSeconds: Int? = null, val distanceMetres: Double? = null,
    val heartRate: Int? = null, val notes: String = ""
)

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_records ORDER BY epochDay, id") fun observeAll(): Flow<List<HealthRecord>>
    @Query("SELECT * FROM health_records WHERE id = :id") suspend fun get(id: Long): HealthRecord?
    @Insert suspend fun insert(record: HealthRecord): Long
    @Update suspend fun update(record: HealthRecord): Int
    @Query("DELETE FROM health_records WHERE id = :id") suspend fun delete(id: Long): Int
}

object HealthCalculations {
    fun calculate(r: HealthRecord): Double? {
        if (r.gender !in listOf("", "M", "F") || (r.age != null && r.age !in 1..120) || r.notes.length > 1000) return null
        if (r.kind.needsGender && r.gender.isEmpty()) return null
        if (r.age != null && ((r.kind == HealthKind.NAVY_FAT || r.kind == HealthKind.COOPER) && r.age < 18 || r.kind == HealthKind.RFM_FAT && r.age < 20)) return null
        fun positive(v: Double?) = v != null && v.isFinite() && v > 0 && v <= 1000
        // Optional inputs must also be valid; no NaN/infinite snapshots can enter the database.
        if (listOf(r.heightCm, r.weightKg, r.waistCm, r.abdomenCm, r.neckCm, r.hipsCm).any { it != null && !positive(it) }) return null
        if (r.kind.manual) return r.result.takeIf { it.isFinite() && it > 0 && it <= if (r.kind.bodyFat) 100.0 else 120.0 }
        val value = when (r.kind) {
            HealthKind.BMI -> if (positive(r.heightCm) && positive(r.weightKg)) r.weightKg!! / (r.heightCm!! / 100).let { it * it } else return null
            HealthKind.WAIST_HEIGHT -> if (positive(r.waistCm) && positive(r.heightCm)) r.waistCm!! / r.heightCm!! else return null
            HealthKind.WAIST_HIP -> if (positive(r.waistCm) && positive(r.hipsCm)) r.waistCm!! / r.hipsCm!! else return null
            HealthKind.RFM_FAT -> {
                if (!positive(r.heightCm) || !positive(r.waistCm)) return null
                64 - 20 * r.heightCm!! / r.waistCm!! + if (r.gender == "F") 12 else 0
            }
            HealthKind.NAVY_FAT -> {
                if (!positive(r.heightCm) || !positive(r.neckCm)) return null
                if (r.gender == "M") {
                    if (!positive(r.abdomenCm) || r.abdomenCm!! <= r.neckCm!!) return null
                    86.010 * log10((r.abdomenCm - r.neckCm) / 2.54) - 70.041 * log10(r.heightCm!! / 2.54) + 36.76
                } else {
                    if (!positive(r.waistCm) || !positive(r.hipsCm) || r.waistCm!! + r.hipsCm!! <= r.neckCm!!) return null
                    163.205 * log10((r.waistCm + r.hipsCm - r.neckCm) / 2.54) - 97.684 * log10(r.heightCm!! / 2.54) - 78.387
                }
            }
            HealthKind.ROCKPORT -> {
                if (!positive(r.weightKg) || r.age !in 20..69 || r.timeSeconds !in 1..7200 || r.heartRate !in 30..250) return null
                132.853 - 0.0769 * r.weightKg!! / StrengthEstimates.LB_TO_KG - 0.3877 * r.age!! +
                    (if (r.gender == "M") 6.315 else 0.0) - 3.2649 * r.timeSeconds!! / 60 - 0.1565 * r.heartRate!!
            }
            HealthKind.COOPER -> {
                if (r.distanceMetres == null || !r.distanceMetres.isFinite() || r.distanceMetres !in 505.0..6000.0) return null
                (r.distanceMetres - 504.9) / 44.73
            }
            else -> return null
        }
        return value.takeIf { it.isFinite() && it > 0 && (!r.kind.bodyFat || it <= 100) && (!r.kind.cardio || it <= 120) }
    }

    fun parseTime(text: String): Int? {
        val parts = text.trim().split(':')
        if (parts.size != 2 || !parts.all { it.isNotEmpty() && it.all(Char::isDigit) }) return null
        val minutes = parts[0].toIntOrNull() ?: return null
        val seconds = parts[1].toIntOrNull() ?: return null
        return if (minutes in 0..119 && seconds in 0..59) (minutes * 60 + seconds).takeIf { it > 0 } else null
    }
    fun timeText(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

    fun fatMass(r: HealthRecord): Double? = if (r.kind.bodyFat && r.weightKg != null) r.weightKg * r.result / 100 else null
    fun fatFreeMass(r: HealthRecord): Double? = fatMass(r)?.let { r.weightKg!! - it }

    // Reference categories are descriptive screening context, never personal diagnoses.
    fun reference(r: HealthRecord): String = when (r.kind) {
        HealthKind.BMI -> if (r.age == null || r.age < 20) "CDC adult categories require age 20+. Set age to show a category."
            else "CDC adult category: " + when {
                r.result < 18.5 -> "Underweight (<18.5)"
                r.result < 25 -> "Healthy weight (18.5–<25)"
                r.result < 30 -> "Overweight (25–<30)"
                r.result < 35 -> "Obesity, class 1 (30–<35)"
                r.result < 40 -> "Obesity, class 2 (35–<40)"
                else -> "Obesity, class 3 (≥40)"
            }
        HealthKind.WAIST_HEIGHT -> {
            if (r.age == null || r.age < 18) "NICE adult categories require age 18+."
            else if (r.weightKg == null) "NICE adult ranges: 0.4–<0.5 healthy central adiposity; 0.5–<0.6 increased; ≥0.6 high. Published scope: BMI <35; add weight to check scope."
            else if (r.heightCm == null || r.weightKg / (r.heightCm / 100).let { it * it } >= 35) "Outside NICE's BMI <35 scope for these categories."
            else "NICE central adiposity: " + when {
                r.result < 0.4 -> "Below the published 0.4–<0.5 healthy range; lower is not automatically better."
                r.result < 0.5 -> "Healthy range (0.4–<0.5)"
                r.result < 0.6 -> "Increased (0.5–<0.6): increased diabetes, hypertension and cardiovascular risk"
                else -> "High (≥0.6): further increased diabetes, hypertension and cardiovascular risk"
            }
        }
        HealthKind.WAIST_HIP -> if (r.age == null || r.age < 18 || r.gender.isEmpty()) "WHO adult reference needs age 18+ and profile gender. Common increased-risk cutoffs: M ≥0.90, F ≥0.85."
            else "WHO adult reference: " + if (r.result >= if (r.gender == "M") 0.90 else 0.85) "At/above the increased metabolic-risk cutoff (${if (r.gender == "M") "0.90" else "0.85"})." else "Below the increased metabolic-risk cutoff; this alone does not establish health."
        HealthKind.RFM_FAT -> if (r.age == null || r.age < 20) "RFM mortality study: adults 20+, proposed elevated-risk cutoffs M 30%, F 40%."
            else "RFM study reference: " + (if (r.result >= if (r.gender == "M") 30 else 40) "At/above" else "Below") + " the proposed ${if (r.gender == "M") 30 else 40}% cutoff. Population association, not a personal mortality prediction."
        else -> if (r.kind.cardio) vo2Reference(r) else "NHANES impedance cohort (ages 18–89): lowest joint mortality was reported for M body fat 25–30% with waist/hip 0.95–1.0, and F 30–35% with waist/hip <0.9. These are cohort associations, not healthy/athletic targets; other methods and populations can differ."
    }

    // FRIEND 2015 Table 3: measured treadmill CPX percentiles, ages 20–79 without known CVD.
    private val male = arrayOf(
        doubleArrayOf(29.0,32.1,40.1,48.0,55.2,61.8,66.3), doubleArrayOf(27.2,30.2,35.9,42.4,49.2,56.5,59.8),
        doubleArrayOf(24.2,26.8,31.9,37.8,45.0,52.1,55.6), doubleArrayOf(20.9,22.8,27.1,32.6,39.7,45.6,50.7),
        doubleArrayOf(17.4,19.8,23.7,28.2,34.5,40.3,43.0), doubleArrayOf(16.3,17.1,20.4,24.4,30.4,36.6,39.7))
    private val female = arrayOf(
        doubleArrayOf(21.7,23.9,30.5,37.6,44.7,51.3,56.0), doubleArrayOf(19.0,20.9,25.3,30.2,36.1,41.4,45.8),
        doubleArrayOf(17.0,18.8,22.1,26.7,32.4,38.4,41.7), doubleArrayOf(16.0,17.3,19.9,23.4,27.6,32.0,35.9),
        doubleArrayOf(13.4,14.6,17.2,20.0,23.8,27.0,29.4), doubleArrayOf(13.1,13.6,15.6,18.3,20.8,23.1,24.1))
    fun vo2Reference(r: HealthRecord): String {
        if (r.age !in 20..79 || r.gender !in listOf("M", "F")) return "FRIEND treadmill reference needs age 20–79 and profile gender."
        val row = (if (r.gender == "M") male else female)[r.age!! / 10 - 2]
        val percentiles = listOf(5,10,25,50,75,90,95)
        val index = row.indexOfLast { r.result >= it }
        val band = if (index < 0) "below the 5th" else if (index == 6) "at/above the 95th" else "${percentiles[index]}th–<${percentiles[index+1]}th"
        return "FRIEND 2015: $band percentile (${r.gender}, ages ${r.age / 10 * 10}–${r.age / 10 * 10 + 9}). " +
            if (r.kind == HealthKind.VO2_LAB) "Measured treadmill reference; not a health diagnosis." else "Approximate context only: field/device estimates are not measured treadmill CPX."
    }
}

class HealthRepository(private val db: WorkoutDatabase) {
    suspend fun save(record: HealthRecord): Long = db.withTransaction {
        require(MeasurementInput.parseDay(MeasurementInput.dayText(record.epochDay)) == record.epochDay)
        require(record.formulaVersion == 1)
        val result = requireNotNull(HealthCalculations.calculate(record))
        val saved = record.copy(result = result, notes = record.notes.trim())
        if (record.id == 0L) db.healthDao().insert(saved) else {
            val old = requireNotNull(db.healthDao().get(record.id))
            require(old.kind == record.kind)
            check(db.healthDao().update(saved) == 1)
            record.id
        }
    }
    suspend fun delete(id: Long) = db.withTransaction { check(db.healthDao().delete(id) == 1) }
}
