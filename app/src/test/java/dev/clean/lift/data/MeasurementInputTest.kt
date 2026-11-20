package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test

class MeasurementInputTest {
    @Test fun datesRejectInvalidFutureAndPreEpochDays() {
        assertNotNull(MeasurementInput.parseDay("2024-02-29"))
        assertNull(MeasurementInput.parseDay("2023-02-29"))
        assertNull(MeasurementInput.parseDay("2024-13-01"))
        assertNull(MeasurementInput.parseDay("2999-01-01"))
        assertNull(MeasurementInput.parseDay("1969-12-31"))
        assertNull(MeasurementInput.parseDay("2024-2-9"))
        assertEquals("2024-02-29", MeasurementInput.dayText(MeasurementInput.parseDay("2024-02-29")!!))
    }
    @Test fun valuesAreExactPositiveAndBounded() {
        assertEquals(185125L, MeasurementInput.parseValue("185.125"))
        assertEquals(35000L, MeasurementInput.parseValue("35,000"))
        assertNull(MeasurementInput.parseValue("0"))
        assertNull(MeasurementInput.parseValue("-5"))
        assertNull(MeasurementInput.parseValue("5.0001"))
        assertNull(MeasurementInput.parseValue("1000.001"))
    }
    @Test fun massAndCircumferenceConversionAreReversibleAndTypeSpecific() {
        val pounds = BodyMeasurement(metric = BodyMetric.BODY_WEIGHT, epochDay = 1000, valueMilliUnits = 185125, unit = MeasurementUnit.LB)
        assertEquals(185.125 * 0.45359237, MeasurementInput.convert(pounds, MeasurementUnit.KG), 0.000001)
        assertEquals(185.125, MeasurementInput.convert(pounds, MeasurementUnit.LB), 0.000001)
        val waist = BodyMeasurement(metric = BodyMetric.WAIST, epochDay = 1000, valueMilliUnits = 35000, unit = MeasurementUnit.IN)
        assertEquals(88.9, MeasurementInput.convert(waist, MeasurementUnit.CM), 0.000001)
        assertEquals(listOf(MeasurementUnit.IN, MeasurementUnit.CM), MeasurementInput.units(BodyMetric.LEFT_BICEP))
    }
    @Test(expected = IllegalArgumentException::class) fun lengthCannotBeConvertedToMass() {
        MeasurementInput.convert(BodyMeasurement(metric = BodyMetric.WAIST, epochDay = 1000, valueMilliUnits = 35000, unit = MeasurementUnit.IN), MeasurementUnit.LB)
    }
}
