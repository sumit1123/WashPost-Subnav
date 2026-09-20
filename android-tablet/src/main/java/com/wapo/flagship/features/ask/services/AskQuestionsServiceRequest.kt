package com.wapo.flagship.features.ask.services

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AskQuestionsServiceRequest(
    @Json(name = "wapo_login_id")
    val loginId: String?,
    @Json(name = "j_ucid")
    val jucid: String?,
    @Json(name = "device_id")
    val deviceId: String?,
    @Json(name = "date_time")
    val dateTime: String?,
    @Json(name = "experiment_id")
    val experimentId: String?
)
