package com.washingtonpost.android.config.data.repositories

import com.washingtonpost.android.config.data.datasources.dto.config.RawConfig
import com.washingtonpost.android.config.data.datasources.local.ConfigLocalDataSource
import com.washingtonpost.android.config.data.datasources.local.ConfigPrefsDataSource
import com.washingtonpost.android.config.data.datasources.remote.ConfigRemoteDataSource
import com.washingtonpost.android.config.data.datasources.utils.ConfigCryptoHelper
import com.washingtonpost.android.config.domain.models.ConfigProvider
import com.washingtonpost.android.config.data.datasources.utils.MapConfigParams
import com.washingtonpost.android.config.domain.models.config.Config
import com.washingtonpost.android.config.domain.models.ConfigOverride
import com.washingtonpost.android.config.domain.models.Source
import com.washingtonpost.android.config.domain.repositories.ConfigRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConfigRepositoryImpl(
    private val configPrefsDataSource: ConfigPrefsDataSource,
    private val configLocalDataSource: ConfigLocalDataSource,
    private val configRemoteDataSource: ConfigRemoteDataSource,
    private val configProvider: ConfigProvider,
    private val configCryptoHelper: ConfigCryptoHelper = ConfigCryptoHelper(configProvider.resources),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ConfigRepository {

    private var _cache: ConfigCache? = null

    override suspend fun saveConfigOverrides(overrides: List<ConfigOverride>): Boolean =
        withContext(ioDispatcher) {
            configPrefsDataSource.setConfigOverrides(overrides)
        }

    override suspend fun getConfigOverrides(): List<ConfigOverride>? = withContext(ioDispatcher) {
        configPrefsDataSource.getConfigOverrides()
    }

    override suspend fun getConfig(
        overrides: List<ConfigOverride>,
        source: Source,
    ): Config = withContext(ioDispatcher) {
        val cache = _cache
        if (source == Source.CACHE && cache != null && cache.overrides == overrides) {
            return@withContext cache.config
        }

        if (source == Source.REMOTE || needsUpdate()) {
            _cache = null
            val configUpdated = updateConfigWithRemote()
            if (configUpdated) {
                val currentVersionCode = configProvider.appVersionCode
                configPrefsDataSource.setCurrentVersionCode(currentVersionCode)
            }
        }

        //  If cache is still valid, return it
        _cache?.let { if (it.overrides == overrides) return@withContext it.config }

        //  Get base config and apply overrides
        val baseConfig = configLocalDataSource.getConfig() ?: RawConfig()
        val overridesAsJson = overrides
            .mapNotNull { configLocalDataSource.getConfigOverride(it) }
        val configWithOverrides = baseConfig.override(overridesAsJson)

        //  Map dto models to domain models
        val params = MapConfigParams(
            cryptoHelper = configCryptoHelper,
            configProvider = configProvider,
        )
        configWithOverrides.mapToDomain(params)
            .also { _cache = ConfigCache(config = it, overrides = overrides) }
    }

    private suspend fun updateConfigWithRemote(): Boolean = withContext(ioDispatcher) {
        val remoteRawConfig = configRemoteDataSource.getConfig()
        if (remoteRawConfig != null) {
            val fileDeleted = configLocalDataSource.clearConfig()
            if (!fileDeleted) return@withContext false

            val fileSaved = configLocalDataSource.saveConfig(remoteRawConfig)
            return@withContext fileSaved
        }
        false
    }

    private fun needsUpdate(): Boolean {
        val oldVersionCode = configPrefsDataSource.getCurrentVersionCode()
        val currentVersionCode = configProvider.appVersionCode
        return oldVersionCode != currentVersionCode
    }

    data class ConfigCache(
        val config: Config,
        val overrides: List<ConfigOverride>,
    )
}