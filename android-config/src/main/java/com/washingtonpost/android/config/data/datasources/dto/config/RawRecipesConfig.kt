package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.FiltersItem
import com.washingtonpost.android.config.domain.models.config.ItemsItem
import com.washingtonpost.android.config.domain.models.config.RecipesConfig

@JsonClass(generateAdapter = true)
data class RawRecipesConfig(
    @Json(name = "baseUrl") val baseUrl: String? = null,
    @Json(name = "landingPage") val landingPage: String? = null,
    @Json(name = "filters") val filters: List<RawFiltersItem>? = null,
) {
    fun mapToDomain(): RecipesConfig {
        return RecipesConfig(
            baseUrl = baseUrl.orEmpty(),
            landingPage = landingPage.orEmpty(),
            filters = filters?.map { it.mapToDomain() }.orEmpty(),
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawFiltersItem(
    @Json(name = "queryName") val queryName: String? = null,
    @Json(name = "items") val items: List<RawItemsItem>? = null,
    @Json(name = "group") val group: String? = null,
    @Json(name = "isMultiSelect") val isMultiSelect: Boolean? = null,
    @Json(name = "isQuickFilter") val isQuickFilter: Boolean? = null,
) {
    fun mapToDomain(): FiltersItem {
        return FiltersItem(
            queryName = queryName.orEmpty(),
            items = items?.map { it.mapToDomain() }.orEmpty(),
            group = group.orEmpty(),
            isMultiSelect = isMultiSelect ?: false,
            isQuickFilter = isQuickFilter ?: false,
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawItemsItem(
    @Json(name = "label") val label: String? = null,
    @Json(name = "queryId") val queryId: String? = null,
) {
    fun mapToDomain(): ItemsItem {
        return ItemsItem(
            label = label.orEmpty(),
            queryId = queryId,
        )
    }
}
