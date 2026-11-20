package dev.clean.lift.widgets

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class WidgetCalculationsTest {
    private fun d(value: String) = BigDecimal(value)
    private fun stock(vararg sizes: Pair<String, Int>) = sizes.map { PlateStock(d(it.first), it.second) }
    @Test fun plateInventoryIsFiniteAndSymmetric() {
        val options = WidgetCalculations.plates(d("185"), d("45"), stock("45" to 2, "25" to 2, "10" to 1))
        assertEquals(0, options.exact!!.total.compareTo(d("185")))
        assertEquals(listOf(d("45").stripTrailingZeros() to 1, d("25").stripTrailingZeros() to 1), options.exact!!.perSide)
        assertNull(options.higher)
        assertEquals(0, options.lower!!.total.compareTo(d("135")))
    }
    @Test fun platesFindNonGreedyExactCombination() {
        val exact = WidgetCalculations.plates(d("12"), d("0"), stock("4" to 2, "3" to 4)).exact!!
        assertEquals(listOf(d("3") to 2), exact.perSide)
    }
    @Test fun nearbyLoadsAndBelowBarAreExplicit() {
        val options = WidgetCalculations.plates(d("47"), d("45"), stock("2.5" to 2))
        assertNull(options.exact)
        assertEquals(0, options.lower!!.total.compareTo(d("45")))
        assertEquals(0, options.higher!!.total.compareTo(d("50")))
        val below = WidgetCalculations.plates(d("20"), d("45"), emptyList())
        assertNull(below.lower)
        assertEquals(0, below.higher!!.total.compareTo(d("45")))
    }
    @Test fun duplicateSizesSharePhysicalInventory() {
        val result = WidgetCalculations.plates(d("5"), d("0"), stock("2.5" to 1, "2.500" to 1))
        assertNotNull(result.exact)
    }
    @Test fun plateResultsMatchBruteForceInventory() {
        val inventory = stock("7" to 4, "5" to 2, "2" to 4)
        val possible = buildSet { for (a in 0..2) for (b in 0..1) for (c in 0..2) add(10 + 2 * (7*a + 5*b + 2*c)) }
        for (target in 0..60) {
            val result = WidgetCalculations.plates(d(target.toString()), d("10"), inventory)
            assertEquals(possible.contains(target), result.exact != null)
            assertEquals(possible.filter { it < target }.maxOrNull()?.toDouble(), result.lower?.total?.toDouble())
            assertEquals(possible.filter { it > target }.minOrNull()?.toDouble(), result.higher?.total?.toDouble())
            listOfNotNull(result.exact, result.lower, result.higher).forEach { load ->
                assertEquals(load.total.toDouble(), 10 + 2 * load.perSide.sumOf { it.first.toDouble() * it.second }, 0.0)
                load.perSide.forEach { pair -> assertTrue(pair.second <= inventory.first { it.size.compareTo(pair.first) == 0 }.count / 2) }
            }
        }
    }
    @Test fun percentagesKeepExactDecimals() {
        assertEquals(0, WidgetCalculations.percentage(d("127.5"), d("72.5")).compareTo(d("92.4375")))
    }
    @Test fun paceUsesExactMileConversion() {
        val pace = WidgetCalculations.pace(1609.344, 480.0)
        assertEquals(480.0, pace.secondsPerMile, 0.000001)
        assertEquals(7.5, pace.milesPerHour, 0.000001)
    }
    @Test fun splitsPreservePartialLastSegmentAndTotal() {
        val splits = WidgetCalculations.splits(1609.344, 480.0, 400.0)
        assertEquals(5, splits.size)
        assertEquals(1609.344, splits.last().distanceMetres, 0.0)
        assertEquals(480.0, splits.sumOf { it.segmentSeconds }, 0.000001)
        assertEquals(480.0, splits.last().cumulativeSeconds, 0.0)
    }
    @Test fun wholeDistanceCanBeOneSplit() { assertEquals(1, WidgetCalculations.splits(1000.0, 300.0, 1000.0).size) }
    @Test fun inputRejectsNonFiniteAndInvalidTimes() {
        listOf("NaN", "Infinity", "-1", "0").forEach { assertNull(WidgetCalculations.positive(it)) }
        listOf("1:60", "1:2:60", "-1:00", "0:00", "1", "999999999999999:00").forEach { assertNull(WidgetCalculations.duration(it)) }
        assertEquals(3723.0, WidgetCalculations.duration("1:02:03")!!, 0.0)
        assertEquals("1:00", WidgetCalculations.time(59.6))
        listOf("1e2", "NaN", "-1", "1.0001", "2001").forEach { assertNull(WidgetCalculations.load(it)) }
    }
    @Test(expected = IllegalArgumentException::class) fun rejectHugeSplitLists() { WidgetCalculations.splits(1000.0, 300.0, 1.0) }
    @Test(expected = IllegalArgumentException::class) fun rejectNegativeInventory() { WidgetCalculations.plates(d("20"), d("0"), stock("5" to -1)) }
}
