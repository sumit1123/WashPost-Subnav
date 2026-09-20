package com.wapo.adsinf.sdk.nimbus

import com.adsbynimbus.openrtb.enumerations.Position
import com.adsbynimbus.openrtb.request.App
import com.adsbynimbus.openrtb.request.Content
import com.adsbynimbus.request.NimbusRequest
import com.adsbynimbus.request.RequestManager
import com.adsbynimbus.request.addApsLoader
import com.adsbynimbus.request.addApsResponse
import com.adsbynimbus.request.withAdMobBanner
import com.wapo.adsinf.interfaces.AdRequestFactory
import com.wapo.adsinf.models.AdConfig
import com.wapo.adsinf.models.AdLoadContext
import com.wapo.adsinf.models.AdRequest
import com.wapo.adsinf.models.AdResult
import com.wapo.adsinf.models.AdLoadSession
import com.wapo.adsinf.sdk.amazon.APSBidLoader
import com.wapo.adsinf.utils.AdLoggingUtils.toDebugString
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.domain.manager.ConfigManager
import com.washingtonpost.android.config.domain.models.config.banners.AdBidSource
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderConfig
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderType

class NimbusBannerAdRequestFactory(
    private val apsBidLoader: APSBidLoader = APSBidLoader(),
) : AdRequestFactory {
    private val bannersConfig get() = ConfigManager.getInstance().config.adsConfig.bannersConfig
    private val nimbusConfig get() = bannersConfig.nimbus

    override suspend fun createRequest(
        adConfig: AdConfig,
        adLoaderConfig: AdLoaderConfig,
        adLoadSession: AdLoadSession,
    ): AdResult<AdRequest.Nimbus> {
        adLoadSession.updateLastAttempt {
            it.copy(
                adRequest = AdLoadContext.AdRequest(
                    sdkName = AdLoaderType.NIMBUS,
                    section = adConfig.section.orEmpty(),
                    adUnitId = adConfig.adUnitId,
                    bidSources = adLoaderConfig.bidSources,
                )
            )
        }

        val nimbusRequest = getBaseNimbusRequest(adConfig)
        adConfig.adRequestTargets.contentUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { nimbusRequest.setContentUrl(it) }
        if (adLoaderConfig.bidSources.contains(AdBidSource.ADMOB)) {
            nimbusRequest.apply {
                withAdMobBanner(adUnitId = nimbusConfig.adMobUnitId)
            }
        }
        if (adLoaderConfig.bidSources.contains(AdBidSource.APS)) {
            val dtbResponse = apsBidLoader.fetchAPSBid(adConfig, adLoaderConfig.loaderType, adLoadSession)
            when (dtbResponse) {
                is APSBidLoader.DTBAdResult.Success -> {
                    nimbusRequest.apply {
                        addApsResponse(dtbResponse.response)
                        addApsLoader(dtbResponse.response.adLoader)
                    }
                }

                is APSBidLoader.DTBAdResult.Failure -> {
                    nimbusRequest.apply {
                        addApsLoader(dtbResponse.apsError.adLoader)
                    }
                }
            }
        }
        return AdResult.Success(
            AdRequest.Nimbus(
                request = nimbusRequest,
                adConfig = adConfig,
                adLoaderConfig = adLoaderConfig,
            ),
        ).also {
            Logger.d(TAG, "NimbusRequest created: ${it.value.request.toDebugString()}")
        }
    }

    private fun getBaseNimbusRequest(config: AdConfig): NimbusRequest {
        val nimbusFormats = NimbusUtils.convertToNimbusFormatList(config.adDimensions)
        return NimbusRequest.forBannerAd(
            config.adUnitId,
            nimbusFormats.first(),
            Position.UNKNOWN,
        ).apply {
            //  Apply all formats
            request.imp[0].banner?.format = nimbusFormats.toTypedArray()
        }
    }

    private fun NimbusRequest.setContentUrl(url: String) {
        request.app = (request.app ?: RequestManager.getApp() ?: App()).apply {
            content = Content(url = url)
        }
    }

    companion object {
        private const val TAG = "NimbusAdRequestFactory"
    }
}
