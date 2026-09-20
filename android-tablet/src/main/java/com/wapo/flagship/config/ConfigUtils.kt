package com.wapo.flagship.config

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.remotelog.logger.RemoteLog
import com.wapo.flagship.AppContext.GENERAL_PREFERENCES
import com.wapo.flagship.DualLogUploader
import com.wapo.flagship.FlagshipApplication
import com.wapo.flagship.RemoteLogProviderImpl
import com.wapo.flagship.Utils
import com.wapo.flagship.data.ArchiveManager
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.ConfigProvider
import com.washingtonpost.android.config.domain.models.StoreType
import com.washingtonpost.android.config.domain.models.config.Config
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.wapo.adsinf.AdManager
import com.wapo.android.commons.config.Constants
import com.wapo.android.commons.logs.EventLog
import com.wapo.flagship.SecureAppData
import com.wapo.flagship.kmp.core.DependencyRegistry
import com.washingtonpost.android.BuildConfig
import com.washingtonpost.android.config.domain.manager.ConfigManagerState
import com.washingtonpost.android.paywall.PaywallService
import com.washingtonpost.customnav.repo.CustomNavRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlin.reflect.full.memberProperties
import kotlin.system.exitProcess

object ConfigUtils {
    private val configScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initConfig(
        applicationContext: Context,
        kmpRegistry: DependencyRegistry,
    ) {
        ConfigManager.init(getConfigProvider(applicationContext))
        ConfigManager.getInstance().registerCallbacks(applicationContext, kmpRegistry)
    }

    private fun getConfigProvider(appContext: Context) = object : ConfigProvider {
        override val configScope: CoroutineScope = ConfigUtils.configScope
        override val applicationContext: Context = appContext
        override val generalPrefs: SharedPreferences = appContext
            .getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE)

        override val storeType: StoreType = when {
            Utils.isProductFlavorAmazon() || Utils.isAmazonBuild() -> StoreType.AMAZON
            else -> StoreType.GOOGLE
        }

        override val appVersionCode: Int
            get() = com.wapo.android.commons.util.Utils.getAppVersionCode(applicationContext)
        override val deviceUniqueId: String = DeviceUtils.getUniqueDeviceId(appContext)
        override val deviceSerialId: String = DeviceUtils.getDeviceSerialId(appContext)
        override val isTablet: Boolean = DeviceUtils.isTablet(applicationContext)
        override val isDebugBuild: Boolean = BuildConfig.DEBUG ||
                BuildConfig.BUILD_TYPE in setOf("debug", "beta")
        override val archiveDirectory: String = ArchiveManager.ARCHIVE_DIRECTORY
        override val canStoreRemoteLogs: Boolean = FlagshipApplication.CAN_STORE_REMOTE_LOGS
        override val currentVersionCodePrefKey: String = Constants.PREF_KEY_CURRENT_VERSION_CODE
        override val packageName: String get() = appContext.packageName

        override fun remoteLogError(eventLog: EventLog) {
            RemoteLog.e(applicationContext, eventLog)
        }

