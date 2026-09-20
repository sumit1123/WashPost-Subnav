package com.wapo.flagship.features.gifting.models

/**
 * A status class for request gift url api call
 */
sealed class RequestUrlApiStatus {
    /**
     * Call was successful and [url] was returned.
     */
    data class Success(
        val url: String,
    ) : RequestUrlApiStatus()

    /**
     * In all other cases other than success this api is treated as failure.
     */
    object Failure : RequestUrlApiStatus()
}
