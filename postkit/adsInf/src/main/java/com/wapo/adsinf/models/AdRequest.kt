package com.wapo.adsinf.models


import com.adsbynimbus.request.NimbusRequest
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderConfig
import java.util.UUID

sealed class AdRequest(
    val id: String = UUID.randomUUID().toString(),
    open val adConfig: AdConfig,
    open val adLoaderConfig: AdLoaderConfig,
) {
    data class Google(
        val request: AdManagerAdRequest,
        override val adConfig: AdConfig,
        override val adLoaderConfig: AdLoaderConfig,
    ) : AdRequest(adConfig = adConfig, adLoaderConfig = adLoaderConfig)

    data class Nimbus(
        val request: NimbusRequest,
        override val adConfig: AdConfig,
        override val adLoaderConfig: AdLoaderConfig,
    ) : AdRequest(adConfig = adConfig, adLoaderConfig = adLoaderConfig)
}
