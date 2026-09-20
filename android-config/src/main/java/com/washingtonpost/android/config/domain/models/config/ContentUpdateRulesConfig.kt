package com.washingtonpost.android.config.domain.models.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContentUpdateRulesConfig(
    @Json(name = "sectionsUiRequestsTimeout") val sectionsUiRequestsTimeout: Long,
    @Json(name = "sectionsColdUpdatesInterval") val sectionsColdUpdatesInterval: Long,
    @Json(name = "sectionsSyncLimit") val sectionsSyncLimit: Int,
    @Json(name = "foregroundSyncTimeInSeconds") val foregroundSyncTimeInSeconds: Long,
) {
    companion object {
        const val SECTIONS_UI_REQUESTS_TIMEOUT = 10000L
        const val SECTIONS_COLD_UPDATES_INTERVAL = 1800000L
    }
}