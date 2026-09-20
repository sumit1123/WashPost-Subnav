/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.data

import android.content.Context
import android.content.Intent
import android.os.Build
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.domain.repository.RemoteLogRepo
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.tts.domain.OnTtsEvent
import com.wapo.flagship.features.tts.domain.TtsRepository
import com.wapo.flagship.features.tts.domain.TtsState
import com.wapo.flagship.features.tts.service.TtsForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsRepositoryImpl @Inject constructor(
    private val applicationContext: Context,
    private val ttsEngineSource: TtsEngineSource,
    private val remoteLogRepo: RemoteLogRepo
) : TtsRepository {

    private val _ttsState = MutableStateFlow(TtsState())

    private val _ttsEvent = MutableStateFlow<OnTtsEvent>(OnTtsEvent.None)
    private val repositoryScope = CoroutineScope(Dispatchers.Default + Job())
    private var ttsJob: Job? = null

    private var currentTitle: String? = null

    private val queue: ArrayDeque<String> = ArrayDeque()
    private var total: Int = 0
    private var progress = 0f

    init {
        ttsEngineSource.ttsEvents
            .onEach { event -> handleTtsEvent(event) }
            .launchIn(repositoryScope)
    }

    private fun handleTtsEvent(event: TtsEvent) {
        when (event) {
            is TtsEvent.OnStart -> {
                _ttsState.update {
                    it.copy(
                        isSpeaking = true,
                        isPause = false,
                        isInitializing = false,
                        currentUtteranceId = event.utteranceId,
                        error = null
                    )
                }
            }
            is TtsEvent.OnDone -> {
                queue.removeFirstOrNull()
                updateProgressTracker()
                if (queue.isEmpty()) {
                    _ttsState.update {
                        it.copy(isSpeaking = false, isPause = false, progress = 0f, currentUtteranceId = null)
                    }
                    removeNotification()
                } else {
                    playNext()
                }
            }
            is TtsEvent.OnError -> {
                clearData()
                _ttsState.update {
                    it.copy(isSpeaking = false, isPause = false, error = event.error, currentUtteranceId = null)
                }
                setError(event.error)
                removeNotification()
            }
            is TtsEvent.OnProgress -> {}
        }
    }

    override fun speak(textToSpeak: List<String>, title: String?) {
        ttsJob?.cancel()
        clearData()

        if (!textToSpeak.isEmpty()) {
            currentTitle = title
            updateNotification(title)

            ttsJob = repositoryScope.launch {
                try {
                    for (chunk in textToSpeak) {
                        queue.add(chunk)
                    }

                    total = textToSpeak.size
                    playNext()
                } catch (e: Exception) {
                    _ttsState.update { it.copy(error = e.message, isSpeaking = false, isPause = false, isInitializing = false) }
                }
            }
        } else {
            _ttsState.update {
                it.copy(isSpeaking = false, isPause = false, progress = 0f, currentUtteranceId = null)
            }
        }
    }

    private fun playNext() {
        if (ttsJob?.isCancelled == true || queue.isEmpty()) return
        val text = queue.first()
        ttsEngineSource.speak(text, text.hashCode().toString())
        updateNotification(currentTitle)
    }

    override fun pause() {
        ttsEngineSource.stop()
        _ttsState.update { it.copy(isSpeaking = false, isPause = true) }
        removeNotification()
    }

    override fun resume() {
        if (queue.isNotEmpty()) {
            playNext()
            _ttsState.update { it.copy(isSpeaking = true, isPause = false) }
        }
    }

    override fun error(error: String) {
        _ttsState.update { it.copy(error = error) }
        setError(error)
        removeNotification()
    }

    private fun setError(error: String) {
        EventLog
            .Builder()
            .apply {
                setErrorMessage(TTS_ENGINE_SOURCE_ERROR)
                setModule(LogModules.AUDIO)
                setErrorMessage(error)
            }.run {
                remoteLogRepo.w(build())
            }
        _ttsEvent.update {
            val isNetworkError = !AppContextUtils.isConnectingOrConnected()
            val errorTextId = if (isNetworkError) R.string.audio_error_offline else R.string.audio_error_other
            OnTtsEvent.OnTtsError(errorTextId)
        }
    }

    override fun stop() {
        clearData()
        ttsJob?.cancel()
        ttsEngineSource.stop()
        _ttsState.update {
            it.copy(
                isSpeaking = false,
                isPause = false,
                isInitializing = false,
                progress = 0f,
                currentUtteranceId = null,
                error = null
            )
        }
        removeNotification()
    }

    private fun updateNotification(title: String?) {
        val serviceIntent = Intent(applicationContext, TtsForegroundService::class.java).apply {
            putExtra(TtsForegroundService.TITLE_EXTRA, title)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(serviceIntent)
        } else {
            applicationContext.startService(serviceIntent)
        }
    }

    private fun removeNotification() {
        val serviceIntent = Intent(applicationContext, TtsForegroundService::class.java)
        applicationContext.stopService(serviceIntent)
        _ttsEvent.update { OnTtsEvent.None }
    }

    override fun observeTtsState(): StateFlow<TtsState> = _ttsState.asStateFlow()

    override fun observeTtsEvent(): StateFlow<OnTtsEvent> = _ttsEvent.asStateFlow()

    override fun shutdown() {
        onCleared()
    }

    private fun updateProgressTracker() {
        val segment = 1f / total
        progress += segment
        val progressValue =  progress.coerceAtMost(1f)
        _ttsState.update { it.copy(progress = progressValue) }
        updateNotification(currentTitle)
    }

    private fun clearData() {
        queue.clear()
        total = 0
        progress = 0f
    }

    fun onCleared() {
        stop()
        clearData()
        _ttsEvent.update { OnTtsEvent.None }
        ttsJob?.cancel()
        repositoryScope.coroutineContext[Job]?.cancel()
        removeNotification()
    }

    companion object {
        const val TTS_ENGINE_SOURCE_ERROR = "TtsEngineSource Error"
    }
}
