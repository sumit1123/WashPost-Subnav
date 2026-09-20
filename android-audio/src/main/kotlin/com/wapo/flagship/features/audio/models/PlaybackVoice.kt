/* Copyright (c) 2022 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.audio.models

import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.parcelize.Parcelize

/**
 * Model class for PlaybackVoice UI. Text is the label for the voice and url is the audio stream for the voice.
 */
@Parcelize
open class PlaybackVoice(
    val id: String,
    val text: String,
    val rawUrl: String,
    val adsUrl: String?,
    val duration: Long?
) : Parcelable {

    fun toJson(): String {
        return Gson().toJson(this)
    }
}
