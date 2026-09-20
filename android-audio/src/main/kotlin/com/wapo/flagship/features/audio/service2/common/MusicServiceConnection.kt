/*
 * Copyright 2018 Google Inc. All rights reserved.
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

package com.wapo.flagship.features.audio.service2.common

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.session.SessionToken
import com.wapo.android.commons.util.Logger
import androidx.annotation.OptIn
import androidx.concurrent.futures.await
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.wapo.flagship.features.audio.service2.media.*
import com.wapo.flagship.features.audio.service2.media.extensions.PlayerState
import com.wapo.flagship.features.audio.service2.media.library.BrowseTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Singleton


/**
 * A connection to the music service that provides access to playback controls and media browsing.
 */
@Singleton
@OptIn(UnstableApi::class)
class MusicServiceConnection() {

    private val TAG = "MusicServiceConnection"

    val isConnected = MutableLiveData<Boolean>().apply { postValue(false) }
    val networkFailure = MutableLiveData<Boolean>().apply { postValue(false) }

    val playbackState = MutableLiveData<Int>().apply { postValue(Player.STATE_IDLE) }
    val playWhenReady = MutableLiveData<Boolean>().apply { postValue(false) }
    val nowPlaying = MutableLiveData<MediaItem?>()

    val mediaItemList = MutableLiveData<List<MediaItem>>().apply { postValue(Collections.emptyList()) }

    var playerPlaybackSpeed: Float = 1f

    private lateinit var sessionToken: SessionToken

    // Using lateinit for mediaBrowser and mediaController as they are assigned once after future completion
    private lateinit var mediaBrowser: MediaBrowser
    private lateinit var mediaController: MediaController

