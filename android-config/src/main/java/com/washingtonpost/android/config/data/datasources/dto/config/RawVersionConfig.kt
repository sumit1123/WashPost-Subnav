package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.StoreType
import com.washingtonpost.android.config.domain.models.config.VersionConfig
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams

@JsonClass(generateAdapter = true)
data class RawVersionConfig(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "amazon") val amazon: List<Int>? = null,
    @Json(name = "playStore") val playStore: List<Int>? = null,
    @Json(name = "amazonForceUpdate") val amazonForceUpdate: Boolean? = null,
    @Json(name = "playStoreForceUpdate") val playStoreForceUpdate: Boolean? = null,
    @Json(name = "minSdk") val minSdk: Int? = null,
    @Json(name = "minSdkMessage") val minSdkMessage: String? = null,
) {
    fun mapToDomain(params: MapConfigParams): VersionConfig {
        return when (params.configProvider.storeType) {
            StoreType.AMAZON -> VersionConfig(
                enabled = enabled ?: true,
                versionCodesToUpdate = amazon.orEmpty(),
                forceUpdate = amazonForceUpdate ?: false,
                minSdk = minSdk ?: -1,
                minSdkMessage = minSdkMessage ?: OS_UPGRADE_DEFAULT_MESSAGE,
            )

            else -> VersionConfig(
                enabled = enabled ?: true,
                versionCodesToUpdate = playStore.orEmpty(),
                forceUpdate = playStoreForceUpdate ?: false,
                minSdk = minSdk ?: -1,
                minSdkMessage = minSdkMessage ?: OS_UPGRADE_DEFAULT_MESSAGE,
            )
        }
    }

    companion object {
        private const val OS_UPGRADE_DEFAULT_MESSAGE = "Your Android Version is no longer supported"
    }
}