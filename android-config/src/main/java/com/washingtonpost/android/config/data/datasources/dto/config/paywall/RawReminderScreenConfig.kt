package com.washingtonpost.android.config.data.datasources.dto.config.paywall

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywall.ReminderScreenConfig

@JsonClass(generateAdapter = true)
data class RawReminderScreenConfig(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "frequency") val frequency: Long? = null,
) {
    fun mapToDomain(): ReminderScreenConfig {
        return ReminderScreenConfig(
            enabled = enabled ?: true,
            frequency = frequency ?: 86400000,
        )
    }
}