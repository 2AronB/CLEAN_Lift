package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test

class StrengthMetricsTest {
    private val time = 1704110400000L
    private fun sample(exercise: Long, workout: Long, weight: Double, reps: Int = 1) =
        StrengthSample(exercise * 100 + workout, workout, time + workout * StrengthEstimates.DAY_MILLIS, exercise, reps, weight)
    @Test fun singlesAndEstimatesAreSeparateAndRepRecordsAreExact() {
        val single = sample(1,1,100.0)
        val five = sample(1,1,90.0,5)
        assertNull(StrengthMetrics.estimate(single))
        assertEquals(105.0, StrengthMetrics.estimate(five)!!, 1e-9)
        assertNull(StrengthMetrics.estimate(five.copy(reps = 11)))
        assertNull(StrengthMetrics.estimate(five.copy(weightKg = 0.0)))
        assertEquals(90.0, StrengthMetrics.kg(listOf(single,five), StrengthMetric.REP_RECORD,5)!!,1e-9)
        assertNull(StrengthMetrics.kg(listOf(single,five), StrengthMetric.REP_RECORD,8))
        assertEquals(100.0, StrengthMetrics.kg(listOf(single,five), StrengthMetric.SINGLE,5)!!,1e-9)
    }
    @Test fun relativeStrengthRequiresSameDayWeightAndNeverUsesAFutureWeight() {
        val s = sample(1,1,100.0)
        val day = MeasurementInput.parseDay(MeasurementInput.today(s.performedAt))!!
        val old = BodyMeasurement(id=1,metric=BodyMetric.BODY_WEIGHT,epochDay=day-1,valueMilliUnits=80000,unit=MeasurementUnit.KG)
        val future = old.copy(id=2,epochDay=day+1)
        val matching = old.copy(id=3,epochDay=day)
        assertNull(StrengthMetrics.sameDayBodyweight(listOf(s),listOf(old,future)))
        val series = StrengthMetrics.series(listOf(Exercise(1,"Lift","lift")),listOf(s),setOf(1),listOf(matching),false,5)
        assertEquals(1.25,series.first { it.strengthMetric==StrengthMetric.RELATIVE_SINGLE }.points.single().value,1e-9)
        assertTrue(series.first { it.strengthMetric==StrengthMetric.RELATIVE_ESTIMATE }.points.isEmpty())
    }
    @Test fun repeatedLiftOccurrencesCombinePerWorkoutWithoutCombiningDifferentWorkouts() {
        val sets = listOf(sample(1,1,80.0,5),sample(1,1,90.0,5).copy(setId=999),sample(1,2,85.0,5))
        val series = StrengthMetrics.series(listOf(Exercise(1,"Renamed","renamed")),sets,setOf(1),emptyList(),false,5)
        assertEquals(listOf(90.0,85.0),series.first { it.strengthMetric==StrengthMetric.REP_RECORD }.points.map { it.value })
        assertTrue(series.all { it.title.startsWith("Renamed") })
        assertEquals(6,series.size)
    }
    @Test fun bigThreeDifferentDateBestSumIsNotASessionTotal() {
        val sets = listOf(sample(1,1,100.0),sample(2,2,80.0),sample(3,3,140.0))
        val series = StrengthMetrics.bigThree(sets.asReversed(),listOf(1,2,3),false)
        assertTrue(series.first { it.key=="big3.SINGLE.SESSION" }.points.isEmpty())
        assertEquals(320.0,series.first { it.key=="big3.SINGLE.BEST" }.points.single().value,1e-9)
        assertTrue(series.first { it.key=="big3.ESTIMATED.BEST" }.points.isEmpty())
    }
    @Test fun sameSessionTotalsNeedEveryMappedLiftAndKeepEstimatedModeDistinct() {
        val sets = listOf(sample(1,1,100.0,5),sample(2,1,80.0,5),sample(3,1,140.0,5))
        val series = StrengthMetrics.bigThree(sets,listOf(1,2,3),false)
        assertEquals(320 * (1 + 5/30.0),series.first { it.key=="big3.ESTIMATED.SESSION" }.points.single().value,1e-9)
        assertTrue(series.first { it.key=="big3.SINGLE.SESSION" }.points.isEmpty())
        assertTrue(StrengthMetrics.bigThree(sets,listOf(1,1,3),false).all { it.points.isEmpty() })
        assertTrue(StrengthMetrics.bigThree(sets,listOf(1,null,3),false).all { it.points.isEmpty() })
    }
    @Test fun runningBestSumNeverFallsAndUnitsOnlyAffectDisplay() {
        val sets = listOf(sample(1,1,100.0),sample(2,1,80.0),sample(3,1,140.0),sample(1,2,90.0),sample(2,3,90.0))
        val kg = StrengthMetrics.bigThree(sets,listOf(1,2,3),false).first { it.key=="big3.SINGLE.BEST" }.points
        val lb = StrengthMetrics.bigThree(sets,listOf(1,2,3),true).first { it.key=="big3.SINGLE.BEST" }.points
        assertEquals(listOf(320.0,320.0,330.0),kg.map { it.value })
        assertEquals(330.0/StrengthEstimates.LB_TO_KG,lb.last().value,1e-9)
        assertEquals(100.0,sets.first().weightKg,0.0)
    }
    @Test fun bigThreeProvenanceKeepsOriginalDatesAndExcludesLaterRecords() {
        val first = listOf(sample(1,1,100.0), sample(2,2,80.0), sample(3,3,140.0))
        val later = sample(1,4,150.0)
        val sources = StrengthMetrics.bigThreeComponents((first + later).asReversed(), listOf(1,2,3), 3, false, true)
        assertEquals(listOf(1L,2L,3L), sources.map { it.workoutId })
        assertEquals(320.0, sources.sumOf { it.weightKg }, 0.0)
        assertEquals(1, StrengthMetrics.bigThreeComponents(first, listOf(1,2,3), 3, false, false).size)
    }
}
