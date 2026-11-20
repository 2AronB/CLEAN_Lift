package dev.clean.lift.ui

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.clean.lift.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MeasurementsState(val loading: Boolean = true, val failed: Boolean = false, val values: List<BodyMeasurement> = emptyList())

class MeasurementsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WorkoutDatabase.get(application)
    private val repository = MeasurementRepository(db)
    private val mutableState = MutableStateFlow(MeasurementsState())
    val state = mutableState.asStateFlow()
    private val mutableOperation = MutableStateFlow(WorkoutOperation.Idle)
    val operation = mutableOperation.asStateFlow()
    private val mutableDuplicate = MutableStateFlow(false)
    val duplicate = mutableDuplicate.asStateFlow()
    private var job: Job? = null
    fun select(metric: BodyMetric) {
        job?.cancel()
        mutableState.value = MeasurementsState()
        job = viewModelScope.launch {
            try { db.measurementDao().observe(metric).collect { mutableState.value = MeasurementsState(loading = false, values = it) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.value = MeasurementsState(loading = false, failed = true) }
        }
    }
    fun reset() { if (mutableOperation.value != WorkoutOperation.Saving) { mutableOperation.value = WorkoutOperation.Idle; mutableDuplicate.value = false } }
    private fun change(action: suspend () -> Unit) {
        if (mutableOperation.value == WorkoutOperation.Saving || mutableOperation.value == WorkoutOperation.Saved) return
        mutableOperation.value = WorkoutOperation.Saving; mutableDuplicate.value = false
        viewModelScope.launch {
            try { action(); mutableOperation.value = WorkoutOperation.Saved }
            catch (e: CancellationException) { throw e }
            catch (_: SQLiteConstraintException) { mutableDuplicate.value = true; mutableOperation.value = WorkoutOperation.Failed }
            catch (_: Exception) { mutableOperation.value = WorkoutOperation.Failed }
        }
    }
    fun save(id: Long?, metric: BodyMetric, date: String, value: String, unit: MeasurementUnit) = change { repository.save(id, metric, date, value, unit) }
    fun delete(id: Long) = change { repository.delete(id) }
}
