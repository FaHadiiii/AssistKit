package com.publilius.scroller.model

sealed interface ScrollState {
    data object Idle : ScrollState
    data object Running : ScrollState
    data object Paused : ScrollState
    data object StoppedDueToEnd : ScrollState
}
