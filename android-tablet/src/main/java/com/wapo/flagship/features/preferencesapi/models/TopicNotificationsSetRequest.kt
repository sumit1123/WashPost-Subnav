// Copyright (c) 2023 The Washington Post. All rights reserved.

package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TopicNotificationsSetRequest(
    @SerializedName("value")
    @Json(name = "value")
    override val value: List<String?>? = null,
) : PreferencesApiSetRequest
