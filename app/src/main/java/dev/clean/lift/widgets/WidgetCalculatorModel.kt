package dev.clean.lift.widgets

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WidgetDefaults(val loading: Boolean = true, val equipment: Map<String, WidgetEquipment> = emptyMap(),
    val location: WidgetLocation = WidgetLocation.HOME, val saving: Boolean = false, val message: String = "") {
    fun equipment(location: WidgetLocation, unit: LoadUnit) = equipment.getValue(profileKey(location, unit))
}

class WidgetCalculatorModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("widgets-calculators", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(WidgetDefaults())
    val state = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            mutable.value = withContext(Dispatchers.IO) {
                val values = preferences.all.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap()
                WidgetDefaults(loading = false, equipment = WidgetLocation.entries.flatMap { location ->
                    LoadUnit.entries.map { unit -> profileKey(location, unit) to readWidgetEquipment(values, location, unit) }
                }.toMap(), location = WidgetLocation.entries.firstOrNull { it.name == values["location"] } ?: WidgetLocation.HOME)
            }
        }
    }
    fun selectLocation(location: WidgetLocation) {
        if (mutable.value.loading || mutable.value.saving) return
        mutable.value = mutable.value.copy(location = location, message = "")
        preferences.edit().putString("location", location.name).apply()
    }
    fun save(location: WidgetLocation, unit: LoadUnit, bar: String, rows: List<String>, onSaved: () -> Unit) {
        val stock = parseStock(rows) ?: return
        val load = WidgetCalculations.load(bar, true) ?: return
        if (mutable.value.loading || mutable.value.saving) return
        mutable.value = mutable.value.copy(saving = true, message = "")
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { runCatching {
                WidgetCalculations.plates(load, load, stock)
                val key = profileKey(location, unit)
                check(preferences.edit().putString("plates-$key", rows.joinToString("|"))
                    .putString("bar-$key", bar).commit()) { "Could not save equipment. Your draft is still here." }
            } }
            val success = outcome.isSuccess
            mutable.value = mutable.value.copy(saving = false,
                equipment = if (success) mutable.value.equipment + (profileKey(location, unit) to WidgetEquipment(bar, rows)) else mutable.value.equipment,
                message = if (success) "${location.label} equipment saved (${unit.label})." else outcome.exceptionOrNull()?.message ?: "Could not save equipment. Your draft is still here.")
            if (success) onSaved()
        }
    }
    fun clearMessage() { mutable.value = mutable.value.copy(message = "") }
}
