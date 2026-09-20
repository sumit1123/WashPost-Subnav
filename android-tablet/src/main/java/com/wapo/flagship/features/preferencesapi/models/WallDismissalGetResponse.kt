package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WallDismissalGetResponse(
    @SerializedName("preferenceValues")
    override val preferenceValues: List<WallDismissalGetResponseValuesItem?>? = null,
    @SerializedName("state")
    override val state: String? = null,
    @SerializedName("status")
    override val status: String? = null
) : PreferencesApiGetResponse

@JsonClass(generateAdapter = true)
data class WallDismissalGetResponseValuesItem(
    @SerializedName("lastUpdated")
    override val lastUpdated: Long? = null,
    @SerializedName("dateCreated")
    override val dateCreated: Long? = null,
    @SerializedName("id")
    override val id: Id? = null,
    @SerializedName("value")
    override val value: Map<String, Map<String, SnoozeInfo?>?>? = null,
) : PreferencesApiGetValuesItem
