package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ChartbeatConfig

@JsonClass(generateAdapter = true)
data class RawChartbeatConfig(
    @Json(name = "accountId") val accountId: String? = null,
    @Json(name = "domain") val domain: String? = null,
) {
    fun mapToDomain(): ChartbeatConfig {
        return ChartbeatConfig(
            accountId = accountId.orEmpty(),
            domain = domain.orEmpty(),
        )
    }
}
