package com.washingtonpost.android.config.domain.models.config

import com.washingtonpost.android.config.domain.models.config.banners.AdBannersConfig

import com.washingtonpost.android.config.domain.models.AudioAdsConfig

data class AdsConfig(
    val nonSubscriberAdConfig: AdConfig,
    val subscriberAdConfig: AdConfig,
    val overrides: AdConfig,
    val firstAdSlot: AdConfig,
    val search: AdSectionConfig,
    val comics: AdSectionConfig,
    val audio: AudioAdsConfig,
    val contextualTargeting: ContextualTargeting,
    val bannersConfig: AdBannersConfig,
)

data class AdSectionConfig(
    val enabled: Boolean?,
    val commercialNode: String?,
    val firstItemDelay: Int?,
    val adItemInterval: Int?,
    val minimumResults: Int?,
)

data class AdConfig(
    val defaultConfig: Map<AdPositionConfig, Any>,
    val positionConfig: Map<Int, Map<AdPositionConfig, Any>>,
)

enum class AdPositionConfig {
    MIN_PARAGRAPHS_BETWEEN_ADS,
    MIN_CHARACTERS_BETWEEN_ADS,
    MIN_CHARACTERS_BEFORE_AD,
    MIN_CHARACTERS_AFTER_AD,
    MAX_NUMBER_OF_ADS,
    SHOW_BOTTOM_AD,
    SECTION_PATH,
    OVERRIDE_FIRST_SLOT,
    AD_WIDTH,
    AD_HEIGHT,
    ENABLE,
    COMMERCIAL_NODE;
}

data class ContextualTargeting(
    val content: ContextualTargetingContent,
)

data class ContextualTargetingContent(
    val enabled: Boolean,
    val url: String,
    val timeout: Long,
)
