package dev.clean.lift.widgets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.clean.lift.data.HealthRecord
import dev.clean.lift.data.WorkoutDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AttemptOperation(val busy: Boolean = false, val message: String = "", val savedToken: String = "")
class WidgetAttemptModel(application: Application) : AndroidViewModel(application) {
    private val db = WorkoutDatabase.get(application)
    private val repository = WidgetAttemptRepository(db)
    val attempts = db.widgetAttemptDao().observeAll().catch { mutable.value = AttemptOperation(message = "Could not load saved attempts. Reopen this screen to retry.") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val mutable = MutableStateFlow(AttemptOperation())
    val operation = mutable.asStateFlow()
    fun clear() { if (!mutable.value.busy) mutable.value = AttemptOperation() }
    fun save(attempt: WidgetAttempt, health: HealthRecord? = null) {
        if (mutable.value.busy || mutable.value.savedToken == attempt.token) return
        mutable.value = AttemptOperation(busy = true)
        viewModelScope.launch {
            try { repository.save(attempt, health); mutable.value = AttemptOperation(message = if (health == null) "Saved. Valid completed results appear in Progress." else "Saved with the linked health result in Progress.", savedToken = attempt.token) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = AttemptOperation(message = "Save failed. Your draft is still here; try again.") }
        }
    }
    fun delete(id: Long) {
        if (mutable.value.busy) return
        mutable.value = AttemptOperation(busy = true)
        viewModelScope.launch {
            try { repository.delete(id); mutable.value = AttemptOperation(message = "Attempt and its linked health result deleted.") }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = AttemptOperation(message = "Delete failed. Try again.") }
        }
    }
}
