package com.wapo.flagship.di.app.modules.features.readinghistory

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReadingHistoryServiceGetResponse (
    @Json(name = "status")
    val status: String?,
    @Json(name= "state")
    val state: String?,
    @Json(name= "readingHistoryV3")
    val readingHistory: ReadingHistory?

)

@JsonClass(generateAdapter = true)
data class ReadingHistory(
    @Json(name="data")
    val data: List<Data>,
)

@JsonClass(generateAdapter = true)
data class Data(
    @Json(name = "content_type")
    val contentType: String?,
    @Json(name = "content_id")
    val contentId: String?,
    @Json(name = "percent_consumed")
    val percentConsumed: Float?,
    @Json(name = "display_date")
    val displayDate: String?,
    @Json(name = "reading_time_milli")
    val readingTimeMilli: Long?,
    @Json(name="listen_depth_sec")
    val listenDepthSec: Long?,
    @Json(name="deepest_scroll_id")
    val deepestScrollId: String?,
    @Json(name="canonical_url")
    val canonicalUrl: String?,
    @Json(name="link_url")
    val linkUrl: String?,
    @Json(name="hidden")
    val hidden: Boolean?,
    @Json(name="media_id")
    val mediaId: String?,
    @Json(name="image_url")
    val imageUrl: String?,
    @Json(name="title")
    val title: String?,
    @Json(name="stream_url")
    val streamUrl: String?,
    @Json(name="label")
    val label: String?,
)