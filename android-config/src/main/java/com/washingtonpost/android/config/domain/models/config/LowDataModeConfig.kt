package com.washingtonpost.android.config.domain.models.config

/**
 * Data class that holds the information for a low data mode config
 */
data class LowDataModeConfig(
    val enable: Boolean,
    val liteUrlPath: String,
)