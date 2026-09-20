/* Copyright (c) 2023 The Washington Post. All rights reserved. */

package com.wapo.flagship.features.articles.recycler

interface SubsJSCallbacks {
    fun productOfferDetails(product: String, info: String)
    fun enabledPush(subscribed: Boolean)
}
