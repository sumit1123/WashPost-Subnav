package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NewsprintStateResponse(
    @SerializedName("status")
    override val status: String? = null,
    @SerializedName("state")
    override val state: String? = null,
    @SerializedName("preferenceValues")
    override val preferenceValues: List<NewsprintStatePreferenceValueItem?>? = null,
) : PreferencesApiGetResponse

@JsonClass(generateAdapter = true)
data class NewsprintStatePreferenceValueItem(
    @SerializedName("id")
    override val id: Id? = null,
    @SerializedName("value")
    override val value: NewsprintStateValueItem? = null,
    @SerializedName("lastUpdated")
    override val lastUpdated: Long? = null,
    @SerializedName("dateCreated")
    override val dateCreated: Long? = null,
) : PreferencesApiGetValuesItem

@JsonClass(generateAdapter = true)
data class NewsprintStateValueItem(
    @SerializedName("period")
    @Json(name = "period")
    val period: String? = null,
    @SerializedName("body")
    @Json(name = "body")
    val body: NewsprintStateValueBody? = null,
)

@JsonClass(generateAdapter = true)
data class NewsprintStateValueBody(
    @SerializedName("has_viewed_newsprint")
    @Json(name = "has_viewed_newsprint")
    val hasViewedNewsprint: Boolean? = null,
    @SerializedName("reader_type")
    @Json(name = "reader_type")
    val readerType: String? = null,
)
