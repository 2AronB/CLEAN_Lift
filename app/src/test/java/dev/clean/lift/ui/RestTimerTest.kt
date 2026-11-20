package dev.clean.lift.ui

import org.junit.Assert.*
import org.junit.Test

class RestTimerTest {
    @Test fun plannedRestIsUsedAndBoundedWithTwoMinuteDefault() {
        assertEquals(120, RestTimer.seconds(null))
        assertEquals(90, RestTimer.seconds(90))
        assertEquals(1, RestTimer.seconds(0))
        assertEquals(600, RestTimer.seconds(10_000))
    }

    @Test fun remainingRoundsUpAndReachesZeroOnlyAtTheEnd() {
        assertEquals(120, RestTimer.remaining(120_000, 0))
        assertEquals(1, RestTimer.remaining(120_000, 119_001))
        assertEquals(0, RestTimer.remaining(120_000, 120_000))
        assertEquals(0, RestTimer.remaining(120_000, 500_000))
    }

    @Test fun extendAddsToRemainingRestOrRestartsFromNowWhenFinished() {
        assertEquals(150_000, RestTimer.extend(120_000, 10_000))
        assertEquals(230_000, RestTimer.extend(120_000, 200_000))
    }

    @Test fun finishedRestLingersBrieflyThenClears() {
        assertFalse(RestTimer.expired(0, 1_000_000))
        assertFalse(RestTimer.expired(120_000, 129_999))
        assertTrue(RestTimer.expired(120_000, 130_000))
    }

    @Test fun formatsMinutesAndSeconds() {
        assertEquals("2:00", RestTimer.format(120))
        assertEquals("0:05", RestTimer.format(5))
        assertEquals("10:00", RestTimer.format(600))
    }
}
