package com.publilius.scroller.data

import com.publilius.scroller.model.OverlayPosition
import com.publilius.scroller.model.ScrollSpeed
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val scrollSpeed: Flow<ScrollSpeed>

    suspend fun setScrollSpeed(speed: ScrollSpeed)

    suspend fun getOverlayPosition(): OverlayPosition?

    suspend fun setOverlayPosition(position: OverlayPosition)
}
