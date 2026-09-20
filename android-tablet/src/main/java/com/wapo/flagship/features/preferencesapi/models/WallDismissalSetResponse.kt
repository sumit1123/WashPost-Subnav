package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WallDismissalSetResponse(
    @SerializedName("preferenceValue")
    override val preferenceValue: WallDismissalSetResponseValuesItem?,
    @SerializedName("state")
    override val state: String?,
    @SerializedName("status")
    override val status: String?
) : PreferencesApiSetResponse

@JsonClass(generateAdapter = true)
data class WallDismissalSetResponseValuesItem(
    @SerializedName("lastUpdated")
    override val lastUpdated: Long? = null,
    @SerializedName("dateCreated")
    override val dateCreated: Long? = null,
    @SerializedName("id")
    override val id: Id? = null,
    @SerializedName("value")
    override val value: Map<String, Map<String, SnoozeInfo?>?>? = null,
) : PreferencesApiSetValuesItem
