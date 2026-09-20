package com.wapo.flagship.features.audio.config2

data class AudioMediaAdConfig(
    val adSetUrl: String,
    val adBreaks: List<AudioMediaAdBreak>?,
    val primarySectionId: String?,
    val seriesName: String? = null,
    val contentLanguage: String? = null,
    val tritonExtStid: String? = null,
    val tritonFeedType: String? = null,
    val tritonDeliveryMethod: String? = null,
    val useVAST: Boolean? = null,
)

sealed interface AudioMediaAdBreak {
    val maxAds: Int

    data class Preroll(
        override val maxAds: Int = 1
    ): AudioMediaAdBreak

    data class Midroll(
        override val maxAds: Int = 2,
        val timeMs: Long
    ): AudioMediaAdBreak

    data class Postroll(
        override val maxAds: Int = 2
    ): AudioMediaAdBreak
}
