package dev.clean.lift.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Visible-screen rest countdown for Workout (2026-11-19). Times use SystemClock.elapsedRealtime so
 * wall-clock changes do not matter. Nothing is stored and no background work, sound, vibration or
 * permission is involved: leaving the app does not alert you when rest ends.
 */
internal object RestTimer {
    const val DEFAULT_SECONDS = 120
    const val EXTEND_SECONDS = 30
    /** How long the finished state stays visible before the card clears itself. */
    const val DONE_LINGER_MILLIS = 10_000L
    const val MAX_SECONDS = 600

    fun seconds(planned: Int?): Int = (planned ?: DEFAULT_SECONDS).coerceIn(1, MAX_SECONDS)

    /** Whole seconds left, rounded up so 0 appears only when rest is over. */
    fun remaining(endsAt: Long, now: Long): Int = if (endsAt <= now) 0 else ((endsAt - now + 999) / 1000).toInt()

    fun extend(endsAt: Long, now: Long): Long = maxOf(endsAt, now) + EXTEND_SECONDS * 1000L

    fun expired(endsAt: Long, now: Long): Boolean = endsAt > 0 && now >= endsAt + DONE_LINGER_MILLIS

    fun format(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
internal fun RestCard(remaining: Int, onExtend: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    val done = remaining == 0
    val colors = if (done) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
        else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
    Card(modifier.fillMaxWidth(), colors = colors) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }) {
                Text(if (done) "Rest done" else "Rest", style = MaterialTheme.typography.labelLarge)
                Text(if (done) "Next set when ready" else RestTimer.format(remaining), style = if (done) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium)
            }
            if (!done) TextButton(onClick = onExtend) { Text("+30s") }
            TextButton(onClick = onSkip) { Text(if (done) "Dismiss" else "Skip") }
        }
    }
}
