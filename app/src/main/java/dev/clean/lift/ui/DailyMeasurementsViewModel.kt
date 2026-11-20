package dev.clean.lift.ui

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.clean.lift.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DailySaveState(val saving: Boolean = false, val saved: Boolean = false, val error: String? = null)

class DailyMeasurementsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MeasurementRepository(WorkoutDatabase.get(application))
    private val mutable = MutableStateFlow(DailySaveState())
    val state = mutable.asStateFlow()
    fun reset() { if (!mutable.value.saving) mutable.value = DailySaveState() }
    fun save(date: String, entries: List<DailyMeasurement>) {
        if (mutable.value.saving || mutable.value.saved) return
        mutable.value = DailySaveState(saving = true)
        viewModelScope.launch {
            try { repository.saveDay(date, entries); mutable.value = DailySaveState(saved = true) }
            catch (e: CancellationException) { throw e }
            catch (_: SQLiteConstraintException) { mutable.value = DailySaveState(error = "A measurement already exists for this date. Nothing was saved. Clear that field here, or edit the saved entry from its chart.") }
            catch (_: Exception) { mutable.value = DailySaveState(error = "Could not save measurements. Nothing was saved; your draft is still here.") }
        }
    }
}
