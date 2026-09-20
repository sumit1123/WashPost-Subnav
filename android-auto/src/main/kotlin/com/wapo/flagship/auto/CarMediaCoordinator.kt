package com.wapo.flagship.auto

import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.media.MediaPlaybackManager
import androidx.concurrent.futures.await
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.wapo.flagship.features.audio.service2.media.CMD_GET_MEDIA_SESSION_TOKEN
import com.wapo.flagship.features.audio.service2.media.CONTROLLER_SOURCE_CAR_TEMPLATES
import com.wapo.flagship.features.audio.service2.media.CONTROLLER_SOURCE_KEY
import com.wapo.flagship.features.audio.service2.media.FOR_YOU_SECTION_ID
import com.wapo.flagship.features.audio.service2.media.MEDIA_SESSION_TOKEN_KEY
import com.wapo.flagship.features.audio.service2.media.MusicService
import com.wapo.flagship.features.audio.service2.media.PODCAST_SECTION_ID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class CarMediaState(
    val isLoading: Boolean = true,
    val itemsBySection: Map<String, List<MediaItem>> =
        MEDIA_SECTION_IDS.associateWith { emptyList() },
    val activeMediaItem: MediaItem? = null,
    val queue: List<MediaItem> = emptyList(),
    val currentQueueIndex: Int = C.INDEX_UNSET,
    val connectionError: Throwable? = null,
)

