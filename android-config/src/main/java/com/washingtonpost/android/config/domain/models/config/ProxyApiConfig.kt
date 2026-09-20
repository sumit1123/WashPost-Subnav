package com.washingtonpost.android.config.domain.models.config

data class ProxyApiConfig(
    val enabled: Boolean,
    val apiHitCount: Int,
    val baseUrl: String
)