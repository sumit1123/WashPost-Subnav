package com.wapo.flagship.features.articles2.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Targeting(
    @Json(name = "adcall")
    val adCall: Any? = null,
    @Json(name = "permutive")
    val permutive: Any? = null,
    @Json(name = "permutive_dict")
    val permutiveDict: Any? = null,
)
