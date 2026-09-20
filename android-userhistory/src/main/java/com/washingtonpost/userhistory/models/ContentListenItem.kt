package com.washingtonpost.userhistory.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.userhistory.remote.UserHistoryEventType

@JsonClass(generateAdapter = true)
data class ContentListenItem(
    @SerializedName("contentid")
    @Json(name="contentid")
    val contentId: String,

    @SerializedName("wapo_login_id")
    @Json(name="wapo_login_id")
    val wapoLoginId: String?,

    @SerializedName("j_ucid")
    @Json(name="j_ucid")
    val jucId: String?,

    @SerializedName("listen_depth_sec")
    @Json(name="listen_depth_sec")
    var listenDepthSec: Int = -1,

    @SerializedName("content_total_sec")
    @Json(name="content_total_sec")
    var contentTotalSec: Int = -1,

    @SerializedName("conclusion_state")
    @Json(name="conclusion_state")
    var conclusionState: String = ConclusionState.UNKNOWN.stateName,

    /** ISO8601 format */
    @SerializedName("client_event_time")
    @Json(name="client_event_time")
    override var clientEventTime: String,

    @SerializedName("interface")
    @Json(name="interface")
    override val interfase: String = "android",

    @SerializedName("event_type")
    @Json(name="event_type")
    override val eventType: String = UserHistoryEventType.CONTENT_LISTEN.eventName,

    @SerializedName("content_type")
    @Json(name="content_type")
    var contentType: String = ContentType.UNKNOWN.typeName,

    ): UserHistoryBaseEvent(
    eventType = eventType,
    interfase = interfase,
    clientEventTime = clientEventTime
)

enum class ConclusionState(val stateName: String) {
    NEXT("next"),
    PREVIOUS("previous"),
    CONTENT_SELECT("content-select"),
    PAUSE_DISMISSED("pause-dismissed"),
    PLAY_DISMISSED("play-dismissed"),
    END("end"),
    OTHER("other"),
    UNKNOWN("unknown")
}

enum class ContentType(val typeName: String) {
    AUDIO_ARTICLE("audio_article"),
    PODCAST("podcast"),
    UNKNOWN("unknown");
}
