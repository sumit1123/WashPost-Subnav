package com.washingtonpost.android.config.domain.models.config.banners

data class AdBannersConfig(
    val autoRefreshIntervalInSeconds: Int?,
    val defaultLoadConfig: List<AdLoaderConfig>,
    val nimbus: NimbusConfig,
    val dtb: DTBConfig,
) {
    data class NimbusConfig(
        val enabled: Boolean,
        val testMode: Boolean,
        val testPercent: Int,
        val adMobUnitId: String,
        val loadConfig: List<AdLoaderConfig>,
        val refreshIntervalInSeconds: Int?,
    )

    data class DTBConfig(
        val app: String,
        val slots: Map<AdDimension, String>,
    )
}