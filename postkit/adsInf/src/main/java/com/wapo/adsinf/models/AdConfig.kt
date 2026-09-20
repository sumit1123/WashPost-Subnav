package com.wapo.adsinf.models

import androidx.annotation.DrawableRes
import com.wapo.adsinf.utils.AdsUtil
import com.washingtonpost.android.config.domain.models.config.banners.AdDimension
import com.washingtonpost.android.config.domain.models.config.banners.AdLoaderConfig
import kotlin.collections.filter
import kotlin.collections.map

/**
 * Configuration object used by the [com.wapo.adsinf.BannerAdView] to request and render ads
 * This class centralizes everything needed to determine:
 * - which ad network to use
 * - how the request should be built
 * - how the ad should be rendered
 * - what fallback behavior to apply
 *
 * @param adUnitId Identifier for the specific ad placement
 * @param adRequestTargets Targeting parameters for the ad request
 * @param networkExtras Additional targeting information such as CCPA or other regulatory bundles
 * @param adLoadConfig Ordered list of loader/bidSources combinations to load the ad from. The default
 * value is loaded from `config.json` or the remote config file
 * (e.g. APS can load an ad using GAM or Nimbus, so this param helps to decide which one to use)
 * @param adDimensions List of possible ad dimensions that the AdView may use to render the ad
 * @param adSlotType Indicates whether the slot is "tall" or "short". This is used to resize the ad
 * container
 * @param containerMinHeight Height the [com.wapo.adsinf.BannerAdView] should reserve in the ad layout
 * @param offlineViewResId Optional drawable to display when user is offline or when no ad can be
 * rendered
 * @param section Identifier of the screen or section of the app where this ad is being rendered.
 * Useful for analytics and debugging
 */
data class AdConfig(
    val adUnitId: String,
    val adRequestTargets: AdRequestTargets = AdRequestTargets.getDefault(),
    val networkExtras: NetworkExtras? = null,
    private val adLoadConfig: List<AdLoaderConfig> = AdsUtil.getAdLoadConfig(),
    val adDimensions: List<AdDimension> = listOf(AdsUtil.DEFAULT_AD_SIZE),
    val adSlotType: AdSlotType? = null,
    val containerMinHeight: Int = AdsUtil.MIN_HEIGHT_TO_RESERVE_IN_AD_CONTAINER,
    @DrawableRes val offlineViewResId: Int? = null,
    val section: String? = null,
) {
    val safeAdLoadChain: List<AdLoaderConfig> by lazy {
        adLoadConfig
            .filter { AdsUtil.isAdLoaderAvailable(it.loaderType) }
            .map {
                it.copy(
                    bidSources = it.bidSources.filter { AdsUtil.isAdBidSourceAvailable(it) }
                )
            }
    }
}