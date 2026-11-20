package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test

class DailyMeasurementsTest {
    @Test fun skipsBlanksAndPreservesExactEnteredUnits() {
        val records = DailyMeasurementInput.records(20_000, listOf(
            DailyMeasurement(BodyMetric.BODY_WEIGHT, "185.125", MeasurementUnit.LB),
            DailyMeasurement(BodyMetric.WAIST, "  ", MeasurementUnit.CM),
            DailyMeasurement(BodyMetric.HEIGHT, "180.1", MeasurementUnit.CM)))
        assertEquals(listOf(185125L, 180100L), records.map { it.valueMilliUnits })
        assertEquals(listOf(MeasurementUnit.LB, MeasurementUnit.CM), records.map { it.unit })
        assertTrue(records.all { it.id == 0L && it.epochDay == 20_000L })
    }
    @Test fun rejectsDuplicateMetricsMixedUnitsAndPartialInvalidInput() {
        val weight = DailyMeasurement(BodyMetric.BODY_WEIGHT, "80", MeasurementUnit.KG)
        listOf(listOf(weight, weight), listOf(weight.copy(unit = MeasurementUnit.CM)),
            listOf(weight, DailyMeasurement(BodyMetric.WAIST, "bad", MeasurementUnit.CM)),
            listOf(weight.copy(value = ""))).forEach { entries ->
            assertTrue(runCatching { DailyMeasurementInput.records(20_000, entries) }.isFailure)
        }
    }
}
