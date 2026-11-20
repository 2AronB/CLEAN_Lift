package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test

class ExerciseCatalogTest {
    @Test fun catalogIntegrityAndLegacyCoverage() {
        val entries = ExerciseCatalog.exercises
        assertEquals(69, entries.size)
        assertEquals(entries.size, entries.map { it.key }.toSet().size)
        entries.forEach {
            assertTrue(it.key.matches(Regex("[a-z0-9]+(-[a-z0-9]+)*")))
            assertTrue(it.primary.isNotEmpty())
            assertTrue(it.primary.intersect(it.secondary).isEmpty())
            assertTrue(it.primary.intersect(it.stabilizers).isEmpty())
            assertTrue(it.secondary.intersect(it.stabilizers).isEmpty())
            assertTrue(it.equipmentAlternatives.isNotEmpty())
            assertTrue(it.equipmentAlternatives.all { group -> group.isNotEmpty() })
            assertTrue(it.sourceIds.isNotEmpty())
            assertTrue(it.sourceIds.all { id -> id in ExerciseCatalog.sources })
        }
        CommonExercises.names.forEach { assertEquals(it, 1, ExerciseCatalog.legacyCandidates(it).size) }
        assertTrue(ExerciseCatalog.legacyCandidates("My custom exercise").isEmpty())
    }
    @Test fun equipmentRequiresCompleteGroupButAcceptsAlternatives() {
        val bench = ExerciseCatalog.find("bench-barbell")!!
        assertFalse(bench.availableWith(setOf(Equipment.BARBELL, Equipment.PLATES)))
        assertTrue(bench.availableWith(bench.equipmentAlternatives.single()))
        val goblet = ExerciseCatalog.find("goblet-squat")!!
        assertTrue(goblet.availableWith(setOf(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE)))
        assertTrue(goblet.availableWith(setOf(Equipment.DUMBBELL_SINGLE, Equipment.FLOOR_SPACE)))
        assertFalse(goblet.availableWith(setOf(Equipment.FLOOR_SPACE)))
    }
    @Test fun selectionBlocksUnsupportedLoggingAndTechnicalPowerByDefault() {
        val allGear = Equipment.entries.toSet()
        val ordinary = ExerciseCatalog.eligible(allGear, setOf(RecordingMode.LOAD_REPS, RecordingMode.REPS_ONLY))
        assertFalse(ordinary.any { it.key in setOf("plank", "farmer-carry", "snatch", "box-jump") })
        assertTrue(ordinary.any { it.key == "push-up" })
        val power = ExerciseCatalog.eligible(allGear, RecordingMode.entries.toSet(), Skill.MODERATE, true)
        assertTrue(power.any { it.key == "swing-kettlebell" })
        assertFalse(power.any { it.key == "snatch" })
        assertEquals(ordinary, ExerciseCatalog.eligible(allGear, setOf(RecordingMode.LOAD_REPS, RecordingMode.REPS_ONLY)))
        assertEquals(ordinary.map { it.key }.sorted(), ordinary.map { it.key })
    }
    @Test fun floorOnlyDoesNotInventPullingCoverage() {
        val entries = ExerciseCatalog.eligible(setOf(Equipment.FLOOR_SPACE), setOf(RecordingMode.REPS_ONLY))
        assertFalse(entries.any { Movement.HORIZONTAL_PULL in it.movements || Movement.VERTICAL_PULL in it.movements })
        assertEquals(setOf(Muscle.LATS), ExerciseCatalog.missingPrimaryTargets(entries.map { it.key }.toSet(), setOf(Muscle.LATS, Muscle.PECS)))
    }
    @Test fun stabilizersDoNotSatisfyPrimaryCoverage() {
        assertEquals(setOf(Muscle.ABS), ExerciseCatalog.missingPrimaryTargets(setOf("back-squat-barbell"), setOf(Muscle.QUADS, Muscle.ABS)))
    }
    @Test fun modifiersAreBoundedAndPreserveComparisonIdentity() {
        val entry = ExerciseCatalog.find("bench-barbell")!!
        val ordinary = ExerciseModifiers()
        val paused = ExerciseModifiers(pauseSeconds = 2)
        assertTrue(entry.supports(paused))
        assertNotEquals(ExerciseCatalog.comparisonKey(entry.key, ordinary), ExerciseCatalog.comparisonKey(entry.key, paused))
        assertFalse(entry.supports(ExerciseModifiers(pauseSeconds = -1)))
        assertFalse(entry.supports(ExerciseModifiers(eccentricSeconds = 11)))
        assertFalse(entry.supports(ExerciseModifiers(intent = Intent.BALLISTIC)))
        assertFalse(entry.supports(ExerciseModifiers(range = RangeOfMotion.ISOMETRIC)))
        val swing = ExerciseCatalog.find("swing-kettlebell")!!
        assertTrue(swing.supports(ExerciseModifiers(intent = Intent.BALLISTIC)))
        assertFalse(swing.supports(ExerciseModifiers(intent = Intent.BALLISTIC, pauseSeconds = 2)))
        assertFalse(ExerciseCatalog.find("plank")!!.supports(ExerciseModifiers(range = RangeOfMotion.ISOMETRIC, eccentricSeconds = 3)))
    }
    @Test(expected = IllegalArgumentException::class) fun unknownCoverageKeyIsRejected() {
        ExerciseCatalog.missingPrimaryTargets(setOf("unknown"), setOf(Muscle.PECS))
    }
}
