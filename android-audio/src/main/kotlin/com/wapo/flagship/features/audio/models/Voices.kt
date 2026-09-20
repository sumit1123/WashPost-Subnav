package com.wapo.flagship.features.audio.models

import com.google.gson.annotations.SerializedName

/**
 * This is a response model for the request made to download the list pof available voices [voices]
 */
data class Voices(
    @SerializedName("voices")
    val voices: List<Voice>?
)