package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Interface that holds the general structure for responses from GET calls made to the preferences API.
 * Implement this for any new preference that we use the preferences API for.
 */
interface PreferencesApiGetResponse {
    val preferenceValues: List<PreferencesApiGetValuesItem?>?
    val state: String?
    val status: String?
}

/**
 * Interface that holds the general structure for the "preferenceValues" field in responses from GET calls made to the preferences API.
 * Implement this for any new preference that we use the preferences API for.
 * Make sure to use the correct data structure for [value] (implement one if needed).
 */
interface PreferencesApiGetValuesItem {
    val lastUpdated: Long?
    val dateCreated: Long?
    val id: Id?
    val value: Any?
}

@JsonClass(generateAdapter = true)
data class Id(
    @SerializedName("loginId")
    @Json(name = "loginId")
    val loginId: String? = null,
    @SerializedName("preference")
    @Json(name = "preference")
    val preference: Preference? = null,
)

@JsonClass(generateAdapter = true)
data class Type(
    @SerializedName("name")
    @Json(name = "name")
    val name: String? = null,
    @SerializedName("id")
    @Json(name = "id")
    val id: Int? = null,
)

@JsonClass(generateAdapter = true)
data class Preference(
    @SerializedName("lastUpdated")
    @Json(name = "lastUpdated")
    val lastUpdated: Long? = null,
    @SerializedName("dateCreated")
    @Json(name = "dateCreated")
    val dateCreated: Long? = null,
    @SerializedName("name")
    @Json(name = "name")
    val name: String? = null,
    @SerializedName("id")
    @Json(name = "id")
    val id: String? = null,
    @SerializedName("type")
    @Json(name = "type")
    val type: Type? = null,
)
