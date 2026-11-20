package dev.clean.lift.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.clean.lift.data.BackupRepository
import dev.clean.lift.data.WorkoutDatabase
import kotlinx.coroutines.CancellationException

/** Finish the durable settings journal before feature ViewModels can read or write preferences. */
@Composable
internal fun RestoreAwareApp(onRestoreComplete: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val repository = remember { BackupRepository(context, WorkoutDatabase.get(context)) }
    val saved = rememberSaveableStateHolder()
    var generation by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(retry) {
        failed = false
        try { repository.recoverPending(); generation = repository.generation() }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
    }
    val ready = generation
    if (ready != null) saved.SaveableStateProvider("data-generation-$ready") { WorkoutLoggerApp(onRestoreComplete) }
    else Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (failed) {
                Text("Could not open local data or finish a pending restore. Your records remain on this device.")
                Button(onClick = { retry++ }) { Text("Retry") }
            } else { CircularProgressIndicator(); Text("Opening local data…") }
        }
    }
}
