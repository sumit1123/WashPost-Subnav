// Copyright (c) 2024 The Washington Post. All rights reserved.

package com.washingtonpost.userhistory.remote

import com.washingtonpost.userhistory.models.UserHistoryEvents
import com.washingtonpost.userhistory.models.UserHistoryServicePostResponse
import com.washingtonpost.userhistory.network.APIResult
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface UserHistoryService {
    @POST("events/")
    suspend fun postUserHistoryEvents(
        @HeaderMap headers: HashMap<String, String>,
        @Body userHistoryEvents: UserHistoryEvents,
    ): APIResult<UserHistoryServicePostResponse>
}

enum class UserHistoryEventType(
    val eventName: String,
) {
    FY_VIEWED("fy_viewed"),
    SCROLL_DEPTH("scroll_depth"),
    CONTENT_LISTEN("content_listen"),
    HABIT_TILE_VIEWED("habit_tile_viewed"),
    PAGEVIEW("pageview"),
    PUSH_ORIGINATED("push_originated"),
    VIDEO_VIEWED("video_watched"),
    HEADLINE_VIEW("headline_view"),
    ALL("all_events")
}

const val USER_HISTORY_EVENT_TYPE = "userHistoryEventType"
const val POST_USER_HISTORY_EVENT_TYPE = "postUserHistoryEvents"
