package com.washingtonpost.android.config.data.datasources.dto.config.paywallconf

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywallconf.ProductSkuEntry

@JsonClass(generateAdapter = true)
data class RawProductSkuEntry(
    @Json(name = "playstore") val playstore: String? = null,
    @Json(name = "amazon") val amazon: String? = null,
    @Json(name = "basePlanId") val basePlanId: String? = null,
    @Json(name = "type") val type: String? = "renewable",
    @Json(name = "duration") val duration: Long? = null,
) {
    fun mapToDomain(): ProductSkuEntry {
        return ProductSkuEntry(
            playstore = playstore,
            amazon = amazon,
            basePlanId = basePlanId,
            type = type,
            duration = duration
        )
    }
}