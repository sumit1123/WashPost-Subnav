/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.querypolicies

import com.wapo.flagship.models.Status
import com.wapo.flagship.network.APIResult

/**
 * A policy that never initiate a network request and always serves the cache
 * It is supposed to be used to retrieve a cache entity from cache
 */
class CacheOnlyQueryPolicy<T> : QueryPolicy<T> {

    override fun needUpdate(t: T?): Boolean {
        return false
    }

    override fun onResponse(result: APIResult<T>): Status<out T> {
        return Status.Error("network response should never happen")
    }

    override fun timeOut(): Int {
        return 2500
    }

}