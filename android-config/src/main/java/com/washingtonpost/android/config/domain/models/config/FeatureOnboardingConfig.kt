package com.washingtonpost.android.config.domain.models.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.data.datasources.dto.config.RawImageConfig

/**
 * Data class that holds the information for a feature onboarding flow from config
 */
@JsonClass(generateAdapter = true)
data class FeatureOnboardingConfig(
    val id: String?,
    val screens: List<FeatureOnboardingScreenConfig>?
)

/**
 * Data class that holds the information for a specific feature onboarding screen
 */
@JsonClass(generateAdapter = true)
data class FeatureOnboardingScreenConfig(
    val id: String?,
    val image: ImageConfig?,
    val title: String?,
    val text: String?,
)