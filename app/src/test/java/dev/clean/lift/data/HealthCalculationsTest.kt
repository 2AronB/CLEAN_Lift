package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test

class HealthCalculationsTest {
    private fun record(kind: HealthKind) = HealthRecord(kind = kind, epochDay = 0, result = 0.0, gender = "M", age = 30)
    @Test fun bmiAndRatioExamplesUseCanonicalUnits() {
        assertEquals(25.0, HealthCalculations.calculate(record(HealthKind.BMI).copy(heightCm = 180.0, weightKg = 81.0))!!, 1e-9)
        assertEquals(0.5, HealthCalculations.calculate(record(HealthKind.WAIST_HEIGHT).copy(heightCm = 180.0, waistCm = 90.0))!!, 1e-9)
        assertEquals(0.9, HealthCalculations.calculate(record(HealthKind.WAIST_HIP).copy(waistCm = 90.0, hipsCm = 100.0))!!, 1e-9)
    }
    @Test fun navyUsesDifferentSitesAndSexSpecificEquations() {
        val m = record(HealthKind.NAVY_FAT).copy(heightCm = 70 * 2.54, neckCm = 15 * 2.54, abdomenCm = 34 * 2.54)
        val f = m.copy(gender = "F", heightCm = 64 * 2.54, neckCm = 13 * 2.54, waistCm = 29 * 2.54, hipsCm = 39 * 2.54)
        assertEquals(17.513, HealthCalculations.calculate(m)!!, 0.01)
        assertEquals(29.214, HealthCalculations.calculate(f)!!, 0.01)
        assertEquals(HealthCalculations.calculate(m)!!, HealthCalculations.calculate(m.copy(waistCm = 10.0, hipsCm = 10.0))!!, 1e-9)
        assertNull(HealthCalculations.calculate(m.copy(abdomenCm = 20.0)))
        assertNull(HealthCalculations.calculate(f.copy(hipsCm = null)))
        assertNull(HealthCalculations.calculate(m.copy(gender = "")))
    }
    @Test fun rfmRemainsADistinctEstimate() {
        val r = record(HealthKind.RFM_FAT).copy(heightCm = 180.0, waistCm = 90.0)
        assertEquals(24.0, HealthCalculations.calculate(r)!!, 1e-9)
        assertEquals(36.0, HealthCalculations.calculate(r.copy(gender = "F"))!!, 1e-9)
        assertNull(HealthCalculations.calculate(r.copy(waistCm = 1.0)))
    }
    @Test fun rockportConvertsKgAndSecondsAndUsesFinishingPulse() {
        val r = record(HealthKind.ROCKPORT).copy(weightKg = 160 * StrengthEstimates.LB_TO_KG, age = 30, timeSeconds = 900, heartRate = 110)
        assertEquals(49.0445, HealthCalculations.calculate(r)!!, 0.0001)
        assertEquals(6.315, HealthCalculations.calculate(r)!! - HealthCalculations.calculate(r.copy(gender = "F"))!!, 1e-9)
        assertNull(HealthCalculations.calculate(r.copy(age = 19)))
        assertNull(HealthCalculations.calculate(r.copy(age = 70)))
        assertNull(HealthCalculations.calculate(r.copy(heartRate = null)))
        assertNull(HealthCalculations.calculate(r.copy(timeSeconds = 0)))
    }
    @Test fun cooperKeepsDistanceEquationIndependentOfProfile() {
        val r = record(HealthKind.COOPER).copy(distanceMetres = 2500.0)
        assertEquals(44.603, HealthCalculations.calculate(r)!!, 0.01)
        assertEquals(HealthCalculations.calculate(r)!!, HealthCalculations.calculate(r.copy(gender = "F", age = 60))!!, 1e-9)
        assertNull(HealthCalculations.calculate(r.copy(distanceMetres = 500.0)))
        assertNull(HealthCalculations.calculate(r.copy(distanceMetres = Double.NaN)))
    }
    @Test fun clockInputRejectsAmbiguousOrOverflowingTimes() {
        assertEquals(905, HealthCalculations.parseTime("15:05"))
        assertEquals("15:05", HealthCalculations.timeText(905))
        listOf("15.05", "15:60", "0:00", "-1:20", "2147483647:00", "1:2:3").forEach { assertNull(HealthCalculations.parseTime(it)) }
    }
    @Test fun bmiCategoriesHaveExactBoundariesAndAdultScope() {
        val r = record(HealthKind.BMI)
        assertTrue(HealthCalculations.reference(r.copy(result = 18.5)).contains("Healthy weight"))
        assertTrue(HealthCalculations.reference(r.copy(result = 25.0)).contains("Overweight"))
        assertTrue(HealthCalculations.reference(r.copy(result = 30.0)).contains("class 1"))
        assertTrue(HealthCalculations.reference(r.copy(result = 35.0)).contains("class 2"))
        assertTrue(HealthCalculations.reference(r.copy(result = 40.0)).contains("class 3"))
        assertTrue(HealthCalculations.reference(r.copy(age = 19)).contains("require age"))
    }
    @Test fun waistReferencesDoNotClassifyOutsidePublishedScope() {
        val r = record(HealthKind.WAIST_HEIGHT).copy(heightCm = 180.0, weightKg = 81.0, result = 0.5)
        assertTrue(HealthCalculations.reference(r).contains("Increased"))
        assertTrue(HealthCalculations.reference(r.copy(result = 0.6)).contains("High"))
        assertTrue(HealthCalculations.reference(r.copy(result = 0.3)).contains("not automatically better"))
        assertTrue(HealthCalculations.reference(r.copy(weightKg = 120.0)).contains("Outside"))
        assertTrue(HealthCalculations.reference(r.copy(weightKg = null)).contains("add weight"))
    }
    @Test fun friendComparisonUsesSavedAgeGenderAndSeparatesEstimatedContext() {
        val r = record(HealthKind.VO2_LAB).copy(age = 25, result = 48.0)
        assertTrue(HealthCalculations.reference(r).contains("50th–<75th"))
        assertTrue(HealthCalculations.reference(r.copy(gender = "F")).contains("75th–<90th"))
        assertTrue(HealthCalculations.reference(r.copy(kind = HealthKind.COOPER)).contains("Approximate context"))
        assertTrue(HealthCalculations.reference(r.copy(age = 80)).contains("needs age"))
        assertTrue(HealthCalculations.reference(r.copy(result = 66.3)).contains("at/above the 95th"))
    }
    @Test fun fatMassAndSeriesDoNotMixMethodsOrInventMissingWeight() {
        val a = record(HealthKind.FAT_DXA).copy(id = 1, result = 20.0, weightKg = 80.0)
        val b = a.copy(id = 2, kind = HealthKind.FAT_BIA, result = 25.0, weightKg = null)
        assertEquals(16.0, HealthCalculations.fatMass(a)!!, 1e-9)
        assertEquals(64.0, HealthCalculations.fatFreeMass(a)!!, 1e-9)
        assertNull(HealthCalculations.fatMass(b))
        val series = MetricSeriesBuilder.build(emptyList(), listOf(a,b), false)
        assertEquals(1, series.first { it.key == "health.FAT_DXA" }.points.size)
        assertEquals(1, series.first { it.key == "health.FAT_BIA" }.points.size)
        assertTrue(series.first { it.key == "ffmass.FAT_BIA" }.points.isEmpty())
    }
    @Test fun sevenDayAverageUsesRecordedCalendarDaysAndKeepsRawData() {
        val points = listOf(MetricPoint(1,80.0,1), MetricPoint(7,82.0,2), MetricPoint(8,90.0,3))
        assertEquals(listOf(80.0,81.0,86.0), MetricSeriesBuilder.weightAverage(points).map { it.value })
        assertEquals(90.0, points.last().value, 0.0)
        assertEquals(12.5, MetricSeriesBuilder.normalized(points).last().value, 1e-9)
    }
    @Test fun rejectsNonFiniteInputsAndInvalidOptionalSnapshots() {
        val r = record(HealthKind.BMI).copy(heightCm = 180.0, weightKg = 81.0)
        assertNull(HealthCalculations.calculate(r.copy(heightCm = Double.POSITIVE_INFINITY)))
        assertNull(HealthCalculations.calculate(r.copy(neckCm = -1.0)))
        assertNull(HealthCalculations.calculate(r.copy(gender = "X")))
        assertNull(HealthCalculations.calculate(record(HealthKind.FAT_DXA).copy(result = 101.0)))
        assertNull(HealthCalculations.calculate(record(HealthKind.VO2_LAB).copy(result = Double.NaN)))
    }
}
