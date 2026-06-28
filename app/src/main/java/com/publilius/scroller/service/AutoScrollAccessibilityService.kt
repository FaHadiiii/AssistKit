package com.publilius.scroller.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.publilius.scroller.controller.ScrollCommand
import com.publilius.scroller.controller.ScrollController
import com.publilius.scroller.model.ScrollProfile
import com.publilius.scroller.model.ScrollState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume

class AutoScrollAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val eventCounter = AtomicLong(0L)
    private val endOfContentTracker = EndOfContentTracker()

    private var loopJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            ScrollController.commands.collectLatest { command ->
                when (command) {
                    ScrollCommand.Start -> startScrollLoop()
                    ScrollCommand.Pause -> stopScrollLoop()
                    ScrollCommand.Stop -> stopScrollLoop()
                    ScrollCommand.LockScreen -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                    ScrollCommand.VolumeUp,
                    ScrollCommand.VolumeDown,
                    -> Unit
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName == packageName) {
            return
        }
        if (
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            eventCounter.incrementAndGet()
        }
    }

    override fun onInterrupt() {
        stopScrollLoop()
    }

    override fun onDestroy() {
        stopScrollLoop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startScrollLoop() {
        if (loopJob?.isActive == true) {
            return
        }
        endOfContentTracker.reset()
        loopJob = serviceScope.launch {
            while (ScrollController.state.value == ScrollState.Running) {
                val profile = ScrollController.scrollSpeed.value.profile
                val baseline = eventCounter.get()
                val dispatched = dispatchScrollGesture(profile)
                val hadEffect = dispatched && waitForObservedEffect(baseline)
                val shouldStop = endOfContentTracker.recordAttempt(hadEffect)
                if (shouldStop) {
                    ScrollController.stopDueToEnd()
                    break
                }
                delay(profile.intervalMs)
            }
        }
    }

    private fun stopScrollLoop() {
        loopJob?.cancel()
        loopJob = null
        endOfContentTracker.reset()
    }

    private suspend fun waitForObservedEffect(baseline: Long): Boolean {
        return withTimeoutOrNull(EFFECT_TIMEOUT_MS) {
            while (eventCounter.get() <= baseline) {
                delay(60L)
            }
            true
        } ?: false
    }

    private suspend fun dispatchScrollGesture(profile: ScrollProfile): Boolean {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        val path = Path().apply {
            moveTo(width * profile.xFraction, height * profile.startYFraction)
            lineTo(width * profile.xFraction, height * profile.endYFraction)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0L,
                    profile.gestureDurationMs,
                ),
            )
            .build()

        return suspendCancellableCoroutine { continuation ->
            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }
            val dispatched = dispatchGesture(gesture, callback, null)
            if (!dispatched && continuation.isActive) {
                continuation.resume(false)
            }
        }
    }

    companion object {
        private const val EFFECT_TIMEOUT_MS = 900L
    }
}
