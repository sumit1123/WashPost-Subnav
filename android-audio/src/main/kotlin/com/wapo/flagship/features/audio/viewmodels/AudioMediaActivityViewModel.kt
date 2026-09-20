/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.audio.viewmodels

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerControlView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.AudioTrackerEvent
import com.wapo.flagship.features.audio.ClassicAudioManager2
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.ads.mappers.AudioAdAppConfigMapper.getConfig
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.AudioMediaConfigList
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.fragments.AudioAdUiState
import com.wapo.flagship.features.audio.models.AudioEvent
import com.wapo.flagship.features.audio.models.AudioPlaybackState
import com.wapo.flagship.features.audio.models.MediaItemData
import com.wapo.flagship.features.audio.models.PlaybackSpeed
import com.wapo.flagship.features.audio.playlist.Playlist
import com.wapo.flagship.features.audio.service2.media.extensions.playerType
import com.wapo.flagship.features.feedback.domain.FeedbackRepositoryProvider
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.userhistory.models.ConclusionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel class to handle [ClassicAudioManager2] calls and live data transformations.
 * View classes can use this ViewModel class to handle any audio operations.
 */
@HiltViewModel
class AudioMediaActivityViewModel @Inject constructor(
    private val audioManager: ClassicAudioManager2,
    private val configManager: ConfigManager,
    private val feedbackRepositoryProvider: FeedbackRepositoryProvider,
) : ViewModel() {

    val mediaItems: LiveData<List<MediaItemData>> =
        audioManager.mediaItems.map { it }

    val nowPlayingAudioItem: LiveData<NowPlayingAudioItem?> =
        audioManager.nowPlayingAudioItem.map { it }

    val showPlayerEvent: LiveData<NowPlayingAudioItem?> =
        nowPlayingAudioItem.map { it }

    val audioPlaybackState: LiveData<AudioPlaybackState>
        get() = audioManager.globalAudioPlaybackState

    val event: SharedFlow<AudioEvent> = audioManager.event

    val isFeedbackEnabled: Flow<Boolean> = configManager.state
        .map { feedbackRepositoryProvider.isEnabled }
        .distinctUntilChanged()

    val currentFeedbackEnabled: Boolean
        get() = feedbackRepositoryProvider.isEnabled

    private val _pageChangeEvent = MutableLiveData<Int>()
    val pageChangeEvent: LiveData<Int> = _pageChangeEvent

    private val _position = MutableLiveData<Float>()
    val position: LiveData<Float> = _position

    private val _openArticleEvent = LiveEvent<AudioArticleEvent>()
    val openArticleEvent: LiveData<AudioArticleEvent> = _openArticleEvent

    private val _playPreviousEvent = LiveEvent<Any>()
    val playPreviousEvent: LiveData<Any> = _playPreviousEvent

    private val _playNextEvent = LiveEvent<Any>()
    val playNextEvent: LiveData<Any> = _playNextEvent

    val addPlaylistArticleEvent = LiveEvent<String>()
    val addPlaylistPodcastEvent = LiveEvent<Playlist>()

    val audioPlayerEllipsisClickEvent = LiveEvent<AudioMediaConfig>()

    val removePlayListEvent = MutableLiveData<String>()

    val audioPlayerCurrentPlayingEllipsisEvent = LiveEvent<AudioMediaConfig>()

    private val _isLowDataModeEnable = LiveEvent<Boolean>()
    val isLowDataModeEnable: LiveData<Boolean> = _isLowDataModeEnable

    private val _playerCurrentPosition = MutableStateFlow(0L)

    private val timeRequiredToSkipAdMillis: Long?
        get() {
            val playerType = audioManager.player?.mediaMetadata?.playerType
                ?.let { value -> enumValues<PlayerType>().firstOrNull { it.name == value } }
                ?: PlayerType.PODCAST
            val adConfig = configManager.config.adsConfig.audio.getConfig(playerType)
            return adConfig?.timeRequiredToSkipAdInSeconds?.times(1000L)
        }
    val adUiState =
        combine(audioManager.isPlayingAd, _playerCurrentPosition) { isPlayingAd, playerPosition ->
            AudioAdUiState(
                isPlayingAd = isPlayingAd,
                isSkipAdEnabled = timeRequiredToSkipAdMillis?.let { playerPosition >= it } ?: false,
            )
        }.distinctUntilChanged()
    val isPlayingAd = adUiState.map { it.isPlayingAd }.distinctUntilChanged()

    init {
        viewModelScope.launch {
            while (isActive) {
                val player = audioManager.player
                if (player != null && player.isPlaying) {
                    val duration = player.duration
                    _playerCurrentPosition.update {
                        val maxPosition = if (duration > 0) duration else Long.MAX_VALUE
                        player.currentPosition.coerceIn(0, maxPosition)
                    }
                    if (duration > 0) {
                        updatePosition()
                    } else {
                        // Ensure position stays in a valid range when duration is unknown or zero.
                        _position.value = 0f
                    }
                }
                delay(500L)
            }
        }
    }

    fun getConfigFromMediaItem(mediaItemData: MediaItemData): AudioMediaConfig? {
        return audioManager.audioMediaSource.value?.audioMediaConfigList?.list?.find { it.id == mediaItemData.mediaId }
    }

    fun dispatchAddPlayListArticleEvent(url: String) {
        addPlaylistArticleEvent.value = url
    }

    fun dispatchAddPlayListPodcastEvent(playlist: Playlist) {
        addPlaylistPodcastEvent.value = playlist
    }

    fun dispatchRemovePlayListArticleEvent(url: String) {
        removePlayListEvent.value = url
    }

    @OptIn(UnstableApi::class)
    fun setPlayer(playerControlView: PlayerControlView) {
        playerControlView.player = audioManager.player
        val duration = audioManager.player?.contentDuration ?: 0L

        playerControlView.setProgressUpdateListener { position, _ ->
            _position.value = (position.toFloat().div(duration.toFloat()))
        }

    }

    fun updatePosition() {
        if (audioManager.player?.isPlaying == true) {
            val duration = audioManager.player?.duration ?: 0L
            val position = audioManager.player?.contentPosition ?: 0L
            _position.value = (position.toFloat()).div(duration.toFloat())
        }
    }

    fun getPosition(): Float {
        return audioManager.player?.contentPosition?.toFloat() ?: 0F
    }

    fun getPlayer(): Player? {
        return audioManager.player
    }

    fun getPlaybackSpeed(): PlaybackSpeed {
        return audioManager.getPlaybackSpeed()
    }

    fun getPlaylistId(): String? {
        return audioManager.audioMediaSource.value?.audioMediaConfigList?.id
    }

    fun getAudioProvider(): AudioProvider {
        return audioManager.audioProvider
    }

    fun getNowPlayingAudioPlaybackState(): AudioPlaybackState? {
        return nowPlayingAudioItem.value?.audioPlaybackState
    }

    fun setPlaybackSpeed(playbackSpeed: PlaybackSpeed) {
        audioManager.setPlaybackSpeed(playbackSpeed)
    }

    fun isMediaActive(config: AudioMediaConfig): Boolean {
        return audioManager.isMediaActive(config)
    }

    private fun isMediaListActive(configList: AudioMediaConfigList): Boolean {
        val sameList = if (configList.list.isNotEmpty()
            && configList.list.size == audioManager.audioMediaSource.value?.audioMediaConfigList?.list?.size
        ) {
            val newIds = configList.list.map { it.id }
            val currentIds =
                audioManager.audioMediaSource.value?.audioMediaConfigList?.list?.map { it.id }
            newIds == currentIds
        } else {
            false
        }
        return sameList
    }

    fun playMedia(config: AudioMediaConfig) {
        audioManager.playMedia(config)
    }

    fun playMedia(configList: AudioMediaConfigList, playMediaItemIndex: Int = 0) {
        if (playMediaItemIndex < 0) return
        if (playMediaItemIndex in 0 until configList.list.size) {
            if (isMediaActive(configList.list[playMediaItemIndex])) {
                audioManager.captureContentListenConclusion(ConclusionState.CONTENT_SELECT)
                audioManager.pauseOrPlay()
            } else if (isMediaListActive(configList)) {
                audioManager.captureContentListenConclusion(ConclusionState.CONTENT_SELECT)
                audioManager.playMediaAtIndex(playMediaItemIndex)
            } else {
                audioManager.playMedia(configList, playMediaItemIndex)
            }
        }
    }

    fun playMedia(mediaId: String) {
        audioManager.audioMediaSource.value?.audioMediaConfigList?.let { configList ->
            configList.list.indexOfFirst { it.id == mediaId }
                .let { playMedia(configList, it) }
        }
    }

    fun playMediaAtIndex(playMediaItemIndex: Int) {
        val mediaConfigListSize =
            audioManager.audioMediaSource.value?.audioMediaConfigList?.list?.size ?: 0

        if (playMediaItemIndex in 0 until mediaConfigListSize) {
            audioManager.audioMediaSource.value?.audioMediaConfigList?.let {
                if (!isMediaActive(it.list[playMediaItemIndex])) {
                    playMedia(it, playMediaItemIndex)
                }
            }
        }
    }

    fun pauseOrPlay() {
        audioManager.pauseOrPlay()
    }

    fun playPrevious() = audioManager.playPrevious()

    fun playNext() = audioManager.playNext()

    fun hasPrevious(): Boolean = audioManager.hasPrevious()

    fun hasNext(): Boolean = audioManager.hasNext()

    fun pauseMedia() {
        audioManager.pauseMedia()
    }

    fun resumeMedia() {
        audioManager.resumeMedia()
    }

    fun stopMedia(conclusionState: ConclusionState = ConclusionState.OTHER) {
        audioManager.stopMedia(conclusionState)
    }

    fun dispatchPageChangeEvent(position: Int) {
        _pageChangeEvent.value = position
    }

    fun dispatchPlayPreviousEvent() {
        _playPreviousEvent.value = Any()
    }

    fun dispatchPlayNextEvent() {
        _playNextEvent.value = Any()
    }

    fun removeMedia(audioMediaConfig: AudioMediaConfig) {
        viewModelScope.launch {
            audioManager.removeSingleMedia(audioMediaConfig)
        }
    }

    fun dispatchOpenArticleEvent(audioArticleEvent: AudioArticleEvent) {
        _openArticleEvent.value = audioArticleEvent
    }

    fun shouldShowPersistentPlayer(): Boolean {
        return nowPlayingAudioItem.value != null &&
                nowPlayingAudioItem.value?.audioPlaybackState != AudioPlaybackState.Cleared
    }

    val shouldShowPp = nowPlayingAudioItem.map { shouldShowFromState(it?.audioPlaybackState) }

    private fun shouldShowFromState(state: AudioPlaybackState?): Boolean {
        if (state == null) {
            return false
        }

        return when (state) {
            AudioPlaybackState.Cleared,
            AudioPlaybackState.JSONSourceError,
            is AudioPlaybackState.Error -> false

            else -> true
        }
    }

    fun getNowPlayingIndex(): Int {
        return nowPlayingAudioItem.value?.nowPlayingItemIndex ?: -1
    }

    fun reSubmitConfigList() {
        val configList = audioManager.audioMediaSource.value?.audioMediaConfigList
        val nowPlayingIndex = getNowPlayingIndex()
        configList?.let { audioManager.playMedia(it, nowPlayingIndex) }
    }

    fun seekTo(positionMS: Long) {
        audioManager.seekTo(positionMS)
    }

    fun dispatchAudioTrackerEvent(event: AudioTrackerEvent) {
        audioManager.dispatchAudioTrackerEvent(event)
    }

    fun setIsLowDataMode(enable: Boolean) {
        _isLowDataModeEnable.value = enable
    }

    fun setIsAudioCarousel(index: Int) {
        audioManager.audioMediaSource.value?.audioMediaConfigList?.list?.get(index)?.isAudioCarousel =
            true
    }

    fun skipAd() {
        audioManager.skipAd()
    }
}

private const val TAG = "AudioMediaActivityVM"

data class AudioArticleEvent(
    val contentUrl: String,
    val appSection: String,
    val sectionDisplayName: String
)