        override fun decryptSecureData(value: String): String? =
            SecureAppData.decryptSecureData(value)
    }

    private fun ConfigManager.registerCallbacks(
        appContext: Context,
        kmpRegistry: DependencyRegistry,
    ) {
        scope.launch {
            state.mapNotNull { it?.config }.distinctUntilChanged()
                .collectLatest { onConfigUpdated(it, kmpRegistry) }
        }
        scope.launch {
            var lastConfig = state.value
            state
                .filterNotNull()
                .distinctUntilChangedBy { it.overrides }
                .collectLatest {
                    onEnvUpdated(
                        prevConfigState = lastConfig?.copy(), // Important: pass a copy of lastConfig and not the reference to lastConfig
                        configState = it,
                        appContext = appContext,
                    )
                    lastConfig = it
                }
        }
    }

    private fun onConfigUpdated(
        config: Config,
        kmpRegistry: DependencyRegistry,
    ) {
        // Install the KMP logger route before legacy Android logging is
        // initialized. Otherwise early config/Ads logs can reach LogFileWriter
        // during the handoff even when the KMP logger is configured.
        kmpRegistry.applyConfig(config.kmpConfig)
        RemoteLog.initialize(
            config.loggerConfig,
            DualLogUploader(),
            RemoteLogProviderImpl()
        )
    }

    private fun onEnvUpdated(
        prevConfigState: ConfigManagerState?,
        configState: ConfigManagerState,
        appContext: Context,
    ) {
        val generalPrefs =
            appContext.getSharedPreferences(GENERAL_PREFERENCES, Context.MODE_PRIVATE)

        configScope.launch {
            val needsRestart = prevConfigState != null && setOf(
                updatePaywallEnv(prevConfig = prevConfigState.config, config = configState.config),
                updateFeedsEnv(prevConfig = prevConfigState.config, config = configState.config),
                updateSiteServiceEnv(
                    prevConfig = prevConfigState.config,
                    config = configState.config,
                    generalPrefs = generalPrefs,
                    appContext = appContext
                ),
                updateIterableEnv(prevConfig = prevConfigState.config, config = configState.config),
                updateAdsConfig(prevConfig = prevConfigState.config, config = configState.config),
            ).any { it.needsRestart }
            if (needsRestart) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "App will restart to apply changes", Toast.LENGTH_SHORT)
                        .show()
                    delay(1000L)
                    restartApp(appContext)
                }
            }
        }
    }

    private fun updatePaywallEnv(prevConfig: Config, config: Config): UpdateEnvResult {
        val needsUpdate = !equalsIgnoring(
            prevConfig.paywallConfig,
            config.paywallConfig,
            "oAuthConfigStub"
        ) && !equalsIgnoring(
            prevConfig.paywallConfig.oAuthConfigStub,
            config.paywallConfig.oAuthConfigStub,
            "authorizationState", "configHash"
        )
        if (needsUpdate) {
            if (PaywallService.getInstance().isWpUserLoggedIn) {
                PaywallService.getInstance().logOutCurrentUser()
            }
            val tetroBaseUrl = config.paywallConfig.tetroBaseUrl
            val meteringProxyBaseUrl = config.paywallConfig.meteringProxyBaseUrl
            PaywallService.getInstance().tetroManager.resetTetroNetwork(
                tetroBaseUrl,
                meteringProxyBaseUrl
            )
        }
        return UpdateEnvResult(needsRestart = needsUpdate)
    }

    private suspend fun updateFeedsEnv(prevConfig: Config, config: Config): UpdateEnvResult {
        val needsUpdate =
            prevConfig.singleNativeContentTemplate != config.singleNativeContentTemplate
        if (needsUpdate) {
            // Feeds: Clear articles db and exit the app.
            withContext(Dispatchers.IO) {
                FlagshipApplication.getInstance().articleDatabase.articlesDao()
                    .cleanUp(Long.MAX_VALUE)
            }
        }
        return UpdateEnvResult(needsRestart = needsUpdate)
    }

    private suspend fun updateSiteServiceEnv(
        prevConfig: Config,
        config: Config,
        appContext: Context,
        generalPrefs: SharedPreferences
    ): UpdateEnvResult {
        val needsUpdate = prevConfig.siteServiceConfig != config.siteServiceConfig
        if (needsUpdate) {
            // Site Service: Clear configs and exit the app.
            withContext(Dispatchers.IO) {
                // keys are from CustomNavRepository.kt file.
                generalPrefs.edit { putString(CustomNavRepository.CUSTOM_SECTIONS_KEY, null) }
                FlagshipApplication.getInstance().contentManager.wapoConfigManager.let {
                    it.deleteLocalConfigs(appContext)
                    it.loadConfigs(appContext)
                }
            }
        }
        return UpdateEnvResult(needsRestart = needsUpdate)
    }

    private suspend fun updateIterableEnv(prevConfig: Config, config: Config): UpdateEnvResult {
        val needsUpdate = prevConfig.iterableConfig.banner != config.iterableConfig.banner
        return UpdateEnvResult(needsRestart = needsUpdate)
    }

    private fun updateAdsConfig(prevConfig: Config, config: Config): UpdateEnvResult {
        if (prevConfig.adsConfig.bannersConfig != config.adsConfig.bannersConfig) {
            AdManager.getInstance().onConfigUpdated()
        }
        return UpdateEnvResult(false)
    }

    private fun restartApp(context: Context): UpdateEnvResult {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent)

        exitProcess(0)
    }

    private data class UpdateEnvResult(
        val needsRestart: Boolean,
    )

    private inline fun <reified T : Any> equalsIgnoring(
        first: T,
        second: T,
        vararg ignoredProperties: String
    ): Boolean {
        val ignored = ignoredProperties.toSet()
        return T::class.memberProperties
            .filter { it.name !in ignored }
            .all { prop ->
                prop.get(first) == prop.get(second)
            }
    }

    private const val TAG = "ConfigUtils"
}
