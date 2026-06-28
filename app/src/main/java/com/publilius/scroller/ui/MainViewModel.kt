package com.publilius.scroller.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.publilius.scroller.controller.ScrollController
import com.publilius.scroller.data.SettingsRepository
import com.publilius.scroller.model.ScrollSpeed
import com.publilius.scroller.model.ScrollState
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
    ),
    val scrollState: ScrollState = ScrollState.Idle,
    val scrollSpeed: ScrollSpeed = ScrollSpeed.DEFAULT,
    val overlayVisible: Boolean = false,
)

class MainViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val permissions = MutableStateFlow(
        PermissionStatus(
            accessibilityEnabled = false,
            overlayEnabled = false,
        ),
    )

    val uiState: StateFlow<MainUiState> = combine(
        permissions,
        ScrollController.state,
        ScrollController.scrollSpeed,
        ScrollController.overlayVisible,
    ) { permissionStatus, scrollState, scrollSpeed, overlayVisible ->
        MainUiState(
            permissions = permissionStatus,
            scrollState = scrollState,
            scrollSpeed = scrollSpeed,
            overlayVisible = overlayVisible,
        )
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

    class Factory(
        private val settingsRepository: SettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(settingsRepository) as T
        }
    }
}
