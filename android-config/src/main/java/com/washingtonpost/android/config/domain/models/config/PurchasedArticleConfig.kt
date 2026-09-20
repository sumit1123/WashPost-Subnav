package com.washingtonpost.android.config.domain.models.config

import com.squareup.moshi.Json

data class PurchasedArticleConfig(
    var baseUrl: String,
    var metadataServiceBaseUrl: String
)
