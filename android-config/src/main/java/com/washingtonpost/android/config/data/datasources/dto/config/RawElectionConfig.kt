package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ElectionConfig
import com.washingtonpost.android.config.domain.models.config.LinksItems

@JsonClass(generateAdapter = true)
data class RawElectionConfig(
    @Json(name = "baseUrl") val baseUrl: String? = null,
    @Json(name = "landingPage") val landingPage: String? = null,
    @Json(name = "links") val links: List<RawLinksItems>? = null,
) {
    fun mapToDomain(): ElectionConfig {
        return ElectionConfig(
            baseUrl = baseUrl.orEmpty(),
            landingPage = landingPage.orEmpty(),
            links = links?.map { it.mapToDomain() }.orEmpty(),
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawLinksItems(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "path") val path: String? = null,
) {
    fun mapToDomain(): LinksItems {
        return LinksItems(
            id = id.orEmpty(),
            name = name.orEmpty(),
            type = type.orEmpty(),
            path = path.orEmpty(),
        )
    }
}
