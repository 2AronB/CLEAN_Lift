package dev.clean.lift.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.data.MeasurementInput
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupScreen(onBack: () -> Unit, onRestored: () -> Unit, model: BackupViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    var confirmation by rememberSaveable { mutableStateOf("") }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json"), model::writeExport)
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), model::load)
    fun back() { if (!state.busy && !state.recoveryPending) { model.dismiss(); onBack() } }
    BackHandler { back() }
    LaunchedEffect(state.requestExportFile) {
        if (state.requestExportFile) {
            model.exportPickerOpened()
            try { save.launch("CLEAN-Lift-${MeasurementInput.today()}.json") } catch (_: Exception) { model.exportPickerFailed() }
        }
    }
    LaunchedEffect(state.restart) { if (state.restart) { model.restartConsumed(); onRestored() } }
    Scaffold(modifier = Modifier.imePadding(), topBar = { TopAppBar(title = { Text("Backup & restore") },
        navigationIcon = { BackButton(enabled = !state.busy && !state.recoveryPending, onClick = { back() }) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Save a readable backup of your records, profile and saved settings. In the file picker choose On this device or Downloads to keep it local. Files contain private health data and are not encrypted.") }
            item { Button(enabled = !state.busy && !state.recoveryPending, onClick = model::prepareExport) { Text("Save backup") } }
            item { OutlinedButton(enabled = !state.busy && !state.recoveryPending, onClick = {
                confirmation = ""
                try { open.launch(arrayOf("application/json", "application/octet-stream")) } catch (_: Exception) { model.pickerFailed() }
            }) { Text("Choose backup to restore") } }
            item { Text("Backups include saved and active workouts, exercises, templates, body measurements, health results and widget attempts. Unsaved drafts, running timers and audio recordings are not included. Reselect recordings after restoring. Maximum file size: 8 MiB.", style = MaterialTheme.typography.bodySmall) }
            if (state.busy) item { CircularProgressIndicator(); Text("Preparing, validating or saving…") }
            state.message?.let { item { Text(it) } }
            if (state.recoveryPending) item { Button(enabled = !state.busy, onClick = model::finishRestore) { Text("Finish restore") } }
            state.preview?.let { preview ->
                item { HorizontalDivider(); Text("Restore preview", style = MaterialTheme.typography.titleLarge)
                    Text("Backup from ${DateFormat.getDateTimeInstance().format(Date(preview.createdAt))}")
                    Text("Profile: ${preview.profileName.ifEmpty { "Not set" }}")
                }
                listOf("workouts" to "Workouts (includes unfinished)", "exercises" to "Exercises", "workout_sets" to "Logged sets",
                    "workout_templates" to "Plans", "body_measurements" to "Body measurements",
                    "health_records" to "Health results", "widget_attempts" to "Widget attempts").forEach { (table, title) ->
                    item { Text("$title: ${preview.counts[table] ?: 0} in backup • ${state.current[table] ?: 0} currently") }
                }
                item { Text("Restoring replaces all current records and saved settings, including unfinished workouts. Entries added since this backup will be lost. Save a current backup first if you want to keep them. After restoring, the app restarts at Workout.") }
                item { OutlinedTextField(confirmation, { confirmation = it }, enabled = !state.busy && !state.recoveryPending,
                    label = { Text("Type RESTORE to confirm replacement") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { Button(enabled = confirmation == "RESTORE" && !state.busy && !state.recoveryPending, onClick = model::restore) { Text("Replace data and restore") } }
            }
        }
    }
}
