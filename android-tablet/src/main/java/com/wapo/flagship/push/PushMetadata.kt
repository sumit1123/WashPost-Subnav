package com.wapo.flagship.push

data class PushMetadata(
    val headline: String?,
    val kicker: String?,
    val trackingId: String?,
    val analyticsId: String?,
    val pushTimestamp: String?,
)
