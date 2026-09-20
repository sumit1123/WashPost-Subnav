package com.washingtonpost.userhistory.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserHistoryServicePostResponse(
    @Json(name = "events_received")
    val eventsReceived: Int?,

    @Json(name = "success")
    val success: Boolean?
)
