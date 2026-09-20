/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.querypolicies

/**
 * A policy that always makes a network request irrespectively of cache existence
 * @param cacheFallback controls whether to serve the cache if the network request fails
 * This query policy is mainly used for notifications, alerts, deeplinks etc.
 */
class BypassCacheQueryPolicy<T>(cacheFallback: Boolean = true) :
    DefaultQueryPolicy<T>(cacheFallback) {

    override fun needUpdate(t: T?): Boolean {
        super.needUpdate(t)
        // always bypass cache
        return true
    }

}