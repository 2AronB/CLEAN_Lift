package dev.clean.lift.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.clean.lift.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class WorkoutScreenState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val active: WorkoutWithExercises? = null,
    val latest: WorkoutWithExercises? = null,
    val catalog: List<Exercise> = emptyList()
)

enum class WorkoutOperation { Idle, Saving, Saved, Failed }
enum class WorkoutChange { Other, AddExercise, SaveSet, RemoveSet, GuidedNext, StartGuide }
data class PreviousValuesState(
    val entryId: Long? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val sets: List<PreviousWorkoutSet> = emptyList()
) {
    val values: PreviousWorkoutValues? get() = sets.lastOrNull()?.values
}

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WorkoutDatabase.get(application)
    private val repository = WorkoutRepository(db)
    private val mutableState = MutableStateFlow(WorkoutScreenState())
    val state = mutableState.asStateFlow()
    private val mutableOperation = MutableStateFlow(WorkoutOperation.Idle)
    val operation = mutableOperation.asStateFlow()
    private val mutablePrevious = MutableStateFlow(PreviousValuesState())
    val previous = mutablePrevious.asStateFlow()
    private var previousJob: Job? = null
    private var loadJob: Job? = null
    var lastChange = WorkoutChange.Other
        private set
    var lastAddedExercise: WorkoutExercise? = null
        private set

    init { load() }

    fun loadPrevious(entryId: Long) {
        previousJob?.cancel()
        mutablePrevious.value = PreviousValuesState(entryId = entryId, loading = true)
        previousJob = viewModelScope.launch {
            try {
                val entry = requireNotNull(db.dao().workoutExercise(entryId))
                val sets = db.progressDao().previousSets(entry.exerciseId)
                mutablePrevious.value = PreviousValuesState(entryId = entryId, sets = sets)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutablePrevious.value = PreviousValuesState(entryId = entryId, failed = true) }
        }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, failed = false)
            try {
                CommonExercises.installOnce(db)
                combine(db.dao().observeActive(), db.dao().observeLatestFinished(), db.dao().observeExercises()) { active, latest, catalog ->
                    WorkoutScreenState(loading = false, active = active?.ordered(), latest = latest?.ordered(), catalog = catalog)
                }.collect { mutableState.value = it }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.value = mutableState.value.copy(loading = false, failed = true) }
        }
    }

    fun resetOperation() { if (mutableOperation.value != WorkoutOperation.Saving) mutableOperation.value = WorkoutOperation.Idle }

    private fun change(kind: WorkoutChange = WorkoutChange.Other, action: suspend () -> Unit) {
        if (mutableOperation.value == WorkoutOperation.Saving || mutableOperation.value == WorkoutOperation.Saved) return
        mutableOperation.value = WorkoutOperation.Saving
        viewModelScope.launch {
            try {
                action()
                lastChange = kind
                mutableOperation.value = WorkoutOperation.Saved
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableOperation.value = WorkoutOperation.Failed }
        }
    }

    fun start() = change { repository.start() }
    fun startTemplate(templateId: Long) = change(WorkoutChange.StartGuide) {
        val id = repository.startTemplate(templateId)
        lastAddedExercise = GuidedPlan.next(requireNotNull(db.dao().workoutDetails(id)))?.exercise
    }
    fun advance(entryId: Long, override: Boolean = false) = change(WorkoutChange.GuidedNext) {
        lastAddedExercise = repository.advance(entryId, override)
    }
    fun addExercise(workoutId: Long, exerciseId: Long, override: Boolean = false) = change(WorkoutChange.AddExercise) {
        val id = repository.addExercise(workoutId, exerciseId, override)
        lastAddedExercise = requireNotNull(db.dao().workoutExercise(id))
    }
    fun saveSet(entryId: Long, setId: Long?, values: SetValues, unit: WeightUnit, override: Boolean = false) = change(WorkoutChange.SaveSet) { repository.saveSet(entryId, setId, values, unit, override) }
    fun removeSet(id: Long) = change(WorkoutChange.RemoveSet) { repository.removeSet(id) }
    fun removeExercise(id: Long, override: Boolean = false) = change { repository.removeExercise(id, override) }
    fun finish(id: Long, override: Boolean = false) = change { repository.finish(id, override) }
    fun cancel(id: Long) = change { repository.cancel(id) }
}
