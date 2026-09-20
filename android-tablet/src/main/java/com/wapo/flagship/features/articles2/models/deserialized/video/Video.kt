package com.wapo.flagship.features.articles2.models.deserialized.video

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.flagship.features.articles2.models.Item
import com.wapo.flagship.features.articles2.models.deserialized.Omniture
import kotlin.collections.LinkedHashMap

@JsonClass(generateAdapter = true)
data class Video(
    @Json(name = "adconfig")
    val adconfig: Adconfig?,
    @Json(name = "blurb")
    val blurb: String?,
    @Json(name = "contenturl")
    val contenturl: String?,
    @Json(name = "credits")
    val credits: String?,
    @Json(name = "duration")
    val duration: Long?,
    @Json(name = "embedCode")
    val embedCode: String?,
    @Json(name = "fallback")
    val fallback: String?,
    @Json(name = "fullcaption")
    val fullcaption: String?,
    @Json(name = "host")
    val host: Host?,
    @Json(name = "id")
    val id: String?,
    @Json(name = "imageHeight")
    val imageHeight: Int?,
    @Json(name = "imageURL")
    val imageURL: String?,
    @Json(name = "imageWidth")
    val imageWidth: Int,
    @Json(name = "isLive")
    val isLive: Boolean?,
    @Json(name = "lmt")
    val lmt: Long?,
    @Json(name = "mediaURL")
    val mediaURL: String?,
    @Json(name = "omniture")
    val omniture: Omniture?,
    @Json(name = "placement")
    val placement: Any?,
    @Json(name = "published")
    val published: Long?,
    @Json(name = "shareurl")
    val shareurl: String?,
    @Json(name = "streamURL")
    val streamURL: String?,
    @Json(name = "streams")
    val streams: Streams?,
    @Json(name = "subtitlesURL")
    val subtitlesURL: String?,
    @Json(name = "title")
    val title: String?,
    @Json(name = "url")
    val url: String?,
    @Json(name = "vertical")
    val vertical: Boolean?,
    @Json(name = "widthFactor")
    val widthFactor: Any?,
    @Json(name = "type")
    override val type: String?,
    @Json(name = "content")
    val content: Content?,
    @Json(name = "autoplay")
    val autoplay: Boolean?,
    @Json(name = "isLooping")
    val isLooping: Boolean?,
    @Json(name = "promo")
    val promo: Promo?,
) : Item(type = type) {

    data class Stream(val bitrate: Int?, val url: String?)

    fun getStreamUrl(
        mobileMaxBitRate: Int,
        tabletMaxBitRate: Int
    ): String? {
        val nonNullStreams = mutableListOf<Stream>().apply {
            addAll(streams?.mp4?.map { Stream(it.bitrate, it.url) } ?: emptyList())
            addAll(streams?.ts?.map { Stream(it.bitrate, it.url) } ?: emptyList())
        }
        if (nonNullStreams.isEmpty()) return null
        val preferredStreams = arrayOf("_master.m3u8", "_mobile.m3u8", ".m3u8", ".mp4", ".webm")
        val maxBitRate = if (AppContextUtils.isTablet()) tabletMaxBitRate else mobileMaxBitRate
        if (nonNullStreams.isNotEmpty()) {
            val preferredMap: LinkedHashMap<String, MutableList<Stream>> = LinkedHashMap()
            for (preferredStream in preferredStreams) {
                if (preferredMap[preferredStream] == null) {
                    preferredMap[preferredStream] = mutableListOf()
                }
                //group urls by preferred streams && reject streams that exceed max bit rate && sort by max bit rate or greatest width
                preferredMap[preferredStream]?.addAll(
                    nonNullStreams.filter {
                        it.url?.endsWith(preferredStream) == true && it.bitrate?.let { bitRate -> bitRate <= maxBitRate } ?: false
                    }.sortedByDescending {
                        it.bitrate ?: 0
                    }.toList()
                )
                Logger.d("WpVideoMapper", "$preferredStream: ${preferredMap[preferredStream]}")
            }
            val preferredStreamFormat =
                preferredMap.keys.firstOrNull { preferredMap[it]?.isNotEmpty() ?: false }
            Logger.d(
                "WpVideoMapper",
                "preferred url: ${preferredMap[preferredStreamFormat]?.firstOrNull()?.url}"
            )
            return preferredMap[preferredStreamFormat]?.firstOrNull()?.url ?: nonNullStreams.firstOrNull()?.url
        }
        return null
    }
}
