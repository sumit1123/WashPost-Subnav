package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.android.commons.util.Logger
import com.wapo.android.commons.util.Utils
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams
import com.washingtonpost.android.config.domain.models.AudioAdConfig
import com.washingtonpost.android.config.domain.models.AudioAdsConfig
import com.washingtonpost.android.config.domain.models.AudioContentType
import com.washingtonpost.android.config.utils.MapUtils.merge

@JsonClass(generateAdapter = true)
data class RawAudioAdsConfig(
    @Json(name = "defaults") val defaults: RawAudioAdConfig? = null,
    @Json(name = "configsByContentType") val configsByContentType: Map<String, RawAudioAdConfig>? = null,
) {
    fun mapToDomain(params: MapConfigParams): AudioAdsConfig {
        val defaults = defaults ?: RawAudioAdConfig()
        return AudioAdsConfig(
            configsByContentType = buildMap {
                AudioContentType.entries.forEach { contentType ->
                    val config = configsByContentType?.get(contentType.name.lowercase())
                    val finalConfig = config?.let {
                        defaults.merge(it).mapToDomain(params, contentType)
                    }
                    finalConfig?.let {
                        put(contentType, it)
                    }
                }
            },
        ).also {
            Logger.d(TAG, it.toString())
        }
    }

    companion object {
        private const val TAG = "RawAdsAudioConfig"
    }
}

@JsonClass(generateAdapter = true)
data class RawAudioAdConfig(
    @Json(name = "vastEnabled") val vastEnabled: Boolean? = null,
    @Json(name = "minAdBreakIntervalSeconds") val minAdBreakIntervalSeconds: Int? = null,
    @Json(name = "minGlobalAdIntervalSeconds") val minGlobalAdIntervalSeconds: Int? = null,
    @Json(name = "timeRequiredToSkipAdInSeconds") val timeRequiredToSkipAdInSeconds: Int? = null,
    @Json(name = "vastTemplate") val vastTemplate: String? = null,
    @Json(name = "adBreaks") val adBreaks: List<RawAdBreak>? = null,
    @Json(name = "macros") val macros: RawAdMacros? = null,
) {
    fun mapToDomain(params: MapConfigParams, contentType: AudioContentType): AudioAdConfig {
        return AudioAdConfig(
            vastEnabled = vastEnabled ?: false,
            minAdBreakIntervalSeconds = minAdBreakIntervalSeconds,
            minGlobalAdIntervalSeconds = minGlobalAdIntervalSeconds,
            timeRequiredToSkipAdInSeconds = timeRequiredToSkipAdInSeconds ?: 5,
            vastTemplate = vastTemplate.orEmpty(),
            adBreaks = adBreaks?.mapNotNull { it.mapToDomain() }
                ?: when (contentType) {
                    AudioContentType.PODCAST -> listOf(
                        AudioAdConfig.AdBreak("preroll", 1, null),
                        AudioAdConfig.AdBreak("midroll", 2, null),
                        AudioAdConfig.AdBreak("postroll", 2, null)
                    )

                    AudioContentType.ARTICLE -> listOf(
                        AudioAdConfig.AdBreak("preroll", 1, null)
                    )
                },
            macros = (macros ?: RawAdMacros()).mapToDomain(params, contentType),
        )
    }

    @JsonClass(generateAdapter = true)
    data class RawAdBreak(
        @Json(name = "type") val type: String? = null,
        @Json(name = "maxAds") val maxAds: Int? = null,
        @Json(name = "timeSeconds") val timeSeconds: Int? = null,
    ) {
        fun mapToDomain(): AudioAdConfig.AdBreak? {
            type ?: return null
            return AudioAdConfig.AdBreak(
                type = type,
                maxAds = maxAds ?: 1,
                timeSeconds = timeSeconds,
            )
        }
    }

    @JsonClass(generateAdapter = true)
    data class RawAdMacros(
        @Json(name = "siteUrl") val siteUrl: String? = null,
        @Json(name = "playStoreUrl") val playStoreUrl: String? = null,
        @Json(name = "amazonStoreAppId") val amazonStoreAppId: String? = null,
        @Json(name = "amazonStoreUrl") val amazonStoreUrl: String? = null,
        @Json(name = "primarySectionId") val primarySectionId: String? = null,
        @Json(name = "contentLanguage") val contentLanguage: String? = null,
        @Json(name = "tritonExtStid") val tritonExtStid: String? = null,
        @Json(name = "tritonFeedType") val tritonFeedType: String? = null,
        @Json(name = "tritonDeliveryMethod") val tritonDeliveryMethod: String? = null,
    ) {
        fun mapToDomain(
            params: MapConfigParams,
            contentType: AudioContentType
        ): AudioAdConfig.AdMacros {
            return AudioAdConfig.AdMacros(
                siteUrl = siteUrl ?: "https://www.washingtonpost.com",
                storeUrl = when {
                    Utils.isAmazonBuild() -> amazonStoreUrl
                        ?: "https://www.amazon.com/Washington-Post-Company-The/dp/B00LI6CXZA"

                    else -> playStoreUrl
                        ?: "https://play.google.com/store/apps/details?id=com.washingtonpost.android"
                },
                storeId = when {
                    Utils.isAmazonBuild() -> amazonStoreAppId ?: "B00LI6CXZA"
                    else -> params.packageName
                },
                primarySectionId = primarySectionId,
                contentLanguage = contentLanguage ?: "en",
                tritonExtStid = tritonExtStid
                    ?: when (contentType) {
                        AudioContentType.ARTICLE -> "wp-1407653"
                        else -> null
                    },
                tritonFeedType = tritonFeedType
                    ?: when (contentType) {
                        AudioContentType.PODCAST -> "podcast"
                        else -> null
                    },
                tritonDeliveryMethod = tritonDeliveryMethod ?: "progressive",
            )
        }
    }
}
