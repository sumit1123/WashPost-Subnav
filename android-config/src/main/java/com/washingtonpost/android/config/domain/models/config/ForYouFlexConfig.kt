package com.washingtonpost.android.config.domain.models.config

data class ForYouFlexConfig(
    val url: String,
    val ttl: Long,
    val checkReadList: Boolean,
    val maxSize: Int,
)
