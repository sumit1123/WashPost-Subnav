/*
 *  Copyright (c) 2022 The Washington Post. All rights reserved.
 */

package com.wapo.flagship.querypolicies

import com.wapo.flagship.models.Status
import com.wapo.flagship.network.APIResult

/**
 * A request interface that allows fine tune data retrieving from the repository
 */
interface QueryPolicy<T> {
    fun needUpdate(t: T?): Boolean
    fun onResponse(result: APIResult<T>): Status<out T>

    /**
     * Network request timeout in MS
     */
    fun timeOut() : Int

}