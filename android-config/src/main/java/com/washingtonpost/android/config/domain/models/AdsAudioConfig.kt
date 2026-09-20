package com.washingtonpost.android.config.domain.models

data class AudioAdsConfig(
    val configsByContentType: Map<AudioContentType, AudioAdConfig>,
)

data class AudioAdConfig(
    val vastEnabled: Boolean,
    val minAdBreakIntervalSeconds: Int?,
    val minGlobalAdIntervalSeconds: Int?,
    val timeRequiredToSkipAdInSeconds: Int?,
    val vastTemplate: String,
    val adBreaks: List<AdBreak>?,
    val macros: AdMacros,
) {
    data class AdBreak(
        val type: String,
        val maxAds: Int,
        val timeSeconds: Int?,
    )

    data class AdMacros(
        val siteUrl: String,
        val storeUrl: String,
        val storeId: String,
        val primarySectionId: String?,
        val contentLanguage: String,
        val tritonExtStid: String?,
        val tritonFeedType: String?,
        val tritonDeliveryMethod: String?,
    )
}

enum class AudioContentType {
    ARTICLE, PODCAST
}