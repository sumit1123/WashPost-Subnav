package com.washingtonpost.android.paywall.api

import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.paywall.PaywallConnector
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthStateManager
import com.washingtonpost.android.paywall.network.retrofit.APIResult
import com.washingtonpost.android.paywall.network.retrofit.APIResult.Failure.Companion.isConnectionProblem
import com.washingtonpost.android.paywall.util.PaywallConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

interface NonceRepository {
    /**
     * Fetches a nonce/token for external link authentication
     * @param externalUrl: The URL to authenticate with (for logging context)
     * @return The nonce if successfully fetched, null otherwise
     */
    suspend fun fetchNonce(
        externalUrl: String?
    ): String?
}

class NonceRepositoryImpl(
    private val nonceApi: NonceApiService,
    private val paywallService: PaywallService,
    private val authStateManager: AuthStateManager,
    private val paywallConnector: PaywallConnector,
    private val ioDispatcher: CoroutineDispatcher,
) : NonceRepository {
    override suspend fun fetchNonce(
        externalUrl: String?,
    ): String? = withContext(ioDispatcher) {
        val nonceUrl = paywallService.oAuthConfigStub.nonceUrl
        val accessToken = authStateManager.getCurrent().accessToken
        val clientId = paywallConnector.getClientId()

        when {
            nonceUrl.isEmpty() -> {
                Logger.e(TAG, "NonceUrl not configured")
                return@withContext null
            }

            accessToken.isNullOrEmpty() -> {
                Logger.e(TAG, "Not authenticated")
                return@withContext null
            }

            clientId.isNullOrEmpty() -> {
                Logger.e(TAG, "clientId not found")
                return@withContext null
            }
        }

        val result = nonceApi.fetchNonce(
            nonceUrl,
            PaywallConstants.BEARER_PREFIX + " " + accessToken,
            clientId
        )

        when (result) {
            is APIResult.Success -> {
                val nonce = result.data?.nonce
                if (!nonce.isNullOrEmpty()) {
                    nonce
                } else {
                    paywallConnector.logE(
                        EventLog.Builder()
                            .setErrorMessage("Nonce fetch failed: Nonce is null or empty in generate-login-nonce response")
                            .set("external_url", externalUrl)
                    )
                    null
                }
            }

            is APIResult.Failure -> {
                if (paywallConnector.isOnline && !result.statusCode.isConnectionProblem()) {
                    paywallConnector.logE(
                        EventLog.Builder()
                            .setErrorMessage("Nonce fetch failed: response=${result.rawResponse}")
                            .setErrorCode(result.statusCode)
                            .set("external_url", externalUrl)
                    )
                }
                null
            }

            is APIResult.NetworkError -> {
                if (paywallConnector.isOnline) {
                    paywallConnector.logE(
                        EventLog.Builder()
                            .setErrorMessage("Nonce fetch failed: message=${result.error.message}")
                            .set("external_url", externalUrl)
                    )
                }
                null
            }
        }
    }

    companion object {
        private const val TAG = "NonceRepositoryImpl"
    }
}