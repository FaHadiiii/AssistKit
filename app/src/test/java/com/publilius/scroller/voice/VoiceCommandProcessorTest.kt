package com.publilius.scroller.voice

import com.publilius.scroller.model.ScrollState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandProcessorTest {
    @Test
    fun processesRecognizedCommandOnlyOnceInsideDebounceWindow() {
        var nowMs = 1_000L
        var pauseCalls = 0
        val processor = VoiceCommandProcessor(
            nowMs = { nowMs },
            onPause = { pauseCalls += 1 },
            onStart = { },
        )

        assertTrue(processor.process(ScrollState.Running, listOf("pause")))
        assertFalse(processor.process(ScrollState.Running, listOf("pause")))

        nowMs += 1_500L
        assertTrue(processor.process(ScrollState.Running, listOf("pause")))
        assertEquals(2, pauseCalls)
    }

    @Test
    fun routesStartWhenPaused() {
        var startCalls = 0
        var nowMs = 5_000L
        val processor = VoiceCommandProcessor(
            nowMs = { nowMs },
            onPause = { },
            onStart = { startCalls += 1 },
        )

        assertTrue(processor.process(ScrollState.Paused, listOf("start scrolling")))
        assertEquals(1, startCalls)
    }
}
