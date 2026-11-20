package dev.clean.lift.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.clean.lift.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val workouts: List<WorkoutSummary> = emptyList(),
    val hasMore: Boolean = false
)

data class HistoryDetailState(
    val id: Long? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val workout: WorkoutWithExercises? = null
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = WorkoutDatabase.get(application).dao()
    private val repository = WorkoutRepository(WorkoutDatabase.get(application))
    private val mutableState = MutableStateFlow(HistoryState())
    val state = mutableState.asStateFlow()
    private val mutableDetail = MutableStateFlow(HistoryDetailState())
    val detail = mutableDetail.asStateFlow()
    private val mutableOperation = MutableStateFlow(WorkoutOperation.Idle)
    val operation = mutableOperation.asStateFlow()
    private var listJob: Job? = null
    private var detailJob: Job? = null
    private var limit = 50
    var query: String = ""
        private set

    init { load() }

    fun load() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            // Keep search input and keyboard mounted during query changes.
            mutableState.value = mutableState.value.copy(failed = false)
            try {
                // Summary rows avoid loading every historical set just to draw the list.
                dao.observeCompleted(limit + 1, query).collect {
                    mutableState.value = HistoryState(loading = false, workouts = it.take(limit), hasMore = it.size > limit)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.value = mutableState.value.copy(loading = false, failed = true) }
        }
    }

    fun more() { limit += 50; load() }
    fun search(value: String) { if (value != query) { query = value; limit = 50; load() } }

    fun select(id: Long?) {
        detailJob?.cancel()
        mutableDetail.value = HistoryDetailState(id = id, loading = id != null)
        if (id == null) return
        detailJob = viewModelScope.launch {
            try {
                dao.observeWorkout(id).collect {
                    mutableDetail.value = HistoryDetailState(id = id, workout = it?.takeIf { record -> record.workout.finishedAt != null }?.ordered())
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableDetail.value = HistoryDetailState(id = id, failed = true) }
        }
    }

    fun resetOperation() { if (mutableOperation.value != WorkoutOperation.Saving) mutableOperation.value = WorkoutOperation.Idle }

    fun correct(workoutId: Long, setId: Long, values: SetValues, unit: WeightUnit) {
        if (mutableOperation.value == WorkoutOperation.Saving || mutableOperation.value == WorkoutOperation.Saved) return
        mutableOperation.value = WorkoutOperation.Saving
        viewModelScope.launch {
            try {
                repository.correctFinishedSet(workoutId, setId, values, unit)
                mutableOperation.value = WorkoutOperation.Saved
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableOperation.value = WorkoutOperation.Failed }
        }
    }
}
