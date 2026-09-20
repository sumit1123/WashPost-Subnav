package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams
import com.washingtonpost.android.config.domain.models.config.banners.AdBannersConfig
import com.washingtonpost.android.config.domain.models.config.banners.AdBidSource
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderConfig
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderType

@JsonClass(generateAdapter = true)
data class RawAdBannersConfig(
    @Json(name = "autoRefreshIntervalInSecondsV2") val autoRefreshIntervalInSeconds: Int? = null,
    @Json(name = "defaultLoadConfig") val defaultLoadConfig: List<RawAdLoaderConfig>? = null,
    @Json(name = "nimbus") val nimbus: RawNimbusConfig? = null,
    @Json(name = "dtb") val dtb: RawDtbConfig? = null,
) {

    fun mapToDomain(params: MapConfigParams): AdBannersConfig {
        return AdBannersConfig(
            autoRefreshIntervalInSeconds = autoRefreshIntervalInSeconds,
            defaultLoadConfig = defaultLoadConfig?.mapNotNull { it.mapToDomain() }.orEmpty(),
            nimbus = (nimbus ?: RawNimbusConfig()).mapToDomain(params),
            dtb = (dtb ?: RawDtbConfig()).mapToDomain(),
        )
    }

    @JsonClass(generateAdapter = true)
    data class RawAdLoaderConfig(
        @Json(name = "loader") val loader: String? = null,
        @Json(name = "bidSources") val bidSources: List<String>? = null,
    ) {
        fun mapToDomain(): AdLoaderConfig? {
            val loaderType = toLoaderType(loader.orEmpty()) ?: return null
            return AdLoaderConfig(
                loaderType = loaderType,
                bidSources = bidSources?.mapNotNull { toBidSource(it) }.orEmpty(),
            )
        }
    }

    @JsonClass(generateAdapter = true)
    data class RawNimbusConfig(
        @Json(name = "enabled") val enabled: Boolean? = null,
        @Json(name = "testMode") val testMode: Boolean? = null,
        @Json(name = "testPercent") val testPercent: Int? = null,
        @Json(name = "adMobUnitId") val adMobUnitId: String? = null,
        @Json(name = "loadConfig") val loadConfig: List<RawAdLoaderConfig>? = null,
        @Json(name = "refreshInterval") val refreshIntervalInSeconds: Int? = null,
    ) {
        fun mapToDomain(params: MapConfigParams): AdBannersConfig.NimbusConfig {
            return AdBannersConfig.NimbusConfig(
                enabled = enabled ?: false,
                testMode = testMode ?: false,
                testPercent = testPercent ?: 0,
                adMobUnitId = adMobUnitId.orEmpty(),
                loadConfig = loadConfig?.mapNotNull { it.mapToDomain() }.orEmpty(),
                refreshIntervalInSeconds = refreshIntervalInSeconds,
            )
        }
    }

    @JsonClass(generateAdapter = true)
    data class RawDtbConfig(
        @Json(name = "app") val app: String? = null,
        @Json(name = "slots") val slots: Map<String, String>? = null,
    ) {
        fun mapToDomain(): AdBannersConfig.DTBConfig {
            val slotsMap = mutableMapOf<AdDimension, String>()
            for ((key, value) in slots.orEmpty()) {
                val adDimension = toAdDimension(key)
                if (adDimension != null) slotsMap.put(adDimension, value)
            }
            if (slotsMap.isEmpty()) {
                slotsMap.put(AdDimension.Medium, "fa708271-4f1f-4abf-aef7-66e3d1a37612")
            }
            return AdBannersConfig.DTBConfig(
                app = app.orEmpty(),
                slots = slotsMap,
            )
        }
    }

    companion object {
        private fun toLoaderType(str: String): AdLoaderType? {
            return when (str.lowercase()) {
                "google" -> AdLoaderType.GOOGLE
                "nimbus" -> AdLoaderType.NIMBUS
                else -> null
            }
        }

        private fun toBidSource(str: String): AdBidSource? {
            return when (str.lowercase()) {
                "gam" -> AdBidSource.GAM
                "aps" -> AdBidSource.APS
                "admob" -> AdBidSource.ADMOB
                "nimbus" -> AdBidSource.NIMBUS
                else -> null
            }
        }

        private fun toAdDimension(str: String): AdDimension? {
            val dimensions = str.split("x").mapNotNull { it.toIntOrNull() }
            if (dimensions.size == 2) {
                return AdDimension.values()
                    .firstOrNull { it.w == dimensions[0] && it.h == dimensions[1] }
            }
            return null
        }
    }
}
