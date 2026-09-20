package com.wapo.flagship.kmp.core.config

import android.os.Build
import com.wapo.android.commons.constants.CLIENT_APP
import com.wapo.android.commons.constants.CLIENT_APP_VERSION
import com.wapo.android.commons.constants.DEVICE_ID
import com.wapo.android.commons.constants.DEVICE_NAME
import com.wapo.android.commons.constants.OS_VERSION
import com.wapo.android.commons.constants.PLATFORM
import com.wapo.android.commons.constants.USER_AGENT
import com.wapo.android.commons.domain.DeviceUtilRepo
import com.wapo.android.commons.domain.UtilsRepo
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.flagship.util.tracking.Measurement
import com.wapo.kmpshared.core.network.NetworkSession
import com.wapo.kmpshared.core.network.PlatformCredentials
import com.wapo.kmpshared.core.network.PlatformNetworkProvider
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlatformNetworkProviderImpl @Inject constructor(
    private val deviceUtilRepo: DeviceUtilRepo,
    private val utilsRepo: UtilsRepo,
    private val defaultClient: OkHttpClient,
) : PlatformNetworkProvider {
    private val loggerExecutor by lazy {
        ThreadPoolExecutor(
            1, 1,
            60L, TimeUnit.SECONDS,
            LinkedBlockingQueue(),
        ) { r -> Thread(r, "kmp-logger-okhttp").apply { isDaemon = true } }
            .apply { allowCoreThreadTimeOut(true) }
    }

    private val loggerDispatcher by lazy {
        Dispatcher(loggerExecutor).apply {
            maxRequests = 1
            maxRequestsPerHost = 1
        }
    }

    private val loggerClient by lazy {
        defaultClient.newBuilder()
            .dispatcher(loggerDispatcher)
            .retryOnConnectionFailure(true)
            .build()
    }


    override fun getHeaders(): Map<String, String> {
        val headerMap = mutableMapOf<String, String>()

        headerMap[PLATFORM] = if (utilsRepo.isAmazonBuild()) "Amazon" else "Android"
        headerMap[CLIENT_APP] = "android-classic"
        headerMap[DEVICE_NAME] = "${Build.MANUFACTURER}-${Build.MODEL}"
        headerMap[OS_VERSION] = Build.VERSION.SDK_INT.toString()
        headerMap[DEVICE_ID] = deviceUtilRepo.getDeviceSerialId() ?: "unknown"
        headerMap[CLIENT_APP_VERSION] = utilsRepo.getAppVersionName()
        headerMap[USER_AGENT] = Measurement.getUserAgent()

        return headerMap
    }

    override fun getSessions(): NetworkSession =
        NetworkSession(
            default = defaultClient,
            logger = loggerClient,
        )

    override fun getCredentials(): PlatformCredentials? = buildCachedCredentials()

    override suspend fun refreshCredentials(): PlatformCredentials? =
        withContext(Dispatchers.IO) { buildCredentials() }

    /**
     * Returns credentials using only cached data — no network calls.
     * Safe to call on the main thread.
     */
    private fun buildCachedCredentials(): PlatformCredentials? {
        val appContext = AppContextUtils.appContext
        val loggedInUser = PaywallService.getInstance()?.loggedInUser ?: return null
        val accessToken = AuthHelper.getInstance(appContext).accessToken ?: return null
        val clientID = PaywallService.getConnector()?.getClientId() ?: return null
        // Read only the already-stored ctoken — avoids a blocking /profile network call.
        val commentsToken = loggedInUser.cToken ?: return null

        return PlatformCredentials(
            loginID = loggedInUser.uuid,
            secureLoginID = loggedInUser.secureLoginID,
            accessToken = accessToken,
            clientID = clientID,
            commentsToken = commentsToken,
        )
    }

    /**
     * Builds credentials by force-refreshing the ctoken via a blocking /profile call.
     * Must only be called from a background thread (e.g. inside [refreshCredentials]).
     */
    private fun buildCredentials(): PlatformCredentials? {
        val appContext = AppContextUtils.appContext
        val loggedInUser = PaywallService.getInstance()?.loggedInUser ?: return null
        val accessToken = AuthHelper.getInstance(appContext).accessToken ?: return null
        val clientID = PaywallService.getConnector()?.getClientId() ?: return null
        val commentsToken = PaywallService.getInstance()?.getValidCToken() ?: return null

        return PlatformCredentials(
            loginID = loggedInUser.uuid,
            secureLoginID = loggedInUser.secureLoginID,
            accessToken = accessToken,
            clientID = clientID,
            commentsToken = commentsToken,
        )
    }
}
