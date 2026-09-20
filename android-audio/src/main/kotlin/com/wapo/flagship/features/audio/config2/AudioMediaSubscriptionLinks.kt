package com.wapo.flagship.features.audio.config2

import com.google.gson.annotations.SerializedName

data class AudioMediaSubscriptionLinks(
    @SerializedName("alexa") val alexa: String? = null,
    @SerializedName("apple_podcasts") val applePodcasts: String? = null,
    @SerializedName("google_play") val googlePlay: String? = null,
    @SerializedName("iheart_radio") val iheartRadio: String? = null,
    @SerializedName("radio_public") val radioPublic: String? = null,
    @SerializedName("rss") val rss: String? = null,
    @SerializedName("spotify") val spotify: String? = null,
    @SerializedName("stitcher") val stitcher: String? = null,
    @SerializedName("tune_in") val tuneIn: String? = null,
)
