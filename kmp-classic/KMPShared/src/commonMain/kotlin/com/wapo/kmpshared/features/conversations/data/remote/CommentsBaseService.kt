package com.wapo.kmpshared.features.conversations.data.remote

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.core.di.Qualifiers
import com.wapo.kmpshared.core.lifecycle.AppLifecycle
import com.wapo.kmpshared.core.network.BaseService
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import com.wapo.kmpshared.logger.WPLogger
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.koin.core.annotation.Named

abstract class CommentsBaseService(
    @Named(Qualifiers.APPLICATION_SCOPE) private val externalScope: CoroutineScope,
    val coder: Json,
    appLifecycle: AppLifecycle,
    platformNetworkProvider: PlatformNetworkProvider,
    logger: WPLogger,
) : BaseService(appLifecycle, platformNetworkProvider, externalScope, logger) {
    /**
     * The Single Source of Truth for GraphQL calls.
     * 1. Executes the network call.
     * 2. Extracts the body as text.
     * 3. Snoops for GraphQL 'errors' array.
     * 4. Decodes the success data into T.
     */
    protected suspend inline fun <reified T> executeCommentsRequest(
        requestName: String,
        logContext: String? = null,
        noinline gqlRequest: suspend () -> HttpResponse,
    ): KMPResult<T> =
        toKMPResult(requestName, logContext) {
            val response = gqlRequest()
            val bodyString = response.bodyAsText()

            val graphQLError = checkCommentsError(bodyString)
            if (graphQLError != null) {
                throw Exception(graphQLError.message)
            }

            coder.decodeFromString<T>(bodyString)
        }

    companion object {
        @PublishedApi
        internal const val TAG = "CommentsBaseService"
    }

    protected fun checkCommentsError(bodyString: String): KMPResult.Error? =
        try {
            val errorCheck = coder.decodeFromString<CommentsResponse<JsonElement>>(bodyString)
            if (!errorCheck.errors.isNullOrEmpty()) {
                KMPResult.Error(message = errorCheck.errors.first().message)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
}
