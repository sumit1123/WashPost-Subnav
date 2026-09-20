/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wapo.flagship.features.audio.service2.media

import CustomMediaSourceFactory
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION
import androidx.media3.common.Player.EVENT_PLAY_WHEN_READY_CHANGED
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.monthDayYearFormat
import com.wapo.flagship.features.audio.ClassicAudioManager2
import com.wapo.flagship.features.audio.PlayerType
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.audio.config.PodcastTracker
import com.wapo.flagship.features.audio.config2.AudioMediaConfig
import com.wapo.flagship.features.audio.models.MediaItemData
import com.wapo.flagship.features.audio.service2.media.extensions.METADATA_KEY_CONTENT_URL
import com.wapo.flagship.features.audio.service2.media.extensions.duration
import com.wapo.flagship.features.audio.service2.media.extensions.toUri
import com.wapo.flagship.features.audio.service2.media.library.BrowseTree
import com.wapo.flagship.features.audio.service2.media.library.BrowseTree.Companion.ROOT
import com.wapo.flagship.features.audio.service2.media.library.CONCAT_CHILDREN_DURATIONS
import com.wapo.flagship.features.audio.service2.media.library.CONCAT_CHILDREN_URIS
import com.wapo.flagship.features.audio.service2.media.library.IS_CONCAT2
import com.wapo.flagship.features.audio.service2.media.library.MusicSource
import com.wapo.flagship.features.audio.service2.media.library.from
import com.wapo.flagship.features.utils.PersonalizedPodcastHelper
import com.washingtonpost.userhistory.models.ConclusionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.net.URLEncoder
import java.util.Collections
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import androidx.core.net.toUri
import com.wapo.android.domain.repository.LoadRenderMetrics
import com.wapo.android.domain.repository.LoadRenderMetricsEvent
import com.wapo.flagship.features.audio.ads.ui.AudioAdsController
import com.wapo.flagship.features.audio.ads.ima.ImaService
import com.wapo.flagship.features.audio.ads.mappers.AudioAdAppConfigMapper.isVastEnabled
import com.wapo.flagship.features.audio.ads.repository.AudioAdsRepository
import com.wapo.flagship.features.audio.ads.util.AdPlayerUtils.putExtra
import com.wapo.flagship.features.audio.service2.media.extensions.METADATA_KEY_ADS_CONFIG
import com.washingtonpost.android.config.domain.manager.ConfigManager

/**
 * MusicService is a MediaLibraryService that provides access to a music library.
 * It uses ExoPlayer for playback and supports browsing and playing media items.
 * The service handles custom commands, media item retrieval, and session management.
 */
@UnstableApi
@AndroidEntryPoint
open class MusicService : MediaLibraryService() {

    @Inject
    lateinit var audioManager: ClassicAudioManager2

    @Inject
    lateinit var adsController: AudioAdsController

    @Inject
    lateinit var adsRepository: AudioAdsRepository

    @Inject
    lateinit var loadRenderMetrics: LoadRenderMetrics

    private var isConnectedToAuto: Boolean = false

    // more than one automotive controller can be connected at a time (for example the Android Auto
    // host and the car templates app), so track them individually instead of relying on a single
    // connect/disconnect pair to keep [isConnectedToAuto] accurate
    private val automotiveControllers = mutableSetOf<MediaSession.ControllerInfo>()

    private var isPauseOnAuto = false

    private var currentSection: String? = null

    // Using a private property for the actual MusicSource, and a setter that also updates browseTree
    private var _mediaSource: MusicSource? = null
    private var mediaSource: MusicSource?
        get() = _mediaSource
        set(value) {
            if (_mediaSource != value) { // Only update if it's actually different
                _mediaSource = value
                browseTree = if (value != null && value.size() > 0) BrowseTree(value) else null
                // Re-initialize browseTree with the new mediaSource
                browseTree?.onTreeUpdated {
                    // Notify subscribers that a tree node (like ROOT) has been updated
                    CoroutineScope(Dispatchers.Default).launch {
                        MusicServiceNotifier.browseTreeUpdatedFlow.emit(ROOT)
                    }
                }
                browseTree?.updateTree() // Ensure the tree is built with the new data

                mediaSession?.let { session ->
                    Logger.d(
                        TAG,
                        "AudioDebug: mediaSource changed. Notifying children for ROOT_ID."
                    )

                    // Get the actual count from the (now updated) browseTree
                    val rootChildrenCount = browseTree?.get(ROOT)?.size ?: 0
                    session.notifyChildrenChanged(ROOT, rootChildrenCount, null)
                }
            }
        }

    private lateinit var packageValidator: PackageValidator
    private lateinit var currentPlayer: Player

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
    private var mediaSession: MediaLibrarySession? = null
    private var currentPlaylistItems: List<MediaItem> = emptyList()
    private var currentMediaItemIndex: Int = 0

    private val audioAdsConfig get() = ConfigManager.getInstance().config.adsConfig.audio

    private var browseTree: BrowseTree? = null

    private val mAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener = PlayerEventListener()
    private lateinit var customCommands: List<SessionCommand>

    var cachedForYouList = listOf<AudioMediaConfig>()
    var cachedPodcastsList: List<AudioMediaConfig> = emptyList()
    var podcastSubscription: Subscription? = null

    var duration = 0L

    private val isReady = CompletableDeferred<Unit>()

    private fun prefetchData() {
        CoroutineScope(Dispatchers.IO).launch {
            cachedForYouList = audioManager.getAudioRecommendationList()
            cachedPodcastsList = getPodcasts()
            if (!isReady.isCompleted) {
                isReady.complete(Unit)
            }
        }
    }

    private var exoPlayer: ExoPlayer? = null

    private var sessionCallback: SessionCallback? = null


