package dev.clean.lift.widgets

import org.junit.Assert.*
import org.junit.Test

class WidgetTimerTest {
    @Test fun intervalsUseAbsoluteBoundariesAndStopExactly() {
        val p = WidgetTimerPlans.general("Intervals", 20, 10, 3)
        assertEquals(TimerPosition(0, 1, 1, false), p.at(19_999))
        assertEquals(TimerPosition(1, 1, 10_000, false), p.at(20_000))
        assertEquals(TimerPosition(0, 3, 20_000, false), p.at(60_000))
        assertTrue(p.at(90_000).complete); assertTrue(p.at(Long.MAX_VALUE).complete)
    }
    @Test fun tempoSkipsZeroPausesAndKeepsRepCount() {
        val p = WidgetTimerPlans.tempo(listOf(3, 0, 1, 0), 8, false)
        assertEquals(listOf("Lower", "Lift"), p.phases.map { it.label })
        assertEquals(32_000, p.totalMillis)
        assertEquals(TimerPosition(1, 2, 1_000, false), p.at(7_000))
    }
    @Test fun emomAndAmrapHaveDifferentRoundSemantics() {
        assertEquals(600_000, WidgetTimerPlans.general("EMOM", 1, 1, 10).totalMillis)
        assertEquals(1, WidgetTimerPlans.general("AMRAP", 600, 1, 10).rounds)
        assertEquals(500, WidgetTimerPlans.metronome(120, 8).phases.single().millis)
    }
    @Test fun rejectsEmptyZeroAndUnboundedPlans() {
        for (block in listOf<() -> Unit>({ WidgetTimerPlans.tempo(listOf(0,0,0,0), 8, false) },
            { WidgetTimerPlans.general("Intervals", 86400, 1, 2) }, { WidgetTimerPlans.metronome(0, 8) })) {
            assertTrue(runCatching(block).isFailure)
        }
    }
    @Test fun missedFastCuesAreFlaggedInsteadOfCatchingUp() {
        val p=WidgetTimerPlans.metronome(300,100)
        assertFalse(p.missedTransitions(199,200))
        assertTrue(p.missedTransitions(199,600))
        assertFalse(p.missedTransitions(p.totalMillis-1,p.totalMillis))
        val tempo=WidgetTimerPlans.tempo(listOf(1,0,1,0),8,false)
        assertTrue(tempo.missedTransitions(999,2100))
    }
}
