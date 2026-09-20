package com.washingtonpost.userhistory.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class UserHistoryEvents(
    @SerializedName("events")
    @Json(name="events")
    val events: List<Any> = mutableListOf()
)
