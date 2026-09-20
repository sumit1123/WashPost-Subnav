package com.washingtonpost.userhistory.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.washingtonpost.userhistory.remote.UserHistoryEventType

data class DefaultHeadlineViewEvent(
    @SerializedName("client_event_time")
    @Json(name = "client_event_time")
    override val clientEventTime: String,

    @SerializedName("event_type")
    @Json(name = "event_type")
    override val eventType: String = UserHistoryEventType.HEADLINE_VIEW.eventName,

    @SerializedName("event_subtype")
    @Json(name = "event_subtype")
    val eventSubType: String? = null,

    @SerializedName("articleid")
    @Json(name = "articleid")
    val articleId: String,

    @SerializedName("wapo_login_id")
    @Json(name = "wapo_login_id")
    val wapoLoginId: String?,

    @SerializedName("j_ucid")
    @Json(name = "j_ucid")
    val jucId: String?,

    @SerializedName("interface")
    @Json(name = "interface")
    override val interfase: String = "android",

    @SerializedName("surface")
    @Json(name = "surface")
    val surface: String,

    @SerializedName("surface_variant")
    @Json(name = "surface_variant")
    val surfaceVariant: String? = "default",

    @SerializedName("recommendation_content_type")
    @Json(name = "recommendation_content_type")
    val recommendationContentType: String?,

    @SerializedName("request_id")
    @Json(name = "request_id")
    val requestId: String?,

    @SerializedName("current_url")
    @Json(name = "current_url")
    val currentUrl: String,

    @SerializedName("currently_page_articleid")
    @Json(name = "currently_page_articleid")
    val currentlyPageArticleId: String? = null,

    @SerializedName("module_category")
    @Json(name = "module_category")
    val moduleCategory: String,

    @SerializedName("module_position")
    @Json(name = "module_position")
    val modulePosition: Int,

    @SerializedName("position_in_module")
    @Json(name = "position_in_module")
    val positionInModule: Int,

    @SerializedName("pageview_id")
    @Json(name = "pageview_id")
    val pageViewId: String? = null,

    @SerializedName("j_tid")
    @Json(name = "j_tid")
    val jtid: String?,

    @SerializedName("reading_time_milli")
    @Json(name = "reading_time_milli")
    var readingTimeMilli: Long? = null,

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

    @SerializedName("removed_save_story")
    @Json(name = "removed_save_story")
    var removedSaveStory: Boolean = false,

    @SerializedName("gifted")
    @Json(name = "gifted")
    var gifted: Boolean = false,
) : UserHistoryBaseEvent(
    eventType = eventType,
    interfase = interfase,
    clientEventTime = clientEventTime,
)