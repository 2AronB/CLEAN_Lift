package dev.clean.lift.widgets

enum class WidgetLocation(val label: String) { HOME("Home"), GYM("Gym") }
data class WidgetEquipment(val bar: String, val rows: List<String>)

internal fun profileKey(location: WidgetLocation, unit: LoadUnit) = "${location.name}-${unit.name}"

/** Old equipment had no location. Preserve it under Home without inventing Gym equipment.
 * Legacy keys are retained; explicit empty inventories are valid and must not resurrect defaults. */
internal fun readWidgetEquipment(values: Map<String, String>, location: WidgetLocation, unit: LoadUnit): WidgetEquipment {
    val key = profileKey(location, unit)
    val rowsText = values["plates-$key"] ?: if (location == WidgetLocation.HOME) values["plates-${unit.name}"] else null
    val barText = values["bar-$key"] ?: if (location == WidgetLocation.HOME) values["bar-${unit.name}"] else null
    val rows = rowsText?.let { if (it.isEmpty()) emptyList() else it.split('|') }
        ?.takeIf { parseStock(it) != null } ?: defaultStock(unit)
    return WidgetEquipment(barText?.takeIf { WidgetCalculations.load(it, true) != null } ?: defaultBar(unit), rows)
}

internal fun defaultBar(unit: LoadUnit) = if (unit == LoadUnit.LB) "45" else "20"
internal fun defaultStock(unit: LoadUnit): List<String> =
    (if (unit == LoadUnit.LB) listOf("45", "35", "25", "10", "5", "2.5") else listOf("25", "20", "15", "10", "5", "2.5", "1.25"))
        .map { "$it:0" }

internal fun parseStock(rows: List<String>): List<PlateStock>? {
    if (rows.size > 16) return null
    return rows.map { row ->
        val pieces = row.split(':')
        if (pieces.size != 2) return null
        val size = WidgetCalculations.load(pieces[0])?.takeIf { it <= java.math.BigDecimal(1000) } ?: return null
        val count = pieces[1].trim().takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }?.toIntOrNull()?.takeIf { it in 0..100 } ?: return null
        PlateStock(size, count)
    }
}
