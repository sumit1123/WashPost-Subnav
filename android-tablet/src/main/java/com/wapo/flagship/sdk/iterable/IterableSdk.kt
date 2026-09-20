// Copyright (c) 2025 The Washington Post. All rights reserved.
package com.wapo.flagship.sdk.iterable

import android.content.Context
import android.util.Log
import com.iterable.iterableapi.IterableApi
import com.iterable.iterableapi.IterableConfig
import com.iterable.iterableapi.IterableEmbeddedManager
import com.iterable.iterableapi.IterableEmbeddedMessage
import com.iterable.iterableapi.IterableInAppManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.AppContextUtils
import com.wapo.android.commons.util.Logger
import com.wapo.android.push.PushService
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.AppContext
import com.wapo.flagship.Utils
import com.wapo.flagship.features.deeplinks.AirshipAnalytics
import com.wapo.flagship.features.deeplinks.DeepLinksProcessor
import com.wapo.flagship.sdk.iterable.handlers.IterableAppCustomActionHandler
import com.wapo.flagship.sdk.iterable.handlers.IterableAppEmbeddedUpdateHandler
import com.wapo.flagship.sdk.iterable.handlers.IterableAppInAppHandler
import com.wapo.flagship.sdk.iterable.handlers.IterableAppUrlHandler
import com.wapo.flagship.sdk.iterable.models.IamMessageType
import com.wapo.flagship.sdk.iterable.models.MessageRequirement
import com.wapo.flagship.sdk.iterable.models.PaywallUser
import com.wapo.flagship.sdk.iterable.models.buildUserContext
import com.wapo.flagship.util.coroutines.CoroutineScopeProvider
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.ConfigOverride
import com.washingtonpost.android.config.domain.models.config.Config
import com.washingtonpost.android.paywall.models.MessageOperator
import com.washingtonpost.android.paywall.models.MessagePropertyType
import com.washingtonpost.android.paywall.util.PaywallConstants
import com.washingtonpost.android.paywall.util.PaywallUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import javax.inject.Inject
import kotlin.collections.get

/**
 * Iterable SDK integration class to make their API calls
 */
