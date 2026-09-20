package com.wapo.flagship.features.ask.viewmodels

import androidx.lifecycle.ViewModel
import com.wapo.flagship.features.ask.TalkToThePostAudioManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TalkToThePostVoiceSelectionViewModel @Inject constructor(
    private val audioManager: TalkToThePostAudioManager
) : ViewModel() {
    fun playVoiceSelection(id: String) {
        audioManager.playVoiceSelectionMessage(id)
    }

    fun destroy() {
        audioManager.releaseAudioPlayers()
    }
}
