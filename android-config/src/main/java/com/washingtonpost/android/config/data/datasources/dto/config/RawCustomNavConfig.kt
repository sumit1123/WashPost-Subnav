package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.CustomNavConfig

@JsonClass(generateAdapter = true)
data class RawCustomNavConfig(
    @Json(name = "lockedSections") val lockedSections: List<String>? = null,
) {
    fun mapToDomain(): CustomNavConfig {
        return CustomNavConfig(
            lockedSections = lockedSections.orEmpty(),
        )
    }
}