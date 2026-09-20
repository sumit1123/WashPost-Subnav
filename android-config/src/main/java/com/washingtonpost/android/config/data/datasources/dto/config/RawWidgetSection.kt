package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.WidgetSection

@JsonClass(generateAdapter = true)
data class RawWidgetSection(
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "bundleName") val bundleName: String? = null,
    @Json(name = "fusionBundleName") val fusionBundleName: String? = null,
) {
    fun mapToDomain(): WidgetSection {
        return WidgetSection(
            displayName = displayName.orEmpty(),
            bundleName = bundleName.orEmpty(),
            fusionBundleName = fusionBundleName,
        )
    }
}