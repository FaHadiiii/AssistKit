package com.publilius.scroller.controller

import com.publilius.scroller.model.ScrollState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScrollControllerTest {
    @After
    fun tearDown() {
        ScrollController.resetForTests()
    }

    @Test
    fun stateTransitions_followIdleRunningPausedRunningIdle() = runTest {
        ScrollController.start()
        assertEquals(ScrollState.Running, ScrollController.state.value)

        ScrollController.pause()
        assertEquals(ScrollState.Paused, ScrollController.state.value)

        ScrollController.start()
        assertEquals(ScrollState.Running, ScrollController.state.value)

        ScrollController.stop()
        assertEquals(ScrollState.Idle, ScrollController.state.value)
    }

    @Test
    fun deviceCommands_emitWithoutChangingScrollState() = runTest {
        ScrollController.volumeUp()
        ScrollController.volumeDown()
        ScrollController.lockScreen()
        assertEquals(ScrollState.Idle, ScrollController.state.value)
    }
}
