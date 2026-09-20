// Copyright (c) 2023 The Washington Post. All rights reserved.

package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContentPacksGetResponse(
    @SerializedName("status")
    override val status: String? = null,
    @SerializedName("state")
    override val state: String? = null,
    @SerializedName("preferenceValues")
    override val preferenceValues: List<ContentPacksGetValuesItem?>? = null,
) : PreferencesApiGetResponse

@JsonClass(generateAdapter = true)
data class ContentPacksGetValuesItem(
    @SerializedName("id")
    override val id: Id? = null,
    @SerializedName("value")
    override val value: List<ContentPacksValueItem?>? = null,
    @SerializedName("lastUpdated")
    override val lastUpdated: Long? = null,
    @SerializedName("dateCreated")
    override val dateCreated: Long? = null,
) : PreferencesApiGetValuesItem

@JsonClass(generateAdapter = true)
data class ContentPacksValueItem(
    @SerializedName("interested")
    @Json(name = "interested")
    val interested: Boolean? = null,
    @SerializedName("pack")
    @Json(name = "pack")
    val pack: String? = null,
    @SerializedName("referenceId")
    @Json(name = "referenceId")
    val referenceId: String? = null,
)
