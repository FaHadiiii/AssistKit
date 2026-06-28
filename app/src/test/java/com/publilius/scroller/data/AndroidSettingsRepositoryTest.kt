package com.publilius.scroller.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.publilius.scroller.model.OverlayPosition
import com.publilius.scroller.model.ScrollSpeed
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidSettingsRepositoryTest {
    @Test
    fun repositoryPersistsScrollSpeedAndOverlayPosition() = runTest {
        val tempDirectory = Files.createTempDirectory("settings-repo-test")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tempDirectory.resolve("settings.preferences_pb").toFile() },
        )
        val repository = AndroidSettingsRepository(dataStore)

        assertEquals(ScrollSpeed.DEFAULT, repository.scrollSpeed.first())
        assertNull(repository.getOverlayPosition())

        repository.setScrollSpeed(ScrollSpeed.fromLevel(9))
        repository.setOverlayPosition(OverlayPosition(x = 120, y = 240))

        assertEquals(ScrollSpeed.fromLevel(9), repository.scrollSpeed.first())
        assertEquals(OverlayPosition(120, 240), repository.getOverlayPosition())
    }
}
