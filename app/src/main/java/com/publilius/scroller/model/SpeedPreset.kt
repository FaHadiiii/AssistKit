package com.publilius.scroller.model

data class ScrollSpeed(
    val level: Int,
) {
    val profile: ScrollProfile
        get() {
            val normalized = (level - MIN_LEVEL).toFloat() / (MAX_LEVEL - MIN_LEVEL).toFloat()
            return ScrollProfile(
                gestureDurationMs = lerpLong(start = 440L, end = 200L, fraction = normalized),
                intervalMs = lerpLong(start = 3000L, end = 650L, fraction = normalized),
                startYFraction = lerpFloat(start = 0.70f, end = 0.84f, fraction = normalized),
                endYFraction = lerpFloat(start = 0.48f, end = 0.18f, fraction = normalized),
            )
        }

    fun increase(): ScrollSpeed = fromLevel(level + 1)

    fun decrease(): ScrollSpeed = fromLevel(level - 1)

    companion object {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 10
        const val DEFAULT_LEVEL = 5

        val DEFAULT = ScrollSpeed(DEFAULT_LEVEL)

        fun fromLevel(level: Int): ScrollSpeed = ScrollSpeed(
            level = level.coerceIn(MIN_LEVEL, MAX_LEVEL),
        )

        private fun lerpLong(start: Long, end: Long, fraction: Float): Long {
            return (start + ((end - start) * fraction)).toLong()
        }

        private fun lerpFloat(start: Float, end: Float, fraction: Float): Float {
            return start + ((end - start) * fraction)
        }
    }
}

data class ScrollProfile(
    val gestureDurationMs: Long,
    val intervalMs: Long,
    val startYFraction: Float,
    val endYFraction: Float,
    val xFraction: Float = 0.5f,
)
