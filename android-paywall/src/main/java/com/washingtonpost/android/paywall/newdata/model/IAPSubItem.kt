/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.washingtonpost.android.paywall.newdata.model

/**
 * Open class used to manage common data fields between sub items on both platforms
 */
open class IAPSubItem {
    open var productId: String? = null
    open var title: String?  = null
    open var basePlanId: String? = null
    open var basePrice: String? = null
    open var currencyCode: String? = null
    open var subscriptionPeriod: String? = null
    /** All active offers in the config **/
    open var offers: MutableList<IAPOfferItem>? = null
    open var offerPrice: String? = null
    open var offerPeriod: String? = null
    open var offerPriceCycles: Int? = null

    open fun getIntroOfferIfInConfig(): IAPOfferItem? {
        return null
    }
}
