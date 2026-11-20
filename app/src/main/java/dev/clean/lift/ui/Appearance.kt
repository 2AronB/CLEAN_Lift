package dev.clean.lift.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Built-in color sets (owner choice 2026-11-19: selectable palettes). Names are stored; keep them stable. */
enum class Palette(val label: String, val description: String) {
    IRON("Iron", "Charcoal, ember and steel"),
    CHALK("Chalk", "Strong pastels: lilac, mint and peach"),
    FOREST("Forest", "Deep greens and brass"),
    WALLPAPER("Wallpaper", "Match your phone's colors");

    companion object {
        val DEFAULT = IRON
        fun fromName(name: String?): Palette = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark");

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

data class Appearance(val palette: Palette = Palette.DEFAULT, val mode: ThemeMode = ThemeMode.SYSTEM)

/**
 * Appearance lives in the existing `local-profile` preferences (keys `palette` and `theme`) so it
 * travels with JSON backups. Older backups without these keys restore to the defaults.
 */
object AppearanceStore {
    const val FILE = "local-profile"
    const val PALETTE_KEY = "palette"
    const val THEME_KEY = "theme"

    private val mutable = MutableStateFlow(Appearance())
    val state: StateFlow<Appearance> = mutable.asStateFlow()

    /** Called on every activity start so a completed restore is reflected after relaunch. */
    fun reload(context: Context) {
        val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        mutable.value = Appearance(
            Palette.fromName(preferences.getString(PALETTE_KEY, null)),
            ThemeMode.fromName(preferences.getString(THEME_KEY, null))
        )
    }

    /** Blocking write; call off the main thread. Updates the live theme only after the commit succeeds. */
    fun save(context: Context, appearance: Appearance): Boolean {
        val saved = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString(PALETTE_KEY, appearance.palette.name).putString(THEME_KEY, appearance.mode.name).commit()
        if (saved) mutable.value = appearance
        return saved
    }

    fun validPalette(value: String): Boolean = Palette.entries.any { it.name == value }
    fun validTheme(value: String): Boolean = ThemeMode.entries.any { it.name == value }
}
