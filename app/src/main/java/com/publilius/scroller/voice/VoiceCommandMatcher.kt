package com.publilius.scroller.voice

import com.publilius.scroller.model.ScrollState
import java.util.Locale

enum class VoiceCommand {
    Start,
    Pause,
}

object VoiceCommandMatcher {
    private val fillerTokens = setOf(
        "please",
        "scroll",
        "scrolling",
        "now",
        "assistkit",
        "hey",
        "ok",
        "okay",
    )

    fun expectedCommand(state: ScrollState): VoiceCommand? {
        return when (state) {
            ScrollState.Running -> VoiceCommand.Pause
            ScrollState.Paused -> VoiceCommand.Start
            ScrollState.Idle,
            ScrollState.StoppedDueToEnd,
            -> null
        }
    }

    fun match(state: ScrollState, transcripts: List<String>): VoiceCommand? {
        val expected = expectedCommand(state) ?: return null
        return transcripts.firstNotNullOfOrNull { transcript ->
            if (matchesExpected(transcript, expected)) expected else null
        }
    }

    private fun matchesExpected(transcript: String, expected: VoiceCommand): Boolean {
        val normalized = transcript
            .lowercase(Locale.US)
            .replace(Regex("[^a-z\\s]"), " ")
            .trim()
        if (normalized.isEmpty()) {
            return false
        }

        val tokens = normalized.split(Regex("\\s+"))
        val commandToken = when (expected) {
            VoiceCommand.Start -> "start"
            VoiceCommand.Pause -> "pause"
        }

        return tokens.contains(commandToken) &&
            tokens.all { token -> token == commandToken || token in fillerTokens }
    }
}
