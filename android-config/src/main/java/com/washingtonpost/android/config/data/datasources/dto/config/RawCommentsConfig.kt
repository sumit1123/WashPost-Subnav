package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.CommentsConfig

@JsonClass(generateAdapter = true)
data class RawCommentsConfig(
    @Json(name = "url")
    val url: String? = null,
) {
    fun mapToDomain(): CommentsConfig {
        return CommentsConfig(
            url = url
                ?: "https://tabletapi.washingtonpost.com/apps-data-service/article-metadata.json/",
        )
    }
}