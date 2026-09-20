package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SetPersoPodConfigResponse(
    @SerializedName("status")
    override val status: String? = null,
    @SerializedName("state")
    override val state: String? = null,
    @SerializedName("preferenceValue")
    override val preferenceValue: PersoPodConfigSetValuesItem? = null,
) : PreferencesApiSetResponse

@JsonClass(generateAdapter = true)
data class PersoPodConfigSetValuesItem(
    @SerializedName("lastUpdated")
    override val lastUpdated: Long? = null,
    @SerializedName("dateCreated")
    override val dateCreated: Long? = null,
    @SerializedName("id")
    override val id: Id? = null,
    @SerializedName("value")
    override val value: PersoPodConfigValueItem? = null

) : PreferencesApiSetValuesItem

@JsonClass(generateAdapter = true)
data class PersoPodConfigValueItem(
    @SerializedName("topics")
    val topics: List<String>? = null,
    @SerializedName("voice_group")
    val voiceGroup: String? = null,
    @SerializedName("length")
    val length: String? = null
)
