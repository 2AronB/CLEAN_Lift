package dev.clean.lift.widgets

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WidgetClockState(val elapsed: Long = 0, val running: Boolean = false, val interrupted: Boolean = false,
    val laps: List<Long> = emptyList(), val tally: Int = 0)

// One visible clock per screen. Restored clocks are always paused; a process restart cannot replay cues.
class WidgetClockModel(private val saved: SavedStateHandle) : ViewModel() {
    private var plan: TimerPlan? = null
    private val mutable = MutableStateFlow(WidgetClockState(saved["elapsed"] ?: 0L,
        interrupted = saved.get<Boolean>("wasRunning") == true || saved.get<Boolean>("interrupted") == true,
        laps = saved.get<LongArray>("laps")?.toList().orEmpty(), tally = saved["tally"] ?: 0))
    val state = mutable.asStateFlow()
    private var began = 0L
    private var base = 0L
    private var lastTick = 0L
    private var tone: ToneGenerator? = null
    var sound = true
    fun configure(value: TimerPlan) {
        if (saved.get<String>("plan") != value.signature) reset()
        plan = value; saved["plan"] = value.signature
    }
    fun start() {
        val p = plan ?: return
        if (mutable.value.running || mutable.value.elapsed >= p.totalMillis) return
        base = mutable.value.elapsed; began = SystemClock.elapsedRealtime(); lastTick = began
        mutable.value = mutable.value.copy(running = true); persist(); cue()
    }
    fun tick() {
        if (!mutable.value.running) return
        val p = plan ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastTick > 1500) { pause(interrupted = true); return }
        val old = p.at(mutable.value.elapsed)
        val elapsed = (base + now - began).coerceAtMost(p.totalMillis)
        if (p.missedTransitions(mutable.value.elapsed,elapsed)) { pause(interrupted=true); return }
        val next = p.at(elapsed)
        mutable.value = mutable.value.copy(elapsed = elapsed, running = !next.complete)
        if (old.phase != next.phase || old.round != next.round || next.complete || elapsed / 15_000 != (base + lastTick - began) / 15_000) cue()
        lastTick = now; persist()
    }
    fun pause(interrupted: Boolean = false) {
        if (mutable.value.running) {
            val elapsed = (base + SystemClock.elapsedRealtime() - began).coerceAtMost(plan?.totalMillis ?: Long.MAX_VALUE)
            mutable.value = mutable.value.copy(elapsed = elapsed, running = false, interrupted = mutable.value.interrupted || interrupted)
            persist()
        }
    }
    fun reset() { mutable.value = WidgetClockState(); persist() }
    fun lap() { if (mutable.value.running && mutable.value.laps.size < 200) { mutable.value = mutable.value.copy(laps = mutable.value.laps + mutable.value.elapsed); persist() } }
    fun tally(delta: Int) { mutable.value = mutable.value.copy(tally = (mutable.value.tally + delta).coerceIn(0, 100_000)); persist() }
    private fun cue() {
        if (sound) runCatching { if (tone == null) tone = ToneGenerator(AudioManager.STREAM_MUSIC, 75); tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 90) }
    }
    private fun persist() {
        saved["elapsed"] = mutable.value.elapsed; saved["wasRunning"] = mutable.value.running
        saved["interrupted"] = mutable.value.interrupted
        saved["laps"] = mutable.value.laps.toLongArray(); saved["tally"] = mutable.value.tally
    }
    override fun onCleared() { tone?.release() }
}
