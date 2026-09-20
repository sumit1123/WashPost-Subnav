package com.wapo.flagship.features.articles2.models.deserialized

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.articles2.models.AudioTracking
import com.wapo.flagship.features.articles2.models.Item

@JsonClass(generateAdapter = true)
data class Audio(
    @Json(name = "mediaID")
    val mediaId: String?,
    @Json(name = "adsUrl")
    val adsUrl: String?,
    @Json(name = "rawUrl")
    val rawUrl: String?,
    @Json(name = "subtype")
    val subtype: String?,
    @Json(name = "manifestUrl")
    val manifestUrl: String?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "image")
    val image: Image?,
    @Json(name = "duration")
    val duration: Long?,
    @Json(name = "caption")
    val caption: String?,
    @Json(name = "inlinePlayer")
    val inlinePlayer: InlinePlayer?,
    @Json(name = "title")
    val title: Title?,
    @Json(name = "label")
    val label: Kicker?,
    @Json(name = "tracking")
    val tracking: AudioTracking?,
    @Json(name="transitions")
    var transitions: List<Audio>?,
    @Json(name = "children")
    var children: List<Audio>?,
    @Json(name = "url")
    val url: String?,
    @Json(name = "adconfig")
    val adConfig: ArticleAudioAdConfig? = null,
    @Json(name = "series")
    val series: String? = null,
) : Item(type = type) {
    enum class SubType(val value: String) {
        STANDALONE("standalone")
    }
}
