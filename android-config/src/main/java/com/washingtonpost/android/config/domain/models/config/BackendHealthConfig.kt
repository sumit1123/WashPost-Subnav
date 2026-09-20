package com.washingtonpost.android.config.domain.models.config

data class BackendHealthConfig(
    val backendHealthMonitorURL: String,
    val fallbackURL: String,
    val fallbackStaticURL: String,
)