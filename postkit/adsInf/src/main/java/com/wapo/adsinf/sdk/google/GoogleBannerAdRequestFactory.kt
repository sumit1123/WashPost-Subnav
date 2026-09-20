package com.wapo.adsinf.sdk.google

import com.amazon.device.ads.DTBAdResponse
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.wapo.adsinf.interfaces.AdRequestFactory
import com.wapo.adsinf.models.AdConfig
import com.wapo.adsinf.models.AdLoadContext
import com.wapo.adsinf.models.AdRequest
import com.wapo.adsinf.models.AdRequestTargets
import com.wapo.adsinf.models.AdResult
import com.wapo.adsinf.models.AdLoadSession
import com.wapo.adsinf.models.NetworkExtras
import com.wapo.adsinf.sdk.amazon.APSBidLoader
import com.wapo.adsinf.utils.AdLoggingUtils.toDebugString
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.domain.models.config.banners.AdBidSource
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderConfig
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderType

class GoogleBannerAdRequestFactory(
    private val apsBidLoader: APSBidLoader = APSBidLoader(),
) : AdRequestFactory {

    override suspend fun createRequest(
        adConfig: AdConfig,
        adLoaderConfig: AdLoaderConfig,
        adLoadSession: AdLoadSession,
    ): AdResult<AdRequest.Google> {
        adLoadSession.updateLastAttempt {
            it.copy(
                adRequest = AdLoadContext.AdRequest(
                    sdkName = AdLoaderType.GOOGLE,
                    section = adConfig.section.orEmpty(),
                    adUnitId = adConfig.adUnitId,
                    bidSources = adLoaderConfig.bidSources,
                )
            )
        }

        //  Use a copy of targeting params to prevent stale APS targeting on subsequent ad refreshes
        val adRequestTargets = adConfig.adRequestTargets.copy()
        val networkExtras = adConfig.networkExtras
        var dtbResponse: APSBidLoader.DTBAdResult? = null
        if (adLoaderConfig.bidSources.contains(AdBidSource.APS)) {
            dtbResponse = apsBidLoader.fetchAPSBid(adConfig, adLoaderConfig.loaderType, adLoadSession)
            if (dtbResponse is APSBidLoader.DTBAdResult.Success) {
                adRequestTargets.putAll(dtbResponse.response.getCustomRequestTargets())
            }
        }
        if (adConfig.safeAdLoadChain.any { it.loaderType == AdLoaderType.NIMBUS }) {
            adRequestTargets.addNimbusEnabled(true)
        }
        return AdResult.Success(
            AdRequest.Google(
                request = createAdManagerAdRequest(adRequestTargets, networkExtras),
                adConfig = adConfig,
                adLoaderConfig = adLoaderConfig,
            ),
        ).also {
            Logger.d(TAG, "AdManagerAdRequest created: ${it.value.request.toDebugString()}")
        }
    }

    private fun createAdManagerAdRequest(
        adRequestTargets: AdRequestTargets?,
        networkExtras: NetworkExtras?
    ): AdManagerAdRequest {
        val request = AdManagerAdRequest.Builder()

        if (adRequestTargets != null) {
            // Adding Custom Targeting list
            if (adRequestTargets.customTargetsMap.isNotEmpty()) {
                Logger.d(TAG, "CustomTargetsMap: ${adRequestTargets.customTargetsMap}")
                for (entry in adRequestTargets.customTargetsMap.entries) {
                    request.addCustomTargeting(entry.key, entry.value)
                }
            }
            // Adding Simple Custom Targeting list
            if (adRequestTargets.simpleCustomTargetsMap.isNotEmpty()) {
                Logger.d(TAG, "SimpleCustomTargetsMap: ${adRequestTargets.simpleCustomTargetsMap}")
                for (entry in adRequestTargets.simpleCustomTargetsMap.entries) {
                    request.addCustomTargeting(entry.key, entry.value)
                }
            }
            // Content url
            val contentUrl = adRequestTargets.contentUrl
            if (!contentUrl.isNullOrEmpty() && contentUrl.length <= 512) {
                request.setContentUrl(adRequestTargets.contentUrl.orEmpty())
            }
        }
        // NetworkExtras
        if (networkExtras != null && networkExtras.adapterClass != null && networkExtras.networkExtrasBundle != null) {
            request.addNetworkExtrasBundle(
                networkExtras.adapterClass,
                networkExtras.networkExtrasBundle
            )
        }

        return (request.build())
    }

    private fun DTBAdResponse.getCustomRequestTargets(): Map<String, List<String>> {
        if (adCount > 0) return defaultDisplayAdsRequestCustomParams
        return emptyMap()
    }

    companion object {
        private const val TAG = "GoogleAdRequestFactory"
    }
}