package com.washingtonpost.android.config.data.datasources.dto.config.paywall

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywall.SubscriberDataModel

@JsonClass(generateAdapter = true)
data class RawSubscriberDataModel(
    @Json(name = "message") val message: String? = null,
    @Json(name = "heading") val heading: String? = null,
) {
    fun mapToDomain(): SubscriberDataModel {
        return SubscriberDataModel(
            message = message.orEmpty(),
            heading = heading.orEmpty(),
        )
    }
}