package com.washingtonpost.userhistory.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.userhistory.remote.UserHistoryEventType

@JsonClass(generateAdapter = true)
data class HabitTileViewItem(
    @SerializedName("tile_link")
    @Json(name="tile_link")
    val tileLink: String?,

    @SerializedName("wapo_login_id")
    @Json(name="wapo_login_id")
    val wapoLoginId: String?,

    @SerializedName("j_ucid")
    @Json(name="j_ucid")
    val jucId: String?,

    @SerializedName("request_id")
    @Json(name="request_id")
    val requestId: String?,

    @SerializedName("surface")
    @Json(name="surface")
    val surface: String,

    @SerializedName("position")
    @Json(name="position")
    val position: Int,

    @SerializedName("tile_category")
    @Json(name="tile_category")
    val tileCategory: String?,

    @SerializedName("tile_label")
    @Json(name="tile_label")
    val tileLabel: String?,

    @SerializedName("tile_category_detail")
    @Json(name="tile_category_detail")
    val tileCategoryDetail: String?,

    @SerializedName("clicked")
    @Json(name="clicked")
    var clicked: Boolean = false,

    /** ISO8601 format */
    @SerializedName("client_event_time")
    @Json(name = "client_event_time")
    override val clientEventTime: String,

    @SerializedName("interface")
    @Json(name="interface")
    override val interfase: String = "android",

    @SerializedName("event_type")
    @Json(name="event_type")
    override val eventType: String = UserHistoryEventType.HABIT_TILE_VIEWED.eventName,

    @SerializedName("test_group")
    @Json(name="test_group")
    val testGroup: String?
): UserHistoryBaseEvent(
    eventType = eventType,
    interfase = interfase,
    clientEventTime = clientEventTime
)
