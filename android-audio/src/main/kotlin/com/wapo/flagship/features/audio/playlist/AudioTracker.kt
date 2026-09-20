package com.wapo.flagship.features.audio.playlist

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class AudioTracker(
    @Json(name = "tab_name") val tabName: String? = null,
    @Json(name = "app_section") val appSection: String? = null,
    @Json(name = "tracking_info") val trackingInfo: AudioTrackingInfo? = null,
    @Json(name = "speed") var speed: Float = 1f,
    @Json(name = "voice") var voice: String? = null,
    @Json(name = "feed") val feed: String? = null,
    @Json(name = "is_flex_audio") val isFlexAudio: Boolean = false,
    @Json(name = "is_action_button") val isActionButton: Boolean = false,
    @Json(name = "is_audio_carousel") val isAudioCarousel: Boolean = false,
    @Json(name = "is_action_audio") var isActionAudio: Boolean = false,
    @Json(name = "is_audio_playlist") var isAudioPlaylist: Boolean = false,
    @Json(name = "av_name") val avName: String? = null,
)
