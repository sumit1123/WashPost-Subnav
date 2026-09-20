package com.washingtonpost.android.config.data.datasources.dto.config.paywall

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.StoreType
import com.washingtonpost.android.config.domain.models.config.paywall.BottomCtaModel
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams

@JsonClass(generateAdapter = true)
data class RawBottomCtaModel(
    @Json(name = "sku") val productId: String? = null,
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "newSubMessage") val newSubMessage: String? = null,
    @Json(name = "terminatedSubMessage") val terminatedSubMessage: String? = null,
) {
    fun mapToDomain(params: MapConfigParams): BottomCtaModel {
        val defaultProductId = if (params.configProvider.storeType == StoreType.AMAZON) {
            "wp.unified.basic"
        } else {
            "wp.classic.basic"
        }
        return BottomCtaModel(
            productId = productId ?: defaultProductId,
            enabled = enabled ?: true,
            newSubMessage = newSubMessage ?: "Unlimited access to all our journalism",
            terminatedSubMessage = terminatedSubMessage ?: "Unlimited access to all our journalism",
        )
    }
}