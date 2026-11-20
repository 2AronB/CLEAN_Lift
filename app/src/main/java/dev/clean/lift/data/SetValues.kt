package dev.clean.lift.data

import java.math.BigDecimal

data class SetValues(val weightMilliUnits: Long, val reps: Int)

object SetInput {
    const val MAX_WEIGHT_MILLI = 1_000_000_000L
    const val MAX_REPS = 999

    fun parseWeight(value: String): Long? {
        val text = value.trim().replace(',', '.')
        if (!Regex("\\d+(?:\\.\\d{1,3})?").matches(text)) return null
        return try {
            BigDecimal(text).movePointRight(3).longValueExact().takeIf { it in 0..MAX_WEIGHT_MILLI }
        } catch (_: ArithmeticException) { null }
        catch (_: NumberFormatException) { null }
    }

    fun parseReps(value: String): Int? = value.trim().takeIf { Regex("\\d+").matches(it) }
        ?.toIntOrNull()?.takeIf { it in 1..MAX_REPS }

    fun parse(weight: String, reps: String): SetValues? {
        val parsedWeight = parseWeight(weight) ?: return null
        val parsedReps = parseReps(reps) ?: return null
        return SetValues(parsedWeight, parsedReps)
    }

    fun weightText(milliUnits: Long): String = BigDecimal.valueOf(milliUnits, 3).stripTrailingZeros().toPlainString()
}
