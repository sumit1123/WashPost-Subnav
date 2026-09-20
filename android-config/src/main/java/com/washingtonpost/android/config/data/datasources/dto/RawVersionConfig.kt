package com.washingtonpost.android.config.data.datasources.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RawVersionConfig(
    @Json(name = "com.washingtonpost.android") val app: RawVersionConfigApp?,
)

@JsonClass(generateAdapter = true)
data class RawVersionConfigApp(
    @Json(name = "VersionService") val versionService: RawVersionService?,
)

@JsonClass(generateAdapter = true)
data class RawVersionService(
    @Json(name = "ads_disabled") val adsDisabled: Boolean?,
    @Json(name = "app_upgrade_url") val appUpgradeUrl: String?,
    @Json(name = "feed_config_build_number") val feedConfigNumber: Int?,
    @Json(name = "force_update") val forceUpdate: Boolean?,
    @Json(name = "max_available_version") val maxVersion: String?,
    @Json(name = "max_available_version_tag") val maxVersionTag: String?,
    @Json(name = "min_supported_version") val minSupportedVersion: String,
    @Json(name = "min_supported_version_tag") val minSupportedVersionTag: String,
    @Json(name = "upgrade_prompt_disabled") val promptDisabled: Boolean?,
)
