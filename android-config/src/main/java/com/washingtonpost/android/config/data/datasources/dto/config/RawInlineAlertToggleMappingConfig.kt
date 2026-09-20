package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.InlineAlertToggleMappingConfig

/**
 * Data class that hold the information for an inline alert toggle segment from the config
 * @param alertSegmentKey the topic key for the segment
 * @param includedSourceSections list of article sourcesections that should show this segment toggle
 * @param excludedSourceSubsections list of article sourcesubsections that should be excluded from being shown
 */
@JsonClass(generateAdapter = true)
data class RawInlineAlertToggleMappingConfig(
    @Json(name = "alertSegmentKey") val alertSegmentKey: String? = null,
    @Json(name = "includedSourceSections") val includedSourceSections: List<String>? = null,
    @Json(name = "excludedSourceSubsections") val excludedSourceSubsections: List<String>? = null,
) {
    fun mapToDomain(): InlineAlertToggleMappingConfig {
        return InlineAlertToggleMappingConfig(
            alertSegmentKey = alertSegmentKey,
            includedSourceSections = includedSourceSections.orEmpty(),
            excludedSourceSubsections = excludedSourceSubsections.orEmpty(),
        )
    }
}