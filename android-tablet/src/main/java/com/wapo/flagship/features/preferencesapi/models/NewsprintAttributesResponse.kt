package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NewsprintAttributesResponse(
    @SerializedName("status")
    override val status: String? = null,
    @SerializedName("state")
    override val state: String? = null,
    @SerializedName("preferenceValues")
    override val preferenceValues: List<NewsprintAttributesPreferenceValueItem?>? = null,
) : PreferencesApiGetResponse

@JsonClass(generateAdapter = true)
data class NewsprintAttributesPreferenceValueItem(
    @SerializedName("id")
    override val id: Id? = null,
    @SerializedName("value")
    override val value: NewsprintAttributesValueItem? = null,
    @SerializedName("lastUpdated")
    override val lastUpdated: Long? = null,
    @SerializedName("dateCreated")
    override val dateCreated: Long? = null,
) : PreferencesApiGetValuesItem

@JsonClass(generateAdapter = true)
data class NewsprintAttributesValueItem(
    @SerializedName("period")
    @Json(name = "period")
    val period: String? = null,
    @SerializedName("body")
    @Json(name = "body")
    val body: NewsprintAttributesValueBody? = null,
)

@JsonClass(generateAdapter = true)
data class NewsprintAttributesValueBody(
    @SerializedName("engaged_status")
    @Json(name = "engaged_status")
    val engagedStatus: String? = null,
    @SerializedName("reader_type")
    @Json(name = "reader_type")
    val readerType: String? = null,
)