    private suspend fun observePodcasts(): List<AudioMediaConfig> =
        suspendCoroutine { continuation ->
            podcastSubscription = audioManager.audioProvider.getPodcastItems()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    continuation.resume(it)
                }, {
                    Logger.e("MusicService", "Error fetching podcast items: ${it.message}", it)
                    continuation.resume(emptyList())
                })
        }

    private suspend fun observePersonalizedPodcasts(): AudioMediaConfig? {
        return audioManager.audioProvider.getPersonalizedPodcasts()
    }

    private suspend fun getPodcasts(): MutableList<AudioMediaConfig> = coroutineScope {
        val podcastsDeferred = async { observePodcasts() }
        val generatedPodcastDeferred = async { observePersonalizedPodcasts() }
        val podcasts = podcastsDeferred.await()
        val generatedPodcast = generatedPodcastDeferred.await()
        val audioList = mutableListOf<AudioMediaConfig>()
        generatedPodcast?.let {
            audioList.add(generatedPodcast)
        }
        audioList.addAll(podcasts)
        audioList
    }

    @ExperimentalCoroutinesApi
    override fun onCreate() {
        super.onCreate()
        if (audioAdsConfig.configsByContentType.any { it.value.vastEnabled }) {
            ImaService.initializeIMASDK(this)
        }

        createPlayer()

        customCommands = listOf(
            SessionCommand(CMD_GET_PLAYBACK_SPEED, Bundle.EMPTY),
            SessionCommand(CMD_SET_PLAYBACK_SPEED, Bundle.EMPTY),
            SessionCommand(CMD_SEEK_TO, Bundle.EMPTY),
            SessionCommand(CMD_HIDE_NOTIFICATION, Bundle.EMPTY),
            SessionCommand(CUSTOM_ACTION_FORWARD_15, Bundle.EMPTY),
            SessionCommand(CUSTOM_ACTION_REWIND_15, Bundle.EMPTY)
        )

        // Create the MediaSession immediately.
        // onGetLibraryRoot and onGetChildren will handle the 'loading' state for content.
        createMediaSessionIfNeeded()

        // Collect latest value from audioManager.audioMediaSource.
        // The setter for `mediaSource` will handle updating `browseTree` and notifying clients.
        serviceScope.launch {
            audioManager.audioMediaSource
                .onEach { Logger.d(TAG, "AudioDebug: Emitted new source: $it") }
                .collect { newSource ->
                    newSource?.whenReady { isReady ->
                        if (isReady) {
                            // This assignment triggers the custom setter of `mediaSource`
                            // which then builds the browseTree and calls notifyChildrenChanged.
                            mediaSource = newSource
                            Logger.d(TAG, "AudioDebug: audioMediaSource collected new source.")
                        }
                    }
                }
        }

        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)

        setListener(MediaSessionServiceListener())
        audioManager.player = exoPlayer // Ensure audioManager has the correct player instance
        adsController.attachPlayer(currentPlayer)
        callStartForegroundIfNeeded()
    }

    private fun callStartForegroundIfNeeded() {
        val version = Build.VERSION.SDK_INT
        val android11 = Build.VERSION_CODES.R
        if (version <= android11) {
            val nm = NotificationManagerCompat.from(this@MusicService)
            ensureNotificationChannel(nm)
            val startNotification = NotificationCompat.Builder(this, NOW_PLAYING_CHANNEL_ID)
                .setContentTitle("Starting...")
                .setSmallIcon(androidx.media3.session.R.drawable.media3_notification_small_icon)
                .setOngoing(true)
                .build()
            startForeground(NOW_PLAYING_NOTIFICATION_ID, startNotification)
        }
    }

    override fun onRebind(intent: Intent?) {
        Logger.d(TAG, "AudioDebug: MusicService, reBind() called")
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Logger.d(TAG, "AudioDebug: MusicService, onUnbind() called")
        return super.onUnbind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Logger.d(TAG, "AudioDebug: MusicService, onTaskRemoved()")
        release()
    }

    override fun onDestroy() {
        Logger.d(TAG, "AudioDebug: MusicService, onDestroy()")
        release()
        super.onDestroy()
    }

    companion object {
        private const val ANDROID_AUTO_PACKAGE_NAME = "com.google.android.projection.gearhead"
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Logger.d(
            TAG,
            "AudioDebug: MusicService, onGetSession() called by controller: ${controllerInfo.packageName}, uid: ${controllerInfo.uid}"
        )
        createMediaSessionIfNeeded()
        return mediaSession
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, startInForegroundRequired)
        Logger.d(
            TAG,
            "AudioDebug: onUpdateNotification, startInForegroundRequired=$startInForegroundRequired"
        )
    }

    private fun saveRecentSongToStorage() {
        if (currentPlaylistItems.isEmpty() || currentMediaItemIndex < 0 || currentMediaItemIndex >= currentPlaylistItems.size) {
            return
        }
        val currentMediaItem = currentPlaylistItems[currentMediaItemIndex]
        val position = currentPlayer.currentPosition

        serviceScope.launch {
            PersistentStorage.saveRecentSong(
                currentMediaItem,
                position,
                applicationContext
            )
        }
    }

    private fun release() {

        audioManager.stopMedia()
        audioManager.releaseMusicServiceConnection()
        audioManager.releaseAdsController()
        _mediaSource = null
        exoPlayer?.run {
            stop()
            clearMediaItems()
            removeListener(playerListener)
            release()
        }
        exoPlayer = null

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        clearListener()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        serviceJob.cancel()
        stopSelf()
    }

    private fun createPlayer() {
        exoPlayer = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(15_000)
            .setSeekForwardIncrementMs(15_000)
            .setMediaSourceFactory(CustomMediaSourceFactory(this))
            .build()
            .apply {
                setAudioAttributes(mAudioAttributes, true)
                setHandleAudioBecomingNoisy(true)
                addListener(playerListener)
            }

        currentPlayer = exoPlayer!!
        audioManager.player = currentPlayer
        adsController.attachPlayer(currentPlayer)
    }

    private fun createMediaSessionIfNeeded() {
        if (mediaSession == null) {
            if (exoPlayer == null) {
                createPlayer()
            }
            sessionCallback = SessionCallback()
            mediaSession = MediaLibrarySession.Builder(this, exoPlayer!!, sessionCallback!!).apply {
                getPendingIntentToLaunchUI()?.let { setSessionActivity(it) }
            }.build()
        }
    }

    fun refreshForYouList(
        current: List<AudioMediaConfig>,
        incoming: List<AudioMediaConfig>
    ): MutableList<AudioMediaConfig> {
        if (incoming.isEmpty()) return current.toMutableList()

        // want to maintain the order of the For You list in Auto as
        // 1. pinned items at the top
        // 2. unpinned items that are already in the current list but not listened to yet
        // 3. new unpinned items at the end
        // the same audio can appear in both lists (the For You feed is cached and
        // shared with the phone), so track what has been added to avoid duplicate items
        val result = mutableListOf<AudioMediaConfig>()
        val seen = mutableSetOf<String>()

        fun addIfNew(item: AudioMediaConfig) {
            val key = item.contentUrl ?: item.id ?: return
            if (seen.add(key)) result.add(item)
        }

        for (item in incoming) {
            if (item.pinned == true) addIfNew(item)
        }

        for (item in current) {
            if (item.pinned != true) addIfNew(item)
        }

        for (item in incoming) {
            if (item.pinned != true) addIfNew(item)
        }

        return result
    }

    private inner class SessionCallback : MediaLibrarySession.Callback {

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            Logger.d(
                TAG,
                "AudioDebug: MusicService, onConnect() called by controller: ${controller.packageName}, uid: ${controller.uid}"
            )

            if (isAutomotiveController(controller)) {
                prefetchData()
                automotiveControllers.add(controller)
                isConnectedToAuto = true
            }

            val availableSessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
            availableSessionCommands.remove(SessionCommand.COMMAND_CODE_LIBRARY_SEARCH)
            availableSessionCommands.remove(SessionCommand.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT)
            for (sessionCommand in customCommands) {
                availableSessionCommands.add(sessionCommand)
            }
            if (isCarTemplatesController(controller)) {
                availableSessionCommands.add(
                    SessionCommand(CMD_GET_MEDIA_SESSION_TOKEN, Bundle.EMPTY)
                )
            }
            Logger.d(
                TAG,
                "AudioDebug: Connection accepted for controller: ${controller.packageName}"
            )
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands.build())
                .build()
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            super.onPostConnect(session, controller)
            mediaSession?.setCustomLayout(
                controller,
                listOf(
                    CommandButton.Builder(CommandButton.ICON_SKIP_BACK_15)
                        .setDisplayName(CUSTOM_ACTION_REWIND_15_DISPLAY_NAME)
                        .setSessionCommand(SessionCommand(CUSTOM_ACTION_REWIND_15, Bundle.EMPTY))
                        .build(),
                    CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_15)
                        .setDisplayName(CUSTOM_ACTION_FORWARD_15_DISPLAY_NAME)
                        .setSessionCommand(SessionCommand(CUSTOM_ACTION_FORWARD_15, Bundle.EMPTY))
                        .build()
                )
            )
        }

        override fun onDisconnected(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ) {
            Logger.d(
                TAG,
                "AudioDebug: MusicService, onDisconnected() called by controller: ${controller.packageName}, uid: ${controller.uid}"
            )
            // Optionally handle disconnection logic here, like cleaning up resources or saving state
            // only clear the flag once every automotive controller is gone, otherwise an unrelated
            // controller disconnecting turns off car behavior while a car is still connected
            val wasAutomotive = automotiveControllers.remove(controller)
            if (wasAutomotive) {
                isConnectedToAuto = automotiveControllers.isNotEmpty()
            }
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            Logger.d(
                TAG,
                "AudioDebug: MusicService, onCustomCommand(), customCommand=${customCommand.customAction}"
            )
            return when (customCommand.customAction) {

                CUSTOM_ACTION_REWIND_15 -> {
                    val currentPosition = currentPlayer.currentPosition
                    currentPlayer.seekTo(currentPosition - 15000) // 15 seconds back
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CUSTOM_ACTION_FORWARD_15 -> {
                    val currentPosition = currentPlayer.currentPosition
                    currentPlayer.seekTo(currentPosition + 15000) // 15 seconds forward
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CMD_GET_PLAYBACK_SPEED -> {
                    val speed = getPlaybackSpeed()
                    Futures.immediateFuture(
                        SessionResult(
                            SessionResult.RESULT_SUCCESS,
                            Bundle().also { it.putFloat(PLAYBACK_SPEED_VALUE, speed) })
                    )
                }

                CMD_SET_PLAYBACK_SPEED -> {
                    val speed = args.getFloat(PLAYBACK_SPEED_VALUE, 1f)
                    setPlaybackSpeed(speed)
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CMD_SEEK_TO -> {
                    val seekValue = args.getLong(SEEK_TO_VALUE, -1L)
                    if (seekValue > -1) {
                        setSeekValue(seekValue)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    } else {
                        Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                    }
                }

                CMD_HIDE_NOTIFICATION -> {
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CMD_GET_MEDIA_SESSION_TOKEN -> {
                    if (!isCarTemplatesController(controller)) {
                        Futures.immediateFuture(
                            SessionResult(SessionError.ERROR_PERMISSION_DENIED)
                        )
                    } else {
                        val resultExtras = Bundle().apply {
                            putParcelable(MEDIA_SESSION_TOKEN_KEY, session.platformToken)
                        }
                        Futures.immediateFuture(
                            SessionResult(SessionResult.RESULT_SUCCESS, resultExtras)
                        )
                    }
                }

                else -> Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE)) // Corrected default return
            }
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<androidx.media3.common.MediaItem>> {
            Logger.d(TAG, "AudioDebug: MusicService, onGetLibraryRoot()")

            // If mediaSource is not yet loaded, indicate that content is not yet ready.
            if (mediaSource == null && !isAutomotiveController(browser)) {
                Logger.d(
                    TAG,
                    "AudioDebug: MediaSource is null in onGetLibraryRoot, returning NOT_READY."
                )
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }

            val mediaItem = buildMediaItem(
                title = if (isAutomotiveController(browser)) "Auto root" else "root",
                mediaId = ROOT,
                isPlayable = true,
                isBrowsable = true,
                mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED
            )
            return Futures.immediateFuture(LibraryResult.ofItem(mediaItem, params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            Logger.d(TAG, "AudioDebug: MusicService, onGetItem(), mediaId=$mediaId")

            // If mediaSource is not yet loaded, indicate that content is not yet ready.
            if (mediaSource == null && !isAutomotiveController(browser)) {
                Logger.d(TAG, "AudioDebug: MediaSource is null in onGetItem, returning NOT_READY.")
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }

            val itemToPlay = browseTree?.getItem(mediaId) // Use browseTree's getItem
            if (itemToPlay == null) {
                Logger.d(TAG, "Content not found: MediaID=$mediaId")
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }
            return Futures.immediateFuture(LibraryResult.ofItem(itemToPlay, /* params= */ null))
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            if (isAutomotiveController(controller)) {

                if (mediaItems.isEmpty()) {
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0)
                    )
                }

                val updatedMediaItems: List<MediaItem>?

                if (currentPlayer.currentMediaItem != null) {
                    val mediaId = currentPlayer.currentMediaItem?.mediaId
                    val item = cachedPodcastsList.find { it.id == mediaId }
                        ?: cachedForYouList.find { it.id == mediaId }
                    if (item != null && item in cachedForYouList) {
                        // Do not remove it if the new audio item clicked is same as the one being played
                        // This is to make sure the content is still available to play when clicked on again
                        if (mediaId != mediaItems[0].mediaId && item.pinned != true) {
                            var updatedList = cachedForYouList - item
                            cachedForYouList = emptyList()
                            this@MusicService.mediaSession?.notifyChildrenChanged(
                                FOR_YOU_SECTION_ID, cachedForYouList.size, null
                            )
                            if (updatedList.size < 4) {
                                val oldList = updatedList.mapNotNull { it.contentUrl }
                                val newList =
                                    runBlocking { audioManager.getAudioRecommendationList(oldList) }
                                updatedList = refreshForYouList(updatedList, newList)
                            }
                            cachedForYouList = updatedList
                            this@MusicService.mediaSession?.notifyChildrenChanged(
                                FOR_YOU_SECTION_ID, cachedForYouList.size, null
                            )
                        }
                    }
                    if (item != null) {
                        item.audioTracking?.stopTrackingProgress()
                        item.audioTracking?.reset()
                    }
                }

                val mediaItem = mediaItems[0]
                val selectedPodcast = cachedPodcastsList.firstOrNull { it.id == mediaItem.mediaId }
                var index = 0
                if (selectedPodcast != null) {
                    updatedMediaItems =
                        when (selectedPodcast.audioType) {
                            PersonalizedPodcastHelper.PersonalizedPodcastItemType.ONBOARDING -> {
                                val generatedPodcast = runBlocking {
                                    audioManager.audioProvider.generatePersonalizedPodcast()
                                }
                                generatedPodcast?.let {
                                    val updatedList =
                                        cachedPodcastsList.take(1) + it + cachedPodcastsList.drop(1)
                                    cachedPodcastsList = updatedList
                                    index = cachedPodcastsList.indexOf(selectedPodcast)
                                    cachedPodcastsList.map { item ->
                                        createPlayableMediaItem(item)
                                    }
                                } ?: cachedPodcastsList.map { item ->
                                    createPlayableMediaItem(item)
                                }
                            }

                            PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER -> {
                                cachedPodcastsList =
                                    cachedPodcastsList.filter { it.id != selectedPodcast.id }
                                this@MusicService.mediaSession?.notifyChildrenChanged(
                                    PODCAST_SECTION_ID, cachedPodcastsList.size, null
                                )
                                val generatedPodcast = runBlocking {
                                    audioManager.audioProvider.generatePersonalizedPodcast()
                                }
                                generatedPodcast?.let {
                                    cachedPodcastsList = listOf(it) + cachedPodcastsList
                                    cachedPodcastsList.map { item ->
                                        createPlayableMediaItem(
                                            item
                                        )
                                    }
                                } ?: run {
                                    val playableList =
                                        cachedPodcastsList.filter { it.streamUrl != null }
                                    if (playableList.isEmpty()) {
                                        index = 0
                                        emptyList()
                                    } else {
                                        val computedIndex =
                                            playableList.indexOfFirst { it.id == mediaItem.mediaId }
                                        index = if (computedIndex >= 0) computedIndex else 0
                                        cachedPodcastsList.map { item ->
                                            createPlayableMediaItem(item)
                                        }
                                    }
                                }
                            }

                            else -> {
                                index = cachedPodcastsList.filter { it.streamUrl != null }
                                    .indexOfFirst { it.id == mediaItem.mediaId }
                                cachedPodcastsList.filter { it.streamUrl != null }.map {
                                    createPlayableMediaItem(it)
                                }
                            }
                        }

                    this@MusicService.mediaSession?.notifyChildrenChanged(
                        PODCAST_SECTION_ID, cachedPodcastsList.size, null
                    )

                    audioManager.captureContentListenConclusion(
                        ConclusionState.CONTENT_SELECT,
                        duration = cachedPodcastsList[index].duration,
                        contentType = PlayerType.PODCAST.name
                    )

                    val podcast = cachedPodcastsList[index]

                    PodcastTracker(
                        audioManager.audioProvider,
                        mediaItem.toMediaItemData(podcast.getPlayerType(), podcast),
                        cachedPodcastsList[index].audioTracking
                    ).also {
                        it.player = exoPlayer
                        it.progressThreshold = index
                        it.startTracking()
                    }
                    if (currentSection == FOR_YOU_SECTION_NAME) {
                        audioManager.audioProvider.trackCarPlayOpenEvent(
                            PODCAST_SECTION_ID,
                            isTopRibbon = true
                        )
                    }
                    currentSection = PODCAST_SECTION_NAME
                } else if (cachedForYouList.find { it.id == mediaItem.mediaId } != null) {
                    index = cachedForYouList.indexOfFirst { it.id == mediaItem.mediaId }
                    audioManager.captureContentListenConclusion(
                        ConclusionState.CONTENT_SELECT,
                        duration = cachedPodcastsList[index].duration,
                        contentType = cachedForYouList[index].getPlayerType().name
                    )
                    if (index == cachedForYouList.size - 1) {
                        val oldList = cachedForYouList.mapNotNull { it.contentUrl }
                        val newList =
                            runBlocking { audioManager.getAudioRecommendationList(oldList) }
                        cachedForYouList = refreshForYouList(cachedForYouList, newList)
                    }
                    this@MusicService.mediaSession?.notifyChildrenChanged(
                        FOR_YOU_SECTION_ID, cachedForYouList.size, null
                    )
                    cachedForYouList[index].resolveAdState(
                        audioManager.audioProvider.getAudioAdInterval(),
                        audioManager.audioProvider.shouldSuppressAds()
                    )
                    if (cachedForYouList[index].getPlayAd()) {
                        val updatedMediaItem = loadForYouAudio(cachedForYouList[index])
                        if (updatedMediaItem != null) {
                            val updatedList =
                                cachedForYouList.map { createPlayableMediaItem(it) }
                                    .toMutableList()
                            updatedList[index] = updatedMediaItem
                            updatedMediaItems = updatedList
                        } else {
                            updatedMediaItems = cachedForYouList.map {
                                createPlayableMediaItem(it)
                            }
                        }
                    } else {
                        updatedMediaItems = cachedForYouList.map {
                            createPlayableMediaItem(it)
                        }
                    }
                    cachedForYouList[index].audioTracking?.apply {
                        setIsRollThrough(false)
                        updateProgressThreshold(index)
                        onStart()
                        startTrackingTtsProgress(currentPlayer)
                    }
                    audioManager.addAudioToHistory(cachedForYouList[index])
                    if (currentSection == PODCAST_SECTION_NAME) {
                        audioManager.audioProvider.trackCarPlayOpenEvent(
                            FOR_YOU_SECTION_ID,
                            cachedForYouList[index].audioTracking?.getAudioTrackingInfo(),
                            true
                        )
                    }
                    currentSection = FOR_YOU_SECTION_NAME

                } else {
                    val item = browseTree?.getItem(mediaItem.mediaId)
                    updatedMediaItems = if (item != null) {
                        listOf(item)
                    } else {
                        null
                    }
                }

                val safeStartIndex = if (updatedMediaItems.isNullOrEmpty()) {
                    C.INDEX_UNSET
                } else {
                    index.coerceIn(0, updatedMediaItems.lastIndex)
                }
                val safeStartPositionMs = if (updatedMediaItems.isNullOrEmpty()) {
                    C.TIME_UNSET
                } else {
                    startPositionMs
                }

                if (!updatedMediaItems.isNullOrEmpty()) {
                    currentPlaylistItems = updatedMediaItems
                    currentMediaItemIndex = safeStartIndex
                    if (audioManager.musicServiceConnection.playWhenReady.value == true) {
                        audioManager.disableNowPlayingItem()
                        isPauseOnAuto = true
                    }
                }
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        updatedMediaItems ?: emptyList(),
                        safeStartIndex,
                        safeStartPositionMs
                    )
                )
            } else if (isConnectedToAuto) {
                val updatedItems: MutableList<MediaItem> = mutableListOf()
                mediaItems.forEach {
                    val resizedImageUrl =
                        audioManager.audioProvider.getImageResizerUrlForAuto(it.mediaMetadata.artworkUri.toString())
                    val item = it.buildUpon().setMediaMetadata(
                        it.mediaMetadata.buildUpon().setArtworkUri(resizedImageUrl.toUri()).build()
                    ).build()
                    updatedItems.add(item)
                }
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        updatedItems,
                        startIndex,
                        startPositionMs
                    )
                )
            } else if (mediaItems.isNotEmpty() && mediaItems.get(0).mediaMetadata.extras?.getBoolean(
                    IS_CONCAT2,
                    false
                ) == true
            ) {
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        mediaItems,
                        startIndex,
                        startPositionMs
                    )
                )
            }
            return super.onSetMediaItems(
                mediaSession,
                controller,
                mediaItems,
                startIndex,
                startPositionMs
            )
        }


        override fun onSubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            Logger.d(TAG, "AudioDebug: MusicService, onSubscribe(), parentId=$parentId")

            // If mediaSource is null, or browseTree is not ready, return error.
            if (mediaSource == null && !isAutomotiveController(browser)) {
                Logger.d(
                    TAG,
                    "AudioDebug: MediaSource is null in onSubscribe, returning NOT_READY."
                )
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }
            if (browseTree?.get(parentId)
                    .isNullOrEmpty() && parentId != ROOT && !isAutomotiveController(browser)
            ) {
                // If it's not the root and has no children, maybe it's not browsable or invalid
                Logger.d(
                    TAG,
                    "AudioDebug: onSubscribe: ParentId '$parentId' has no children or is not browsable."
                )
                // You might choose RESULT_ERROR_BAD_VALUE or RESULT_ERROR_NOT_SUPPORTED depending on your logic
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
            }

            // In Media3, onSubscribe primarily registers the browser's interest.
            // The actual children are returned via onGetChildren.
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {

            val isRootRequestFromAuto = isAutomotiveController(browser) &&
                (parentId == PODCAST_SECTION_ID || parentId == FOR_YOU_SECTION_ID || parentId == ROOT)

            if (isRootRequestFromAuto) {
                val loadRenderEventValue = if (parentId == PODCAST_SECTION_ID) {
                    LoadRenderMetricsEvent.AndroidAutoPodcastRenderEvent
                } else {
                    LoadRenderMetricsEvent.AndroidAutoForYouRenderEvent
                }
                loadRenderMetrics.startLoadRenderMetrics(loadRenderEventValue)
            }

            Logger.d(
                TAG,
                "AudioDebug: MusicService, onGetChildren(), parentId=$parentId, page=$page, pageSize=$pageSize"
            )

            // If mediaSource is null, return an error indicating content is not ready.
            if (mediaSource == null && !isAutomotiveController(browser)) {
                Logger.d(
                    TAG,
                    "AudioDebug: MediaSource is null in onGetChildren, returning NOT_READY."
                )
                return Futures.immediateFuture(
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                )
            }
            val backgroundExecutor = Dispatchers.IO.asExecutor()
            return Futures.submitAsync({
                runBlocking { isReady.await() }

                val items = when {
                    parentId == ROOT && isAutomotiveController(browser) -> {
                        if (currentSection != FOR_YOU_SECTION_NAME) {
                            audioManager.audioProvider.trackCarPlayOpenEvent(
                                FOR_YOU_SECTION_ID,
                                cachedForYouList[0].audioTracking?.getAudioTrackingInfo(),
                                false
                            )
                            currentSection = FOR_YOU_SECTION_NAME
                        }
                        val result = listOf(
                            createBrowsableMediaItem(FOR_YOU_SECTION_ID, FOR_YOU_SECTION_NAME),
                            createBrowsableMediaItem(PODCAST_SECTION_ID, PODCAST_SECTION_NAME)
                        )
                        if (isRootRequestFromAuto) {
                            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.AndroidAutoForYouRenderEvent)
                        }
                        result
                    }

                    parentId == FOR_YOU_SECTION_ID -> {
                        val result = if (cachedForYouList.isNotEmpty()) {
                            cachedForYouList.map { audioMediaConfig ->
                                val audioItem = createPlayableMediaItem(audioMediaConfig)
                                runBlocking {
                                    withContext(Dispatchers.Main) {
                                        val existingIds =
                                            (0 until currentPlayer.mediaItemCount).mapNotNull {
                                                currentPlayer.getMediaItemAt(it).mediaId
                                            }
                                        if (currentPlayer.mediaItemCount != 0 && !existingIds.contains(
                                                audioItem.mediaId
                                            )
                                        ) {
                                            currentPlayer.addMediaItem(audioItem)
                                        }
                                    }
                                }
                                audioItem
                            }
                        } else {
                            emptyList()
                        }
                        if (isRootRequestFromAuto) {
                            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.AndroidAutoForYouRenderEvent)
                        }
                        result
                    }

                    parentId == PODCAST_SECTION_ID -> cachedPodcastsList.map {
                        val result = createPlayableMediaItem(it)
                        if (isRootRequestFromAuto) {
                            loadRenderMetrics.stopLoadRenderMetrics(LoadRenderMetricsEvent.AndroidAutoPodcastRenderEvent)
                        }
                        result
                    }

                    else -> browseTree?.get(parentId) ?: Collections.emptyList()
                }

                val start = page * pageSize
                val end = (start + pageSize).coerceAtMost(items.size)
                val pagedChildren = if (start < end) {
                    items.subList(start, end)
                } else {
                    emptyList()
                }

                Futures.immediateFuture(
                    LibraryResult.ofItemList(
                        ImmutableList.copyOf(pagedChildren),
                        params
                    )
                )
            }, backgroundExecutor)
        }

        private fun createBrowsableMediaItem(id: String, title: String): MediaItem {
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                ).build()
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            Logger.d(
                TAG,
                "AudioDebug: MusicService, onAddMediaItems(), mediaItemsSize=${mediaItems.size}"
            )
            // If mediaSource is null and is not an automotive controller, fetch data from audioMediaSource.
            if (mediaSource == null && !isAutomotiveController(controller)) {
                serviceScope.launch {
                    audioManager.audioMediaSource.collect { source ->
                        mediaSource = source
                    }
                }
            }

            val updatedMediaItems: List<MediaItem> =
                mediaItems.map { mediaItem ->
                    if (mediaItem.requestMetadata.searchQuery != null)
                        getMediaItemFromSearchQuery(mediaItem.requestMetadata.searchQuery!!)
                    else
                        mediaSource?.find { item -> item.mediaId == mediaItem.mediaId } ?: mediaItem
                }
            return Futures.immediateFuture(updatedMediaItems.toMutableList())
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            // Return an empty playlist to disable resume gracefully
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L)
            )
        }

        private fun getMediaItemFromSearchQuery(query: String): MediaItem {
            Logger.d(TAG, "AudioDebug: MusicService, getMediaItemFromSearchQuery(), query=${query}")
            val mediaTitle =
                if (query.startsWith("play ", ignoreCase = true)) {
                    query.drop(5)
                } else {
                    query
                }

            // Ensure mediaSource is not null before searching
            val mediaItem = mediaSource?.search(mediaTitle, Bundle.EMPTY)?.firstOrNull()
                ?: mediaSource?.first() // Fallback to first item if search fails

            // Handle case where no media item is found at all.
            return mediaItem ?: throw IllegalStateException("No media item found for query: $query")
        }

        private fun buildMediaItem(
            title: String,
            mediaId: String,
            isPlayable: Boolean,
            isBrowsable: Boolean,
            mediaType: @MediaMetadata.MediaType Int,
            subtitleConfigurations: List<MediaItem.SubtitleConfiguration> = mutableListOf(),
            album: String? = null,
            artist: String? = null,
            genre: String? = null,
            sourceUri: Uri? = null,
            imageUri: Uri? = null
        ): MediaItem {
            val metadata =
                MediaMetadata.Builder()
                    .setAlbumTitle(album)
                    .setTitle(title)
                    .setArtist(artist)
                    .setGenre(genre)
                    .setIsBrowsable(isBrowsable)
                    .setIsPlayable(isPlayable)
                    .setArtworkUri(imageUri)
                    .setMediaType(mediaType)
                    .build()

            return MediaItem.Builder()
                .setMediaId(mediaId)
                .setSubtitleConfigurations(subtitleConfigurations)
                .setMediaMetadata(metadata)
                .setUri(sourceUri)
                .build()
        }
    }

    @UnstableApi
    private inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    if (playbackState == Player.STATE_READY) {
                        saveRecentSongToStorage()
                    }
                }

                Player.STATE_ENDED -> {
                    if (isConnectedToAuto) {
                        val item =
                            cachedPodcastsList.find { it.id == currentPlayer.currentMediaItem?.mediaId }
                                ?: cachedForYouList.find { it.id == currentPlayer.currentMediaItem?.mediaId }
                        if (item != null) {
                            item.audioTracking?.apply {
                                onComplete()
                                stopTrackingProgress()
                                reset()
                            }
                        }
                    } else {
                        audioManager.captureContentListenConclusion(ConclusionState.END)
                    }
                }

                else -> {
                }
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(EVENT_POSITION_DISCONTINUITY)
                || events.contains(EVENT_MEDIA_ITEM_TRANSITION)
                || events.contains(EVENT_PLAY_WHEN_READY_CHANGED)
            ) {

                currentMediaItemIndex = if (currentPlaylistItems.isNotEmpty()) {
                    Util.constrainValue(
                        player.currentMediaItemIndex,
                        /* min= */ 0,
                        /* max= */ currentPlaylistItems.size - 1
                    )
                } else 0
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
            if (isConnectedToAuto) {
                val mediaItem = currentPlayer.currentMediaItem
                val podcastAudioMediaConfig =
                    cachedPodcastsList.find { it.id == mediaItem?.mediaId }
                val forYouAudioMediaConfig = cachedForYouList.find { it.id == mediaItem?.mediaId }
                // Handle placeholder items explicitly and exit early.
                if (mediaItem?.mediaId == PersonalizedPodcastHelper.PersonalizedPodcastItemType.PLACEHOLDER) {
                    currentPlayer.removeMediaItem(0)
                    return
                }
                // Only compute index when we have a matching podcast config.
                val index = podcastAudioMediaConfig?.let { cachedPodcastsList.indexOf(it) } ?: -1

                if (reason == Player.DISCONTINUITY_REASON_SEEK
                    && oldPosition.mediaItemIndex == newPosition.mediaItemIndex
                ) {
                    val seekDelta = newPosition.positionMs - oldPosition.positionMs
                    if (abs(seekDelta) == 15000L) {
                        // This was a 15-second seek within the same media item
                        // No need to modify tracking state as we're staying in the same item
                        return
                    }
                }

                if (podcastAudioMediaConfig != null) {
                    if (index > -1) {
                        cachedPodcastsList[index].audioTracking?.apply {
                            if (reason == Player.DISCONTINUITY_REASON_SEEK
                                && newPosition.mediaItemIndex > oldPosition.mediaItemIndex
                            ) {
                                // Next button clicked
                                audioManager.captureContentListenConclusion(
                                    ConclusionState.NEXT,
                                    oldPosition.contentPositionMs,
                                    cachedPodcastsList[index].duration,
                                    PlayerType.PODCAST.name
                                )
                                removeOnboardingPodcastIfNeeded(index)
                            } else if (reason == Player.DISCONTINUITY_REASON_SEEK
                                && newPosition.mediaItemIndex < oldPosition.mediaItemIndex
                            ) {
                                // Previous button clicked
                                audioManager.captureContentListenConclusion(
                                    ConclusionState.PREVIOUS,
                                    oldPosition.contentPositionMs,
                                    cachedPodcastsList[index].duration,
                                    PlayerType.PODCAST.name
                                )
                            }
                            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                                audioManager.captureContentListenConclusion(
                                    ConclusionState.END,
                                    oldPosition.contentPositionMs,
                                    oldPosition.contentPositionMs,
                                    PlayerType.PODCAST.name
                                )
                                removeOnboardingPodcastIfNeeded(index)
                            }
                        }
                    }
                } else if (forYouAudioMediaConfig != null) {
                    val index = cachedForYouList.indexOf(forYouAudioMediaConfig) - 1
                    if (index > -1) {
                        cachedForYouList[index].audioTracking?.apply {
                            if (reason == Player.DISCONTINUITY_REASON_SEEK
                                && newPosition.mediaItemIndex > oldPosition.mediaItemIndex
                            ) {
                                // Next button clicked
                                audioManager.captureContentListenConclusion(
                                    ConclusionState.NEXT,
                                    oldPosition.contentPositionMs,
                                    cachedForYouList[index].duration,
                                    cachedForYouList[index].getPlayerType().name
                                )
                                setIsRollThrough(false)
                                onSkipForward()
                                stopTrackingProgress()
                                reset()
                                if (cachedForYouList[index].pinned != true) {
                                    cachedForYouList = cachedForYouList - cachedForYouList[index]
                                    currentPlaylistItems = cachedForYouList.map {
                                        createPlayableMediaItem(it)
                                    }
                                    currentPlayer.removeMediaItem(index)
                                }
                                if (cachedForYouList.size < 4 || index == cachedForYouList.size - 1) {
                                    val currentList = cachedForYouList
                                    val oldList = currentList.mapNotNull { it.contentUrl }
                                    val newList = runBlocking {
                                        audioManager.getAudioRecommendationList(oldList)
                                    }
                                    cachedForYouList = refreshForYouList(currentList, newList)
                                }
                                mediaSession?.controllerForCurrentRequest?.let {
                                    mediaSession?.notifyChildrenChanged(
                                        FOR_YOU_SECTION_ID, cachedForYouList.size, null
                                    )
                                }
                                if (index < cachedForYouList.size) {
                                    cachedForYouList[index].resolveAdState(
                                        audioManager.audioProvider.getAudioAdInterval(),
                                        audioManager.audioProvider.shouldSuppressAds()
                                    )
                                    if (cachedForYouList[index].getPlayAd()) {
                                        val updatedMediaItem =
                                            loadForYouAudio(cachedForYouList[index])
                                        if (updatedMediaItem != null) {
                                            currentPlayer.replaceMediaItem(index, updatedMediaItem)
                                        }
                                    }
                                    cachedForYouList[index].audioTracking?.apply {
                                        setIsRollThrough(false)
                                        updateProgressThreshold(index)
                                        onStart()
                                        startTrackingTtsProgress(currentPlayer)
                                    }
                                    audioManager.addAudioToHistory(cachedForYouList[index])
                                }
                            } else if (reason == Player.DISCONTINUITY_REASON_SEEK
                                && newPosition.mediaItemIndex < oldPosition.mediaItemIndex
                            ) {
                                // Previous button clicked
                                audioManager.captureContentListenConclusion(
                                    ConclusionState.PREVIOUS,
                                    oldPosition.contentPositionMs,
                                    cachedForYouList[index].duration,
                                    cachedForYouList[index].getPlayerType().name
                                )
                                setIsRollThrough(false)
                                onSkipBackward()
                                stopTrackingProgress()
                                reset()
                                if (index > 0) {
                                    cachedForYouList[index - 1].audioTracking?.apply {
                                        setIsRollThrough(false)
                                        updateProgressThreshold(index - 1)
                                        onStart()
                                        startTrackingTtsProgress(currentPlayer)
                                    }
                                }
                            }
                            if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                                audioManager.captureContentListenConclusion(
                                    ConclusionState.END,
                                    oldPosition.contentPositionMs,
                                    oldPosition.contentPositionMs,
                                    cachedForYouList[index].getPlayerType().name
                                )
                                onComplete()
                                stopTrackingProgress()
                                reset()
                                var updatedList = cachedForYouList
                                if (cachedForYouList[index].pinned != true) {
                                    updatedList = cachedForYouList - cachedForYouList[index]
                                    currentPlayer.removeMediaItem(index)
                                }
                                currentPlaylistItems = updatedList.map {
                                    createPlayableMediaItem(
                                        it
                                    )
                                }
                                cachedForYouList = emptyList()
                                mediaSession?.notifyChildrenChanged(
                                    FOR_YOU_SECTION_ID, cachedForYouList.size, null
                                )
                                if (updatedList.size < 4 || index == updatedList.size - 1) {
                                    val oldList = updatedList.mapNotNull { it.contentUrl }
                                    val newList = runBlocking {
                                        audioManager.getAudioRecommendationList(oldList)
                                    }
                                    updatedList = refreshForYouList(updatedList, newList)
                                }
                                cachedForYouList = updatedList
                                mediaSession?.notifyChildrenChanged(
                                    FOR_YOU_SECTION_ID, cachedForYouList.size, null
                                )
                                if (index < cachedForYouList.size) {
                                    cachedForYouList[index].resolveAdState(
                                        audioManager.audioProvider.getAudioAdInterval(),
                                        audioManager.audioProvider.shouldSuppressAds()
                                    )
                                    if (cachedForYouList[index].getPlayAd()) {
                                        val updatedMediaItem =
                                            loadForYouAudio(cachedForYouList[index])
                                        if (updatedMediaItem != null) {
                                            currentPlayer.replaceMediaItem(index, updatedMediaItem)
                                            currentPlayer.prepare()
                                        }
                                    }
                                    cachedForYouList[index].audioTracking?.apply {
                                        setIsRollThrough(true)
                                        updateProgressThreshold(index)
                                        onStart()
                                        startTrackingTtsProgress(currentPlayer)
                                    }
                                    audioManager.addAudioToHistory(cachedForYouList[index])
                                }
                            }
                        }
                    }
                }
            }
            if (!isConnectedToAuto && reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                audioManager.captureContentListenConclusion(
                    ConclusionState.END,
                    oldPosition.contentPositionMs,
                    oldPosition.contentPositionMs
                )
            }
        }

        private fun removeOnboardingPodcastIfNeeded(index: Int) {
            if (index == 1 && cachedPodcastsList[0].audioType == PersonalizedPodcastHelper.PersonalizedPodcastItemType.ONBOARDING && index + 1 < cachedPodcastsList.size && cachedPodcastsList[index + 1].audioType == PersonalizedPodcastHelper.PersonalizedPodcastItemType.AUDIO_PODCAST) {
                var updatedList = cachedPodcastsList
                cachedPodcastsList = emptyList()
                mediaSession?.notifyChildrenChanged(
                    PODCAST_SECTION_ID, cachedPodcastsList.size, null
                )
                updatedList = updatedList - updatedList[0]
                cachedPodcastsList = updatedList
                currentPlayer.removeMediaItem(0)
                mediaSession?.notifyChildrenChanged(
                    PODCAST_SECTION_ID, cachedPodcastsList.size, null
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (isPauseOnAuto && isConnectedToAuto) {
                audioManager.musicServiceConnection.getPlayer()?.play()
                isPauseOnAuto = false
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            var message = R.string.generic_error;
            val articleUrl =
                if (currentMediaItemIndex > -1 && currentMediaItemIndex < currentPlaylistItems.size)
                    currentPlaylistItems[currentMediaItemIndex].mediaMetadata.extras?.getString(
                        METADATA_KEY_CONTENT_URL, null
                    )
                else null
            Logger.e(TAG, "Player error: " + error.errorCodeName + " (" + error.errorCode + ")");
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS
                || error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
            ) {
                message = R.string.error_media_not_found;
            }

            EventLog.Builder().apply {
                setMessage("MusicService Playback Error")
                setErrorMessage(error.message)
                setErrorCode(error.errorCode)
                setContentUrl(currentPlayer.currentMediaItem?.mediaId)
                set("article_url", articleUrl)
                setModule(LogModules.AUDIO)
            }.run {
                audioManager.audioProvider.onError(applicationContext, this)
            }

            if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS || error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND) {
                if (currentMediaItemIndex < currentPlaylistItems.size - 1) {
                    Logger.d(TAG, "Attempting to play next item after error")
                    currentPlayer.seekToNextMediaItem()
                    currentPlayer.prepare()
                    return // Skip showing error toast since we're trying next item
                }
            }
        }
    }

    @UnstableApi
    private inner class MediaSessionServiceListener : Listener {

        @SuppressLint("MissingPermission")
        override fun onForegroundServiceStartNotAllowedException() {
            val notificationManagerCompat = NotificationManagerCompat.from(this@MusicService)
            ensureNotificationChannel(notificationManagerCompat)
            val pendingIntent = getPendingIntentToLaunchUI()
            val builder =
                NotificationCompat.Builder(this@MusicService, NOW_PLAYING_CHANNEL_ID)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(androidx.media3.session.R.drawable.media3_notification_small_icon)
                    .setContentTitle(getString(R.string.notification_content_title))
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(getString(R.string.notification_content_text))
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
            notificationManagerCompat.notify(NOW_PLAYING_NOTIFICATION_ID, builder.build())
        }
    }

    @OptIn(UnstableApi::class)
    private fun ensureNotificationChannel(notificationManagerCompat: NotificationManagerCompat) {
        if (Util.SDK_INT < 26 || notificationManagerCompat.getNotificationChannel(
                NOW_PLAYING_CHANNEL_ID
            ) != null
        ) {
            return
        }

        val channel =
            NotificationChannel(
                NOW_PLAYING_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        notificationManagerCompat.createNotificationChannel(channel)
    }

    private fun getPlaybackSpeed(): Float {
        return exoPlayer?.playbackParameters?.speed ?: 1f
    }

    private fun setPlaybackSpeed(speed: Float) {
        currentPlayer.playbackParameters = PlaybackParameters(speed)
    }

    private fun setSeekValue(seekValue: Long) {
        currentPlayer.seekTo(seekValue)
    }

    private fun getPendingIntentToLaunchUI(): PendingIntent? {
        return packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
            PendingIntent.getActivity(
                this,
                0,
                sessionIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    private fun isAutomotiveController(controller: MediaSession.ControllerInfo): Boolean {
        val automotivePackages = listOf(
            "com.google.android.projection.gearhead",  // Android Auto
            "com.google.android.autosimulator",        // Android Auto Simulator
            "com.google.android.carassistant"          // Google Assistant on Android Automotive OS
        )
        return automotivePackages.contains(controller.packageName) || isCarTemplatesController(controller)
    }

    private fun isCarTemplatesController(controller: MediaSession.ControllerInfo): Boolean {
        return controller.connectionHints.getString(CONTROLLER_SOURCE_KEY) ==
            CONTROLLER_SOURCE_CAR_TEMPLATES
    }

    fun loadForYouAudio(audioMediaConfig: AudioMediaConfig): MediaItem? {
        var resolvedMediaUrl: String? = null
        var resolvedMediaAdsUrl: String? = null
        if (audioAdsConfig.isVastEnabled(audioMediaConfig)) {
            resolvedMediaUrl = audioMediaConfig.rawUrl
        } else {
            resolvedMediaUrl =
                if (audioMediaConfig.getPlayAd() && !audioMediaConfig.adsUrl.isNullOrEmpty()) {
                    resolvedMediaAdsUrl = audioMediaConfig.adsUrl
                    audioMediaConfig.adsUrl
                } else
                    audioMediaConfig.rawUrl
        }
        audioMediaConfig.resolvedMediaUrl = resolvedMediaUrl
        audioMediaConfig.resolvedAdsUrl =
            appendAdCustomTargetingValues(audioMediaConfig.adsCustomTargeting, resolvedMediaAdsUrl)
        if (resolvedMediaUrl != null) {
            val hasChildrenOrTransitions =
                !audioMediaConfig.children.isNullOrEmpty() || !audioMediaConfig.transitions.isNullOrEmpty()

            val mediaMetaData =
                if (hasChildrenOrTransitions) {
                    buildChildrenMetadata(audioMediaConfig)
                } else {
                    MediaMetadata.Builder().from(audioMediaConfig).build()
                }

            val mediaItem = MediaItem.Builder().apply {
                setMediaId(audioMediaConfig.id)
                if (audioAdsConfig.isVastEnabled(audioMediaConfig)) {
                    setUri(resolvedMediaUrl.toUri())
                    val adsConfig = adsRepository.getAdsConfig(audioMediaConfig)
                    putExtra(mediaMetaData) {
                        putParcelable(METADATA_KEY_ADS_CONFIG, adsConfig)
                    }
                } else {
                    if (!audioMediaConfig.resolvedAdsUrl.isNullOrEmpty() && audioMediaConfig.getPlayAd()) {
                        setUri(audioMediaConfig.resolvedAdsUrl.toUri())
                        EventLog.Builder().apply {
                            setModule(LogModules.AUDIO)
                            setMessage("Requesting Audio Ads URL")
                            set("ads_url", audioMediaConfig.resolvedAdsUrl)
                        }.run {
                            audioManager.audioProvider.debugLog(this@MusicService, this)
                        }
                    } else {
                        setUri(resolvedMediaUrl.toUri())
                    }
                }
                setTag(audioMediaConfig)
                setMediaMetadata(mediaMetaData)
            }.build()

            return mediaItem
        }
        return null
    }

    private fun appendAdCustomTargetingValues(
        params: Map<String, List<String>>?,
        adsUrl: String?
    ): String? {
        if (params.isNullOrEmpty()) return null
        return if (!adsUrl.isNullOrEmpty()) {
            val urlBuilder = StringBuilder(adsUrl)
            if (!adsUrl.contains("?")) {
                urlBuilder.append("?")
            } else if (!adsUrl.endsWith("?")) {
                urlBuilder.append("&")
            }

            params.entries.joinToString("&") { (key, value) ->
                val encodedKey = URLEncoder.encode(key, "UTF-8")
                val encodedValue = URLEncoder.encode(value.joinToString(","), "UTF-8")
                "$encodedKey=$encodedValue"
            }.let {
                urlBuilder.append(it)
            }

            return urlBuilder.toString()
        } else null
    }

    private fun createPlayableMediaItem(audioMediaConfig: AudioMediaConfig): MediaItem {
        val hasChildren = !audioMediaConfig.children.isNullOrEmpty()
        val hasTransitions = !audioMediaConfig.transitions.isNullOrEmpty()
        val mediaMetadata = MediaMetadata.Builder()
            .from(audioMediaConfig)
            .build()

        if (hasChildren || hasTransitions) {
            val metadata = buildChildrenMetadata(audioMediaConfig)
            return MediaItem.Builder()
                .setMediaId(audioMediaConfig.id)
                .setMediaMetadata(metadata)
                .build()
        }
        return MediaItem.Builder()
            .setMediaId(audioMediaConfig.id)
            .setUri(audioMediaConfig.streamUrl ?: audioMediaConfig.rawUrl)
            .setMediaMetadata(mediaMetadata)
            .apply {
                if (audioAdsConfig.isVastEnabled(audioMediaConfig)) {
                    val adsConfig = adsRepository.getAdsConfig(audioMediaConfig)
                    putExtra(mediaMetadata) {
                        putParcelable(METADATA_KEY_ADS_CONFIG, adsConfig)
                    }
                }
            }
            .build()
    }

    private fun buildChildrenMetadata(audioMediaConfig: AudioMediaConfig): MediaMetadata {
        val playlist = mutableListOf<AudioMediaConfig>()
        audioMediaConfig.transitions?.let { playlist.addAll(it) }
        playlist.add(audioMediaConfig.copy(children = null, transitions = null))
        audioMediaConfig.children?.let { playlist.addAll(it) }

        val uris = playlist.mapNotNull {
            it.url ?: it.rawUrl
        }
        val durations = playlist.map { (it.duration ?: 0L).toString() }

        return MediaMetadata.Builder()
            .from(audioMediaConfig)
            .setExtras(Bundle().apply {
                putStringArrayList(CONCAT_CHILDREN_URIS, ArrayList(uris))
                putStringArrayList(CONCAT_CHILDREN_DURATIONS, ArrayList(durations))
                putBoolean(IS_CONCAT2, true)
            })
            .build()
    }

    fun MediaItem.toMediaItemData(
        playerType: PlayerType,
        podcast: AudioMediaConfig
    ): MediaItemData {
        val date = podcast.date?.let {
            monthDayYearFormat(Date(it))
        }
        return MediaItemData(
            mediaId = this.mediaId,
            mediaUrl = this.localConfiguration?.uri.toString(),
            primaryLabel = podcast.title,
            title = "",
            subtitle = "",
            albumArtUrl = "",
            displayDate = date,
            duration = this.mediaMetadata.duration,
            seriesSlug = "podcast",
            podcastSlug = podcast.title,
            playerTypeName = playerType.name,
            playbackState = PlaybackStateCompat.STATE_NONE,
            audioType = podcast.audioType
        )
    }
}

object MusicServiceNotifier {
    val browseTreeUpdatedFlow = MutableSharedFlow<String>(replay = 1)
}

const val NETWORK_FAILURE = "NETWORK_FAILURE"
const val CMD_GET_PLAYBACK_SPEED = "CMD_GET_PLAYBACK_SPEED"
const val CMD_SET_PLAYBACK_SPEED = "CMD_SET_PLAYBACK_SPEED"
const val PLAYBACK_SPEED_VALUE = "PLAYBACK_SPEED_VALUE"
const val CMD_SEEK_TO = "CMD_SEEK_TO"
const val SEEK_TO_VALUE = "SEEK_TO_VALUE"
const val CMD_HIDE_NOTIFICATION = "CMD_HIDE_NOTIFICATION"
const val NOW_PLAYING_CHANNEL_ID = "com.example.android.media.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION_ID = 0xb339 // Arbitrary number used to identify our notification
const val FOR_YOU_SECTION_NAME = "For You"
const val FOR_YOU_SECTION_ID = "for_you"
const val PODCAST_SECTION_NAME = "Podcasts"
const val PODCAST_SECTION_ID = "podcasts"
const val CONTROLLER_SOURCE_KEY = "source"
const val CONTROLLER_SOURCE_CAR_TEMPLATES = "car_templates"
const val CMD_GET_MEDIA_SESSION_TOKEN = "CMD_GET_MEDIA_SESSION_TOKEN"
const val MEDIA_SESSION_TOKEN_KEY = "MEDIA_SESSION_TOKEN_KEY"

typealias CommandHandler = (parameters: Bundle, callback: ResultReceiver?) -> Unit

private const val TAG = "MusicService"

const val CUSTOM_ACTION_REWIND_15 = "androidx.media3.session.command.REWIND"
const val CUSTOM_ACTION_FORWARD_15 = "androidx.media3.session.command.FAST_FORWARD"
const val CUSTOM_ACTION_REWIND_15_DISPLAY_NAME = "Rewind 15 seconds"
const val CUSTOM_ACTION_FORWARD_15_DISPLAY_NAME = "Forward 15 seconds"
