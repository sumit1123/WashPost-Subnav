// Copyright (c) 2023 The Washington Post. All rights reserved.

package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContentPacksSetResponse(
    @SerializedName("preferenceValue")
    override val preferenceValue: ContentPacksSetValuesItem? = null,
    @SerializedName("state")
    override val state: String? = null,
    @SerializedName("status")
    override val status: String? = null,
) : PreferencesApiSetResponse

@JsonClass(generateAdapter = true)
data class ContentPacksSetValuesItem(
    @SerializedName("lastUpdated")
    override val lastUpdated: Long? = null,
    @SerializedName("dateCreated")
    override val dateCreated: Long? = null,
    @SerializedName("id")
    override val id: Id? = null,
    @SerializedName("value")
    override val value: List<ContentPacksValueItem?>? = null,
) : PreferencesApiSetValuesItem
