package dev.clean.lift.widgets

import java.math.BigDecimal
import java.math.RoundingMode

enum class LoadUnit(val label: String) { LB("lb"), KG("kg") }
enum class DistanceUnit(val label: String, val metres: Double) {
    METRES("m", 1.0), KM("km", 1000.0), MILES("mi", 1609.344)
}

data class PlateStock(val size: BigDecimal, val count: Int)
data class PlateLoad(val total: BigDecimal, val perSide: List<Pair<BigDecimal, Int>>)
data class PlateOptions(val exact: PlateLoad?, val lower: PlateLoad?, val higher: PlateLoad?)
data class PaceResult(val secondsPerKm: Double, val secondsPerMile: Double, val kmPerHour: Double, val milesPerHour: Double)
data class Split(val distanceMetres: Double, val segmentSeconds: Double, val cumulativeSeconds: Double)

object WidgetCalculations {
    // Plain decimal input only. Exact thousandths preserve equipment sizes without floating-point drift.
    fun load(text: String, allowZero: Boolean = false): BigDecimal? {
        val trimmed = text.trim()
        if (!Regex("[0-9]+(?:\\.[0-9]{1,3})?").matches(trimmed)) return null
        return trimmed.toBigDecimalOrNull()?.takeIf { it <= BigDecimal(2000) && if (allowZero) it.signum() >= 0 else it.signum() > 0 }
    }

    fun positive(text: String, max: Double = 1_000_000.0): Double? =
        text.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 && it <= max }

    fun duration(text: String): Double? {
        val parts = text.trim().split(':')
        if (parts.size !in 2..3 || !parts.all { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }) return null
        val numbers = parts.map { it.toLongOrNull() ?: return null }
        if (numbers[0] > 999 || numbers.drop(1).any { it !in 0..59 }) return null
        val seconds = if (parts.size == 2) numbers[0] * 60 + numbers[1] else numbers[0] * 3600 + numbers[1] * 60 + numbers[2]
        return seconds.toDouble().takeIf { it in 1.0..86_400.0 }
    }

    fun time(seconds: Double): String {
        require(seconds.isFinite() && seconds >= 0 && seconds <= 1_000_000_000_000)
        val rounded = BigDecimal.valueOf(seconds).setScale(0, RoundingMode.HALF_UP).toLong()
        return if (rounded >= 3600) "${rounded / 3600}:${(rounded / 60 % 60).toString().padStart(2, '0')}:${(rounded % 60).toString().padStart(2, '0')}"
        else "${rounded / 60}:${(rounded % 60).toString().padStart(2, '0')}"
    }

    fun percentage(base: BigDecimal, percent: BigDecimal): BigDecimal {
        require(base > BigDecimal.ZERO && base <= BigDecimal(2000) && percent > BigDecimal.ZERO && percent <= BigDecimal(200))
        return base.multiply(percent).divide(BigDecimal(100))
    }

    fun pace(distanceMetres: Double, seconds: Double): PaceResult {
        require(distanceMetres.isFinite() && distanceMetres in 1.0..1_000_000.0 && seconds.isFinite() && seconds in 1.0..86_400.0)
        val speed = distanceMetres / seconds
        return PaceResult(1000 / speed, 1609.344 / speed, speed * 3.6, speed * 3600 / 1609.344)
    }

    fun splits(distanceMetres: Double, seconds: Double, segmentMetres: Double): List<Split> {
        pace(distanceMetres, seconds) // shared validation
        require(segmentMetres.isFinite() && segmentMetres >= 1 && segmentMetres <= distanceMetres)
        val count = kotlin.math.ceil(distanceMetres / segmentMetres).toInt()
        require(count <= 500) { "Choose a longer split distance (maximum 500 splits)." }
        return (1..count).map { index ->
            val end = minOf(index * segmentMetres, distanceMetres)
            val start = (index - 1) * segmentMetres
            Split(end, seconds * (end - start) / distanceMetres, seconds * end / distanceMetres)
        }
    }

    /** Bounded coin change on one side of a symmetrically loaded bar. Odd spare plates are unused.
     * No greedy approximation: each physical inventory count is respected. */
    fun plates(target: BigDecimal, bar: BigDecimal, stock: List<PlateStock>): PlateOptions {
        require(target >= BigDecimal.ZERO && target <= BigDecimal(4000) && bar >= BigDecimal.ZERO && bar <= BigDecimal(2000))
        require(stock.size <= 16 && stock.all { it.size > BigDecimal.ZERO && it.size <= BigDecimal(1000) && it.count in 0..100 && it.size.stripTrailingZeros().scale() <= 3 })
        val coins = stock.groupBy { it.size.stripTrailingZeros() }.map { (size, entries) ->
            val count = entries.sumOf { it.count }
            require(count <= 100) { "At most 100 plates of each size." }
            size to count / 2
        }.filter { it.second > 0 }.sortedByDescending { it.first }
        val weights = coins.map { it.first.movePointRight(3).intValueExact() }
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val divisor = weights.fold(0, ::gcd).coerceAtLeast(1)
        val scaled = weights.map { it / divisor }
        val total = coins.indices.sumOf { scaled[it].toLong() * coins[it].second }
        require(total <= 200_000) { "Inventory has too many fine increments. Use fewer plates or coarser sizes." }
        var reachable = BooleanArray(total.toInt() + 1).also { it[0] = true }
        val choices = Array(coins.size) { IntArray(reachable.size) }
        coins.indices.forEach { i ->
            val remaining = IntArray(reachable.size) { -1 }
            for (sum in reachable.indices) {
                remaining[sum] = when {
                    reachable[sum] -> coins[i].second
                    sum >= scaled[i] && remaining[sum - scaled[i]] > 0 -> remaining[sum - scaled[i]] - 1
                    else -> -1
                }
                if (remaining[sum] >= 0) choices[i][sum] = coins[i].second - remaining[sum]
            }
            reachable = BooleanArray(reachable.size) { remaining[it] >= 0 }
        }
        fun actual(sum: Int) = bar.add(BigDecimal(sum.toLong() * divisor * 2).movePointLeft(3))
        fun result(sum: Int): PlateLoad {
            var rest = sum
            val side = coins.indices.reversed().mapNotNull { i ->
                val count = choices[i][rest]
                rest -= scaled[i] * count
                (coins[i].first to count).takeIf { count > 0 }
            }.sortedByDescending { it.first }
            check(rest == 0)
            return PlateLoad(actual(sum), side)
        }
        var exact: Int? = null
        var lower: Int? = null
        var higher: Int? = null
        for (sum in reachable.indices) if (reachable[sum]) {
            when (actual(sum).compareTo(target)) {
                0 -> exact = sum
                -1 -> lower = sum
                1 -> if (higher == null) higher = sum
            }
        }
        return PlateOptions(exact?.let(::result), lower?.let(::result), higher?.let(::result))
    }
}
