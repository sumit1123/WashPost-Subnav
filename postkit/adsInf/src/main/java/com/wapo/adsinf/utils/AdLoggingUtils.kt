package com.wapo.adsinf.utils

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.wapo.adsinf.AdManager
import com.wapo.adsinf.models.AdError
import com.wapo.adsinf.models.AdLoadContext
import com.wapo.adsinf.models.AdRequest
import com.wapo.adsinf.models.AdLoadSession
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.commons.logs.Sampling
import com.wapo.android.commons.util.Logger
import com.wapo.android.remotelog.logger.RemoteLog
import com.washingtonpost.android.config.domain.models.config.banners.AdBidSource
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderType
import com.washingtonpost.android.config.domain.models.config.banners.AdSdk

object AdLoggingUtils {
    private const val TAG = "AdLoggingUtils"
    private val adProvider get() = AdManager.getInstance().adProvider

    fun remoteLogSdkSuccess(
        adLoadSession: AdLoadSession,
        sdk: AdSdk,
    ) {
        val loadId = adLoadSession.attempts.lastOrNull()?.id.orEmpty()
        val eventLog = EventLog.Builder()
            .setMessage("BannerAd Response Metrics")
            .setModule(LogModules.ADS)
            .setAdContext(adLoadSession, loadId, sdk)
            .set("request_tag", sdk.name.lowercase())
            .setSampling(Sampling.One)
            .build()
        RemoteLog.d(adProvider.applicationContext, eventLog)
        Logger.d(TAG, "remoteLogSdkSuccess: ${eventLog.dataString}")
    }

    fun remoteLogSdkFailure(
        adError: AdError,
        adLoadSession: AdLoadSession,
        sdk: AdSdk,
    ) {
        val loadId = adLoadSession.attempts.lastOrNull()?.id.orEmpty()
        val eventLog = EventLog.Builder()
            .setMessage("BannerAd FailedToLoad Error")
            .setModule(LogModules.ADS)
            .setAdContext(adLoadSession, loadId, sdk)
            .set("request_tag", sdk.name.lowercase())
            .setErrorCode(adError.type.errorCode)
            .setErrorMessage(adError.message)
            .setSampling(Sampling.One)
            .build()
        RemoteLog.e(adProvider.applicationContext, eventLog)
        Logger.d(TAG, "remoteLogSdkFailure: ${eventLog.dataString}")
    }

    fun remoteLogFailure(
        adError: AdError,
        extras: Map<String, String> = emptyMap(),
    ) {
        val eventLog = EventLog.Builder()
            .setMessage("BannerAd FailedToLoad Error")
            .setModule(LogModules.ADS)
            .setErrorCode(adError.type.errorCode)
            .setErrorMessage(adError.message)
            .apply {
                extras.forEach { (key, value) ->
                    set(key, value)
                }
            }
            .build()
        RemoteLog.e(adProvider.applicationContext, eventLog)
        Logger.d(TAG, "remoteLogFailure: ${eventLog.dataString}")
    }

