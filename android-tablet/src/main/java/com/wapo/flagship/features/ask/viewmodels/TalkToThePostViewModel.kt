package com.wapo.flagship.features.ask.viewmodels

import android.content.Context
import android.media.AudioTrack
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.domain.repository.SearchRepo
import com.wapo.flagship.features.ask.TalkToThePostAudioManager
import com.wapo.flagship.features.ask.TalkToThePostSpeechRecManager
import com.wapo.flagship.features.ask.TalkToThePostVoiceResult
import com.wapo.flagship.features.ask.domain.SseDataType
import com.wapo.flagship.features.ask.session.AskSessionGuard
import com.wapo.flagship.features.ask.session.AskSessionOwner
import com.wapo.flagship.features.search2.events.SseEvent
import com.wapo.flagship.features.search2.events.UserEvent
import com.wapo.flagship.features.settings.ASK_SAM_JUNIPER_ID
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.tracking.Measurement
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class TalkToThePostViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val searchRepo: SearchRepo,
    private val speechRecManager: TalkToThePostSpeechRecManager,
    private val audioManager: TalkToThePostAudioManager,
) : ViewModel() {

    private var sseJob: Job? = null
    private var sseTimeoutJob: Job? = null
    private var ownsSession = AskSessionGuard.tryAcquire(AskSessionOwner.PHONE)
    private var destroyed = false
    private val _talkUiState = MutableStateFlow(TalkUiState.FIRST_LISTEN_IN_SESSION)
    val talkUiState: MutableStateFlow<TalkUiState> = _talkUiState
    val isMirroringAndroidAuto =
        !ownsSession && AskSessionGuard.isOwnedBy(AskSessionOwner.ANDROID_AUTO)
    private val _captionsEnabled = MutableStateFlow(false)
    val captionsEnabled = _captionsEnabled

    fun toggleCaptions() {
        val newState = !_captionsEnabled.value
        _captionsEnabled.value = newState
        PrefUtils.setTalkToThePostCaptionsEnabled(context, newState)
        Measurement.trackTalkToggleCaptions(newState)
        if (newState && _talkUiState.value == TalkUiState.RESPONDING) {
            _talkUiState.value = TalkUiState.RESPONDING_CAPTIONS
        } else if (!newState && _talkUiState.value == TalkUiState.RESPONDING_CAPTIONS) {
            _talkUiState.value = TalkUiState.RESPONDING
        }
    }

    private val _captionsIndex = MutableStateFlow(0)
    val captionsIndex = _captionsIndex

    fun updateCaptionsIndex(index: Int) {
        _captionsIndex.value = index
    }

    private val _captionsDoneRendering = MutableStateFlow(false)
    fun onCaptionsDoneRendering() {
        _captionsDoneRendering.value = true
    }

    private val _orbRadius = MutableStateFlow(OrbRadius.MIN_RADIUS)
    val orbRadius = _orbRadius

    var conversationId: String? = null

    var timestampAndTranscript = Pair<Float?, String?>(null, null)
    var entryPoint: String? = null
    private var turnId: String? = null

    private val _userEvent = LiveEvent<UserEvent>()
    val userEvent: LiveEvent<UserEvent> = _userEvent

    fun eventTrigger(event: UserEvent) {
        _userEvent.postValue(event)
    }

    private val _previousResponseText = MutableStateFlow("")
    val previousResponseText = _previousResponseText
    private val _responseText: MutableStateFlow<String> = MutableStateFlow("")
    val responseText: MutableStateFlow<String> = _responseText

    private val _thinkingText: MutableStateFlow<String> = MutableStateFlow("")
    val thinkingText: MutableStateFlow<String> = _thinkingText

    private val _deviceVolume: MutableStateFlow<Int> = MutableStateFlow(1)
    val deviceVolume = _deviceVolume

    fun updateDeviceVolume(newVolume: Int) {
        _deviceVolume.value = newVolume
    }

    init {
        _captionsEnabled.value = PrefUtils.getTalkToThePostCaptionsEnabled(context)
        if (isMirroringAndroidAuto) {
            _talkUiState.value = TalkUiState.ACTIVE_IN_ANDROID_AUTO
        } else {
            if (PrefUtils.getTalkToThePostVisitCount(context) == 0) {
                updateState(TalkUiState.ONBOARDING)
            } else {
                updateState(TalkUiState.FIRST_LISTEN_IN_SESSION)
            }
            observeVoiceResult()
            observeInputRms()
            observeOutputRms()
            initSseJob()
        }
    }

    private fun initSseJob() {
        sseJob = viewModelScope.launch {
            searchRepo.sseEventState.collect { state ->
                withContext(Dispatchers.Main) {
                    when (state) {
                        is SseEvent.Data -> {
                            when (state.type?.let { SseDataType.fromString(it) }) {
                                SseDataType.NEW_CONVERSATION_ID -> {
                                    conversationId = state.data
                                }
                                SseDataType.TURN_ID -> {
                                    turnId = state.data
                                }
                                SseDataType.AUDIO_REPLY -> {
                                    sseTimeoutJob?.cancel()
                                    if (_talkUiState.value == TalkUiState.THINKING) {
                                        updateState(TalkUiState.RESPONDING)
                                        Measurement.trackTalkResponded(turnId)
                                    }
                                    withContext(Dispatchers.IO) {
                                        audioManager.writeAnswerChunk(state.data)
                                    }
                                }
                                SseDataType.REPLY -> {
                                    _responseText.value += state.data
                                }
                                SseDataType.THINKING_STATE_UPDATE -> {
                                    _thinkingText.value = state.data
                                }
                                else -> {}
                            }
                        }
                        is SseEvent.Closed -> {
                            sseTimeoutJob?.cancel()
                            audioManager.setAnswerNotificationMarkerPosition()
                        }
                        is SseEvent.Error -> {
                            sseTimeoutJob?.cancel()
                            updateState(TalkUiState.API_ERROR)
                            EventLog.Builder().apply {
                                setMessage("Ask Sam SSE error")
                                setModule(LogModules.ASK_THE_POST)
                                setErrorMessage(state.toString())
                            }.run {
                                RemoteLog.e(context, build())
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun observeVoiceResult() {
        viewModelScope.launch {
            speechRecManager.voiceResult.collect { voiceResult ->
                voiceResult?.let {
                    when (it) {
                        is TalkToThePostVoiceResult.Success -> {
                            updateState(TalkUiState.THINKING)
                            startOrContinueLiveConversation(it.text, entryPoint = entryPoint)
                        }
                        is TalkToThePostVoiceResult.Failure -> {
                            if (audioManager.recognitionErrorCount < TalkToThePostSpeechRecManager.MAX_ERROR_COUNT) {
                                updateState(TalkUiState.RECOGNITION_ERROR)
                                Measurement.trackTalkSpeechRecError(turnId, false)
                            } else {
                                updateState(TalkUiState.RECOGNITION_ERROR_MAX)
                                Measurement.trackTalkSpeechRecError(turnId, true)
                            }
                        }
                    }
                    speechRecManager.clearQuestion()
                }
            }
        }
    }

    private fun observeInputRms() {
        viewModelScope.launch {
            speechRecManager.inputRms.collect { newValue ->
                _orbRadius.value = when {
                    newValue > 7 -> {
                        OrbRadius.MAX_RADIUS
                    }
                    newValue > 5 -> {
                        OrbRadius.MED_RADIUS
                    }
                    else -> {
                        OrbRadius.MIN_RADIUS
                    }
                }
            }
        }
    }

    private fun observeOutputRms() {
        viewModelScope.launch {
            audioManager.outputRms.collect { newValue ->
                _orbRadius.value = when {
                    newValue > 2000 -> {
                        OrbRadius.MAX_RADIUS
                    }
                    newValue > 1000 -> {
                        OrbRadius.MED_RADIUS
                    }
                    else -> {
                        OrbRadius.MIN_RADIUS
                    }
                }
            }
        }
    }

    fun waitForEndOfAudioPlayback(audioType: AudioType) {
        when (audioType) {
            AudioType.ONBOARDING -> {
                audioManager.messageAudioTrack.setPlaybackPositionUpdateListener(
                    object : AudioTrack.OnPlaybackPositionUpdateListener {
                        override fun onPeriodicNotification(track: AudioTrack?) {}

                        override fun onMarkerReached(track: AudioTrack?) {
                            updateState(TalkUiState.FIRST_LISTEN_IN_SESSION)
                        }
                    }
                )
            }
            AudioType.ANSWER -> {
                audioManager.answerAudioTrack.setPlaybackPositionUpdateListener(
                    object : AudioTrack.OnPlaybackPositionUpdateListener {
                        override fun onPeriodicNotification(track: AudioTrack?) {}

                        override fun onMarkerReached(track: AudioTrack?) {
                            if (_talkUiState.value == TalkUiState.RESPONDING) {
                                updateState(TalkUiState.LISTENING)
                            } else if (_talkUiState.value == TalkUiState.RESPONDING_CAPTIONS) {
                                viewModelScope.launch {
                                    withTimeoutOrNull(5000) {
                                        _captionsDoneRendering.first { it }
                                    }
                                    updateState(TalkUiState.LISTENING)
                                }
                            }
                        }
                    }
                )
            }
            AudioType.ERROR -> {
                audioManager.messageAudioTrack.setPlaybackPositionUpdateListener(
                    object : AudioTrack.OnPlaybackPositionUpdateListener {
                        override fun onPeriodicNotification(track: AudioTrack?) {}

                        override fun onMarkerReached(track: AudioTrack?) {
                            updateState(TalkUiState.LISTENING)
                        }
                    }
                )
            }
        }
    }

    fun startOrContinueLiveConversation(query: String, entryPoint: String?) {
        if (!ownsSession) return
        sseTimeoutJob?.cancel()
        if (sseJob == null || sseJob?.isActive != true) {
            initSseJob()
        }

        conversationId?.let { conversationId ->
            searchRepo.continueLiveConversation(
                conversationId = conversationId,
                query = query,
                voiceResponse = PrefUtils.getTalkToThePostVoiceSelection(context) ?: ASK_SAM_JUNIPER_ID,
                entryPoint = entryPoint
            )
            Measurement.trackTalkFollowUpQuestionAsked(turnId)
        } ?: run {
            searchRepo.startLiveConversation(
                query,
                voiceResponse = PrefUtils.getTalkToThePostVoiceSelection(context) ?: ASK_SAM_JUNIPER_ID,
                timestamp = this.timestampAndTranscript.first,
                transcriptUrl = this.timestampAndTranscript.second,
                entryPoint = entryPoint
            )
            Measurement.trackTalkFirstQuestionAsked()
        }

        // Set up timeout cancellation
        sseTimeoutJob = viewModelScope.launch {
            delay(SSE_TIMEOUT)
            sseJob?.cancel()
            updateState(TalkUiState.API_ERROR)
        }
    }

    fun initializeListening() {
        if (!ownsSession) return
        observeVoiceResult()
    }

    fun onTalkPlayPauseButtonTap() {
        if (!ownsSession) return
        when (_talkUiState.value) {
            TalkUiState.FIRST_LISTEN_IN_SESSION,
            TalkUiState.LISTENING -> {
                updateState(TalkUiState.LISTENING_ON_HOLD)
            }
            TalkUiState.LISTENING_ON_HOLD,
            TalkUiState.THINKING -> {
                updateState(TalkUiState.LISTENING)
            }
            TalkUiState.ONBOARDING,
            TalkUiState.RESPONDING,
            TalkUiState.RESPONDING_CAPTIONS,
            TalkUiState.RECOGNITION_ERROR,
            TalkUiState.RECOGNITION_ERROR_MAX,
            TalkUiState.API_ERROR -> {
                updateState(TalkUiState.RESPONSE_PAUSED_LISTENING)
            }
            TalkUiState.RESPONSE_PAUSED_LISTENING -> {
                updateState(TalkUiState.RESPONDING)
            }
            TalkUiState.ACTIVE_IN_ANDROID_AUTO -> Unit
        }
    }

    fun updateState(newState: TalkUiState) {
        if (!ownsSession && newState != TalkUiState.ACTIVE_IN_ANDROID_AUTO) return
        Logger.d(TAG, "New state: $newState")
        _talkUiState.value = newState
        _orbRadius.value = OrbRadius.MED_RADIUS
        when (newState) {
            TalkUiState.ONBOARDING -> {
                audioManager.playMessageAudio()
                viewModelScope.launch(Dispatchers.IO) {
                    audioManager.writeOnboardingMessageToAudioTrack()
                }
                waitForEndOfAudioPlayback(AudioType.ONBOARDING)
            }
            TalkUiState.FIRST_LISTEN_IN_SESSION -> {
                audioManager.answerAudioTrack.pause()
                audioManager.messageAudioTrack.pause()
                speechRecManager.startListening()
            }
            TalkUiState.LISTENING_ON_HOLD -> {
                speechRecManager.stopListening()
                audioManager.resetMessageTrack()
                audioManager.resetAnswerTrack()
            }
            TalkUiState.RESPONSE_PAUSED_LISTENING -> {
                audioManager.answerAudioTrack.pause()
                audioManager.messageAudioTrack.pause()
                speechRecManager.startListening()
            }
            TalkUiState.LISTENING -> {
                audioManager.answerAudioTrack.pause()
                audioManager.messageAudioTrack.pause()
                speechRecManager.startListening()
            }
            TalkUiState.RECOGNITION_ERROR -> {
                speechRecManager.stopListening()
                audioManager.resetMessageTrack()
                audioManager.playMessageAudio()
                viewModelScope.launch(Dispatchers.IO) {
                    audioManager.writeErrorMessageToAudioTrack(ErrorType.RECOGNITION_ERROR)
                }
                waitForEndOfAudioPlayback(AudioType.ERROR)
            }
            TalkUiState.RECOGNITION_ERROR_MAX -> {
                speechRecManager.stopListening()
                audioManager.resetMessageTrack()
                audioManager.playMessageAudio()
                viewModelScope.launch(Dispatchers.IO) {
                    audioManager.writeErrorMessageToAudioTrack(ErrorType.RECOGNITION_ERROR_MAX)
                }
                waitForEndOfAudioPlayback(AudioType.ERROR)
            }
            TalkUiState.API_ERROR -> {
                speechRecManager.stopListening()
                audioManager.resetMessageTrack()
                audioManager.playMessageAudio()
                viewModelScope.launch(Dispatchers.IO) {
                    audioManager.writeErrorMessageToAudioTrack(ErrorType.API_ERROR)
                }
                waitForEndOfAudioPlayback(AudioType.ERROR)
            }
            TalkUiState.THINKING -> {
                _thinkingText.value = ""
                audioManager.resetAnswerTrack()
                if (responseText.value.isNotEmpty()) {
                    _previousResponseText.value += _responseText.value + "\n"
                    _responseText.value = ""
                    _captionsIndex.value = 0
                }
            }
            TalkUiState.RESPONDING -> {
                speechRecManager.stopListening()
                audioManager.recognitionErrorCount = 0
                audioManager.playAnswerAudio()
                waitForEndOfAudioPlayback(AudioType.ANSWER)
                if (_captionsEnabled.value) {
                    _talkUiState.value = TalkUiState.RESPONDING_CAPTIONS
                }
            }
            TalkUiState.RESPONDING_CAPTIONS -> {
                _captionsDoneRendering.value = false
            }
            TalkUiState.ACTIVE_IN_ANDROID_AUTO -> Unit
        }
    }

    fun requestAudioFocus() {
        if (!ownsSession) return
        audioManager.requestAudioFocus()
    }

    fun abandonAudioFocus() {
        if (!ownsSession) return
        audioManager.abandonAudioFocus()
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        if (!ownsSession) return
        speechRecManager.stopListening()
        speechRecManager.destroyRecognizer()
        audioManager.abandonAudioFocus()
        audioManager.releaseAudioPlayers()
        sseJob?.cancel()
        sseTimeoutJob?.cancel()
        viewModelScope.launch {
            try {
                searchRepo.endLiveConversation()
            } finally {
                AskSessionGuard.release(AskSessionOwner.PHONE)
                ownsSession = false
            }
        }
    }

    enum class TalkUiState {
        ONBOARDING,
        FIRST_LISTEN_IN_SESSION,
        LISTENING_ON_HOLD,
        THINKING,
        RESPONDING,
        RESPONDING_CAPTIONS,
        RECOGNITION_ERROR,
        RECOGNITION_ERROR_MAX,
        API_ERROR,
        RESPONSE_PAUSED_LISTENING,
        LISTENING,
        ACTIVE_IN_ANDROID_AUTO,
    }

    enum class AudioType {
        ONBOARDING,
        ANSWER,
        ERROR
    }

    enum class ErrorType {
        RECOGNITION_ERROR,
        RECOGNITION_ERROR_MAX,
        API_ERROR
    }

    enum class OrbRadius {
        MIN_RADIUS,
        MED_RADIUS,
        MAX_RADIUS
    }

    companion object {
        val TAG = TalkToThePostViewModel::class.simpleName
        const val SSE_TIMEOUT = 10_000L // 10 seconds
    }
}
