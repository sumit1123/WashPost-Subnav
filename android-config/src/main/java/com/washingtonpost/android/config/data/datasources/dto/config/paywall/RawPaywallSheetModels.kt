package com.washingtonpost.android.config.data.datasources.dto.config.paywall

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.washingtonpost.android.config.domain.models.config.paywall.PaywallSheetModel
import com.washingtonpost.android.config.domain.models.config.paywall.PaywallSheetModels
import com.washingtonpost.android.config.domain.models.config.paywall.Product
import com.washingtonpost.android.config.domain.models.config.paywall.ProductPage

/**
 * There are different models for defferent scenarios:
 * [meterNoSub] - For Paywall : User hits meter and has never had a subscription
 * [meterTerminatedSub] - For Paywall : User hits meter and has a sub that was terminated
 * [globalCtaNoSub] - For Product Page : User hits subscribe button on home page, in settings, or bottom cta with no previous sub
 * [globalCtaTerminatedSub] - For Product Page : User hits subscribe button on home page, in settings, or bottom cta with terminated sub
 * [featureNoSub] - For Paywall : User tries to save a story with no previous sub
 * [featureTerminatedSub] - For Paywall : User tries to save a story with terminated sub
 *
 * Note: the values in these models can be overwritten from the config.json files. If a value is not overwritten, default values are set.
 */
@JsonClass(generateAdapter = true)
data class RawPaywallSheetModels(
    @Json(name = "meterNoSub") val meterNoSub: RawPaywallSheetModel? = null,
    @Json(name = "meterTerminatedSub") val meterTerminatedSub: RawPaywallSheetModel? = null,
    @Json(name = "globalCtaNoSub") val globalCtaNoSub: RawPaywallSheetModel? = null,
    @Json(name = "globalCtaTerminatedSub") val globalCtaTerminatedSub: RawPaywallSheetModel? = null,
    @Json(name = "featureNoSub") val featureNoSub: RawPaywallSheetModel? = null,
    @Json(name = "featureTerminatedSub") val featureTerminatedSub: RawPaywallSheetModel? = null
) {
    fun mapToDomain(): PaywallSheetModels {
        return PaywallSheetModels(
            meterNoSub = (meterNoSub ?: RawPaywallSheetModel()).mapToDomain(),
            meterTerminatedSub = (meterTerminatedSub ?: RawPaywallSheetModel()).mapToDomain(),
            globalCtaNoSub = (globalCtaNoSub ?: RawPaywallSheetModel()).mapToDomain(),
            globalCtaTerminatedSub = (globalCtaTerminatedSub ?: RawPaywallSheetModel())
                .mapToDomain(),
            featureNoSub = (featureNoSub ?: RawPaywallSheetModel()).mapToDomain(),
            featureTerminatedSub = (featureTerminatedSub ?: RawPaywallSheetModel()).mapToDomain(),
        )
    }
}

/**
 * Main Paywall / Product Page model
 * [label] - top label
 * [title] - describes what triggered the paywall
 * [pages] - [RawProductPage] objects that show product offerings
 */
@JsonClass(generateAdapter = true)
data class RawPaywallSheetModel(
    @Json(name = "label") val label: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "pages") val pages: List<RawProductPage>? = null,
) {
    fun mapToDomain(): PaywallSheetModel {
        return PaywallSheetModel(
            label = label ?: "SPECIAL OFFER",
            title = title ?: "Subscribe to get the full experience.",
            pages = pages?.map { it.mapToDomain() } ?: listOf(
                ProductPage(
                    "",
                    "Core",
                    listOf("Unlimited access on the web and in our apps"),
                    listOf(
                        "A <b>bonus subscription</b> to share",
                        "Monthly <b>30-day digital passes</b> to share",
                        "Unlimited <b>eBooks</b> written by our journalists"
                    ),
                    listOf(
                        Product("wp.classic.basic", ""),
                        Product("wp.classic.basic.annual", "BEST VALUE")
                    )
                ),
                ProductPage(
                    "",
                    "Premium",
                    listOf(
                        "Unlimited access on the web and in our apps",
                        "A <b>bonus subscription</b> to share",
                        "Monthly <b>30-day digital passes</b> to share",
                        "Unlimited <b>eBooks</b> written by our journalists"
                    ),
                    listOf(),
                    listOf(
                        Product("monthly_all_access", ""),
                        Product("wp.classic.premium.annual", "BEST VALUE")
                    )
                ),
            ),
        )
    }
}

/**
 * Model for Product Page
 * [title] - Product grouping
 * [featuresExcluded] - Features not a part of this product group
 * [featuresIncluded] - Features that are a part of this product group
 * [products] - [RawProduct] objects that describe the specific sub offering.
 */
@JsonClass(generateAdapter = true)
data class RawProductPage(
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "featuresIncluded") val featuresIncluded: List<String>? = null,
    @Json(name = "featuresExcluded") val featuresExcluded: List<String>? = null,
    @Json(name = "products") val products: List<RawProduct>? = null,
) {
    fun mapToDomain(): ProductPage {
        return ProductPage(
            icon = icon.orEmpty(),
            title = title.orEmpty(),
            featuresIncluded = featuresIncluded.orEmpty(),
            featuresExcluded = featuresExcluded.orEmpty(),
            products = products?.map { it.mapToDomain() }.orEmpty(),
        )
    }
}

/**
 * [sku] - product productId used to retrieve sub offering from app store
 * [pill] - tag to describe savings and other info for sub offering
 */
@JsonClass(generateAdapter = true)
data class RawProduct(
    @Json(name = "sku") val sku: String? = null,
    @Json(name = "pill") val pill: String? = null,
) {
    fun mapToDomain(): Product {
        return Product(
            sku = sku.orEmpty(),
            pill = pill.orEmpty(),
        )
    }
}