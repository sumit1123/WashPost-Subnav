package com.washingtonpost.userhistory.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.userhistory.remote.UserHistoryEventType

@JsonClass(generateAdapter = true)
data class PushNotificationViewItem(
    @SerializedName("event_type")
    @Json(name = "event_type")
    override val eventType: String = UserHistoryEventType.PUSH_ORIGINATED.eventName,
    @SerializedName("articleid")
    @Json(name = "articleid")
    val articleId: String,
    @SerializedName("event_subtype")
    @Json(name = "event_subtype")
    val eventSubType: String,
    @SerializedName("canonical_url")
    @Json(name = "canonical_url")
    val canonicalUrl: String,
    @SerializedName("wapo_login_id")
    @Json(name = "wapo_login_id")
    val loginId: String?,
    @SerializedName("j_ucid")
    @Json(name = "j_ucid")
    val jucId: String?,
    @SerializedName("client_event_time")
    @Json(name = "client_event_time")
    override val clientEventTime: String,
    @SerializedName("interface")
    @Json(name = "interface")
    override val interfase: String,
    @SerializedName("surface")
    @Json(name = "surface")
    val surface: String,
    @SerializedName("surface_variant")
    @Json(name = "surface_variant")
    val surfaceVariant: String,
    @SerializedName("push_id")
    @Json(name = "push_id")
    val pushId: String,
    @SerializedName("push_category")
    @Json(name = "push_category")
    val pushCategory: String,
    @SerializedName("test_group")
    @Json(name = "test_group")
    val testGroup: String,
    @SerializedName("clicked")
    @Json(name = "clicked")
    val clicked: Boolean?,
    @SerializedName("shared")
    @Json(name = "shared")
    val shared: Boolean?,
    @SerializedName("deviceId")
    @Json(name="deviceId")
    val deviceId: String?,
    @SerializedName("appVersion")
    @Json(name="appVersion")
    val appVersion: String?,
    @SerializedName("platform")
    @Json(name="platform")
    val devicePlatform: String?,
) : UserHistoryBaseEvent(
    eventType = eventType,
    interfase = interfase,
    clientEventTime = clientEventTime
)
