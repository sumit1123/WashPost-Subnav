package com.washingtonpost.android.config.data.datasources.remote

import android.content.Context
import android.util.Log
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.retrofit.DefaultHeadersInterceptor
import com.wapo.android.commons.util.AppContextUtils.isConnectingOrConnected
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.R
import com.washingtonpost.android.config.data.datasources.dto.RawVersionConfig
import com.washingtonpost.android.config.data.datasources.dto.config.RawConfig
import com.washingtonpost.android.config.data.datasources.utils.ConfigMoshiAdapters
import com.washingtonpost.android.config.domain.models.ConfigProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ConfigRemoteDataSource(
    applicationContext: Context,
    private val configProvider: ConfigProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val configUrl = applicationContext.getString(R.string.configRemoteLocation)
    private val versionConfigUrl = applicationContext.getString(R.string.configVersionUrl)
    private val rawConfigAdapter = ConfigMoshiAdapters.rawConfigAdapter
    private val rawVersionConfigAdapter = ConfigMoshiAdapters.rawVersionConfigAdapter
    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(DefaultHeadersInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getConfig(): RawConfig? {
        return try {
            if (configUrl.isBlank()) return null
            val json = download(configUrl)
            json?.let { rawConfigAdapter.fromJson(it) }
        } catch (e: Exception) {
            Logger.e(TAG, Log.getStackTraceString(e))
            null
        }
    }

    suspend fun getVersionConfig(): RawVersionConfig? {
        return try {
            val json = download(versionConfigUrl)
            json?.let { rawVersionConfigAdapter.fromJson(it) }
        } catch (e: Exception) {
            Logger.e(TAG, Log.getStackTraceString(e))
            null
        }
    }

    private suspend fun download(url: String, retries: Int = 3): String? =
        withContext(ioDispatcher) {
            val request = Request.Builder().url(url).get().build()
            repeat(retries) { attempt ->
                try {
                    val response = client.newCall(request).execute()
                    return@withContext response.use {
                        if (!it.isSuccessful) throw IOException("HTTP ${it.code}")
                        it.body?.string()
                    }
                } catch (e: Exception) {

                    // Log non-network errors only
                    val isNetworkError = !isConnectingOrConnected()
                    if(!isNetworkError) {
                        configProvider.remoteLogError(
                            EventLog.Builder()
                                .setModule(LogModules.CONFIG)
                                .setMessage("Error fetching configs from $url")
                                .setErrorMessage(e.message)
                                .build()
                        )
                    }
                    if (attempt == (retries - 1)) throw e    // rethrow on last attempt
                }
            }
            null
        }

    companion object {
        private const val TAG = "ConfigRemoteDataSource"
    }
}