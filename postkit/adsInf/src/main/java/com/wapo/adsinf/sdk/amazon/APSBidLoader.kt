package com.wapo.adsinf.sdk.amazon

import android.content.Context
import com.amazon.device.ads.DTBAdCallback
import com.amazon.device.ads.DTBAdNetwork
import com.amazon.device.ads.DTBAdNetworkInfo
import com.amazon.device.ads.DTBAdRequest
import com.amazon.device.ads.DTBAdResponse
import com.amazon.device.ads.DTBAdSize
import com.wapo.adsinf.AdManager
import com.wapo.adsinf.models.AdConfig
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.wapo.adsinf.models.AdLoadContext
import com.wapo.adsinf.models.AdLoadSession
import com.wapo.adsinf.sdk.amazon.AmazonAdsUtils.toAdError
import com.wapo.adsinf.utils.AdLoggingUtils
import com.wapo.android.commons.logs.EventLog
import com.wapo.android.commons.logs.LogModules
import com.wapo.android.remotelog.logger.RemoteLog
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.banners.AdBidSource
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderType
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID
import kotlin.coroutines.resume

class APSBidLoader {
    private val adProvider get() = AdManager.getInstance().adProvider
    private val bannersConfig get() = ConfigManager.getInstance().config.adsConfig.bannersConfig
    private val dtbConfig get() = bannersConfig.dtb

    suspend fun fetchAPSBid(
        config: AdConfig,
        loaderType: AdLoaderType,
        adLoadSession: AdLoadSession,
    ): DTBAdResult = suspendCancellableCoroutine { continuation ->
        adLoadSession.updateLastAttempt {
            val adRequest = it.adRequest
            it.copy(
                adRequest = adRequest?.copy(
                    bidSources = adRequest.bidSources
                        ?.let { if (it.contains(AdBidSource.APS)) it else it + (AdBidSource.APS) }
                        ?: listOf(AdBidSource.APS),
                )
            )
        }

        val loader = DTBAdRequest(getDTBAdNetworkInfo(loaderType))
        val adSizes = getAdSizes(config.adDimensions)
        loader.setSizes(*adSizes.toTypedArray())
        val requestStartTime = System.currentTimeMillis()
        loader.loadAd(
            object : DTBAdCallback {
                override fun onFailure(err: com.amazon.device.ads.AdError) {
                    val responseTime = System.currentTimeMillis() - requestStartTime
                    adLoadSession.updateLastAttempt {
                        it.copy(
                            bidResponses = it.bidResponses.orEmpty().plus(
                                AdLoadContext.BidResponse.Failure(
                                    bidSource = AdBidSource.APS,
                                    responseTime = responseTime,
                                    sdkErrorCode = err.code.name,
                                    sdkErrorMessage = err.message,
                                )
                            )
                        )
                    }
                    AdLoggingUtils.remoteLogSdkFailure(
                        err.toAdError(),
                        adLoadSession,
                        AdBidSource.APS,
                    )
                    if (continuation.isActive) {
                        continuation.resume(DTBAdResult.Failure(err))
                    }
                }

                override fun onSuccess(dtbAdResponse: DTBAdResponse) {
                    val responseTime = System.currentTimeMillis() - requestStartTime
                    adLoadSession.updateLastAttempt {
                        it.copy(
                            bidResponses = it.bidResponses.orEmpty().plus(
                                AdLoadContext.BidResponse.Success(
                                    bidSource = AdBidSource.APS,
                                    responseTime = responseTime,
                                )
                            )
                        )
                    }
                    AdLoggingUtils.remoteLogSdkSuccess(adLoadSession, AdBidSource.APS)
                    if (continuation.isActive) {
                        continuation.resume(DTBAdResult.Success(dtbAdResponse))
                    }
                }
            }
        )
    }

    private fun getDTBAdNetworkInfo(loader: AdLoaderType): DTBAdNetworkInfo {
        val dtbAdNetwork = when (loader) {
            AdLoaderType.GOOGLE -> DTBAdNetwork.GOOGLE_AD_MANAGER
            AdLoaderType.NIMBUS -> DTBAdNetwork.NIMBUS
        }
        return DTBAdNetworkInfo(dtbAdNetwork)
    }

    private fun getAdSizes(adDimensions: List<AdDimension>): List<DTBAdSize> {
        val adSizeList = mutableListOf<DTBAdSize>()
        val supportedAmazonDimensionsMap = dtbConfig.slots
        val adDimensions = adDimensions
            .filter { supportedAmazonDimensionsMap.keys.contains(it) }
            .ifEmpty { supportedAmazonDimensionsMap.keys }
        for (dimension in adDimensions) {
            val slotId = supportedAmazonDimensionsMap[dimension] ?: continue
            val size = DTBAdSize(dimension.w, dimension.h, slotId)
            size.pubSettings = getPublisherSettings(adProvider.applicationContext)
            adSizeList.add(size)
        }
        return adSizeList
    }

    private fun getPublisherSettings(context: Context): JSONObject {
        val privacyObj = JSONObject()
        try {
            val privacyString = adProvider.ccpaAdsPrivacyString
            privacyObj.put("us_privacy", privacyString)
        } catch (ex: JSONException) {
            RemoteLog.e(
                context.applicationContext,
                EventLog.Builder()
                    .setMessage("Failed to set IAB's privacy string in pubSettings")
                    .setModule(LogModules.ADS)
                    .setErrorMessage(ex.message)
                    .build()
            )
        }
        return privacyObj
    }

    sealed class DTBAdResult {
        data class Success(val response: DTBAdResponse) : DTBAdResult()
        data class Failure(
            val apsError: com.amazon.device.ads.AdError
        ) : DTBAdResult()
    }

    companion object {
        private const val TAG = "APSBidFetcher"
    }
}