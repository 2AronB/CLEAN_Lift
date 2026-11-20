package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test

class MetricComparisonTest {
    private val weight = MetricSeries("weight", "Weight", "kg", "Body", emptyList())
    private val waist = MetricSeries("waist", "Waist", "cm", "Body", emptyList())
    private val height = MetricSeries("height", "Height", "cm", "Body", emptyList())

    @Test fun keepsDistinctVisibleSelections() {
        assertEquals(height to weight, MetricComparison.select(listOf(weight, waist, height), "height", "weight"))
    }

    @Test fun hiddenFirstDoesNotDuplicateTheSecond() {
        assertEquals(weight to waist, MetricComparison.select(listOf(weight, waist), "hidden", "weight"))
    }

    @Test fun identicalOrHiddenSecondFallsBackToAnotherVisibleMetric() {
        listOf("weight", "hidden").forEach { second ->
            assertEquals(weight to waist, MetricComparison.select(listOf(weight, waist), "weight", second))
        }
    }

    @Test fun insufficientVisibleMetricsLeavesSelectionsEmptyRatherThanDuplicated() {
        assertEquals(weight to null, MetricComparison.select(listOf(weight), "weight", "weight"))
        assertEquals(null to null, MetricComparison.select(emptyList(), "weight", "waist"))
    }
}
