package com.wapo.flagship.features.audio.models

data class PlaybackAudioArticle(
    val url: String?,
    val titlePrefix: String?,
    val title: String?,
    val displayDate: String?,
    val playbackVoices: List<PlaybackVoice>?
)
