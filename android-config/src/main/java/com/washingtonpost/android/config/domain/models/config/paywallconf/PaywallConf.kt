package com.washingtonpost.android.config.domain.models.config.paywallconf

data class PaywallConf(
    val abTestName: String?,
    val blockers: List<Blocker>?,
    val metering: Metering?,
    val frontSubscriptionBanner: FrontSubscriptionBanner?,
    val productToSkuMap: Map<String, ProductSkuEntry>?,
    val adFreeProductToSkuMap: Map<String, String>?,
    val adFreeIterableProducts: List<String>?,
    val paywallOffers: Map<String, List<PaywallOffer>>?,
    val allActiveOffers: Map<String, List<String>>?,
)
