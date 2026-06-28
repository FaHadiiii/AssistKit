package com.publilius.scroller.service

class EndOfContentTracker(
    private val threshold: Int = DEFAULT_THRESHOLD,
) {
    private var consecutiveNoEffectAttempts: Int = 0

    fun recordAttempt(hadEffect: Boolean): Boolean {
        consecutiveNoEffectAttempts = if (hadEffect) {
            0
        } else {
            consecutiveNoEffectAttempts + 1
        }
        return consecutiveNoEffectAttempts >= threshold
    }

    fun reset() {
        consecutiveNoEffectAttempts = 0
    }

    companion object {
        const val DEFAULT_THRESHOLD = 3
    }
}
