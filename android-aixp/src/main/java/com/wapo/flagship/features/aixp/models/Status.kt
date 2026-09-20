/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.models

/**
 * Status of the request made to load an article.
 */
sealed class Status<T> {
    /**
     * It is used when cache isn't available to be served.
     */
    class Network<T>(val data: T) : Status<T>()

    /**
     * Cache status is used most of the times with [DefaultQueryPolicy]
     */
    class Cache<T>(val data: T) : Status<T>()

    /**
     * Reports error when network call fails and cache isn't available.
     */
    class Error(val message: String) : Status<Nothing>()
}