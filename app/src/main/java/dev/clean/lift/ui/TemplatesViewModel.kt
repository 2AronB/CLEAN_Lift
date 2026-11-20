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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TemplatesState(
    val loading: Boolean = true, val failed: Boolean = false,
    val templates: List<TemplateWithExercises> = emptyList(),
    val exercises: List<Exercise> = emptyList(), val active: Boolean = false,
    val generatedHistory: List<Workout> = emptyList()
)

class TemplatesViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WorkoutDatabase.get(application)
    private val repository = TemplateRepository(db)
    private val generatorRepository = GeneratorRepository(db)
    private val locations = GeneratorLocations(application.getSharedPreferences("generator", android.content.Context.MODE_PRIVATE))
    val savedLocation: WorkoutLocation get() = locations.selected
    fun savedEquipment(location: WorkoutLocation) = locations.equipment(location)
    fun rememberLocation(location: WorkoutLocation) = locations.select(location)
    fun rememberEquipment(location: WorkoutLocation, equipment: Set<Equipment>) = locations.remember(location, equipment)
    private val mutableState = MutableStateFlow(TemplatesState())
    val state = mutableState.asStateFlow()
    private val mutableOperation = MutableStateFlow(WorkoutOperation.Idle)
    val operation = mutableOperation.asStateFlow()
    private val mutableDuplicate = MutableStateFlow(false)
    val duplicate = mutableDuplicate.asStateFlow()
    private var job: Job? = null
    init { load() }

    fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, failed = false)
            try {
                CommonExercises.installOnce(db)
                combine(db.dao().observeTemplates(), db.dao().observeExercises(), db.dao().observeActive(), db.dao().observeGeneratedHistory()) { templates, exercises, active, history ->
                    TemplatesState(loading = false, templates = templates, exercises = exercises, active = active != null, generatedHistory = history)
                }.collect { mutableState.value = it }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.value = mutableState.value.copy(loading = false, failed = true) }
        }
    }
    fun reset() { if (mutableOperation.value != WorkoutOperation.Saving) { mutableOperation.value = WorkoutOperation.Idle; mutableDuplicate.value = false } }
    private fun change(action: suspend () -> Unit) {
        if (mutableOperation.value == WorkoutOperation.Saving || mutableOperation.value == WorkoutOperation.Saved) return
        mutableOperation.value = WorkoutOperation.Saving
        mutableDuplicate.value = false
        viewModelScope.launch {
            try { action(); mutableOperation.value = WorkoutOperation.Saved }
            catch (e: CancellationException) { throw e }
            catch (_: SQLiteConstraintException) { mutableDuplicate.value = true; mutableOperation.value = WorkoutOperation.Failed }
            catch (_: Exception) { mutableOperation.value = WorkoutOperation.Failed }
        }
    }
    fun save(id: Long?, name: String, rows: List<PlannedExercise>) = change { repository.save(id, name, rows) }
    fun saveGenerated(name: String, session: String, rows: List<GeneratedExercise>) = change { generatorRepository.save(name, session, rows) }
    fun delete(id: Long) = change { repository.delete(id) }
}
