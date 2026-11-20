package dev.clean.lift.data

import org.junit.Assert.*
import org.junit.Test

class GuidedPlanTest {
    private fun entry(id: Long, position: Int, target: Int?, count: Int, done: Boolean = false) = ExerciseWithSets(
        WorkoutExercise(id, 1, 1, "Lift", position, target, done),
        List(count) { WorkoutSet(it.toLong() + 1, id, it, 100000, WeightUnit.KG, 5) }
    )
    private fun workout(vararg entries: ExerciseWithSets, guided: Boolean = true) = WorkoutWithExercises(
        Workout(1, 1000, templateName = if (guided) "Plan" else null), entries.toList()
    )
    @Test fun guidanceUsesPositionsAndStopsFixedAppendAtTarget() {
        val first = entry(1, 0, 2, 1)
        val later = entry(2, 1, null, 0)
        val session = workout(later, first)
        assertEquals(1L, GuidedPlan.next(session)!!.exercise.id)
        assertTrue(GuidedPlan.canAppend(session, first))
        assertFalse(GuidedPlan.canAppend(session, later))
        assertFalse(GuidedPlan.canAdvance(first))
        val complete = entry(1, 0, 2, 2)
        assertFalse(GuidedPlan.canAppend(complete))
        assertTrue(GuidedPlan.canAdvance(complete))
    }
    @Test fun untilDoneRequiresExplicitCompletionAndEmptyFinishStaysBlocked() {
        val until = entry(1, 0, null, 10)
        assertTrue(GuidedPlan.canAppend(until))
        assertTrue(GuidedPlan.canAdvance(until))
        assertFalse(GuidedPlan.canFinish(workout(until)))
        assertTrue(GuidedPlan.canFinish(workout(until.copy(exercise = until.exercise.copy(completed = true)))))
        assertFalse(GuidedPlan.canFinish(workout(entry(1, 0, 2, 0, true))))
    }
    @Test fun skippedExercisesCanFinishWithSavedDataButEmptyManualEntriesCannot() {
        val skipped = entry(1, 0, 3, 0, true)
        val completed = entry(2, 1, 2, 2)
        assertTrue(GuidedPlan.canFinish(workout(skipped, completed)))
        assertFalse(GuidedPlan.canFinish(workout(skipped, completed, guided = false)))
    }
}
