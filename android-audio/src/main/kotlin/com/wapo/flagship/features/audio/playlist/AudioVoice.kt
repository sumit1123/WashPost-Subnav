package com.wapo.flagship.features.audio.playlist

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class AudioVoice(
    @Json(name = "id") val id: String?,
    @Json(name = "text") val text: String?,
    @Json(name = "raw_url") val rawUrl: String?,
    @Json(name = "ads_url") val adsUrl: String?,
    @Json(name = "duration") val duration: Long?
)