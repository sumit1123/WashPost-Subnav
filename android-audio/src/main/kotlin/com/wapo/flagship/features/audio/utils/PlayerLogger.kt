package com.wapo.flagship.features.audio.utils

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.wapo.flagship.features.audio.ads.util.AdPlayerUtils.adsConfig
import com.wapo.flagship.features.audio.ads.util.AdPlayerUtils.isAd
import jakarta.inject.Inject

class PlayerLogger @Inject constructor() {
    fun playbackStateToString(playbackState: Int): String {
        return when (playbackState) {
            Player.STATE_IDLE -> "STATE_IDLE"
            Player.STATE_BUFFERING -> "STATE_BUFFERING"
            Player.STATE_READY -> "STATE_READY"
            Player.STATE_ENDED -> "STATE_ENDED"
            else -> "STATE_UNKNOWN"
        }
    }

    fun playerEventsToString(events: Player.Events): String {
        val result = mutableListOf<String>()
        for (i in 0 until events.size()) {
            result.add(playerEventToString(events.get(i)))
        }
        return result.joinToString()
    }

    fun playerEventToString(event: Int): String {
        return when (event) {
            Player.EVENT_TIMELINE_CHANGED -> "EVENT_TIMELINE_CHANGED"
            Player.EVENT_MEDIA_ITEM_TRANSITION -> "EVENT_MEDIA_ITEM_TRANSITION"
            Player.EVENT_TRACKS_CHANGED -> "EVENT_TRACKS_CHANGED"
            Player.EVENT_IS_LOADING_CHANGED -> "EVENT_IS_LOADING_CHANGED"
            Player.EVENT_PLAYBACK_STATE_CHANGED -> "EVENT_PLAYBACK_STATE_CHANGED"
            Player.EVENT_PLAY_WHEN_READY_CHANGED -> "EVENT_PLAY_WHEN_READY_CHANGED"
            Player.EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED -> "EVENT_PLAYBACK_SUPPRESSION_REASON_CHANGED"
            Player.EVENT_IS_PLAYING_CHANGED -> "EVENT_IS_PLAYING_CHANGED"
            Player.EVENT_REPEAT_MODE_CHANGED -> "EVENT_REPEAT_MODE_CHANGED"
            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED -> "EVENT_SHUFFLE_MODE_ENABLED_CHANGED"
            Player.EVENT_PLAYER_ERROR -> "EVENT_PLAYER_ERROR"
            Player.EVENT_POSITION_DISCONTINUITY -> "EVENT_POSITION_DISCONTINUITY"
            Player.EVENT_PLAYBACK_PARAMETERS_CHANGED -> "EVENT_PLAYBACK_PARAMETERS_CHANGED"
            Player.EVENT_AVAILABLE_COMMANDS_CHANGED -> "EVENT_AVAILABLE_COMMANDS_CHANGED"
            Player.EVENT_MEDIA_METADATA_CHANGED -> "EVENT_MEDIA_METADATA_CHANGED"
            Player.EVENT_PLAYLIST_METADATA_CHANGED -> "EVENT_PLAYLIST_METADATA_CHANGED"
            Player.EVENT_SEEK_BACK_INCREMENT_CHANGED -> "EVENT_SEEK_BACK_INCREMENT_CHANGED"
            Player.EVENT_SEEK_FORWARD_INCREMENT_CHANGED -> "EVENT_SEEK_FORWARD_INCREMENT_CHANGED"
            Player.EVENT_MAX_SEEK_TO_PREVIOUS_POSITION_CHANGED -> "EVENT_MAX_SEEK_TO_PREVIOUS_POSITION_CHANGED"
            Player.EVENT_TRACK_SELECTION_PARAMETERS_CHANGED -> "EVENT_TRACK_SELECTION_PARAMETERS_CHANGED"
            Player.EVENT_AUDIO_ATTRIBUTES_CHANGED -> "EVENT_AUDIO_ATTRIBUTES_CHANGED"
            Player.EVENT_AUDIO_SESSION_ID -> "EVENT_AUDIO_SESSION_ID"
            Player.EVENT_VOLUME_CHANGED -> "EVENT_VOLUME_CHANGED"
            Player.EVENT_SKIP_SILENCE_ENABLED_CHANGED -> "EVENT_SKIP_SILENCE_ENABLED_CHANGED"
            Player.EVENT_SURFACE_SIZE_CHANGED -> "EVENT_SURFACE_SIZE_CHANGED"
            Player.EVENT_VIDEO_SIZE_CHANGED -> "EVENT_VIDEO_SIZE_CHANGED"
            Player.EVENT_RENDERED_FIRST_FRAME -> "EVENT_RENDERED_FIRST_FRAME"
            Player.EVENT_CUES -> "EVENT_CUES"
            Player.EVENT_METADATA -> "EVENT_METADATA"
            Player.EVENT_DEVICE_INFO_CHANGED -> "EVENT_DEVICE_INFO_CHANGED"
            Player.EVENT_DEVICE_VOLUME_CHANGED -> "EVENT_DEVICE_VOLUME_CHANGED"
            else -> "EVENT_UNKNOWN"
        }
    }

    fun mediaItemTransitionReasonToString(reason: Int): String {
        return when (reason) {
            Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "MEDIA_ITEM_TRANSITION_REASON_REPEAT"
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> "MEDIA_ITEM_TRANSITION_REASON_AUTO"
            Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "MEDIA_ITEM_TRANSITION_REASON_SEEK"
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> "MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED"
            else -> "MEDIA_ITEM_TRANSITION_REASON_UNKNOWN"
        }
    }

    fun discontinuityReasonToString(reason: Int): String {
        return when (reason) {
            Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> "DISCONTINUITY_REASON_AUTO_TRANSITION"
            Player.DISCONTINUITY_REASON_SEEK -> "DISCONTINUITY_REASON_SEEK"
            Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> "DISCONTINUITY_REASON_SEEK_ADJUSTMENT"
            Player.DISCONTINUITY_REASON_SKIP -> "DISCONTINUITY_REASON_SKIP"
            Player.DISCONTINUITY_REASON_REMOVE -> "DISCONTINUITY_REASON_REMOVE"
            Player.DISCONTINUITY_REASON_INTERNAL -> "DISCONTINUITY_REASON_INTERNAL"
            Player.DISCONTINUITY_REASON_SILENCE_SKIP -> "DISCONTINUITY_REASON_SILENCE_SKIP"
            else -> "DISCONTINUITY_REASON_UNKNOWN"
        }
    }

    fun playerDetailsToString(player: Player? = null): String {
        return "Player(player=$player, currentPosition=${player?.currentPosition}, currentMediaItem=${mediaItemDetailsToString(player?.currentMediaItem)})"
    }

    fun mediaItemDetailsToString(mediaItem: MediaItem? = null): String {
        return "MediaItem(mediaItem=$mediaItem, isAd=${mediaItem?.isAd}, adsConfig=${mediaItem?.adsConfig})"
    }

}