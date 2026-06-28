package com.publilius.scroller.data

import android.content.Context

object AppContainer {
    @Volatile
    private var settingsRepository: SettingsRepository? = null

    fun settingsRepository(context: Context): SettingsRepository {
        return settingsRepository ?: synchronized(this) {
            settingsRepository ?: AndroidSettingsRepository.create(context.applicationContext).also {
                settingsRepository = it
            }
        }
    }
}
