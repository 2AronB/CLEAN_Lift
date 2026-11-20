package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test

class SetInputTest {
    @Test fun decimalWeightsRemainExactAndRoundTrip() {
        assertEquals(SetValues(185125, 8), SetInput.parse("185.125", "8"))
        assertEquals(SetValues(72500, 12), SetInput.parse(" 72,5 ", " 12 "))
        assertEquals(SetValues(0, 1), SetInput.parse("0", "1"))
        listOf(0L, 1L, 72500L, 185125L, SetInput.MAX_WEIGHT_MILLI).forEach {
            assertEquals(it, SetInput.parseWeight(SetInput.weightText(it)))
        }
    }

    @Test fun invalidNumbersAndExcessPrecisionAreRejected() {
        listOf("", " ", "-1", "NaN", "Infinity", "1e3", "12.3456", "1,000.25", "1000000.001", "999999999999999999999999", ".", "1.").forEach {
            assertNull("Unexpected valid weight: $it", SetInput.parseWeight(it))
        }
        listOf("", "0", "-1", "1.5", "1000", "2147483648", "1e2").forEach {
            assertNull("Unexpected valid reps: $it", SetInput.parseReps(it))
        }
        assertEquals(999, SetInput.parseReps("999"))
    }
}
