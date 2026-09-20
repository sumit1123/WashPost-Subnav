// Copyright (c) 2026 The Washington Post. All rights reserved.

package com.wapo.kmpshared.core.network

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.core.lifecycle.AppLifecycle
import com.wapo.kmpshared.logger.WPLogger
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

abstract class BaseService(
    private val appLifecycle: AppLifecycle,
    private val platformNetworkProvider: PlatformNetworkProvider,
    private val externalScope: CoroutineScope,
    private val logger: WPLogger,
) {
    /**
     * Maps network execution to a generic NetworkResult with an optional status code in error.
     */
    suspend fun <T> toKMPResult(
        requestName: String,
        logContext: String? = null,
        block: suspend () -> T,
    ): KMPResult<T> =
        try {
            KMPResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleException(e, requestName, logContext)
        }

    protected fun <T> handleException(
        e: Exception,
        requestName: String,
        logContext: String? = null,
    ): KMPResult<T> {
        val isNetworkIssue = isConnectivityError(e, appLifecycle.reachability.value.isReachable)
        val info = if (logContext != null) "$logContext " else ""

        val resultMessage: String
        val logMessage: String

        when {
            isNetworkIssue -> {
                resultMessage = "Request timed out or no connection."
                logMessage = resultMessage
            }

            e is ResponseException -> {
                val statusCode = e.response.status
                // Currently, credentials are refreshed only on a 401 Unauthorized response.
                // As we extend to implement new features, we should revisit whether refreshing
                if (statusCode == HttpStatusCode.Unauthorized) {
                    refreshCredentials()
                }
                resultMessage = e.message ?: "Unknown Error"
                logMessage = "[$requestName Error] $info err_code: ${statusCode.value} | ${e.message}"
            }

            else -> {
                resultMessage = e.message ?: "Unknown Error"
                logMessage = "[$requestName Error] $info ${e.message ?: "Unknown Error"}"
            }
        }

        // Only log if it's NOT a network/connectivity issue
        if (!isNetworkIssue) {
            logger.error(logMessage, TAG)
        }

        return KMPResult.Error(message = resultMessage)
    }

    private fun refreshCredentials() {
        externalScope.launch {
            try {
                platformNetworkProvider.refreshCredentials()
            } catch (_: Exception) {
                // logged on client side
            }
        }
    }

    companion object {
        const val TAG = "BaseService"
    }
}

expect fun isConnectivityError(
    e: Throwable,
    isReachable: Boolean,
): Boolean
