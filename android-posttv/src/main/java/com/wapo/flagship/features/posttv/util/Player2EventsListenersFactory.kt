/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.posttv.util

import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.wapo.flagship.features.posttv.R
import com.wapo.flagship.features.posttv.model.ControllerViewEvent
import com.wapo.flagship.features.posttv.model.PlaybackState
import java.util.UUID

/**
 * Factory method to map [Player.EventListener] methods to [PlaybackState]
 */
fun playbackStateListener(
    playerState: MutableLiveData<PlaybackState>,
    player: ExoPlayer?,
    fetchPercentage: Float?
) =
    object : Player.Listener {
        val handler = Handler(Looper.getMainLooper())
        var playerId: String? = null

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            playerId = UUID.randomUUID().toString()
        }

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                ExoPlayer.STATE_IDLE -> playerState.value = PlaybackState.Idle
                ExoPlayer.STATE_BUFFERING -> playerState.value = PlaybackState.Buffering
                ExoPlayer.STATE_READY -> playerState.value = PlaybackState.Ready
                ExoPlayer.STATE_ENDED -> playerState.value = PlaybackState.Ended
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            playerState.value = PlaybackState.Error(error)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            playerState.value = PlaybackState.PositionDiscontinuity(reason)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (player == null) return

            if (playerState.value != PlaybackState.Ended) {
                playerState.value = PlaybackState.CancelCountdownTimer
            }
            if (isPlaying) {
                handler.postDelayed(this::getCurrentPlayerPosition, 1000)
            }
        }

        private fun getCurrentPlayerPosition() {
            if (player == null) return

            val remainingDuration = player.duration - player.currentPosition
            if (remainingDuration in 4500..5500) {
                playerState.value = PlaybackState.ShowCountdownTimer
            } else if (remainingDuration in 1..4500 || remainingDuration > 5500) {
                if (player.isPlaying) {
                    fetchPercentage?.let { it ->
                        val fetchDuration = (player.contentDuration.times(it))
                        if (fetchDuration <= player.contentPosition) {
                            playerId?.let {
                                playerState.value = PlaybackState.FetchDurationReached(it)
                            }
                        }
                    }
                    handler.postDelayed(this::getCurrentPlayerPosition, 1000)
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            super.onTracksChanged(tracks)
            // onPlaybackStateChanged is not getting called sometimes for ExoPlayer.EVENT_TRACKS_CHANGED event.
            // So updating state here as this method gets called whenever Player#getCurrentTracks() changes.
            playerState.value = PlaybackState.TrackChanged
        }
    }

/**
 * Factory method to map [View.OnClickListener] methods to [ControllerViewEvent]
 */
fun controllerViewClickListener(controllerViewEvent: MutableLiveData<ControllerViewEvent>) =
    View.OnClickListener { v ->
        when (v?.id) {
            R.id.exo_share -> controllerViewEvent.value = ControllerViewEvent.Share
            R.id.exo_cc -> controllerViewEvent.value = ControllerViewEvent.Captions
            R.id.exo_volume -> controllerViewEvent.value = ControllerViewEvent.Volume
            R.id.exo_mute -> controllerViewEvent.value = ControllerViewEvent.Mute
            R.id.exo_unmute -> controllerViewEvent.value = ControllerViewEvent.Unmute
            androidx.media3.ui.R.id.exo_play -> controllerViewEvent.value = ControllerViewEvent.Play
            androidx.media3.ui.R.id.exo_pause -> controllerViewEvent.value = ControllerViewEvent.Pause
            R.id.exo_fullscreen -> controllerViewEvent.value = ControllerViewEvent.FullScreen
            R.id.exo_pip -> controllerViewEvent.value = ControllerViewEvent.PictureInPicture
        }
    }