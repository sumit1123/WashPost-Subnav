package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckPrefResponse(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("lastUpdated")
    val lastUpdated: Long? = null
)
