package dev.clean.lift.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileState(val name: String = "", val age: String = "", val gender: String = "", val loading: Boolean = true, val saving: Boolean = false, val failed: Boolean = false)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("local-profile", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(ProfileState())
    val state = mutableState.asStateFlow()
    private val mutableAttempts = MutableStateFlow(listOf(0, 0, 0))
    val attempts = mutableAttempts.asStateFlow()

    val appearance = AppearanceStore.state
    private val mutableAppearanceFailed = MutableStateFlow(false)
    val appearanceFailed = mutableAppearanceFailed.asStateFlow()

    init { load() }

    /** The theme changes only after the preference is committed; a failed write leaves it as it was. */
    fun setAppearance(value: Appearance) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { AppearanceStore.save(getApplication(), value) }
            mutableAppearanceFailed.value = !saved
        }
    }

    fun load() {
        viewModelScope.launch {
            val profile = withContext(Dispatchers.IO) {
                mutableAttempts.value = List(3) { preferences.getInt("switch-attempt-$it", 0).coerceIn(0, 7) }
                ProfileState(
                    name = preferences.getString("name", "").orEmpty(),
                    age = preferences.getString("age", "").orEmpty(),
                    gender = preferences.getString("gender", "").orEmpty().takeIf(::validGender).orEmpty(),
                    loading = false
                )
            }
            mutableState.value = profile
        }
    }

    fun recordAttempt(index: Int) {
        if (mutableState.value.loading || index !in 0..2) return
        val count = nextSwitchAttempt(mutableAttempts.value[index])
        mutableAttempts.value = mutableAttempts.value.toMutableList().also { it[index] = count }
        // Only the joke sequence is remembered; no enabled feature is stored.
        preferences.edit().putInt("switch-attempt-$index", count).apply()
    }

    fun save(name: String, age: String, gender: String) {
        if (mutableState.value.loading || mutableState.value.saving || !validProfile(name, age, gender)) return
        val previous = mutableState.value
        mutableState.value = previous.copy(saving = true, failed = false)
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                preferences.edit().putString("name", name.trim()).putString("age", age.trim()).putString("gender", gender).commit()
            }
            mutableState.value = if (saved) ProfileState(name.trim(), age.trim(), gender, loading = false)
            else previous.copy(saving = false, failed = true)
        }
    }
}

internal fun validProfile(name: String, age: String, gender: String = ""): Boolean =
    name.trim().length <= 80 && (age.isBlank() || (age.trim().all { it in '0'..'9' } && age.trim().toIntOrNull() in 1..120)) && validGender(gender)

internal fun validGender(gender: String): Boolean = gender == "" || gender == "M" || gender == "F"

// First two are silent, third has the initial response, then four jokes repeat.
internal fun nextSwitchAttempt(previous: Int): Int = if (previous >= 7) 4 else previous + 1
