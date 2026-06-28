package com.publilius.scroller.voice

import com.publilius.scroller.model.ScrollState
import com.publilius.scroller.model.VoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceControlPolicyTest {
    @Test
    fun listensOnlyWhenRunningOrPaused() {
        assertTrue(VoiceControlPolicy.shouldListen(ScrollState.Running))
        assertTrue(VoiceControlPolicy.shouldListen(ScrollState.Paused))
        assertFalse(VoiceControlPolicy.shouldListen(ScrollState.Idle))
        assertFalse(VoiceControlPolicy.shouldListen(ScrollState.StoppedDueToEnd))
    }

    @Test
    fun returnsListeningStatusesForEligibleStates() {
        assertEquals(
            VoiceStatus.ListeningForPause,
            VoiceControlPolicy.statusFor(
                state = ScrollState.Running,
                voiceCommandsEnabled = true,
                hasMicrophonePermission = true,
                recognitionAvailable = true,
            ),
        )
        assertEquals(
            VoiceStatus.ListeningForStart,
            VoiceControlPolicy.statusFor(
                state = ScrollState.Paused,
                voiceCommandsEnabled = true,
                hasMicrophonePermission = true,
                recognitionAvailable = true,
            ),
        )
    }

    @Test
    fun returnsFallbackStatusesForPermissionAvailabilityAndErrors() {
        assertEquals(
            VoiceStatus.PermissionRequired,
            VoiceControlPolicy.statusFor(
                state = ScrollState.Running,
                voiceCommandsEnabled = true,
                hasMicrophonePermission = false,
                recognitionAvailable = true,
            ),
        )
        assertEquals(
            VoiceStatus.Unavailable,
            VoiceControlPolicy.statusFor(
                state = ScrollState.Paused,
                voiceCommandsEnabled = true,
                hasMicrophonePermission = true,
                recognitionAvailable = false,
            ),
        )
        assertEquals(
            VoiceStatus.Error,
            VoiceControlPolicy.statusFor(
                state = ScrollState.Running,
                voiceCommandsEnabled = true,
                hasMicrophonePermission = true,
                recognitionAvailable = true,
                blockedByErrors = true,
            ),
        )
    }

    @Test
    fun returnsDisabledWhenVoiceCommandsAreTurnedOff() {
        assertEquals(
            VoiceStatus.Disabled,
            VoiceControlPolicy.statusFor(
                state = ScrollState.Running,
                voiceCommandsEnabled = false,
                hasMicrophonePermission = true,
                recognitionAvailable = true,
            ),
        )
    }
}
