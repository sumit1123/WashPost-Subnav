package com.wapo.flagship.features.audio.models

import com.google.gson.annotations.SerializedName

data class Voice(
    @SerializedName("adsUrl")
    val adsUrl: String?,
    @SerializedName("id")
    val id: String?,
    @SerializedName("label")
    val label: String?,
    @SerializedName("marksUrl")
    val marksUrl: String?,
    @SerializedName("rawUrl")
    val rawUrl: String?,
    @SerializedName("s3Key")
    val s3Key: String?,
    @SerializedName("duration")
    val duration: Double?
)