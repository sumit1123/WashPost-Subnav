package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ContentUpdateRulesConfig

@JsonClass(generateAdapter = true)
data class RawContentUpdateRulesConfig(
    @Json(name = "sectionsUiRequestsTimeout") val sectionsUiRequestsTimeout: Long? = null,
    @Json(name = "sectionsColdUpdatesInterval") val sectionsColdUpdatesInterval: Long? = null,
    @Json(name = "sectionsSyncLimit") val sectionsSyncLimit: Int? = null,
    @Json(name = "foregroundSyncTimeInSeconds") val foregroundSyncTimeInSeconds: Long? = null,
) {
    fun mapToDomain(): ContentUpdateRulesConfig {
        return ContentUpdateRulesConfig(
            sectionsUiRequestsTimeout = sectionsUiRequestsTimeout
                ?: ContentUpdateRulesConfig.SECTIONS_UI_REQUESTS_TIMEOUT,
            sectionsColdUpdatesInterval = sectionsColdUpdatesInterval
                ?: ContentUpdateRulesConfig.SECTIONS_COLD_UPDATES_INTERVAL,
            sectionsSyncLimit = sectionsSyncLimit ?: Int.MAX_VALUE,
            foregroundSyncTimeInSeconds = foregroundSyncTimeInSeconds ?: 90,
        )
    }
}