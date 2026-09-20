package com.wapo.flagship.features.newsletter.repo.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NewsletterEnrollmentsResponse(
    @Json(name = "newsletters")
    val newsletters: List<String?>? = null,
)
