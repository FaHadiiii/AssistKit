package com.publilius.scroller.controller

import com.publilius.scroller.model.ScrollState
import com.publilius.scroller.model.ScrollSpeed
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object ScrollController {
    private val _state = MutableStateFlow<ScrollState>(ScrollState.Idle)
    val state: StateFlow<ScrollState> = _state.asStateFlow()

    private val _scrollSpeed = MutableStateFlow(ScrollSpeed.DEFAULT)
    val scrollSpeed: StateFlow<ScrollSpeed> = _scrollSpeed.asStateFlow()

    private val _overlayExpanded = MutableStateFlow(false)
    val overlayExpanded: StateFlow<Boolean> = _overlayExpanded.asStateFlow()

    private val _overlayVisible = MutableStateFlow(false)
    val overlayVisible: StateFlow<Boolean> = _overlayVisible.asStateFlow()

    private val _commands = MutableSharedFlow<ScrollCommand>(extraBufferCapacity = 8)
    val commands: SharedFlow<ScrollCommand> = _commands.asSharedFlow()

    fun start() {
        _state.value = ScrollState.Running
        _commands.tryEmit(ScrollCommand.Start)
    }

    fun pause() {
        if (_state.value == ScrollState.Running) {
            _state.value = ScrollState.Paused
            _commands.tryEmit(ScrollCommand.Pause)
        }
    }

    fun stop() {
        _state.value = ScrollState.Idle
        _commands.tryEmit(ScrollCommand.Stop)
    }

    fun stopDueToEnd() {
        _state.value = ScrollState.StoppedDueToEnd
        _commands.tryEmit(ScrollCommand.Stop)
    }

    fun setScrollSpeed(speed: ScrollSpeed) {
        _scrollSpeed.value = speed
    }

    fun syncSavedSpeed(speed: ScrollSpeed) {
        _scrollSpeed.value = speed
    }

    fun increaseSpeed() {
        _scrollSpeed.value = _scrollSpeed.value.increase()
    }

    fun decreaseSpeed() {
        _scrollSpeed.value = _scrollSpeed.value.decrease()
    }

    fun setOverlayExpanded(expanded: Boolean) {
        _overlayExpanded.value = expanded
    }

    fun setOverlayVisible(visible: Boolean) {
        _overlayVisible.value = visible
    }

    fun volumeUp() {
        _commands.tryEmit(ScrollCommand.VolumeUp)
    }

    fun volumeDown() {
        _commands.tryEmit(ScrollCommand.VolumeDown)
    }

    fun lockScreen() {
        _commands.tryEmit(ScrollCommand.LockScreen)
    }

    fun resetForTests() {
        _state.value = ScrollState.Idle
        _scrollSpeed.value = ScrollSpeed.DEFAULT
        _overlayExpanded.value = false
        _overlayVisible.value = false
    }
}

sealed interface ScrollCommand {
    data object Start : ScrollCommand
    data object Pause : ScrollCommand
    data object Stop : ScrollCommand
    data object VolumeUp : ScrollCommand
    data object VolumeDown : ScrollCommand
    data object LockScreen : ScrollCommand
}
