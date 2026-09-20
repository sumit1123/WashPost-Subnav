// Copyright (c) 2022 The Washington Post. All rights reserved.

package com.wapo.flagship.features.preferencesapi.repo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wapo.android.commons.util.Logger
import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import com.wapo.android.commons.constants.*
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.AppContext
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.content.AlertsSettingsImpl
import com.wapo.flagship.features.preferencesapi.models.TopicNotificationsAnonRequest
import com.wapo.flagship.features.preferencesapi.models.TopicNotificationsGetResponse
import com.wapo.flagship.features.preferencesapi.models.TopicNotificationsSetRequest
import com.wapo.flagship.features.preferencesapi.models.TopicNotificationsSetResponse
import com.wapo.flagship.features.preferencesapi.services.PreferencesApiService
import com.wapo.flagship.features.preferencesapi.state.PreferencesSyncCoordinator
import com.wapo.flagship.features.preferencesapi.util.AnonymousEncryptor
import com.wapo.flagship.util.PrefUtils
import com.wapo.flagship.util.ReachabilityUtil
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.android.paywall.auth.AuthHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import kotlin.jvm.Throws

class TopicNotificationsRepo {
    private var preferencesApiService: PreferencesApiService? = null
    private val shouldEnableNetworkDebugging =
        false // enable to verify each call with headers and response code
    private val interceptor: HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            this.level = HttpLoggingInterceptor.Level.BODY
        }
    private val client: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .apply {
                addInterceptor(DefaultHeadersInterceptor())
                if (shouldEnableNetworkDebugging) this.addInterceptor(interceptor)
            }.cache(null)
            .build()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val moshi = Moshi.Builder().build()
    private val anonRequestAdapter by lazy {
        moshi.adapter(TopicNotificationsAnonRequest::class.java)
    }

    private fun getPreferencesApiService(): PreferencesApiService {
        if (this.preferencesApiService == null) {
            val converterFactory = GsonConverterFactory.create(GsonBuilder().create())
            this.preferencesApiService =
                Retrofit
                    .Builder()
                    .baseUrl(ConfigManager.getInstance().config.preferencesApiConfig.preferenceBaseUrl)
                    .client(client)
                    .addConverterFactory(converterFactory)
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build()
                    .create(PreferencesApiService::class.java)
        }
        return this.preferencesApiService!!
    }

    fun syncTopicsWithPreferencesApi(isStale: Boolean = false) {
        if (!ReachabilityUtil.isConnected(FlagshipApplication.getInstance())) return
        if (!isNotificationsPermissionGranted()) return

        scope.launch {
            try {
                if (isStale) {
                    getTopicNotifications { remoteList ->
                        val remoteConversations = remoteList?.contains(CONVERSATIONS_KEY) ?: false

                        val alerts = AlertsSettingsImpl()
                        val localConversationsEnabled = alerts.getAlertsTopicsList()
                            .find { it.topic.topicKey == CONVERSATIONS_KEY }
                            ?.isEnabled == true

                        // If remote differs, adjust local toggle first.
                        if (remoteConversations != localConversationsEnabled) {
                            AppContext.changeTopicEnabled(CONVERSATIONS_KEY, remoteConversations)
                            // Build new local list snapshot for push (include conversations change plus existing local enables).
                            val updatedLocalList =
                                alerts.getAlertsTopicsList()
                                    .filter { it.isEnabled }
                                    .map { it.topic.topicKey }
                            setTopicNotifications(updatedLocalList)
                        } else {
                            Logger.d("TopicSync", "Conversations already in sync; skip SET")
                            // LMT was set in processGetTopicNotifications; no push needed.
                        }
                    }
                } else {
                    val localTopics = AlertsSettingsImpl().getAlertsTopicsList()
                        .filter { it.isEnabled }
                        .map { it.topic.topicKey }
                    setTopicNotifications(localTopics)
                }
            } catch (e: Exception) {
                EventLog.Builder()
                    .setMessage("conversations sync failed")
                    .setModule(LogModules.PREFERENCES)
                    .setErrorMessage(e.message)
                    .run { RemoteLog.e(FlagshipApplication.getInstance(), build()) }
            }

        }
    }

    private fun isNotificationsPermissionGranted(): Boolean {
        val context = FlagshipApplication.getInstance().applicationContext
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                FlagshipApplication.getInstance().applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun getConversationNotificationPreference(callback: (Boolean?) -> Unit) {
        if (PaywallService.getInstance()?.isWpUserLoggedIn == true &&
            ReachabilityUtil.isConnected(
                FlagshipApplication.getInstance(),
            )
        ) {
            scope.launch {
                getTopicNotifications { enabledTopics ->
                    callback(enabledTopics?.contains(CONVERSATIONS_KEY))
                }
            }
        } else {
            callback(null)
        }
    }

    fun mergeTopics(
        localTopics: List<String>,
        remoteTopics: List<String>?,
    ): List<String> =
        remoteTopics?.let {
            localTopics.union(it).toList()
        } ?: localTopics

    fun getTopicNotifications(callback: (List<String>?) -> Unit) {
        val context = FlagshipApplication.getInstance().applicationContext
        AuthHelper.getInstance(context).runWithValidToken {
            // This code runs ONLY after token is ensured valid
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = getPreferencesApiService().getTopicNotifications(getHeaders())
                    val topics = processGetTopicNotifications(response)
                    callback(topics)
                } catch (e: Exception) {
                    callback(null)
                }
            }
        }
    }

    private fun setTopicNotifications(topicsList: List<String>) {
        if (PaywallService.getInstance()?.isWpUserLoggedIn == true) {
            setTopicNotificationsForLoggedInUser(topicsList)
        } else {
            setTopicNotificationsForAnonymousUser(topicsList)
        }
    }

    private fun setTopicNotificationsForLoggedInUser(topicsList: List<String>) {
        AuthHelper.getInstance(FlagshipApplication.getInstance().applicationContext).runWithValidToken {
            scope.launch {
                try {
                    PreferencesSyncCoordinator.markDirty(PreferencesSyncCoordinator.TOPIC_NOTIFICATIONS)
                    val requestBody = TopicNotificationsSetRequest(topicsList)
                    val response =
                        getPreferencesApiService().setTopicNotifications(
                            getHeaders(),
                            requestBody,
                        )
                    processSetTopicNotifications(response)
                } catch (e: Exception) {
                    val builder: EventLog.Builder = EventLog.Builder()
                    builder
                        .setMessage("setTopicNotifications request failed")
                        .setModule(LogModules.PREFERENCES)
                        .setErrorMessage(e.message)
                        .set("cause", e.cause)
                    RemoteLog.e(FlagshipApplication.getInstance(), builder.build())
                }
            }
        }
    }

    private fun setTopicNotificationsForAnonymousUser(topicsList: List<String>) {
        scope.launch {
            try {
                PreferencesSyncCoordinator.markDirty(PreferencesSyncCoordinator.TOPIC_NOTIFICATIONS)
                val body = TopicNotificationsAnonRequest(
                    value = topicsList,
                    deviceId = PaywallService.getConnector().deviceId,
                    timestamp = System.currentTimeMillis(),
                )
                val jsonBody = anonRequestAdapter.toJson(body)
                val response = getPreferencesApiService().setTopicNotificationsForAnonymousUser(
                    getHeadersForAnonymousRequest(jsonBody),
                    jsonBody.toRequestBody("application/json".toMediaType())
                )
                processSetTopicNotifications(response)
            } catch (e: Exception) {
                val isNetworkError = !AppContextUtils.isConnectingOrConnected()
                if (!isNetworkError) {
                    val builder: EventLog.Builder = EventLog.Builder()
                    builder.setMessage("setTopicNotificationsForAnonymousUser request failed")
                        .setModule(LogModules.PREFERENCES)
                        .setErrorMessage(e.message)
                        .set("cause", e.cause)
                    RemoteLog.e(FlagshipApplication.getInstance(), builder.build())
                }
            }
        }
    }

    private fun processGetTopicNotifications(response: TopicNotificationsGetResponse): List<String>? =
        if (response.status?.lowercase(Locale.US) == SUCCESS) {
            val topicNotificationsPreference =
                response.preferenceValues?.find {
                    it?.id?.preference?.name == TOPIC_NOTIFICATIONS_NAME
                }
            val topicsList = topicNotificationsPreference?.value
            topicNotificationsPreference?.lastUpdated?.let { lu ->
                PreferencesSyncCoordinator.updateFromRemote(
                    FlagshipApplication.getInstance().applicationContext,
                    PreferencesSyncCoordinator.TOPIC_NOTIFICATIONS,
                    lu
                )
            }
            topicsList?.let { it.ifEmpty { null } }
        } else {
            remoteLogUnsuccessfulResponse(GET, response.toString())
            null
        }

    private fun processSetTopicNotifications(response: TopicNotificationsSetResponse) {
        if (response.status?.lowercase(Locale.US) == SUCCESS) {
            PrefUtils.setHasSyncedTopicNotifications(
                FlagshipApplication.getInstance().applicationContext,
                true,
            )
            AppContext.setPrefApiOneTimeSync(
                FlagshipApplication.getInstance().applicationContext,
                true
            )
            Logger.d("PushPrefApiSyncSuccess", "onUpgrade " + response.preferenceValue)
            val lu = response.preferenceValue?.lastUpdated ?: System.currentTimeMillis()
            PreferencesSyncCoordinator.updateFromRemote(
                FlagshipApplication.getInstance().applicationContext,
                PreferencesSyncCoordinator.TOPIC_NOTIFICATIONS,
                lu
            )
        } else {
            remoteLogUnsuccessfulResponse(SET, response.toString())
        }
    }


    private fun remoteLogUnsuccessfulResponse(
        method: String,
        response: String,
    ) {
        EventLog
            .Builder()
            .apply {
                setMessage(
                    "$MESSAGE: $TOPIC_NOTIFICATIONS_NAME/$method returned a response of $response",
                )
                setModule(LogModules.PREFERENCES)
            }.run {
                RemoteLog.e(FlagshipApplication.getInstance().applicationContext, build())
            }
    }

    private fun getHeaders(): HashMap<String, String> {
        val headers = getBaseHeaders()
        headers[AUTHORIZATION] = "Bearer " +
            AuthHelper
                .getInstance(
                    FlagshipApplication.getInstance().applicationContext,
                ).accessToken
        headers[CLIENT_ID] = PaywallService.getConnector().clientId
        headers[CLIENT_IP] = PaywallService.getConnector().ipAddress
        return headers
    }

    @Throws(
        IllegalStateException::class,
        NoSuchAlgorithmException::class,
        InvalidKeyException::class
    )
    private fun getHeadersForAnonymousRequest(jsonBody: String): HashMap<String, String> {
        val headers = getBaseHeaders()
        headers[SENDER] = NATIVE_APPS
        headers[X_ACCESS_TOKEN] = AnonymousEncryptor.encrypt(jsonBody)
        headers[CLIENT_ID] = PaywallService.getConnector().clientId
        headers[CLIENT_IP] = PaywallService.getConnector().ipAddress
        return headers
    }

    private fun getBaseHeaders(): HashMap<String, String> =
        hashMapOf(
            Pair(CLIENT_APP, PaywallService.getConnector().appName),
            Pair(REQUEST_ID, UUID.randomUUID().toString()),
            Pair(DEVICE_ID, PaywallService.getConnector().deviceId),
            Pair(CLIENT_USER_AGENT, PaywallService.getConnector().userAgent),
            Pair(CLIENT_APP_VERSION, PaywallService.getConnector().appVersion),
            Pair(OS_VERSION, Build.VERSION.SDK_INT.toString()),
            Pair(DEVICE_NAME, Build.MANUFACTURER + "-" + Build.MODEL),
        )

    companion object {
        const val MESSAGE = "Preferences API call failed"
        const val SUCCESS = "success"
        const val GET = "get"
        const val SET = "set"
        const val TOPIC_NOTIFICATIONS_NAME = "topic-notifications"
        const val CONVERSATIONS_KEY = "conversations"
        const val NATIVE_APPS = "nativeapps"

        @Volatile
        private var INSTANCE: TopicNotificationsRepo? = null

        @JvmStatic
        fun getInstance(): TopicNotificationsRepo =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: TopicNotificationsRepo().also {
                        INSTANCE = it
                    }
            }
    }
}
