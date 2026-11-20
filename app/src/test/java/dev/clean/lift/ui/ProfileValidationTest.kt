package dev.clean.lift.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidationTest {
    @Test fun genderIsOptionalAndLimitedToStoredChoices() {
        listOf("", "M", "F").forEach { assertTrue(validProfile("", "", it)) }
        listOf("m", "f", "unknown", " M ").forEach { assertFalse(validProfile("", "", it)) }
    }
    @Test fun optionalProfileAndAgeBounds() {
        assertTrue(validProfile("", ""))
        assertTrue(validProfile(" Sam ", " 1 "))
        assertTrue(validProfile("A".repeat(80), "120"))
        listOf("0", "121", "-1", "20.5", "abc", "999999999999", "١٢").forEach { assertFalse(validProfile("", it)) }
        assertFalse(validProfile("A".repeat(81), "20"))
    }
}
