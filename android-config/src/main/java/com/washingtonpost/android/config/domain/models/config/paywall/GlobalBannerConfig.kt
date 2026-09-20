package com.washingtonpost.android.config.domain.models.config.paywall

data class GlobalBannerConfig(
    val banners: List<GlobalBanner>,
)

data class GlobalBanner(
    val id: String?,
    val productId: String?,
    val offerId: String?,
    val title: String?,
    val subtitle: String?,
    val url: String? = null,
    val productName: String? = null,
    val action: String? = null,
)