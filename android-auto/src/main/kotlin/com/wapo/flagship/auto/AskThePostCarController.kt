package com.wapo.flagship.auto

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.car.app.CarContext
import com.wapo.flagship.auto.ask.AskErrorAudioProvider
import com.wapo.flagship.auto.ask.AskErrorAudioType
import com.wapo.flagship.auto.ask.AskSpeechRecognizer
import com.wapo.flagship.auto.ask.AskSpeechResult
import com.wapo.flagship.auto.ask.AskThePostEvent
import com.wapo.flagship.auto.ask.AskThePostGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
internal class AskThePostCarController(
    carContext: CarContext,
    private val askThePostGateway: AskThePostGateway,
    private val speechRecognizer: AskSpeechRecognizer,
    private val errorAudioProvider: AskErrorAudioProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(AskThePostCarState.IDLE)
    val state: StateFlow<AskThePostCarState> = _state.asStateFlow()

    private val audioFocusManager =
        AskThePostAudioFocusManager(
            context = carContext,
            onFocusChanged = ::handleAudioFocusChange,
        )
    private val carMicrophone =
        AskThePostCarMicrophone(
            carContext = carContext,
            onDismissed = {
                scope.launch {
                    if (_state.value == AskThePostCarState.LISTENING) end()
                }
            },
        )

    private val audioStreamer =
        AskThePostAudioStreamer(
            onPlaybackStarted = {
                // if ask the post has an answer to play, and UI is in thinking state then transition to answering state
                if (playbackPurpose == PlaybackPurpose.ANSWER && _state.value == AskThePostCarState.THINKING) {
                    recognitionErrorCount = 0
                    transitionTo(AskThePostCarState.ANSWERING)
                }
            },
            onPlaybackComplete = {
                when (playbackPurpose) {
                    PlaybackPurpose.RETRY_ERROR -> {
                        playbackPurpose = PlaybackPurpose.NONE
                        if (_state.value == AskThePostCarState.PAUSED) {
                            stateBeforePause = AskThePostCarState.LISTENING
                        } else {
                            startListening()
                        }
                    }
                    PlaybackPurpose.ERROR -> {
                        playbackPurpose = PlaybackPurpose.NONE
                        transitionTo(AskThePostCarState.ERROR)
                    }
                    PlaybackPurpose.ANSWER -> {
                        playbackPurpose = PlaybackPurpose.NONE
                        when (_state.value) {
                            AskThePostCarState.ANSWERING -> startListening()
                            AskThePostCarState.PAUSED -> stateBeforePause = AskThePostCarState.LISTENING
                            else -> Unit
                        }
                    }
                    PlaybackPurpose.NONE -> Unit
                }
            },
        )

    private var conversationId: String? = null
    private var ownsSession = false
    private var stateBeforePause = AskThePostCarState.IDLE
    private var started = false
    private var ended = false
    private var receivedAudio = false
    private var outputPausedForFocusLoss = false
    private var playbackPurpose = PlaybackPurpose.NONE
    private var errorPlaybackJob: Job? = null
    private var sessionReleaseJob: Job? = null
    private var recognitionErrorCount = 0

    init {
        observeRecognition()
        observeConversationEvents()
    }

    fun start() {
        if (started || ended) return
        if (!acquireSession()) return
        recognitionErrorCount = 0
        started = true
        startListening()
    }

    fun startWithQuestion(question: String) {
        val trimmedQuestion = question.trim()
        if (started || ended || trimmedQuestion.isEmpty()) return
        if (!acquireSession()) return
        recognitionErrorCount = 0
        started = true
        submitQuestion(trimmedQuestion)
    }

    fun togglePause() {
        if (ended) return
        if (_state.value == AskThePostCarState.PAUSED) {
            if (!audioFocusManager.requestFocus()) {
                enterErrorState()
                return
            }
            when (stateBeforePause) {
                AskThePostCarState.LISTENING -> {
                    transitionTo(AskThePostCarState.LISTENING)
                    startListening()
                }
                AskThePostCarState.ANSWERING -> {
                    transitionTo(AskThePostCarState.ANSWERING)
                    audioStreamer.resume()
                }
                else -> {
                    transitionTo(stateBeforePause)
                    audioStreamer.resume()
                }
            }
        } else {
            stateBeforePause = _state.value
            stopInputIfListening(stateBeforePause)
            audioStreamer.pause()
            transitionTo(AskThePostCarState.PAUSED)
        }
    }

    fun pause() {
        if (ended || _state.value in focusReleasedStates) return
        stateBeforePause = _state.value
        stopInputIfListening(stateBeforePause)
        audioStreamer.pause()
        transitionTo(AskThePostCarState.PAUSED)
    }

    fun retry() {
        if (ended || !started) return
        recognitionErrorCount = 0
        startListening()
    }

    fun end() {
        if (ended || !started) return
        stopInputIfListening(_state.value)
        errorPlaybackJob?.cancel()
        errorPlaybackJob = null
        playbackPurpose = PlaybackPurpose.NONE
        audioStreamer.stop()
        conversationId = null
        receivedAudio = false
        recognitionErrorCount = 0
        started = false
        transitionTo(AskThePostCarState.IDLE)
        endOwnedSession()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun destroy() {
        if (ended) return
        ended = true
        errorPlaybackJob?.cancel()
        speechRecognizer.stopListening()
        carMicrophone.release()
        speechRecognizer.destroy()
        audioStreamer.release()
        audioFocusManager.abandonFocus()
        if (ownsSession) {
            ownsSession = false
            sessionReleaseJob = scope.launch {
                try {
                    askThePostGateway.endConversation()
                } finally {
                    sessionReleaseJob = null
                    scope.cancel()
                }
            }
        } else if (sessionReleaseJob != null) {
            sessionReleaseJob?.invokeOnCompletion { scope.cancel() }
        } else {
            scope.cancel()
        }
    }

    private fun acquireSession(): Boolean {
        if (ownsSession) return true
        ownsSession = askThePostGateway.tryAcquireSession()
        return ownsSession
    }

    private fun endOwnedSession() {
        if (!ownsSession) return
        ownsSession = false
        sessionReleaseJob = scope.launch {
            try {
                askThePostGateway.endConversation()
            } finally {
                sessionReleaseJob = null
            }
        }
    }

    private fun observeRecognition() {
        scope.launch {
            speechRecognizer.results.collect { result ->
                if (_state.value != AskThePostCarState.LISTENING) return@collect
                carMicrophone.stop()
                when (result) {
                    is AskSpeechResult.Success -> {
                        if (result.text.isBlank()) {
                            enterRecognitionErrorState()
                        } else {
                            submitQuestion(result.text)
                        }
                    }
                    AskSpeechResult.Failure -> enterRecognitionErrorState()
                }
                speechRecognizer.clearResult()
            }
        }
    }

    private fun observeConversationEvents() {
        scope.launch {
            askThePostGateway.events.collect { event ->
                when (event) {
                    is AskThePostEvent.ConversationStarted -> {
                        if (started) conversationId = event.conversationId
                    }
                    is AskThePostEvent.AudioChunk -> {
                        if (started && _state.value in responseStates) {
                            if (_state.value != AskThePostCarState.PAUSED && !requestAudioFocus()) {
                                enterErrorState()
                                return@collect
                            }
                            if (!audioFocusManager.hasFocus) {
                                outputPausedForFocusLoss = true
                                audioStreamer.pause()
                            }
                            receivedAudio = true
                            audioStreamer.appendBase64Chunk(event.base64Audio)
                        }
                    }
                    AskThePostEvent.Closed -> {
                        if (started && _state.value in responseStates) {
                            audioStreamer.finishAnswer()
                            if (!receivedAudio) {
                                if (_state.value == AskThePostCarState.PAUSED) {
                                    stateBeforePause = AskThePostCarState.LISTENING
                                } else {
                                    startListening()
                                }
                            }
                        }
                    }
                    AskThePostEvent.Error -> if (started) enterErrorState(AskErrorAudioType.API)
                }
            }
        }
    }

    private fun submitQuestion(question: String) {
        if (!requestAudioFocus()) {
            enterErrorState()
            return
        }
        transitionTo(AskThePostCarState.THINKING)
        receivedAudio = false
        playbackPurpose = PlaybackPurpose.ANSWER
        audioStreamer.beginAnswer()
        conversationId?.let {
            askThePostGateway.continueConversation(
                conversationId = it,
                question = question,
            )
        } ?: askThePostGateway.startConversation(question)
    }

    private fun startListening() {
        if (!started || ended || _state.value == AskThePostCarState.PAUSED) return
        errorPlaybackJob?.cancel()
        errorPlaybackJob = null
        playbackPurpose = PlaybackPurpose.NONE
        if (!requestAudioFocus()) {
            enterErrorState()
            return
        }
        transitionTo(AskThePostCarState.LISTENING)
        runCatching {
            val audioSource = carMicrophone.start()
            speechRecognizer.startListening(
                silenceTimeoutMillis = SILENCE_TIMEOUT_MILLIS,
                initialSilenceTimeoutMillis = INITIAL_SILENCE_TIMEOUT_MILLIS,
                audioSource = audioSource,
            )
        }.onFailure {
            carMicrophone.stop()
            enterRecognitionErrorState()
        }
    }

    private fun requestAudioFocus(): Boolean {
        return audioFocusManager.requestFocus()
    }

    private fun stopInputIfListening(state: AskThePostCarState) {
        if (state != AskThePostCarState.LISTENING) return
        speechRecognizer.stopListening()
        carMicrophone.stop()
    }

    private fun handleAudioFocusChange(change: AskThePostAudioFocusChange) {
        if (ended || _state.value in focusReleasedStates) return
        when (change) {
            AskThePostAudioFocusChange.GAINED -> {
                if (_state.value in answerInProgressStates && outputPausedForFocusLoss) {
                    outputPausedForFocusLoss = false
                    audioStreamer.resume()
                }
            }

            AskThePostAudioFocusChange.LOST_TRANSIENTLY -> {
                // SpeechRecognizer may temporarily own focus while the microphone is open.
                // Keep the conversation active and reacquire focus before thinking/playback.
                if (_state.value == AskThePostCarState.ANSWERING) {
                    outputPausedForFocusLoss = true
                    audioStreamer.pause()
                }
            }

            AskThePostAudioFocusChange.LOST -> enterErrorState()
        }
    }

    private fun enterRecognitionErrorState() {
        val shouldRetry = recognitionErrorCount < MAX_RECOGNITION_RETRIES
        val errorAudioType =
            if (shouldRetry) {
                recognitionErrorCount++
                AskErrorAudioType.RECOGNITION
            } else {
                AskErrorAudioType.RECOGNITION_MAX
            }
        enterErrorState(
            errorAudioType = errorAudioType,
            resumeListeningAfterAudio = shouldRetry,
        )
    }

    private fun enterErrorState(
        errorAudioType: AskErrorAudioType? = null,
        resumeListeningAfterAudio: Boolean = false,
    ) {
        speechRecognizer.stopListening()
        carMicrophone.stop()
        errorPlaybackJob?.cancel()
        playbackPurpose = PlaybackPurpose.NONE
        audioStreamer.stop()

        if (errorAudioType == null || !requestAudioFocus()) {
            transitionTo(AskThePostCarState.ERROR)
            return
        }

        val speakingState =
            when (errorAudioType) {
                AskErrorAudioType.RECOGNITION,
                AskErrorAudioType.RECOGNITION_MAX,
                -> AskThePostCarState.RECOGNITION_ERROR_SPEAKING
                AskErrorAudioType.API -> AskThePostCarState.ERROR_SPEAKING
            }
        transitionTo(speakingState)
        errorPlaybackJob =
            scope.launch {
                val audio = runCatching { errorAudioProvider.loadErrorAudio(errorAudioType) }.getOrNull()
                if (!started || ended || _state.value != speakingState) return@launch
                if (audio == null || audio.isEmpty()) {
                    transitionTo(AskThePostCarState.ERROR)
                    return@launch
                }
                playbackPurpose =
                    if (resumeListeningAfterAudio) {
                        PlaybackPurpose.RETRY_ERROR
                    } else {
                        PlaybackPurpose.ERROR
                    }
                audioStreamer.beginAnswer()
                audioStreamer.appendPcmChunk(audio)
                audioStreamer.finishAnswer()
            }
    }

    private fun transitionTo(newState: AskThePostCarState) {
        _state.value = newState
        if (newState in focusReleasedStates) {
            outputPausedForFocusLoss = false
            audioFocusManager.abandonFocus()
        }
    }

    private companion object {
        const val SILENCE_TIMEOUT_MILLIS = 1_250L
        const val INITIAL_SILENCE_TIMEOUT_MILLIS = 6_000L
        const val MAX_RECOGNITION_RETRIES = 3
        val focusReleasedStates =
            setOf(
                AskThePostCarState.IDLE,
                AskThePostCarState.PAUSED,
                AskThePostCarState.ERROR,
            )
        val responseStates =
            setOf(
                AskThePostCarState.THINKING,
                AskThePostCarState.ANSWERING,
                AskThePostCarState.PAUSED,
            )
        val answerInProgressStates =
            setOf(
                AskThePostCarState.THINKING,
                AskThePostCarState.ANSWERING,
            )
    }

    private enum class PlaybackPurpose {
        NONE,
        ANSWER,
        RETRY_ERROR,
        ERROR,
    }
}

internal enum class AskThePostCarState {
    IDLE,
    LISTENING,
    THINKING,
    ANSWERING,
    PAUSED,
    RECOGNITION_ERROR_SPEAKING,
    ERROR_SPEAKING,
    ERROR,
}
