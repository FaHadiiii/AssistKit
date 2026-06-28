package com.publilius.scroller.voice

import com.publilius.scroller.model.ScrollState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandMatcherTest {
    @Test
    fun matchesPauseOnlyWhileRunning() {
        assertEquals(
            VoiceCommand.Pause,
            VoiceCommandMatcher.match(ScrollState.Running, listOf("please pause scrolling now")),
        )
        assertNull(
            VoiceCommandMatcher.match(ScrollState.Paused, listOf("please pause scrolling now")),
        )
    }

    @Test
    fun matchesStartOnlyWhilePaused() {
        assertEquals(
            VoiceCommand.Start,
            VoiceCommandMatcher.match(ScrollState.Paused, listOf("assistkit start")),
        )
        assertNull(
            VoiceCommandMatcher.match(ScrollState.Running, listOf("assistkit start")),
        )
    }
}
