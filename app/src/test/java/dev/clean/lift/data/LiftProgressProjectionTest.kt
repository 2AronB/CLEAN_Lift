package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test

class LiftProgressProjectionTest {
    @Test fun groupsRepeatedEntriesBySessionWithoutMixingLiftsOrSameTimeWorkouts() {
        val samples = listOf(
            StrengthSample(5, 2, 1000, 7, 10, 90.0), StrengthSample(2, 1, 1000, 7, 5, 100.0),
            StrengthSample(3, 1, 1000, 7, 3, 120.0), StrengthSample(4, 1, 1000, 8, 5, 999.0),
            StrengthSample(1, 1, 1000, 7, 5, 95.0), StrengthSample(6, 3, 2000, 7, 10, 0.0))
        val points = StrengthEstimates.progress(samples, 7)
        assertEquals(listOf(1L, 2L, 3L), points.map { it.workoutId })
        assertEquals(120.0, points.first().heaviestKg, 0.0)
        assertEquals(100.0, points.first().fiveRepKg!!, 0.0)
        assertNull(points.first().tenRepKg)
        assertEquals(120.0, StrengthEstimates.estimate(points[1])!!, 1e-9)
        assertNull(StrengthEstimates.estimate(points[2]))
        assertTrue(StrengthEstimates.progress(samples, 99).isEmpty())
    }
}
