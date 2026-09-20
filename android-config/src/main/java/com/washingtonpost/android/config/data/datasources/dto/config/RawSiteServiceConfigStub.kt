package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.SiteServiceConfigStub

@JsonClass(generateAdapter = true)
data class RawSiteServiceConfigStub(
    @Json(name = "sectionsBarV2ConfigRemoteLocation") val sectionsBarConfigRemoteLocation: String? = null,
    @Json(name = "sectionsFeaturedConfigRemoteLocation") val sectionsFeaturedConfigRemoteLocation: String? = null,
    @Json(name = "sectionsAZConfigRemoteLocation") val sectionsAZConfigRemoteLocation: String? = null,
    @Json(name = "sectionsBarTestConfigRemoteLocation") val sectionsBarTestConfigRemoteLocation: String? = null,
    @Json(name = "sectionsUnlistedConfigRemoteLocation") val sectionsUnlistedConfigRemoteLocation: String? = null,
    @Json(name = "sectionsRecommendedConfigRemoteLocation") val sectionsRecommendedConfigRemoteLocation: String? = null,
) {
    fun mapToDomain(): SiteServiceConfigStub {
        return SiteServiceConfigStub(
            sectionsBarConfigRemoteLocation = sectionsBarConfigRemoteLocation.orEmpty(),
            sectionsFeaturedConfigRemoteLocation = sectionsFeaturedConfigRemoteLocation.orEmpty(),
            sectionsAZConfigRemoteLocation = sectionsAZConfigRemoteLocation.orEmpty(),
            sectionsBarTestConfigRemoteLocation = sectionsBarTestConfigRemoteLocation.orEmpty(),
            sectionsUnlistedConfigRemoteLocation = sectionsUnlistedConfigRemoteLocation.orEmpty(),
            sectionsRecommendedConfigRemoteLocation = sectionsRecommendedConfigRemoteLocation.orEmpty(),
        )
    }
}