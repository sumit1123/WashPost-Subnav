// Copyright (c) 2023 The Washington Post. All rights reserved.

package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TopicNotificationsGetResponse(
    @SerializedName("preferenceValues")
    override val preferenceValues: List<TopicNotificationsGetValuesItem?>? = null,
    @SerializedName("state")
    override val state: String?,
    @SerializedName("status")
    override val status: String?,
) : PreferencesApiGetResponse

@JsonClass(generateAdapter = true)
data class TopicNotificationsGetValuesItem(
    @SerializedName("lastUpdated")
    override val lastUpdated: Long?,
    @SerializedName("dateCreated")
    override val dateCreated: Long?,
    @SerializedName("id")
    override val id: Id?,
    @SerializedName("value")
    override val value: List<String>? = null,
) : PreferencesApiGetValuesItem