    // The futures to build the browser/controller
    private lateinit var mediaBrowserFuture: ListenableFuture<MediaBrowser>

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main)

    private var isErrorState = false

    private var mediaBrowserPlayerListener: Player.Listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (!isErrorState) {
                this@MusicServiceConnection.playbackState.postValue(playbackState)
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            this@MusicServiceConnection.playWhenReady.postValue(playWhenReady)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            isErrorState = false
            nowPlaying.postValue(mediaItem)
        }

        override fun onPlayerError(error: PlaybackException) {
            this@MusicServiceConnection.playbackState.postValue(PlayerState.ERROR.state)
            isErrorState = true
            Logger.e(TAG, "Player Error: ${error.message}", error)
            // You might want to handle network failures here more specifically
            networkFailure.postValue(true)

            hideNotification()
            clearMediaItems()
        }
    }

    /**
     * Initializes the MusicServiceConnection with the provided context and service component.
     * This method should be called once to set up the connection or to reconnect if needed.
     */
    fun init(context: Context, serviceComponent: ComponentName) {
        sessionToken = SessionToken(context, serviceComponent)
        mediaBrowserFuture = MediaBrowser.Builder(context, sessionToken).buildAsync()
        // Listener for MediaBrowser connection (this is the primary connection point for Browse)
        mediaBrowserFuture.addListener({
            try {
                val browser = mediaBrowserFuture.get()
                this.mediaBrowser = browser // Assign the connected browser
                this.mediaController = browser // MediaBrowser extends MediaController, so it can be used for control too

                if (browser.isConnected) {
                    isConnected.postValue(true)
                    Logger.d(TAG, "AudioDebug: MediaBrowser connected successfully.")

//                    mediaController.prepare()

                    // Add Player.Listener to the MediaBrowser for playback state updates
                    browser.addListener(mediaBrowserPlayerListener)

                    // Initial query for playback speed once connected
                    queryPlaybackSpeed()

                    // Initial subscription to the root of the browse tree
                    fetchMediaListFromParent(BrowseTree.ROOT, shouldWaitForBrowseTreeUpdate = false)
                } else {
                    isConnected.postValue(false)
                    Logger.d(TAG, "AudioDebug: MediaBrowser not connected.")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "AudioDebug: Failed to build MediaBrowser or connect: ${e.message}", e)
                isConnected.postValue(false)
            }
        }, MoreExecutors.directExecutor())

        // We technically don't need a separate listener for mediaControllerFuture if we're using
        // the MediaBrowser as the primary connection and casting it to MediaController.
        // However, if you explicitly need a separate MediaController instance for some reason,
        // you'd keep this. For simplicity and to avoid redundant connection attempts, often
        // you just use MediaBrowser for everything.
        /*
        mediaControllerFuture.addListener({
            try {
                this.mediaController = mediaControllerFuture.get()
                // You might check isConnected here again, but the browser connection is primary
                LogUtil.d(TAG, "AudioDebug: MediaController instance obtained from future.")
            } catch (e: Exception) {
                LogUtil.e(TAG, "AudioDebug: Failed to get MediaController from future", e)
            }
        }, MoreExecutors.directExecutor())
        */
    }

    /**
     * Fetches and updates the children for a given parentMediaId.
     * This method should be called:
     * 1. On initial connection to get the first set of children.
     * 2. Whenever you know the children for a parentId might have changed
     * (e.g., after your MediaLibraryService calls notifyChildrenChanged, you'd trigger this).
     * shouldWaitForBrowseTreeUpdate: If true, waits for the browse tree to be updated before fetching children.
     */
    fun fetchMediaListFromParent(parentMediaId: String, shouldWaitForBrowseTreeUpdate: Boolean = true) {
        // Ensure MediaBrowser is connected before attempting to get children
        if (!::mediaBrowser.isInitialized || !mediaBrowser.isConnected) {
            Logger.w(TAG, "AudioDebug: MediaBrowser is not connected. Cannot fetch children for $parentMediaId")
            return // Exit immediately if not connected
       }

        Logger.d(TAG, "AudioDebug: Requesting children for parentId: $parentMediaId")
        
        serviceScope.launch {
            if (shouldWaitForBrowseTreeUpdate) {
                // wait for the browse tree to be updated before fetching children
                MusicServiceNotifier.browseTreeUpdatedFlow.collect {
                    Logger.d(TAG, "AudioDebug: Browse tree updated, fetching children for $parentMediaId")
                    fetchMediaListFromParentInternal(parentMediaId)
                }
            } else {
                // If we don't need to wait for the browse tree update, just fetch children immediately
                fetchMediaListFromParentInternal(parentMediaId)
            }
        }
    }

    /**
     * Internal method to fetch media items from the parentMediaId.
     */
    private fun fetchMediaListFromParentInternal(parentMediaId: String) = serviceScope.launch {
        Logger.d(TAG, "AudioDebug: Browse tree updated, fetching children for $parentMediaId")

        val result = try {
            mediaBrowser.getChildren(parentMediaId, 0, Int.MAX_VALUE, null).await()
        } catch (e: Exception) {
            Logger.e(TAG, "AudioDebug: Exception fetching children for $parentMediaId", e)
            null
        }

        if (result == null || result.resultCode != LibraryResult.RESULT_SUCCESS) {
            Logger.e(TAG, "AudioDebug: Failed to fetch children for $parentMediaId")
            if (parentMediaId == BrowseTree.ROOT) {
                mediaItemList.postValue(emptyList())
            }
            return@launch
        }

        val mediaItems = result.value
        Logger.d(TAG, "AudioDebug: Successfully fetched ${mediaItems?.size} children for $parentMediaId.")
        if (parentMediaId == BrowseTree.ROOT) {
            mediaItemList.postValue(mediaItems)
            this@MusicServiceConnection.playbackState.postValue(mediaBrowser.playbackState)
            // Post the *current* playWhenReady state
            this@MusicServiceConnection.playWhenReady.postValue(mediaBrowser.playWhenReady)
            // Post the *current* nowPlaying MediaItem
            this@MusicServiceConnection.nowPlaying.postValue(mediaBrowser.currentMediaItem)
            mediaItems?.forEach {
                Logger.d(TAG, "AudioDebug: Fetched MediaItem: ID: ${it.mediaId}, Title: ${it.mediaMetadata.title}")
            }
        }
        return@launch
    }

    /**
     * Releases the MediaBrowser and MediaController resources.
     * This should be called when the component using this connection is destroyed.
     */
    fun release() {
        Logger.d(TAG, "AudioDebug: Releasing MusicServiceConnection resources.")

        serviceJob.cancel()
        try {
            if (::mediaBrowserFuture.isInitialized) {
                mediaBrowserFuture.cancel(true) // Cancel the future to avoid memory leaks
            }
        } catch (e: Exception) {
            Logger.e(TAG, "AudioDebug: Error cancelling MediaBrowserFuture", e)
        }
        try {
            if (::mediaController.isInitialized) {
                mediaController.removeListener(mediaBrowserPlayerListener)
                mediaController.release()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "AudioDebug: Error releasing MediaController", e)
        }
        try {
            if (::mediaBrowser.isInitialized) {
                mediaBrowser.removeListener(mediaBrowserPlayerListener)
                mediaBrowser.release()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "AudioDebug: Error releasing MediaBrowser", e)
        }
    }

    // --- MediaController Interaction Methods ---
    // Ensure mediaController is initialized before calling these methods
    // Use ::mediaController.isInitialized to check

    private fun sendCommand(command: SessionCommand, extras: Bundle = Bundle.EMPTY): ListenableFuture<SessionResult> {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot send command: ${command.customAction}")
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_SESSION_DISCONNECTED))
        }
        return mediaController.sendCustomCommand(command, extras)
    }

    fun queryPlaybackSpeed() {
        val commandFuture = sendCommand(SessionCommand(CMD_GET_PLAYBACK_SPEED, Bundle.EMPTY))
        commandFuture.addListener({
            try {
                val result = commandFuture.get()
                if (result.resultCode == LibraryResult.RESULT_SUCCESS) {
                    playerPlaybackSpeed = result.extras.getFloat(PLAYBACK_SPEED_VALUE, 1f)
                    Logger.d(TAG, "AudioDebug: Queried playback speed: $playerPlaybackSpeed")
                } else {
                    Logger.e(TAG, "AudioDebug: Failed to query playback speed: ${result.resultCode}")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "AudioDebug: Error querying playback speed", e)
            }
        }, MoreExecutors.directExecutor())
    }

    fun setPlaybackSpeed(speed: Float) {
        val bundle = Bundle().apply { putFloat(PLAYBACK_SPEED_VALUE, speed) }
        val commandFuture = sendCommand(SessionCommand(CMD_SET_PLAYBACK_SPEED, bundle))
        commandFuture.addListener({
            try {
                val result = commandFuture.get()
                if (result.resultCode == LibraryResult.RESULT_SUCCESS) {
                    mediaController.setPlaybackSpeed(speed)
                    playerPlaybackSpeed = speed
                    Logger.d(TAG, "AudioDebug: Set playback speed to: $speed")
                } else {
                    Logger.e(TAG, "AudioDebug: Failed to set playback speed: ${result.resultCode}")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "AudioDebug: Error setting playback speed", e)
            }
        }, MoreExecutors.directExecutor())
    }

    fun seekTo(positionMS: Long) {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot seek.")
            return
        }
        mediaController.seekTo(positionMS) // Direct call on MediaController
        Logger.d(TAG, "AudioDebug: Sent seekTo command: $positionMS")
    }

    fun play() {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot play.")
            return
        }
        mediaController.play()
        Logger.d(TAG, "AudioDebug: Sent play command.")
    }

    fun pause() {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot pause.")
            return
        }
        mediaController.pause()
        Logger.d(TAG, "AudioDebug: Sent pause command.")
    }

    fun prepare() {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot prepare.")
            return
        }
        mediaController.prepare()
        Logger.d(TAG, "AudioDebug: Sent prepare command.")
    }

    fun stop() {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot stop.")
            return
        }
        mediaController.stop()
        Logger.d(TAG, "AudioDebug: Sent stop command.")
    }

    fun clearMediaItems() {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot clear media items.")
            return
        }
        mediaController.clearMediaItems()
    }

    fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long = C.TIME_UNSET) {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot set media item.")
            return
        }
        mediaController.setMediaItem(mediaItem, startPositionMs)
        mediaController.prepare() // Prepare after setting to ensure it's ready for playback
        Logger.d(TAG, "AudioDebug: Set media item: ${mediaItem.mediaId}")
    }

    fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot set media items.")
            return
        }
        mediaController.setMediaItems(mediaItems, resetPosition)
    }

    fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int = 0, startPositionInMs: Long? = null) {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot set media items.")
            return
        }
        if (startPositionInMs == null) {
            mediaController.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
        } else {
            mediaController.setMediaItems(mediaItems, startIndex, startPositionInMs)
        }
    }

    fun addMediaItem(mediaItem: MediaItem) {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot add media item.")
            return
        }
        mediaController.addMediaItem(mediaItem)
        mediaController.prepare() // Prepare after adding to ensure it's ready for playback
        Logger.d(TAG, "AudioDebug: Added media item: ${mediaItem.mediaId}")
    }

    fun removeMediaItem(index: Int) {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot remove media item.")
            return
        }
        mediaController.removeMediaItem(index)
        Logger.d(TAG, "AudioDebug: Removed queue item at index: $index")
    }

    fun seekToNextMediaItem() {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot seek to next media item.")
            return
        }
        mediaController.seekToNextMediaItem()
        Logger.d(TAG, "AudioDebug: Sent seekToNextMediaItem command.")
    }

    fun seekToPreviousMediaItem() {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot seek to previous media item.")
            return
        }
        mediaController.seekToPreviousMediaItem()
        Logger.d(TAG, "AudioDebug: Sent seekToPreviousMediaItem command.")
    }

    fun seekToNext() {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot seek to next.")
            return
        }
        mediaController.seekToNext()
        Logger.d(TAG, "AudioDebug: Sent seekToNext command.")
    }

    fun seekToPrevious() {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            Logger.e(TAG, "AudioDebug: MediaController not connected, cannot seek to previous.")
            return
        }
        mediaController.seekToPrevious()
        Logger.d(TAG, "AudioDebug: Sent seekToPrevious command.")
    }

    fun getPlayer(): Player? {
        if (!::mediaController.isInitialized || !mediaController.isConnected) {
            return null
        }
        return mediaController
    }

    fun hideNotification() {
        val commandFuture = sendCommand(SessionCommand(CMD_HIDE_NOTIFICATION, Bundle.EMPTY))
        commandFuture.addListener({
            try {
                val result = commandFuture.get()
                if (result.resultCode == LibraryResult.RESULT_SUCCESS) {
                    Logger.d(TAG, "AudioDebug: Successfully hid notification")
                } else {
                    Logger.e(TAG, "AudioDebug: Failed to hide notification (Result: ${result.resultCode})")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "AudioDebug: Error hiding notification", e)
            }
        }, MoreExecutors.directExecutor())
    }

    companion object {
        @Volatile
        private var instance: MusicServiceConnection? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: MusicServiceConnection()
                    .also { instance = it }
            }
    }
}
