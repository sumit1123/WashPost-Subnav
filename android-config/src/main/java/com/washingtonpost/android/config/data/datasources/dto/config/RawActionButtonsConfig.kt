package com.washingtonpost.android.config.data.datasources.dto.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.ActionButtonsConfig

@JsonClass(generateAdapter = true)
data class RawActionButtonsConfig(
    @Json(name = "saveEnabled") val saveEnabled: Boolean? = null,
) {
    fun mapToDomain(): ActionButtonsConfig {
        return ActionButtonsConfig(
            saveEnabled = saveEnabled ?: true,
        )
    }
}