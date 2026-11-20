package dev.clean.lift.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import dev.clean.lift.data.MeasurementInput
import dev.clean.lift.data.StrengthEstimates

/** Material dates use UTC midnight; the stored value is a calendar day, never local midnight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeasurementDateField(value: String, onChange: (String) -> Unit, label: String = "Date",
                                  enabled: Boolean = true) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val todayText = MeasurementInput.today()
    val today = MeasurementInput.parseDay(todayText)!!
    OutlinedTextField(value, {}, readOnly = true, enabled = enabled, singleLine = true,
        label = { Text(label) }, isError = value.isNotEmpty() && MeasurementInput.parseDay(value) == null,
        trailingIcon = { TextButton(enabled = enabled, onClick = { choosing = true }) { Text("Choose") } },
        modifier = Modifier.fillMaxWidth())
    if (choosing) {
        val dates = remember(today) { object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in 0..today * StrengthEstimates.DAY_MILLIS
            override fun isSelectableYear(year: Int) = year in 1970..todayText.take(4).toInt()
        } }
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = (MeasurementInput.parseDay(value) ?: today) * StrengthEstimates.DAY_MILLIS,
            yearRange = 1970..todayText.take(4).toInt(), selectableDates = dates)
        DatePickerDialog(onDismissRequest = { choosing = false },
            confirmButton = { TextButton(enabled = picker.selectedDateMillis?.let { dates.isSelectableDate(it) } == true,
                onClick = {
                    picker.selectedDateMillis?.let { onChange(MeasurementInput.dayText(it / StrengthEstimates.DAY_MILLIS)) }
                    choosing = false
                }) { Text("Use date") } },
            dismissButton = { TextButton(onClick = { choosing = false }) { Text("Cancel") } }) {
            DatePicker(state = picker)
        }
    }
}
