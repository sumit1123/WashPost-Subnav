package com.washingtonpost.android.config.data.datasources.dto.config.paywall

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywall.CookieConfig

@JsonClass(generateAdapter = true)
data class RawCookieConfig(
    @Json(name = "sameSiteEnabled") val sameSiteEnabled: Boolean? = null,
) {
    fun mapToDomain(): CookieConfig {
        return CookieConfig(
            sameSiteEnabled = sameSiteEnabled ?: false,
        )
    }
}