package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.OneLinkGenerationConfig

@JsonClass(generateAdapter = true)
data class RawOneLinkGenerationConfig(
    @Json(name = "url") val url: String? = null,
) {
    fun mapToDomain(): OneLinkGenerationConfig {
        return OneLinkGenerationConfig(
            url = url
                ?: "https://washpost-apps-origin-prod.site.aws.wapo.pub/deeplink/",
        )
    }
}