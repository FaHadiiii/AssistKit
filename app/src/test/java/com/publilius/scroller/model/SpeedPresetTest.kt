package com.publilius.scroller.model

import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedPresetTest {
    @Test
    fun speedProfiles_progressFromLowToHigh() {
        val slow = ScrollSpeed.fromLevel(1).profile
        val medium = ScrollSpeed.fromLevel(5).profile
        val fast = ScrollSpeed.fromLevel(10).profile

        assertTrue(slow.intervalMs > medium.intervalMs)
        assertTrue(medium.intervalMs > fast.intervalMs)
        assertTrue(slow.startYFraction < fast.startYFraction)
        assertTrue(slow.endYFraction > fast.endYFraction)
    }
}
