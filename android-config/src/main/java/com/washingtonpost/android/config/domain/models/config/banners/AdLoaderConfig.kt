package com.washingtonpost.android.config.domain.models.config.banners

sealed interface AdSdk { val name: String }
enum class AdBidSource: AdSdk { GAM, APS, ADMOB, NIMBUS }
enum class AdLoaderType: AdSdk { GOOGLE, NIMBUS }

data class AdLoaderConfig(
    val loaderType: AdLoaderType,
    val bidSources: List<AdBidSource>,
)