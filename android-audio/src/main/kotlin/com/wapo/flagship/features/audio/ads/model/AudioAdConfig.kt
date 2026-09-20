package com.wapo.flagship.features.audio.ads.model

import android.os.Parcelable
import com.wapo.flagship.features.audio.PlayerType
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioAdConfig(
    val contentType: PlayerType,
    val adBreaks: List<AudioAdBreak>,
): Parcelable

@Parcelize
data class AudioAdBreak(
    val type: AudioAdBreakType,
    val adTagUrl: String,
    val timeMs: Long? = null,
): Parcelable

enum class AudioAdBreakType { PREROLL, MIDROLL, POSTROLL }