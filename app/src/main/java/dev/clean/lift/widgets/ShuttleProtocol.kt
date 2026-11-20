package dev.clean.lift.widgets

data class ShuttleProgress(val attempts: Int = 0, val successful: Int = 0, val lastSuccess: Int = 0, val misses: Int = 0, val consecutive: Int = 0, val ended: Boolean = false) {
    fun judge(hamr: Boolean, reached: Boolean): ShuttleProgress {
        require(!ended && attempts < 1000)
        val next = attempts + 1
        val warnings = if (reached) 0 else consecutive + 1
        val totalMisses = misses + if (reached) 0 else 1
        return copy(attempts = next, successful = successful + if (reached) 1 else 0, lastSuccess = if (reached) next else lastSuccess,
            misses = totalMisses, consecutive = warnings, ended = if (hamr) warnings >= 3 else totalMisses >= 2)
    }
}
