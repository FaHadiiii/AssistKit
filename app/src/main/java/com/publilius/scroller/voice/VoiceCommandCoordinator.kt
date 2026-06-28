package com.publilius.scroller.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.publilius.scroller.controller.ScrollController
import com.publilius.scroller.model.ScrollState
import com.publilius.scroller.model.VoiceStatus
import java.util.Locale

class VoiceCommandCoordinator(
    context: Context,
    private val onPause: () -> Unit,
    private val onStart: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val processor = VoiceCommandProcessor(
        onPause = onPause,
        onStart = onStart,
    )
    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var currentState: ScrollState = ScrollState.Idle
    private var voiceCommandsEnabled = true
    private var destroyed = false
    private var sessionActive = false
    private var consecutiveErrors = 0

    private val restartListeningRunnable = Runnable {
        if (!destroyed) {
            evaluateListening()
        }
    }

    fun start() {
        if (destroyed) return
        evaluateListening()
    }

    fun onScrollStateChanged(state: ScrollState) {
        if (currentState != state) {
            consecutiveErrors = 0
            processor.resetDebounce()
        }
        currentState = state
        evaluateListening()
    }

    fun refreshPermissionState() {
        evaluateListening()
    }

    fun onVoiceCommandsEnabledChanged(enabled: Boolean) {
        voiceCommandsEnabled = enabled
        if (enabled) {
            consecutiveErrors = 0
        }
        evaluateListening()
    }

    fun stop() {
        destroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        stopListeningSession()
        speechRecognizer?.destroy()
        speechRecognizer = null
        ScrollController.setVoiceStatus(VoiceStatus.Inactive)
    }

    private fun evaluateListening() {
        val recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(appContext)
        val hasMicrophonePermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        val blockedByErrors = consecutiveErrors >= MAX_CONSECUTIVE_ERRORS
        val nextStatus = VoiceControlPolicy.statusFor(
            state = currentState,
            voiceCommandsEnabled = voiceCommandsEnabled,
            hasMicrophonePermission = hasMicrophonePermission,
            recognitionAvailable = recognitionAvailable,
            blockedByErrors = blockedByErrors,
        )

        ScrollController.setVoiceStatus(nextStatus)
        if (nextStatus == VoiceStatus.ListeningForPause || nextStatus == VoiceStatus.ListeningForStart) {
            startListeningSession()
        } else {
            stopListeningSession()
        }
    }

    private fun startListeningSession() {
        if (sessionActive || destroyed) {
            return
        }
        val recognizer = ensureSpeechRecognizer() ?: return
        mainHandler.removeCallbacks(restartListeningRunnable)
        sessionActive = true
        try {
            recognizer.startListening(recognizerIntent)
        } catch (_: SecurityException) {
            sessionActive = false
            ScrollController.setVoiceStatus(VoiceStatus.PermissionRequired)
        } catch (_: IllegalStateException) {
            sessionActive = false
            scheduleRetry()
        }
    }

    private fun stopListeningSession() {
        mainHandler.removeCallbacks(restartListeningRunnable)
        if (!sessionActive) {
            return
        }
        sessionActive = false
        runCatching { speechRecognizer?.cancel() }
    }

    private fun ensureSpeechRecognizer(): SpeechRecognizer? {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            return null
        }
        val existing = speechRecognizer
        if (existing != null) {
            return existing
        }
        return SpeechRecognizer.createSpeechRecognizer(appContext).also { recognizer ->
            recognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit

                    override fun onBeginningOfSpeech() = Unit

                    override fun onRmsChanged(rmsdB: Float) = Unit

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() = Unit

                    override fun onError(error: Int) {
                        sessionActive = false
                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            evaluateListening()
                            return
                        }
                        consecutiveErrors += 1
                        if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                            ScrollController.setVoiceStatus(VoiceStatus.Error)
                            return
                        }
                        scheduleRetry()
                    }

                    override fun onResults(results: Bundle?) {
                        sessionActive = false
                        consecutiveErrors = 0
                        processor.process(currentState, extractTranscripts(results))
                        currentState = ScrollController.state.value
                        scheduleRetry()
                    }

                    override fun onPartialResults(partialResults: Bundle?) = Unit

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                },
            )
        }.also {
            speechRecognizer = it
        }
    }

    private fun scheduleRetry() {
        mainHandler.removeCallbacks(restartListeningRunnable)
        if (!destroyed && VoiceControlPolicy.shouldListen(currentState)) {
            mainHandler.postDelayed(restartListeningRunnable, RETRY_DELAY_MS)
        }
    }

    private fun extractTranscripts(results: Bundle?): List<String> {
        return results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.filterNotNull()
            .orEmpty()
    }

    companion object {
        private const val MAX_CONSECUTIVE_ERRORS = 3
        private const val RETRY_DELAY_MS = 350L
    }
}
