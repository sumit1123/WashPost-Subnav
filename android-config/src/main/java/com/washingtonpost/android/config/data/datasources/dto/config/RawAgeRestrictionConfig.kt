/*
 * Copyright (C) 2026 . The Washington Post. All rights reserved.
 */
package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.AgeRestrictionConfig

@JsonClass(generateAdapter = true)
data class RawAgeRestrictionConfig (
    @Json(name = "androidEnabled") val androidEnabled: Boolean? = null,
    @Json(name = "amazonEnabled") val amazonEnabled: Boolean? = null,
    @Json(name = "minAmazonOsVersion") val minAmazonOsVersion: Int? = null,
    @Json(name = "chromebookEnabled") val chromebookEnabled: Boolean? = null,
) {
    fun mapToDomain(): AgeRestrictionConfig {
        return AgeRestrictionConfig(
            androidEnabled = androidEnabled ?: false,
            amazonEnabled = amazonEnabled ?: false,
            minAmazonOsVersion = minAmazonOsVersion,
            chromebookEnabled = chromebookEnabled ?: false
        )
    }
}