@OptIn(ExperimentalCarApi::class, UnstableApi::class)
internal class CarMediaCoordinator(
    private val carContext: CarContext,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(CarMediaState())
    val state: StateFlow<CarMediaState> = _state.asStateFlow()

    private var mediaBrowser: MediaBrowser? = null
    private var connectionJob: Job? = null
    private val refreshJobs = mutableMapOf<String, Job>()
    private var isPlaybackTokenRegistered = false
    private var isReleased = false

    private val browserListener =
        object : MediaBrowser.Listener {
            override fun onDisconnected(controller: MediaController) {
                // clear UI if background audio service is disconnected
                if (controller !== mediaBrowser || isReleased) return
                controller.removeListener(playerListener)
                mediaBrowser = null
                isPlaybackTokenRegistered = false
                refreshJobs.values.forEach(Job::cancel)
                refreshJobs.clear()
                _state.update {
                    it.copy(
                        itemsBySection = MEDIA_SECTION_IDS.associateWith { emptyList() },
                        activeMediaItem = null,
                        queue = emptyList(),
                        currentQueueIndex = C.INDEX_UNSET,
                        connectionError = IllegalStateException("MusicService disconnected"),
                    )
                }
            }

            override fun onChildrenChanged(
                browser: MediaBrowser,
                parentId: String,
                itemCount: Int,
                params: MediaLibraryService.LibraryParams?,
            ) {
                // refresh section if background audio service has updated the media items
                if (parentId !in MEDIA_SECTION_IDS || browser !== mediaBrowser || isReleased) return
                refreshSection(parentId)
            }
        }

    private val playerListener =
        object : Player.Listener {
            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int,
            ) {
                val browser = mediaBrowser ?: return
                updatePlaybackState(browser)
            }

            override fun onTimelineChanged(
                timeline: Timeline,
                reason: Int,
            ) {
                val browser = mediaBrowser ?: return
                updatePlaybackState(browser)
            }
        }

    fun connect() {
        if (isReleased || mediaBrowser != null || connectionJob?.isActive == true) return
        connectionJob = scope.launch { connectToMusicService() }
    }

    suspend fun play(item: MediaItem): Boolean {
        val browser = mediaBrowser?.takeIf { it.isConnected } ?: return false
        // start audio
        browser.setMediaItem(item)
        _state.update {
            it.copy(
                activeMediaItem = item,
                queue = listOf(item),
                currentQueueIndex = 0,
            )
        }
        browser.prepare()
        browser.play()

        // register the media playback token with the car system
        if (!isPlaybackTokenRegistered) {
            isPlaybackTokenRegistered = tryRegisterMediaPlaybackToken(browser)
        }
        return isPlaybackTokenRegistered
    }

    fun playQueueItem(index: Int): Boolean {
        val browser = mediaBrowser?.takeIf { it.isConnected } ?: return false
        if (index !in 0 until browser.mediaItemCount) return false

        browser.seekToDefaultPosition(index)
        browser.play()
        updatePlaybackState(browser)
        return true
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        connectionJob?.cancel()
        connectionJob = null
        refreshJobs.values.forEach(Job::cancel)
        refreshJobs.clear()

        mediaBrowser?.let { browser ->
            browser.removeListener(playerListener)
            MEDIA_SECTION_IDS.forEach { sectionId -> browser.unsubscribe(sectionId) }
            browser.release()
        }
        mediaBrowser = null
    }

    private suspend fun connectToMusicService() {
        runCatching {
            val sessionToken =
                SessionToken(carContext, ComponentName(carContext, MusicService::class.java))
            val browser =
                MediaBrowser
                    .Builder(carContext, sessionToken)
                    .setListener(browserListener)
                    .setConnectionHints(
                        bundleOf(CONTROLLER_SOURCE_KEY to CONTROLLER_SOURCE_CAR_TEMPLATES),
                    ).buildAsync()
                    .await()

            if (isReleased) {
                browser.release()
                return
            }

            mediaBrowser = browser
            browser.addListener(playerListener)
            subscribeToMediaSections(browser)
            isPlaybackTokenRegistered = tryRegisterMediaPlaybackToken(browser)

            // download items for "For You" and "Podcasts" sections
            coroutineScope {
                MEDIA_SECTION_IDS
                    .map { sectionId ->
                        async { sectionId to fetchSection(browser, sectionId) }
                    }.awaitAll()
            }
                .forEach { (sectionId, items) ->
                    updateSection(sectionId, items.orEmpty())
                }

            updatePlaybackState(browser)
            _state.update { it.copy(isLoading = false, connectionError = null) }
        }.onFailure { error ->
            error.rethrowCancellation()
            if (isReleased) return@onFailure
            Log.e(TAG, "Unable to connect the car UI to MusicService", error)
            _state.update { it.copy(isLoading = false, connectionError = error) }
        }
    }

    private suspend fun subscribeToMediaSections(browser: MediaBrowser) {
        // subscribe to the "For You" and "Podcasts" sections
        // so we know when the background audio service changes the media items
        MEDIA_SECTION_IDS.forEach { sectionId ->
            runCatching { browser.subscribe(sectionId, null).await() }
                .onFailure { error ->
                    error.rethrowCancellation()
                    Log.w(TAG, "Unable to subscribe to media section $sectionId", error)
                }
        }
    }

    private fun refreshSection(sectionId: String) {
        // cancel any previous refresh job for this section
        refreshJobs.remove(sectionId)?.cancel()
        refreshJobs[sectionId] =
            scope.launch {
                val browser = mediaBrowser?.takeIf { it.isConnected } ?: return@launch
                // fetch the new items and update UI
                val items = fetchSection(browser, sectionId) ?: return@launch
                if (!isReleased && browser === mediaBrowser) {
                    updateSection(sectionId, items)
                }
            }
    }

    private suspend fun fetchSection(
        browser: MediaBrowser,
        sectionId: String,
    ): List<MediaItem>? =
        runCatching {
            browser
                .getChildren(sectionId, 0, Int.MAX_VALUE, null)
                .await()
                .value
        }.onFailure { error ->
            error.rethrowCancellation()
            Log.w(TAG, "Unable to refresh media section $sectionId", error)
        }.getOrNull()

    private fun updateSection(
        sectionId: String,
        items: List<MediaItem>,
    ) {
        _state.update { current ->
            current.copy(itemsBySection = current.itemsBySection + (sectionId to items))
        }
    }

    private fun updatePlaybackState(browser: MediaBrowser) {
        if (browser !== mediaBrowser || isReleased) return

        val queue =
            (0 until browser.mediaItemCount).map { index ->
                browser.getMediaItemAt(index)
            }
        val currentIndex =
            browser.currentMediaItemIndex
                .takeIf { it in queue.indices }
                ?: C.INDEX_UNSET

        _state.update {
            it.copy(
                activeMediaItem = browser.currentMediaItem,
                queue = queue,
                currentQueueIndex = currentIndex,
            )
        }
    }

    private suspend fun tryRegisterMediaPlaybackToken(browser: MediaBrowser): Boolean =
        runCatching {
            val result =
                browser
                    .sendCustomCommand(
                        SessionCommand(CMD_GET_MEDIA_SESSION_TOKEN, Bundle.EMPTY),
                        Bundle.EMPTY,
                    ).await()
            check(result.resultCode == SessionResult.RESULT_SUCCESS) {
                "MusicService rejected the media playback token command: ${result.resultCode}"
            }

            // convert the modern media3 session into a legacy token required by android auto
            val platformToken =
                BundleCompat.getParcelable(
                    result.extras,
                    MEDIA_SESSION_TOKEN_KEY,
                    android.media.session.MediaSession.Token::class.java,
                ) ?: return@runCatching false
            val compatToken = MediaSessionCompat.Token.fromToken(platformToken)
            val playbackManager = carContext.getCarService(MediaPlaybackManager::class.java)
            playbackManager.registerMediaPlaybackToken(compatToken)
            true
        }.onFailure { error ->
            error.rethrowCancellation()
            Log.e(TAG, "Unable to register the media playback token", error)
        }.getOrDefault(false)

    private companion object {
        const val TAG = "CarMediaCoordinator"
    }
}

internal val MEDIA_SECTION_IDS = listOf(FOR_YOU_SECTION_ID, PODCAST_SECTION_ID)

private fun Throwable.rethrowCancellation() {
    if (this is CancellationException) throw this
}
