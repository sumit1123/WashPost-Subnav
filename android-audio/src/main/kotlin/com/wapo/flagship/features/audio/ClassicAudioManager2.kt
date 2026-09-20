/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.audio

import android.content.Context
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.google.gson.Gson
import com.wapo.android.commons.util.LiveEvent
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.ads.repository.AudioAdsRepository
import com.wapo.flagship.features.audio.ads.ui.AudioAdsController
import com.wapo.flagship.features.audio.ads.util.AdPlayerUtils
import com.wapo.flagship.features.audio.ads.util.AdPlayerUtils.isAd
import com.wapo.flagship.features.audio.ads.util.AdPlayerUtils.isPlayingAd2
import com.wapo.flagship.features.audio.config.AudioProvider
import com.wapo.flagship.features.audio.config.PodcastTracker
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.config2.AudioMediaConfigList
import com.wapo.flagship.features.audio.config2.NowPlayingAudioItem
import com.wapo.flagship.features.audio.models.*
import com.wapo.flagship.features.audio.podcast.PodcastMetadataResolver
import com.wapo.flagship.features.audio.service2.common.MusicServiceConnection
import com.wapo.flagship.features.audio.service2.media.extensions.*
import com.wapo.flagship.features.audio.service2.media.library.AudioMediaSource
import com.wapo.flagship.features.audio.service2.media.library.BrowseTree
import com.wapo.flagship.features.audio.utils.AudioPreferences
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.ONBOARDING
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER
import com.washingtonpost.userhistory.models.ConclusionState
import com.washingtonpost.userhistory.domain.UserHistoryManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.mapNotNull

/**
 * Manager Class to handle all audio operations with MusicService (i.e. MediaLibraryService).
 * This class
 * 1. creates a [MusicServiceConnection] to talk to MediaBrowserServiceCompat
 * [com.wapo.flagship.features.audio.service2.media.MusicService] class.
 * 2. has LiveData members to observe playback and metadata states in View Models.
 */
