package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test

class WorkoutGeneratorTest {
    private val gym = Equipment.entries.toSet()
    private fun request(style: GeneratorStyle = GeneratorStyle.FULL_BODY, session: String = style.sessions.first(), gear: Set<Equipment> = gym,
        effort: Int = 3, swings: Boolean = false) = GeneratorRequest(style, session, gear, 5, effort, swings, true)

    @Test fun staplesWinAndFullBodyIsDeterministicWithoutDuplicates() {
        val input = request()
        val draft = WorkoutGenerator.generate(input)
        assertEquals(listOf("back-squat-barbell", "deadlift-conventional", "bench-barbell", "pull-up-overhand", "dead-bug"), draft.rows.map { it.key })
        assertTrue(draft.gaps.isEmpty())
        assertEquals(draft, WorkoutGenerator.generate(input))
        assertEquals(draft.rows.size, draft.rows.map { it.key }.distinct().size)
    }
    @Test fun floorOnlyHasHonestPullGapAndNoInventedEquipment() {
        val input = request(GeneratorStyle.BODYWEIGHT, gear = setOf(Equipment.FLOOR_SPACE))
        val draft = WorkoutGenerator.generate(input)
        assertEquals(1, draft.gaps.size)
        assertTrue(draft.gaps.single().startsWith("Pull:"))
        assertTrue(draft.rows.any { it.key == "prone-w" })
        assertTrue(draft.gaps.single().contains("not a loaded row"))
        assertTrue(Muscle.LATS in ExerciseCatalog.missingPrimaryTargets(draft.rows.map { it.key }.toSet(), setOf(Muscle.LATS)))
        assertTrue(draft.rows.all { ExerciseCatalog.find(it.key)!!.availableWith(input.equipment) })
        assertTrue(draft.rows.all { ExerciseCatalog.find(it.key)!!.recording == RecordingMode.REPS_ONLY })
    }
    @Test fun emptyEquipmentProducesEmptyDraftWithGaps() {
        val draft = WorkoutGenerator.generate(request(gear = emptySet()))
        assertTrue(draft.rows.isEmpty())
        assertEquals(4, draft.gaps.size)
    }
    @Test fun dumbbellOnlyUsesNoBenchAlternatives() {
        val input = request(gear = setOf(Equipment.FLOOR_SPACE, Equipment.DUMBBELL_PAIR, Equipment.DUMBBELL_SINGLE))
        val draft = WorkoutGenerator.generate(input)
        assertTrue(draft.gaps.isEmpty())
        assertTrue(draft.rows.any { it.key == "rdl-dumbbell" })
        assertTrue(draft.rows.any { it.key == "row-dumbbell" })
        assertFalse(draft.rows.any { it.key == "bench-barbell" })
    }
    @Test fun kettlebellWorksWithOneBellAndSwingsRequireExplicitOptIn() {
        val input = request(GeneratorStyle.KETTLEBELL, gear = setOf(Equipment.KETTLEBELL, Equipment.FLOOR_SPACE))
        val ordinary = WorkoutGenerator.generate(input)
        assertTrue(ordinary.gaps.isEmpty())
        assertEquals("deadlift-kettlebell", ordinary.rows.first().key)
        val power = WorkoutGenerator.generate(input.copy(allowSwings = true))
        assertEquals("swing-kettlebell", power.rows.first().key)
        assertFalse(power.rows.any { it.key == "deadlift-kettlebell" })
        assertFalse(WorkoutGenerator.candidates(input).any { Intent.BALLISTIC in it.allowedIntents })
        assertTrue(power.rows.all { ExerciseCatalog.find(it.key)!!.recording in setOf(RecordingMode.REPS_ONLY, RecordingMode.LOAD_REPS) })
    }
    @Test fun fiveByFiveOnlyAppliesToDesignatedMainLifts() {
        val draft = WorkoutGenerator.generate(request(GeneratorStyle.FIVE_BY_FIVE, effort = 1))
        assertEquals(3, draft.rows.count { it.sets == 5 && it.repMin == 5 && it.repMax == 5 })
        assertEquals(1, draft.rows.first { it.key == "rdl-barbell" }.sets)
    }
    @Test fun nextSessionUsesCompletedMatchingStyleAndCompletionOrder() {
        val history = listOf(
            Workout(1, 500, 3000, generatorSession = "ppl:pull"),
            Workout(2, 100, 3000, generatorSession = "ppl:push"),
            Workout(3, 4000, null, generatorSession = "ppl:legs"),
            Workout(4, 6000, 7000, generatorSession = "full:a"),
            Workout(5, 8000, 9000))
        assertEquals("ppl:legs", WorkoutGenerator.nextSession(GeneratorStyle.PPL, history))
        assertEquals("full:b", WorkoutGenerator.nextSession(GeneratorStyle.FULL_BODY, history))
        assertEquals("ppl:pull", WorkoutGenerator.nextSession(GeneratorStyle.PPL, emptyList()))
    }
    @Test fun effortChangesVolumeWithoutTurningIntoAnExerciseOrWeightMultiplier() {
        val light = WorkoutGenerator.generate(request(effort = 1))
        val hard = WorkoutGenerator.generate(request(effort = 5))
        assertEquals(light.rows.map { it.key }, hard.rows.map { it.key })
        assertTrue(light.rows.sumOf { it.sets } < hard.rows.sumOf { it.sets })
        assertEquals(light.rows.map { it.repMin }, hard.rows.map { it.repMin })
    }
    @Test fun technicalAndUnsupportedExercisesStayGated() {
        val input = request().copy(allowTechnical = false)
        assertFalse(WorkoutGenerator.candidates(input).any { it.skill != Skill.BASIC })
        for (style in GeneratorStyle.entries) {
            val draft = WorkoutGenerator.generate(request(style))
            assertTrue(draft.rows.all { ExerciseCatalog.find(it.key)!!.skill != Skill.ADVANCED })
            assertTrue(draft.rows.all { ExerciseCatalog.find(it.key)!!.recording != RecordingMode.TIME })
        }
    }
    @Test fun pplKeepsHingeOnLegsAndPullHasComplementaryPatterns() {
        val pull = WorkoutGenerator.generate(request(GeneratorStyle.PPL))
        assertEquals(listOf("pull-up-overhand", "row-supported-db"), pull.rows.take(2).map { it.key })
        assertFalse(pull.rows.any { Movement.HINGE in ExerciseCatalog.find(it.key)!!.movements })
        val legs = WorkoutGenerator.generate(request(GeneratorStyle.PPL, "ppl:legs"))
        assertTrue(legs.rows.any { it.key == "deadlift-conventional" })
    }
    @Test(expected = IllegalArgumentException::class) fun incompatibleSessionRejected() {
        WorkoutGenerator.generate(request(session = "ppl:pull"))
    }
    @Test fun tenExercisesArePossibleButNeverPaddedWithDuplicateKeys() {
        for (input in listOf(request().copy(exerciseLimit = 10), request(GeneratorStyle.BODYWEIGHT, gear = setOf(Equipment.FLOOR_SPACE)).copy(exerciseLimit = 10))) {
            val draft = WorkoutGenerator.generate(input)
            assertEquals(10, draft.rows.size)
            assertTrue(WorkoutGenerator.uniqueRows(draft.rows))
        }
        assertEquals(4, WorkoutGenerator.generate(request().copy(exerciseLimit = 4)).rows.size)
    }
    @Test fun presetOrderCanBeKeptAndIsolationFollowsCompoundWork() {
        val draft = WorkoutGenerator.generate(request().copy(exerciseLimit = 10))
        val isolation = draft.rows.indexOfFirst { ExerciseCatalog.find(it.key)!!.coverage == Coverage.ISOLATION }
        assertTrue(draft.rows.drop(isolation).all { ExerciseCatalog.find(it.key)!!.coverage == Coverage.ISOLATION })
        val preset = WorkoutGenerator.generate(request().copy(compoundFirst = false))
        assertEquals(listOf("back-squat-barbell", "bench-barbell", "pull-up-overhand", "deadlift-conventional", "dead-bug"), preset.rows.map { it.key })
    }
    @Test fun abCircuitAddsTwoRoundsAfterMainWorkInEveryStyle() {
        for (style in GeneratorStyle.entries) {
            val draft = WorkoutGenerator.generate(request(style).copy(exerciseLimit = 10, abCircuit = true))
            val rounds = draft.rows.takeLast(6)
            assertEquals(listOf(1, 1, 1, 2, 2, 2), rounds.map { it.circuitRound })
            assertEquals(listOf("dead-bug", "reverse-crunch", "bicycle-crunch"), rounds.take(3).map { it.key })
            assertEquals(rounds.take(3).map { it.key }, rounds.drop(3).map { it.key })
            assertEquals(listOf(15, 15, 60, 15, 15, 60), rounds.map { it.restSeconds })
            assertTrue(rounds.all { it.sets == 1 })
            assertTrue(draft.rows.dropLast(6).all { it.circuitRound == null && it.key != "dead-bug" })
            assertTrue(WorkoutGenerator.uniqueRows(draft.rows))
        }
    }
    @Test fun bodyweightFallbackWorksForAllStylesWithoutInventingPullEquipment() {
        for (style in GeneratorStyle.entries) for (session in style.sessions) {
            val input = GeneratorRequest(style, session, setOf(Equipment.FLOOR_SPACE))
            val draft = WorkoutGenerator.generate(input)
            assertTrue("$style $session", draft.rows.isNotEmpty())
            assertFalse("$style $session", draft.gaps.any { it.contains("no compatible exercise") })
            assertTrue(draft.rows.all { ExerciseCatalog.find(it.key)!!.availableWith(input.equipment) })
            assertTrue(draft.rows.all { ExerciseCatalog.find(it.key)!!.recording == RecordingMode.REPS_ONLY && it.sets != 5 })
        }
    }
    @Test fun lowFixedBarProvidesRealBodyweightPullWithoutTechnicalOptIn() {
        val input = request(GeneratorStyle.BODYWEIGHT, gear = setOf(Equipment.FLOOR_SPACE, Equipment.LOW_FIXED_BAR)).copy(allowTechnical = false)
        val draft = WorkoutGenerator.generate(input)
        assertTrue(draft.rows.any { it.key == "row-inverted" })
        assertTrue(draft.gaps.isEmpty())
    }
    @Test fun finisherRequiresActualFloorSpaceAndDuplicateWithinRoundIsInvalid() {
        val draft = WorkoutGenerator.generate(request(gear = emptySet()).copy(abCircuit = true))
        assertTrue(draft.rows.isEmpty())
        assertTrue(draft.gaps.any { it.startsWith("Ab circuit:") })
        val row = GeneratedExercise("dead-bug", 1, 8, 12, 15, "", 1)
        assertFalse(WorkoutGenerator.uniqueRows(listOf(row, row)))
        assertTrue(WorkoutGenerator.uniqueRows(listOf(row, row.copy(circuitRound = 2))))
    }
    @Test(expected = IllegalArgumentException::class) fun reversedRepsRejected() {
        WorkoutGenerator.validateRow(GeneratedExercise("bench-barbell", 3, 10, 5, 120, ""))
    }
}
