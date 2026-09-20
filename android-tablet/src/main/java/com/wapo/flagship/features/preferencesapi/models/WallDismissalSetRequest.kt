package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WallDismissalSetRequest(
    @SerializedName("value")
    override val value: Map<String, Map<String, SnoozeInfo?>?>?
) : PreferencesApiSetRequest
