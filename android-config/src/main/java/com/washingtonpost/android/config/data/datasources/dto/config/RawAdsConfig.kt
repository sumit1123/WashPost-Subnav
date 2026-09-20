package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams
import com.washingtonpost.android.config.domain.models.config.AdConfig
import com.washingtonpost.android.config.domain.models.config.AdPositionConfig
import com.washingtonpost.android.config.domain.models.config.AdSectionConfig
import com.washingtonpost.android.config.domain.models.config.AdsConfig
import com.washingtonpost.android.config.domain.models.config.ContextualTargeting
import com.washingtonpost.android.config.domain.models.config.ContextualTargetingContent
import kotlin.collections.orEmpty

@JsonClass(generateAdapter = true)
data class RawAdsConfig(
    @Json(name = "nonSubscriberAdConfig") val nonSubscriberAdConfig: RawAdConfig? = null,
    @Json(name = "subscriberAdConfig") val subscriberAdConfig: RawAdConfig? = null,
    @Json(name = "overrides") val overrides: RawAdConfig? = null,
    @Json(name = "firstAdSlot") val firstAdSlot: RawAdConfig? = null,
    @Json(name = "search") val search: RawAdSearchConfig? = null,
    @Json(name = "comics") val comics: RawAdComicConfig? = null,
    @Json(name = "contextualTargeting") val contextualTargeting: RawContextualTargeting? = null,
    @Json(name = "audio") val audio: RawAudioAdsConfig? = null,
    @Json(name = "bannersConfig") val bannersConfig: RawAdBannersConfig? = null,
) {
    fun mapToDomain(params: MapConfigParams): AdsConfig {
        return AdsConfig(
            nonSubscriberAdConfig = (nonSubscriberAdConfig ?: RawAdConfig()).mapToDomain(),
            subscriberAdConfig = (subscriberAdConfig ?: RawAdConfig()).mapToDomain(),
            overrides = (overrides ?: RawAdConfig()).mapToDomain(),
            firstAdSlot = (firstAdSlot ?: RawAdConfig()).mapToDomain(),
            search = (search ?: RawAdSearchConfig()).mapToDomain(),
            comics = (comics ?: RawAdComicConfig()).mapToDomain(),
            audio = (audio ?: RawAudioAdsConfig()).mapToDomain(params),
            contextualTargeting = (contextualTargeting ?: RawContextualTargeting()).mapToDomain(),
            bannersConfig = (bannersConfig ?: RawAdBannersConfig()).mapToDomain(params),
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawAdSearchConfig(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "commercialNode") val commercialNode: String? = null,
    @Json(name = "firstItemDelay") val firstItemDelay: Int? = null,
    @Json(name = "adItemInterval") val adItemInterval: Int? = null,
    @Json(name = "minimumResults") val minimumResults: Int? = null,
) {
    fun mapToDomain(): AdSectionConfig {
        return AdSectionConfig(
            enabled = enabled,
            commercialNode = commercialNode,
            firstItemDelay = firstItemDelay,
            adItemInterval = adItemInterval,
            minimumResults = minimumResults,
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawAdComicConfig(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "commercialNode") val commercialNode: String? = null,
    @Json(name = "firstItemDelay") val firstItemDelay: Int? = null,
    @Json(name = "adItemInterval") val adItemInterval: Int? = null,
    @Json(name = "minimumResults") val minimumResults: Int? = null,
) {
    fun mapToDomain(): AdSectionConfig {
        return AdSectionConfig(
            enabled = enabled,
            commercialNode = commercialNode,
            firstItemDelay = firstItemDelay,
            adItemInterval = adItemInterval,
            minimumResults = minimumResults,
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawAdConfig(
    @Json(name = "defaultConfig") val defaultConfig: Map<RawAdPositionConfig, Any>? = null,
    @Json(name = "positionConfig") val positionConfig: Map<Int, Map<RawAdPositionConfig, Any>>? = null,
) {
    fun mapToDomain(): AdConfig {
        return AdConfig(
            defaultConfig = defaultConfig?.mapKeys { (it.key).mapToDomain() }.orEmpty(),
            positionConfig = positionConfig
                ?.mapValues { it.value.mapKeys { it.key.mapToDomain() } }
                .orEmpty(),
        )
    }
}

enum class RawAdPositionConfig {
    @Json(name = "minParagraphsBetweenAds")
    MIN_PARAGRAPHS_BETWEEN_ADS,

    @Json(name = "minCharactersBetweenAds")
    MIN_CHARACTERS_BETWEEN_ADS,

    @Json(name = "minCharactersBeforeAd")
    MIN_CHARACTERS_BEFORE_AD,

    @Json(name = "minCharactersAfterAd")
    MIN_CHARACTERS_AFTER_AD,

    @Json(name = "maxNumberOfAds")
    MAX_NUMBER_OF_ADS,

    @Json(name = "showBottomAd")
    SHOW_BOTTOM_AD,

    @Json(name = "path")
    SECTION_PATH,

    @Json(name = "overrideFirstAdSlot")
    OVERRIDE_FIRST_SLOT,

    @Json(name = "adWidth")
    AD_WIDTH,

    @Json(name = "adHeight")
    AD_HEIGHT,

    @Json(name = "enabled")
    ENABLE,

    @Json(name = "commercialNode")
    COMMERCIAL_NODE;

    fun mapToDomain(): AdPositionConfig {
        return when (this) {
            MIN_PARAGRAPHS_BETWEEN_ADS -> AdPositionConfig.MIN_PARAGRAPHS_BETWEEN_ADS
            MIN_CHARACTERS_BETWEEN_ADS -> AdPositionConfig.MIN_CHARACTERS_BETWEEN_ADS
            MIN_CHARACTERS_BEFORE_AD -> AdPositionConfig.MIN_CHARACTERS_BEFORE_AD
            MIN_CHARACTERS_AFTER_AD -> AdPositionConfig.MIN_CHARACTERS_AFTER_AD
            MAX_NUMBER_OF_ADS -> AdPositionConfig.MAX_NUMBER_OF_ADS
            SHOW_BOTTOM_AD -> AdPositionConfig.SHOW_BOTTOM_AD
            SECTION_PATH -> AdPositionConfig.SECTION_PATH
            OVERRIDE_FIRST_SLOT -> AdPositionConfig.OVERRIDE_FIRST_SLOT
            AD_WIDTH -> AdPositionConfig.AD_WIDTH
            AD_HEIGHT -> AdPositionConfig.AD_HEIGHT
            ENABLE -> AdPositionConfig.ENABLE
            COMMERCIAL_NODE -> AdPositionConfig.COMMERCIAL_NODE
        }
    }
}

@JsonClass(generateAdapter = true)
data class RawContextualTargeting(
    @Json(name = "content") val content: RawContextualTargetingContent? = null,
) {
    fun mapToDomain(): ContextualTargeting {
        return ContextualTargeting(
            content = (content ?: RawContextualTargetingContent()).mapToDomain()
        )
    }
}

@JsonClass(generateAdapter = true)
data class RawContextualTargetingContent(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "timeout") val timeout: Long? = null,
) {
    fun mapToDomain(): ContextualTargetingContent {
        return ContextualTargetingContent(
            enabled = enabled ?: true,
            url = url ?: "https://ads-metadata-prod.zeustechnology.com/api/v2/content/collection",
            timeout = timeout ?: 1000,
        )
    }
}
