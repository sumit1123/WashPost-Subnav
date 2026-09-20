package com.wapo.flagship.querypolicies

import com.wapo.flagship.base.REQUEST_TIMEOUT_MS
import com.wapo.flagship.model.Status
import com.wapo.flagship.network.retrofit.network.APIResult

/**
 * A policy that never initiate a network request and always serves the cache
 * It is supposed to be used to retrieve a cache entity from cache
 */
class CacheOnlyQueryPolicy<T> : QueryPolicy<T> {
    override fun needUpdate(t: T?): Boolean = false

    override fun onResponse(result: APIResult<T>): Status<out T> = Status.Error("network response should never happen")

    override fun timeOut(): Int = REQUEST_TIMEOUT_MS
}
