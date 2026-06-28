package com.publilius.scroller.voice

import com.publilius.scroller.model.ScrollState
import com.publilius.scroller.model.VoiceStatus

object VoiceControlPolicy {
    fun shouldListen(state: ScrollState): Boolean {
        return state == ScrollState.Running || state == ScrollState.Paused
    }

    fun statusFor(
        state: ScrollState,
        voiceCommandsEnabled: Boolean,
        hasMicrophonePermission: Boolean,
        recognitionAvailable: Boolean,
        blockedByErrors: Boolean = false,
    ): VoiceStatus {
        if (!voiceCommandsEnabled) {
            return VoiceStatus.Disabled
        }
        if (!shouldListen(state)) {
            return VoiceStatus.Inactive
        }
        if (!hasMicrophonePermission) {
            return VoiceStatus.PermissionRequired
        }
        if (!recognitionAvailable) {
            return VoiceStatus.Unavailable
        }
        if (blockedByErrors) {
            return VoiceStatus.Error
        }
        return when (state) {
            ScrollState.Running -> VoiceStatus.ListeningForPause
            ScrollState.Paused -> VoiceStatus.ListeningForStart
            ScrollState.Idle,
            ScrollState.StoppedDueToEnd,
            -> VoiceStatus.Inactive
        }
    }
}
