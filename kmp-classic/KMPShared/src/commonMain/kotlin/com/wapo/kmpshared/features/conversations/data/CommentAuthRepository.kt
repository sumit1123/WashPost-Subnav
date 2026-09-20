package com.wapo.kmpshared.features.conversations.data

import com.wapo.kmpshared.core.KMPResult
import com.wapo.kmpshared.core.config.ConversationsConfig
import com.wapo.kmpshared.core.di.Qualifiers
import com.wapo.kmpshared.core.lifecycle.AppLifecycle
import com.wapo.kmpshared.core.network.BaseService
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import com.wapo.kmpshared.logger.WPLogger
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Single
class CommentAuthRepository(
    @Named(Qualifiers.CONVERSATIONS) private val client: HttpClient,
    @Named(Qualifiers.APPLICATION_SCOPE) private val externalScope: CoroutineScope,
    private val appLifecycle: AppLifecycle,
    private val config: ConversationsConfig,
    private val platformNetworkProvider: PlatformNetworkProvider,
    private val logger: WPLogger,
) : BaseService(appLifecycle, platformNetworkProvider, externalScope, logger) {
    suspend fun updateDisplayName(newName: String): KMPResult<Unit> {
        val credentials = platformNetworkProvider.getCredentials() ?: return KMPResult.Error("Auth Error: Missing platform credentials")

        val updateResult =
            toKMPResult("updateDisplayName") {
                client.post(config.displayNameURL.toString()) {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer ${credentials.accessToken}")
                    header("clientId", credentials.clientID)
                    setBody(UpdateDisplayNameRequest(newName))
                }
            }

        return when (updateResult) {
            is KMPResult.Success -> {
                // Force profile refresh after display name change
                externalScope.launch {
                    try {
                        platformNetworkProvider.refreshCredentials()
                    } catch (e: Exception) {
                        // logged on client side
                    }
                }
                KMPResult.Success(Unit)
            }

            is KMPResult.Error -> {
                updateResult
            }
        }
    }
}
