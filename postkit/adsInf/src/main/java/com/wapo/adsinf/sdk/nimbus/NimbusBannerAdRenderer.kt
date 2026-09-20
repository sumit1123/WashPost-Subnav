package com.wapo.adsinf.sdk.nimbus

import android.util.Size
import com.adsbynimbus.NimbusAdManager
import com.adsbynimbus.NimbusError
import com.adsbynimbus.openrtb.request.Format
import com.adsbynimbus.render.AdController
import com.adsbynimbus.render.AdEvent
import com.adsbynimbus.request.NimbusRequest
import com.adsbynimbus.request.NimbusResponse
import com.wapo.adsinf.BannerAdView
import com.wapo.adsinf.interfaces.AdLoadCallback
import com.wapo.adsinf.interfaces.BannerAdRenderer
import com.wapo.adsinf.models.AdError
import com.wapo.adsinf.models.AdLoadContext
import com.wapo.adsinf.models.AdLoadSession
import com.wapo.adsinf.models.AdRequest
import com.wapo.adsinf.sdk.nimbus.NimbusUtils.toAdError
import com.wapo.adsinf.utils.AdLoggingUtils
import com.wapo.adsinf.utils.AdLoggingUtils.toDebugString
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderType

class NimbusBannerAdRenderer(
    private val nimbusAdManager: NimbusAdManager = NimbusAdManager(),
) : BannerAdRenderer() {

    override fun load(
        parent: BannerAdView,
        adRequest: AdRequest,
        adLoadSession: AdLoadSession,
        callback: AdLoadCallback
    ) {
        if (adRequest !is AdRequest.Nimbus) {
            val error = AdError(
                AdError.ErrorType.INVALID_RENDERER,
                message = "This renderer only supports AdRequest.Nimbus requests. Incoming request is of type ${adRequest.javaClass.name}",
            )
            Logger.e(TAG, error.toDebugString(), error)
            callback.onAdFailed(error)
            return
        }
        loadNimbusRequest(parent, adRequest, adLoadSession, callback)
    }

    private fun loadNimbusRequest(
        parent: BannerAdView,
        adRequest: AdRequest.Nimbus,
        adLoadSession: AdLoadSession,
        callback: AdLoadCallback
    ) {
        parent.showLoading()
        val nimbusAdListener = NimbusAdListener(parent, callback, adRequest, adLoadSession)
        startReqTimer(adRequest.id)
        nimbusAdManager.showAd(
            request = adRequest.request,
            viewGroup = parent,
            listener = nimbusAdListener
        )
    }

    override fun release(parent: BannerAdView) {
        super.release(parent)
        parent.removeAllViews()
    }

    private inner class NimbusAdListener(
        val parentAdView: BannerAdView,
        val callback: AdLoadCallback,
        val request: AdRequest.Nimbus,
        val adLoadSession: AdLoadSession,
    ) : NimbusAdManager.Listener {
        private var adLoadIndex = 0
        private var adImpressionIndex = 0
        private var nimbusResponse: NimbusResponse? = null

        override fun onAdResponse(nimbusResponse: NimbusResponse) {
            this.nimbusResponse = nimbusResponse
            Logger.d(TAG, "Nimbus Ad Loaded from ${nimbusResponse.bid.network}")
            Logger.d(TAG, "onAdResponse: NimbusResponse=${nimbusResponse.toDebugString()}")
            super.onAdResponse(nimbusResponse)
        }

        override fun onAdRendered(controller: AdController) {
            Logger.d(TAG, "onAdRendered: controller=$controller")
            endRequestTimerAndLogSuccess(request.id, nimbusResponse)
            controller.listeners.add(object : AdController.Listener {
                override fun onAdEvent(adEvent: AdEvent) {
                    Logger.d(TAG, "AdController.Listener onAdEvent: $adEvent")
                    when (adEvent) {
                        AdEvent.LOADED -> {
                            parentAdView.hideLoading()
                            callback.onAdLoaded()
                            adLoadIndex++
                        }

                        AdEvent.IMPRESSION -> {
                            parentAdView.hideLoading()
                            if (isTallAdRequest(request.request)) parentAdView.resizeAdSlot()
                            callback.onAdImpression()
                            adImpressionIndex++
                        }

                        AdEvent.DESTROYED -> {
                            parentAdView.hideLoading()
                            if (adLoadIndex == 0) {
                                val adError = AdError(
                                    AdError.ErrorType.RENDER_ERROR,
                                    "Got AdEvent.DESTROYED before AdEvent.LOADED",
                                )
                                callback.onAdFailed(adError)
                            }
                        }

                        else -> {}
                    }
                }

                private fun isTallAdRequest(nimbusRequest: NimbusRequest): Boolean {
                    val formats = mutableListOf<Format>()
                    formats.add(nimbusRequest.request.format)
                    nimbusRequest.request.imp.first().banner?.format?.forEach { formats.add(it) }
                    for (format in formats) {
                        if (format.h == AdDimension.Tall.h) {
                            return true
                        }
                    }
                    return false
                }

                override fun onError(error: NimbusError) {
                    this@NimbusAdListener.onError(error)
                }
            })
        }

        override fun onError(error: NimbusError) {
            Logger.e(TAG, "onError: $error")
            parentAdView.hideLoading()
            endRequestTimerAndLogFailure(request.id, error)
            callback.onAdFailed(error.toAdError())
        }

        private fun endRequestTimerAndLogSuccess(
            requestId: String,
            nimbusResponse: NimbusResponse?
        ) {
            val responseTime = endReqTimer(requestId)
            if (adLoadSession.attempts.last().adResponse == null && responseTime != null) {
                adLoadSession.updateLastAttempt {
                    it.copy(
                        adResponse = AdLoadContext.AdResponse.Success(
                            sdkName = AdLoaderType.NIMBUS,
                            responseTime = responseTime,
                            loadIndex = adLoadIndex,
                            impressionIndex = adImpressionIndex,
                            winningBidSource = nimbusResponse?.bid?.network,
                            adSize = nimbusResponse?.bid?.let { Size(it.width, it.height) },
                        )
                    )
                }
                AdLoggingUtils.remoteLogSdkSuccess(adLoadSession, AdLoaderType.NIMBUS)
            }
        }

        private fun endRequestTimerAndLogFailure(requestId: String, error: Exception) {
            val responseTime = endReqTimer(requestId)
            if (adLoadSession.attempts.last().adResponse == null && responseTime != null) {
                adLoadSession.updateLastAttempt {
                    it.copy(
                        adResponse = AdLoadContext.AdResponse.Failure(
                            sdkName = AdLoaderType.NIMBUS,
                            responseTime = responseTime,
                            sdkErrorCode = (error as? NimbusError)?.errorType?.name,
                            sdkErrorMessage = (error as? NimbusError)?.message,
                        )
                    )
                }
                AdLoggingUtils.remoteLogSdkFailure(
                    when (error) {
                        is AdError -> error
                        is NimbusError -> error.toAdError()
                        else -> AdError(
                            AdError.ErrorType.UNKNOWN,
                            error.message ?: error.toString()
                        )
                    },
                    adLoadSession,
                    AdLoaderType.NIMBUS
                )
            }
        }
    }

    companion object {
        private const val TAG = "NimbusBannerAdRenderer"
    }
}