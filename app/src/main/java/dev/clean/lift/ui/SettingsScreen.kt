package dev.clean.lift.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.clean.lift.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onRestoreComplete: () -> Unit, model: SettingsViewModel = viewModel(), bottomBar: @Composable () -> Unit = {}) {
    var backup by rememberSaveable { mutableStateOf(false) }
    if (backup) { BackupScreen(onBack = { backup = false }, onRestored = onRestoreComplete); return }
    val profile by model.state.collectAsStateWithLifecycle()
    val attempts by model.attempts.collectAsStateWithLifecycle()
    val appearance by model.appearance.collectAsStateWithLifecycle()
    val appearanceFailed by model.appearanceFailed.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var discard by rememberSaveable { mutableStateOf(false) }
    var saveRequested by rememberSaveable { mutableStateOf(false) }
    // These are presentation-only states. No advertising/tracking/purchasing setting exists.
    var attempted by rememberSaveable { mutableStateOf<Int?>(null) }
    var attemptStartedAt by rememberSaveable { mutableLongStateOf(0L) }
    var joke by rememberSaveable { mutableStateOf<String?>(null) }
    val ads = stringArrayResource(R.array.ads_jokes)
    val trackers = stringArrayResource(R.array.trackers_jokes)
    val purchases = stringArrayResource(R.array.purchases_jokes)
    val firstResponse = stringResource(R.string.stop_that)

    fun leave() {
        if (profile.saving) return
        if (editing) {
            if (name != profile.name || age != profile.age || gender != profile.gender) discard = true else editing = false
        } else onBack()
    }
    BackHandler { leave() }
    LaunchedEffect(profile, saveRequested) {
        if (saveRequested && !profile.saving) {
            if (!profile.failed) editing = false
            saveRequested = false
        }
    }
    LaunchedEffect(attempted, profile.loading) {
        val target = attempted ?: return@LaunchedEffect
        if (profile.loading) return@LaunchedEffect
        val count = attempts[target]
        delay((5_000 - (SystemClock.elapsedRealtime() - attemptStartedAt)).coerceIn(0L, 5_000L))
        attempted = null
        joke = when {
            count < 3 -> null
            count == 3 -> firstResponse
            else -> when (target) {
                0 -> ads[(count - 4) % ads.size]
                1 -> trackers[(count - 4) % trackers.size]
                else -> purchases[(count - 4) % purchases.size]
            }
        }
    }
    if (joke != null) AlertDialog(
        onDismissRequest = { joke = null },
        title = { Text(stringResource(R.string.clean_error)) },
        text = { Text(joke!!) },
        confirmButton = { TextButton(onClick = { joke = null }) { Text(stringResource(R.string.keep_it_clean)) } }
    )
    if (discard) AlertDialog(
        onDismissRequest = { discard = false },
        title = { Text(stringResource(R.string.discard_title)) },
        text = { Text(stringResource(R.string.discard_message)) },
        confirmButton = { TextButton(onClick = { discard = false; editing = false }) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.keep_editing)) } }
    )
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = { TopAppBar(
            title = { Text(stringResource(if (editing) R.string.local_profile else R.string.settings)) },
            navigationIcon = { BackButton(onClick = { leave() }, enabled = !profile.saving) },
            actions = {
                if (editing) TextButton(
                    enabled = !profile.saving && validProfile(name, age, gender),
                    onClick = { model.save(name, age, gender); saveRequested = true }
                ) { Text(stringResource(if (profile.saving) R.string.saving else R.string.save)) }
            }
        ) }, bottomBar = { if (!editing) bottomBar() }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (editing) {
                item { Text(stringResource(R.string.profile_privacy)) }
                item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), enabled = !profile.saving,
                    label = { Text(stringResource(R.string.profile_name)) }, singleLine = true,
                    isError = name.trim().length > 80, supportingText = { Text(stringResource(R.string.profile_name_hint)) }) }
                item { OutlinedTextField(age, { age = it }, Modifier.fillMaxWidth(), enabled = !profile.saving,
                    label = { Text(stringResource(R.string.profile_age)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !validProfile("", age), supportingText = { Text(stringResource(R.string.profile_age_hint)) }) }
                item {
                    Text(stringResource(R.string.profile_gender), style = MaterialTheme.typography.bodyLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("" to R.string.not_set, "M" to R.string.gender_m, "F" to R.string.gender_f).forEach { (value, label) ->
                            FilterChip(selected = gender == value, onClick = { gender = value }, enabled = !profile.saving,
                                label = { Text(stringResource(label)) })
                        }
                    }
                    Text(stringResource(R.string.profile_gender_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (profile.failed) item { Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error) }
            } else {
                item { SettingsHeading("Appearance") }
                item {
                    Text("Theme", style = MaterialTheme.typography.bodyLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(selected = appearance.mode == mode,
                                onClick = { model.setAppearance(appearance.copy(mode = mode)) },
                                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)) { Text(mode.label) }
                        }
                    }
                }
                item { Text("Palette", style = MaterialTheme.typography.bodyLarge) }
                Palette.entries.filter { it != Palette.WALLPAPER || wallpaperColorsAvailable }.forEach { palette ->
                    item(key = "palette-${palette.name}") { PaletteRow(palette, appearance.palette == palette) {
                        model.setAppearance(appearance.copy(palette = palette))
                    } }
                }
                if (appearanceFailed) item { Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error) }
                item { HorizontalDivider() }
                item { SettingsHeading(stringResource(R.string.local_profile)) }
                item { Text(stringResource(R.string.profile_privacy)) }
                item { Text(stringResource(R.string.profile_summary,
                    profile.name.ifBlank { stringResource(R.string.not_set) }, profile.age.ifBlank { stringResource(R.string.not_set) },
                    profile.gender.ifBlank { stringResource(R.string.not_set) })) }
                item { OutlinedButton(enabled = !profile.loading, onClick = {
                    name = profile.name; age = profile.age; gender = profile.gender; editing = true
                }) { Text(stringResource(R.string.edit_profile)) } }
                item { HorizontalDivider() }
                item { SettingsHeading(stringResource(R.string.your_data)) }
                item { OutlinedButton(onClick = { backup = true }) { Text("Backup & restore") } }
                item { Text("Save a readable local backup or validate and restore one.", style = MaterialTheme.typography.bodySmall) }
                item { HorizontalDivider() }
                listOf(R.string.enable_ads, R.string.enable_trackers, R.string.enable_purchases).forEachIndexed { index, label ->
                    item {
                        val title = stringResource(R.string.switch_with_status, stringResource(label),
                            stringResource(if (attempted == index) R.string.switch_turning_on else R.string.switch_off))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(title, Modifier.weight(1f).padding(end = 12.dp))
                            Switch(checked = attempted == index, enabled = !profile.loading && attempted == null && joke == null,
                                onCheckedChange = { checked -> if (checked) {
                                    model.recordAttempt(index)
                                    attemptStartedAt = SystemClock.elapsedRealtime()
                                    attempted = index
                                } },
                                modifier = Modifier.semantics { contentDescription = title })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeading(text: String) {
    Text(text, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}

/** A palette choice with a preview of its main colors in the current light/dark mode. */
@Composable
private fun PaletteRow(palette: Palette, selected: Boolean, onSelect: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scheme = if (palette == Palette.WALLPAPER) colorSchemeFor(LocalContext.current, palette, dark) else builtInScheme(palette, dark)
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(palette.label, style = MaterialTheme.typography.titleMedium)
            Text(palette.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            listOf(scheme.primary, scheme.secondaryContainer, scheme.tertiary).forEach { color ->
                Box(Modifier.size(24.dp).clip(CircleShape).background(color).border(2.dp, MaterialTheme.colorScheme.surface, CircleShape))
            }
        }
    }
}
