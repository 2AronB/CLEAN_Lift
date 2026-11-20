package dev.clean.lift.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Typed preferences travel with records; external recordings and URI permissions do not. */
internal class BackupPreferences(private val context: Context, private val checkpoint: (String) -> Unit = {}) {
    companion object {
        val names = listOf("local-profile", "metrics-page", "generator", "widgets-calculators", "widget-audio")
        private fun expectedType(file: String, key: String): String = when (file) {
            "local-profile" -> when {
                key in listOf("name", "age", "gender", "palette", "theme") -> "string"
                key.matches(Regex("switch-attempt-[0-2]")) -> "int"
                else -> error("Unknown profile setting")
            }
            "metrics-page" -> when {
                key in listOf("hidden", "hidden-groups", "benchmarks") -> "strings"
                key in listOf("imperial", "references", "smoothing") -> "boolean"
                key == "order" -> "string"
                key == "rep-count" -> "int"
                key.matches(Regex("big-three-[0-2]")) -> "long"
                else -> error("Unknown metric setting")
            }
            "generator" -> when {
                key == "location" -> "string"
                key == "locations_v1" -> "boolean"
                key in listOf("equipment", "equipment_HOME", "equipment_GYM") -> "strings"
                else -> error("Unknown generator setting")
            }
            "widgets-calculators", "widget-audio" -> "string"
            else -> error("Unknown preference file")
        }
    }

    fun capture(): JSONObject = JSONObject().also { root -> names.forEach { file ->
        val fields = JSONObject()
        context.getSharedPreferences(file, Context.MODE_PRIVATE).all.toSortedMap().forEach { (key, value) ->
            val type = expectedType(file, key)
            val encoded = if (value is Set<*>) JSONArray(value.filterIsInstance<String>().sorted()) else value
            fields.put(key, JSONObject().put("type", type).put("value", encoded))
        }
        root.put(file, fields)
    }; validate(root) }

    fun validate(root: JSONObject) {
        require(root.keys().asSequence().toSet() == names.toSet()) { "Incomplete settings" }
        names.forEach { file ->
            val fields = root.getJSONObject(file)
            require(fields.length() <= 1000)
            fields.keys().forEach { key ->
                require(key.length in 1..100)
                val entry = fields.getJSONObject(key)
                require(entry.keys().asSequence().toSet() == setOf("type", "value"))
                val type = expectedType(file, key)
                require(entry.get("type") == type)
                val value = entry.get("value")
                when (type) {
                    "string" -> require(value is String)
                    "boolean" -> require(value is Boolean)
                    "int" -> require((value is Int || value is Long) && (value as Number).toLong() in Int.MIN_VALUE..Int.MAX_VALUE)
                    "long" -> require(value is Int || value is Long)
                    "strings" -> { require(value is JSONArray && value.length() <= 1000)
                        for (i in 0 until value.length()) require(value.get(i) is String)
                    }
                }
                if (file == "local-profile") when (key) {
                    "name" -> require((value as String).length <= 80)
                    "age" -> require((value as String).isBlank() || value.trim().toIntOrNull() in 1..120)
                    "gender" -> require(value in listOf("", "M", "F"))
                    // Appearance (2026-11-19). Absent in older backups; defaults then apply.
                    "palette" -> require(dev.clean.lift.ui.AppearanceStore.validPalette(value as String))
                    "theme" -> require(dev.clean.lift.ui.AppearanceStore.validTheme(value as String))
                    else -> require((value as Number).toInt() in 0..7)
                }
                if (file == "metrics-page" && key == "rep-count") require((value as Number).toInt() in 1..30)
            }
        }
        val metrics = root.getJSONObject("metrics-page")
        val mappings = (0..2).mapNotNull { index -> metrics.optJSONObject("big-three-$index")?.getLong("value") }
        require(mappings.all { it >= 0 } && mappings.filter { it > 0 }.distinct().size == mappings.count { it > 0 })
    }

    fun apply(root: JSONObject) {
        validate(root)
        names.forEach { file ->
            val editor = context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear()
            // Document grants/files cannot be transferred by a backup; request a new recording.
            if (file != "widget-audio") {
                val fields = root.getJSONObject(file)
                fields.keys().forEach { key ->
                    val entry = fields.getJSONObject(key)
                    when (entry.getString("type")) {
                        "string" -> editor.putString(key, entry.getString("value"))
                        "boolean" -> editor.putBoolean(key, entry.getBoolean("value"))
                        "int" -> editor.putInt(key, entry.getInt("value"))
                        "long" -> editor.putLong(key, entry.getLong("value"))
                        "strings" -> { val values = entry.getJSONArray("value")
                            editor.putStringSet(key, (0 until values.length()).map { values.getString(it) }.toSet())
                        }
                    }
                }
            }
            check(editor.commit()) { "Could not apply restored settings" }
            checkpoint("preference:$file")
        }
    }
}
