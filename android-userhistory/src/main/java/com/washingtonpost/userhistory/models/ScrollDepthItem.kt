package com.washingtonpost.userhistory.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.userhistory.remote.UserHistoryEventType

@JsonClass(generateAdapter = true)
data class ScrollDepthItem(
    @SerializedName("articleid")
    @Json(name = "articleid")
    val articleId: String,

    @SerializedName("wapo_login_id")
    @Json(name = "wapo_login_id")
    val wapoLoginId: String?,

    @SerializedName("j_ucid")
    @Json(name = "j_ucid")
    val jucId: String?,

    @SerializedName("j_tid")
    @Json(name = "j_tid")
    val jtId: String?,

    /** Not sent to API, but used to calculate [readingTimeMilli] */
    val readingStartTimeMilli: Long = System.currentTimeMillis(),

    @SerializedName("reading_time_milli")
    @Json(name = "reading_time_milli")
    var readingTimeMilli: Long = -1,

    @SerializedName("pageview_id")
    @Json(name = "pageview_id")
    val pageViewId: String,

    @SerializedName("total_elements")
    @Json(name = "total_elements")
    val totalElements: Int,

    /** 1-based position in the list of valid content items */
    @SerializedName("deepest_scroll_index")
    @Json(name = "deepest_scroll_index")
    var deepestScrollIndex: Int,

    /** The [id] value for the corresponding article [Item] */
    @SerializedName("deepest_scroll_id")
    @Json(name = "deepest_scroll_id")
    var deepestScrollId: String,

    /** 1-based position in the list of valid content items */
    @SerializedName("most_recent_scroll_index")
    @Json(name = "most_recent_scroll_index")
    var mostRecentScrollIndex: Int,

    /** The [id] value for the corresponding article [Item] */
    @SerializedName("most_recent_scroll_id")
    @Json(name = "most_recent_scroll_id")
    var mostRecentScrollId: String,

    /** ISO8601 format */
    @SerializedName("client_event_time")
    @Json(name = "client_event_time")
    override var clientEventTime: String,

    @SerializedName("interface")
    @Json(name = "interface")
    override val interfase: String = "android",

    @SerializedName("event_type")
    @Json(name = "event_type")
    override val eventType: String = UserHistoryEventType.SCROLL_DEPTH.eventName,
): UserHistoryBaseEvent(
    eventType = eventType,
    interfase = interfase,
    clientEventTime = clientEventTime
)
