package com.washingtonpost.android.config.data.datasources.dto.config.paywall

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywall.GlobalBanner
import com.washingtonpost.android.config.domain.models.config.paywall.GlobalBannerConfig

/**
 * Data class that holds the configuration for global banners
 */
@JsonClass(generateAdapter = true)
data class RawGlobalBannerConfig(
    @Json(name = "banners") val banners: List<RawGlobalBanner>? = null,
) {
    fun mapToDomain(): GlobalBannerConfig {
        return GlobalBannerConfig(
            banners = banners?.map { it.mapToDomain() }.orEmpty(),
        )
    }
}

/**
 * Data class that holds the information for a single global banner
 */
@JsonClass(generateAdapter = true)
data class RawGlobalBanner(
    @Json(name = "id") val id: String? = null,
    @Json(name = "productId") val productId: String? = null,
    @Json(name = "offerId") val offerId: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "productName") val productName: String? = null,
    @Json(name = "action") val action: String? = null
) {
    fun mapToDomain(): GlobalBanner {
        return GlobalBanner(
            id = id,
            productId = productId,
            offerId = offerId,
            title = title,
            subtitle = subtitle,
            url = url,
            productName = productName,
            action = action
        )
    }
}