package com.wapo.flagship.features.preferencesapi.models

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass

/**
 * @param exp expiration date (millisecond timestamp)
 * @param uxp ultimate expiration date: [exp] + (TetroResponse's dismissLifespanSeconds * 1000)
 * @param mht max hide times
 */
@JsonClass(generateAdapter = true)
data class SnoozeInfo(
    @SerializedName("exp")
    var exp: Long?,
    @SerializedName("uxp")
    var uxp: Long?,
    @SerializedName("mht")
    var mht: Int?
)
