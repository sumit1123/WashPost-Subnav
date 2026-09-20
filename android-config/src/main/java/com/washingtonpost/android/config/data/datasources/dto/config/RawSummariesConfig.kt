package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.RealtimeFeedback
import com.washingtonpost.android.config.domain.models.config.RealtimeSummary
import com.washingtonpost.android.config.domain.models.config.SummariesConfig

@JsonClass(generateAdapter = true)
data class RawSummariesConfig(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "realtimeSummary") val realtimeSummary: RawRealtimeSummary? = null,
    @Json(name = "realtimeFeedback") val realtimeFeedback: RawRealtimeFeedback? = null
) {
    fun mapToDomain(): SummariesConfig {
        return SummariesConfig(
            enabled = enabled ?: true,
            realtimeSummary = (realtimeSummary ?: RawRealtimeSummary()).mapToDomain(),
            realtimeFeedback = (realtimeFeedback ?: RawRealtimeFeedback()).mapToDomain()
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawRealtimeSummary(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "url") val url: String? = null
) {
    fun mapToDomain(): RealtimeSummary {
        return RealtimeSummary(
            enabled = enabled ?: true,
            url = url ?: "https://searchapi.washingtonpost.com/realtime/summary",
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawRealtimeFeedback(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "url") val url: String? = null
) {
    fun mapToDomain(): RealtimeFeedback {
        return RealtimeFeedback(
            enabled = enabled ?: true,
            url = url ?: "https://searchapi.washingtonpost.com/realtime/feedback",
        )
    }
}
