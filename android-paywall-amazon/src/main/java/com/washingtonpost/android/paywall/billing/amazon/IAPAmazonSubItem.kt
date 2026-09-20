/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.washingtonpost.android.paywall.billing.amazon

import com.washingtonpost.android.paywall.newdata.model.IAPSubItem

class IAPAmazonSubItem(
    sku: String,
    title: String,
    price: String?,
    currencyCode: String?,
    subscriptionPeriod: String?,
    freeTrialPeriod: String?,
    offerPrice: String?,
    offerPeriod: String?,
    offerPriceCycles: Int?
): IAPSubItem() {
    
    init {
        this.productId = sku
        this.title = title
        this.basePrice = price
        this.currencyCode = currencyCode
        this.subscriptionPeriod = subscriptionPeriod
        this.offerPeriod = freeTrialPeriod
        this.offerPrice = offerPrice
        this.offerPeriod = offerPeriod
        this.offerPriceCycles = offerPriceCycles
    }
}