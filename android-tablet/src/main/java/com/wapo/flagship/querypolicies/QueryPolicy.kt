package com.wapo.flagship.querypolicies

import com.wapo.flagship.model.Status
import com.wapo.flagship.network.retrofit.network.APIResult

/**
 * A request interface that allows fine tune data retrieving from the repository
 */
interface QueryPolicy<T> {
    fun needUpdate(t: T?): Boolean

    fun onResponse(result: APIResult<T>): Status<out T>

    /**
     * Network request timeout in MS
     */
    fun timeOut(): Int
}
