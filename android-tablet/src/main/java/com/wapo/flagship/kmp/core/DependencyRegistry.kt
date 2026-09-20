package com.wapo.flagship.kmp.core

import android.os.Build
import android.os.Handler
import android.os.Looper
import com.wapo.android.commons.config.sec.helper.WapoSecDataProvider
import com.wapo.android.commons.domain.AppContextUtilsRepo
import com.wapo.android.commons.util.DeviceUtils
import com.wapo.android.data.repository.KMPRemoteLogRepoImpl
import com.wapo.android.data.repository.RemoteLogRepoImpl
import com.wapo.flagship.kmp.core.config.PlatformNetworkProviderImpl
import com.wapo.kmpshared.core.config.AppConfig
import com.wapo.kmpshared.core.config.KMPEnv
import com.wapo.kmpshared.core.di.AppDependencies
import com.wapo.kmpshared.core.di.DIFactory
import com.wapo.kmpshared.core.lifecycle.AppEnvironment
import com.wapo.kmpshared.core.lifecycle.DefaultAppLifecycle
import com.wapo.kmpshared.features.conversations.presentation.CommentsStore
import com.wapo.kmpshared.features.feedback.domain.FeedbackRepository
import com.wapo.kmpshared.logger.data.KMPLoggerRuntime
import com.wapo.kmpshared.util.KMPURL
import com.washingtonpost.android.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DependencyRegistry @Inject constructor(
    private val application: android.app.Application,
    private val platformNetworkProvider: PlatformNetworkProviderImpl,
    private val loggerBridge: KMPRemoteLogRepoImpl,
    private val legacyLogger: RemoteLogRepoImpl,
    private val appContextUtilsRepo: AppContextUtilsRepo,
    // Forces SecureAppDataModule to initialize before KMP config is applied.
    private val _secureAppDataProvider: WapoSecDataProvider,
) {
    @Volatile
    private var _dependencies: AppDependencies? = null
    private var lifecycleObserver: KMPLifecycleObserver? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val appEnvironment =
        AppEnvironment(
            env = currentKmpEnv(),
            appBuild = BuildConfig.VERSION_CODE.toString(),
            appVersion = BuildConfig.VERSION_NAME,
            device = Build.DEVICE,
            supportID = DeviceUtils.getUniqueDeviceId(application) ?: "unknown",
            osVersion = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString(),
        )

    init {
        KMPLoggerRuntime.init(application)
    }

    @Synchronized
    fun applyConfig(config: AppConfig?) {
        if (config == null) {
            val observer = lifecycleObserver
            lifecycleObserver = null
            mainHandler.post { observer?.close() }

            loggerBridge.bindKmpLogger(null)
            DIFactory.tearDown()
            _dependencies = null
            return
        }

        if (_dependencies != null) {
            DIFactory.apply(config)
        } else {
            initialize(config)
        }
    }

    private fun initialize(config: AppConfig) {
        val lifecycle = DefaultAppLifecycle(appEnvironment)
        DIFactory.setUp(
            lifecycle = lifecycle,
            network = platformNetworkProvider,
            config = config,
            logger = legacyLogger,
        )

        _dependencies = AppDependencies()
        bindKmpLogger()

        val observer = KMPLifecycleObserver(application, lifecycle)
        lifecycleObserver = observer
        mainHandler.post {
            if (lifecycleObserver === observer) {
                androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
            }
        }
    }

    private fun bindKmpLogger() {
        _dependencies?.logger?.let(loggerBridge::bindKmpLogger)
    }

    val feedbackRepository: FeedbackRepository?
        get() = _dependencies?.feedback

    fun makeCommentsStore(storyURL: KMPURL, storyID: String?): CommentsStore? {
        return _dependencies?.makeCommentsStore(storyURL, storyID)
    }

    private fun currentKmpEnv(): KMPEnv =
        when {
            appContextUtilsRepo.isBetaBuild() -> KMPEnv.Test
            appContextUtilsRepo.isDebuggableBuild() -> KMPEnv.Debug
            else -> KMPEnv.Prod
        }
}
