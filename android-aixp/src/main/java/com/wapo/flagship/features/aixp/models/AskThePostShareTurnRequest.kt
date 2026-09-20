package com.wapo.flagship.features.aixp.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class AskThePostShareTurnRequest(
    @Json(name = "turn_id")
    val turnId: String? = null
)
