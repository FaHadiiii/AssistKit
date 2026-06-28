package com.publilius.scroller.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context.AUDIO_SERVICE
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.publilius.scroller.R
import com.publilius.scroller.controller.ScrollCommand
import com.publilius.scroller.controller.ScrollController
import com.publilius.scroller.data.AppContainer
import com.publilius.scroller.data.SettingsRepository
import com.publilius.scroller.model.ScrollState
import com.publilius.scroller.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OverlayService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var overlayManager: OverlayManager
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()
        settingsRepository = AppContainer.settingsRepository(applicationContext)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        overlayManager = OverlayManager(
            context = this,
            onPersistPosition = { position ->
                serviceScope.launch(Dispatchers.IO) {
                    settingsRepository.setOverlayPosition(position)
                }
            },
            onSpeedChanged = { speed ->
                ScrollController.setScrollSpeed(speed)
                serviceScope.launch(Dispatchers.IO) {
                    settingsRepository.setScrollSpeed(speed)
                }
            },
            onVolumeUp = { ScrollController.volumeUp() },
            onVolumeDown = { ScrollController.volumeDown() },
            onLockScreen = { ScrollController.lockScreen() },
        )

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(ScrollController.state.value))
        ScrollController.setOverlayVisible(true)

        serviceScope.launch(Dispatchers.IO) {
            ScrollController.syncSavedSpeed(settingsRepository.scrollSpeed.first())
            val position = settingsRepository.getOverlayPosition()
            launch(Dispatchers.Main) {
                overlayManager.show(position)
            }
        }

        serviceScope.launch {
            ScrollController.commands.collectLatest { command ->
                when (command) {
                    ScrollCommand.VolumeUp -> {
                        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    }

                    ScrollCommand.VolumeDown -> {
                        audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    }

                    else -> Unit
                }
            }
        }
        serviceScope.launch {
            ScrollController.state.collectLatest { state ->
                overlayManager.render(
                    state = state,
                    scrollSpeed = ScrollController.scrollSpeed.value,
                    expanded = ScrollController.overlayExpanded.value,
                )
                updateNotification(state)
            }
        }
        serviceScope.launch {
            ScrollController.scrollSpeed.collectLatest { speed ->
                overlayManager.render(
                    state = ScrollController.state.value,
                    scrollSpeed = speed,
                    expanded = ScrollController.overlayExpanded.value,
                )
            }
        }
        serviceScope.launch {
            ScrollController.overlayExpanded.collectLatest { expanded ->
                overlayManager.render(
                    state = ScrollController.state.value,
                    scrollSpeed = ScrollController.scrollSpeed.value,
                    expanded = expanded,
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCROLLING -> ScrollController.start()
            ACTION_PAUSE_SCROLLING -> ScrollController.pause()
            ACTION_STOP_SCROLLING -> ScrollController.stop()
            ACTION_STOP_SERVICE -> {
                ScrollController.stop()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlayManager.hide()
        ScrollController.setOverlayVisible(false)
        ScrollController.setOverlayExpanded(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification(state: ScrollState) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: ScrollState): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(
                when (state) {
                    ScrollState.Idle -> getString(R.string.notification_text_idle)
                    ScrollState.Running -> getString(R.string.notification_text_running)
                    ScrollState.Paused -> getString(R.string.notification_text_paused)
                    ScrollState.StoppedDueToEnd -> getString(R.string.notification_text_end)
                },
            )
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(
                0,
                getString(R.string.notification_start),
                pendingServiceIntent(ACTION_START_SCROLLING, 11),
            )
            .addAction(
                0,
                getString(R.string.notification_pause),
                pendingServiceIntent(ACTION_PAUSE_SCROLLING, 12),
            )
            .addAction(
                0,
                getString(R.string.notification_stop),
                pendingServiceIntent(ACTION_STOP_SCROLLING, 13),
            )
            .addAction(
                0,
                getString(R.string.notification_close),
                pendingServiceIntent(ACTION_STOP_SERVICE, 14),
            )
            .build()
    }

    private fun pendingServiceIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, OverlayService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_title),
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "auto_scroller_overlay"
        private const val NOTIFICATION_ID = 401

        const val ACTION_START_SERVICE = "com.publilius.scroller.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.publilius.scroller.action.STOP_SERVICE"
        const val ACTION_START_SCROLLING = "com.publilius.scroller.action.START_SCROLLING"
        const val ACTION_PAUSE_SCROLLING = "com.publilius.scroller.action.PAUSE_SCROLLING"
        const val ACTION_STOP_SCROLLING = "com.publilius.scroller.action.STOP_SCROLLING"

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
