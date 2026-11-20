package dev.clean.lift.widgets

import java.math.BigDecimal
import java.math.RoundingMode

enum class AfEvent(val title: String, val unit: String, val seconds: Int? = null) {
    PUSH_UP("Push-ups • 1 minute", "reps", 60), HAND_RELEASE("Hand-release push-ups • 2 minutes", "reps", 120),
    SIT_UP("Sit-ups • 1 minute", "reps", 60), CROSS_LEG("Cross-leg reverse crunch • 2 minutes", "reps", 120),
    PLANK("Forearm plank", "seconds"), RUN("2-mile run", "seconds"), HAMR("20 m HAMR", "shuttles")
}
internal data class AfThreshold(val points: Double, val values: IntArray)
data class AfInputs(val age: Int, val sex: String, val heightInches: BigDecimal, val waistInches: BigDecimal,
    val strength: AfEvent, val strengthResult: Int, val core: AfEvent, val coreResult: Int, val cardio: AfEvent, val cardioResult: Int)
data class AfScore(val body: Double, val strength: Double, val core: Double, val cardio: Double, val ratioHundredths: Int) {
    val total: Double get() = body + strength + core + cardio
    val minimaMet: Boolean get() = strength > 0 && core > 0 && cardio > 0
    val passes: Boolean get() = minimaMet && total >= 75
}
object AirForceScoring {
    const val MANUAL_URL = "https://static.e-publishing.af.mil/production/1/af_a1/publication/afman36-2905/dafman36-2905.pdf"
    const val CHART_URL = "https://www.afpc.af.mil/Portals/70/documents/FITNESS/PFRA%20Scoring%20Charts.pdf"
    const val RESOURCES_URL = "https://www.afpc.af.mil/Career-Management/Fitness-Program/lookatthescorecharts/"
    fun column(age: Int, sex: String): Int {
        require(age in 17..120 && sex in listOf("M", "F"))
        return (if (age < 25) 0 else ((age - 25) / 5 + 1).coerceAtMost(8)) * 2 + if (sex == "F") 1 else 0
    }
    fun points(event: AfEvent, age: Int, sex: String, result: Int): Double {
        require(result in 0..(if (event in listOf(AfEvent.RUN, AfEvent.PLANK)) 86400 else 1000))
        if (event == AfEvent.RUN && result == 0) return 0.0
        val col = column(age, sex)
        return AirForceTables.rows.getValue(event).firstOrNull { if (event == AfEvent.RUN) result <= it.values[col] else result >= it.values[col] }?.points ?: 0.0
    }
    fun thresholds(event: AfEvent, age: Int, sex: String): List<Pair<Int, Double>> {
        val col = column(age, sex)
        return AirForceTables.rows.getValue(event).map { it.values[col] to it.points }.distinctBy { it.first }
    }
    fun roundedWaist(readings: List<BigDecimal>): BigDecimal {
        require(readings.size == 3 && readings.all { it >= BigDecimal("10") && it <= BigDecimal("100") })
        val values = readings.map { it.multiply(BigDecimal(2)).setScale(0, RoundingMode.DOWN).divide(BigDecimal(2)) }
        require(values.maxOrNull()!!.subtract(values.minOrNull()!!) <= BigDecimal.ONE) { "Waist readings differ by more than 1 inch. Take additional readings until three agree within 1 inch." }
        return values.reduce(BigDecimal::add).divide(BigDecimal(3), 10, RoundingMode.DOWN).multiply(BigDecimal(2)).setScale(0, RoundingMode.DOWN).divide(BigDecimal(2))
    }
    fun ratioHundredths(waist: BigDecimal, height: BigDecimal): Int = waist.multiply(BigDecimal(100)).divide(height, 0, RoundingMode.DOWN).intValueExact()
    fun bodyPoints(ratioHundredths: Int): Double = when {
        ratioHundredths <= 49 -> 20.0
        ratioHundredths <= 54 -> (69 - ratioHundredths).toDouble()
        ratioHundredths <= 59 -> (60 - ratioHundredths) * 2.5
        else -> 0.0
    }
    fun score(i: AfInputs): AfScore {
        column(i.age, i.sex)
        require(i.heightInches in BigDecimal("36")..BigDecimal("100") && i.heightInches.multiply(BigDecimal(2)).stripTrailingZeros().scale() <= 0)
        require(i.waistInches in BigDecimal("10")..BigDecimal("100") && i.waistInches.multiply(BigDecimal(2)).stripTrailingZeros().scale() <= 0)
        require(i.strength in listOf(AfEvent.PUSH_UP, AfEvent.HAND_RELEASE) && i.core in listOf(AfEvent.SIT_UP, AfEvent.CROSS_LEG, AfEvent.PLANK) && i.cardio in listOf(AfEvent.RUN, AfEvent.HAMR))
        val ratio = ratioHundredths(i.waistInches, i.heightInches)
        return AfScore(bodyPoints(ratio), points(i.strength,i.age,i.sex,i.strengthResult), points(i.core,i.age,i.sex,i.coreResult), points(i.cardio,i.age,i.sex,i.cardioResult), ratio)
    }
    // Other components are fixed. Search actual table thresholds, never interpolate.
    fun explore(i: AfInputs, variable: String, target: Double): Pair<Int, AfScore>? {
        require(target.isFinite() && target in 75.0..100.0)
        val event = when (variable) { "Strength" -> i.strength; "Core" -> i.core; "Cardio" -> i.cardio; else -> error("Choose a component") }
        val choices = thresholds(event,i.age,i.sex).map { it.first }.distinct().sorted().let { if (event == AfEvent.RUN) it.reversed() else it }
        return choices.firstNotNullOfOrNull { result ->
            val input = when(variable) { "Strength" -> i.copy(strengthResult = result); "Core" -> i.copy(coreResult = result); else -> i.copy(cardioResult = result) }
            val score = score(input)
            if (score.minimaMet && score.total >= target) result to score else null
        }
    }
}

object AirForcePracticeRules {
    const val MIN_REST_MILLIS = 300_000L
    const val WINDOW_MILLIS = 10_800_000L
    fun canStartNext(finished: Long, now: Long): Boolean = now >= finished && now - finished >= MIN_REST_MILLIS
    fun withinWindow(started: Long, finished: Long): Boolean = finished >= started && finished - started <= WINDOW_MILLIS
}
