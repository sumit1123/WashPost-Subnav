// Copyright (c) 2023 The Washington Post. All rights reserved.

package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TopicNotificationsSetResponse(
    @SerializedName("preferenceValue")
    override val preferenceValue: TopicNotificationsSetValuesItem? = null,
    @SerializedName("state")
    override val state: String? = null,
    @SerializedName("status")
    override val status: String? = null,
) : PreferencesApiSetResponse

@JsonClass(generateAdapter = true)
data class TopicNotificationsSetValuesItem(
    @SerializedName("lastUpdated")
    override val lastUpdated: Long? = null,
    @SerializedName("dateCreated")
    override val dateCreated: Long? = null,
    @SerializedName("id")
    override val id: Id? = null,
    @SerializedName("value")
    override val value: List<String?>? = null,
) : PreferencesApiSetValuesItem
