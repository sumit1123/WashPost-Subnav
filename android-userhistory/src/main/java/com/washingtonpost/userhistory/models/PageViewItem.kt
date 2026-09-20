package com.washingtonpost.userhistory.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.userhistory.remote.UserHistoryEventType

@JsonClass(generateAdapter = true)
data class PageViewItem(
    @SerializedName("articleid")
    @Json(name = "articleid")
    val articleId: String,

    @SerializedName("wapo_login_id")
    @Json(name = "wapo_login_id")
    val wapoLoginId: String?,

    @SerializedName("j_ucid")
    @Json(name = "j_ucid")
    val jucId: String?,

    @SerializedName("pageview_id")
    @Json(name = "pageview_id")
    val pageViewId: String,

    @SerializedName("j_tid")
    @Json(name = "j_tid")
    val jtId: String?,

    @SerializedName("content_type")
    @Json(name = "content_type")
    val contentType: String,

    /** ISO8601 format */
    @SerializedName("client_event_time")
    @Json(name = "client_event_time")
    override val clientEventTime: String,

    @SerializedName("interface")
    @Json(name = "interface")
    override val interfase: String = "android",

    @SerializedName("event_type")
    @Json(name = "event_type")
    override val eventType: String = UserHistoryEventType.PAGEVIEW.eventName,
): UserHistoryBaseEvent(
    eventType = eventType,
    interfase = interfase,
    clientEventTime = clientEventTime
)
