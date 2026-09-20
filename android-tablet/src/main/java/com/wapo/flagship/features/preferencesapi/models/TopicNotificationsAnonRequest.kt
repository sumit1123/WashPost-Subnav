package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TopicNotificationsAnonRequest(
    @SerializedName("value")
    @Json(name = "value")
    override val value: List<String>,
    @SerializedName("deviceId")
    @Json(name = "deviceId")
    val deviceId: String,
    @SerializedName("timestamp")
    @Json(name = "timestamp")
    val timestamp: Long,
) : PreferencesApiSetRequest
