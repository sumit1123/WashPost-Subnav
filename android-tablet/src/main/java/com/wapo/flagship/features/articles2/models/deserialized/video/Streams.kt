package com.wapo.flagship.features.articles2.models.deserialized.video

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Streams(
    @Json(name = "gif")
    val gif: List<Gif>?,
    @Json(name = "gif-mp4")
    val gifMp4: List<GifMp4>?,
    @Json(name = "mp4")
    val mp4: List<Mp4>?,
    @Json(name = "ts")
    val ts: List<T>?,
)
