package com.publilius.scroller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.publilius.scroller.controller.ScrollController
import com.publilius.scroller.data.SettingsRepository
import com.publilius.scroller.model.ScrollSpeed
import com.publilius.scroller.model.ScrollState
import com.publilius.scroller.model.VoiceStatus
import com.publilius.scroller.util.PermissionStatus
import com.publilius.scroller.util.PermissionStatusChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val permissions: PermissionStatus = PermissionStatus(
        accessibilityEnabled = false,
        overlayEnabled = false,
        microphoneEnabled = false,
    ),
    val scrollState: ScrollState = ScrollState.Idle,
    val scrollSpeed: ScrollSpeed = ScrollSpeed.DEFAULT,
    val overlayVisible: Boolean = false,
    val voiceStatus: VoiceStatus = VoiceStatus.Inactive,
    val voiceCommandsEnabled: Boolean = true,
)

class MainViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val permissions = MutableStateFlow(
        PermissionStatus(
            accessibilityEnabled = false,
            overlayEnabled = false,
            microphoneEnabled = false,
        ),
    )

    val uiState: StateFlow<MainUiState> = combine(
        permissions,
        ScrollController.state,
        ScrollController.scrollSpeed,
        ScrollController.overlayVisible,
        ScrollController.voiceStatus,
    ) { permissionStatus, scrollState, scrollSpeed, overlayVisible, voiceStatus ->
        MainUiState(
            permissions = permissionStatus,
            scrollState = scrollState,
            scrollSpeed = scrollSpeed,
            overlayVisible = overlayVisible,
            voiceStatus = voiceStatus,
        )
    }.combine(ScrollController.voiceCommandsEnabled) { uiState, voiceCommandsEnabled ->
        uiState.copy(voiceCommandsEnabled = voiceCommandsEnabled)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = MainUiState(),
    )

    init {
        viewModelScope.launch {
            settingsRepository.scrollSpeed.collect { speed ->
                ScrollController.syncSavedSpeed(speed)
            }
        }
        viewModelScope.launch {
            settingsRepository.voiceCommandsEnabled.collect { enabled ->
                ScrollController.syncVoiceCommandsEnabled(enabled)
            }
        }
    }

    fun refreshPermissions(context: android.content.Context) {
        permissions.value = PermissionStatusChecker.read(context)
    }

    fun setScrollSpeed(speed: ScrollSpeed) {
        ScrollController.setScrollSpeed(speed)
        viewModelScope.launch {
            settingsRepository.setScrollSpeed(speed)
        }
    }

    fun setVoiceCommandsEnabled(enabled: Boolean) {
        ScrollController.syncVoiceCommandsEnabled(enabled)
        viewModelScope.launch {
            settingsRepository.setVoiceCommandsEnabled(enabled)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(settingsRepository) as T
        }
    }
}
