package dev.clean.lift.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SwitchAttemptTest {
    @Test fun initialThresholdThenJokesRepeatWithoutReturningToSilentAttempts() {
        var count = 0
        val sequence = List(12) { nextSwitchAttempt(count).also { count = it } }
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 4, 5, 6, 7, 4), sequence)
    }
}
