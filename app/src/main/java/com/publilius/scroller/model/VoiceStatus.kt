package com.publilius.scroller.model

import com.publilius.scroller.R

enum class VoiceStatus(val labelResId: Int) {
    Inactive(R.string.voice_status_inactive),
    Disabled(R.string.voice_status_disabled),
    ListeningForPause(R.string.voice_status_listening_pause),
    ListeningForStart(R.string.voice_status_listening_start),
    PermissionRequired(R.string.voice_status_permission_required),
    Unavailable(R.string.voice_status_unavailable),
    Error(R.string.voice_status_error),
}
