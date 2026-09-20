package com.wapo.adsinf.sdk.google

import android.content.Context
import android.util.Size
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.wapo.adsinf.BannerAdView
import com.wapo.adsinf.R
import com.wapo.adsinf.interfaces.AdLoadCallback
import com.wapo.adsinf.interfaces.BannerAdRenderer
import com.wapo.adsinf.interfaces.AdUiEventsListener
import com.wapo.adsinf.models.AdError
import com.wapo.adsinf.models.AdLoadContext
import com.wapo.adsinf.models.AdLoadSession
import com.wapo.adsinf.models.AdRequest
import com.wapo.adsinf.sdk.google.GoogleAdsUtils.toAdError
import com.wapo.adsinf.utils.AdLoggingUtils
import com.wapo.adsinf.utils.AdLoggingUtils.toDebugString
import com.wapo.android.commons.util.Logger
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderType
import java.lang.ref.WeakReference
import kotlin.collections.orEmpty
import kotlin.math.ceil

class GoogleBannerAdRenderer : BannerAdRenderer() {
    private var adViewRef: WeakReference<AdManagerAdView?> = WeakReference(null)

    override fun load(
        parent: BannerAdView,
        adRequest: AdRequest,
        adLoadSession: AdLoadSession,
        callback: AdLoadCallback
    ) {
        if (adRequest !is AdRequest.Google) {
            val error = AdError(
                AdError.ErrorType.INVALID_RENDERER,
                message = "This renderer only supports AdRequest.Google requests. Incoming request is of type ${adRequest.javaClass.name}"
            )
            Logger.e(TAG, error.toDebugString(), error)
            callback.onAdFailed(error)
            return
        }
        loadGoogleRequest(parent, adRequest, adLoadSession, callback)
    }

    fun loadGoogleRequest(
        parent: BannerAdView,
        adRequest: AdRequest.Google,
        adLoadSession: AdLoadSession,
        callback: AdLoadCallback
    ) {
        val adView = buildAdManagerAdView(
            parent,
            adRequest,
            adLoadSession,
            callback,
        )
        parent.renderAdView(adView)
        startReqTimer(adRequest.id)
        parent.showLoading()
        adView.loadAd(adRequest.request)
    }

    private fun buildAdManagerAdView(
        adView: BannerAdView,
        adRequest: AdRequest.Google,
        adLoadSession: AdLoadSession,
        adLoadCallback: AdLoadCallback,
    ): AdManagerAdView {
        val adManagerAdView = AdManagerAdView(adView.context)
        adManagerAdView.apply {
            tag = AD_VIEW_TAG
            this.adUnitId = adRequest.adConfig.adUnitId
            adListener = DfpAdsUtilAdListener(
                context,
                adRequest,
                adManagerAdView,
                adView,
                adLoadSession,
                adLoadCallback
            )
            setAdSizes(
                *GoogleAdsUtils.convertToGmsAdSizesList(adRequest.adConfig.adDimensions)
                    .toTypedArray()
            )
        }
        adViewRef = WeakReference(adManagerAdView)
        return adManagerAdView
    }

    private fun buildTestAdView(
        adView: BannerAdView,
        adRequest: AdRequest.Google,
        adLoadSession: AdLoadSession,
        adLoadCallback: AdLoadCallback,
    ): AdManagerAdView {
        val adUnitId = adView.context.resources.getString(R.string.test_admob_banner_ad_unit_id)
        return buildAdManagerAdView(
            adView,
            adRequest.copy(adConfig = adRequest.adConfig.copy(adUnitId = adUnitId)),
            adLoadSession,
            adLoadCallback,
        )
    }

    override fun release(parent: BannerAdView) {
        super.release(parent)
        adViewRef.get()?.let {
            val adListener = it.adListener
            if (adListener is DfpAdsUtilAdListener) {
                adListener.onAdDestroy()
                it.adListener = object : AdListener() {}
            }
            it.destroy()
            parent.removeView(it)
        }
    }

