package com.publilius.scroller.service

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.publilius.scroller.R
import com.publilius.scroller.controller.ScrollController
import com.publilius.scroller.model.OverlayPosition
import com.publilius.scroller.model.ScrollSpeed
import com.publilius.scroller.model.ScrollState
import com.publilius.scroller.model.VoiceStatus
import kotlin.math.roundToInt

class OverlayManager(
    context: Context,
    private val onPersistPosition: (OverlayPosition) -> Unit,
    private val onSpeedChanged: (ScrollSpeed) -> Unit,
    private val onVolumeUp: () -> Unit,
    private val onVolumeDown: () -> Unit,
    private val onLockScreen: () -> Unit,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val rootView = LayoutInflater.from(context).inflate(R.layout.overlay_controls, null)
    private val collapsedPill: ImageButton = rootView.findViewById(R.id.overlayCollapsedPill)
    private val expandedPanel: LinearLayout = rootView.findViewById(R.id.overlayExpandedPanel)
    private val startButton: ImageButton = rootView.findViewById(R.id.overlayStartButton)
    private val pauseButton: ImageButton = rootView.findViewById(R.id.overlayPauseButton)
    private val stopButton: ImageButton = rootView.findViewById(R.id.overlayStopButton)
    private val volumeUpButton: ImageButton = rootView.findViewById(R.id.overlayVolumeUpButton)
    private val volumeDownButton: ImageButton = rootView.findViewById(R.id.overlayVolumeDownButton)
    private val lockScreenButton: ImageButton = rootView.findViewById(R.id.overlayLockScreenButton)
    private val speedDecreaseButton: ImageButton = rootView.findViewById(R.id.overlaySpeedDecreaseButton)
    private val speedIncreaseButton: ImageButton = rootView.findViewById(R.id.overlaySpeedIncreaseButton)
    private val collapseButton: View = rootView.findViewById(R.id.overlayCollapseButton)

    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 32
        y = 180
    }

    private var attached = false
    private var latestState: ScrollState = ScrollState.Idle
    private var latestSpeed: ScrollSpeed = ScrollSpeed.DEFAULT
    private var latestExpanded: Boolean = false
    private var latestVoiceStatus: VoiceStatus = VoiceStatus.Inactive

    private val dimRunnable = Runnable {
        rootView.alpha = IDLE_DIM_ALPHA
    }

    init {
        collapsedPill.setOnClickListener {
            registerInteraction()
            ScrollController.setOverlayExpanded(true)
        }
        collapseButton.setOnClickListener {
            registerInteraction()
            ScrollController.setOverlayExpanded(false)
        }
        startButton.setOnClickListener {
            registerInteraction()
            ScrollController.start()
        }
        pauseButton.setOnClickListener {
            registerInteraction()
            ScrollController.pause()
        }
        stopButton.setOnClickListener {
            registerInteraction()
            ScrollController.stop()
        }
        volumeUpButton.setOnClickListener {
            registerInteraction()
            onVolumeUp()
        }
        volumeDownButton.setOnClickListener {
            registerInteraction()
            onVolumeDown()
        }
        lockScreenButton.setOnClickListener {
            registerInteraction()
            onLockScreen()
        }
        speedDecreaseButton.setOnClickListener {
            registerInteraction()
            onSpeedChanged(latestSpeed.decrease())
        }
        speedIncreaseButton.setOnClickListener {
            registerInteraction()
            onSpeedChanged(latestSpeed.increase())
        }

        val dragListener = DragTouchListener(
            onInteraction = { registerInteraction() },
            onMoved = { x, y ->
                layoutParams.x = x
                layoutParams.y = y
                if (attached) {
                    windowManager.updateViewLayout(rootView, layoutParams)
                }
            },
            onReleased = {
                onPersistPosition(OverlayPosition(layoutParams.x, layoutParams.y))
            },
        )
        collapsedPill.setOnTouchListener(dragListener)
    }

    fun show(initialPosition: OverlayPosition?) {
        if (initialPosition != null) {
            layoutParams.x = initialPosition.x
            layoutParams.y = initialPosition.y
        }
        if (!attached) {
            windowManager.addView(rootView, layoutParams)
            attached = true
        } else {
            windowManager.updateViewLayout(rootView, layoutParams)
        }
        registerInteraction()
        render(latestState, latestSpeed, latestExpanded, latestVoiceStatus)
    }

    fun hide() {
        if (attached) {
            rootView.removeCallbacks(dimRunnable)
            windowManager.removeView(rootView)
            attached = false
        }
    }

    fun render(state: ScrollState, scrollSpeed: ScrollSpeed, expanded: Boolean, voiceStatus: VoiceStatus) {
        latestState = state
        latestSpeed = scrollSpeed
        latestExpanded = expanded
        latestVoiceStatus = voiceStatus

        collapsedPill.isVisible = !expanded
        expandedPanel.isVisible = expanded
        collapsedPill.contentDescription = when (state) {
            ScrollState.Idle -> collapsedPill.context.getString(R.string.state_idle)
            ScrollState.Running -> collapsedPill.context.getString(R.string.state_running)
            ScrollState.Paused -> collapsedPill.context.getString(R.string.state_paused)
            ScrollState.StoppedDueToEnd -> collapsedPill.context.getString(R.string.state_end)
        }
        styleCollapsedButton(state)
        styleActionButton(startButton, enabled = state != ScrollState.Running)
        styleActionButton(pauseButton, enabled = state == ScrollState.Running)
        styleActionButton(stopButton, enabled = state != ScrollState.Idle)
        styleActionButton(volumeUpButton, enabled = true)
        styleActionButton(volumeDownButton, enabled = true)
        styleActionButton(lockScreenButton, enabled = true)
        styleActionButton(speedDecreaseButton, enabled = scrollSpeed.level > ScrollSpeed.MIN_LEVEL)
        styleActionButton(speedIncreaseButton, enabled = scrollSpeed.level < ScrollSpeed.MAX_LEVEL)
        speedDecreaseButton.contentDescription = rootView.context.getString(
            R.string.overlay_speed_decrease_format,
            scrollSpeed.level,
        )
        speedIncreaseButton.contentDescription = rootView.context.getString(
            R.string.overlay_speed_increase_format,
            scrollSpeed.level,
        )
        scheduleDim()
    }

    private fun styleCollapsedButton(state: ScrollState) {
        val tint = when (state) {
            ScrollState.Idle -> 0xFFFFFFFF.toInt()
            ScrollState.Running -> 0xFFBDBDBD.toInt()
            ScrollState.Paused -> 0xFFE0E0E0.toInt()
            ScrollState.StoppedDueToEnd -> 0xFF9E9E9E.toInt()
        }
        ImageViewCompat.setImageTintList(
            collapsedPill,
            android.content.res.ColorStateList.valueOf(tint),
        )
    }

    private fun styleActionButton(button: ImageButton, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1f else 0.4f
    }

    private fun registerInteraction() {
        rootView.alpha = FULL_OPACITY
        scheduleDim()
    }

    private fun scheduleDim() {
        if (!attached) return
        rootView.removeCallbacks(dimRunnable)
        rootView.postDelayed(dimRunnable, IDLE_DIM_DELAY_MS)
    }

    private class DragTouchListener(
        private val onInteraction: () -> Unit,
        private val onMoved: (x: Int, y: Int) -> Unit,
        private val onReleased: () -> Unit,
    ) : View.OnTouchListener {
        private var initialX: Int = 0
        private var initialY: Int = 0
        private var initialTouchX: Float = 0f
        private var initialTouchY: Float = 0f

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    onInteraction()
                    val params = view.rootView.layoutParams as WindowManager.LayoutParams
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return false
                }

                MotionEvent.ACTION_MOVE -> {
                    onInteraction()
                    val nextX = initialX + (event.rawX - initialTouchX).roundToInt()
                    val nextY = initialY + (event.rawY - initialTouchY).roundToInt()
                    onMoved(nextX, nextY)
                    return true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> {
                    onInteraction()
                    onReleased()
                    return false
                }
            }
            return false
        }
    }

    companion object {
        private const val IDLE_DIM_DELAY_MS = 3_500L
        private const val IDLE_DIM_ALPHA = 0.42f
        private const val FULL_OPACITY = 1f
    }
}