@Singleton
class ClassicAudioManager2 @Inject constructor(
    val appContext: Context,
    val musicServiceConnection: MusicServiceConnection,
    val audioProvider: AudioProvider,
    private val audioRecommendationsProvider: AudioRecommendationsProvider,
    private val userHistoryManager: UserHistoryManager,
    private val adsController: AudioAdsController,
    private val adsRepository: AudioAdsRepository,
    private val podcastMetadataResolver: PodcastMetadataResolver,
) {

    private val tag = "ClassicAudioManager2"
    private val audioManagerJob = SupervisorJob()
    private val audioManagerScope = CoroutineScope(Dispatchers.Main + audioManagerJob)

    private val SAVE_HEARD_HISTORY_THRESHOLD = 3000

    /**
     * [com.wapo.flagship.features.audio.service2.media.MusicService] sets this player when player
     * is ready.
     */
    var player: Player? = null

    /**
     * Tracker members.[PodcastTracker] handles start, end, percentage completion events for
     * Podcasts.
     */
    private var podcastTracker: PodcastTracker? = null
    private var currentPlayingTrackingConfig: AudioMediaConfig? = null

    /**
     * LiveData to post [AudioTrackerEvent] event objects based on the playback states.
     * Right now [ClassicAudioManager2] is observing to send those events to [AudioTracker] class
     */
    private val _audioTrackerEvent = LiveEvent<AudioTrackerEvent>()
    val audioTrackerEvent: LiveData<AudioTrackerEvent> = _audioTrackerEvent
    private var audioTrackerEventObserver: Observer<AudioTrackerEvent>? = null

    /**
     * StateFlow to handle [AudioMediaSource] object and is collected by the
     * [com.wapo.flagship.features.audio.service2.media.MusicService] class.
     * App also can find the [AudioMediaConfigList] input object for reading operations.
     */
    private val _audioMediaSource: MutableStateFlow<AudioMediaSource?> = MutableStateFlow(null)
    val audioMediaSource = _audioMediaSource.asStateFlow()

    /**
     * LiveData for UI elements. [com.wapo.flagship.features.audio.service2.media.MusicService]
     * prepares these once [AudioMediaSource] is ready. So UI can observe for [MediaItemData] data
     * to update UI based on playback states.
     */
    private val _mediaItems = MutableLiveData<List<MediaItemData>>(emptyList())
    val mediaItems: LiveData<List<MediaItemData>> = _mediaItems

    private val _globalAudioPlaybackState = MutableLiveData<AudioPlaybackState>()
    val globalAudioPlaybackState: LiveData<AudioPlaybackState> = _globalAudioPlaybackState

    /**
     * LiveData to track currently playing audio item.
     */
    private val _nowPlayingAudioItem = MutableLiveData<NowPlayingAudioItem?>()
    val nowPlayingAudioItem: LiveData<NowPlayingAudioItem?> = _nowPlayingAudioItem

    private val _event: MutableSharedFlow<AudioEvent> = MutableSharedFlow(extraBufferCapacity = 1)
    val event: SharedFlow<AudioEvent> = _event.asSharedFlow()

    val isPlayingAd = adsController.isPlayingAd

    /**
     * LiveData to track each playlist item's playback state. Right now [ClassicAudioManager2] is
     * using for tracking events.
     */
    private val _configListAudioPlaybackStates = LinkedHashMap<String, MutableLiveData<AudioPlaybackState>>()
    val configListAudioPlaybackStates: Map<String, LiveData<AudioPlaybackState>> = _configListAudioPlaybackStates
    private val _configListAudioPlaybackStateObservers = LinkedHashMap<String, Observer<AudioPlaybackState>?>()
    val configListAudioPlaybackStateObservers: Map<String, Observer<AudioPlaybackState>?> = _configListAudioPlaybackStateObservers

    private var isNextClicked = false

    private val playbackStateObserver = Observer<Int> { playbackState ->
        val mediaItem = musicServiceConnection?.nowPlaying?.value
        val playWhenReady = musicServiceConnection?.playWhenReady?.value ?: false
        mediaItem?.let {
            audioManagerScope.launch {
                 updateState(playbackState, playWhenReady, mediaItem)
            }
        }
    }

    private val playWhenReadyObserver = Observer<Boolean> { playWhenReady ->
        val playbackState = musicServiceConnection?.playbackState?.value ?: 0
        val mediaItem = musicServiceConnection?.nowPlaying?.value
        mediaItem?.let {
            audioManagerScope.launch {
                 updateState(playbackState, playWhenReady, mediaItem)
            }
        }
    }

    private val nowPlayingObserver = Observer<MediaItem?> { mediaItem ->
        val playbackState = musicServiceConnection?.playbackState?.value ?: 0
        val playWhenReady = musicServiceConnection?.playWhenReady?.value ?: false
        mediaItem?.let {
            audioManagerScope.launch {
                 updateState(playbackState, playWhenReady, mediaItem)
            }
        }
    }

    fun disableNowPlayingItem() {
        _nowPlayingAudioItem.postValue(null)
    }

    //Updated mediaItems when an item is removed from playlist
    fun updateMediaList(list: List<MediaItemData>){
        _mediaItems.postValue(list)
    }

    fun playSingleMedia(audioMediaConfig: AudioMediaConfig) {
        playMedia(AudioMediaConfigList(list = mutableListOf(audioMediaConfig)))
    }

    fun playMedia(audioMediaConfigList: AudioMediaConfigList, playMediaItemIndex: Int = 0) {
        audioManagerScope.launch {
            stopMedia(ConclusionState.CONTENT_SELECT)
            initializeConfigListAudioPlaybackStates(audioMediaConfigList)
            observeConfigListAudioPlaybackStateChanges()
            observePollyTrackerEvent()
            with(AudioMediaSource(appContext, audioProvider, audioMediaConfigList, adsRepository, podcastMetadataResolver,
                { id, state ->
                    setAudioPlaybackState(id, state)
                },
                { state ->
                    setMediaSourcePlaybackState(state, playMediaItemIndex)
                }
            )) {
                _audioMediaSource.emit(this)
                whenReady {
                    subscribe {
                        if (_nowPlayingAudioItem.value?.audioPlaybackState != AudioPlaybackState.JSONSourceInitialized) {
                            return@subscribe
                        }
                        playMediaItemIndexIfNotPlaying(
                            audioMediaConfigList,
                            playMediaItemIndex
                        )
                    }
                }
            }
        }
    }

    // This function finds the item in the carousel that is a placeholder and replaces it with the new media config,
    suspend fun replaceMediaAndPlay(newMediaConfig: AudioMediaConfig) {
        audioManagerScope.launch {
            // Getting the current source and creating a mutable copy of the config list
            val currentSource = audioMediaSource.value ?: return@launch
            val configList = currentSource.audioMediaConfigList.list.toMutableList()

            // Finding the placeholder index to replace
            val placeholderIndex = configList.indexOfFirst { it.audioType == PLACEHOLDER }
            if (placeholderIndex == -1) {
                Logger.e(tag, "Placeholder Media ID 'placeholder' not found in config list.")
                return@launch
            }

            // Pausing current audio
            musicServiceConnection.pause()
            captureContentListenConclusion(ConclusionState.CONTENT_SELECT)

            // Updating the list with the new config
            configList[placeholderIndex] = newMediaConfig
            val updatedConfigList = AudioMediaConfigList(currentSource.audioMediaConfigList.id, configList)

            // Register the new list IDs so we can observe their state changes
            initializeConfigListAudioPlaybackStates(updatedConfigList)
            observeConfigListAudioPlaybackStateChanges()

            // Create a new AudioMediaSource with the updated list. This triggers content loading.
            with(AudioMediaSource(appContext, audioProvider, updatedConfigList, adsRepository, podcastMetadataResolver,
                { id, state ->
                    setAudioPlaybackState(id, state)
                },
                { state ->
                    // Use the placeholder's original index when notifying the service state
                    setMediaSourcePlaybackState(state, placeholderIndex)
                }
            )) {
                // replacing the entire source.
                _audioMediaSource.emit(this) //

                whenReady { isSourceReady ->
                    if (isSourceReady) {
                        _mediaItems.postValue(mediaItemToMediaItemData(getCatalog()))

                        playMediaList(getCatalog(), startIndex = placeholderIndex, startPositionMs = 0L)
                    } else {
                        Logger.e(tag, "Failed to initialize new AudioMediaSource after item replacement.")
                    }
                }
            }
        }
    }

    /**
     * A job that manages saving heard audio to history and loading / caching next audio
     */
    private var listenHistoryJob: Job? = null

    /**
     * Flow that emits current audio config after [SAVE_HEARD_HISTORY_THRESHOLD] of the audio has completed
     */
    private fun listenHistoryFlow(player: Player) = flow {
        var hasEmitted = false
        while (!hasEmitted) {
            if(player.playbackState == PlayerState.READY.state && player.playWhenReady) {
                if(player.currentPosition > SAVE_HEARD_HISTORY_THRESHOLD) {
                    currentPlayingTrackingConfig?.let {
                        emit(it)
                    }
                    hasEmitted = true
                }
            }
            withContext(Dispatchers.Default) {
                delay(1000)
            }
        }
    }

    /**
     * This job is triggered when audio has started playing.
     */
    private fun startListenHistoryJob() {
        if (_nowPlayingAudioItem.value?.audioMediaConfig?.getPlayerType() == PlayerType.STANDALONE || _nowPlayingAudioItem.value?.audioMediaConfig?.audioType == ONBOARDING) {
            return
        }
        listenHistoryJob?.cancel()
        listenHistoryJob = audioManagerScope.launch {
            player?.let {
                listenHistoryFlow(it).cancellable().collect { config ->
                    // Add current audio to listen history
                    audioRecommendationsProvider.addAudioToHistory(config)
                    // If there is a next audio (from playlist), do not load next audio (recommended)
                    if(!hasNext() && player?.isPlayingAd2 != true) {
                        // Load next audio from network
                        val excludeList:List<String> =
                            mediaItems.value?.mapNotNull { item -> item.contentUrl }
                            ?: emptyList()
                        audioRecommendationsProvider.loadNextAudioRecommendation(excludeList)
                        // Post next audio config to live data
                        audioRecommendationsProvider.getNextAudioRecommendation()?.let { conf ->
                            addSingleMediaToQueue(conf)
                        }
                        // Reset rollthrough until we know current audio playing is completed.
                        audioRecommendationsProvider.setRollthroughNextAudio(false)
                    }
                }
            }
        }
    }

    /**
     * Stop this job when audio is closed.
     */
    private fun stopListenHistoryJob() {
        listenHistoryJob?.cancel()
        listenHistoryJob = null
    }

    fun resumeMedia() {
        musicServiceConnection?.play()
    }

    fun pauseMedia() {
        musicServiceConnection?.pause()
    }

    fun stopMedia(conclusionState: ConclusionState = ConclusionState.UNKNOWN) {
        captureContentListenConclusion(conclusionState)
        _nowPlayingAudioItem.postValue(
            NowPlayingAudioItem(
                nowPlayingAudioItem.value?.playlistId,
                nowPlayingAudioItem.value?.audioMediaConfigItemIndex ?: -1,
                nowPlayingAudioItem.value?.audioMediaConfig,
                nowPlayingAudioItem.value?.nowPlayingItemIndex ?: -1,
                nowPlayingAudioItem.value?.mediaItemData,
                AudioPlaybackState.Cleared
            )
        )
        musicServiceConnection.apply {
            hideNotification()
            playbackState.removeObserver(playbackStateObserver)
            nowPlaying.removeObserver(nowPlayingObserver)
            playWhenReady.removeObserver(playWhenReadyObserver)
            mediaItemList.postValue(emptyList())
            stop()
            clearMediaItems()
        }
        _mediaItems.value = emptyList()
        _audioMediaSource.value = null
        _configListAudioPlaybackStateObservers.forEach { entry ->
            entry.value?.let { value ->
                configListAudioPlaybackStates[entry.key]?.removeObserver(value)
            }
        }
        _configListAudioPlaybackStates.clear()
        _configListAudioPlaybackStateObservers.clear()
        podcastTracker?.cancelJobs()
        currentPlayingTrackingConfig?.audioTracking?.stopTrackingProgress()
        podcastTracker = null
        audioTrackerEventObserver?.let { audioTrackerEvent.removeObserver(it) }
        audioTrackerEventObserver = null
        currentPlayingTrackingConfig = null
        stopListenHistoryJob()
        audioManagerScope.launch { _event.emit(AudioEvent.Stop) }
    }

    fun releaseMusicServiceConnection() {
        musicServiceConnection.apply {
            hideNotification()
            playbackState.removeObserver(playbackStateObserver)
            nowPlaying.removeObserver(nowPlayingObserver)
            playWhenReady.removeObserver(playWhenReadyObserver)
            mediaItemList.postValue(emptyList())
            release()
            player?.release()
            player = null
        }
    }

    fun skipAd() {
        adsController.skipAd()
    }

    fun releaseAdsController() {
        adsController.release()
    }

    fun pauseOrPlay() {
        val playbackState = musicServiceConnection?.playbackState?.value
        val playWhenReady = musicServiceConnection?.playWhenReady?.value ?: false
        val mediaItem = musicServiceConnection?.nowPlaying?.value
        mapToAudioPlaybackState(playbackState ?: 0, playWhenReady, mediaItem).let {
            when (it) {
                is AudioPlaybackState.Playing -> {
                    musicServiceConnection?.pause()
                }
                AudioPlaybackState.Paused -> {
                    musicServiceConnection?.play()
                }
                else -> {
                    Logger.w(
                        tag, "Playable item clicked but neither play nor pause are enabled!"
                    )
                }
            }
        }
    }

    fun playMedia(config: AudioMediaConfig) {
        if (isMediaActive(config)) {
            pauseOrPlay()
        } else {
            playSingleMedia(config)
        }
    }

    fun isMediaActive(
        config: AudioMediaConfig,
        isActiveIfPlayingAdForContentMedia: Boolean = true
    ): Boolean {
        val nowPlaying = musicServiceConnection.nowPlaying.value
        val playbackState = musicServiceConnection.playbackState.value ?: 0
        val isPrepared = playbackState == PlayerState.READY.state || playbackState == PlayerState.BUFFERING.state
        val isAdForContentItem = nowPlaying?.isAd == true && nowPlaying.mediaMetadata.contentMediaId == config.id
        return (
                isPrepared && config.id == nowPlaying?.mediaId ||
                getUrlWithoutParameters(config.id) == getUrlWithoutParameters(nowPlaying?.mediaId ?: "") ||
                (isActiveIfPlayingAdForContentMedia && isAdForContentItem)
            )
    }

    private fun getUrlWithoutParameters(url: String): String =
        url
            .toUri()
            .buildUpon()
            .clearQuery()
            .fragment("")
            .build()
            .toString()

    fun hasPrevious(): Boolean {
        val nowPlayingItemIndex = nowPlayingItemIndex()
        return nowPlayingItemIndex > 0
    }

    fun hasNext(): Boolean {
        val nowPlayingItemIndex = nowPlayingItemIndex()
        return (nowPlayingItemIndex > -1 && nowPlayingItemIndex < (mediaItems.value?.size ?: 0) - 1)
    }

    fun playPrevious() {
        captureContentListenConclusion(ConclusionState.PREVIOUS)
        if (hasPrevious()) {
            musicServiceConnection?.seekToPreviousMediaItem()
            dispatchAudioTrackerEvent(AudioTrackerEvent.PreviousPlay(nowPlayingItemIndex() - 1))
        }
    }

    /**
     * If there is an audio recommendation, play it next.
     * [hasRecommended] will only be true if there is no remaining items in playlist.o
     */
    fun playNext() {
        captureContentListenConclusion(ConclusionState.NEXT)
        if (hasNext()) {
            musicServiceConnection?.seekToNextMediaItem()
            dispatchAudioTrackerEvent(AudioTrackerEvent.NextPlay(nowPlayingItemIndex() + 1))
        }
    }

    fun playMediaAtIndex(index: Int, positionToStartAtInMs: Long? = null) {
        if (index in 0 until (audioMediaSource.value?.getCatalog()?.size ?: 0)) {
            playMediaList(
                audioMediaSource.value?.getCatalog(), index, positionToStartAtInMs
            )
        }
    }

    fun nowPlayingItemIndex(): Int {
        return nowPlayingAudioItem.value?.nowPlayingItemIndex ?: -1
    }

    fun seekTo(positionMS: Long) {
        musicServiceConnection?.seekTo(positionMS)
    }

    private fun setAudioPlaybackState(id: String, audioPlaybackState: AudioPlaybackState) {
        _configListAudioPlaybackStates[id]?.value = audioPlaybackState
    }

    private fun setMediaSourcePlaybackState(audioPlaybackState: AudioPlaybackState, playMediaItemIndex: Int) {
        _nowPlayingAudioItem.postValue(
            NowPlayingAudioItem(
                audioMediaSource.value?.audioMediaConfigList?.id,
                playMediaItemIndex,
                audioMediaSource.value?.audioMediaConfigList?.list?.get(playMediaItemIndex),
                -1,
                null,
                audioPlaybackState
            )
        )
    }

    fun setNowPlayingItem(audioPlaybackState: AudioPlaybackState, audioMediaConfig: AudioMediaConfig, index: Int) {
        _nowPlayingAudioItem.postValue(
            NowPlayingAudioItem(
                null,
                index,
                audioMediaConfig,
                index,
                null,
                audioPlaybackState
            )
        )
    }

    fun hasStopped(): Boolean {
        return audioMediaSource.value == null
    }

    private fun mapToAudioPlaybackState(playerState: Int, playWhenReady: Boolean, mediaItem: MediaItem?): AudioPlaybackState {
        return when (PlayerState.fromState(playerState)) {
            PlayerState.IDLE -> AudioPlaybackState.None
            PlayerState.ENDED -> AudioPlaybackState.Stopped
            PlayerState.BUFFERING -> AudioPlaybackState.Buffering
            PlayerState.READY -> {
                when {
                    playWhenReady && mediaItem?.isAd == true -> AudioPlaybackState.Playing.PlayingAd
                    playWhenReady -> AudioPlaybackState.Playing.PlayingContent
                    else -> AudioPlaybackState.Paused
                }
            }
            PlayerState.ERROR -> {
                AudioPlaybackState.LaunchTTS
            }
            else -> {
                AudioPlaybackState.None
            }
        }
    }

    private fun updateAudioPlaybackState(playerState: Int, playWhenReady: Boolean,  mediaItem: MediaItem) {
        val audioPlaybackState = mapToAudioPlaybackState(playerState, playWhenReady, mediaItem)
        _configListAudioPlaybackStates[mediaItem.mediaId]?.postValue(audioPlaybackState)
    }

    private fun initializeConfigListAudioPlaybackStates(audioMediaConfigList: AudioMediaConfigList) {
        _configListAudioPlaybackStates.clear()
        _configListAudioPlaybackStateObservers.clear()
        audioMediaConfigList.list.forEach {
            _configListAudioPlaybackStates[it.id] = MutableLiveData<AudioPlaybackState>()
            _configListAudioPlaybackStateObservers[it.id] = null
        }
    }

    private fun observeConfigListAudioPlaybackStateChanges() {
        configListAudioPlaybackStateObservers.forEach { entry ->
            observeConfigListAudioPlaybackStateChanges(entry.key, entry.value)
        }
    }

    private fun observeConfigListAudioPlaybackStateChanges(key: String?, value: Observer<AudioPlaybackState>?) {
        key ?: return
        value?.let { value ->
            configListAudioPlaybackStates[key]?.removeObserver(value)
        }
        if (_configListAudioPlaybackStates[key] == null) {
            _configListAudioPlaybackStates[key] = MutableLiveData<AudioPlaybackState>()
        }
        _configListAudioPlaybackStateObservers[key] = Observer<AudioPlaybackState> { audioPlaybackState ->
            Logger.d(tag, "AudioDebug, configListAudioPlaybackStateChange, state=$audioPlaybackState, id=${key}")
            if (audioPlaybackState is AudioPlaybackState.LaunchTTS) {
                audioManagerScope.launch { _event.emit(AudioEvent.LaunchTts) }
            }
            trackEvents(audioPlaybackState, key)
        }.also {
            configListAudioPlaybackStates[key]?.observeForever(it)
        }
    }

    private fun observePollyTrackerEvent() {
        audioTrackerEventObserver?.let { audioTrackerEvent.removeObserver(it) }
        audioTrackerEventObserver = Observer<AudioTrackerEvent> {

            when (it) {
                AudioTrackerEvent.Start -> {
                    currentPlayingTrackingConfig?.audioTracking?.onStart()
                }
                AudioTrackerEvent.SkipBackward -> {
                    currentPlayingTrackingConfig?.audioTracking?.onSkipBackward()
                }
                AudioTrackerEvent.SkipForward -> {
                    currentPlayingTrackingConfig?.audioTracking?.onSkipForward()
                }
                AudioTrackerEvent.Complete -> {
                    currentPlayingTrackingConfig?.audioTracking?.onComplete()
                    // If there is no next item in playlist and there is a recommended item
                }
                AudioTrackerEvent.Reset -> {
                    currentPlayingTrackingConfig?.audioTracking?.reset()
                }
                is AudioTrackerEvent.PreviousPlay -> {
                    currentPlayingTrackingConfig?.audioTracking?.onPreviousPlay(it.position)
                }
                is AudioTrackerEvent.NextPlay -> {
                    currentPlayingTrackingConfig?.audioTracking?.onNextPlay(it.position)
                    isNextClicked = true
                }
                is AudioTrackerEvent.UpdateVoice -> {
                    currentPlayingTrackingConfig?.audioTracking?.updateVoice(it.voice)
                }
                is AudioTrackerEvent.UpdatePlayAd -> {
                    currentPlayingTrackingConfig?.audioTracking?.updatePlayAd(it.playAd)
                }
                is AudioTrackerEvent.UpdateSpeed -> {
                    currentPlayingTrackingConfig?.audioTracking?.updateSpeed(it.speed)
                }
                is AudioTrackerEvent.UpdateProgressThreshold -> {
                    currentPlayingTrackingConfig?.audioTracking?.updateProgressThreshold(it.position)
                }
                is AudioTrackerEvent.UpdateAudioType -> {
                    currentPlayingTrackingConfig?.audioTracking?.updateAudioType(it.playerType)
                }
                is AudioTrackerEvent.UpdateRollThrough -> {
                    currentPlayingTrackingConfig?.audioTracking?.setIsRollThrough(it.rollThrough)
                }
                is AudioTrackerEvent.UpdateDuration -> {
                    currentPlayingTrackingConfig?.audioTracking?.updateDuration(it.duration)
                }
            }
        }.also {
            audioTrackerEvent.observeForever(it)
        }
    }

    private fun playMediaItemIndexIfNotPlaying(
        audioMediaConfigList: AudioMediaConfigList,
        playMediaItemIndex: Int
    ) {
        if (playMediaItemIndex >= 0 && playMediaItemIndex < audioMediaConfigList.list.size) {
            if (audioMediaConfigList.list[playMediaItemIndex].id != nowPlayingAudioItem.value?.mediaItemData?.mediaId
                || nowPlayingAudioItem.value?.audioPlaybackState == AudioPlaybackState.Stopped
                || nowPlayingAudioItem.value?.audioPlaybackState == AudioPlaybackState.Cleared) {
                playMediaAtIndex(playMediaItemIndex, audioMediaConfigList.list[playMediaItemIndex].positionToStartInMs)
            }
        }
    }

    private fun playMediaList(mediaItemList: List<MediaItem>?, startIndex: Int = 0, startPositionMs: Long? = null) {
        mediaItemList ?: return
        if (_mediaItems.value?.isEmpty() == true) {
            _mediaItems.value = mediaItemToMediaItemData(mediaItemList)
        }
        val currentMediaItem = player?.currentMediaItem
        if (
            currentMediaItem != null &&
            currentMediaItem.isAd &&
            AdPlayerUtils.isAdForMediaItemList(currentMediaItem, mediaItemList, startIndex)
        ) {
            //  The media will resume after the current ad completes
            return
        }
        musicServiceConnection.pause()
        musicServiceConnection.apply {
            setMediaItems(mediaItemList, startIndex, startPositionMs)
            // Set the user's preference before preparing/starting playback so a newly opened
            // player does not briefly begin at the service default speed.
            applySavedPlaybackSpeed()
            prepare() // Prepares the media for playback
            play()    // Starts playback
        }
    }

    private fun applySavedPlaybackSpeed() {
        val savedSpeed = AudioPreferences.getAudioPlaybackSpeed(appContext)
        musicServiceConnection.setPlaybackSpeed(savedSpeed)
        dispatchAudioTrackerEvent(AudioTrackerEvent.UpdateSpeed(savedSpeed))
    }

    private fun subscribe(action: (MusicServiceConnection) -> Unit) {
        if (musicServiceConnection.isConnected.value == true) {
            // If connected, fetch media list and observe playback states
            musicServiceConnection.fetchMediaListFromParent(BrowseTree.ROOT)
            musicServiceConnection.mediaItemList.observeForever { mediaItemList ->
                val mediaItemDataList: List<MediaItemData> = mediaItemList?.map { mediaItem ->
                    createMediaItemData(mediaItem)
                } ?: emptyList()
                _mediaItems.value = mediaItemDataList
                audioManagerScope.launch {
                    action(musicServiceConnection)
                }
            }
            musicServiceConnection.playbackState.observeForever(playbackStateObserver)
            musicServiceConnection.nowPlaying.observeForever(nowPlayingObserver)
            musicServiceConnection.playWhenReady.observeForever(playWhenReadyObserver)
        }
    }

    private fun mediaItemToMediaItemData(mediaItemList: List<MediaItem>?): List<MediaItemData> {
        return mediaItemList?.map { mediaItem ->
            createMediaItemData(mediaItem)
        } ?: emptyList()
    }

    private fun createMediaItemData(mediaItem: MediaItem): MediaItemData {
        val gson = Gson()
        val voices = mediaItem.mediaMetadata.extras?.getStringArrayList(METADATA_KEY_VOICES)?.run {
            this.map { jsonString -> gson.fromJson(jsonString, PlaybackVoice::class.java) }
        }
        return MediaItemData(
            mediaItem.mediaId,
            mediaItem.mediaMetadata.mediaUri.toString(),
            mediaItem.mediaMetadata.displayTitlePrefix,
            mediaItem.mediaMetadata.displayTitleSeparator,
            primaryLabel = mediaItem.mediaMetadata.displayPrimaryLabel,
            secondaryLabel = mediaItem.mediaMetadata.displaySecondaryLabel,
            mediaItem.mediaMetadata.title.toString(),
            mediaItem.mediaMetadata.subtitle?.toString(),
            mediaItem.mediaMetadata.extras?.getString(METADATA_KEY_IMAGE_URL),
            mediaItem.mediaMetadata.extras?.getString(METADATA_KEY_IMAGE_CAPTION),
            mediaItem.mediaMetadata.albumArtUri.toString(),
            mediaItem.mediaMetadata.date,
            mediaItem.mediaMetadata.duration,
            mediaItem.mediaMetadata.seriesSlug,
            mediaItem.mediaMetadata.podcastSlug,
            mediaItem.mediaMetadata.subscriptionLinks,
            null,
            mediaItem.mediaMetadata.firstPublished,
            voices,
            mediaItem.mediaMetadata.caption,
            mediaItem.mediaMetadata.contentUrl,
            mediaItem.mediaMetadata.sectionName,
            mediaItem.mediaMetadata.playerType,
            PlayerState.IDLE.state,
            mediaItem.mediaMetadata.playAd,
            style = mediaItem.mediaMetadata.displayPrimaryLabelStyle,
            audioType = mediaItem.mediaMetadata.audioType,
            isShared = mediaItem.mediaMetadata.extras?.getBoolean(IS_SHARED_PODCAST)
        )
    }

    private fun updateState(
        playerState: Int,
        playWhenReady: Boolean,
        mediaItem: MediaItem
    ) {
        var mediaItemsList = mediaItems.value?.map {
            val state =
                if (it.mediaId == mediaItem.mediaId) playerState else PlayerState.IDLE.state
            it.copy(playbackState = state)
        } ?: emptyList()
        updateAudioPlaybackState(playerState, playWhenReady, mediaItem)

        val audioPlaybackState = mapToAudioPlaybackState(playerState, playWhenReady, mediaItem)
        _globalAudioPlaybackState.postValue(audioPlaybackState)

        val mediaContentId = if(mediaItem.isAd) mediaItem.mediaMetadata.contentMediaId else mediaItem.mediaId
        val nowPlayingMediaConfigIndex =
            audioMediaSource.value?.audioMediaConfigList?.list?.indexOfFirst { it.id == mediaContentId }
                ?: -1
        if (nowPlayingMediaConfigIndex > -1 && mediaItemsList.isNotEmpty()) {
            val nowPlayingMediaConfig =
                audioMediaSource.value?.audioMediaConfigList?.list?.get(nowPlayingMediaConfigIndex)
            var nowPlayingMediaIndex =
                mediaItemsList.indexOfFirst { it.mediaId == mediaContentId }
            if (nowPlayingMediaIndex == -1) {
                //mediaItemList is not updated with the new mediaItem, so update it
                mediaItemsList = mediaItemToMediaItemData(audioMediaSource.value?.getCatalog())
                updateMediaList(mediaItemsList)
                Logger.d(
                    tag,
                    "AudioDebug: updateState, mediaItemsList is not updated with the new mediaItem, so update it"
                )
                nowPlayingMediaIndex =
                    mediaItemsList.indexOfFirst { it.mediaId == mediaContentId }
            }
            //check is nowPlayingMediaIndex is valid
            if (nowPlayingMediaIndex > -1) {
                _nowPlayingAudioItem.postValue(
                    NowPlayingAudioItem(
                        audioMediaSource.value?.audioMediaConfigList?.id,
                        nowPlayingMediaConfigIndex,
                        nowPlayingMediaConfig,
                        nowPlayingMediaIndex,
                        mediaItemsList[nowPlayingMediaIndex],
                        audioPlaybackState
                    )
                )
                Logger.d(
                    tag,
                    "AudioDebug: updateState, this=${hashCode()}, _mediaItemsSize=${_mediaItems.value?.size}, itemsSize=${mediaItemsList.size}," +
                            " changedId=${mediaContentId}, nowPlayingId=${mediaItemsList[nowPlayingMediaIndex].mediaId}," +
                            " changedState=${playerState}, nowPlayingState=${mediaItemsList[nowPlayingMediaIndex].playbackState}"
                )
            }
        }
    }

    /**
     * Sets the playback speed [playbackSpeed] which ensures that the speed is from one of the
     * values from the set.
     */
    fun setPlaybackSpeed(playbackSpeed: PlaybackSpeed) {
        // Playback speed is a user preference, not state scoped to the current player UI.
        // Persist it here because this is the common path for the section-front, article,
        // mini-player, and Settings controls.
        AudioPreferences.setAudioPlaybackSpeed(playbackSpeed.speed, appContext)
        musicServiceConnection.setPlaybackSpeed(playbackSpeed.speed)
        dispatchAudioTrackerEvent(AudioTrackerEvent.UpdateSpeed(playbackSpeed.speed))
    }

    /**
     * Gets the [Float] playback speed value form [ClassicAudioManager] and resolves it in to
     * [PlaybackSpeed]
     */
    fun getPlaybackSpeed(): PlaybackSpeed {
        return when (musicServiceConnection.playerPlaybackSpeed) {
            0.75f -> PlaybackSpeed.Slow()
            1f -> PlaybackSpeed.Normal()
            1.25f -> PlaybackSpeed.Quick()
            1.5f -> PlaybackSpeed.Fast()
            1.75f -> PlaybackSpeed.Faster()
            2f -> PlaybackSpeed.Fastest()
            else -> PlaybackSpeed.Normal()
        }
    }

    private fun trackEvents(audioPlaybackState: AudioPlaybackState, currentObservedKey: String) {
        // Tracking, Tracking, Tracking.
        when (audioPlaybackState) {
            AudioPlaybackState.Playing.PlayingContent -> {
                Logger.d(tag, "AudioDebug, configListAudioPlaybackStateChange, should create a tracker? = ${currentPlayingTrackingConfig?.id != currentObservedKey}")
                if (currentPlayingTrackingConfig?.id != currentObservedKey) {
                    // New item is playing. Start a tracker based on the type.
                    podcastTracker?.cancelJobs()
                    val previousPlayedConfig = currentPlayingTrackingConfig
                    previousPlayedConfig?.audioTracking?.stopTrackingProgress()
                    currentPlayingTrackingConfig = audioMediaSource.value?.audioMediaConfigList?.list?.firstOrNull { it.id == currentObservedKey}
                    if (shouldStartListenHistory(currentPlayingTrackingConfig)) {
                        startListenHistoryJob()
                    }

                    when (currentPlayingTrackingConfig?.getPlayerType()) {
                        PlayerType.PODCAST -> {
                            Logger.d(tag, "AudioDebug, configListAudioPlaybackStateChange, creating a tracker")

                            val isRollThrough = previousPlayedConfig?.id?.let { id ->
                                configListAudioPlaybackStates[id]?.value is AudioPlaybackState.Playing
                            }

                            if (isRollThrough == true) {
                                podcastTracker?.trackRollThroughComplete(player?.duration)
                            }

                            podcastTracker = PodcastTracker(
                                audioProvider,
                                nowPlayingAudioItem.value?.mediaItemData,
                                currentPlayingTrackingConfig?.audioTracking
                            ).also {
                                it.player = player
                                it.progressThreshold = nowPlayingItemIndex()
                                it.startTracking()
                            }
                        }
                        PlayerType.AUTOMATED, PlayerType.HUMAN, PlayerType.STANDALONE -> {
                            Logger.d(tag, "AudioDebug, configListAudioPlaybackStateChange, Sending events to AudioTracker")
                            // reset
                            dispatchAudioTrackerEvent(AudioTrackerEvent.Reset)

                            // is RollThrough? Then update RollThrough status and send completion event
                            val isPreviousPlaying = previousPlayedConfig?.id?.let { id ->
                                configListAudioPlaybackStates[id]?.value is AudioPlaybackState.Playing
                            }

                            val isRollThrough = isPreviousPlaying == true && !isNextClicked
                            // update RollThrough
                            dispatchAudioTrackerEvent(
                                // set roll through if rollthrough from playlist or if recommended is rollthrough
                                AudioTrackerEvent.UpdateRollThrough(
                                    (isRollThrough ?: false) || audioRecommendationsProvider.isRecommendedRollthrough()
                                )
                            )
                            Logger.d(tag, "AudioDebug, configListAudioPlaybackStateChange, isRollThrough=$isRollThrough")
                            // Send completion event if it is RollThrough
                            if (isRollThrough == true) {
                                previousPlayedConfig?.audioTracking?.onComplete()
                            }
                            isNextClicked = false

                            // update AudioType
                            currentPlayingTrackingConfig?.getPlayerType()?.let { playerType ->
                                dispatchAudioTrackerEvent(AudioTrackerEvent.UpdateAudioType(playerType))
                            }
                            // update Voice
                            val voice =
                                if (currentPlayingTrackingConfig?.getPlayerType() == PlayerType.HUMAN) ""
                                else AudioPreferences.getPreferredPlaybackVoice(currentPlayingTrackingConfig?.voices)?.id ?: ""
                            dispatchAudioTrackerEvent(AudioTrackerEvent.UpdateVoice(voice))
                            // update Speed
                            dispatchAudioTrackerEvent(AudioTrackerEvent.UpdateSpeed(getPlaybackSpeed().speed))
                            // update Threshold
                            dispatchAudioTrackerEvent(AudioTrackerEvent.UpdateProgressThreshold(
                                nowPlayingItemIndex()
                            ))
                            // update Duration
                            currentPlayingTrackingConfig?.duration?.let {
                                dispatchAudioTrackerEvent(AudioTrackerEvent.UpdateDuration(it))
                            } ?: player?.duration?.let {
                                // We want to make sure duration is rounded up to the next second.
                                val duration = (it.toFloat() + 999f)/1000f
                                dispatchAudioTrackerEvent(AudioTrackerEvent.UpdateDuration(duration = duration.toLong()))
                            }
                            // Send Start Event
                            dispatchAudioTrackerEvent(AudioTrackerEvent.Start)
                            player?.let {
                                currentPlayingTrackingConfig?.audioTracking?.startTrackingTtsProgress(it)
                            }
                        }
                        else -> {
                            // No op
                        }
                    }
                }
            }
            AudioPlaybackState.Stopped, AudioPlaybackState.Cleared -> {
                Logger.d(tag, "AudioDebug, configListAudioPlaybackStateChange, stopping tracker")
                when (currentPlayingTrackingConfig?.getPlayerType()) {
                    PlayerType.PODCAST -> {
                        podcastTracker?.cancelJobs()
                        podcastTracker = null
                    }
                    PlayerType.AUTOMATED, PlayerType.HUMAN, PlayerType.STANDALONE -> {
                        dispatchAudioTrackerEvent(AudioTrackerEvent.Complete)
                        currentPlayingTrackingConfig?.audioTracking?.stopTrackingProgress()
                    }
                    else -> {
                        // No op
                    }
                }
            }
            else -> {

            }
        }
    }

    private fun shouldStartListenHistory(config: AudioMediaConfig?): Boolean {
        if (config == null) {
            return false
        }

        return when (config.getPlayerType()) {
            PlayerType.PODCAST, PlayerType.HUMAN, PlayerType.AUTOMATED, PlayerType.STANDALONE -> true
            else -> false
        }
    }

    fun dispatchAudioTrackerEvent(event: AudioTrackerEvent) {
        _audioTrackerEvent.value = event
    }

    suspend fun addSingleMediaToQueue(audioMediaConfig: AudioMediaConfig) {
        audioManagerScope.launch {
            // Check Media is already in the queue. Return if it is.
            val mediaMetaDataItemsList = audioMediaSource.value?.getMedia(audioMediaConfig)
            if (mediaMetaDataItemsList != null) {
                return@launch
            }
            // Load and add if queue is already there. Otherwise play it as a SingleMedia.
            if ((audioMediaSource.value?.audioMediaConfigList?.list?.size ?: 0) > 0) {
                // Load media
                val newMediaItemsList = audioMediaSource.value?.loadMedia(audioMediaConfig)
                newMediaItemsList ?: return@launch
                // Add media to the config list and queue

                val entry = configListAudioPlaybackStateObservers.entries.firstOrNull { it.key == audioMediaConfig.id }
                observeConfigListAudioPlaybackStateChanges(entry?.key ?: audioMediaConfig.id, entry?.value)
                val audioMediaSourceCopy = audioMediaSource.value?.copy()
                audioMediaSourceCopy?.apply {
                    audioMediaConfigList.list.add(audioMediaConfig)
                    addMediaToCatalog(audioMediaConfig.id, newMediaItemsList)
                }
                with(audioMediaSourceCopy) {
                    _audioMediaSource.emit(audioMediaSourceCopy)
                    this?.whenReady {
                        newMediaItemsList.firstOrNull()?.let { mediaItem ->
                            musicServiceConnection.addMediaItem(mediaItem)
                            val currentItems = _mediaItems.value?.toMutableList() ?: mutableListOf()
                            currentItems.add(createMediaItemData(mediaItem))
                            _mediaItems.postValue(currentItems)
                        }
                    }
                }
            }
        }
    }

    suspend fun removeSingleMedia(audioMediaConfig: AudioMediaConfig) {
        audioManagerScope.launch {
            // Check Media is already in the queue. Return if it is not.
            val mediaItemList = audioMediaSource.value?.getMedia(audioMediaConfig)
            mediaItemList ?: return@launch
            // Remove media from the config list and queue
            audioMediaSource.value?.audioMediaConfigList?.list?.removeAll { it.id == audioMediaConfig.id }
            audioMediaSource.value?.removeMediaFromCatalog(audioMediaConfig.id)
            if (audioMediaSource.value?.audioMediaConfigList?.list.isNullOrEmpty()) {
                // Stop and shutdown the player when there are no elements.
                stopMedia()
            } else {
                audioMediaSource.value?.getCatalog()?.let { catalog ->
                    // Set the media items to the service connection
                    musicServiceConnection.setMediaItems(catalog, resetPosition = false)
                }
            }
        }
    }

    /**
     * Fires a content_listen event if the current tracking config is not null
     * @param conclusionState The state in which the audio concluded
     * @param currentPosition Optional param for when the current [Player]'s current position is not
     *                        accurate or available in the current scenario
     * @param duration Optional param for when the current [Player]'s duration is not accurate or
     *                 available in the current scenario
     */
    fun captureContentListenConclusion(
        conclusionState: ConclusionState = ConclusionState.OTHER,
        currentPosition: Long? = null,
        duration: Long? = null,
        contentType: String? = null
    ) {
        currentPlayingTrackingConfig?.let {
            userHistoryManager.captureContentListenConclusion(
                it.arcId ?: it.id,
                conclusionState,
                currentPosition ?: player?.currentPosition,
                duration ?: player?.duration,
                contentType ?: it.getPlayerType().name
            )
        }
    }

    suspend fun getAudioRecommendationList(excludeList: List<String>? = null): List<AudioMediaConfig> {
        return audioRecommendationsProvider.getAudioRecommendationList(excludeList)
    }

    fun addAudioToHistory(audioMediaConfig: AudioMediaConfig) {
        audioManagerScope.launch {
            audioRecommendationsProvider.addAudioToHistory(audioMediaConfig)
        }
    }

    fun isAudioDisabled(url: String): Boolean {
        return audioProvider.getDisabledAudioUrls().contains(url)
    }
}
