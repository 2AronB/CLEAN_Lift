package dev.clean.lift.widgets

import java.math.BigDecimal

internal enum class PlateMode(val label: String) { PERCENT("%Max"), TARGET("Target weight"), WARMUP("Warm-up loads") }
internal data class PlateTarget(val caption: String?, val weight: BigDecimal)

internal fun plateTargets(mode: PlateMode, value: String, percentage: String, steps: String): List<PlateTarget> {
    val base = WidgetCalculations.load(value) ?: error("Enter a positive weight up to 2,000, with up to 3 decimal places.")
    return when (mode) {
        PlateMode.TARGET -> listOf(PlateTarget(null, base))
        PlateMode.PERCENT -> {
            val p = WidgetCalculations.load(percentage)?.takeIf { it <= BigDecimal(200) }
                ?: error("Enter a percentage greater than 0 and no more than 200.")
            listOf(PlateTarget("${plateDecimal(p)}% of 1RM", WidgetCalculations.percentage(base, p)))
        }
        PlateMode.WARMUP -> {
            val raw = steps.split(',')
            require(raw.size in 1..8) { "Enter 1–8 comma-separated percentages." }
            raw.map { text ->
                val p = WidgetCalculations.load(text)?.takeIf { it <= BigDecimal(100) }
                    ?: error("Warm-up percentages must be greater than 0 and no more than 100.")
                PlateTarget("${plateDecimal(p)}%", WidgetCalculations.percentage(base, p))
            }
        }
    }
}
internal fun plateDecimal(value: BigDecimal) = value.stripTrailingZeros().toPlainString()
