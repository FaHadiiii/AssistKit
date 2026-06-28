package com.publilius.scroller.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndOfContentTrackerTest {
    @Test
    fun trackerStops_afterThresholdOfNoEffectAttempts() {
        val tracker = EndOfContentTracker(threshold = 3)

        assertFalse(tracker.recordAttempt(hadEffect = false))
        assertFalse(tracker.recordAttempt(hadEffect = false))
        assertTrue(tracker.recordAttempt(hadEffect = false))
    }

    @Test
    fun trackerResetsAfterObservedEffect() {
        val tracker = EndOfContentTracker(threshold = 2)

        assertFalse(tracker.recordAttempt(hadEffect = false))
        assertFalse(tracker.recordAttempt(hadEffect = true))
        assertFalse(tracker.recordAttempt(hadEffect = false))
        assertTrue(tracker.recordAttempt(hadEffect = false))
    }
}
