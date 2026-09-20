package com.wapo.flagship.network.retrofit.network

import okhttp3.Headers
import java.net.HttpURLConnection

/**
 * Base Result class that wraps API responses into successes or failures.
 */
sealed class APIResult<out T> {
    data class Success<T>(
        val data: T?,
        val headers: Headers,
    ) : APIResult<T>()

    data class Failure(
        val statusCode: Int,
        val rawResponse: String?,
    ) : APIResult<Nothing>() {
        companion object {
            /**
             * Helper function to group http error statuses that describe
             * a connection problem with the API
             */
            fun Int.isConnectionProblem(): Boolean =
                when (this) {
                    HttpURLConnection.HTTP_FORBIDDEN, // When a url requires a VPN connection
                    HttpURLConnection.HTTP_CLIENT_TIMEOUT,
                    -> true
                    else -> false
                }
        }

        /**
         * Added for Remote logging purposes
         */
        override fun getMessage(): String = "Status=$statusCode; Rawresponse=$rawResponse"
    }

    class NetworkError(
        val error: Throwable,
    ) : APIResult<Nothing>() {
        /**
         * Added for Remote logging purposes
         */
        override fun getMessage(): String = "message=${error.message}"
    }

    /**
     * Override this for remote logging
     */
    open fun getMessage(): String = "No Message"
}
