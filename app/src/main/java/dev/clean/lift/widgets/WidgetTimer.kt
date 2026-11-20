package dev.clean.lift.widgets

data class TimerPhase(val label: String, val millis: Long)
data class TimerPosition(val phase: Int, val round: Int, val remaining: Long, val complete: Boolean)
data class TimerPlan(val phases: List<TimerPhase>, val rounds: Int) {
    init {
        require(phases.isNotEmpty() && phases.size <= 8 && phases.all { it.millis in 100..86_400_000 })
        require(rounds in 1..100_000 && cycleMillis <= 86_400_000 && totalMillis <= 86_400_000)
    }
    val cycleMillis: Long get() = phases.sumOf { it.millis }
    val totalMillis: Long get() = cycleMillis * rounds
    val signature: String get() = "$rounds/" + phases.joinToString("/") { "${it.label}:${it.millis}" }
    fun at(elapsed: Long): TimerPosition {
        val safe = elapsed.coerceIn(0, totalMillis)
        if (safe == totalMillis) return TimerPosition(phases.lastIndex, rounds, 0, true)
        var offset = safe % cycleMillis
        phases.forEachIndexed { index, phase ->
            if (offset < phase.millis) return TimerPosition(index, (safe / cycleMillis).toInt() + 1, phase.millis - offset, false)
            offset -= phase.millis
        }
        error("Unreachable timer position")
    }
    fun missedTransitions(before: Long, after: Long): Boolean {
        fun ordinal(p: TimerPosition) = (p.round-1).toLong()*phases.size + p.phase + if(p.complete) 1 else 0
        return ordinal(at(after)) - ordinal(at(before)) > 1
    }
}

object WidgetTimerPlans {
    fun general(mode: String, seconds: Int, rest: Int, rounds: Int): TimerPlan = when (mode) {
        "Stopwatch" -> TimerPlan(listOf(TimerPhase("Elapsed", 86_400_000)), 1)
        "Intervals" -> TimerPlan(listOf(TimerPhase("Work", seconds * 1000L), TimerPhase("Rest", rest * 1000L)), rounds)
        "EMOM" -> TimerPlan(listOf(TimerPhase("Minute", 60_000)), rounds)
        else -> TimerPlan(listOf(TimerPhase(if (mode == "AMRAP") "AMRAP" else "Time remaining", seconds * 1000L)), 1)
    }
    fun tempo(seconds: List<Int>, reps: Int, continuous: Boolean): TimerPlan {
        require(seconds.size == 4 && seconds.all { it in 0..30 } && seconds.sum() > 0)
        val labels = listOf("Lower", "Bottom pause", "Lift", "Top pause")
        val phases = seconds.mapIndexedNotNull { i, s -> if (s == 0) null else TimerPhase(labels[i], s * 1000L) }
        return TimerPlan(phases, if (continuous) (86_400_000 / phases.sumOf { it.millis }).toInt() else reps)
    }
    fun metronome(bpm: Int, beats: Int): TimerPlan {
        require(bpm in 30..300)
        return TimerPlan(listOf(TimerPhase("Beat", 60_000L / bpm)), beats)
    }
    fun clockText(millis: Long): String {
        val seconds = millis.coerceAtLeast(0) / 1000
        return if (seconds < 3600) "%d:%02d".format(java.util.Locale.US, seconds / 60, seconds % 60)
        else "%d:%02d:%02d".format(java.util.Locale.US, seconds / 3600, seconds / 60 % 60, seconds % 60)
    }
}
