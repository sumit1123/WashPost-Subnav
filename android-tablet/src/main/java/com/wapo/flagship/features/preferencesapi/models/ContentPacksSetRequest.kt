package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContentPacksSetRequest(
    @SerializedName("value")
    @Json(name = "value")
    override val value: List<ContentPacksValueItem?>? = null,
) : PreferencesApiSetRequest
