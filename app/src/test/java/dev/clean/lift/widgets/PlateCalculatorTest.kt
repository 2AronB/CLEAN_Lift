package dev.clean.lift.widgets

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class PlateCalculatorTest {
    @Test fun targetModeDoesNotAddADuplicateCaption() {
        val target = plateTargets(PlateMode.TARGET, "185", "", "").single()
        assertNull(target.caption)
        assertEquals(0, target.weight.compareTo(BigDecimal("185")))
    }
    @Test fun percentageModeFeedsTheSamePlateSolver() {
        val target = plateTargets(PlateMode.PERCENT, "200", "75", "").single()
        assertEquals(0, target.weight.compareTo(BigDecimal("150")))
        val options = WidgetCalculations.plates(target.weight, BigDecimal("40"), listOf(PlateStock(BigDecimal("25"), 4), PlateStock(BigDecimal("5"), 2)))
        assertNotNull(options.exact)
        assertEquals("75% of 1RM", target.caption)
    }
    @Test fun warmupTargetsKeepUserOrderAndExactPercentages() {
        val targets = plateTargets(PlateMode.WARMUP, "127.5", "", "40, 72.5, 80")
        assertEquals(listOf("40%", "72.5%", "80%"), targets.map { it.caption })
        assertEquals(0, targets[1].weight.compareTo(BigDecimal("92.4375")))
    }
    @Test(expected = IllegalStateException::class) fun warmupRejectsEmptyStep() { plateTargets(PlateMode.WARMUP, "200", "", "40,,80") }
    @Test(expected = IllegalStateException::class) fun percentageRejectsNonFiniteInput() { plateTargets(PlateMode.PERCENT, "200", "NaN", "") }
}
