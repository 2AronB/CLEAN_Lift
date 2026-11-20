package dev.clean.lift.widgets

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShuttleAudioState(val ready: Boolean = false, val playing: Boolean = false, val ended: Boolean = false,
    val position: Int = 0, val interrupted: Boolean = false, val message: String = "")
class ShuttleAudioModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("widget-audio", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(ShuttleAudioState(ended = saved["ended"] ?: false, position = saved["position"] ?: 0,
        interrupted = saved.get<Boolean>("interrupted") == true || saved.get<Boolean>("playing") == true))
    val state = mutable.asStateFlow()
    private var player: MediaPlayer? = null
    private var protocol = saved.get<String>("protocol").orEmpty()
    private var lastTick = 0L
    fun load(kind: String, uri: Uri? = null) {
        if (uri == null && kind == protocol && player != null) return
        val retained = if (uri == null && kind == protocol) mutable.value else ShuttleAudioState()
        release(); protocol = kind
        val selected = uri ?: preferences.getString(kind, null)?.let(Uri::parse)
        if (selected == null) { mutable.value = retained.copy(message = "Choose the matching official 20 m recording stored on your device."); persist(); return }
        mutable.value = retained.copy(ready = false, playing = false, message = "Loading local recording…"); persist()
        try {
            val newPlayer = MediaPlayer()
            player = newPlayer
            newPlayer.setOnPreparedListener {
                if (player !== it) return@setOnPreparedListener
                mutable.value = retained.copy(ready = true, playing = false, ended = retained.ended || retained.interrupted,
                    message = if (retained.interrupted) "Recording interrupted. Start a new attempt for reliable cues." else "Local recording ready. Verify its protocol before starting.")
                if (uri != null && !preferences.edit().putString(kind, selected.toString()).commit()) mutable.value = mutable.value.copy(message = "Ready for this run; could not remember the recording.")
                persist()
            }
            newPlayer.setOnCompletionListener { mutable.value = mutable.value.copy(playing = false, ended = true, position = it.duration); persist() }
            newPlayer.setOnErrorListener { _, _, _ -> mutable.value = mutable.value.copy(playing = false, ready = false, ended = true, interrupted = true, message = "Audio failed. Reselect a local recording."); persist(); true }
            newPlayer.setDataSource(getApplication(), selected)
            newPlayer.prepareAsync()
        } catch (_: Exception) { release(); mutable.value = retained.copy(ready = false, playing = false, message = "Could not open the recording. Choose it again."); persist() }
    }
    fun start() {
        if (!mutable.value.ready || mutable.value.playing || mutable.value.ended) return
        runCatching { player?.start(); lastTick = android.os.SystemClock.elapsedRealtime(); mutable.value = mutable.value.copy(playing = true) }
            .onFailure { mutable.value = mutable.value.copy(interrupted = true, message = "Could not start audio.") }
        persist()
    }
    fun tick() {
        if (!mutable.value.playing) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastTick > 1500) { stop(interrupted = true); return }
        lastTick = now
        runCatching { mutable.value = mutable.value.copy(position = player?.currentPosition ?: 0) }
        persist()
    }
    fun stop(interrupted: Boolean = false) {
        if (mutable.value.playing) {
            runCatching { player?.pause() }
            mutable.value = mutable.value.copy(playing = false, ended = true, interrupted = mutable.value.interrupted || interrupted)
            persist()
        }
    }
    fun restart(kind: String) { protocol = ""; load(kind) }
    private fun persist() {
        saved["protocol"] = protocol; saved["playing"] = mutable.value.playing; saved["ended"] = mutable.value.ended
        saved["interrupted"] = mutable.value.interrupted; saved["position"] = mutable.value.position
    }
    private fun release() { player?.release(); player = null }
    override fun onCleared() { release() }
}
