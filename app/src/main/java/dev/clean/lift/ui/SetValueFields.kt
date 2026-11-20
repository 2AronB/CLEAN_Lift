package dev.clean.lift.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.clean.lift.R
import dev.clean.lift.data.*

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun SetValueFields(
    weight: String, reps: String, unitName: String, enabled: Boolean,
    onWeight: (String) -> Unit, onReps: (String) -> Unit, onUnit: (String) -> Unit, onSave: () -> Unit
) {
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            WeightUnit.entries.forEach { unit ->
                FilterChip(selected = unitName == unit.name, enabled = enabled,
                    onClick = { onUnit(unit.name) },
                    label = { Text(stringResource(if (unit == WeightUnit.LB) R.string.pounds else R.string.kilograms)) })
            }
        }
        OutlinedTextField(
            value = weight, onValueChange = onWeight, singleLine = true, enabled = enabled,
            label = { Text(stringResource(if (unitName == WeightUnit.LB.name) R.string.weight_lb else R.string.weight_kg)) },
            supportingText = { Text(stringResource(R.string.weight_guidance)) },
            isError = weight.isNotBlank() && SetInput.parseWeight(weight) == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }), modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = reps, onValueChange = onReps, singleLine = true, enabled = enabled,
            label = { Text(stringResource(R.string.reps)) }, supportingText = { Text(stringResource(R.string.reps_guidance)) },
            isError = reps.isNotBlank() && SetInput.parseReps(reps) == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (enabled && SetInput.parse(weight, reps) != null) onSave() }), modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun setDescription(set: WorkoutSet): String = stringResource(
    R.string.set_description, SetInput.weightText(set.weightMilliUnits),
    stringResource(if (set.weightUnit == WeightUnit.LB) R.string.lb else R.string.kg), set.reps
)
