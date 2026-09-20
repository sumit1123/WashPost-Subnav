package com.washingtonpost.android.config.data.datasources.dto.config.paywallconf

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywallconf.PaywallConf

@JsonClass(generateAdapter = true)
data class RawPaywallConf(
    @Json(name = "abTestName") val abTestName: String? = null,
    @Json(name = "blockers") val blockers: List<RawBlocker>? = null,
    @Json(name = "metering") val metering: RawMetering? = null,
    @Json(name = "frontSubscriptionBanner") val frontSubscriptionBanner: RawFrontSubscriptionBanner? = null,
    @Json(name = "productToSkuMap") val productToSkuMap: Map<String, RawProductSkuEntry>? = null,
    @Json(name = "adFreeProductToSkuMap") val adFreeProductToSkuMap: Map<String, String>? = null,
    @Json(name = "ad_free_iterable_products") val adFreeIterableProducts: List<String>? = null,
    /** Offers available for paywall */
    @Json(name = "availableOffers") val paywallOffers: Map<String, List<RawPaywallOffer>>? = null,
    /** Offers available for any direct purchase (airship IAM, etc.) */
    @Json(name = "offersAllowedList") val allActiveOffers: Map<String, List<String>>? = null,
) {
    fun mapToDomain(): PaywallConf {
        return PaywallConf(
            abTestName = abTestName,
            blockers = blockers?.map { it.mapToDomain() },
            metering = metering?.mapToDomain(),
            frontSubscriptionBanner = frontSubscriptionBanner?.mapToDomain(),
            productToSkuMap = productToSkuMap?.mapValues { it.value.mapToDomain() },
            adFreeProductToSkuMap = adFreeProductToSkuMap,
            adFreeIterableProducts = adFreeIterableProducts,
            paywallOffers = paywallOffers?.mapValues { it.value.map { it.mapToDomain() } },
            allActiveOffers = allActiveOffers,
        )
    }
}
