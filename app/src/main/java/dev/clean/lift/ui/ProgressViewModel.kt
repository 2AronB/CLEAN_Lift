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

data class ProgressState(
    val loading: Boolean = true, val failed: Boolean = false,
    val exercises: List<Exercise> = emptyList(), val id: Long? = null,
    val points: List<ExerciseProgressPoint> = emptyList()
)

class ProgressViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WorkoutDatabase.get(application)
    private val mutableState = MutableStateFlow(ProgressState())
    val state = mutableState.asStateFlow()
    private var catalogJob: Job? = null
    private var pointsJob: Job? = null
    init { load() }
    fun load() {
        catalogJob?.cancel()
        catalogJob = viewModelScope.launch {
            try {
                db.dao().observeExercises().collect { mutableState.value = mutableState.value.copy(exercises = it, loading = false, failed = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.value = mutableState.value.copy(loading = false, failed = true) }
        }
    }
    fun select(id: Long?) {
        pointsJob?.cancel()
        mutableState.value = mutableState.value.copy(id = id, points = emptyList(), loading = id != null, failed = false)
        if (id == null) return
        pointsJob = viewModelScope.launch {
            try {
                db.progressDao().observeExercise(id).collect { mutableState.value = mutableState.value.copy(points = it, loading = false, failed = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.value = mutableState.value.copy(loading = false, failed = true) }
        }
    }
}
