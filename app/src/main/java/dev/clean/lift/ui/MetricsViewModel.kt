package dev.clean.lift.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.clean.lift.data.*
import dev.clean.lift.widgets.WidgetAttempt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class MetricsState(
    val loading: Boolean = true, val failed: Boolean = false,
    val body: List<BodyMeasurement> = emptyList(), val health: List<HealthRecord> = emptyList(),
    val widgetAttempts: List<WidgetAttempt> = emptyList(),
    val exercises: List<Exercise> = emptyList(), val strength: List<StrengthSample> = emptyList(),
    val benchmarks: Set<Long> = emptySet(), val bigThree: List<Long?> = listOf(null, null, null), val repCount: Int = 5,
    val gender: String = "", val age: Int? = null,
    val hidden: Set<String> = emptySet(), val hiddenGroups: Set<String> = emptySet(), val order: List<String> = emptyList(),
    val imperial: Boolean = true, val references: Boolean = true, val smoothing: Boolean = false,
    val preferencesSaving: Boolean = false, val preferencesFailed: Boolean = false
)

class MetricsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WorkoutDatabase.get(application)
    private val repository = HealthRepository(db)
    private val preferences = application.getSharedPreferences("metrics-page", Context.MODE_PRIVATE)
    private val profile = application.getSharedPreferences("local-profile", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(MetricsState())
    val state = mutable.asStateFlow()
    private val mutableOperation = MutableStateFlow(WorkoutOperation.Idle)
    val operation = mutableOperation.asStateFlow()
    private var job: Job? = null
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> loadProfile() }

    init {
        mutable.value = mutable.value.copy(hidden = preferences.getStringSet("hidden", emptySet()).orEmpty().toSet(),
            hiddenGroups = preferences.getStringSet("hidden-groups", emptySet()).orEmpty().toSet(),
            order = preferences.getString("order", "").orEmpty().split('|').filter { it.isNotEmpty() },
            imperial = preferences.getBoolean("imperial", true), references = preferences.getBoolean("references", true),
            smoothing = preferences.getBoolean("smoothing", false),
            benchmarks = preferences.getStringSet("benchmarks", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet(),
            bigThree = List(3) { preferences.getLong("big-three-$it", 0).takeIf { id -> id > 0 } },
            repCount = preferences.getInt("rep-count", 5).coerceIn(1, 30))
        loadProfile(); profile.registerOnSharedPreferenceChangeListener(listener); load()
    }
    private fun loadProfile() {
        mutable.value = mutable.value.copy(gender = profile.getString("gender", "").orEmpty().takeIf { it in listOf("M", "F") }.orEmpty(),
            age = profile.getString("age", "").orEmpty().toIntOrNull())
    }
    fun load() {
        job?.cancel()
        mutable.value = mutable.value.copy(loading = true, failed = false)
        job = viewModelScope.launch {
            try { combine(db.measurementDao().observeAll(), db.healthDao().observeAll(), db.dao().observeExercises(), db.progressDao().observeStrengthSets(), db.widgetAttemptDao().observeAll()) { body, health, exercises, strength, attempts ->
                mutable.value.copy(loading = false, failed = false, body = body, health = health, exercises = exercises, strength = strength, widgetAttempts = attempts)
            }.collect { loaded ->
                // Read preferences/profile from the current state, not an earlier asynchronous emission.
                mutable.value = mutable.value.copy(loading = false, failed = false, body = loaded.body, health = loaded.health,
                    exercises = loaded.exercises, strength = loaded.strength, widgetAttempts = loaded.widgetAttempts)
            } } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = mutable.value.copy(loading = false, failed = true) }
        }
    }
    fun configure(hidden: Set<String> = mutable.value.hidden, hiddenGroups: Set<String> = mutable.value.hiddenGroups, order: List<String> = mutable.value.order,
                  imperial: Boolean = mutable.value.imperial, references: Boolean = mutable.value.references,
                  smoothing: Boolean = mutable.value.smoothing, benchmarks: Set<Long> = mutable.value.benchmarks,
                  bigThree: List<Long?> = mutable.value.bigThree, repCount: Int = mutable.value.repCount) {
        if (mutable.value.preferencesSaving) return
        if (bigThree.size != 3 || bigThree.filterNotNull().distinct().size != bigThree.filterNotNull().size || repCount !in 1..30) {
            mutable.value = mutable.value.copy(preferencesFailed = true); return
        }
        mutable.value = mutable.value.copy(preferencesSaving = true, preferencesFailed = false)
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                val editor = preferences.edit().putStringSet("hidden", hidden.toSet()).putString("order", order.distinct().joinToString("|"))
                    .putStringSet("hidden-groups", hiddenGroups.toSet())
                    .putBoolean("imperial", imperial).putBoolean("references", references).putBoolean("smoothing", smoothing)
                    .putStringSet("benchmarks", benchmarks.map { it.toString() }.toSet()).putInt("rep-count", repCount)
                bigThree.forEachIndexed { index, id -> editor.putLong("big-three-$index", id ?: 0) }
                editor.commit()
            }
            mutable.value = if (saved) mutable.value.copy(hidden = hidden.toSet(), hiddenGroups = hiddenGroups.toSet(), order = order.distinct(), imperial = imperial,
                references = references, smoothing = smoothing, benchmarks = benchmarks.toSet(), bigThree = bigThree.toList(), repCount = repCount, preferencesSaving = false)
            else mutable.value.copy(preferencesSaving = false, preferencesFailed = true)
        }
    }
    fun reset() { if (mutableOperation.value != WorkoutOperation.Saving) mutableOperation.value = WorkoutOperation.Idle }
    private fun change(action: suspend () -> Unit) {
        if (mutableOperation.value in listOf(WorkoutOperation.Saving, WorkoutOperation.Saved)) return
        mutableOperation.value = WorkoutOperation.Saving
        viewModelScope.launch {
            try { action(); mutableOperation.value = WorkoutOperation.Saved }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableOperation.value = WorkoutOperation.Failed }
        }
    }
    fun save(record: HealthRecord) = change { repository.save(record) }
    fun delete(id: Long) = change { repository.delete(id) }
    override fun onCleared() { profile.unregisterOnSharedPreferenceChangeListener(listener); super.onCleared() }
}
