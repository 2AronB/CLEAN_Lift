package dev.clean.lift.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.R
import dev.clean.lift.widgets.WidgetsScreen

/*
 * Four tabs (owner request 2026-11-19: consolidate). Enum names stay as before so saved tab state
 * and docs keep matching; only the labels changed. Settings opens from the gear on each tab root.
 */
private enum class Destination(val label: String, val icon: Int) {
    Workout("Workout", R.drawable.ic_workout), Templates("Plans", R.drawable.ic_templates),
    Metrics("Progress", R.drawable.ic_metrics), Widgets("Tools", R.drawable.ic_widgets)
}

@Composable
fun WorkoutLoggerApp(onRestoreComplete: () -> Unit) {
    val workoutModel: WorkoutViewModel = viewModel()
    var selectedName by rememberSaveable { mutableStateOf(Destination.Workout.name) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val selected = Destination.entries.firstOrNull { it.name == selectedName } ?: Destination.Workout
    val tabState = rememberSaveableStateHolder()
    val tabs: @Composable () -> Unit = {
        NavigationBar {
            Destination.entries.forEach { destination ->
                NavigationBarItem(selected = selected == destination, onClick = { selectedName = destination.name },
                    icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                    label = { Text(destination.label) })
            }
        }
    }
    val home = { selectedName = Destination.Workout.name }
    val openSettings = { settingsOpen = true }
    // Settings covers the current tab; Back returns to that tab with its state kept by the holder.
    if (settingsOpen) {
        tabState.SaveableStateProvider("Settings") {
            SettingsScreen(onBack = { settingsOpen = false }, onRestoreComplete = onRestoreComplete)
        }
        return
    }
    tabState.SaveableStateProvider(selected.name) {
        when (selected) {
            Destination.Workout -> WorkoutScreen(bottomBar = tabs, model = workoutModel, onSettings = openSettings)
            Destination.Templates -> TemplatesScreen(onBack = home, onStart = {
                workoutModel.startTemplate(it); selectedName = Destination.Workout.name
            }, bottomBar = tabs, onSettings = openSettings)
            Destination.Metrics -> MetricsHome(onBack = home, bottomBar = tabs, onSettings = openSettings)
            Destination.Widgets -> WidgetsScreen(onBack = home, bottomBar = tabs, onSettings = openSettings)
        }
    }
}
