package dev.clean.lift.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.clean.lift.R

/** The native top-bar Back arrow used by every sub-page (replaces the former text "Back" buttons). */
@Composable
internal fun BackButton(onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
    }
}

/** App Settings live behind this gear on each tab's top bar rather than in the navigation bar. */
@Composable
internal fun SettingsButton(onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
    }
}
