package com.publilius.scroller.voice

import android.os.SystemClock
import com.publilius.scroller.model.ScrollState

class VoiceCommandProcessor(
    private val nowMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val debounceWindowMs: Long = DEFAULT_DEBOUNCE_WINDOW_MS,
    private val onPause: () -> Unit,
    private val onStart: () -> Unit,
) {
    private var lastHandledAtMs: Long? = null

    fun process(state: ScrollState, transcripts: List<String>): Boolean {
        val command = VoiceCommandMatcher.match(state, transcripts) ?: return false
        val now = nowMs()
        if (lastHandledAtMs != null && now - lastHandledAtMs!! < debounceWindowMs) {
            return false
        }

        lastHandledAtMs = now
        when (command) {
            VoiceCommand.Pause -> onPause()
            VoiceCommand.Start -> onStart()
        }
        return true
    }

    fun resetDebounce() {
        lastHandledAtMs = null
    }

    companion object {
        private const val DEFAULT_DEBOUNCE_WINDOW_MS = 1_200L
    }
}
