package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.DeepLinkConfig
import com.washingtonpost.android.config.domain.models.config.DeepLinkDestination

@JsonClass(generateAdapter = true)
data class RawDeepLinkConfig(
    @Json(name = "excludeList") val externalExcludeList: List<String>? = null,
    @Json(name = "internalExcludeList") val internalExcludeList: List<String>? = null,
    @Json(name = "externalPurchaseUrls") val externalPurchaseUrlList: List<String>? = null,
    @Json(name = "paramExcludeList") val paramExcludeList: List<String>? = null,
    @Json(name = "simpleWebViewList") val simpleWebViewList: List<String>? = null,
    @Json(name = "allowedHosts") val allowedHosts: List<String>? = null,
    @Json(name = "validSourceAppValues") val validSourceAppValues: List<String>? = null,
    @Json(name = "destinations") val destinations: List<RawDeepLinkDestination>? = null,
    @Json(name = "ungiftedURLs") val ungiftedURLs: List<String>? = null,

    ) {
    fun mapToDomain(): DeepLinkConfig {
        return DeepLinkConfig(
            externalExcludeList = externalExcludeList.orEmpty(),
            internalExcludeList = internalExcludeList.orEmpty(),
            externalPurchaseUrlList = externalPurchaseUrlList.orEmpty(),
            paramExcludeList = paramExcludeList.orEmpty(),
            simpleWebViewList = simpleWebViewList.orEmpty(),
            allowedHosts = allowedHosts.orEmpty(),
            validSourceAppValues = validSourceAppValues.orEmpty(),
            destinations = destinations?.map { it.mapToDomain() }.orEmpty(),
            ungiftedURLs = ungiftedURLs.orEmpty()
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawDeepLinkDestination(
    @Json(name = "scheme") val scheme: String? = null,
    @Json(name = "domain") val domain: String? = null,
    @Json(name = "path") val path: String? = null,
    @Json(name = "destination") val destination: String? = null,
) {
    fun mapToDomain(): DeepLinkDestination {
        return DeepLinkDestination(
            scheme = scheme.orEmpty(),
            domain = domain.orEmpty(),
            path = path.orEmpty(),
            destination = destination.orEmpty(),
        )
    }
}
