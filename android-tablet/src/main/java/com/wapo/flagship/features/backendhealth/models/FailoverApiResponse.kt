package com.wapo.flagship.features.backendhealth.models

import com.google.gson.annotations.SerializedName

data class FailoverApiResponse(
    @SerializedName("api_status") val apiStatus: String? = null,
    @SerializedName("endpoint") val endpoint: String? = null,
)