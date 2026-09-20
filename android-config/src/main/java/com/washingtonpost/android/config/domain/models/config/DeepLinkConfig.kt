package com.washingtonpost.android.config.domain.models.config

data class DeepLinkConfig(
    val externalExcludeList: List<String>,
    val internalExcludeList: List<String>,
    val externalPurchaseUrlList: List<String>,
    val paramExcludeList: List<String>,
    val simpleWebViewList: List<String>,
    val allowedHosts: List<String>,
    val validSourceAppValues: List<String>,
    val destinations: List<DeepLinkDestination>,
    val ungiftedURLs: List<String>,
)

data class DeepLinkDestination(
    val scheme: String,
    val domain: String,
    val path: String,
    val destination: String,
)