    fun EventLog.Builder.setAdContext(
        adLoadSession: AdLoadSession,
        loadId: String,
        sdk: AdSdk,
    ): EventLog.Builder {
        return this.apply {
            //  Find ad context
            val adContext = adLoadSession.attempts.lastOrNull { it.id == loadId }

            //  Log request details
            set("hashcode", adLoadSession.id)
            adContext?.adRequest?.let {
                set("loader", it.sdkName.name)
                set("bid_sources", it.bidSources?.joinToString().orEmpty())
                set("section", it.section)
                set("ad_unit_id", it.adUnitId)
            }

            //  Log SDK response details
            set("sdk_name", sdk.name)
            val response = when (sdk) {
                is AdBidSource -> adContext?.bidResponses?.lastOrNull { it.bidSource == sdk }
                is AdLoaderType -> adContext?.adResponse?.takeIf { it.sdkName == sdk }
            }
            val isSuccess = response is AdLoadContext.BidResponse.Success
                    || response is AdLoadContext.AdResponse.Success
            response?.let {
                set("time_ms", it.responseTime)

                if (it is AdLoadContext.AdResponse.Success) {
                    set("ad_load_index", it.loadIndex)
                    set("ad_impression_index", it.impressionIndex)
                    set("ad_size", it.adSize?.let { "${it.width}x${it.height}" })
                    set("winning_bid_source", it.winningBidSource)
                }

                if (it is AdLoadContext.BidResponse.Failure) {
                    set("sdk_error_code", it.sdkErrorCode)
                    set("sdk_error_message", it.sdkErrorMessage)
                }
                if (it is AdLoadContext.AdResponse.Failure) {
                    set("sdk_error_code", it.sdkErrorCode)
                    set("sdk_error_message", it.sdkErrorMessage)
                }
            }

            //  Log response times
            var totalTime = 0L
            adLoadSession.attempts.forEach {
                val loader = it.adRequest?.sdkName?.name?.lowercase().orEmpty()
                it.bidResponses?.forEach {
                    totalTime += it.responseTime
                    set("${loader}_${it.bidSource.name.lowercase()}_time_ms", it.responseTime)
                }
                it.adResponse?.let {
                    totalTime += it.responseTime
                    set("${it.sdkName.name.lowercase()}_time_ms", it.responseTime)
                }
            }
            val adLoadChain = adLoadSession.adLoadChain
            val shouldLogTotalTime = sdk is AdLoaderType &&
                (isSuccess || sdk == adLoadChain.lastOrNull()?.loaderType)
            if (shouldLogTotalTime) {
                set("total_time_ms", totalTime)
            }
        }
    }

    fun AdError.toDebugString(): String {
        return "AdError(errorType=$type, errorMessage=$message)"
    }

    fun AdRequest.toDebugString(): String {
        val request = when (this) {
            is AdRequest.Google -> request
            is AdRequest.Nimbus -> request
        }
        return "AdRequest(request=${request.toDebugString()}, adLoaderConfig=$adLoaderConfig)"
    }

    fun Any.toDebugString(): String? {
        return if (AdManager.getInstance().adProvider.isDebugBuild) {
            "${javaClass.name}(${getJsonForLogs()})"
        } else {
            this.toString()
        }
    }

    private fun Any.getJsonForLogs(): String {
        if (adProvider.isDebugBuild) {
            return try {
                val classesToExcludeFromSerialization = setOf("ApsInterceptor")
                val gson = GsonBuilder()
                    .registerTypeAdapterFactory(object : TypeAdapterFactory {
                        override fun <T : Any?> create(
                            gson: Gson?,
                            type: TypeToken<T?>?
                        ): TypeAdapter<T?>? {
                            val shouldExclude = classesToExcludeFromSerialization.any {
                                type?.rawType?.name?.endsWith(it) == true
                            }
                            return if (shouldExclude) {
                                object : TypeAdapter<T?>() {
                                    override fun write(out: JsonWriter?, value: T?) {
                                        out?.value(type.toString())
                                    }

                                    override fun read(`in`: JsonReader?): T? = null
                                }
                            } else {
                                null
                            }
                        }
                    })
                    .addSerializationExclusionStrategy(object : ExclusionStrategy {
                        override fun shouldSkipField(f: FieldAttributes?): Boolean {
                            return f?.name in setOf(
                                "adConfig",
                                "adLoaderConfig"
                            ) && f?.declaringClass == AdRequest::class.java
                        }

                        override fun shouldSkipClass(clazz: Class<*>?): Boolean = false

                    })
                    .serializeNulls()
                    .create()
                gson.toJson(this)
            } catch (e: Exception) {
                Logger.e(
                    "AdLoggingUtils",
                    "Error getting pretty Json for ${this.javaClass.simpleName}, error=$e, message=${e.message}",
                    e
                )
                this.toString()
            }
        } else {
            return ""
        }
    }
}
