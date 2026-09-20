package com.washingtonpost.android.config.domain.models.config

data class AudioConfig(
    val audioAdInterval: Long,
    val fullWidth: ImageServiceConfig,
    val thumbnail: ImageServiceConfig,
    val audioApiBaseUrl: String,
    val audioDisabledUrls: List<String>,
)