class IterableSdk @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val coroutineScopeProvider: CoroutineScopeProvider,
    private val secDataProvider: WapoSecDataProvider,
    private val config: Config,
    private val deepLinksProcessor: DeepLinksProcessor,
    private val contextUtils: AppContextUtils,
    private val moshi: Moshi
) {

    private var sdkInitialized = false
    private var iamAutomationPaused: Boolean? = null
    private val userFields: IterableAppUserFields by lazy {
        IterableAppUserFields(
            this,
            applicationContext
        )
    }
    private lateinit var embeddedUpdateHandler: IterableAppEmbeddedUpdateHandler
    private lateinit var analyticsHandler: IterableAnalyticsHandler
    private lateinit var inAppHandler: IterableAppInAppHandler

    init {
        initializeSdk()
    }

    private fun initializeSdk() {
        if (Utils.isProductFlavorAmazon() && config.iterableConfig.enableAmazonIntegration == false) return
        // Prepare Iterable Config
        inAppHandler = IterableAppInAppHandler(
            coroutineScopeProvider.sync,
            { getIamManager() },
            AirshipAnalytics
        )
        val configBuilder = IterableConfig.Builder()
            // handles https, action, itbl, and iterable links by default
            .setAllowedProtocols(allowedProtocols)
            .setCustomActionHandler(IterableAppCustomActionHandler(inAppHandler))
            .setUrlHandler(IterableAppUrlHandler(deepLinksProcessor, inAppHandler))
            .setInAppHandler(inAppHandler)
            .setAutoPushRegistration(false)
            .setEnableEmbeddedMessaging(true)

        // Enable Iterable Debug log level (0-6). 6 is default
        if (contextUtils.isDebuggableBuild())
            configBuilder.setLogLevel(0)

        // Get the Sdk key
        val key = if (AppContextUtils.isDebuggableBuild() && isSandboxEnv())
            secDataProvider.iterableSecretDev
        else
            secDataProvider.iterableSecretProd

        // Initialize SdK
        IterableApi.initialize(applicationContext, key, configBuilder.build())

        // Initialize Sdk handlers
        IterableApi.getInstance().apply {
            embeddedUpdateHandler = IterableAppEmbeddedUpdateHandler(
                config.iterableConfig, this@IterableSdk, contextUtils,
                { getSupportedBlockerConfigVersion() }
            ).apply {
                embeddedManager.addUpdateListener(this)
            }
            analyticsHandler =
                IterableAnalyticsHandler(
                    coroutineScopeProvider.sync,
                    this@apply, AirshipAnalytics,
                    inAppHandler
                )
            getIamManager().addListener {
                Logger.d(TAG, "Iterable, onInboxUpdated()")
                syncIamMessages()
            }
            Logger.d(TAG, "Iterable, initializeSdk(), sandbox=${isSandboxEnv()}")
        }

        sdkInitialized = true
    }

    fun attachIamProvider(pushService: PushService) {
        if (!sdkInitialized) return
        pushService.addIamProvider(IterableIamProvider(this))
    }

    fun getIterableApi(): IterableApi = IterableApi.getInstance()

    fun getIamManager(): IterableInAppManager = getIterableApi().inAppManager

    fun getEmbeddedManager(): IterableEmbeddedManager = getIterableApi().embeddedManager

    private fun getDerivedUserId(): String = AppContext.getIterableUserId(applicationContext)

    fun isSdkInitialized() = sdkInitialized

    private fun syncEmbeddedMessages() {
        if (!sdkInitialized) return
        Logger.v(TAG, "Iterable, syncEmbeddedMessages()")
        getEmbeddedManager().syncMessages()
    }

    fun syncIamMessages() {
        if (!sdkInitialized) return
        Logger.v(TAG, "Iterable, syncIamMessages()")
        inAppHandler.syncInAppMessages()
    }

    fun syncUserProfile() {
        if (!sdkInitialized) return
        Logger.v(
            TAG,
            "Iterable, syncUserProfile(), userId=${getIterableApi().userId}, derivedUserId=${getDerivedUserId()}"
        )
        // Update UserId
        getIterableApi().setUserId(getDerivedUserId(), { jsonObject ->
            Logger.v(TAG, "Iterable, successHandler, jsonObject=$jsonObject")
        }, { msg, jsonObject ->
            Logger.e(TAG, "Iterable, failureHandler, msg=$msg, jsonObject=$jsonObject")
            EventLog.Builder().apply {
                setMessage("Iterable SDK Error")
                setModule(LogModules.ITERABLE)
                setErrorMessage(msg)
                set("failure_data", jsonObject)
                setForceUpload()
            }.run {
                if (AppContextUtils.isConnectingOrConnected()) {
                    RemoteLog.e(AppContextUtils.appContext, build())
                }
            }
        })
        // Update user fields
        userFields.syncUserFields()
        // Update device fields
        userFields.syncDeviceAttributes()
    }

    fun syncDeviceAttributes() {
        if (!sdkInitialized) return
        Logger.v(TAG, "Iterable, syncDeviceAttributes()")
        // Update device fields
        userFields.syncDeviceAttributes()
    }

    fun onPrivacyConsentStateChanged(consent: Boolean) {
        Logger.v(TAG, "Iterable, onPrivacyConsentStateChanged(), consent=$consent")
        if (getIterableApi().userId != getDerivedUserId()) {
            syncUserProfile()
        }
    }

    fun pauseIamMessages() {
        if (!sdkInitialized) return
        if (iamAutomationPaused == true) return
        Logger.v(TAG, "Iterable, InAppMessage, pauseIamMessages()")
        getIamManager().setAutoDisplayPaused(true)
        iamAutomationPaused = true
    }

    fun resumeIamMessages() {
        if (!sdkInitialized) return
        if (iamAutomationPaused == false || !hasIamMessages()) return
        Logger.v(TAG, "Iterable, InAppMessage, resumeIamMessages()")
        getIamManager().setAutoDisplayPaused(false)
        iamAutomationPaused = false
    }

    fun hasIamMessages(): Boolean {
        if (!sdkInitialized) return false
        Logger.v(
            TAG,
            "Iterable, InAppMessage, hasIAMessages=${getIamManager().messages.isNotEmpty()}"
        )
        return getIamManager().messages.isNotEmpty()
    }

    fun getEmbeddedMessagesState(placementId: Long?): StateFlow<List<IterableEmbeddedMessage>> {
        if (!sdkInitialized) return MutableStateFlow(listOf())
        return embeddedUpdateHandler.messagesMapState[placementId] ?: MutableStateFlow(listOf())
    }

    fun getAnalyticsHandler(): IterableAnalyticsHandler? {
        if (!sdkInitialized) return null
        return analyticsHandler
    }

    fun updateEmbeddedMessagesMap() {
        if (!sdkInitialized) return
        embeddedUpdateHandler.updateMessagesMap()
    }

    fun isInAppMessageDisplayed(): Boolean {
        if (!sdkInitialized) return false
        return inAppHandler.isInAppMessageDisplayed()
    }

    fun getInAppMessageState(type: IamMessageType): StateFlow<List<IterableAppInAppHandler.IamMessage>> {
        if (!sdkInitialized) return MutableStateFlow(listOf())
        return inAppHandler.inAppMessagesFlow[type] ?: MutableStateFlow(listOf())
    }

    fun removeInAppMessage(messageId: String) {
        if (!sdkInitialized) return
        inAppHandler.removeInAppMessage(messageId)
    }

    fun sendEvent(eventName: String, dataFields: Map<String, Any>?) {
        if (!sdkInitialized || dataFields.isNullOrEmpty()) return
        try {
            val type =
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            val adapter = moshi.adapter<Map<String, Any>>(type)
            val jsonString = adapter.toJson(dataFields)
            if (!jsonString.isNullOrEmpty()) {
                val dataFieldObject = JSONObject(jsonString)
                getIterableApi().track(eventName, dataFieldObject)
            } else {
                Logger.v(
                    TAG,
                    "Iterable, SendEvent, failed to convert dataFields to JSONObject, eventName=$eventName, dataFields=$dataFields, error=Json string is null or empty"
                )
            }
        } catch (e: RuntimeException) {
            Logger.v(
                TAG,
                "Iterable, SendEvent, failed to convert dataFields to JSONObject, eventName=$eventName, dataFields=$dataFields, error=${e.message}"
            )
        }
    }

    fun getSupportedBlockerConfigVersion() = PaywallConstants.SUPPORTED_CONFIG_VERSION

    companion object {
        private const val TAG = "IterableSdk"
        private val allowedProtocols = arrayOf("washpost")

        private fun isSandboxEnv(): Boolean =
            ConfigOverride.ITERABLE_SANDBOX in ConfigManager.getInstance().state.value?.overrides.orEmpty()
    }
}
