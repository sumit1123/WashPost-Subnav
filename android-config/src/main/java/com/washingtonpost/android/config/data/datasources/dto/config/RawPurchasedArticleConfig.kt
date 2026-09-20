package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.PurchasedArticleConfig

@JsonClass(generateAdapter = true)
data class RawPurchasedArticleConfig(
    @Json(name = "baseUrl") val baseUrl: String? = null,
    @Json(name = "metadataServiceBaseUrl") val metadataServiceBaseUrl: String? = null,
) {

    fun mapToDomain(): PurchasedArticleConfig {
        return PurchasedArticleConfig(
            baseUrl = baseUrl ?:"https://subscribe.washingtonpost.com/",
            metadataServiceBaseUrl = metadataServiceBaseUrl ?: "https://api.washingtonpost.com/metadata-service/v1/metadata/canonical/"
        )
    }

}
