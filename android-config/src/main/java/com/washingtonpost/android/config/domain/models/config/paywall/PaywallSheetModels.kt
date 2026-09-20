package com.washingtonpost.android.config.domain.models.config.paywall

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
data class PaywallSheetModels(
    val meterNoSub: PaywallSheetModel,
    val meterTerminatedSub: PaywallSheetModel,
    val globalCtaNoSub: PaywallSheetModel,
    val globalCtaTerminatedSub: PaywallSheetModel,
    val featureNoSub: PaywallSheetModel,
    val featureTerminatedSub: PaywallSheetModel,
)

/**
 * Main Paywall / Product Page model
 * [label] - top label
 * [title] - describes what triggered the paywall
 * [pages] - [ProductPage] objects that show product offerings
 */
data class PaywallSheetModel(
    val label: String,
    val title: String,
    val pages: List<ProductPage>,
)

/**
 * Model for Product Page
 * [title] - Product grouping
 * [featuresExcluded] - Features not a part of this product group
 * [featuresIncluded] - Features that are a part of this product group
 * [products] - [Product] objects that describe the specific sub offering.
 */
data class ProductPage(
    val icon: String,
    val title: String,
    val featuresIncluded: List<String>,
    val featuresExcluded: List<String>,
    val products: List<Product>,
)

/**
 * [sku] - product productId used to retrieve sub offering from app store
 * [pill] - tag to describe savings and other info for sub offering
 */
data class Product(
    val sku: String,
    val pill: String,
)