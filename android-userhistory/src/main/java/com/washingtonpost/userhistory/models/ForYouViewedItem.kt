/* Copyright (c) 2024 The Washington Post. All rights reserved. */

package com.washingtonpost.userhistory.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.userhistory.remote.UserHistoryEventType

@JsonClass(generateAdapter = true)
data class ForYouViewedItem(
    @SerializedName("articleid")
    @Json(name = "articleid")
    val articleId: String?,

    @SerializedName("wapo_login_id")
    @Json(name = "wapo_login_id")
    val wapoLoginId: String?,

    @SerializedName("j_ucid")
    @Json(name = "j_ucid")
    val jucId: String?,

    @SerializedName("j_tid")
    @Json(name = "j_tid")
    val jtId: String?,

    @SerializedName("reading_time_milli")
    @Json(name = "reading_time_milli")
    var readingTimeMilli: Long? = null,

    @SerializedName("foryou_event")
    @Json(name = "foryou_event")
    val forYouEvent: String?,

    @SerializedName("surface")
    @Json(name = "surface")
    val surface: String,

    @SerializedName("surface_variant")
    @Json(name = "surface_variant")
    val surfaceVariant: String,

    @SerializedName("pageview_id")
    @Json(name = "pageview_id")
    val pageViewId: String,

    @SerializedName("recipe_id")
    @Json(name = "recipe_id")
    val recipeId: String?,

    @SerializedName("test_id")
    @Json(name = "test_id")
    val testId: String?,

    @SerializedName("rec_reason")
    @Json(name = "rec_reason")
    val recReason: String?,

    @SerializedName("position")
    @Json(name = "position")
    val position: Int,

    @SerializedName("clicked")
    @Json(name = "clicked")
    var clicked: Boolean = false,

    @SerializedName("listened")
    @Json(name = "listened")
    var listened: Boolean = false,

    @SerializedName("shared")
    @Json(name = "shared")
    var shared: Boolean = false,

    @SerializedName("added_playlist")
    @Json(name = "added_playlist")
    var addedPlaylist: Boolean = false,

    @SerializedName("removed_playlist")
    @Json(name = "removed_playlist")
    var removedPlaylist: Boolean = false,

    @SerializedName("saved_story")
    @Json(name = "saved_story")
    var savedStory: Boolean = false,

    @SerializedName("gifted")
    @Json(name = "gifted")
    var gifted: Boolean = false,

    @SerializedName("recommendation_content_type")
    @Json(name = "recommendation_content_type")
    var contentType: String? = null,

    @SerializedName("watch_total_sec")
    @Json(name = "watch_total_sec")
    var autoplayDuration: Long = 0L,

    @SerializedName("video_total_sec")
    @Json(name = "video_total_sec")
    var videoDuration: Long = 0L,

    /** ISO8601 format */
    @SerializedName("client_event_time")
    @Json(name = "client_event_time")
    override var clientEventTime: String,

    @SerializedName("interface")
    @Json(name = "interface")
    override val interfase: String = "android",

    @SerializedName("event_type")
    @Json(name = "event_type")
    override val eventType: String = UserHistoryEventType.FY_VIEWED.eventName,
    @SerializedName("foryou_feed_session_id")
    @Json(name = "foryou_feed_session_id")
    val forYouFeedSessionId: String = "",
) : UserHistoryBaseEvent(
    eventType = eventType,
    interfase = interfase,
    clientEventTime = clientEventTime
)
