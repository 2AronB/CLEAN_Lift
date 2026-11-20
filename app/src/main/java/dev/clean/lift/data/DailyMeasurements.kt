package dev.clean.lift.data

data class DailyMeasurement(val metric: BodyMetric, val value: String, val unit: MeasurementUnit)

object DailyMeasurementInput {
    fun records(day: Long, entries: List<DailyMeasurement>): List<BodyMeasurement> {
        require(day >= 0 && entries.size <= BodyMetric.entries.size)
        require(entries.map { it.metric }.distinct().size == entries.size)
        val entered = entries.filter { it.value.isNotBlank() }
        require(entered.isNotEmpty())
        return entered.map {
            require(it.unit in MeasurementInput.units(it.metric))
            BodyMeasurement(metric = it.metric, epochDay = day,
                valueMilliUnits = requireNotNull(MeasurementInput.parseValue(it.value)), unit = it.unit)
        }
    }
}