    private inner class DfpAdsUtilAdListener(
        private val context: Context,
        private val adRequest: AdRequest.Google,
        private val adView: AdManagerAdView,
        private val uiEventsListener: AdUiEventsListener,
        private val adLoadSession: AdLoadSession,
        private val adLoadCallback: AdLoadCallback,
    ) : AdListener() {
        private var adLoadIndex = 0
        private var adImpressionIndex = 0

        override fun onAdClosed() {
            Logger.d(TAG, "onAdClosed()")
            super.onAdClosed()
        }

        override fun onAdOpened() {
            Logger.d(TAG, "onAdOpened()")
            super.onAdOpened()
        }

        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            Logger.d(TAG, "onAdFailedToLoad() - $loadAdError")
            val responseTime = endReqTimer(adRequest.id)
            if (adLoadSession.attempts.last().adResponse == null && responseTime != null) {
                adLoadSession.updateLastAttempt {
                    it.copy(
                        adResponse = AdLoadContext.AdResponse.Failure(
                            sdkName = AdLoaderType.GOOGLE,
                            responseTime = responseTime,
                            sdkErrorCode = loadAdError.code.toString(),
                            sdkErrorMessage = loadAdError.message,
                        )
                    )
                }
                AdLoggingUtils.remoteLogSdkFailure(
                    loadAdError.toAdError(),
                    adLoadSession,
                    AdLoaderType.GOOGLE
                )
            }
            adLoadCallback.onAdFailed(loadAdError.toAdError())
            // Remove progressbar
            uiEventsListener.hideLoading()

            super.onAdFailedToLoad(loadAdError)
        }

        override fun onAdLoaded() {
            Logger.d(TAG, "onAdLoaded()")
            val responseTime = endReqTimer(adRequest.id)
            if (adLoadSession.attempts.last().adResponse == null && responseTime != null) {
                adLoadSession.updateLastAttempt {
                    it.copy(
                        adResponse = AdLoadContext.AdResponse.Success(
                            sdkName = AdLoaderType.GOOGLE,
                            responseTime = responseTime,
                            loadIndex = adLoadIndex,
                            impressionIndex = adImpressionIndex,
                            winningBidSource = adView.responseInfo?.let {
                                it.loadedAdapterResponseInfo?.let {
                                    it.adSourceName.ifEmpty { it.adapterClassName.substringAfterLast(".") }
                                } ?: it.mediationAdapterClassName?.substringAfterLast(".")
                            },
                            adSize = adView.adSize?.let { Size(it.width, it.height) },
                        )
                    )
                }
                AdLoggingUtils.remoteLogSdkSuccess(
                    adLoadSession,
                    AdLoaderType.GOOGLE
                )
            }
            adLoadCallback.onAdLoaded()
            // Remove progressbar
            uiEventsListener.hideLoading()
            // Check adView has enough width to display Ad
            if (!hasEnoughWidthToDisplayAd(adView.measuredWidth, adView.measuredHeight)) {
                // Parent view is not having enough space for AdView.
                // Show offlineAd
                uiEventsListener.showOfflineAd(adRequest.adConfig)
            }

            adLoadIndex++
            super.onAdLoaded()
        }

        override fun onAdClicked() {
            Logger.d(TAG, "onAdClicked()")
            super.onAdClicked()
        }

        override fun onAdImpression() {
            Logger.d(TAG, "onAdImpression()")
            adLoadCallback.onAdImpression()
            if (needsResize()) uiEventsListener.resizeAdSlot()
            adImpressionIndex++
            super.onAdImpression()
        }

        private fun needsResize(): Boolean = isTallAdRequest(adView)

        private fun isTallAdRequest(adView: AdManagerAdView): Boolean {
            val requestedAdSizes = adView.adSizes
            if (requestedAdSizes != null) {
                for (size in requestedAdSizes) {
                    if (size.height == AdDimension.Tall.h) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * Method to release any resources.
         */
        fun onAdDestroy() {
            Logger.d(TAG, "onAdDestroy()")
        }

        fun hasEnoughWidthToDisplayAd(parentWidth: Int, parentHeight: Int): Boolean {
            // 0's validation condition here due to getMeasuredWidth()/getMeasuredHeight() are returning
            // zero on some devices.
            // So just returning true to keep as is behavior.
            if (parentWidth == 0 && parentHeight == 0) {
                return true
            }

            val outMetrics = context.resources.displayMetrics
            val density = outMetrics.density
            val parentWidthInDp = ceil((parentWidth / density).toDouble()).toFloat()
            val parentHeightInDp = ceil((parentHeight / density).toDouble()).toFloat()

            // Check view has enough width and height to display Ad of size adSize(s)
            var hasEnoughSize = false
            val adSize = adView.adSize
            if (adSize != null) {
                if (parentWidthInDp >= adSize.width) {
                    hasEnoughSize = true
                }
            } else if (adView.adSizes != null) {
                for (tAdSize in adView.adSizes.orEmpty()) {
                    if (parentWidthInDp >= tAdSize.width) {
                        hasEnoughSize = true
                        break
                    }
                }
            }
            return hasEnoughSize
        }
    }

    companion object {
        private const val TAG = "GoogleBannerAdRenderer"
        private const val AD_VIEW_TAG = "adviewTag"
    }
}