/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.models

import com.wapo.flagship.features.articles2.models.Article415

/**
 * Status of the request made to load an article.
 */
sealed class Status<T> {

    /**
     * Network status is used in case of  [com.wapo.flagship.querypolicies.BypassCacheQueryPolicy]
     * e.g. notifications, deeplinks etc.
     * It is also used when cache isn't available to be served.
     */
    class Network<T>(val data: T) : Status<T>()

    /**
     * Cache status is used most of the times with
     * [com.wapo.flagship.querypolicies.DefaultQueryPolicy]
     */
    class Cache<T>(val data: T) : Status<T>()

    /**
     * Reports error when network call fails and cache isn't available.
     */
    class Error(val message: String) : Status<Nothing>()

    /**
     * Reported for forced webview article loading.
     */
    class Error415(val article415: Article415?) : Status<Nothing>()

}