package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test

class StrengthEstimatesTest {
    @Test fun fiveAndTenRepEstimatesMatchFormulaAndRejectUnsupportedAttempts() {
        assertEquals(116.6666667, StrengthEstimates.epley(100.0, 5)!!, 0.00001)
        assertEquals(133.3333333, StrengthEstimates.epley(100.0, 10)!!, 0.00001)
        assertNull(StrengthEstimates.epley(0.0, 5))
        assertNull(StrengthEstimates.epley(-100.0, 5))
        assertNull(StrengthEstimates.epley(Double.NaN, 5))
        assertNull(StrengthEstimates.epley(100.0, 8))
    }
    @Test fun repFilterSelectsEligibleBestAndConvertsUnitsWithoutChangingRecords() {
        val point = ExerciseProgressPoint(1, 1000, 120.0, 110.0, 100.0)
        assertEquals(110 * (1 + 5 / 30.0), StrengthEstimates.estimate(point, 5)!!, 0.00001)
        assertEquals(100 * (1 + 10 / 30.0), StrengthEstimates.estimate(point)!!, 0.00001)
        assertEquals(185.0, StrengthEstimates.inUnit(185 * StrengthEstimates.LB_TO_KG, WeightUnit.LB), 0.00001)
        assertNull(StrengthEstimates.estimate(point.copy(fiveRepKg = null, tenRepKg = null)))
    }
    @Test fun scenariosCompoundOnlyChosenRateAndNeverMutateHistoricalBase() {
        assertEquals(100.0, StrengthEstimates.scenario(100.0, 365, 0.0), 0.00001)
        assertEquals(90.0, StrengthEstimates.scenario(100.0, 30, 10.0), 0.00001)
        assertEquals(81.0, StrengthEstimates.scenario(100.0, 60, 10.0), 0.00001)
        assertEquals(100.0, StrengthEstimates.scenario(100.0, 0, 10.0), 0.00001)
        assertEquals(0L, StrengthEstimates.ageDays(2000, 1000))
    }
    @Test(expected = IllegalArgumentException::class) fun invalidLossRateIsRejected() {
        StrengthEstimates.scenario(100.0, 30, -1.0)
    }
}
