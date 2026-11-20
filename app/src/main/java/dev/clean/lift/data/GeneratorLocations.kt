package dev.clean.lift.data

import android.content.SharedPreferences

enum class WorkoutLocation(val label: String) { GYM("Gym"), HOME("Home") }

class GeneratorLocations(private val preferences: SharedPreferences) {
    init {
        // Preserve the former single setup as Home; never guess equipment at the gym.
        if (!preferences.getBoolean("locations_v1", false)) {
            val legacy = preferences.getStringSet("equipment", setOf(Equipment.FLOOR_SPACE.name)).orEmpty().toSet()
            val editor = preferences.edit()
            if (!preferences.contains(key(WorkoutLocation.HOME))) editor.putStringSet(key(WorkoutLocation.HOME), legacy)
            editor.putBoolean("locations_v1", true).apply()
        }
    }
    private fun key(location: WorkoutLocation) = "equipment_${location.name}"
    val selected: WorkoutLocation get() = WorkoutLocation.entries.firstOrNull { it.name == preferences.getString("location", null) } ?: WorkoutLocation.HOME
    fun select(location: WorkoutLocation) { preferences.edit().putString("location", location.name).apply() }
    fun equipment(location: WorkoutLocation): Set<Equipment> = preferences.getStringSet(key(location), setOf(Equipment.FLOOR_SPACE.name)).orEmpty()
        .mapNotNull { value -> Equipment.entries.firstOrNull { it.name == value } }.toSet()
    fun remember(location: WorkoutLocation, equipment: Set<Equipment>) {
        preferences.edit().putStringSet(key(location), equipment.map { it.name }.toSet()).apply()
    }
}
