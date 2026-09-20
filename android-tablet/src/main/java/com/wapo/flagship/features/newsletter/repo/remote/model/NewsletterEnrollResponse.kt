package com.wapo.flagship.features.newsletter.repo.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NewsletterEnrollResponse(
    @Json(name = "success")
    val success: Boolean? = null,
)
