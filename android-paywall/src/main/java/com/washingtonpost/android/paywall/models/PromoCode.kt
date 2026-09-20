package com.washingtonpost.android.paywall.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Model for promo code that is generated from the the verify device subscription request.
 * This model creates moshi adapters so that we can easily serialize/deserialize for storing and retrieving
 * the promo code from the shared preferences.
 */
@JsonClass(generateAdapter = true)
data class PromoCode(
    @Json(name = "promoCode")
    val promoCode: String? = null,
    @Json(name = "promoName")
    val promoName:String? = null,
    @Json(name = "startDate")
    val startDate: String? = null,
    @Json(name = "endDate")
    val endDate: String? = null,
    @Json(name = "sku")
    val sku: String? = null,
    @Json(name = "promoTermType")
    /*
        Promo term type can be e.g. FREE TRIAL
     */
    val promoTermType: String? = null,
    @Json(name = "promoTerm")
    /*
        Promo term can be in DAYS, MONTHS etc.
     */
    val promoTerm: String? = null,
    /*
        Duration in Days, months etc. E.g 60 for 6 days FREE TRIAL
     */
    @Json(name = "promoDuration")
    val promoDuration: Int? = null)
