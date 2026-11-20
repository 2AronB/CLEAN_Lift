package dev.clean.lift.ui

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.clean.lift.data.Exercise
import dev.clean.lift.data.ExerciseNames
import dev.clean.lift.data.WorkoutDatabase
import dev.clean.lift.data.CommonExercises
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseListState(val loading: Boolean = true, val exercises: List<Exercise> = emptyList(), val failed: Boolean = false)
enum class SaveResult { Idle, Saving, Saved, Invalid, Duplicate, Failed }

class ExercisesViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WorkoutDatabase.get(application)
    private val dao = db.dao()
    private val mutableList = MutableStateFlow(ExerciseListState())
    val list = mutableList.asStateFlow()
    private val mutableSave = MutableStateFlow(SaveResult.Idle)
    val save = mutableSave.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            mutableList.value = mutableList.value.copy(loading = true, failed = false)
            try {
                CommonExercises.installOnce(db)
                dao.observeExercises().collect { mutableList.value = ExerciseListState(loading = false, exercises = it) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableList.value = mutableList.value.copy(loading = false, failed = true) }
        }
    }

    fun resetSave() { mutableSave.value = SaveResult.Idle }

    fun save(id: Long?, input: String) {
        if (mutableSave.value == SaveResult.Saving) return
        if (!ExerciseNames.valid(input)) { mutableSave.value = SaveResult.Invalid; return }
        val name = ExerciseNames.clean(input)
        val key = ExerciseNames.key(name)
        mutableSave.value = SaveResult.Saving
        viewModelScope.launch {
            try {
                if (id == null) dao.insertExercise(Exercise(name = name, normalizedName = key))
                else check(dao.renameExercise(id, name, key) == 1)
                mutableSave.value = SaveResult.Saved
            } catch (e: CancellationException) { throw e }
            catch (_: SQLiteConstraintException) { mutableSave.value = SaveResult.Duplicate }
            catch (_: Exception) { mutableSave.value = SaveResult.Failed }
        }
    }
}
