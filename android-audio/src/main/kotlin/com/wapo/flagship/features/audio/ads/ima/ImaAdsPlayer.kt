package com.wapo.flagship.features.audio.ads.ima

import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.ads.interactivemedia.v3.api.AdPodInfo
import com.google.ads.interactivemedia.v3.api.player.AdMediaInfo
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.audio.ads.util.AdPlayerUtils.isPlayingAd2
import com.wapo.flagship.features.audio.service2.media.extensions.METADATA_KEY_CONTENT_MEDIA_ID
import com.wapo.flagship.features.audio.service2.media.extensions.METADATA_KEY_CONTENT_MEDIA_PLAYLIST_IDS
import com.wapo.flagship.features.audio.service2.media.extensions.METADATA_KEY_IS_AD
import com.wapo.flagship.features.audio.service2.media.extensions.METADATA_LIST_DELIMITER
import com.wapo.flagship.features.audio.service2.media.extensions.duration
import com.wapo.flagship.features.audio.R
import com.wapo.flagship.features.audio.service2.common.MusicServiceConnection
import com.wapo.flagship.features.audio.utils.PlayerLogger

class ImaAdsPlayer(
    val basePlayer: Player,
    val musicServiceConnection: MusicServiceConnection,
    val resources: Resources,
    val logger: PlayerLogger,
) : VideoAdPlayer {
    private val callbacks = mutableListOf<VideoAdPlayer.VideoAdPlayerCallback>()
    private var currentAd: AdMediaInfo? = null
    private var lastContentState: ContentPlaybackState? = null
    val currentPosition get() = basePlayer.currentPosition
    val duration get() = basePlayer.duration
    private val isPlayingAd get() = basePlayer.isPlayingAd2

    private val imaProgressTracker: ImaProgressTracker = ImaProgressTracker {
        currentAd?.let {
            for (callback in callbacks) {
                callback.onAdProgress(it, getAdProgress())
            }
        }
    }
    private val playerListenerForCallbacksUpdates = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            Logger.d(TAG, "onPlaybackStateChanged: playbackState=${logger.playbackStateToString(playbackState)}")
            val currentAd = this@ImaAdsPlayer.currentAd ?: return
            when (playbackState) {
                Player.STATE_BUFFERING -> callbacks.forEach { it.onBuffering(currentAd) }
                Player.STATE_READY -> callbacks.forEach { it.onLoaded(currentAd) }
                Player.STATE_ENDED -> callbacks.forEach { it.onEnded(currentAd) }
                else -> {}
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, playbackState: Int) {
            Logger.d(TAG, "onPlayWhenReadyChanged: playWhenReady=$playWhenReady, playbackState=${logger.playbackStateToString(playbackState)}")
            onPlaybackStateChanged(playbackState)
        }

        override fun onVolumeChanged(volume: Float) {
            Logger.d(TAG, "onVolumeChanged: volume=$volume")
            val currentAd = this@ImaAdsPlayer.currentAd ?: return
            callbacks.forEach { it.onVolumeChanged(currentAd, volume.toInt()) }
        }

        override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
            Logger.d(TAG, "onDeviceVolumeChanged: volume=$volume, muted=$muted")
            val currentAd = this@ImaAdsPlayer.currentAd ?: return
            callbacks.forEach { it.onVolumeChanged(currentAd, volume) }
        }
    }

    init {
        basePlayer.addListener(playerListenerForCallbacksUpdates)
    }

    override fun loadAd(adMediaInfo: AdMediaInfo, adPodInfo: AdPodInfo) {
        Logger.d(TAG, "ImaVideoAdPlayer loadAd: adMediaInfo=$adMediaInfo, adPodInfo=$adPodInfo")
    }

    @UnstableApi
    override fun playAd(adMediaInfo: AdMediaInfo) {
        Logger.d(TAG, "ImaVideoAdPlayer playAd: adMediaInfo=$adMediaInfo")
        imaProgressTracker.start()
        if (currentAd == adMediaInfo) {
            callbacks.forEach { it.onResume(adMediaInfo) }
        } else {
            currentAd = adMediaInfo
            callbacks.forEach { it.onPlay(adMediaInfo) }
            val currentState = saveCurrentContentState()

            val mediaMetadata = MediaMetadata.Builder()
                .setExtras(
                    Bundle().apply {
                        putBoolean(METADATA_KEY_IS_AD, true)
                        putString(METADATA_KEY_CONTENT_MEDIA_ID, currentState?.currentMediaItem?.mediaId)
                        putString(
                            METADATA_KEY_CONTENT_MEDIA_PLAYLIST_IDS,
                            currentState?.mediaItems?.joinToString(METADATA_LIST_DELIMITER) { it.mediaId }
                        )
                    }
                )
                .setDisplayTitle(resources.getString(R.string.now_playing_ad))
                .build()
            val mediaItem = MediaItem.Builder()
                .setUri(adMediaInfo.url.toUri())
                .setMediaMetadata(mediaMetadata)
                .build()

            basePlayer.apply {
                musicServiceConnection.setPlaybackSpeed(1.0f)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
        }
    }

    override fun pauseAd(adMediaInfo: AdMediaInfo) {
        Logger.d(TAG, "ImaVideoAdPlayer pauseAd: adMediaInfo=$adMediaInfo")
        imaProgressTracker.stop()
        basePlayer.playWhenReady = false
        callbacks.forEach { it.onPause(adMediaInfo) }
    }

    override fun stopAd(adMediaInfo: AdMediaInfo) {
        Logger.d(TAG, "ImaVideoAdPlayer stopAd: adMediaInfo=$adMediaInfo")
        imaProgressTracker.stop()
        basePlayer.playWhenReady = false
        callbacks.forEach { it.onEnded(adMediaInfo) }
    }

    fun skipAd() {
        val currentAd = this@ImaAdsPlayer.currentAd ?: return
        imaProgressTracker.stop()
        callbacks.forEach { it.onEnded(currentAd) }
    }

    //  This method can be called multiple times by the IMA SDK. It is important not to remove
    //  [playerListenerForCallbacksUpdates] in this method, since there is just one instance of
    //  [ImaAdsPlayer] and the listener is added in the constructor. There is an alternative
    //  [releaseAll] function that will clean everything.
    override fun release() {
        Logger.d(TAG, "ImaVideoAdPlayer release")
        imaProgressTracker.stop()
        currentAd = null
        lastContentState = null
    }

    fun releaseAll() {
        release()
        basePlayer.removeListener(playerListenerForCallbacksUpdates)
    }

    override fun addCallback(callback: VideoAdPlayer.VideoAdPlayerCallback) {
        callbacks.add(callback)
    }

    override fun removeCallback(callback: VideoAdPlayer.VideoAdPlayerCallback) {
        callbacks.remove(callback)
    }

    override fun getAdProgress(): VideoProgressUpdate {
        Logger.d(TAG, "ImaVideoAdPlayer getAdProgress: currentAd=$currentAd, player.currentPosition=${basePlayer.currentPosition}, player.duration=${basePlayer.duration}")
        if (currentAd == null) {
            return VideoProgressUpdate.VIDEO_TIME_NOT_READY
        }

        return VideoProgressUpdate(basePlayer.currentPosition, basePlayer.duration)
    }

    override fun getVolume(): Int {
        Logger.d(TAG, "ImaVideoAdPlayer getVolume: player.volume=${basePlayer.volume}")
        return (100 * basePlayer.volume).toInt()
    }

    private fun saveCurrentContentState(): ContentPlaybackState? {
        Logger.d(TAG, "saveCurrentContentState: currentMediaItem=${basePlayer.currentMediaItem}, currentPosition=${basePlayer.currentPosition}, playWhenReady=${basePlayer.playWhenReady}, isPlayingAd=${isPlayingAd}, playbackSpeed=${basePlayer.playbackParameters.speed}")
        if (isPlayingAd) return null
        val currentMediaItems = mutableListOf<MediaItem>()
        for (index in 0 until basePlayer.mediaItemCount) {
            currentMediaItems.add(basePlayer.getMediaItemAt(index))
        }
        lastContentState = ContentPlaybackState(
            mediaItems = currentMediaItems,
            currentMediaItem = basePlayer.currentMediaItem,
            currentPositionMs = basePlayer.currentPosition,
            playWhenReady = basePlayer.playWhenReady,
            playerSpeed = basePlayer.playbackParameters.speed,
        )
        return lastContentState
    }

    fun pauseContent() {
        Logger.d(TAG, "pauseContent")
        basePlayer.playWhenReady = false
        saveCurrentContentState()
    }

    fun resumeContent() {
        Logger.d(TAG, "resumeContent: isPlayingAd=$isPlayingAd")
        if (isPlayingAd) {
            val contentState = lastContentState ?: return
            val (mediaItems, startIndex, startPositionMs) =
                with(contentState) {
                    if (mediaItems.isEmpty()) {
                        Logger.w(TAG, "resumeContent: no content media items to restore")
                        return
                    }
                    val startIndex = mediaItems
                        .indexOfFirst { it.mediaId == currentMediaItem?.mediaId }
                        .takeIf { it >= 0 } ?: 0
                    val duration = mediaItems[startIndex].mediaMetadata.duration
                    val startPositionMs = currentPositionMs
                        .coerceIn(0, duration?.let { it * 1000 } ?: Long.MAX_VALUE)
                    Triple(mediaItems, startIndex, startPositionMs)
                }
            basePlayer.apply {
                musicServiceConnection.setPlaybackSpeed(contentState.playerSpeed)
                setMediaItems(mediaItems, startIndex, startPositionMs)
                prepare()
                playWhenReady = true
            }
        } else {
            basePlayer.play()
        }
    }

    fun addListener(listener: Player.Listener) {
        basePlayer.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        basePlayer.removeListener(listener)
    }

    data class ContentPlaybackState(
        val mediaItems: List<MediaItem>,
        val currentMediaItem: MediaItem?,
        val currentPositionMs: Long,
        val playWhenReady: Boolean,
        val playerSpeed: Float,
    )

    private class ImaProgressTracker(
        private val sendProgressUpdate: () -> Unit
    ) : Handler.Callback {
        private val messageHandler: Handler = Handler(Looper.getMainLooper(), this)

        override fun handleMessage(msg: Message): Boolean {
            when (msg.what) {
                QUIT -> messageHandler.removeMessages(UPDATE)
                UPDATE, START -> {
                    sendProgressUpdate()
                    messageHandler.removeMessages(UPDATE)
                    messageHandler.sendEmptyMessageDelayed(UPDATE, UPDATE_PERIOD_MS.toLong())
                }

                else -> {}
            }
            return true
        }

        fun start() {
            messageHandler.sendEmptyMessage(START)
        }

        fun stop() {
            messageHandler.sendMessageAtFrontOfQueue(Message.obtain(messageHandler, QUIT))
        }

        companion object {
            const val START: Int = 0
            const val UPDATE: Int = 1
            const val QUIT: Int = 2
            const val UPDATE_PERIOD_MS: Int = 1000
        }
    }

    companion object {
        private const val TAG = "ImaAdsPlayer"
    }
}
