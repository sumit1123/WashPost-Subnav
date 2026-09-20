package com.wapo.flagship.features.audio.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wapo.android.commons.util.LiveEvent
import com.wapo.flagship.features.audio.ClassicAudioManager2
import com.wapo.flagship.features.audio.fragments.PlaybackSpeedDialogFragment
import com.wapo.flagship.features.audio.models.PlaybackSpeed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared [ViewModel] for managing playback speed, used by player screens and [PlaybackSpeedDialogFragment].
 */
@HiltViewModel
class PlaybackSpeedViewModel @Inject constructor(
    private val audioManager: ClassicAudioManager2,
) : ViewModel() {
    private val _selectedPlaybackSpeed = LiveEvent<PlaybackSpeed>()
    val selectedPlaybackSpeed: LiveData<PlaybackSpeed> = _selectedPlaybackSpeed

    init {
        viewModelScope.launch {
            audioManager.isPlayingAd.collect {
                syncPlaybackSpeed()
            }
        }
    }

    /**
     * Order list of playback speeds from lowest to highest.
     * Allows for incrementing/decrementing speed
     */
    private val playbackSpeedList = listOf(
        PlaybackSpeed.Slow(),
        PlaybackSpeed.Normal(),
        PlaybackSpeed.Quick(),
        PlaybackSpeed.Fast(),
        PlaybackSpeed.Faster(),
        PlaybackSpeed.Fastest()
    )

    /**
     * Selects the playback speed if it exists.
     * Used for loading speed from prefs.
     */
    fun selectPlaybackSpeed(speed: Float) {
        playbackSpeedList.find { it.speed == speed }?.let {
            _selectedPlaybackSpeed.value = it
        }
    }

    /**
     * Selects the playback speed after user selection.
     */
    fun selectPlaybackSpeed(playbackSpeed: PlaybackSpeed) {
        _selectedPlaybackSpeed.value = playbackSpeed
    }

    /**
     * Increment audio speed to next higher level
     */
    fun incrementPlaybackSpeed() {
        _selectedPlaybackSpeed.value?.let {  currentSpeed->
            val index = playbackSpeedList.indexOf(currentSpeed)
            if(index>=0 && index<playbackSpeedList.lastIndex) {
                _selectedPlaybackSpeed.value = playbackSpeedList[index+1]
            }
        }
    }

    /**
     * Decrement audio speed to next lower level
     */
    fun decrementPlaybackSpeed() {
        _selectedPlaybackSpeed.value?.let {  currentSpeed->
            val index = playbackSpeedList.indexOf(currentSpeed)
            if(index>0 && index<=playbackSpeedList.lastIndex) {
                _selectedPlaybackSpeed.value = playbackSpeedList[index-1]
            }
        }
    }

    fun syncPlaybackSpeed() {
        val playbackSpeed = audioManager.getPlaybackSpeed()
        if (playbackSpeed != selectedPlaybackSpeed.value) {
            _selectedPlaybackSpeed.value = playbackSpeed
        }
    }
}