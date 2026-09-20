package com.wapo.adsinf.interfaces

import com.wapo.adsinf.sdk.google.GoogleBannerAdRequestFactory
import com.wapo.adsinf.sdk.nimbus.NimbusBannerAdRequestFactory
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderType

fun interface AdRequestFactoryResolver {
    fun getAdBannerRequestFactory(type: AdLoaderType): AdRequestFactory
}

class DefaultAdRequestFactoryResolver : AdRequestFactoryResolver {
    val factories = mapOf(
        AdLoaderType.GOOGLE to GoogleBannerAdRequestFactory(),
        AdLoaderType.NIMBUS to NimbusBannerAdRequestFactory(),
    )

    override fun getAdBannerRequestFactory(type: AdLoaderType): AdRequestFactory {
        return factories[type] ?: error("No builder for type $type")
    }
}