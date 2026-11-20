package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class ExerciseNamesTest {
    @Test fun whitespaceIsCleanedAndBlankNamesAreRejected() {
        assertEquals("Bench Press", ExerciseNames.clean("  Bench\t  Press  "))
        assertFalse(ExerciseNames.valid(" \t\n "))
        assertTrue(ExerciseNames.valid("a".repeat(100)))
        assertFalse(ExerciseNames.valid("a".repeat(101)))
    }
    @Test fun keysIgnoreCaseAndDoNotDependOnDeviceLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("incline press", ExerciseNames.key(" INCLINE  PRESS "))
            assertEquals(ExerciseNames.key("Bench Press"), ExerciseNames.key(" bench PRESS "))
        } finally { Locale.setDefault(previous) }
    }
}
