package com.washingtonpost.userhistory.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.userhistory.remote.UserHistoryEventType

@JsonClass(generateAdapter = true)
data class VideoViewItem(
    @SerializedName("event_type")
    @Json(name = "event_type")
    override val eventType: String = UserHistoryEventType.VIDEO_VIEWED.eventName,
    @SerializedName("client_event_time")
    @Json(name = "client_event_time")
    override val clientEventTime: String,
    @SerializedName("interface")
    @Json(name = "interface")
    override val interfase: String,
    @SerializedName("autoplay_duration")
    @Json(name = "autoplay_duration")
    val autoplayDuration: Long?,
    @SerializedName("watch_total_sec")
    @Json(name = "watch_total_sec")
    val watchTotalSec: Long?,
    @SerializedName("video_total_sec")
    @Json(name = "video_total_sec")
    val videoTotalSec: Long,
    @SerializedName("video_surface")
    @Json(name = "video_surface")
    val videoSurface: String,
    @SerializedName("content_type")
    @Json(name = "content_type")
    val contentType: String,
    @SerializedName("conclusion_state")
    @Json(name = "conclusion_state")
    val videoConclusionState: String,
    @SerializedName("wapo_login_id")
    @Json(name = "wapo_login_id")
    val wapoLoginId: String,
    @SerializedName("j_ucid")
    @Json(name = "j_ucid")
    val jucId: String?,
    @SerializedName("content_id")
    @Json(name = "content_id")
    val contentId: String,
) : UserHistoryBaseEvent(
    eventType = eventType,
    interfase = interfase,
    clientEventTime = clientEventTime
)

enum class VideoConclusionState(val stateValue: String) {
    AUTO_COMPLETE("auto-complete"),
    VIDEO_NEXT("next"),
    VIDEO_END("end"),
    VIDEO_BACK("back"),
    SCROLLED_THROUGH("scrolled_through"),
    VIDEO_OPENED("video_opened")
}
