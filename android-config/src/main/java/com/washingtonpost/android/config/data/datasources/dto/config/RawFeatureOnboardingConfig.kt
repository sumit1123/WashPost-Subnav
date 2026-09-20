package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.FeatureOnboardingConfig
import com.washingtonpost.android.config.domain.models.config.FeatureOnboardingScreenConfig

/**
 * Data class that holds the information for a feature onboarding flow from config
 */
@JsonClass(generateAdapter = true)
data class RawFeatureOnboardingConfig(
    @Json(name = "id") val id: String? = null,
    @Json(name = "screens") val screens: List<RawFeatureOnboardingScreenConfig>? = null
) {
    fun mapToDomain(): FeatureOnboardingConfig {
        return FeatureOnboardingConfig(
            id = id,
            screens = screens?.map { it.mapToDomain() },
        )
    }
}

/**
 * Data class that holds the information for a specific feature onboarding screen
 */
@JsonClass(generateAdapter = true)
data class RawFeatureOnboardingScreenConfig(
    @Json(name = "id") val id: String? = null,
    @Json(name = "image") val image: RawImageConfig? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "text") val text: String? = null,
) {
    fun mapToDomain(): FeatureOnboardingScreenConfig {
        return FeatureOnboardingScreenConfig(
            id = id,
            image = image?.mapToDomain(),
            title = title,
            text = text,
        )
    }
}


