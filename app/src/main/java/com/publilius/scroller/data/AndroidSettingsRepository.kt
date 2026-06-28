package com.publilius.scroller.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.publilius.scroller.model.OverlayPosition
import com.publilius.scroller.model.ScrollSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class AndroidSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val scrollSpeed: Flow<ScrollSpeed> = dataStore.data.map { preferences ->
        val level = preferences[SCROLL_SPEED_LEVEL_KEY]
            ?: preferences[LEGACY_SPEED_PRESET_KEY]?.let(::legacyPresetToLevel)
            ?: ScrollSpeed.DEFAULT_LEVEL
        ScrollSpeed.fromLevel(level)
    }

    override suspend fun setScrollSpeed(speed: ScrollSpeed) {
        dataStore.edit { preferences ->
            preferences[SCROLL_SPEED_LEVEL_KEY] = speed.level
            preferences.remove(LEGACY_SPEED_PRESET_KEY)
        }
    }

    override suspend fun getOverlayPosition(): OverlayPosition? {
        return dataStore.data.map { preferences ->
            val x = preferences[OVERLAY_X_KEY]
            val y = preferences[OVERLAY_Y_KEY]
            if (x == null || y == null) {
                null
            } else {
                OverlayPosition(x = x, y = y)
            }
        }.firstOrNull()
    }

    override suspend fun setOverlayPosition(position: OverlayPosition) {
        dataStore.edit { preferences ->
            preferences[OVERLAY_X_KEY] = position.x
            preferences[OVERLAY_Y_KEY] = position.y
        }
    }

    companion object {
        private val SCROLL_SPEED_LEVEL_KEY = intPreferencesKey("scroll_speed_level")
        private val LEGACY_SPEED_PRESET_KEY = stringPreferencesKey("speed_preset")
        private val OVERLAY_X_KEY = intPreferencesKey("overlay_x")
        private val OVERLAY_Y_KEY = intPreferencesKey("overlay_y")

        private fun legacyPresetToLevel(value: String): Int {
            return when (value) {
                "SLOW" -> 3
                "MEDIUM" -> 5
                "FAST" -> 8
                else -> ScrollSpeed.DEFAULT_LEVEL
            }
        }

        fun create(context: Context): AndroidSettingsRepository {
            val dataStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                produceFile = { context.preferencesDataStoreFile("auto_scroller_settings.preferences_pb") },
            )
            return AndroidSettingsRepository(dataStore)
        }
    }
}
