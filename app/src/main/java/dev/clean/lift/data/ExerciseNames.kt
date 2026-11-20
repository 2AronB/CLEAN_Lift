package dev.clean.lift.data

import java.util.Locale

object ExerciseNames {
    const val MAX_LENGTH = 100
    fun clean(value: String): String = value.trim().replace(Regex("\\s+"), " ")
    fun key(value: String): String = clean(value).lowercase(Locale.ROOT)
    fun valid(value: String): Boolean = clean(value).length in 1..MAX_LENGTH
}
