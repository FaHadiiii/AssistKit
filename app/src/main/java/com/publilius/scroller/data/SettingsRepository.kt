package com.publilius.scroller.data

import com.publilius.scroller.model.OverlayPosition
import com.publilius.scroller.model.ScrollSpeed
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val scrollSpeed: Flow<ScrollSpeed>
    val voiceCommandsEnabled: Flow<Boolean>

    suspend fun setScrollSpeed(speed: ScrollSpeed)
    suspend fun setVoiceCommandsEnabled(enabled: Boolean)

    suspend fun getOverlayPosition(): OverlayPosition?

    suspend fun setOverlayPosition(position: OverlayPosition)
}
