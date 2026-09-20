package com.washingtonpost.android.config.domain.manager

import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.data.datasources.local.ConfigLocalDataSource
import com.washingtonpost.android.config.data.datasources.local.ConfigPrefsDataSource
import com.washingtonpost.android.config.data.datasources.remote.ConfigRemoteDataSource
import com.washingtonpost.android.config.data.repositories.ConfigRepositoryImpl
import com.washingtonpost.android.config.domain.models.ConfigProvider
import com.washingtonpost.android.config.domain.models.config.Config
import com.washingtonpost.android.config.domain.models.ConfigOverride
import com.washingtonpost.android.config.domain.models.Source
import com.washingtonpost.android.config.domain.models.StoreType
import com.washingtonpost.android.config.domain.repositories.ConfigRepository
import com.washingtonpost.android.config.utils.asObservable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.IllegalStateException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.system.exitProcess

class ConfigManager private constructor(
    private val configProvider: ConfigProvider,
    private val repository: ConfigRepository,
    val scope: CoroutineScope,
    private val isDebugBuild: Boolean,
) {
    private val defaultDebugOverrides = listOf(
        when (configProvider.storeType) {
            StoreType.AMAZON -> ConfigOverride.LOGGER_DEV_AMAZON
            else -> ConfigOverride.LOGGER_DEV_PLAYSTORE
        }
    )
    private val initializationLatch = CountDownLatch(1)
    private val initExecutor = Executors.newSingleThreadExecutor()
    private suspend fun getInitialState(): ConfigManagerState {
        val overrides = if (isDebugBuild) {
            repository.getConfigOverrides().orEmpty() + defaultDebugOverrides
        } else {
            emptyList()
        }
        return ConfigManagerState(
            isUpdatingEnv = false,
            overrides = overrides,
            config = repository.getConfig(
                overrides = overrides,
                source = Source.CACHE,
            )
        )
    }

    private val _state: MutableStateFlow<ConfigManagerState?> = MutableStateFlow(null)
    val state: StateFlow<ConfigManagerState?> = _state.asStateFlow()

    val configObs = state.mapNotNull { it?.config }.distinctUntilChanged().asObservable(scope)
    val config: Config
        get() {
            val config = _state.value?.config
            if (config != null) return config

            //  Try (lazy) config initialization
            try {
                initializationLatch.await()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                Logger.d(TAG, "Waiting for config interrupted ${e.message}")
            }
            val newConfig = _state.value?.config
            checkConfigNotNull(
                newConfig,
                configProvider::remoteLogError,
                IllegalStateException("Config should not be null after awaiting for initialization")
            )
            return newConfig
        }

    val isProdSignIn: Boolean get() = _state.value?.overrides?.contains(ConfigOverride.PAYWALL_STAGE) == false

    init {
        initExecutor.execute {
            try {
                //  Launch blocking config update from cache
                val initialState = runBlocking { getInitialState() }
                _state.update { initialState }

                //  Launch non-blocking config update from remote
                updateConfig(Source.REMOTE)
            } catch (e: Exception) {
                //  Should not happen, but if it happens, exit immediately
                checkConfigNotNull(_state.value?.config, configProvider::remoteLogError, e)
            } finally {
                initializationLatch.countDown()     //  Signals that the cache is ready
                initExecutor.shutdown()
            }
        }
    }

    /**
     * Remove any previous override and only apply [overrides] to the prod config.json file.
     */
    fun setOverrides(overrides: List<ConfigOverride>) {
        if (!isDebugBuild) return

        val sortedOverrides = (overrides + defaultDebugOverrides)
            .distinctBy { it.group }    //  Keep only one item per group
            .sortedBy { it.ordinal }    //  Sort the overrides, so that cache works better

        if (state.value?.overrides == sortedOverrides) return

        _state.update { it?.copy(isUpdatingEnv = true) }
        scope.launch {
            repository.saveConfigOverrides(sortedOverrides)
            val config = repository.getConfig(sortedOverrides)
            _state.update {
                it?.copy(
                    isUpdatingEnv = false,
                    overrides = sortedOverrides,
                    config = config,
                ) ?: ConfigManagerState(false, sortedOverrides, config)
            }
        }
    }

    fun updateConfig(source: Source = Source.DEFAULT) {
        scope.launch {
            val overrides = _state.value?.overrides.orEmpty()
            val config = repository.getConfig(overrides, source)
            _state.update {
                it?.copy(
                    overrides = overrides,
                    config = config,
                ) ?: ConfigManagerState(false, overrides, config)
            }
        }
    }

    companion object {
        private const val TAG = "ConfigManager"

        @Volatile
        private var instance: ConfigManager? = null
        private var configProvider: ConfigProvider? = null
        var isInitialized: Boolean = false

        @Synchronized
        fun init(configProvider: ConfigProvider) {
            if (isInitialized) return
            this.configProvider = configProvider
            this.isInitialized = true
        }

        fun getInstance(): ConfigManager {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val configProvider = this.configProvider
                        ?: throw Exception("Config provider should be set in Application.onCreate")

                    val configPrefsDataSource = ConfigPrefsDataSource(configProvider)
                    val configLocalDataSource = ConfigLocalDataSource(
                        configProvider.applicationContext,
                        configProvider,
                    )
                    val configRemoteDataSource = ConfigRemoteDataSource(
                        configProvider.applicationContext,
                        configProvider
                    )
                    val repository = ConfigRepositoryImpl(
                        configPrefsDataSource = configPrefsDataSource,
                        configLocalDataSource = configLocalDataSource,
                        configRemoteDataSource = configRemoteDataSource,
                        configProvider = configProvider,
                    )
                    ConfigManager(
                        configProvider = configProvider,
                        repository = repository,
                        isDebugBuild = configProvider.isDebugBuild,
                        scope = configProvider.configScope,
                    ).also {
                        instance = it
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
private fun checkConfigNotNull(
    value: Config?,
    remoteLogError: ((EventLog) -> Unit)? = null,
    exception: Exception? = null,
): Config {
    contract {
        returns() implies (value != null)
    }

    if (value == null) {
        Logger.wtf("checkConfigNotNull", "Config is null: $exception", exception)
        remoteLogError?.invoke(
            EventLog.Builder()
                .setModule(LogModules.CONFIG)
                .setMessage("Fatal error: Config is null")
                .setErrorMessage(exception?.message)
                .build()
        )
        exitProcess(0)
    } else {
        return value
    }
}