package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.FindHighlightItemConfig

@JsonClass(generateAdapter = true)
data class RawFindHighlightItemConfig(
    @Json(name = "id") val id: String? = null,
    @Json(name = "position") val position: Int? = null
) {
    fun mapToDomain(): FindHighlightItemConfig {
        return FindHighlightItemConfig(
            id = id.orEmpty(),
            position = position ?: 0,
        )
    }
}
