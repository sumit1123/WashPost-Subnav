package com.wapo.flagship.features.newsletter.repo.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.wapo.flagship.features.newsletter.domain.models.NewslettersKey

data class NewslettersRequestBody(
    @Json(name = "newsletters")
    val newsletters: List<NewslettersKey>? = null,
    @Json(name = "profile")
    val profile: Profile? = null,
    @Json(name = "suppressConfirmation")
    val suppressConfirmation: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class Profile(
    @Json(name = "loginId")
    val loginId: String? = null,
    @Json(name = "initiative")
    val initiative: String? = null,
    @Json(name = "method")
    val method: String? = null,
    @Json(name = "location")
    val location: String? = null,
    @Json(name = "username")
    val username: String? = null,
)
