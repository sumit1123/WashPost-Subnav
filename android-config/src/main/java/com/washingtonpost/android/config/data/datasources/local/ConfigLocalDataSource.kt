package com.washingtonpost.android.config.data.datasources.local

import android.content.Context
import androidx.annotation.RawRes
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.R
import com.washingtonpost.android.config.data.datasources.dto.config.RawConfig
import com.washingtonpost.android.config.data.datasources.utils.ConfigMoshiAdapters
import com.washingtonpost.android.config.domain.models.ConfigOverride
import com.washingtonpost.android.config.domain.models.ConfigProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class ConfigLocalDataSource(
    private val applicationContext: Context,
    private val configProvider: ConfigProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val filesHelper: FilesHelper = FilesHelper(applicationContext, ioDispatcher),
) {
    private val rawConfigAdapter = ConfigMoshiAdapters.rawConfigAdapter
    private val mapAdapter = ConfigMoshiAdapters.mapAdapter

    suspend fun saveConfig(config: RawConfig, filename: String = CONFIG_FILENAME): Boolean {
        return try {
            val json = rawConfigAdapter.toJson(config)
            filesHelper.saveToFile(json, filename)
        } catch (e: Exception) {
            Logger.e(TAG, "Error saving config $config to file $filename, error=$e")
            false
        }
    }

    suspend fun clearConfig(filename: String = CONFIG_FILENAME): Boolean {
        return filesHelper.clearFile(filename)
    }

    suspend fun getConfig(
        filename: String = CONFIG_FILENAME,
        @RawRes resource: Int = R.raw.config,
    ): RawConfig? {
        var src = ""
        return try {
            var result: RawConfig? = null

            //  Try first to fetch config from file
            src = "config file $filename"
            val config = filesHelper.readFromFile(filename)
            if (config != null) {
                val configFromFile = rawConfigAdapter.fromJson(config)
                result = configFromFile
            }

            //  Fetch config from resources and replace the result if the resource config has a higher version
            src = "config resource $resource"
            val configRes = filesHelper.readFromResources(resource)
            if (configRes != null) {
                val configFromRes = rawConfigAdapter.fromJson(configRes)
                if ((configFromRes?.version ?: -1) > (result?.version ?: -1)) {
                    result = configFromRes
                }
            }

            if (result == null) {
                configProvider.remoteLogError(
                    EventLog.Builder()
                        .setModule(LogModules.CONFIG)
                        .setErrorMessage("Config not found in local files")
                        .build()
                )
                Logger.w(TAG, "No configuration files/resources found")
            }
            result
        } catch (e: Exception) {
            configProvider.remoteLogError(
                EventLog.Builder()
                    .setModule(LogModules.CONFIG)
                    .setMessage("Error fetching local config from $src")
                    .setErrorMessage(e.message)
                    .build()
            )
            Logger.e(TAG, "Error fetching config from $src")
            null
        }
    }

    suspend fun getConfigOverride(override: ConfigOverride): Map<String, Any?>? {
        return try {
            val json = filesHelper.readFromAssets(override.filePath)
            json?.let { mapAdapter.fromJson(it) }
        } catch (e: Exception) {
            Logger.e(TAG, "Error fetching config overrides ${override.filename}, error: $e")
            null
        }
    }

    companion object {
        private const val TAG = "ConfigLocalDataSource"
        private const val CONFIG_FILENAME = "config.json"
    }
}
