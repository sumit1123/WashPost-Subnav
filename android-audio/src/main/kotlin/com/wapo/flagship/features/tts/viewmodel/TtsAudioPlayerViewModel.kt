/* Copyright (c) 2026 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.tts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.flagship.features.tts.domain.ObserveTtsEventsUseCase
import com.wapo.flagship.features.tts.domain.ObserveTtsStateUseCase
import com.wapo.flagship.features.tts.domain.OnTtsEvent
import com.wapo.flagship.features.tts.domain.ProvideExternalTtsRepo
import com.wapo.flagship.features.tts.domain.TtsRepository
import com.wapo.flagship.features.tts.domain.TtsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TtsAudioPlayerViewModel @Inject constructor(
    private val ttsRepository: TtsRepository,
    private val observeTtsStateUseCase: ObserveTtsStateUseCase,
    private val observeTtsEventsUseCase: ObserveTtsEventsUseCase,
    private val provideExternalTtsRepo: ProvideExternalTtsRepo
) : ViewModel() {

    val ttsState: StateFlow<TtsState> = observeTtsStateUseCase()
        .stateIn(viewModelScope,
            SharingStarted.WhileSubscribed(5000), TtsState())

    val ttsEvents: StateFlow<OnTtsEvent> = observeTtsEventsUseCase()
        .stateIn(viewModelScope,
            SharingStarted.WhileSubscribed(5000), OnTtsEvent.None)

    private var ttsJob: Job? = null

    fun playTts(textToSpeak: List<String>, title: String?) {
        ttsJob?.cancel()

        ttsJob = viewModelScope.launch {
            ttsRepository.speak(textToSpeak, title)
        }
    }

    fun playSource(path: String) {
        viewModelScope.launch {
            try {
                val ttsValue = provideExternalTtsRepo.getExternalTts(path)
                playTts(textToSpeak = ttsValue.ttsList, title = ttsValue.title)
            } catch (e: Exception) {
                ttsRepository.error(e.message ?: "Error getting External Tts")
            }
        }
    }

    fun stopTts() {
        ttsRepository.stop()
    }

    fun pauseTts() {
        ttsRepository.pause()
    }

    fun resumeTts() {
        ttsRepository.resume()
    }

    override fun onCleared() {
        ttsJob?.cancel()
        ttsRepository.stop()
        super.onCleared()
    }
}
